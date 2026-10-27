package de.chesscoach.coaching.strategic;

import de.chesscoach.chess.ChessRulesService;
import de.chesscoach.coaching.Contexts;
import de.chesscoach.coaching.PositionContext;
import de.chesscoach.coaching.StrategicRule;
import de.chesscoach.domain.coaching.Certainty;
import de.chesscoach.domain.coaching.CoachingFinding;
import de.chesscoach.domain.coaching.FindingCategory;
import de.chesscoach.domain.coaching.Polarity;
import de.chesscoach.domain.coaching.StrategicPlan;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Jede strategische Regel (STRAT-001 bis STRAT-012) mit kuratierten Teststellungen. */
class StrategicRulesTest {

    private static final String START = ChessRulesService.STANDARD_FEN;
    private static final String KNIGHT_F3_E5 = "rnbqkbnr/pppp1ppp/8/4p3/8/5N2/PPPPPPPP/RNBQKB1R w KQkq - 0 2";

    private static List<CoachingFinding> run(StrategicRule rule, PositionContext c) {
        List<CoachingFinding> findings = rule.matches(c) ? rule.evaluate(c) : List.of();
        assertThat(findings).allSatisfy(f -> {
            assertThat(f.category()).isEqualTo(FindingCategory.STRATEGIC);
            assertThat(f.explanation()).isNotBlank();
            assertThat(f.ruleId()).isEqualTo(rule.id());
        });
        return findings;
    }

    @Nested
    class Strat001Development {
        private final DevelopmentRule rule = new DevelopmentRule();

        @Test
        void developingKnight() {
            List<CoachingFinding> f = run(rule, Contexts.of(START, "g1f3").build());
            assertThat(f).extracting(CoachingFinding::title).containsExactly("Figur entwickelt");
            assertThat(f.get(0).explanation()).contains("g1").contains("f3").contains("Noch unentwickelt");
        }

        @Test
        void movingDevelopedPieceAgain() {
            List<CoachingFinding> f = run(rule, Contexts.of(KNIGHT_F3_E5, "f3g5").build());
            assertThat(f).extracting(CoachingFinding::title).contains("Bereits entwickelte Figur erneut gezogen");
            assertThat(f).allMatch(x -> x.certainty() == Certainty.HEURISTIC);
        }

        @Test
        void earlyQueenMoveIsNotCountedAsSecondMove() {
            String fen = "rnbqkbnr/pppp1ppp/8/4p3/4P3/8/PPPP1PPP/RNBQKBNR w KQkq - 0 2";
            List<CoachingFinding> f = run(rule, Contexts.of(fen, "d1h5").build());
            assertThat(f).extracting(CoachingFinding::title).containsExactly("Frühe Damenzüge");
        }

        @Test
        void notApplicableAfterOpening() {
            assertThat(rule.matches(Contexts.of("8/8/4k3/8/8/3K4/8/R7 w - - 0 40", "a1a2").build())).isFalse();
        }
    }

    @Nested
    class Strat002Center {
        private final CenterControlRule rule = new CenterControlRule();

        @Test
        void pawnOccupiesCenter() {
            List<CoachingFinding> f = run(rule, Contexts.of(START, "e2e4").build());
            assertThat(f).extracting(CoachingFinding::polarity).containsExactly(Polarity.POSITIVE);
            assertThat(f.get(0).explanation()).contains("e4").contains("d5");
        }

        @Test
        void retreatLosesCenterControl() {
            List<CoachingFinding> f = run(rule, Contexts.of(KNIGHT_F3_E5, "f3g1").build());
            assertThat(f).extracting(CoachingFinding::polarity).containsExactly(Polarity.NEGATIVE);
            assertThat(f.get(0).affectedSquares()).contains("d4", "e5");
        }

        @Test
        void pawnLever() {
            String fen = "r1bqkbnr/pppp1ppp/2n5/4p3/4P3/5N2/PPPP1PPP/RNBQKB1R w KQkq - 2 3";
            List<CoachingFinding> f = run(rule, Contexts.of(fen, "d2d4").build());
            assertThat(f).extracting(CoachingFinding::title).contains("Zentraler Bauernhebel");
        }
    }

    @Nested
    class Strat003KingSafety {
        private final KingSafetyStrategicRule rule = new KingSafetyStrategicRule();
        private static final String READY_TO_CASTLE = "rnbqkbnr/pppppppp/8/8/8/5NP1/PPPPPPBP/RNBQK2R w KQkq - 0 4";

        @Test
        void castling() {
            assertThat(run(rule, Contexts.of(READY_TO_CASTLE, "e1g1").build())).extracting(CoachingFinding::title)
                    .containsExactly("Rochade");
        }

        @Test
        void losingCastlingRights() {
            assertThat(run(rule, Contexts.of(READY_TO_CASTLE, "e1f1").build())).extracting(CoachingFinding::title)
                    .containsExactly("Rochaderecht verloren");
        }

        @Test
        void weakeningPawnShield() {
            String fen = "r1bqk2r/pppp1ppp/2n2n2/2b1p3/2B1P3/5N2/PPPP1PPP/RNBQ1RK1 w kq - 6 5";
            List<CoachingFinding> f = run(rule, Contexts.of(fen, "g2g4").build());
            assertThat(f).extracting(CoachingFinding::title).containsExactly("Bauernschutz des Königs gelockert");
            assertThat(f.get(0).affectedSquares()).contains("g2", "g1");
        }

        @Test
        void uncastledKingIsNotPenalisedPerSe() {
            assertThat(run(rule, Contexts.of(READY_TO_CASTLE, "d2d3").build())).isEmpty();
        }
    }

    @Nested
    class Strat004Activity {
        private final ActivityRule rule = new ActivityRule();

        @Test
        void knightOnTheRim() {
            assertThat(run(rule, Contexts.of(START, "b1a3").build())).extracting(CoachingFinding::title)
                    .containsExactly("Springer am Rand");
        }

        @Test
        void rookActivated() {
            List<CoachingFinding> f = run(rule, Contexts.of("4k3/8/8/8/8/8/8/R3K3 w Q - 0 1", "a1a4").build());
            assertThat(f).extracting(CoachingFinding::title).containsExactly("Figur aktiviert");
        }

        @Test
        void pawnBlocksOwnBishop() {
            List<CoachingFinding> f = run(rule, Contexts.of("4k3/8/8/8/8/8/3P4/4KB2 w - - 0 1", "d2d3").build());
            assertThat(f).extracting(CoachingFinding::title).containsExactly("Eigener Läufer blockiert");
            assertThat(f.get(0).affectedSquares()).contains("f1", "d3");
        }
    }

    @Nested
    class Strat005PawnStructure {
        private final PawnStructureRule rule = new PawnStructureRule();

        @Test
        void capturingTowardsDoubledIsolatedPawns() {
            List<CoachingFinding> f = run(rule, Contexts.of("4k3/8/8/8/8/4n3/4PP2/4K3 w - - 0 1", "f2e3").build());
            assertThat(f).extracting(CoachingFinding::title)
                    .contains("Eigene Struktur: Doppelbauer", "Eigene Struktur: isolierter Bauer");
            assertThat(f).allMatch(x -> x.polarity() == Polarity.NEGATIVE);
            assertThat(f.get(0).affectedSquares()).contains("e3");
        }

        @Test
        void creatingPassedPawn() {
            List<CoachingFinding> f = run(rule, Contexts.of("4k3/8/8/3p4/4P3/8/8/4K3 w - - 0 1", "e4d5").build());
            assertThat(f).extracting(CoachingFinding::title).contains("Eigene Struktur: Freibauer");
            assertThat(f).filteredOn(x -> x.title().contains("Freibauer")).allMatch(CoachingFinding::isPositive);
        }

        @Test
        void quietPieceMoveChangesNothing() {
            assertThat(run(rule, Contexts.of(START, "g1f3").build())).isEmpty();
        }
    }

    @Nested
    class Strat006Outpost {
        private final OutpostRule rule = new OutpostRule();

        @Test
        void knightOnOutpost() {
            List<CoachingFinding> f = run(rule, Contexts.of("4k3/pp3ppp/8/8/4P3/2N5/PP3PPP/4K3 w - - 0 1", "c3d5").build());
            assertThat(f).extracting(CoachingFinding::title).containsExactly("Vorposten besetzt");
            assertThat(f.get(0).affectedSquares()).containsExactly("d5");
        }

        @Test
        void attackablePostIsNoOutpost() {
            assertThat(run(rule, Contexts.of("4k3/pp2pppp/8/8/4P3/2N5/PP3PPP/4K3 w - - 0 1", "c3d5").build())).isEmpty();
        }

        @Test
        void pawnMoveCreatesHoles() {
            List<CoachingFinding> f = run(rule, Contexts.of("rnbqk3/8/8/8/3P4/8/5P2/RNBQK3 w - - 0 1", "f2f4").build());
            assertThat(f).extracting(CoachingFinding::title).containsExactly("Schwache Felder im eigenen Lager");
        }
    }

    @Nested
    class Strat007OpenLines {
        private final OpenLinesRule rule = new OpenLinesRule();

        @Test
        void rookToOpenFile() {
            List<CoachingFinding> f = run(rule, Contexts.of("4k3/pp3ppp/8/8/8/8/PP3PPP/R3K3 w Q - 0 1", "a1d1").build());
            assertThat(f).extracting(CoachingFinding::title).containsExactly("Offene Linie besetzt");
            assertThat(f.get(0).explanation()).contains("d-Linie");
        }

        @Test
        void rookToSeventhRank() {
            List<CoachingFinding> f = run(rule, Contexts.of("4k3/pp3ppp/8/8/8/8/PP3PPP/3RK3 w - - 0 1", "d1d7").build());
            assertThat(f).extracting(CoachingFinding::title).contains("Eindringen auf die siebte Reihe");
        }

        @Test
        void knightMovesAreIgnored() {
            assertThat(rule.matches(Contexts.of(START, "g1f3").build())).isFalse();
        }
    }

    @Nested
    class Strat008Compensation {
        private final MaterialCompensationRule rule = new MaterialCompensationRule();
        private static final String KINGS_GAMBIT = "rnbqkbnr/pppp1ppp/8/4p3/4P3/8/PPPP1PPP/RNBQKBNR w KQkq - 0 2";

        @Test
        void gambitWithCompensation() {
            List<CoachingFinding> f = run(rule, Contexts.of(KINGS_GAMBIT, "f2f4", "e5f4", "g1f3", "g7g5").cp(30, 5, 30).build());
            assertThat(f).extracting(CoachingFinding::title).containsExactly("Kompensation für Material");
            assertThat(f.get(0).certainty()).isEqualTo(Certainty.VERIFIED);
        }

        @Test
        void materialDeficitWithBadScoreIsNotCompensation() {
            var c = Contexts.of(KINGS_GAMBIT, "f2f4", "e5f4", "g1f3", "g7g5").cp(30, -200, 30).build();
            assertThat(run(rule, c)).isEmpty();
        }

        @Test
        void noEngineScoreNoClaim() {
            assertThat(run(rule, Contexts.of(KINGS_GAMBIT, "f2f4", "e5f4").build())).isEmpty();
        }
    }

    @Nested
    class Strat009Exchange {
        private final ExchangeRule rule = new ExchangeRule();

        @Test
        void givingUpBishopPair() {
            String fen = "r1bqkbnr/1ppp1ppp/p1n5/1B2p3/4P3/5N2/PPPP1PPP/RNBQK2R w KQkq - 0 4";
            List<CoachingFinding> f = run(rule, Contexts.of(fen, "b5c6", "d7c6").build());
            assertThat(f).extracting(CoachingFinding::title).contains("Läuferpaar abgegeben");
        }

        @Test
        void simplifyingWhenAhead() {
            String fen = "3rk3/8/8/8/8/8/PPP5/3RK3 w - - 0 1";
            List<CoachingFinding> f = run(rule, Contexts.of(fen, "d1d8", "e8d8").build());
            assertThat(f).extracting(CoachingFinding::title).contains("Vereinfachung mit Materialvorteil");
        }

        @Test
        void noExchangeWithoutRecapture() {
            String fen = "r1bqkbnr/1ppp1ppp/p1n5/1B2p3/4P3/5N2/PPPP1PPP/RNBQK2R w KQkq - 0 4";
            assertThat(run(rule, Contexts.of(fen, "b5c6").build())).isEmpty();
        }
    }

    @Nested
    class Strat010Initiative {
        private final InitiativeRule rule = new InitiativeRule();

        @Test
        void tempoLossByMovingBack() {
            var c = Contexts.of(KNIGHT_F3_E5, "f3g1").previousOwn(START, "g1f3").build();
            assertThat(run(rule, c)).extracting(CoachingFinding::title).containsExactly("Tempoverlust");
        }

        @Test
        void concreteThreat() {
            List<CoachingFinding> f = run(rule, Contexts.of("4k3/8/8/3n4/8/8/8/R3K3 w Q - 0 1", "a1a5").build());
            assertThat(f).extracting(CoachingFinding::polarity).containsExactly(Polarity.POSITIVE);
            assertThat(f.get(0).explanation()).contains("Springer d5");
        }

        @Test
        void quietMoveHasNoInitiative() {
            assertThat(run(rule, Contexts.of(START, "a2a3").build())).isEmpty();
        }
    }

    @Nested
    class Strat011Plans {
        private final LongTermPlanRule rule = new LongTermPlanRule();

        @Test
        void attackIsolatedPawn() {
            var c = Contexts.of("4k3/pp3ppp/8/3p4/8/8/PP3PPP/3RK3 w - - 0 1", "e1e2").build();
            List<StrategicPlan> plans = rule.plans(c);
            assertThat(plans).filteredOn(p -> p.side().equals("USER")).extracting(StrategicPlan::title)
                    .contains("Schwachen Bauern d5 angreifen");
            assertThat(plans).allSatisfy(p -> assertThat(p.basis()).isNotBlank());
            assertThat(run(rule, c)).hasSize(1);
        }

        @Test
        void kingActivationInEndgame() {
            var c = Contexts.of("8/8/8/8/8/8/k7/6K1 w - - 0 50", "g1f1").build();
            assertThat(rule.plans(c)).extracting(StrategicPlan::title).contains("König aktivieren");
        }

        @Test
        void engineContinuationIsLabelledAsSearchLine() {
            var c = Contexts.of(START, "e2e4", "e7e5", "g1f3").build();
            assertThat(rule.plans(c)).filteredOn(p -> p.title().startsWith("Fortsetzung laut Engine"))
                    .allSatisfy(p -> assertThat(p.explanation()).contains("keine erzwungene Zugfolge"));
        }
    }

    @Nested
    class Strat012Endgame {
        private final EndgameRule rule = new EndgameRule();

        @Test
        void kingTowardsCenter() {
            assertThat(run(rule, Contexts.of("8/8/8/8/8/8/k6P/6K1 w - - 0 50", "g1f2").build()))
                    .extracting(CoachingFinding::title).containsExactly("König aktiviert");
        }

        @Test
        void opposition() {
            List<CoachingFinding> f = run(rule, Contexts.of("8/8/4k3/8/8/4K3/4P3/8 w - - 0 1", "e3e4").build());
            assertThat(f).extracting(CoachingFinding::title).contains("Opposition");
        }

        @Test
        void rookBehindPassedPawn() {
            List<CoachingFinding> f = run(rule, Contexts.of("8/7k/8/P7/8/8/8/1R4K1 w - - 0 1", "b1a1").build());
            assertThat(f).extracting(CoachingFinding::title).containsExactly("Turm hinter dem Freibauern");
        }

        @Test
        void notApplicableInMiddlegame() {
            assertThat(rule.matches(Contexts.of(START, "e2e4").build())).isFalse();
        }
    }
}
