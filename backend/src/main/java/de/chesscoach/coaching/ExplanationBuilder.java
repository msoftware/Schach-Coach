package de.chesscoach.coaching;

import de.chesscoach.domain.analysis.Score;
import de.chesscoach.domain.coaching.CoachingFinding;
import de.chesscoach.domain.coaching.CoachingTip;
import de.chesscoach.domain.coaching.Confidence;
import de.chesscoach.domain.coaching.ExplanationSection;
import de.chesscoach.domain.coaching.ExplanationStatement;
import de.chesscoach.domain.coaching.ExplanationType;
import de.chesscoach.domain.coaching.FindingCategory;
import de.chesscoach.domain.coaching.MoveClassification;
import de.chesscoach.domain.coaching.Severity;
import de.chesscoach.domain.coaching.StrategicPlan;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Ebene 3: Setzt aus Engine-Ergebnissen und Regelbefunden eine deutschsprachige, nachvollziehbare Erklärung zusammen.
 * Jede Aussage trägt ihre Quellen (Regel-ID oder "ENGINE"); Varianten werden als Hauptvarianten der Suche und
 * nicht als erzwungene Zugfolgen gekennzeichnet.
 */
@Component
public class ExplanationBuilder {

    public static final String ENGINE = "ENGINE";

    /**
     * @param played          Kontext des gespielten Zuges
     * @param playedFindings  Befunde des gespielten Zuges (Ebene 1 und 2)
     * @param best            Kontext des besten Zuges (leer, wenn der gespielte Zug der beste ist)
     * @param bestFindings    Befunde des besten Zuges
     * @param classification  Ergebnis der Qualitätsklassifizierung
     * @param confidence      Zuverlässigkeit der Analyse
     * @param confidenceNotes Gründe für eine eingeschränkte Zuverlässigkeit
     * @param plans           langfristige Pläne
     * @param alternatives    weitere gute Kandidaten (SAN mit Bewertung), ohne gespielten und besten Zug
     * @param analysisDepth   erreichte Suchtiefe der Kandidatenbewertung
     */
    public record Input(PositionContext played, List<CoachingFinding> playedFindings, Optional<PositionContext> best,
                        List<CoachingFinding> bestFindings, MistakeClassifier.Result classification, Confidence confidence,
                        List<String> confidenceNotes, List<StrategicPlan> plans, List<String> alternatives, int analysisDepth) {
    }

    public MoveExplanation build(Input in) {
        List<CoachingFinding> findings = prioritize(applyTacticalPrecedence(in.playedFindings(), in.classification()));
        MoveClassification cls = in.classification().classification();
        ExplanationType type = determineType(in, findings);
        PositionContext c = in.played();
        String san = c.line().ply(0).numberedSan();
        String headline = cls.german() + ": " + san;

        Optional<CoachingFinding> main = mainFinding(findings, cls);
        String mainReason;
        List<String> mainSources;
        if (main.isPresent()) {
            mainReason = main.get().explanation();
            mainSources = List.of(main.get().ruleId());
        } else {
            mainReason = engineSummary(in);
            mainSources = List.of(ENGINE);
        }

        List<ExplanationSection> sections = new ArrayList<>();
        sections.add(goodOrBad(in, findings, type, main));
        sections.add(consequence(in, findings));
        sections.add(opponentReply(in, findings));
        sections.add(betterMove(in));
        sections.add(whyBetter(in, findings));
        sections.add(plans(in));
        String principle = principle(findings, cls, type);
        sections.add(new ExplanationSection("principle", "Welches Schachprinzip sollte ich mir merken?",
                List.of(new ExplanationStatement(principle, main.map(f -> List.of(f.ruleId())).orElse(List.of())))));

        String better = in.best().map(b -> "Besser war " + b.line().ply(0).numberedSan() + " ("
                        + display(b.scoreAfter()) + " statt " + display(c.scoreAfter()) + ").")
                .orElse("Du hast den besten gefundenen Zug gespielt.");
        CoachingTip tip = new CoachingTip(type.german(), sections.get(0).statements().isEmpty() ? headline
                : sections.get(0).statements().get(0).text(), mainReason, better,
                sections.get(1).statements().isEmpty() ? "" : sections.get(1).statements().get(0).text(), principle, in.confidence());

        List<String> tags = new ArrayList<>();
        for (CoachingFinding f : findings) {
            if (f.isNegative() && f.severity().atLeast(Severity.MEDIUM) && !f.overriddenByTactics() && !tags.contains(f.ruleId())) {
                tags.add(f.ruleId());
            }
        }
        return new MoveExplanation(type, headline, mainReason, mainSources, sections, tip, tags, findings);
    }

    /** Nachgewiesene taktische Folgen haben Vorrang vor strategischen Heuristiken (Requirements 5.3). */
    static List<CoachingFinding> applyTacticalPrecedence(List<CoachingFinding> findings, MistakeClassifier.Result classification) {
        boolean tacticalRefutation = findings.stream().anyMatch(f -> f.category() == FindingCategory.TACTICAL && f.isNegative()
                && f.isVerified() && f.severity().atLeast(Severity.MEDIUM));
        boolean engineRefutation = classification.classification() == MoveClassification.MISTAKE
                || classification.classification() == MoveClassification.BLUNDER;
        if (!tacticalRefutation && !engineRefutation) {
            return findings;
        }
        return findings.stream()
                .map(f -> f.category() == FindingCategory.STRATEGIC && f.isPositive() && !f.isVerified() ? f.withOverriddenByTactics() : f)
                .toList();
    }

    static List<CoachingFinding> prioritize(List<CoachingFinding> findings) {
        return findings.stream().sorted(Comparator.comparingInt(ExplanationBuilder::weight).reversed()).toList();
    }

    private static int weight(CoachingFinding f) {
        int w = f.severity().ordinal() * 10;
        if (f.isVerified()) {
            w += 6;
        }
        if (f.category() == FindingCategory.TACTICAL) {
            w += 4;
        }
        if (f.overriddenByTactics()) {
            w -= 30;
        }
        if (f.severity() == Severity.INFO) {
            w -= 5;
        }
        return w;
    }

    private static Optional<CoachingFinding> mainFinding(List<CoachingFinding> findings, MoveClassification cls) {
        boolean error = cls.isError();
        return findings.stream()
                .filter(f -> !f.overriddenByTactics() && f.severity() != Severity.INFO)
                .filter(f -> error ? f.isNegative() : !f.isNegative())
                .findFirst();
    }

    ExplanationType determineType(Input in, List<CoachingFinding> findings) {
        MoveClassification cls = in.classification().classification();
        PositionContext c = in.played();
        if (cls == MoveClassification.UNCERTAIN) {
            return ExplanationType.UNCERTAIN_ANALYSIS;
        }
        if (cls == MoveClassification.FORCED) {
            return ExplanationType.FORCED_MOVE;
        }
        Set<String> negativeRules = new LinkedHashSet<>();
        Set<String> negativeTitles = new LinkedHashSet<>();
        for (CoachingFinding f : findings) {
            if (f.isNegative() && !f.overriddenByTactics()) {
                negativeRules.add(f.ruleId());
                negativeTitles.add(f.title());
            }
        }
        if (cls == MoveClassification.MISTAKE || cls == MoveClassification.BLUNDER || cls == MoveClassification.INACCURACY) {
            boolean bestWins = c.bestScore().map(s -> s.isWinningMate() || s.centipawnsClamped(3000) >= 300).orElse(false);
            boolean playedWins = c.scoreAfter().map(s -> s.isWinningMate() || s.centipawnsClamped(3000) >= 300).orElse(false);
            if (negativeRules.contains("TACT-011") || negativeTitles.contains("Angriff ignoriert")) {
                return ExplanationType.MISSED_DEFENSE;
            }
            boolean missedMate = c.bestScore().map(Score::isWinningMate).orElse(false)
                    && !c.scoreAfter().map(Score::isWinningMate).orElse(false);
            if (missedMate || bestWins && !playedWins && !negativeRules.contains("TACT-002")) {
                return ExplanationType.MISSED_WIN;
            }
            if (negativeRules.contains("TACT-002")) {
                boolean motif = negativeRules.stream().anyMatch(r -> !r.equals("TACT-002") && !r.equals("TACT-003") && r.startsWith("TACT"));
                return motif ? ExplanationType.TACTICAL_BLUNDER : ExplanationType.MATERIAL_LOSS;
            }
            if (negativeRules.stream().anyMatch(r -> r.startsWith("TACT")) && cls != MoveClassification.INACCURACY) {
                return ExplanationType.TACTICAL_BLUNDER;
            }
            if (negativeRules.contains("TACT-001")) {
                return ExplanationType.MISSED_WIN;
            }
            return ExplanationType.STRATEGIC_INACCURACY;
        }
        Score before = c.scoreBefore().orElse(null);
        Score after = c.scoreAfter().orElse(null);
        if (before != null && after != null && before.centipawnsClamped(3000) <= -200 && !after.isMate()
                && after.centipawnsClamped(3000) >= -60) {
            return ExplanationType.DRAWING_RESOURCE;
        }
        boolean endgameTechnique = c.after().isEndgame() && findings.stream()
                .anyMatch(f -> (f.ruleId().equals("STRAT-012") || f.ruleId().equals("TACT-014")) && f.isPositive());
        if (endgameTechnique) {
            return ExplanationType.ENDGAME_TECHNIQUE;
        }
        return cls == MoveClassification.GOOD ? ExplanationType.GOOD_MOVE : ExplanationType.BEST_MOVE;
    }

    private ExplanationSection goodOrBad(Input in, List<CoachingFinding> findings, ExplanationType type, Optional<CoachingFinding> main) {
        List<ExplanationStatement> st = new ArrayList<>();
        MoveClassification cls = in.classification().classification();
        PositionContext c = in.played();
        String san = c.line().ply(0).numberedSan();
        st.add(new ExplanationStatement(classificationSentence(cls, san, in.classification()), List.of(ENGINE)));
        main.ifPresent(f -> st.add(new ExplanationStatement(f.explanation(), List.of(f.ruleId()))));
        findings.stream()
                .filter(f -> main.map(m -> m != f).orElse(true))
                .filter(f -> !f.overriddenByTactics() && f.severity().atLeast(Severity.LOW) && f.severity() != Severity.INFO)
                .limit(2)
                .forEach(f -> st.add(new ExplanationStatement(f.explanation(), List.of(f.ruleId()))));
        findings.stream().filter(CoachingFinding::overriddenByTactics).findFirst().ifPresent(f -> st.add(new ExplanationStatement(
                "Strategisch spricht zwar etwas für den Zug (" + f.title() + "), doch die nachgewiesene taktische bzw. "
                        + "rechnerische Widerlegung wiegt schwerer.", List.of(f.ruleId(), ENGINE))));
        if (in.confidence() != Confidence.HIGH) {
            st.add(new ExplanationStatement("Hinweis: Die Zuverlässigkeit dieser Analyse ist " + in.confidence().german()
                    + (in.confidenceNotes().isEmpty() ? "." : " (" + String.join("; ", in.confidenceNotes()) + ").")
                    + " Die Bewertung ist nicht als endgültig zu verstehen.", List.of(ENGINE)));
        }
        if (type == ExplanationType.UNCERTAIN_ANALYSIS) {
            st.add(new ExplanationStatement("Eine verlässliche Einordnung des Zuges ist mit den vorliegenden Daten nicht möglich.",
                    List.of(ENGINE)));
        }
        return new ExplanationSection("assessment", "Was war an meinem Zug gut oder schlecht?", st);
    }

    private static String classificationSentence(MoveClassification cls, String san, MistakeClassifier.Result result) {
        String base = switch (cls) {
            case BEST -> san + " ist der beste oder ein gleichwertig bester Zug.";
            case EXCELLENT -> san + " ist sehr gut und praktisch gleichwertig mit dem besten Zug.";
            case GOOD -> san + " ist ein solider Zug mit höchstens geringem Nachteil.";
            case INACCURACY -> san + " ist ungenau: Ein Teil des möglichen Vorteils geht verloren.";
            case MISTAKE -> san + " ist ein Fehler: Ein wesentlicher Vorteil geht verloren oder ein Nachteil entsteht.";
            case BLUNDER -> san + " ist ein grober Fehler mit erheblichem Nachteil.";
            case FORCED -> san + " war praktisch erzwungen – es gab keine gleichwertige Alternative.";
            case UNCERTAIN -> "Für " + san + " liegt keine ausreichend zuverlässige Bewertung vor.";
        };
        if (result.provisional() && cls != MoveClassification.UNCERTAIN) {
            base += " (vorläufige Einordnung)";
        }
        return base;
    }

    private ExplanationSection consequence(Input in, List<CoachingFinding> findings) {
        List<ExplanationStatement> st = new ArrayList<>();
        PositionContext c = in.played();
        if (c.scoreBefore().isPresent() && c.scoreAfter().isPresent()) {
            st.add(new ExplanationStatement("Die Engine-Bewertung aus deiner Sicht ändert sich von " + c.scoreBefore().get().display()
                    + " vor dem Zug auf " + c.scoreAfter().get().display() + " danach.", List.of(ENGINE)));
        }
        Integer loss = in.classification().lossCentipawns();
        if (loss != null && loss > 0 && c.differsFromBest()) {
            st.add(new ExplanationStatement("Gegenüber dem besten Zug beträgt der Verlust etwa " + loss + " Centipawns ("
                    + String.format(java.util.Locale.GERMANY, "%.0f", in.classification().winProbabilityLoss() * 100)
                    + " Prozentpunkte Gewinnerwartung).", List.of(ENGINE)));
        }
        findings.stream()
                .filter(f -> !f.overriddenByTactics() && f.evidence() != null && f.evidence().materialBefore() != null
                        && f.evidence().materialAfter() != null)
                .findFirst()
                .ifPresent(f -> st.add(new ExplanationStatement("Materialbilanz: " + GermanText.balance(f.evidence().materialBefore())
                        + " → " + GermanText.balance(f.evidence().materialAfter()) + " (" + f.title() + ").", List.of(f.ruleId()))));
        if (st.isEmpty()) {
            st.add(new ExplanationStatement("Ohne Engine-Bewertung kann die Konsequenz nicht quantifiziert werden.", List.of()));
        }
        return new ExplanationSection("consequence", "Welche konkrete Konsequenz hatte er?", st);
    }

    private ExplanationSection opponentReply(Input in, List<CoachingFinding> findings) {
        List<ExplanationStatement> st = new ArrayList<>();
        PositionContext c = in.played();
        Optional<LinePly> reply = c.opponentReply();
        if (reply.isPresent()) {
            st.add(new ExplanationStatement("In der berechneten Hauptvariante antwortet der Gegner mit " + reply.get().numberedSan()
                    + ": " + c.line().numbered(0, Math.min(c.line().size(), 6))
                    + ". Das ist die von der Suche gefundene Fortsetzung, keine erzwungene Zugfolge.", List.of(ENGINE)));
            findings.stream()
                    .filter(f -> f.isNegative() && f.title().startsWith("Gegnerische"))
                    .limit(2)
                    .forEach(f -> st.add(new ExplanationStatement(f.explanation(), List.of(f.ruleId()))));
        } else if (c.oracle().legalMoves(c.after().toFen()).isEmpty()) {
            st.add(new ExplanationStatement("Der Gegner hat nach deinem Zug keinen legalen Zug mehr.", List.of()));
        } else {
            st.add(new ExplanationStatement("Für die gegnerische Antwort liegt keine Engine-Variante vor.", List.of()));
        }
        return new ExplanationSection("opponentReply", "Was hätte der Gegner darauf spielen können?", st);
    }

    private ExplanationSection betterMove(Input in) {
        List<ExplanationStatement> st = new ArrayList<>();
        PositionContext c = in.played();
        if (in.best().isPresent()) {
            PositionContext b = in.best().get();
            st.add(new ExplanationStatement("Besser war " + b.line().ply(0).numberedSan() + " mit einer Bewertung von "
                    + display(b.scoreAfter()) + " statt " + display(c.scoreAfter()) + " nach deinem Zug.", List.of(ENGINE)));
            if (b.line().size() > 1) {
                st.add(new ExplanationStatement("Hauptvariante: " + b.line().numbered(0, Math.min(b.line().size(), 8))
                        + " (gefundene Hauptvariante, keine erzwungene Zugfolge).", List.of(ENGINE)));
            }
        } else {
            st.add(new ExplanationStatement("Du hast den besten gefundenen Zug gespielt.", List.of(ENGINE)));
        }
        if (!in.alternatives().isEmpty()) {
            st.add(new ExplanationStatement("Weitere geprüfte Kandidaten: " + String.join(", ", in.alternatives()) + ".", List.of(ENGINE)));
        }
        return new ExplanationSection("betterMove", "Welcher Zug wäre besser gewesen?", st);
    }

    private ExplanationSection whyBetter(Input in, List<CoachingFinding> playedFindings) {
        List<ExplanationStatement> st = new ArrayList<>();
        if (in.best().isEmpty()) {
            playedFindings.stream().filter(f -> f.isPositive() && !f.overriddenByTactics()).limit(3)
                    .forEach(f -> st.add(new ExplanationStatement(f.explanation(), List.of(f.ruleId()))));
            if (st.isEmpty()) {
                st.add(new ExplanationStatement("Kein anderer geprüfter Zug erreicht laut Engine eine bessere Bewertung.", List.of(ENGINE)));
            }
            return new ExplanationSection("whyBetter", "Warum ist dein Zug gut?", st);
        }
        PositionContext b = in.best().get();
        String bestSan = b.line().ply(0).numberedSan();
        Set<String> playedPositive = new LinkedHashSet<>();
        playedFindings.stream().filter(CoachingFinding::isPositive).forEach(f -> playedPositive.add(f.ruleId() + "|" + f.title()));
        Set<String> bestNegative = new LinkedHashSet<>();
        in.bestFindings().stream().filter(CoachingFinding::isNegative).forEach(f -> bestNegative.add(f.ruleId() + "|" + f.title()));

        in.bestFindings().stream()
                .filter(f -> f.isPositive() && !playedPositive.contains(f.ruleId() + "|" + f.title()) && f.severity() != Severity.INFO)
                .sorted(Comparator.comparingInt(ExplanationBuilder::weight).reversed())
                .limit(3)
                .forEach(f -> st.add(new ExplanationStatement(bestSan + ": " + f.explanation(), List.of(f.ruleId()))));
        playedFindings.stream()
                .filter(f -> f.isNegative() && !f.overriddenByTactics() && !bestNegative.contains(f.ruleId() + "|" + f.title()))
                .limit(2)
                .forEach(f -> st.add(new ExplanationStatement(bestSan + " vermeidet das Problem „" + f.title() + "“.", List.of(f.ruleId()))));
        if (st.isEmpty()) {
            st.add(new ExplanationStatement("Die Regelbasis findet keinen einzelnen Merkmalsunterschied; der Vorteil von " + bestSan
                    + " ergibt sich aus der Engine-Berechnung: " + b.line().numbered(0, Math.min(b.line().size(), 6))
                    + " (Hauptvariante, keine erzwungene Zugfolge).", List.of(ENGINE)));
        }
        return new ExplanationSection("whyBetter", "Warum ist dieser alternative Zug besser?", st);
    }

    private ExplanationSection plans(Input in) {
        List<ExplanationStatement> st = new ArrayList<>();
        in.plans().stream().filter(p -> p.side().equals("USER")).limit(2)
                .forEach(p -> st.add(new ExplanationStatement("Für dich: " + p.title() + ". " + p.explanation(),
                        List.of(p.source() == de.chesscoach.domain.coaching.FindingSource.ENGINE ? ENGINE : "STRAT-011"))));
        in.plans().stream().filter(p -> p.side().equals("OPPONENT")).limit(2)
                .forEach(p -> st.add(new ExplanationStatement("Für den Gegner: " + p.title() + ". " + p.explanation(),
                        List.of(p.source() == de.chesscoach.domain.coaching.FindingSource.ENGINE ? ENGINE : "STRAT-011"))));
        if (st.isEmpty()) {
            st.add(new ExplanationStatement("Aus den Stellungsmerkmalen ergibt sich derzeit kein eindeutiger langfristiger Plan.", List.of("STRAT-011")));
        }
        return new ExplanationSection("plans", "Welche langfristigen Pläne ergeben sich daraus?", st);
    }

    private static String principle(List<CoachingFinding> findings, MoveClassification cls, ExplanationType type) {
        Optional<CoachingFinding> relevant = mainFinding(findings, cls);
        if (relevant.isPresent() && relevant.get().learningPrinciple() != null) {
            return relevant.get().learningPrinciple();
        }
        return switch (type) {
            case FORCED_MOVE -> "Auch erzwungene Züge verdienen Aufmerksamkeit: Prüfe, warum die Alternativen scheitern.";
            case UNCERTAIN_ANALYSIS -> "Wenn die Lage unklar ist, wähle Züge, die deine Figuren verbessern und keine Schwächen schaffen.";
            case DRAWING_RESOURCE -> "In schlechteren Stellungen zählt jede Remisressource: Suche aktive Verteidigung statt passiven Abwartens.";
            default -> cls.isError()
                    ? "Vergleiche vor jedem Zug mindestens zwei Kandidaten und prüfe die forcierenden Antworten des Gegners."
                    : "Spiele weiter nach dem Grundsatz: Erst Sicherheit prüfen, dann die Figuren aktiv verbessern.";
        };
    }

    private static String engineSummary(Input in) {
        PositionContext c = in.played();
        if (c.scoreAfter().isEmpty()) {
            return "Für diesen Zug liegt keine verlässliche Engine-Bewertung vor.";
        }
        if (in.best().isPresent()) {
            return "Laut Engine-Analyse ist " + in.best().get().line().ply(0).numberedSan() + " stärker ("
                    + display(in.best().get().scoreAfter()) + " statt " + display(c.scoreAfter()) + ").";
        }
        return "Laut Engine-Analyse hält der Zug die Bewertung bei " + c.scoreAfter().get().display() + ".";
    }

    private static String display(Optional<Score> score) {
        return score.map(Score::display).orElse("unbekannt");
    }
}
