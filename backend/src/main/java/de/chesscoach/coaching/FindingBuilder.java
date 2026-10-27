package de.chesscoach.coaching;

import de.chesscoach.domain.coaching.Certainty;
import de.chesscoach.domain.coaching.CoachingFinding;
import de.chesscoach.domain.coaching.Confidence;
import de.chesscoach.domain.coaching.Evidence;
import de.chesscoach.domain.coaching.FindingSource;
import de.chesscoach.domain.coaching.Polarity;
import de.chesscoach.domain.coaching.Severity;
import de.chesscoach.domain.position.Position;
import de.chesscoach.domain.position.Squares;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Erzeugt Befunde mit einheitlichen Pflichtangaben einer Regel. */
public final class FindingBuilder {

    private final CoachingRule rule;
    private Severity severity = Severity.LOW;
    private Polarity polarity = Polarity.NEUTRAL;
    private Certainty certainty = Certainty.HEURISTIC;
    private String title;
    private String explanation;
    private final Set<String> squares = new LinkedHashSet<>();
    private final Set<String> pieces = new LinkedHashSet<>();
    private Evidence evidence;
    private String principle;
    private FindingSource source = FindingSource.COACHING_RULE;

    private FindingBuilder(CoachingRule rule) {
        this.rule = rule;
        this.title = rule.title();
        this.principle = rule.learningPrinciple();
    }

    public static FindingBuilder of(CoachingRule rule) {
        return new FindingBuilder(rule);
    }

    public FindingBuilder severity(Severity severity) {
        this.severity = severity;
        return this;
    }

    public FindingBuilder positive() {
        this.polarity = Polarity.POSITIVE;
        return this;
    }

    public FindingBuilder negative() {
        this.polarity = Polarity.NEGATIVE;
        return this;
    }

    public FindingBuilder neutral() {
        this.polarity = Polarity.NEUTRAL;
        return this;
    }

    public FindingBuilder verified() {
        this.certainty = Certainty.VERIFIED;
        return this;
    }

    public FindingBuilder heuristic() {
        this.certainty = Certainty.HEURISTIC;
        return this;
    }

    public FindingBuilder engineSource() {
        this.source = FindingSource.ENGINE;
        return this;
    }

    public FindingBuilder title(String title) {
        this.title = title;
        return this;
    }

    public FindingBuilder explanation(String explanation) {
        this.explanation = explanation;
        return this;
    }

    public FindingBuilder principle(String principle) {
        this.principle = principle;
        return this;
    }

    public FindingBuilder square(int square) {
        if (square >= 0) {
            squares.add(Squares.name(square));
        }
        return this;
    }

    public FindingBuilder squares(Collection<Integer> list) {
        list.forEach(this::square);
        return this;
    }

    /** Fügt die Figur auf dem Feld als betroffene Figur hinzu (z. B. "weißer Springer f3"). */
    public FindingBuilder piece(Position position, int square) {
        if (square >= 0 && position.pieceAt(square) != null) {
            pieces.add(position.pieceAt(square).germanName() + " " + Squares.name(square));
            squares.add(Squares.name(square));
        }
        return this;
    }

    public FindingBuilder pieces(Position position, Collection<Integer> list) {
        list.forEach(sq -> piece(position, sq));
        return this;
    }

    public FindingBuilder evidence(Evidence evidence) {
        this.evidence = evidence;
        return this;
    }

    /** Beweiskette aus einer Variante (auslösender Zug + Folgezüge). */
    public FindingBuilder lineEvidence(EngineLine line, int plies, Integer materialBefore, Integer materialAfter,
                                       String outcome, Confidence confidence) {
        List<String> follow = new ArrayList<>();
        for (int i = 1; i < Math.min(line.size(), plies); i++) {
            follow.add(line.ply(i).numberedSan());
        }
        this.evidence = new Evidence(line.start().toFen(), line.isEmpty() ? null : line.ply(0).numberedSan(), follow,
                materialBefore, materialAfter, outcome, certainty, confidence);
        return this;
    }

    public CoachingFinding build() {
        if (explanation == null || explanation.isBlank()) {
            throw new IllegalStateException("Befund ohne Begründung für Regel " + rule.id());
        }
        Evidence ev = evidence != null ? evidence
                : new Evidence(null, null, List.of(), null, null, null, certainty,
                certainty == Certainty.VERIFIED ? Confidence.HIGH : Confidence.MEDIUM);
        if (ev.certainty() != certainty) {
            ev = new Evidence(ev.startFen(), ev.triggerMove(), ev.line(), ev.materialBefore(), ev.materialAfter(),
                    ev.outcome(), certainty, ev.confidence());
        }
        return new CoachingFinding(rule.id(), rule.category(), source, severity, polarity, certainty, title,
                GermanText.capitalize(explanation), List.copyOf(squares), List.copyOf(pieces), ev, List.of(), principle, false);
    }
}
