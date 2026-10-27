package de.chesscoach.coaching.tactical;

import de.chesscoach.coaching.AbstractRule;
import de.chesscoach.coaching.FindingBuilder;
import de.chesscoach.coaching.GermanText;
import de.chesscoach.coaching.LinePly;
import de.chesscoach.coaching.PositionContext;
import de.chesscoach.coaching.TacticalRule;
import de.chesscoach.domain.coaching.CoachingFinding;
import de.chesscoach.domain.coaching.Confidence;
import de.chesscoach.domain.coaching.Severity;
import de.chesscoach.domain.position.Color;
import de.chesscoach.domain.position.LineMotifs;
import de.chesscoach.domain.position.Position;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * TACT-007 Spieß: Eine wertvolle Figur (oder der König) wird angegriffen und muss ausweichen, wodurch
 * dahinterliegendes Material verloren geht.
 */
@Component
public class SkewerRule extends AbstractRule implements TacticalRule {

    public SkewerRule() {
        super("TACT-007", "Spieß",
                "Erkennt Spieße durch den eigenen Zug und durch die gegnerische Antwort.",
                List.of("Linien langschrittiger Figuren", "Werte von vorderer und hinterer Figur",
                        "Deckung der hinteren Figur", "Folgezüge der Engine-Variante"),
                "Stelle König und Dame oder Dame und Turm nicht ungeschützt auf dieselbe Linie oder Diagonale.");
    }

    @Override
    public List<CoachingFinding> evaluate(PositionContext c) {
        List<CoachingFinding> findings = new ArrayList<>();
        for (LineMotifs.Motif skewer : newSkewers(c.before(), c.after(), c.user())) {
            if (skewer.attacker() != c.move().to()) {
                continue;
            }
            boolean confirmed = c.line().size() > 2 && c.line().ply(2).isCapture() && c.line().ply(2).move().to() == skewer.back();
            FindingBuilder b = FindingBuilder.of(this).positive()
                    .severity(confirmed ? Severity.HIGH : Severity.MEDIUM)
                    .piece(c.after(), skewer.attacker()).piece(c.after(), skewer.front()).piece(c.after(), skewer.back())
                    .explanation(GermanText.capitalize(GermanText.yourPieceOn(c.after(), skewer.attacker())) + " spießt "
                            + GermanText.opponentPieceAcc(c.after(), skewer.front()) + " auf: Weicht sie aus, wird "
                            + GermanText.opponentPieceOn(c.after(), skewer.back()) + " dahinter angreifbar."
                            + (confirmed ? " Die Engine-Variante bestätigt den Gewinn: " + c.line().numbered(0, 3) + "." : ""));
            if (confirmed) {
                b.verified().lineEvidence(c.line(), 3, null, null, "Spieß gewinnt Material", Confidence.HIGH);
            } else {
                b.heuristic();
            }
            findings.add(b.build());
        }
        if (c.opponentReply().isPresent()) {
            LinePly reply = c.opponentReply().get();
            for (LineMotifs.Motif skewer : newSkewers(reply.before(), reply.after(), c.opponent())) {
                if (skewer.attacker() != reply.move().to()) {
                    continue;
                }
                findings.add(FindingBuilder.of(this).negative().verified()
                        .title("Gegnerischer Spieß")
                        .severity(Severity.HIGH)
                        .piece(reply.after(), skewer.attacker()).piece(reply.after(), skewer.front()).piece(reply.after(), skewer.back())
                        .explanation("Dein Zug erlaubt den Spieß " + reply.numberedSan() + ": "
                                + GermanText.yourPieceOn(reply.after(), skewer.front()) + " muss ausweichen, danach ist "
                                + GermanText.yourPieceOn(reply.after(), skewer.back()) + " angreifbar.")
                        .lineEvidence(c.line(), 4, null, null, "Gegnerischer Spieß", Confidence.MEDIUM)
                        .build());
            }
        }
        return findings;
    }

    private static List<LineMotifs.Motif> newSkewers(Position before, Position after, Color attacker) {
        List<LineMotifs.Motif> old = LineMotifs.find(before, attacker);
        return LineMotifs.find(after, attacker).stream()
                .filter(m -> m.type() == LineMotifs.Type.SKEWER)
                .filter(m -> old.stream().noneMatch(o -> o.type() == m.type() && o.front() == m.front() && o.back() == m.back()))
                .toList();
    }
}
