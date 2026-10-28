package de.chesscoach.coaching.tactical;

import de.chesscoach.coaching.Contexts;
import de.chesscoach.coaching.PositionContext;
import de.chesscoach.coaching.TacticalRule;
import de.chesscoach.domain.analysis.Score;
import de.chesscoach.domain.coaching.Certainty;
import de.chesscoach.domain.coaching.CoachingFinding;
import de.chesscoach.domain.coaching.Polarity;
import de.chesscoach.domain.coaching.Severity;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Jede taktische Regel (TACT-001 bis TACT-016) mit kuratierten Teststellungen: erwartete Erkennung, betroffene
 * Felder, Beweiskette sowie Fälle, in denen die Regel ausdrücklich nicht anschlagen darf.
 */
class TacticalRulesTest {

    private static List<CoachingFinding> run(TacticalRule rule, PositionContext c) {
        return rule.matches(c) ? rule.evaluate(c) : List.of();
    }

    private static final String AFTER_NC6 = "r1bqkbnr/pppp1ppp/2n5/4p3/4P3/5N2/PPPP1PPP/RNBQKB1R w KQkq - 2 3";
    private static final String KNIGHT_TAKES_E4 = "rnbqkb1r/pppppppp/8/8/4n3/2N5/PPPP1PPP/R1BQKBNR w KQkq - 0 3";

    @Nested
    class Tact001MaterialGain {
        private final MaterialGainRule rule = new MaterialGainRule();

        @Test
        void recognisesVerifiedGain() {
            var c = Contexts.of(KNIGHT_TAKES_E4, "c3e4", "d7d5", "e4g3").cp(-80, 220, 220).build();
            List<CoachingFinding> f = run(rule, c);
            assertThat(f).hasSize(1);
            assertThat(f.get(0).polarity()).isEqualTo(Polarity.POSITIVE);
            assertThat(f.get(0).certainty()).isEqualTo(Certainty.VERIFIED);
            assertThat(f.get(0).evidence().materialBefore()).isEqualTo(-1);
            assertThat(f.get(0).evidence().materialAfter()).isEqualTo(2);
            assertThat(f.get(0).affectedSquares()).contains("e4");
        }

        @Test
        void recognisesMissedGain() {
            var c = Contexts.of(KNIGHT_TAKES_E4, "d2d3", "e4c3", "b2c3").cp(-80, -90, 220)
                    .best("c3e4", "d7d5", "e4g3").build();
            List<CoachingFinding> f = run(rule, c);
            assertThat(f).extracting(CoachingFinding::title).contains("Materialgewinn verpasst");
            assertThat(f).noneMatch(x -> x.polarity() == Polarity.POSITIVE);
        }

        /**
         * Gefunden beim Probespielen: Nach 6...Nxc4 lobte der Coach 7.dxc4 mit „du gewinnst so viel wie eine Figur“.
         * Zurückschlagen stellt nur das Material wieder her – es ist kein Gewinn.
         */
        @Test
        void recaptureIsNotPraisedAsMaterialGain() {
            String beforeOpponent = "r1bqk2r/pppp1ppp/5n2/2b1n3/2B1P3/3P4/PPP2PPP/RNBQ1RK1 b kq - 1 6";
            String afterOpponent = "r1bqk2r/pppp1ppp/5n2/2b5/2n1P3/3P4/PPP2PPP/RNBQ1RK1 w kq - 0 7";
            var c = Contexts.of(afterOpponent, "d3c4", "e8g8", "b1c3").cp(-320, -320, -320)
                    .previousOpponent(beforeOpponent, "e5c4").build();
            List<CoachingFinding> f = run(rule, c);
            assertThat(f).extracting(CoachingFinding::title).containsExactly("Zurückgeschlagen");
            assertThat(f.get(0).explanation())
                    .contains("Der Gegner hat gerade einen Läufer von dir geschlagen, jetzt nimmst du einen Springer")
                    .contains("gewonnen hast du dadurch aber nichts")
                    .doesNotContain("gewinnst du");
        }

        @Test
        void recaptureHintDoesNotDependOnLaterMaterialInTheLine() {
            // Dieselbe Rückeroberung, aber die Engine-Variante gewinnt später scheinbar weiteres Material (4...Nxe4? 5.Qd5),
            // das die Bewertung nicht bestätigt: Der Hinweis „Zurückgeschlagen“ muss trotzdem erscheinen.
            String beforeOpponent = "r1bqk2r/pppp1ppp/5n2/2b1n3/2B1P3/3P4/PPP2PPP/RNBQ1RK1 b kq - 1 6";
            String afterOpponent = "r1bqk2r/pppp1ppp/5n2/2b5/2n1P3/3P4/PPP2PPP/RNBQ1RK1 w kq - 0 7";
            var c = Contexts.of(afterOpponent, "d3c4", "f6e4", "d1d5", "e4f2", "f1f2", "c5f2", "g1f2").cp(-320, -320, -320)
                    .previousOpponent(beforeOpponent, "e5c4").build();
            assertThat(run(rule, c)).extracting(CoachingFinding::title).contains("Zurückgeschlagen");
        }

        @Test
        void takingMoreThanWasLostWithoutNetGainIsExplainedHonestly() {
            // Gefunden beim Probespielen: 10...Nxf2 (Bauer) 11.Rxf2 (Springer); danach holt Schwarz mit ...Qh4/...Bxf2+
            // das Material zurück. „Damit holst du dir das Material zurück“ passte nicht zu „Springer für Bauer“.
            String beforeOpponent = "r1bq1rk1/ppp2ppp/3p4/2b5/2P1P1n1/P1N4P/1PP2PP1/R1BQ1RK1 b - - 0 10";
            String afterOpponent = "r1bq1rk1/ppp2ppp/3p4/2b5/2P1P3/P1N4P/1PP2nP1/R1BQ1RK1 w - - 0 11";
            var c = Contexts.of(afterOpponent, "f1f2", "d8h4", "d1f3", "c5f2", "f3f2", "h4f2", "g1f2").cp(-380, -400, -400)
                    .previousOpponent(beforeOpponent, "g4f2").build();
            List<CoachingFinding> f = run(rule, c);
            assertThat(f).extracting(CoachingFinding::title).containsExactly("Zurückgeschlagen");
            assertThat(f.get(0).explanation()).contains("einen Bauern von dir geschlagen, jetzt nimmst du einen Springer")
                    .contains("sogar mehr, als du verloren hast").doesNotContain("holst du dir das Material zurück");
        }

        @Test
        void captureOnAnotherSquareIsNoRecapture() {
            String beforeOpponent = "r1bqk2r/pppp1ppp/5n2/2b1n3/2B1P3/3P4/PPP2PPP/RNBQ1RK1 b kq - 1 6";
            String afterOpponent = "r1bqk2r/pppp1ppp/5n2/2b5/2n1P3/3P4/PPP2PPP/RNBQ1RK1 w kq - 0 7";
            // Statt zurückzuschlagen nimmt Weiß nichts auf c4 – kein Rückschlag-Hinweis.
            var c = Contexts.of(afterOpponent, "b1c3", "c4b2").cp(-320, -420, -320)
                    .previousOpponent(beforeOpponent, "e5c4").build();
            assertThat(run(rule, c)).extracting(CoachingFinding::title).doesNotContain("Zurückgeschlagen");
        }

        @Test
        void winningMoreThanWasJustLostIsStillAGain() {
            // 2...Nxe4 schlägt einen Bauern, 3.Nxe4 gewinnt dafür einen Springer: Netto bleibt ein Gewinn.
            String beforeOpponent = "rnbqkb1r/pppppppp/5n2/8/4P3/2N5/PPPP1PPP/R1BQKBNR b KQkq - 2 2";
            var c = Contexts.of(KNIGHT_TAKES_E4, "c3e4", "d7d5", "e4g3").cp(-80, 220, 220)
                    .previousOpponent(beforeOpponent, "f6e4").build();
            List<CoachingFinding> f = run(rule, c);
            assertThat(f).hasSize(1);
            assertThat(f.get(0).explanation()).contains("gewinnst du so viel wie zwei Bauern");
        }

        @Test
        void doesNotClaimGainWhenLineDoesNotConfirmIt() {
            var c = Contexts.of(AFTER_NC6, "f3e5", "c6e5").cp(24, -229, 23).build();
            assertThat(run(rule, c)).isEmpty();
        }
    }

    @Nested
    class Tact002MaterialLoss {
        private final MaterialLossRule rule = new MaterialLossRule();

        /**
         * Gefunden beim Probespielen: Nach 11...Qh4 war der Turm f2 nicht mehr zu retten. Der Coach nannte 12.Qf3
         * „ein Opfer mit Ausgleich“ – richtig ist: Der Verlust war unvermeidbar, der Zug begrenzt den Schaden.
         */
        @Test
        void unavoidableLossIsNotCalledASacrifice() {
            String fen = "r1b2rk1/ppp2ppp/3p4/2b5/2P1P2q/P1N4P/1PP2RP1/R1BQ2K1 w - - 1 12";
            var c = Contexts.of(fen, "d1f3", "h4f2", "f3f2", "c5f2", "g1f2", "c8e6", "b2b3", "f7f5", "c1f4", "f5e4")
                    .cp(-418, -419, -419).best("d1e1", "c5f2", "e1f2", "h4f2", "g1f2").build();
            List<CoachingFinding> f = run(rule, c);
            assertThat(f).extracting(CoachingFinding::title).containsExactly("Verlust nicht mehr zu verhindern");
            assertThat(f.get(0).explanation()).contains("nicht mehr zu verhindern").doesNotContain("Opfer");
        }

        @Test
        void realSacrificeWithCompensationIsStillRecognised() {
            // Material war vorher ausgeglichen, die Bewertung bleibt nach dem Figurenopfer stabil: echtes Opfer.
            var c = Contexts.of(AFTER_NC6, "f3e5", "c6e5").cp(30, 20, 30).best("d2d4", "e5d4", "f3d4").build();
            List<CoachingFinding> f = run(rule, c);
            assertThat(f).extracting(CoachingFinding::title).containsExactly("Materialopfer mit Kompensation");
        }

        @Test
        void recognisesAvoidableLoss() {
            var c = Contexts.of(AFTER_NC6, "f3e5", "c6e5", "b1c3", "g8f6").cp(24, -229, 23)
                    .best("d2d4", "e5d4", "f3d4").build();
            List<CoachingFinding> f = run(rule, c);
            assertThat(f).hasSize(1);
            CoachingFinding loss = f.get(0);
            assertThat(loss.polarity()).isEqualTo(Polarity.NEGATIVE);
            assertThat(loss.certainty()).isEqualTo(Certainty.VERIFIED);
            assertThat(loss.evidence().materialAfter()).isEqualTo(-2);
            assertThat(loss.affectedSquares()).contains("e5");
            assertThat(loss.explanation()).contains("3...Nxe5");
        }

        @Test
        void gambitWithCompensationIsNotAnError() {
            String fen = "rnbqkbnr/pppp1ppp/8/4p3/4P3/8/PPPP1PPP/RNBQKBNR w KQkq - 0 2";
            var c = Contexts.of(fen, "f2f4", "e5f4", "g1f3", "g7g5").cp(30, 5, 30).best("g1f3", "b8c6").build();
            List<CoachingFinding> f = run(rule, c);
            assertThat(f).hasSize(1);
            assertThat(f.get(0).polarity()).isEqualTo(Polarity.NEUTRAL);
            assertThat(f.get(0).title()).contains("Kompensation");
        }

        @Test
        void bestMoveLosingMaterialIsNotReportedAsAvoidable() {
            var c = Contexts.of(AFTER_NC6, "f3e5", "c6e5").cp(24, -229, -229).best("f3e5", "c6e5").build();
            assertThat(run(rule, c)).isEmpty();
        }
    }

    @Nested
    class Tact003HangingPieces {
        private final HangingPiecesRule rule = new HangingPiecesRule();

        @Test
        void verifiedWhenEngineCaptures() {
            var c = Contexts.of(AFTER_NC6, "f3e5", "c6e5").build();
            List<CoachingFinding> f = run(rule, c);
            assertThat(f).hasSize(1);
            assertThat(f.get(0).certainty()).isEqualTo(Certainty.VERIFIED);
            assertThat(f.get(0).affectedPieces()).containsExactly("weißer Springer e5");
        }

        @Test
        void heuristicWithoutConfirmingLine() {
            var c = Contexts.of(AFTER_NC6, "f3e5").build();
            List<CoachingFinding> f = run(rule, c);
            assertThat(f).hasSize(1);
            assertThat(f.get(0).certainty()).isEqualTo(Certainty.HEURISTIC);
            assertThat(f.get(0).explanation()).contains("nicht sofort");
        }

        @Test
        void quietDevelopingMoveHasNoHangingPiece() {
            var c = Contexts.of(AFTER_NC6, "f1b5", "a7a6").build();
            assertThat(run(rule, c)).isEmpty();
        }
    }

    @Nested
    class Tact004PieceAttack {
        private final PieceAttackRule rule = new PieceAttackRule();

        @Test
        void newAttackOnUndefendedPiece() {
            var c = Contexts.of("4k3/8/8/3n4/8/8/8/R3K3 w Q - 0 1", "a1a5", "e8d7", "a5d5").build();
            List<CoachingFinding> f = run(rule, c);
            assertThat(f).hasSize(1);
            assertThat(f.get(0).polarity()).isEqualTo(Polarity.POSITIVE);
            assertThat(f.get(0).certainty()).isEqualTo(Certainty.VERIFIED);
            assertThat(f.get(0).affectedSquares()).contains("a5", "d5");
        }

        @Test
        void ignoredAttack() {
            var c = Contexts.of("4k3/8/8/8/8/2p5/3N4/4K3 w - - 0 1", "e1f1", "c3d2").build();
            List<CoachingFinding> f = run(rule, c);
            assertThat(f).extracting(CoachingFinding::title).containsExactly("Angriff ignoriert");
            assertThat(f.get(0).certainty()).isEqualTo(Certainty.VERIFIED);
        }

        @Test
        void movingTheAttackedPieceAwayIsNotIgnoring() {
            var c = Contexts.of("4k3/8/8/8/8/2p5/3N4/4K3 w - - 0 1", "d2e4").build();
            assertThat(run(rule, c)).noneMatch(x -> x.title().equals("Angriff ignoriert"));
        }
    }

    @Nested
    class Tact005DoubleAttack {
        private final DoubleAttackRule rule = new DoubleAttackRule();

        @Test
        void queenAttacksKingAndRook() {
            var c = Contexts.of("r3k3/8/8/8/8/8/8/3QK3 w - - 0 1", "d1a4", "e8f7", "a4a8").build();
            List<CoachingFinding> f = run(rule, c);
            assertThat(f).hasSize(1);
            assertThat(f.get(0).polarity()).isEqualTo(Polarity.POSITIVE);
            assertThat(f.get(0).certainty()).isEqualTo(Certainty.VERIFIED);
            assertThat(f.get(0).affectedSquares()).contains("a8", "e8");
        }

        @Test
        void opponentDoubleAttackAfterQuietMove() {
            var c = Contexts.of("3qk3/8/8/8/8/8/7P/R3K3 w Q - 0 1", "h2h3", "d8a5", "e1f2", "a5a1").build();
            List<CoachingFinding> f = run(rule, c);
            assertThat(f).extracting(CoachingFinding::title).containsExactly("Gegnerischer Doppelangriff");
            assertThat(f.get(0).severity()).isEqualTo(Severity.HIGH);
        }

        @Test
        void knightForkIsNotCountedAsDoubleAttack() {
            var c = Contexts.of("r3k3/8/8/1N6/8/8/8/4K3 w - - 0 1", "b5c7").build();
            assertThat(run(rule, c)).isEmpty();
        }
    }

    @Nested
    class Tact006Pin {
        private final PinRule rule = new PinRule();

        @Test
        void absolutePinIsVerifiedByLegalMoves() {
            var c = Contexts.of("4k3/8/2n5/8/8/8/8/4KB2 w - - 0 1", "f1b5").build();
            List<CoachingFinding> f = run(rule, c);
            assertThat(f).hasSize(1);
            assertThat(f.get(0).certainty()).isEqualTo(Certainty.VERIFIED);
            assertThat(f.get(0).explanation()).contains("den gegnerischen Springer c6").contains("König");
        }

        @Test
        void walkingIntoPin() {
            var c = Contexts.of("4k3/8/8/8/1b6/8/3N4/3K4 w - - 0 1", "d1e1").build();
            List<CoachingFinding> f = run(rule, c);
            assertThat(f).extracting(CoachingFinding::polarity).containsExactly(Polarity.NEGATIVE);
            assertThat(f.get(0).affectedSquares()).contains("d2", "b4", "e1");
        }

        @Test
        void noPinWithoutLine() {
            var c = Contexts.of("4k3/8/2n5/8/8/8/8/4KB2 w - - 0 1", "f1d3").build();
            assertThat(run(rule, c)).isEmpty();
        }
    }

    @Nested
    class Tact007Skewer {
        private final SkewerRule rule = new SkewerRule();

        @Test
        void rookSkewersKingAndQueen() {
            var c = Contexts.of("4q3/8/8/4k3/8/8/8/R5K1 w - - 0 1", "a1e1", "e5d6", "e1e8").build();
            List<CoachingFinding> f = run(rule, c);
            assertThat(f).hasSize(1);
            assertThat(f.get(0).certainty()).isEqualTo(Certainty.VERIFIED);
            assertThat(f.get(0).affectedSquares()).contains("e1", "e5", "e8");
        }

        @Test
        void noSkewerWhenNothingBehind() {
            var c = Contexts.of("8/8/8/4k3/8/8/8/R5K1 w - - 0 1", "a1e1").build();
            assertThat(run(rule, c)).isEmpty();
        }
    }

    @Nested
    class Tact008DiscoveredAttack {
        private final DiscoveredAttackRule rule = new DiscoveredAttackRule();

        @Test
        void discoveredCheck() {
            var c = Contexts.of("4k3/8/8/8/4N3/8/8/4RK2 w - - 0 1", "e4c5").build();
            List<CoachingFinding> f = run(rule, c);
            assertThat(f).extracting(CoachingFinding::title).containsExactly("Abzugsschach");
            assertThat(f.get(0).affectedSquares()).contains("e1", "e8", "e4");
        }

        @Test
        void noDiscoveryWhenLineStaysClosed() {
            var c = Contexts.of("4k3/8/8/8/4N3/8/8/4RK2 w - - 0 1", "f1g2").build();
            assertThat(run(rule, c)).isEmpty();
        }
    }

    @Nested
    class Tact009Intermezzo {
        private final IntermezzoRule rule = new IntermezzoRule();
        private static final String BEFORE_CAPTURE = "4k3/8/1b6/8/3N3Q/8/8/3RK3 b - - 0 1";
        private static final String AFTER_CAPTURE = "4k3/8/8/8/3b3Q/8/8/3RK3 w - - 0 1";

        @Test
        void checkInsteadOfRecapture() {
            var c = Contexts.of(AFTER_CAPTURE, "h4e4", "e8d7", "d1d4").cp(0, 0, 0)
                    .previousOpponent(BEFORE_CAPTURE, "b6d4").build();
            assertThat(rule.matches(c)).isTrue();
            List<CoachingFinding> f = rule.evaluate(c);
            assertThat(f).extracting(CoachingFinding::polarity).containsExactly(Polarity.POSITIVE);
        }

        @Test
        void missedIntermezzo() {
            var c = Contexts.of(AFTER_CAPTURE, "d1d4").cp(0, -100, 50).best("h4e4", "e8d7", "d1d4")
                    .previousOpponent(BEFORE_CAPTURE, "b6d4").build();
            assertThat(rule.evaluate(c)).extracting(CoachingFinding::title).containsExactly("Zwischenzug verpasst");
        }

        @Test
        void notApplicableWithoutPreviousCapture() {
            var c = Contexts.of(AFTER_CAPTURE, "h4e4").build();
            assertThat(rule.matches(c)).isFalse();
        }
    }

    @Nested
    class Tact010MateThreat {
        private final MateThreatRule rule = new MateThreatRule();

        @Test
        void checkmate() {
            var c = Contexts.of("6k1/5ppp/8/8/8/8/8/R5K1 w - - 0 1", "a1a8").build();
            assertThat(run(rule, c)).extracting(CoachingFinding::title).containsExactly("Schachmatt");
        }

        @Test
        void quietMoveCreatesMateThreat() {
            var c = Contexts.of("6k1/5ppp/8/8/3B4/8/5PPP/3R2K1 w - - 0 1", "d4e3").build();
            List<CoachingFinding> f = run(rule, c);
            assertThat(f).hasSize(1);
            assertThat(f.get(0).explanation()).contains("Rd8#");
            assertThat(f.get(0).certainty()).isEqualTo(Certainty.VERIFIED);
        }

        @Test
        void missedMate() {
            var c = Contexts.of("6k1/5ppp/8/8/8/8/5PPP/R5K1 w - - 0 1", "g1f1")
                    .scores(Score.mate(1), Score.cp(500), Score.mate(1)).best("a1a8").build();
            assertThat(run(rule, c)).extracting(CoachingFinding::title).contains("Matt verpasst");
        }

        @Test
        void noThreatInQuietPosition() {
            var c = Contexts.of(AFTER_NC6, "f1c4").build();
            assertThat(run(rule, c)).isEmpty();
        }
    }

    @Nested
    class Tact011MateDefense {
        private final MateDefenseRule rule = new MateDefenseRule();

        @Test
        void allowsMateInOne() {
            var c = Contexts.of("6k1/8/5q2/8/8/5N2/6PP/7K w - - 0 1", "f3e5").build();
            List<CoachingFinding> f = run(rule, c);
            assertThat(f).extracting(CoachingFinding::severity).containsExactly(Severity.CRITICAL);
            assertThat(f.get(0).explanation()).contains("Qf1#");
        }

        @Test
        void parriesBackRankMateThreat() {
            var c = Contexts.of("r5k1/8/8/8/8/8/5PPP/6K1 w - - 0 1", "h2h3").build();
            assertThat(run(rule, c)).extracting(CoachingFinding::title).containsExactly("Mattdrohung abgewehrt");
        }

        @Test
        void noFindingWithoutMateDanger() {
            var c = Contexts.of(AFTER_NC6, "f1c4").build();
            assertThat(run(rule, c)).isEmpty();
        }
    }

    @Nested
    class Tact012KingSafety {
        private final KingSafetyTacticalRule rule = new KingSafetyTacticalRule();

        @Test
        void pawnCaptureOpensFileTowardsKing() {
            var c = Contexts.of("6r1/4k3/8/8/8/5b2/6PP/7K w - - 0 1", "g2f3").build();
            List<CoachingFinding> f = run(rule, c);
            assertThat(f).hasSize(1);
            assertThat(f.get(0).affectedSquares()).contains("g1", "g2", "g8");
            assertThat(f.get(0).certainty()).isEqualTo(Certainty.HEURISTIC);
        }

        @Test
        void quietMoveAwayFromKingIsHarmless() {
            var c = Contexts.of("6r1/4k3/8/8/8/5b2/6PP/7K w - - 0 1", "h2h3").build();
            assertThat(run(rule, c)).isEmpty();
        }
    }

    @Nested
    class Tact013Defense {
        private final DefenseRule rule = new DefenseRule();

        @Test
        void defenderLeaves() {
            var c = Contexts.of("1r2k3/8/8/8/8/1B6/3N4/4K3 w - - 0 1", "d2f3", "b8b3").build();
            List<CoachingFinding> f = run(rule, c);
            assertThat(f).extracting(CoachingFinding::title).contains("Verteidiger abgezogen");
            assertThat(f.get(0).certainty()).isEqualTo(Certainty.VERIFIED);
            assertThat(f.get(0).affectedSquares()).contains("b3");
        }

        @Test
        void moveCreatesOverloadedDefender() {
            // Turm e8 ist einziger Verteidiger von Springer e5 und Läufer a8; Ta1 greift a8 zusätzlich an.
            var c = Contexts.of("b3r1k1/8/8/4n3/8/8/8/1R2R1K1 w - - 0 1", "b1a1").build();
            List<CoachingFinding> f = run(rule, c);
            assertThat(f).extracting(CoachingFinding::title).containsExactly("Überlasteter Verteidiger");
            assertThat(f.get(0).affectedSquares()).contains("e8", "e5", "a8");
            var map = DefenseRule.overloadedDefenders(c.after(), de.chesscoach.domain.position.Color.BLACK);
            assertThat(map).containsOnlyKeys(de.chesscoach.domain.position.Squares.parse("e8"));
        }

        @Test
        void unrelatedMoveKeepsDefence() {
            var c = Contexts.of("1r2k3/8/8/8/8/1B6/3N4/4K3 w - - 0 1", "e1e2").build();
            assertThat(run(rule, c)).isEmpty();
        }
    }

    @Nested
    class Tact014PassedPawn {
        private final PassedPawnTacticalRule rule = new PassedPawnTacticalRule();

        @Test
        void promotionInLine() {
            var c = Contexts.of("8/4P3/8/8/8/k7/8/4K3 w - - 0 1", "e7e8q", "a3b2").build();
            List<CoachingFinding> f = run(rule, c);
            assertThat(f).hasSize(1);
            assertThat(f.get(0).polarity()).isEqualTo(Polarity.POSITIVE);
            assertThat(f.get(0).certainty()).isEqualTo(Certainty.VERIFIED);
        }

        @Test
        void ruleOfTheSquare() {
            var c = Contexts.of("8/8/8/8/P7/8/8/5k1K w - - 0 1", "a4a5").build();
            assertThat(run(rule, c)).extracting(CoachingFinding::title).containsExactly("Freibauer außerhalb des Quadrats");
        }

        @Test
        void kingInsideSquareCatchesPawn() {
            var c = Contexts.of("8/8/2k5/8/P7/8/8/7K w - - 0 1", "a4a5").build();
            assertThat(run(rule, c)).isEmpty();
        }
    }

    @Nested
    class Tact015Fork {
        private final ForkRule rule = new ForkRule();

        @Test
        void knightForksKingAndRook() {
            var c = Contexts.of("r3k3/8/8/1N6/8/8/8/4K3 w - - 0 1", "b5c7", "e8d7", "c7a8").build();
            List<CoachingFinding> f = run(rule, c);
            assertThat(f).hasSize(1);
            assertThat(f.get(0).certainty()).isEqualTo(Certainty.VERIFIED);
            assertThat(f.get(0).affectedSquares()).contains("c7", "a8", "e8");
        }

        @Test
        void opponentForkAfterQuietMove() {
            var c = Contexts.of("4k3/8/8/8/3n4/8/7P/R3K3 w Q - 0 1", "h2h3", "d4c2", "e1d2", "c2a1").build();
            List<CoachingFinding> f = run(rule, c);
            assertThat(f).extracting(CoachingFinding::title).containsExactly("Gegnerische Gabel");
            assertThat(f.get(0).polarity()).isEqualTo(Polarity.NEGATIVE);
        }

        @Test
        void singleTargetIsNoFork() {
            var c = Contexts.of("4k3/8/8/1N6/8/8/8/4K3 w - - 0 1", "b5c7").build();
            assertThat(run(rule, c)).isEmpty();
        }
    }

    @Nested
    class Tact016Simplification {
        private final SimplificationRule rule = new SimplificationRule();

        /**
         * Gefunden beim Probespielen: Nach 12...Qxf2+ (Turm geschlagen) hieß es zu 13.Qxf2 Bxf2+ 14.Kxf2: „Du gibst die
         * Dame ab und bekommst dafür die Dame und einen Läufer – das lohnt sich für dich.“ Der Turm fehlte in der Bilanz.
         */
        @Test
        void recaptureCountsThePieceTheOpponentJustTook() {
            String beforeOpponent = "r1b2rk1/ppp2ppp/3p4/2b5/2P1P2q/P1N2Q1P/1PP2RP1/R1B3K1 b - - 2 12";
            String afterOpponent = "r1b2rk1/ppp2ppp/3p4/2b5/2P1P3/P1N2Q1P/1PP2qP1/R1B3K1 w - - 0 13";
            var c = Contexts.of(afterOpponent, "f3f2", "c5f2", "g1f2").cp(-410, -414, -414)
                    .previousOpponent(beforeOpponent, "h4f2").build();
            List<CoachingFinding> f = run(rule, c);
            assertThat(f).hasSize(1);
            assertThat(f.get(0).polarity()).isEqualTo(Polarity.NEGATIVE);
            assertThat(f.get(0).explanation())
                    .contains("Rechnet man deinen gerade geschlagenen Turm mit")
                    .contains("bekommst dafür nur")
                    .doesNotContain("lohnt sich");
        }

        @Test
        void losingExchange() {
            var c = Contexts.of(AFTER_NC6, "f3e5", "c6e5").build();
            List<CoachingFinding> f = run(rule, c);
            assertThat(f).hasSize(1);
            assertThat(f.get(0).polarity()).isEqualTo(Polarity.NEGATIVE);
            assertThat(f.get(0).evidence().materialAfter()).isEqualTo(-2);
        }

        @Test
        void equalTradeIsNeutral() {
            String fen = "r1bqkbnr/1ppp1ppp/p1n5/1B2p3/4P3/5N2/PPPP1PPP/RNBQK2R w KQkq - 0 4";
            var c = Contexts.of(fen, "b5c6", "d7c6").build();
            List<CoachingFinding> f = run(rule, c);
            assertThat(f).extracting(CoachingFinding::polarity).containsExactly(Polarity.NEUTRAL);
        }

        @Test
        void notApplicableForQuietMove() {
            var c = Contexts.of(AFTER_NC6, "f1c4", "g8f6").build();
            assertThat(rule.matches(c)).isFalse();
        }
    }
}
