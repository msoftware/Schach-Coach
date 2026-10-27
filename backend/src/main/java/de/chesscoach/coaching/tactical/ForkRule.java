package de.chesscoach.coaching.tactical;

import de.chesscoach.coaching.AbstractRule;
import de.chesscoach.coaching.FindingBuilder;
import de.chesscoach.coaching.GermanText;
import de.chesscoach.coaching.PositionContext;
import de.chesscoach.coaching.TacticalRule;
import de.chesscoach.domain.coaching.CoachingFinding;
import de.chesscoach.domain.coaching.Confidence;
import de.chesscoach.domain.coaching.Severity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * TACT-015 Gabel: Ein Springer oder Bauer greift zwei oder mehr relevante Ziele gleichzeitig an.
 * Geprüft werden der eigene Zug (positiv) und die gegnerische Antwort der Engine-Variante (negativ).
 */
@Component
public class ForkRule extends AbstractRule implements TacticalRule {

    public ForkRule() {
        super("TACT-015", "Gabel",
                "Erkennt Springer- und Bauerngabeln durch den eigenen Zug oder die gegnerische Antwort.",
                List.of("Angriffe der gezogenen Figur", "Wert und Deckung der Ziele", "Folgezug der Engine-Variante"),
                "Achte auf Felder, von denen ein gegnerischer Springer zwei deiner Figuren gleichzeitig angreifen kann.");
    }

    @Override
    public List<CoachingFinding> evaluate(PositionContext c) {
        List<CoachingFinding> findings = new ArrayList<>();
        if (c.movedPiece() != null && MultiTargetDetector.isForkPiece(c.movedPiece().kind())) {
            List<Integer> targets = MultiTargetDetector.allTargets(c.after(), c.move().to());
            List<Integer> fresh = MultiTargetDetector.newTargets(c.before(), c.after(), c.move().from(), c.move().to());
            if (targets.size() >= 2 && !fresh.isEmpty()) {
                boolean confirmed = MultiTargetDetector.followUpCaptures(c.line(), 0, targets);
                FindingBuilder b = FindingBuilder.of(this).positive()
                        .severity(confirmed ? Severity.HIGH : Severity.MEDIUM)
                        .piece(c.after(), c.move().to()).pieces(c.after(), targets);
                String text = GermanText.yourPieceOn(c.after(), c.move().to()) + " greift gleichzeitig "
                        + MultiTargetDetector.describeTargets(c.after(), targets) + " an (Gabel).";
                if (confirmed) {
                    b.verified().explanation(text + " Laut Engine-Variante gewinnst du dadurch Material: "
                                    + c.line().numbered(0, 3) + ".")
                            .lineEvidence(c.line(), 3, null, null, "Gabel gewinnt Material", Confidence.HIGH);
                } else {
                    b.heuristic().explanation(text + " Der Gegner kann nicht beide Ziele zugleich in Sicherheit bringen, "
                            + "sofern er keine stärkere Gegendrohung hat.");
                }
                findings.add(b.build());
            }
        }
        c.opponentReply().ifPresent(reply -> {
            if (reply.moved() == null || !MultiTargetDetector.isForkPiece(reply.moved().kind())) {
                return;
            }
            List<Integer> targets = MultiTargetDetector.allTargets(reply.after(), reply.move().to());
            List<Integer> fresh = MultiTargetDetector.newTargets(reply.before(), reply.after(), reply.move().from(), reply.move().to());
            if (targets.size() >= 2 && !fresh.isEmpty()) {
                boolean confirmed = MultiTargetDetector.followUpCaptures(c.line(), 1, targets);
                FindingBuilder b = FindingBuilder.of(this).negative()
                        .title("Gegnerische Gabel")
                        .severity(confirmed ? Severity.HIGH : Severity.MEDIUM)
                        .piece(reply.after(), reply.move().to()).pieces(reply.after(), targets);
                String text = "Nach deinem Zug ist die Gabel " + reply.numberedSan() + " möglich: "
                        + GermanText.opponentPieceOn(reply.after(), reply.move().to()) + " greift "
                        + MultiTargetDetector.describeTargets(reply.after(), targets) + " an.";
                if (confirmed) {
                    b.verified().explanation(text + " Die Engine-Variante gewinnt damit Material: " + c.line().numbered(0, 4) + ".")
                            .lineEvidence(c.line(), 4, null, null, "Gegnerische Gabel gewinnt Material", Confidence.HIGH);
                } else {
                    b.verified().explanation(text + " Diese Antwort stammt aus der Engine-Hauptvariante.")
                            .lineEvidence(c.line(), 2, null, null, "Gegnerische Gabel", Confidence.MEDIUM);
                }
                findings.add(b.build());
            }
        });
        return findings;
    }
}
