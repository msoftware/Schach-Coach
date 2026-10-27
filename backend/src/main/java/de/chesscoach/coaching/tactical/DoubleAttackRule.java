package de.chesscoach.coaching.tactical;

import de.chesscoach.coaching.AbstractRule;
import de.chesscoach.coaching.FindingBuilder;
import de.chesscoach.coaching.LinePly;
import de.chesscoach.coaching.PositionContext;
import de.chesscoach.coaching.TacticalRule;
import de.chesscoach.coaching.TacticsToolkit;
import de.chesscoach.domain.coaching.CoachingFinding;
import de.chesscoach.domain.coaching.Confidence;
import de.chesscoach.domain.coaching.Severity;
import de.chesscoach.domain.position.Color;
import de.chesscoach.domain.position.Position;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * TACT-005 Doppelangriff: Eine Dame, ein Turm, ein Läufer oder der König bedroht mehrere relevante Ziele
 * gleichzeitig – oder der Zug erzeugt zusammen mit einer abgedeckten Figur zwei Drohungen.
 * (Springer- und Bauerngabeln behandelt TACT-015.)
 */
@Component
public class DoubleAttackRule extends AbstractRule implements TacticalRule {

    public DoubleAttackRule() {
        super("TACT-005", "Doppelangriff",
                "Erkennt Doppelangriffe langschrittiger Figuren und kombinierte Drohungen mehrerer Figuren.",
                List.of("relevante Ziele jeder Figur vor und nach dem Zug", "Wert und Deckung der Ziele",
                        "Folgezüge der Engine-Variante"),
                "Doppelangriffe entstehen oft mit der Dame: Prüfe, ob sie nach einem Zug zwei ungedeckte Figuren oder König und Figur zugleich angreift.");
    }

    @Override
    public List<CoachingFinding> evaluate(PositionContext c) {
        List<CoachingFinding> findings = new ArrayList<>();
        detect(c.before(), c.after(), c.user(), c.move().from(), c.move().to()).ifPresent(targets -> {
            boolean confirmed = MultiTargetDetector.followUpCaptures(c.line(), 0, targets);
            FindingBuilder b = FindingBuilder.of(this).positive()
                    .severity(confirmed ? Severity.HIGH : Severity.MEDIUM)
                    .pieces(c.after(), targets);
            String text = "Nach " + c.line().ply(0).numberedSan() + " bedrohst du gleichzeitig "
                    + MultiTargetDetector.describeTargets(c.after(), targets) + ".";
            if (confirmed) {
                b.verified().explanation(text + " Laut Engine-Variante gewinnt das Material: " + c.line().numbered(0, 3) + ".")
                        .lineEvidence(c.line(), 3, null, null, "Doppelangriff gewinnt Material", Confidence.HIGH);
            } else {
                b.heuristic().explanation(text + " Der Gegner kann voraussichtlich nicht beide Drohungen gleichzeitig parieren.");
            }
            findings.add(b.build());
        });
        if (c.opponentReply().isPresent()) {
            LinePly reply = c.opponentReply().get();
            detect(reply.before(), reply.after(), c.opponent(), reply.move().from(), reply.move().to()).ifPresent(targets -> {
                boolean confirmed = MultiTargetDetector.followUpCaptures(c.line(), 1, targets);
                findings.add(FindingBuilder.of(this).negative().verified()
                        .title("Gegnerischer Doppelangriff")
                        .severity(confirmed ? Severity.HIGH : Severity.MEDIUM)
                        .pieces(reply.after(), targets)
                        .explanation("Dein Zug erlaubt den Doppelangriff " + reply.numberedSan() + ", der "
                                + MultiTargetDetector.describeTargets(reply.after(), targets) + " bedroht."
                                + (confirmed ? " In der Engine-Variante geht dadurch Material verloren: " + c.line().numbered(0, 4) + "." : ""))
                        .lineEvidence(c.line(), confirmed ? 4 : 2, null, null, "Gegnerischer Doppelangriff",
                                confirmed ? Confidence.HIGH : Confidence.MEDIUM)
                        .build());
            });
        }
        return findings;
    }

    /** Liefert die Ziele eines Doppelangriffs der Seite {@code mover} nach dem Zug {@code from}→{@code to}. */
    static java.util.Optional<List<Integer>> detect(Position before, Position after, Color mover, int from, int to) {
        if (after.pieceAt(to) == null) {
            return java.util.Optional.empty();
        }
        boolean forkPiece = MultiTargetDetector.isForkPiece(after.pieceAt(to).kind());
        List<Integer> movedTargets = MultiTargetDetector.allTargets(after, to);
        List<Integer> freshMoved = MultiTargetDetector.newTargets(before, after, from, to);
        if (!forkPiece && movedTargets.size() >= 2 && !freshMoved.isEmpty()) {
            return java.util.Optional.of(movedTargets);
        }
        // Kombinierter Angriff: gezogene Figur plus abgedeckte Linie.
        Set<Integer> combined = new LinkedHashSet<>(freshMoved);
        for (int[] discovered : TacticsToolkit.discoveredAttacks(before, after, mover, from, to)) {
            combined.add(discovered[1]);
        }
        if (combined.size() >= 2 && !freshMoved.isEmpty() && combined.size() > freshMoved.size()) {
            return java.util.Optional.of(List.copyOf(combined));
        }
        return java.util.Optional.empty();
    }
}
