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

    /** Regeln, deren Aussage (Materialgewinn/-verlust) im Abschnitt "Was passiert durch deinen Zug?" steht. */
    private static final Set<String> MATERIAL_RULES = Set.of("TACT-001", "TACT-002", "TACT-016");

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
        sections.add(new ExplanationSection("principle", "Was kannst du dir merken?",
                List.of(new ExplanationStatement(principle, main.map(f -> List.of(f.ruleId())).orElse(List.of())))));

        String better = in.best().map(b -> "Besser war: " + GermanText.moveWithNotation(b.line().ply(0)) + ".")
                .orElse("Du hast den besten Zug gespielt, den der Computer gefunden hat.");
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
        // „Bester Zug“ nur, wenn es keinen genaueren Zug gibt – sonst widerspräche der Typ der genannten Alternative.
        boolean moreAccurateExists = cls == MoveClassification.EXCELLENT && in.best().isPresent();
        return cls == MoveClassification.GOOD || moreAccurateExists ? ExplanationType.GOOD_MOVE : ExplanationType.BEST_MOVE;
    }

    private ExplanationSection goodOrBad(Input in, List<CoachingFinding> findings, ExplanationType type, Optional<CoachingFinding> main) {
        List<ExplanationStatement> st = new ArrayList<>();
        MoveClassification cls = in.classification().classification();
        PositionContext c = in.played();
        st.add(new ExplanationStatement(classificationSentence(cls, GermanText.moveWithNotation(c.line().ply(0)), in.classification()),
                List.of(ENGINE)));
        main.ifPresent(f -> st.add(new ExplanationStatement(f.explanation(), List.of(f.ruleId()))));
        findings.stream()
                .filter(f -> main.map(m -> m != f).orElse(true))
                .filter(f -> !MATERIAL_RULES.contains(f.ruleId()))
                .filter(f -> !f.overriddenByTactics() && f.severity().atLeast(Severity.LOW) && f.severity() != Severity.INFO)
                .limit(2)
                .forEach(f -> st.add(new ExplanationStatement(f.explanation(), List.of(f.ruleId()))));
        findings.stream().filter(CoachingFinding::overriddenByTactics).findFirst().ifPresent(f -> st.add(new ExplanationStatement(
                "Auf den ersten Blick spricht etwas für deinen Zug (" + f.title() + "). Der Nachteil, den der Computer "
                        + "gefunden hat, wiegt aber schwerer.", List.of(f.ruleId(), ENGINE))));
        if (in.confidence() != Confidence.HIGH) {
            st.add(new ExplanationStatement("Hinweis: Der Computer konnte hier nicht tief genug rechnen. Nimm die Bewertung als "
                    + "Einschätzung, nicht als endgültiges Urteil.", List.of(ENGINE)));
        }
        if (type == ExplanationType.UNCERTAIN_ANALYSIS) {
            st.add(new ExplanationStatement("Eine sichere Bewertung dieses Zuges ist gerade nicht möglich.", List.of(ENGINE)));
        }
        return new ExplanationSection("assessment", "Was war gut, was war schlecht?", st);
    }

    private static String classificationSentence(MoveClassification cls, String move, MistakeClassifier.Result result) {
        String base = switch (cls) {
            case BEST -> "Sehr gut! Mit " + move + " hast du den besten Zug gefunden.";
            case EXCELLENT -> "Sehr gut! " + move + " ist fast genauso stark wie der beste Zug.";
            case GOOD -> "Ein ordentlicher Zug: " + move + ". Es gab etwas Besseres, aber der Unterschied ist klein.";
            case INACCURACY -> "Nicht ganz genau: " + move + " ist kein Fehler, aber es gab einen deutlich besseren Zug.";
            case MISTAKE -> "Das war ein Fehler: Nach " + move + " stehst du spürbar schlechter als nötig.";
            case BLUNDER -> "Das war ein grober Fehler: Nach " + move + " verlierst du viel – zum Beispiel eine Figur oder sogar die Partie.";
            case FORCED -> move + " war praktisch Pflicht – alle anderen Züge wären deutlich schlechter gewesen.";
            case UNCERTAIN -> "Den Zug " + move + " kann der Computer gerade nicht sicher bewerten.";
        };
        if (result.provisional() && cls != MoveClassification.UNCERTAIN) {
            base += " (Achtung: Diese Einschätzung ist vorläufig.)";
        }
        return base;
    }

    private ExplanationSection consequence(Input in, List<CoachingFinding> findings) {
        List<ExplanationStatement> st = new ArrayList<>();
        PositionContext c = in.played();
        if (c.scoreBefore().isPresent() && c.scoreAfter().isPresent()) {
            st.add(new ExplanationStatement("Vor deinem Zug: " + GermanText.evaluationWords(c.scoreBefore().get())
                    + ". Nach deinem Zug: " + GermanText.evaluationWords(c.scoreAfter().get()) + ".", List.of(ENGINE)));
        }
        Integer loss = in.classification().lossCentipawns();
        if (loss != null && loss >= 30 && c.differsFromBest()) {
            st.add(new ExplanationStatement("Im Vergleich zum besten Zug hast du " + GermanText.pawnsWords(loss) + " verschenkt.",
                    List.of(ENGINE)));
        }
        boolean immediateCapture = c.line().ply(0).isCapture() || c.opponentReply().map(LinePly::isCapture).orElse(false);
        if (c.hasEngineLine() && immediateCapture) {
            String trade = GermanText.tradeWords(c.line(), c.user(), c.line().settledIndex(12), c.previousOpponentMove().orElse(null));
            if (!trade.isBlank()) {
                st.add(new ExplanationStatement(trade, List.of(ENGINE)));
            }
        }
        if (st.isEmpty()) {
            st.add(new ExplanationStatement("Ohne Bewertung des Computers lässt sich die Folge nicht genau sagen.", List.of()));
        }
        return new ExplanationSection("consequence", "Was passiert durch deinen Zug?", st);
    }

    private ExplanationSection opponentReply(Input in, List<CoachingFinding> findings) {
        List<ExplanationStatement> st = new ArrayList<>();
        PositionContext c = in.played();
        Optional<LinePly> reply = c.opponentReply();
        if (reply.isPresent()) {
            st.add(new ExplanationStatement("Der Gegner kann so antworten: " + GermanText.moveWithNotation(reply.get()) + ".",
                    List.of(ENGINE)));
            if (c.line().size() > 2) {
                st.add(new ExplanationStatement("So könnte es weitergehen: " + GermanText.lineWords(c.line(), 2, 3, c.user())
                        + ". Das ist eine mögliche Fortsetzung, die der Computer berechnet hat – keine erzwungene Zugfolge.",
                        List.of(ENGINE)));
            }
            findings.stream()
                    .filter(f -> f.isNegative() && f.title().startsWith("Gegnerische"))
                    .limit(2)
                    .forEach(f -> st.add(new ExplanationStatement(f.explanation(), List.of(f.ruleId()))));
        } else if (c.oracle().legalMoves(c.after().toFen()).isEmpty()) {
            st.add(new ExplanationStatement("Der Gegner hat keinen Zug mehr – die Partie ist zu Ende.", List.of()));
        } else {
            st.add(new ExplanationStatement("Für die Antwort des Gegners liegt keine Berechnung vor.", List.of()));
        }
        return new ExplanationSection("opponentReply", "Wie kann der Gegner antworten?", st);
    }

    /**
     * Einleitung für den Zug, den der Computer vorzieht. Liegt der gespielte Zug innerhalb der Bestzug-Toleranz,
     * ist die Alternative nur gleich gut – „besser“ wäre dann irreführend.
     */
    static String alternativeLead(MoveClassification cls) {
        return switch (cls) {
            case BEST -> "Gleich gut war";
            case EXCELLENT -> "Etwas genauer war";
            default -> "Besser war";
        };
    }

    private ExplanationSection betterMove(Input in) {
        List<ExplanationStatement> st = new ArrayList<>();
        PositionContext c = in.played();
        boolean equal = in.classification().classification() == MoveClassification.BEST;
        if (in.best().isPresent()) {
            PositionContext b = in.best().get();
            st.add(new ExplanationStatement(alternativeLead(in.classification().classification()) + ": "
                    + GermanText.moveWithNotation(b.line().ply(0))
                    + (equal ? " – der Unterschied ist so klein, dass er keine Rolle spielt." : "."), List.of(ENGINE)));
            if (b.scoreAfter().isPresent() && c.scoreAfter().isPresent()) {
                st.add(new ExplanationStatement("Damit gilt: " + GermanText.evaluationWords(b.scoreAfter().get())
                        + (equal ? ". Nach deinem Zug: " : ". Nach deinem Zug dagegen: ")
                        + GermanText.evaluationWords(c.scoreAfter().get()) + ".", List.of(ENGINE)));
            }
            if (b.line().size() > 1) {
                st.add(new ExplanationStatement("So geht es danach weiter: " + GermanText.lineWords(b.line(), 1, 3, c.user())
                        + " (mögliche Fortsetzung, keine erzwungene Zugfolge).", List.of(ENGINE)));
            }
        } else {
            st.add(new ExplanationStatement("Du hast den besten Zug gespielt, den der Computer gefunden hat.", List.of(ENGINE)));
        }
        if (!in.alternatives().isEmpty()) {
            st.add(new ExplanationStatement("Ebenfalls gut gewesen wären: " + String.join("; ", in.alternatives()) + ".", List.of(ENGINE)));
        }
        return new ExplanationSection("betterMove", equal ? "Gab es gleich gute Züge?" : "Was wäre besser gewesen?", st);
    }

    private ExplanationSection whyBetter(Input in, List<CoachingFinding> playedFindings) {
        List<ExplanationStatement> st = new ArrayList<>();
        if (in.best().isPresent() && in.classification().classification() == MoveClassification.BEST) {
            // Gleich guter Alternativzug: nicht erklären, warum er „besser“ ist, sondern was am eigenen Zug gut ist.
            playedFindings.stream().filter(f -> f.isPositive() && !f.overriddenByTactics()).limit(3)
                    .forEach(f -> st.add(new ExplanationStatement(f.explanation(), List.of(f.ruleId()))));
            st.add(new ExplanationStatement("Laut Computer ist " + GermanText.moveWithNotation(in.best().get().line().ply(0))
                    + " genauso stark – beide Züge sind eine gute Wahl.", List.of(ENGINE)));
            return new ExplanationSection("whyBetter", "Warum ist dein Zug gut?", st);
        }
        if (in.best().isEmpty()) {
            playedFindings.stream().filter(f -> f.isPositive() && !f.overriddenByTactics()).limit(3)
                    .forEach(f -> st.add(new ExplanationStatement(f.explanation(), List.of(f.ruleId()))));
            if (st.isEmpty()) {
                st.add(new ExplanationStatement("Kein anderer Zug ist laut Computer besser.", List.of(ENGINE)));
            }
            return new ExplanationSection("whyBetter", "Warum ist dein Zug gut?", st);
        }
        PositionContext b = in.best().get();
        Set<String> playedPositive = new LinkedHashSet<>();
        playedFindings.stream().filter(CoachingFinding::isPositive).forEach(f -> playedPositive.add(f.ruleId() + "|" + f.title()));
        Set<String> bestNegative = new LinkedHashSet<>();
        in.bestFindings().stream().filter(CoachingFinding::isNegative).forEach(f -> bestNegative.add(f.ruleId() + "|" + f.title()));

        in.bestFindings().stream()
                .filter(f -> f.isPositive() && !playedPositive.contains(f.ruleId() + "|" + f.title()) && f.severity() != Severity.INFO)
                .sorted(Comparator.comparingInt(ExplanationBuilder::weight).reversed())
                .limit(3)
                .forEach(f -> st.add(new ExplanationStatement(f.explanation(), List.of(f.ruleId()))));
        playedFindings.stream()
                .filter(f -> f.isNegative() && !f.overriddenByTactics() && !bestNegative.contains(f.ruleId() + "|" + f.title()))
                .limit(2)
                .forEach(f -> st.add(new ExplanationStatement("Mit dem besseren Zug wäre das Problem „" + f.title()
                        + "“ gar nicht erst entstanden.", List.of(f.ruleId()))));
        if (st.isEmpty()) {
            st.add(new ExplanationStatement("Der Unterschied zeigt sich erst beim genauen Vorausrechnen. Der Computer sieht danach "
                    + "diese Fortsetzung: " + GermanText.lineWords(b.line(), 0, 4, b.user()) + ".", List.of(ENGINE)));
        }
        return new ExplanationSection("whyBetter", "Warum ist das besser?", st);
    }

    private ExplanationSection plans(Input in) {
        List<ExplanationStatement> st = new ArrayList<>();
        in.plans().stream().filter(p -> p.side().equals("USER") && p.source() != de.chesscoach.domain.coaching.FindingSource.ENGINE).limit(2)
                .forEach(p -> st.add(new ExplanationStatement("Dein Plan – " + p.title() + ": " + p.explanation(),
                        List.of(p.source() == de.chesscoach.domain.coaching.FindingSource.ENGINE ? ENGINE : "STRAT-011"))));
        in.plans().stream().filter(p -> p.side().equals("OPPONENT") && p.source() != de.chesscoach.domain.coaching.FindingSource.ENGINE).limit(1)
                .forEach(p -> st.add(new ExplanationStatement("Plan des Gegners – " + p.title() + ": " + p.explanation(),
                        List.of(p.source() == de.chesscoach.domain.coaching.FindingSource.ENGINE ? ENGINE : "STRAT-011"))));
        if (st.isEmpty()) {
            st.add(new ExplanationStatement("Gerade gibt es keinen klaren langfristigen Plan. Bring deine Figuren auf gute Felder und "
                    + "achte darauf, dass nichts ungeschützt steht.", List.of("STRAT-011")));
        }
        return new ExplanationSection("plans", "Wie geht es weiter?", st);
    }

    private static String principle(List<CoachingFinding> findings, MoveClassification cls, ExplanationType type) {
        Optional<CoachingFinding> relevant = mainFinding(findings, cls);
        if (relevant.isPresent() && relevant.get().learningPrinciple() != null) {
            return relevant.get().learningPrinciple();
        }
        return switch (type) {
            case FORCED_MOVE -> "Auch wenn es nur einen guten Zug gibt: Schau nach, warum die anderen Züge schlecht wären.";
            case UNCERTAIN_ANALYSIS -> "Wenn du unsicher bist: Bring deine Figuren auf gute Felder und lass nichts ungeschützt stehen.";
            case DRAWING_RESOURCE -> "Auch in schwierigen Stellungen nicht aufgeben: Aktive Verteidigung rettet oft ein Remis.";
            default -> cls.isError()
                    ? "Frag dich vor jedem Zug: Was droht mein Gegner? Steht eine meiner Figuren ungeschützt? "
                    + "Welche zwei, drei Züge kommen in Frage?"
                    : "Weiter so: Erst prüfen, ob alles sicher steht, dann die Figuren verbessern.";
        };
    }

    private static String engineSummary(Input in) {
        PositionContext c = in.played();
        if (c.scoreAfter().isEmpty()) {
            return "Für diesen Zug liegt keine sichere Bewertung des Computers vor.";
        }
        if (in.best().isPresent()) {
            return "Laut Computer war " + GermanText.moveWithNotation(in.best().get().line().ply(0)) + " besser. Nach deinem Zug gilt: "
                    + GermanText.evaluationWords(c.scoreAfter().get()) + ".";
        }
        return "Laut Computer ist das ein guter Zug. Danach gilt: " + GermanText.evaluationWords(c.scoreAfter().get()) + ".";
    }

    private static String display(Optional<Score> score) {
        return score.map(Score::display).orElse("unbekannt");
    }
}
