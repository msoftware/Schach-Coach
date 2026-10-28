package de.chesscoach.application;

import de.chesscoach.application.analysis.CancellationToken;
import de.chesscoach.application.analysis.MoveAnalyzer;
import de.chesscoach.application.game.ComputerPlayer;
import de.chesscoach.chess.ChessRulesService;
import de.chesscoach.coaching.CoachingProperties;
import de.chesscoach.coaching.ExplanationBuilder;
import de.chesscoach.coaching.ExplanationQualityTestAccess;
import de.chesscoach.coaching.MistakeClassifier;
import de.chesscoach.domain.analysis.MoveAnalysis;
import de.chesscoach.domain.coaching.MoveClassification;
import de.chesscoach.domain.game.AnalysisProfile;
import de.chesscoach.domain.game.Difficulty;
import de.chesscoach.engine.CalvinChessEngine;
import de.chesscoach.engine.CalvinEngineITAccess;
import de.chesscoach.infrastructure.config.AnalysisProperties;
import de.chesscoach.infrastructure.config.DifficultyProperties;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Mit der echten Engine: Abstufung der Spielstärken (FR-03) und Analyse-Pipeline inklusive Coaching (FR-04).
 */
@Tag("engine")
class DifficultyAndAnalysisIT {

    static CalvinChessEngine engine;
    static final ChessRulesService RULES = new ChessRulesService();

    /** Stellungen mit eindeutigem Gewinnzug (Matt oder Gewinn einer Figur/Dame). */
    static final List<String[]> TACTICS = List.of(
            new String[]{"6k1/5ppp/8/8/8/8/5PPP/R5K1 w - - 0 1", "a1a8"},
            new String[]{"r1bqkb1r/pppp1ppp/2n2n2/4p2Q/2B1P3/8/PPPP1PPP/RNB1K1NR w KQkq - 4 4", "h5f7"},
            new String[]{"rnb1kbnr/pppp1ppp/8/4p1q1/3P4/2N5/PPP1PPPP/R1BQKBNR w KQkq - 0 3", "c1g5"},
            new String[]{"r3k3/8/8/1N6/8/8/8/4K3 w - - 0 1", "b5c7"},
            new String[]{"4k3/8/8/8/8/8/r7/R3K3 w - - 0 1", "a1a2"},
            new String[]{"4k3/8/8/3q4/8/8/8/3QK3 w - - 0 1", "d1d5"});

    @BeforeAll
    static void start() {
        engine = CalvinEngineITAccess.create("difficulty");
        engine.start();
    }

    @AfterAll
    static void stop() {
        engine.shutdown();
    }

    @Test
    void difficultyLevelsAreGenuinelyGraded() {
        DifficultyProperties props = new DifficultyProperties(null, 42L);
        ComputerPlayer player = new ComputerPlayer(props);
        Map<Difficulty, Integer> solved = new EnumMap<>(Difficulty.class);
        for (Difficulty d : List.of(Difficulty.BEGINNER, Difficulty.EASY, Difficulty.MEDIUM, Difficulty.STRONG)) {
            int count = 0;
            int rounds = d == Difficulty.BEGINNER || d == Difficulty.EASY ? 3 : 1;
            for (int round = 0; round < rounds; round++) {
                for (String[] t : TACTICS) {
                    ComputerPlayer.Choice c = player.choose(engine, RULES.fromFen(t[0]), d);
                    assertThat(RULES.fromFen(t[0]).isLegal(c.uci())).isTrue();
                    if (c.uci().equals(t[1])) {
                        count++;
                    }
                }
            }
            solved.put(d, count * 3 / rounds);
        }
        System.out.println("Gelöste Taktikaufgaben (normiert auf 3 Durchläufe à " + TACTICS.size() + "): " + solved);
        assertThat(solved.get(Difficulty.STRONG)).isEqualTo(3 * TACTICS.size());
        assertThat(solved.get(Difficulty.MEDIUM)).isEqualTo(3 * TACTICS.size());
        assertThat(solved.get(Difficulty.BEGINNER)).isLessThan(solved.get(Difficulty.MEDIUM));
        assertThat(solved.get(Difficulty.BEGINNER)).isLessThanOrEqualTo(solved.get(Difficulty.EASY));
        // Die Parameter unterscheiden sich tatsächlich (keine bloßen Bezeichnungen).
        assertThat(DifficultyProperties.defaults().values()).doesNotHaveDuplicates();
    }

    @Test
    void analysisOfBlunderAndGoodMove() {
        MoveAnalyzer analyzer = new MoveAnalyzer(RULES, ExplanationQualityTestAccess.tactical(), ExplanationQualityTestAccess.strategic(),
                new MistakeClassifier(CoachingProperties.defaults()), new ExplanationBuilder(),
                new AnalysisProperties(60_000, 2000, 40_000, 3, 2.0, 3.0, 9, 12, 8, 200, 40_000, 100, 4.0), CoachingProperties.defaults());
        String fen = "r1bqkbnr/pppp1ppp/2n5/4p3/4P3/5N2/PPPP1PPP/RNBQKB1R w KQkq - 2 3";
        long start = System.currentTimeMillis();
        MoveAnalysis blunder = analyzer.analyze(engine, new MoveAnalyzer.Request(fen, "f3e5", null, null, AnalysisProfile.NORMAL, 1),
                CancellationToken.none(), (s, p) -> { });
        long elapsed = System.currentTimeMillis() - start;
        System.out.println("Analysezeit (Nxe5): " + elapsed + " ms, Knoten " + blunder.engineNodes());
        assertThat(blunder.classification()).isIn(MoveClassification.MISTAKE, MoveClassification.BLUNDER);
        assertThat(blunder.findings()).anyMatch(f -> f.ruleId().equals("TACT-002") || f.ruleId().equals("TACT-003"));
        assertThat(blunder.candidates().size()).isGreaterThanOrEqualTo(3);
        assertThat(blunder.bestMove().uci()).isNotEqualTo("f3e5");
        blunder.candidates().forEach(c -> {
            // Jede Variante besteht nur aus legalen Zügen.
            var validated = RULES.validateLine(fen, c.line().stream().map(v -> v.uci()).toList(), 40);
            assertThat(validated.truncated()).isFalse();
            assertThat(c.searchLine()).isTrue();
        });
        assertThat(blunder.sections()).hasSize(7);

        MoveAnalysis good = analyzer.analyze(engine, new MoveAnalyzer.Request(fen, "f1b5", null, null, AnalysisProfile.NORMAL, 1),
                CancellationToken.none(), (s, p) -> { });
        assertThat(good.classification().isGood()).isTrue();
        assertThat(good.mainReason()).isNotBlank();
    }

    /**
     * Gefunden beim Probespielen: Mit 80 000 Knoten je Kandidat hielt der Coach 6.Bb3 für besser als die gespielte
     * Rochade und stufte diese nur als „Gut“ ein. Tiefer gerechnet scheitert 6.Bb3 an 6...Ng4 (Angriff auf h2/f2);
     * die Rochade ist der stärkste Zug. Die Nachprüfung knapper Entscheidungen muss das erkennen.
     */
    @Test
    void closeCallIsVerifiedBeforeRecommendingAnotherMove() {
        MoveAnalyzer analyzer = new MoveAnalyzer(RULES, ExplanationQualityTestAccess.tactical(), ExplanationQualityTestAccess.strategic(),
                new MistakeClassifier(CoachingProperties.defaults()), new ExplanationBuilder(),
                new AnalysisProperties(150_000, 3000, 80_000, 4, 2.0, 3.0, 9, 12, 8, 200, 40_000, 100, 4.0), CoachingProperties.defaults());
        String fen = "r1bqk2r/pppp1ppp/5n2/2b1n3/2B1P3/3P4/PPP2PPP/RNBQK2R w KQkq - 0 6";
        MoveAnalysis castle = analyzer.analyze(engine, new MoveAnalyzer.Request(fen, "e1g1", null, null, AnalysisProfile.NORMAL, 1),
                CancellationToken.none(), (s, p) -> { });
        assertThat(castle.bestMove().uci()).isNotEqualTo("c4b3");
        assertThat(castle.classification()).isIn(MoveClassification.BEST, MoveClassification.EXCELLENT);
    }

    /**
     * Gefunden beim Probespielen: Nach 9...Ng4 empfahl der Coach „etwas genauer wäre 10.Qe1“. Tiefer gerechnet ist
     * 10.h3 der beste Zug, 10.Qe1 scheitert an 10...Qh4. Die nur flach bewertete Alternative darf den Vergleich
     * gegen den nachgeprüften gespielten Zug nicht gewinnen.
     */
    @Test
    void recommendedMoveIsVerifiedUnderTheSameConditionsAsThePlayedMove() {
        MoveAnalyzer analyzer = new MoveAnalyzer(RULES, ExplanationQualityTestAccess.tactical(), ExplanationQualityTestAccess.strategic(),
                new MistakeClassifier(CoachingProperties.defaults()), new ExplanationBuilder(),
                new AnalysisProperties(150_000, 3000, 80_000, 4, 2.0, 3.0, 9, 12, 8, 200, 40_000, 100, 4.0), CoachingProperties.defaults());
        String fen = "r1bq1rk1/ppp2ppp/3p4/2b5/2P1P1n1/P1N5/1PP2PPP/R1BQ1RK1 w - - 1 10";
        MoveAnalysis h3 = analyzer.analyze(engine, new MoveAnalyzer.Request(fen, "h2h3", null, null, AnalysisProfile.NORMAL, 1),
                CancellationToken.none(), (s, p) -> { });
        assertThat(h3.bestMove().uci()).isNotEqualTo("d1e1");
        assertThat(h3.classification()).isIn(MoveClassification.BEST, MoveClassification.EXCELLENT);
    }

    @Test
    void analysisCanBeCancelled() {
        MoveAnalyzer analyzer = new MoveAnalyzer(RULES, ExplanationQualityTestAccess.tactical(), ExplanationQualityTestAccess.strategic(),
                new MistakeClassifier(CoachingProperties.defaults()), new ExplanationBuilder(),
                new AnalysisProperties(60_000, 2000, 40_000, 3, 2.0, 3.0, 9, 12, 8, 200, 40_000, 100, 4.0), CoachingProperties.defaults());
        CancellationToken token = new CancellationToken();
        token.cancel();
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> analyzer.analyze(engine,
                        new MoveAnalyzer.Request(ChessRulesService.STANDARD_FEN, "e2e4", null, null, AnalysisProfile.FAST, 1), token, (s, p) -> { }))
                .isInstanceOf(java.util.concurrent.CancellationException.class);
    }
}
