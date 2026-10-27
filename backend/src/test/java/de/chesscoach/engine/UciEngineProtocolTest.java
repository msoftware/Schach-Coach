package de.chesscoach.engine;

import de.chesscoach.chess.ChessRulesService;
import de.chesscoach.domain.analysis.Score;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * UCI-Dialog mit einem echten Prozess (Test-Engine): uci, isready, position, go, stop, Parsen von info/bestmove,
 * Timeout, Prozessabbruch, Neustart nach Absturz und ungültige Ausgaben.
 */
class UciEngineProtocolTest {

    private static final String START = ChessRulesService.STANDARD_FEN;
    private final List<UciChessEngine> engines = new ArrayList<>();

    private UciChessEngine engine(String mode) {
        List<String> command = List.of(CalvinChessEngine.currentJava().toString(), "-cp", fakeClasspath(),
                FakeUciEngine.class.getName(), mode);
        UciChessEngine.Settings settings = new UciChessEngine.Settings("test", "0.0", Map.of("Hash", "8", "MultiPV", "3"),
                Duration.ofSeconds(10), Duration.ofMillis(800), Duration.ofSeconds(2));
        UciChessEngine e = new UciChessEngine(new UciProcessManager("fake", command, null, 4096), settings, new ChessRulesService());
        engines.add(e);
        return e;
    }

    /** Nur das Testklassenverzeichnis (die Test-Engine hat keine Abhängigkeiten; vermeidet zu lange Kommandozeilen). */
    static String fakeClasspath() {
        try {
            return Path.of(FakeUciEngine.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toString();
        } catch (java.net.URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    @AfterEach
    void stopAll() {
        engines.forEach(UciChessEngine::shutdown);
    }

    @Test
    void handshakeAndSearch() {
        UciChessEngine e = engine("normal");
        e.start();
        assertThat(e.getEngineInfo().name()).isEqualTo("FakeEngine");
        assertThat(e.getEngineInfo().state()).isEqualTo(EngineState.READY);
        assertThat(e.getEngineInfo().supportsMultiPv()).isFalse();
        EngineAnalysis a = e.analyze(START, AnalysisLimits.depth(3));
        assertThat(a.bestMove()).isEqualTo("e2e4");
        assertThat(a.score()).isEqualTo(Score.cp(21));
        assertThat(a.depth()).isEqualTo(3);
        assertThat(a.principalVariation()).containsExactly("e2e4", "e7e5", "g1f3");
        assertThat(a.completed()).isTrue();
        assertThat(a.nodes()).isEqualTo(500);
    }

    @Test
    void stopReturnsPartialResultWithinGrace() {
        UciChessEngine e = engine("hang");
        e.start();
        EngineAnalysis a = e.analyze(START, AnalysisLimits.movetime(200));
        assertThat(a.bestMove()).isEqualTo("e2e4");
        assertThat(a.completed()).as("Abbruch per stop ist kein reguläres Ende").isFalse();
    }

    @Test
    void explicitStopAnalysis() throws Exception {
        UciChessEngine e = engine("hang");
        e.start();
        Thread stopper = Thread.ofPlatform().start(() -> {
            try {
                Thread.sleep(300);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            e.stopAnalysis();
        });
        EngineAnalysis a = e.analyze(START, AnalysisLimits.nodes(1_000_000));
        stopper.join();
        assertThat(a.completed()).isFalse();
    }

    @Test
    void timeoutKillsAndRestartsEngine() {
        UciChessEngine e = engine("deadlock");
        e.start();
        assertThatThrownBy(() -> e.analyze(START, AnalysisLimits.movetime(100))).isInstanceOf(EngineTimeoutException.class);
        assertThat(e.restartCount()).isEqualTo(1);
        assertThat(e.getEngineInfo().state()).isEqualTo(EngineState.READY);
    }

    @Test
    void crashIsReportedAndEngineRestartsOnNextCall(@TempDir Path dir) {
        UciChessEngine e = engine("crash-once:" + dir.resolve("marker"));
        e.start();
        assertThatThrownBy(() -> e.analyze(START, AnalysisLimits.depth(3))).isInstanceOf(EngineUnavailableException.class);
        EngineAnalysis a = e.analyze(START, AnalysisLimits.depth(3));
        assertThat(a.bestMove()).isEqualTo("e2e4");
        assertThat(e.restartCount()).isEqualTo(1);
    }

    @Test
    void illegalBestMoveIsRejected() {
        UciChessEngine e = engine("illegal");
        e.start();
        assertThatThrownBy(() -> e.findBestMove(START, new EngineLimits(3, null, null)))
                .isInstanceOf(EngineProtocolException.class);
    }

    @Test
    void malformedBestMoveIsRejected() {
        UciChessEngine e = engine("garbage");
        e.start();
        assertThatThrownBy(() -> e.analyze(START, AnalysisLimits.depth(3))).isInstanceOf(EngineProtocolException.class);
    }

    @Test
    void missingEvaluationIsMarkedIncomplete() {
        UciChessEngine e = engine("noinfo");
        e.start();
        EngineAnalysis a = e.analyze(START, AnalysisLimits.depth(3));
        assertThat(a.score()).isNull();
        assertThat(a.completed()).isFalse();
    }

    @Test
    void missingUciOkFailsStart() {
        UciChessEngine e = new UciChessEngine(new UciProcessManager("fake", List.of(CalvinChessEngine.currentJava().toString(),
                "-cp", fakeClasspath(), FakeUciEngine.class.getName(), "silent"), null, 4096),
                new UciChessEngine.Settings("test", "0", Map.of(), Duration.ofMillis(1500), Duration.ofMillis(500), Duration.ofSeconds(2)),
                null);
        engines.add(e);
        assertThatThrownBy(e::start).isInstanceOf(EngineTimeoutException.class);
        assertThat(e.getEngineInfo().state()).isEqualTo(EngineState.FAILED);
    }

    @Test
    void nonExistingProgramIsUnavailable() {
        UciChessEngine e = new UciChessEngine(new UciProcessManager("missing", List.of("does-not-exist-engine-binary"), null, 4096),
                new UciChessEngine.Settings("test", "0", Map.of(), Duration.ofSeconds(1), Duration.ofMillis(500), Duration.ofSeconds(2)),
                null);
        engines.add(e);
        assertThatThrownBy(e::start).isInstanceOf(EngineUnavailableException.class);
        assertThat(e.getEngineInfo().available()).isFalse();
    }

    @Test
    void terminalPositionsAreNeverSentToEngine() {
        UciChessEngine e = engine("normal");
        e.start();
        String mated = "rnb1kbnr/pppp1ppp/8/4p3/6Pq/5P2/PPPPP2P/RNBQKBNR w KQkq - 1 3";
        assertThatThrownBy(() -> e.analyze(mated, AnalysisLimits.depth(2))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void commandsMayNotContainLineBreaks() {
        UciProcessManager manager = new UciProcessManager("x", List.of("java"), null, 100);
        assertThatThrownBy(() -> manager.send("position fen x\ngo")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void unavailableEngineNeverSimulates() {
        UnavailableEngine e = new UnavailableEngine("play", "6.1.1", "Datei fehlt");
        assertThatThrownBy(() -> e.findBestMove(START, new EngineLimits(1, null, null))).isInstanceOf(EngineUnavailableException.class);
        assertThat(e.getEngineInfo().available()).isFalse();
    }
}
