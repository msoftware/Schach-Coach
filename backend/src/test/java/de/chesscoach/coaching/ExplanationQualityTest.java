package de.chesscoach.coaching;

import de.chesscoach.coaching.strategic.ActivityRule;
import de.chesscoach.coaching.strategic.CenterControlRule;
import de.chesscoach.coaching.strategic.DevelopmentRule;
import de.chesscoach.coaching.strategic.EndgameRule;
import de.chesscoach.coaching.strategic.ExchangeRule;
import de.chesscoach.coaching.strategic.InitiativeRule;
import de.chesscoach.coaching.strategic.KingSafetyStrategicRule;
import de.chesscoach.coaching.strategic.LongTermPlanRule;
import de.chesscoach.coaching.strategic.MaterialCompensationRule;
import de.chesscoach.coaching.strategic.OpenLinesRule;
import de.chesscoach.coaching.strategic.OutpostRule;
import de.chesscoach.coaching.strategic.PawnStructureRule;
import de.chesscoach.coaching.tactical.DefenseRule;
import de.chesscoach.coaching.tactical.DiscoveredAttackRule;
import de.chesscoach.coaching.tactical.DoubleAttackRule;
import de.chesscoach.coaching.tactical.ForkRule;
import de.chesscoach.coaching.tactical.HangingPiecesRule;
import de.chesscoach.coaching.tactical.IntermezzoRule;
import de.chesscoach.coaching.tactical.KingSafetyTacticalRule;
import de.chesscoach.coaching.tactical.MateDefenseRule;
import de.chesscoach.coaching.tactical.MateThreatRule;
import de.chesscoach.coaching.tactical.MaterialGainRule;
import de.chesscoach.coaching.tactical.MaterialLossRule;
import de.chesscoach.coaching.tactical.PassedPawnTacticalRule;
import de.chesscoach.coaching.tactical.PinRule;
import de.chesscoach.coaching.tactical.SimplificationRule;
import de.chesscoach.coaching.tactical.SkewerRule;
import de.chesscoach.domain.analysis.Score;
import de.chesscoach.domain.coaching.CoachingFinding;
import de.chesscoach.domain.coaching.Confidence;
import de.chesscoach.domain.coaching.ExplanationSection;
import de.chesscoach.domain.coaching.ExplanationStatement;
import de.chesscoach.domain.coaching.ExplanationType;
import de.chesscoach.domain.coaching.FindingCategory;
import de.chesscoach.domain.coaching.MoveClassification;
import de.chesscoach.domain.position.Piece;
import de.chesscoach.domain.position.Position;
import de.chesscoach.domain.position.Squares;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Katalog kuratierter Teststellungen (Requirements 13.5). Für jede Stellung werden erwartete Regel-IDs,
 * Erklärungstyp, zulässige und ausdrücklich verbotene Aussagen geprüft – sowie allgemeine Invarianten:
 * keine nicht existierenden Figuren, nur legale Varianten, keine Heuristik über einer taktischen Widerlegung,
 * keine als zwingend formulierte unsichere Analyse.
 */
class ExplanationQualityTest {

    static final CoachingProperties PROPS = CoachingProperties.defaults();
    static final TacticalRuleEngine TACTICAL = new TacticalRuleEngine(List.of(new MaterialGainRule(), new MaterialLossRule(),
            new HangingPiecesRule(), new de.chesscoach.coaching.tactical.PieceAttackRule(), new DoubleAttackRule(), new PinRule(),
            new SkewerRule(), new DiscoveredAttackRule(), new IntermezzoRule(), new MateThreatRule(), new MateDefenseRule(),
            new KingSafetyTacticalRule(), new DefenseRule(), new PassedPawnTacticalRule(), new ForkRule(), new SimplificationRule()), PROPS);
    static final StrategicRuleEngine STRATEGIC = new StrategicRuleEngine(List.of(new DevelopmentRule(), new CenterControlRule(),
            new KingSafetyStrategicRule(), new ActivityRule(), new PawnStructureRule(), new OutpostRule(), new OpenLinesRule(),
            new MaterialCompensationRule(), new ExchangeRule(), new InitiativeRule(), new LongTermPlanRule(), new EndgameRule()), PROPS);
    static final MistakeClassifier CLASSIFIER = new MistakeClassifier(PROPS);
    static final ExplanationBuilder BUILDER = new ExplanationBuilder();

    /**
     * @param name           Bezeichnung
     * @param fen            Ausgangsstellung (Benutzer am Zug)
     * @param played         gespielter Zug mit Fortsetzung (Testdaten, keine echte Engine-Ausgabe)
     * @param best           bester Zug mit Fortsetzung (null = gespielter Zug ist der beste)
     * @param scores         Bewertungen vorher / gespielt / bester Zug (Testdaten)
     * @param confidence     Zuverlässigkeit
     * @param expectedClass  erwartete Qualitätsklasse
     * @param expectedType   erwarteter Erklärungstyp
     * @param expectedRules  Regel-IDs, die mindestens einen Befund liefern müssen
     * @param forbiddenRules Regel-IDs, die keinen negativen Befund liefern dürfen
     * @param mustContain    Textbausteine, die in der Erklärung vorkommen müssen
     * @param mustNotContain Textbausteine, die nicht vorkommen dürfen
     */
    record Case(String name, String fen, List<String> played, List<String> best, Score[] scores, Confidence confidence,
                MoveClassification expectedClass, ExplanationType expectedType, List<String> expectedRules,
                List<String> forbiddenRules, List<String> mustContain, List<String> mustNotContain) {
        @Override
        public String toString() {
            return name;
        }
    }

    static Stream<Case> catalog() {
        return Stream.of(
                new Case("Springer eingestellt (3.Nxe5?)", "r1bqkbnr/pppp1ppp/2n5/4p3/4P3/5N2/PPPP1PPP/RNBQKB1R w KQkq - 2 3",
                        List.of("f3e5", "c6e5", "b1c3", "g8f6"), List.of("d2d4", "e5d4", "f3d4"),
                        new Score[]{Score.cp(24), Score.cp(-229), Score.cp(23)}, Confidence.HIGH,
                        MoveClassification.BLUNDER, ExplanationType.TACTICAL_BLUNDER,
                        List.of("TACT-002", "TACT-003", "TACT-016"), List.of(),
                        List.of("3...Nxe5", "Besser war: Bauer von d2 nach d4 (3.d4)", "keine erzwungene Zugfolge"),
                        List.of("gewinnst du", "Kompensation für Material")),
                new Case("Entwicklungszug als Bestzug", "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
                        List.of("g1f3", "d7d5", "d2d4"), null,
                        new Score[]{Score.cp(30), Score.cp(25), Score.cp(25)}, Confidence.HIGH,
                        MoveClassification.BEST, ExplanationType.BEST_MOVE,
                        List.of("STRAT-001"), List.of("TACT-002", "TACT-003"),
                        List.of("Du hast den besten Zug gespielt", "Springer"),
                        List.of("Materialverlust", "grober Fehler")),
                new Case("Gegnerische Gabel zugelassen", "4k3/8/8/8/3n4/8/7P/R3K3 w Q - 0 1",
                        List.of("h2h3", "d4c2", "e1d2", "c2a1"), List.of("a1a4", "d4c6"),
                        new Score[]{Score.cp(0), Score.cp(-500), Score.cp(0)}, Confidence.HIGH,
                        MoveClassification.BLUNDER, ExplanationType.TACTICAL_BLUNDER,
                        List.of("TACT-015", "TACT-002"), List.of(),
                        List.of("Gabel", "1...Nc2+"), List.of("Initiative mit Schachgeboten")),
                new Case("Matt verpasst", "6k1/5ppp/8/8/8/8/5PPP/R5K1 w - - 0 1",
                        List.of("g1f1", "g8f8"), List.of("a1a8"),
                        new Score[]{Score.mate(1), Score.cp(450), Score.mate(1)}, Confidence.HIGH,
                        MoveClassification.MISTAKE, ExplanationType.MISSED_WIN,
                        List.of("TACT-010"), List.of(),
                        List.of("in 1 Zug mattsetzen", "Ra8#"), List.of("Dein Zug droht Matt")),
                new Case("Mattgefahr übersehen", "6k1/8/5q2/8/8/5N2/6PP/7K w - - 0 1",
                        List.of("f3e5", "f6f1"), List.of("h2h3", "f6f3"),
                        new Score[]{Score.cp(-300), Score.mate(-1), Score.cp(-300)}, Confidence.HIGH,
                        MoveClassification.BLUNDER, ExplanationType.MISSED_DEFENSE,
                        List.of("TACT-011"), List.of(),
                        List.of("Qf1#"), List.of()),
                new Case("Unsichere Analyse", "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
                        List.of("a2a3", "e7e5"), List.of("e2e4", "e7e5"),
                        new Score[]{Score.cp(30), Score.cp(-50), Score.cp(30)}, Confidence.LOW,
                        MoveClassification.INACCURACY, ExplanationType.STRATEGIC_INACCURACY,
                        List.of(), List.of("TACT-002"),
                        List.of("Einschätzung ist vorläufig", "nicht als endgültig"), List.of("erzwungen in"))
        );
    }

    record Result(PositionContext played, List<CoachingFinding> findings, MoveExplanation explanation,
                  MistakeClassifier.Result classification) {
    }

    static Result analyse(Case k) {
        Score before = k.scores()[0];
        Score playedScore = k.scores()[1];
        Score bestScore = k.scores()[2];
        EngineLine playedLine = Contexts.line(k.fen(), k.played().toArray(String[]::new));
        EngineLine bestLine = k.best() == null ? playedLine : Contexts.line(k.fen(), k.best().toArray(String[]::new));
        PositionContext played = PositionContext.builder(Position.fromFen(k.fen()), playedLine.ply(0).move(), Contexts.RULES)
                .line(playedLine).scores(before, playedScore, bestScore).bestLine(bestLine).build();
        List<CoachingFinding> findings = new ArrayList<>(TACTICAL.evaluate(played));
        findings.addAll(STRATEGIC.evaluate(played));
        Optional<PositionContext> bestContext = Optional.empty();
        List<CoachingFinding> bestFindings = List.of();
        if (k.best() != null) {
            PositionContext bc = PositionContext.builder(Position.fromFen(k.fen()), bestLine.ply(0).move(), Contexts.RULES)
                    .line(bestLine).scores(before, bestScore, bestScore).bestLine(bestLine).playedMove(false).build();
            bestContext = Optional.of(bc);
            bestFindings = new ArrayList<>(TACTICAL.evaluate(bc));
            bestFindings.addAll(STRATEGIC.evaluate(bc));
        }
        var classification = CLASSIFIER.classify(new MistakeClassifier.Input(before, playedScore, bestScore, 20,
                k.best() == null, null, k.confidence()));
        MoveExplanation explanation = BUILDER.build(new ExplanationBuilder.Input(played, findings, bestContext, bestFindings,
                classification, k.confidence(), k.confidence() == Confidence.HIGH ? List.of() : List.of("geringe Suchtiefe"),
                STRATEGIC.plans(played), List.of(), 12));
        return new Result(played, explanation.findings(), explanation, classification);
    }

    static String fullText(MoveExplanation e) {
        StringBuilder sb = new StringBuilder(e.headline()).append('\n').append(e.mainReason()).append('\n');
        for (ExplanationSection s : e.sections()) {
            for (ExplanationStatement st : s.statements()) {
                sb.append(st.text()).append('\n');
            }
        }
        return sb.toString();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("catalog")
    void catalogExpectations(Case k) {
        Result r = analyse(k);
        String text = fullText(r.explanation());
        assertThat(r.classification().classification()).as("Klasse").isEqualTo(k.expectedClass());
        assertThat(r.explanation().type()).as("Erklärungstyp").isEqualTo(k.expectedType());
        assertThat(Contexts.ids(r.findings())).as("erwartete Regeln").containsAll(k.expectedRules());
        assertThat(r.findings()).as("verbotene negative Befunde")
                .noneMatch(f -> k.forbiddenRules().contains(f.ruleId()) && f.isNegative());
        k.mustContain().forEach(s -> assertThat(text).as("muss enthalten: " + s).contains(s));
        k.mustNotContain().forEach(s -> assertThat(text).as("darf nicht enthalten: " + s).doesNotContain(s));
    }

    @ParameterizedTest(name = "Invarianten: {0}")
    @MethodSource("catalog")
    void invariants(Case k) {
        Result r = analyse(k);
        for (CoachingFinding f : r.findings()) {
            // Keine nicht existierende Figur wird als betroffen bezeichnet.
            for (String piece : f.affectedPieces()) {
                String square = piece.substring(piece.length() - 2);
                String name = piece.substring(0, piece.length() - 3);
                assertThat(pieceExistsSomewhere(r.played(), Squares.parse(square), name))
                        .as(f.ruleId() + ": Figur " + piece + " existiert").isTrue();
            }
            // Beweisketten bestehen nur aus Zügen der geprüften Variante (alle legal).
            if (f.evidence() != null) {
                List<String> numbered = new ArrayList<>();
                r.played().line().plies().forEach(p -> numbered.add(p.numberedSan()));
                r.played().bestLine().ifPresent(l -> l.plies().forEach(p -> numbered.add(p.numberedSan())));
                for (String move : f.evidence().line()) {
                    assertThat(numbered.contains(move) || legalNextMove(r.played(), move))
                            .as(f.ruleId() + " Beweiskette enthält nur legale Züge: " + move).isTrue();
                }
            }
            // Heuristiken formulieren keine Zwangsläufigkeit.
            if (!f.isVerified()) {
                assertThat(f.explanation()).doesNotContain("erzwingt").doesNotContain("zwingend");
            }
            // Materialgewinn nur, wenn die Variante ihn bestätigt.
            if (f.ruleId().equals("TACT-001") && f.isPositive() && f.evidence() != null && f.evidence().materialAfter() != null) {
                assertThat(f.evidence().materialAfter()).isGreaterThan(f.evidence().materialBefore());
            }
        }
        // Strategische Heuristik darf eine nachgewiesene taktische Widerlegung nicht übergehen.
        boolean refuted = r.findings().stream().anyMatch(f -> f.category() == FindingCategory.TACTICAL && f.isNegative()
                && f.isVerified() && f.severity().atLeast(de.chesscoach.domain.coaching.Severity.MEDIUM));
        if (refuted) {
            assertThat(r.findings()).filteredOn(f -> f.category() == FindingCategory.STRATEGIC && f.isPositive() && !f.isVerified())
                    .allMatch(CoachingFinding::overriddenByTactics);
            assertThat(r.explanation().mainReasonSources()).noneMatch(s -> s.startsWith("STRAT"));
        }
        // Jede Aussage der Erklärung ist zurückverfolgbar.
        r.explanation().sections().forEach(s -> assertThat(s.statements()).isNotEmpty());
        assertThat(r.explanation().sections()).hasSize(7);
    }

    @Test
    void goodMovesAreExplainedToo() {
        Result r = analyse(catalog().filter(c -> c.name().startsWith("Entwicklungszug")).findFirst().orElseThrow());
        assertThat(r.explanation().mainReasonSources()).noneMatch(s -> s.equals(ExplanationBuilder.ENGINE));
        assertThat(r.explanation().mainReason()).isNotBlank();
    }

    /** Zug (SAN) ist in der Stellung nach dem Zug legal – für beide Seiten (Drohungsprüfung per Nullzug). */
    private static boolean legalNextMove(PositionContext c, String san) {
        Position after = c.after();
        return Contexts.RULES.legalMoves(after.toFen()).stream().anyMatch(m -> m.san().equals(san))
                || Contexts.RULES.legalMoves(after.withSideToMove(c.user()).toFen()).stream().anyMatch(m -> m.san().equals(san));
    }

    private static boolean pieceExistsSomewhere(PositionContext c, int square, String germanName) {
        List<Position> positions = new ArrayList<>();
        positions.add(c.before());
        c.line().plies().forEach(p -> positions.add(p.after()));
        for (Position p : positions) {
            Piece piece = p.pieceAt(square);
            if (piece != null && piece.germanName().equals(germanName)) {
                return true;
            }
        }
        return false;
    }
}
