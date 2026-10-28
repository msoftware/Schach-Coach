package de.chesscoach.application.game;

import de.chesscoach.application.ErrorCode;
import de.chesscoach.application.GameException;
import de.chesscoach.application.analysis.AnalysisEntry;
import de.chesscoach.application.analysis.AnalysisService;
import de.chesscoach.chess.ChessPositionAdapter;
import de.chesscoach.chess.ChessRulesService;
import de.chesscoach.chess.LegalMove;
import de.chesscoach.domain.game.MoveRecord;
import de.chesscoach.engine.EngineException;
import de.chesscoach.engine.EngineProtocolException;
import de.chesscoach.engine.EngineRegistry;
import de.chesscoach.engine.EngineTimeoutException;
import de.chesscoach.engine.EngineUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

/**
 * Validiert und führt Züge aus. Benutzerzüge lösen automatisch eine Coaching-Analyse aus; Computerzüge werden
 * mit der Spiel-Engine berechnet. Wiederholte Anfragen (gleiche erwartete Version und gleicher Zug) liefern
 * das bereits gespeicherte Ergebnis, statt den Zug doppelt auszuführen.
 */
@Service
public class MoveService {

    private static final Logger log = LoggerFactory.getLogger(MoveService.class);

    private final GameService games;
    private final ChessRulesService rules;
    private final EngineRegistry engines;
    private final ComputerPlayer computer;
    private final AnalysisService analysisService;

    public MoveService(GameService games, ChessRulesService rules, EngineRegistry engines, ComputerPlayer computer,
                       AnalysisService analysisService) {
        this.games = games;
        this.rules = rules;
        this.engines = engines;
        this.computer = computer;
        this.analysisService = analysisService;
    }

    /** Ergebnis eines Zuges; {@code replayed} kennzeichnet eine erkannte Wiederholung derselben Anfrage. */
    public record MoveResult(MoveRecord move, GameSnapshot game, boolean replayed) {
    }

    public MoveResult playUserMove(String gameId, String uci, Integer expectedVersion) {
        String normalized = uci == null ? "" : uci.trim().toLowerCase(Locale.ROOT);
        return games.withLock(gameId, s -> {
            List<MoveRecord> moves = s.moves();
            if (expectedVersion != null && expectedVersion == s.positionVersion() - 1 && !moves.isEmpty()) {
                MoveRecord last = moves.get(moves.size() - 1);
                if (last.byUser() && last.uci().equals(normalized) && last.positionVersion() == s.positionVersion()) {
                    return new MoveResult(last, s.snapshot(true), true);
                }
            }
            GameService.requireVersion(s, expectedVersion);
            if (s.status().isFinished()) {
                throw new GameException(ErrorCode.GAME_FINISHED, "Die Partie ist beendet. Es können keine Züge mehr ausgeführt werden.",
                        s.snapshot(true));
            }
            if (s.position().sideToMove() != s.playerColor() || s.pendingEngineMove() != null) {
                throw new GameException(ErrorCode.NOT_YOUR_TURN, "Der Computer ist am Zug.", s.snapshot(true));
            }
            if (!rules.isWellFormedUci(normalized) || !s.position().isLegal(normalized)) {
                throw new GameException(ErrorCode.ILLEGAL_MOVE, "Der angeforderte Zug ist in der aktuellen Stellung nicht legal.",
                        s.snapshot(true));
            }
            s.redoStack().clear();
            MoveRecord record = applyMove(s, s.position(), normalized, true, rules);
            scheduleAnalysis(s, record, 1);
            return new MoveResult(record, s.snapshot(true), false);
        });
    }

    /** Führt einen bereits als legal geprüften Zug aus (unter der Sperre der Sitzung). */
    static MoveRecord applyMove(GameSession s, ChessPositionAdapter current, String uci, boolean byUser, ChessRulesService rules) {
        LegalMove described = current.describe(uci);
        ChessPositionAdapter next = current.play(uci);
        MoveRecord record = new MoveRecord(UUID.randomUUID().toString(), s.moves().size() + 1, described.uci(), described.san(),
                current.fen(), next.fen(), current.sideToMove(), byUser, s.nextVersion(), Instant.now());
        s.apply(next, record);
        if (s.status().isFinished()) {
            log.info("Partie beendet nach {} Halbzügen: {}", record.ply(), s.status());
        }
        return record;
    }

    /** Legt eine Analyse für den Benutzerzug an und reiht sie ein (unter der Sperre der Sitzung). */
    AnalysisEntry scheduleAnalysis(GameSession s, MoveRecord record, int depthLevel) {
        List<MoveRecord> moves = s.moves();
        int index = moves.indexOf(record);
        MoveRecord previousOpponent = index >= 1 && !moves.get(index - 1).byUser() ? moves.get(index - 1) : null;
        MoveRecord previousOwn = index >= 2 && moves.get(index - 2).byUser() ? moves.get(index - 2) : null;
        AnalysisEntry old = s.analyses().get(record.moveId());
        if (old != null) {
            analysisService.cancel(old);
        }
        AnalysisEntry entry = new AnalysisEntry(s.gameId(), record, previousOpponent, previousOwn, s.analysisProfile(),
                s.settingsVersion(), depthLevel);
        s.analyses().put(record.moveId(), entry);
        analysisService.submit(entry);
        return entry;
    }

    public GameSnapshot redo(String gameId, Integer expectedVersion) {
        return games.redo(gameId, expectedVersion, (s, record) -> scheduleAnalysis(s, record, 1));
    }

    /**
     * Fordert den Computerzug an. Läuft für dieselbe Positionsversion bereits eine Berechnung, wird auf deren
     * Ergebnis gewartet (keine doppelte Ausführung).
     */
    public MoveResult requestEngineMove(String gameId, Integer expectedVersion) {
        CompletableFuture<MoveRecord> future;
        boolean owner = false;
        String fen;
        ChessPositionAdapter position;
        GameSession session = games.session(gameId);
        session.lock().lock();
        try {
            List<MoveRecord> moves = session.moves();
            if (expectedVersion != null && expectedVersion == session.positionVersion() - 1 && !moves.isEmpty()) {
                MoveRecord last = moves.get(moves.size() - 1);
                if (!last.byUser() && last.positionVersion() == session.positionVersion()) {
                    return new MoveResult(last, session.snapshot(true), true);
                }
            }
            if (session.pendingEngineMove() != null && (expectedVersion == null || expectedVersion == session.pendingEngineVersion())) {
                future = session.pendingEngineMove();
            } else {
                GameService.requireVersion(session, expectedVersion);
                if (session.status().isFinished()) {
                    throw new GameException(ErrorCode.GAME_FINISHED, "Die Partie ist beendet.", session.snapshot(true));
                }
                if (session.position().sideToMove() == session.playerColor()) {
                    throw new GameException(ErrorCode.NOT_YOUR_TURN, "Der Benutzer ist am Zug.", session.snapshot(true));
                }
                future = new CompletableFuture<>();
                session.setPendingEngineMove(future, session.positionVersion());
                owner = true;
            }
            position = session.position();
            fen = position.fen();
        } finally {
            session.lock().unlock();
        }
        if (owner) {
            compute(session, future, position, fen);
        }
        try {
            MoveRecord record = future.get();
            return new MoveResult(record, games.get(gameId), !owner);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new GameException(ErrorCode.INTERNAL_ERROR, "Die Anfrage wurde unterbrochen.");
        } catch (ExecutionException e) {
            Throwable cause = e.getCause() instanceof CompletionException ce && ce.getCause() != null ? ce.getCause() : e.getCause();
            if (cause instanceof GameException ge) {
                throw ge;
            }
            throw new GameException(ErrorCode.INTERNAL_ERROR, "Der Computerzug konnte nicht berechnet werden.");
        }
    }

    private void compute(GameSession session, CompletableFuture<MoveRecord> future, ChessPositionAdapter position, String fen) {
        int version;
        session.lock().lock();
        try {
            version = session.pendingEngineVersion();
        } finally {
            session.lock().unlock();
        }
        ComputerPlayer.Choice choice;
        try {
            choice = computer.choose(engines.play(), position, currentDifficulty(session));
        } catch (EngineTimeoutException e) {
            finish(session, future, null, new GameException(ErrorCode.ENGINE_TIMEOUT,
                    "Die Engine hat nicht rechtzeitig geantwortet. Bitte fordere den Computerzug erneut an."));
            return;
        } catch (EngineUnavailableException e) {
            finish(session, future, null, new GameException(ErrorCode.ENGINE_UNAVAILABLE,
                    "Die Schach-Engine ist nicht verfügbar. Computerzüge sind derzeit nicht möglich."));
            return;
        } catch (EngineProtocolException e) {
            finish(session, future, null, new GameException(ErrorCode.ENGINE_ERROR,
                    "Die Engine hat einen ungültigen Zug geliefert. Der Zug wurde nicht ausgeführt."));
            return;
        } catch (EngineException e) {
            finish(session, future, null, new GameException(ErrorCode.ENGINE_ERROR, "Die Engine hat einen Fehler gemeldet."));
            return;
        } catch (RuntimeException e) {
            finish(session, future, null, new GameException(ErrorCode.INTERNAL_ERROR, "Der Computerzug konnte nicht berechnet werden."));
            return;
        }
        session.lock().lock();
        try {
            if (session.positionVersion() != version || !session.position().fen().equals(fen) || session.status().isFinished()) {
                finish(session, future, null, new GameException(ErrorCode.POSITION_VERSION_CONFLICT,
                        "Die Partie wurde während der Berechnung verändert; der Computerzug wurde verworfen.", session.snapshot(true)));
                return;
            }
            if (!session.position().isLegal(choice.uci())) {
                finish(session, future, null, new GameException(ErrorCode.ENGINE_ERROR,
                        "Die Engine hat einen ungültigen Zug geliefert. Der Zug wurde nicht ausgeführt."));
                return;
            }
            MoveRecord record = applyMove(session, session.position(), choice.uci(), false, rules);
            finish(session, future, record, null);
        } finally {
            session.lock().unlock();
        }
    }

    private static de.chesscoach.domain.game.Difficulty currentDifficulty(GameSession session) {
        session.lock().lock();
        try {
            return session.difficulty();
        } finally {
            session.lock().unlock();
        }
    }

    private static void finish(GameSession session, CompletableFuture<MoveRecord> future, MoveRecord record, GameException error) {
        session.lock().lock();
        try {
            if (session.pendingEngineMove() == future) {
                session.clearPendingEngineMove();
            }
        } finally {
            session.lock().unlock();
        }
        if (error != null) {
            future.completeExceptionally(error);
        } else {
            future.complete(record);
        }
    }

    /** Hinweis für den Benutzer: bester Zug der Analyse-Engine für die aktuelle Stellung. */
    public record Hint(String uci, String san, String from, String pieceName, String score, int depth) {
    }

    public Hint hint(String gameId) {
        GameSnapshot snapshot = games.get(gameId);
        if (!snapshot.userToMove()) {
            throw new GameException(ErrorCode.NOT_YOUR_TURN, "Ein Tipp ist nur möglich, wenn du am Zug bist.", snapshot);
        }
        try {
            var move = engines.play().findBestMove(snapshot.fen(), new de.chesscoach.engine.EngineLimits(null, 60000L, null));
            ChessPositionAdapter position = rules.fromFen(snapshot.fen());
            if (!position.isLegal(move.uciMove())) {
                throw new GameException(ErrorCode.ENGINE_ERROR, "Die Engine hat einen ungültigen Zug geliefert.");
            }
            LegalMove legal = position.describe(move.uciMove());
            var piece = position.position().pieceAt(de.chesscoach.domain.position.Squares.parse(legal.from()));
            var score = move.analysis().score();
            return new Hint(legal.uci(), legal.san(), legal.from(), piece.kind().germanName(),
                    score == null ? null : score.display(), move.analysis().depth());
        } catch (EngineUnavailableException e) {
            throw new GameException(ErrorCode.ENGINE_UNAVAILABLE, "Die Schach-Engine ist nicht verfügbar.");
        } catch (EngineTimeoutException e) {
            throw new GameException(ErrorCode.ENGINE_TIMEOUT, "Die Engine hat nicht rechtzeitig geantwortet.");
        } catch (EngineException e) {
            throw new GameException(ErrorCode.ENGINE_ERROR, "Die Engine hat einen Fehler gemeldet.");
        }
    }
}
