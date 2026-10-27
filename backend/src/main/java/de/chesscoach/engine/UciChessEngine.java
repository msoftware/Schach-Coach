package de.chesscoach.engine;

import de.chesscoach.domain.analysis.Score;
import de.chesscoach.domain.position.MoveOracle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Generische UCI-Integration. Unterstützt jede Engine, die das UCI-Protokoll spricht
 * (z. B. Stockfish als austauschbare Alternative); Calvin-spezifisches Verhalten liegt in
 * {@link CalvinChessEngine}.
 */
public class UciChessEngine implements ChessEngine {

    private static final Logger log = LoggerFactory.getLogger(UciChessEngine.class);

    /** Konfiguration einer Instanz (aus {@code EngineProperties} abgeleitet). */
    public record Settings(String role, String configuredVersion, Map<String, String> options,
                           Duration startTimeout, Duration searchGrace, Duration maxSearchTime) {
    }

    protected final UciProcessManager process;
    protected final Settings settings;
    private final MoveOracle moveOracle;
    private final ReentrantLock searchLock = new ReentrantLock(true);

    private volatile EngineState state = EngineState.NOT_STARTED;
    private volatile String message = "Engine wurde noch nicht gestartet.";
    private volatile String engineName = "unbekannt";
    private volatile String engineAuthor = "unbekannt";
    private volatile boolean supportsMultiPv;
    private volatile boolean searching;
    private volatile boolean stopRequested;
    private final java.util.concurrent.atomic.AtomicInteger starts = new java.util.concurrent.atomic.AtomicInteger();

    public UciChessEngine(UciProcessManager process, Settings settings, MoveOracle moveOracle) {
        this.process = process;
        this.settings = settings;
        this.moveOracle = moveOracle;
    }

    @Override
    public void start() {
        searchLock.lock();
        try {
            startLocked();
        } finally {
            searchLock.unlock();
        }
    }

    private void startLocked() {
        state = EngineState.STARTING;
        starts.incrementAndGet();
        try {
            process.destroy();
            process.start();
            handshake();
            state = EngineState.READY;
            message = "Engine ist bereit.";
            log.info("Engine '{}' ({}) bereit: {} {}", settings.role(), engineName, engineAuthor, settings.configuredVersion());
        } catch (RuntimeException e) {
            state = EngineState.FAILED;
            message = "Die Schach-Engine konnte nicht gestartet werden.";
            process.destroy();
            log.error("Engine '{}' konnte nicht gestartet werden: {}", settings.role(), e.getMessage());
            throw e instanceof EngineException ee ? ee : new EngineUnavailableException(message, e);
        }
    }

    private void handshake() {
        process.send("uci");
        long deadline = System.nanoTime() + settings.startTimeout().toNanos();
        boolean uciOk = false;
        List<String> optionNames = new ArrayList<>();
        while (!uciOk) {
            String line = readUntil(deadline, "uciok");
            if (line.startsWith("id name ")) {
                engineName = line.substring("id name ".length()).trim();
            } else if (line.startsWith("id author ")) {
                engineAuthor = line.substring("id author ".length()).trim();
            } else if (line.startsWith("option name ")) {
                String rest = line.substring("option name ".length());
                int typeIndex = rest.indexOf(" type ");
                optionNames.add(typeIndex > 0 ? rest.substring(0, typeIndex) : rest);
            } else if (line.equals("uciok")) {
                uciOk = true;
            }
        }
        supportsMultiPv = optionNames.stream().anyMatch(n -> n.equalsIgnoreCase("MultiPV"));
        for (Map.Entry<String, String> option : settings.options().entrySet()) {
            if (optionNames.stream().anyMatch(n -> n.equalsIgnoreCase(option.getKey()))) {
                process.send("setoption name " + option.getKey() + " value " + option.getValue());
            } else {
                log.warn("Engine bietet die Option '{}' nicht an; sie wird ignoriert.", option.getKey());
            }
        }
        synchronizeReady(deadline);
    }

    private String readUntil(long deadlineNanos, String awaited) {
        long remaining = deadlineNanos - System.nanoTime();
        if (remaining <= 0) {
            throw new EngineTimeoutException("Die Engine hat nicht rechtzeitig auf '" + awaited + "' geantwortet.");
        }
        String line = process.readLine(Duration.ofNanos(remaining));
        if (line == null) {
            throw new EngineTimeoutException("Die Engine hat nicht rechtzeitig auf '" + awaited + "' geantwortet.");
        }
        return line.trim();
    }

    private void synchronizeReady(long deadlineNanos) {
        process.send("isready");
        while (true) {
            String line = readUntil(deadlineNanos, "readyok");
            if (line.equals("readyok")) {
                return;
            }
        }
    }

    @Override
    public EngineInfo getEngineInfo() {
        return new EngineInfo(settings.role(), engineName, engineAuthor, settings.configuredVersion(), state, message,
                supportsMultiPv);
    }

    @Override
    public EngineAnalysis analyze(String fen, AnalysisLimits limits) {
        return search(fen, limits);
    }

    @Override
    public EngineMove findBestMove(String fen, EngineLimits limits) {
        EngineAnalysis analysis = search(fen, limits.toAnalysisLimits());
        if (analysis.bestMove() == null) {
            throw new EngineProtocolException("Die Engine hat keinen Zug geliefert.");
        }
        return new EngineMove(analysis.bestMove(), analysis);
    }

    /** Übersetzt die Suchgrenzen in ein {@code go}-Kommando. Kann für Engine-Besonderheiten überschrieben werden. */
    protected String goCommand(AnalysisLimits limits) {
        StringBuilder go = new StringBuilder("go");
        if (limits.depth() != null) {
            go.append(" depth ").append(limits.depth());
        }
        if (limits.nodes() != null) {
            go.append(" nodes ").append(limits.nodes());
        }
        if (limits.movetimeMillis() != null) {
            go.append(" movetime ").append(limits.movetimeMillis());
        }
        return go.toString();
    }

    private EngineAnalysis search(String fen, AnalysisLimits limits) {
        if (fen == null || fen.indexOf('\n') >= 0 || fen.length() > 100) {
            throw new IllegalArgumentException("Ungültige Stellung");
        }
        if (moveOracle != null && moveOracle.legalMoves(fen).isEmpty()) {
            throw new IllegalArgumentException("In einer Stellung ohne legale Züge kann nicht gesucht werden");
        }
        searchLock.lock();
        try {
            ensureRunning();
            return searchLocked(fen, limits);
        } finally {
            searching = false;
            if (state == EngineState.SEARCHING) {
                state = EngineState.READY;
            }
            searchLock.unlock();
        }
    }

    private void ensureRunning() {
        if (state == EngineState.READY && process.isAlive()) {
            return;
        }
        if (state == EngineState.READY || state == EngineState.SEARCHING) {
            log.warn("Engine '{}' ist abgestürzt und wird neu gestartet.", settings.role());
        }
        startLocked();
    }

    private EngineAnalysis searchLocked(String fen, AnalysisLimits limits) {
        long started = System.nanoTime();
        long readyDeadline = started + settings.startTimeout().toNanos();
        try {
            process.discardPendingOutput();
            synchronizeReady(readyDeadline);
            stopRequested = false;
            searching = true;
            state = EngineState.SEARCHING;
            process.send("position fen " + fen);
            process.send(goCommand(limits));
        } catch (EngineException e) {
            markFailed();
            throw e;
        }

        Duration budget = limits.movetimeMillis() != null
                ? Duration.ofMillis(limits.movetimeMillis()).plus(settings.searchGrace())
                : settings.maxSearchTime();
        long deadline = started + budget.toNanos();
        EngineAnalysisParser.InfoLine lastComplete = null;
        long lastNodes = 0;
        String bestMove = null;
        boolean bestMoveReceived = false;
        boolean timedOut = false;

        while (!bestMoveReceived) {
            long remaining = deadline - System.nanoTime();
            if (remaining <= 0) {
                if (timedOut) {
                    restartAfterFailure();
                    throw new EngineTimeoutException("Die Engine hat nicht rechtzeitig geantwortet.");
                }
                timedOut = true;
                process.send("stop");
                deadline = System.nanoTime() + settings.searchGrace().toNanos();
                continue;
            }
            String line;
            try {
                line = process.readLine(Duration.ofNanos(remaining));
            } catch (EngineUnavailableException e) {
                markFailed();
                throw e;
            }
            if (line == null) {
                continue;
            }
            line = line.trim();
            if (line.startsWith("info")) {
                var info = EngineAnalysisParser.parseInfo(line);
                if (info.isPresent()) {
                    var parsed = info.get();
                    if (parsed.nodes() != null) {
                        lastNodes = parsed.nodes();
                    }
                    if (parsed.isComplete() && (parsed.multipv() == null || parsed.multipv() == 1)) {
                        lastComplete = parsed;
                    }
                }
            } else if (line.startsWith("bestmove")) {
                var best = EngineAnalysisParser.parseBestMove(line);
                if (best.isEmpty()) {
                    restartAfterFailure();
                    throw new EngineProtocolException("Die Engine hat eine ungültige bestmove-Zeile geliefert.");
                }
                bestMove = best.get().move().orElse(null);
                bestMoveReceived = true;
            }
        }

        long elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
        final String chosen = bestMove;
        if (chosen != null && moveOracle != null
                && moveOracle.legalMoves(fen).stream().noneMatch(m -> m.uci().equals(chosen))) {
            log.error("Engine '{}' lieferte einen illegalen Zug ({}) – Neustart.", settings.role(), bestMove);
            restartAfterFailure();
            throw new EngineProtocolException("Die Engine hat einen ungültigen Zug geliefert.");
        }
        Score score = lastComplete == null ? null : lastComplete.score();
        List<String> pv = lastComplete == null ? List.of() : lastComplete.pv();
        if (bestMove != null && (pv.isEmpty() || !pv.get(0).equals(bestMove))) {
            // Hauptvariante passt nicht zum bestmove (z. B. abgebrochene Iteration): nur den Zug selbst verwenden.
            pv = List.of(bestMove);
        }
        boolean completed = bestMove != null && lastComplete != null && !timedOut && !stopRequested;
        return new EngineAnalysis(UUID.randomUUID().toString(), fen, engineName, settings.configuredVersion(),
                lastComplete == null ? 0 : reportedDepth(lastComplete.depth()),
                lastComplete == null || lastComplete.selectiveDepth() == null ? 0 : lastComplete.selectiveDepth(),
                lastNodes, elapsed, score, bestMove, pv, List.of(), completed, limits);
    }

    /** Ermöglicht Engines mit abweichender Tiefenzählung eine Korrektur. */
    protected int reportedDepth(int depth) {
        return depth;
    }

    private void markFailed() {
        state = EngineState.FAILED;
        message = "Die Engine ist ausgefallen und wird beim nächsten Aufruf neu gestartet.";
    }

    private void restartAfterFailure() {
        try {
            startLocked();
        } catch (EngineException e) {
            log.error("Neustart der Engine '{}' fehlgeschlagen: {}", settings.role(), e.getMessage());
        }
    }

    @Override
    public void stopAnalysis() {
        if (searching) {
            stopRequested = true;
            try {
                process.send("stop");
            } catch (EngineException e) {
                log.debug("stop konnte nicht gesendet werden: {}", e.getMessage());
            }
        }
    }

    public boolean isSearching() {
        return searching;
    }

    /** Anzahl der Neustarts nach dem ersten Start (z. B. nach Absturz oder Timeout). */
    public int restartCount() {
        return Math.max(0, starts.get() - 1);
    }

    @Override
    public void shutdown() {
        stopAnalysis();
        process.destroy();
        state = EngineState.STOPPED;
        message = "Engine wurde beendet.";
    }
}
