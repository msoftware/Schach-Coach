package de.chesscoach.engine;

import de.chesscoach.chess.ChessRulesService;
import de.chesscoach.domain.analysis.Score;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Integration mit der mitgelieferten Calvin-Engine (echter Prozess, keine Simulation). */
@Tag("engine")
class CalvinEngineIT {

    static CalvinChessEngine engine;
    static final ChessRulesService RULES = new ChessRulesService();

    static Path jar() {
        return Path.of(System.getProperty("chesscoach.engine.calvin.jar-path", "engine/calvin-chess-engine-6.1.1.jar"));
    }

    static CalvinChessEngine create(String role) {
        var settings = new UciChessEngine.Settings(role, "6.1.1", Map.of("Hash", "16", "Threads", "1"), Duration.ofSeconds(30),
                Duration.ofSeconds(3), Duration.ofSeconds(30));
        return new CalvinChessEngine(new UciProcessManager("calvin-" + role,
                CalvinChessEngine.command(CalvinChessEngine.currentJava(), jar(), 512), null, 8192), settings, RULES);
    }

    @BeforeAll
    static void start() {
        engine = create("it");
        engine.start();
    }

    @AfterAll
    static void stop() {
        engine.shutdown();
    }

    @Test
    void handshakeIdentifiesCalvin() {
        EngineInfo info = engine.getEngineInfo();
        assertThat(info.name()).isEqualTo("Calvin");
        assertThat(info.author()).isEqualTo("Dan Kelsey");
        assertThat(info.version()).isEqualTo("6.1.1");
        assertThat(info.supportsMultiPv()).isFalse();
    }

    @Test
    void bestMoveFromStartIsLegal() {
        EngineMove move = engine.findBestMove(ChessRulesService.STANDARD_FEN, new EngineLimits(null, 20_000L, null));
        assertThat(RULES.fromFen(ChessRulesService.STANDARD_FEN).isLegal(move.uciMove())).isTrue();
        assertThat(move.analysis().score()).isNotNull();
        assertThat(move.analysis().completed()).isTrue();
    }

    @Test
    void depthLimitIsTranslatedForCalvin() {
        EngineAnalysis a = engine.analyze(ChessRulesService.STANDARD_FEN, AnalysisLimits.depth(6));
        assertThat(a.depth()).isEqualTo(6);
    }

    @Test
    void findsMateInOneAndReportsMateScore() {
        String fen = "6k1/5ppp/8/8/8/8/5PPP/R5K1 w - - 0 1";
        EngineAnalysis a = engine.analyze(fen, AnalysisLimits.depth(4));
        assertThat(a.bestMove()).isEqualTo("a1a8");
        assertThat(a.score()).isEqualTo(Score.mate(1));
    }

    @Test
    void scoreIsFromSideToMove() {
        // Schwarz hat eine Dame mehr; Schwarz ist am Zug -> positive Bewertung.
        EngineAnalysis a = engine.analyze("4k3/8/8/8/3q4/8/8/4K3 b - - 0 1", AnalysisLimits.nodes(20_000));
        assertThat(a.score().centipawnsClamped(3000)).isGreaterThan(500);
    }

    @Test
    void stopAnalysisInterruptsLongSearch() throws Exception {
        Thread stopper = Thread.ofPlatform().start(() -> {
            try {
                Thread.sleep(400);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            engine.stopAnalysis();
        });
        long start = System.currentTimeMillis();
        EngineAnalysis a = engine.analyze(ChessRulesService.STANDARD_FEN, AnalysisLimits.movetime(10_000));
        stopper.join();
        assertThat(System.currentTimeMillis() - start).isLessThan(5_000);
        assertThat(a.completed()).isFalse();
        assertThat(a.bestMove()).isNotNull();
    }
}
