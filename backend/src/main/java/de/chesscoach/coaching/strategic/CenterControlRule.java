package de.chesscoach.coaching.strategic;

import de.chesscoach.coaching.AbstractRule;
import de.chesscoach.coaching.FindingBuilder;
import de.chesscoach.coaching.GermanText;
import de.chesscoach.coaching.PositionContext;
import de.chesscoach.coaching.StrategicRule;
import de.chesscoach.domain.coaching.CoachingFinding;
import de.chesscoach.domain.coaching.Severity;
import de.chesscoach.domain.position.Piece;
import de.chesscoach.domain.position.PieceKind;
import de.chesscoach.domain.position.Squares;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * STRAT-002 Zentrumskontrolle (Lasker: Wer das Zentrum beherrscht, hat mehr Raum für Figurenmanöver).
 * Benennt die konkreten Zentrumsfelder, deren Kontrolle sich ändert, und zentrale Bauernhebel.
 */
@Component
public class CenterControlRule extends AbstractRule implements StrategicRule {

    public CenterControlRule() {
        super("STRAT-002", "Zentrumskontrolle",
                "Bewertet die Veränderung von Kontrolle und Besetzung der Felder d4, e4, d5 und e5 sowie zentrale Bauernhebel.",
                List.of("Angriffe beider Seiten auf d4/e4/d5/e5 vor und nach dem Zug", "Besetzung der Zentrumsfelder",
                        "Angriffe des gezogenen Bauern auf gegnerische Zentralbauern"),
                "Kämpfe um das Zentrum: Bauern auf d4/e4 (bzw. d5/e5) und Figuren, die diese Felder kontrollieren, geben deinen Figuren Raum.");
    }

    @Override
    public boolean matches(PositionContext c) {
        return !c.before().isEndgame() && c.movedPiece() != null;
    }

    @Override
    public List<CoachingFinding> evaluate(PositionContext c) {
        List<CoachingFinding> findings = new ArrayList<>();
        int before = StrategicFeatures.centerControl(c.before(), c.user());
        int after = StrategicFeatures.centerControl(c.after(), c.user());
        int opponentBefore = StrategicFeatures.centerControl(c.before(), c.opponent());
        int opponentAfter = StrategicFeatures.centerControl(c.after(), c.opponent());
        List<Integer> gained = new ArrayList<>();
        List<Integer> lost = new ArrayList<>();
        for (int sq : StrategicFeatures.CENTER) {
            int delta = c.after().attackCount(sq, c.user()) - c.before().attackCount(sq, c.user());
            if (delta > 0) {
                gained.add(sq);
            } else if (delta < 0) {
                lost.add(sq);
            }
        }
        String moved = c.movedPiece().kind().germanName();
        boolean occupiesCenter = StrategicFeatures.CENTER.contains(c.move().to());
        int delta = after - before;
        if (c.movedPiece().kind() == PieceKind.PAWN) {
            for (int target : List.of(c.move().to())) {
                long attacks = c.after().attacksFrom(target);
                for (int sq : StrategicFeatures.CENTER) {
                    Piece p = c.after().pieceAt(sq);
                    if ((attacks & (1L << sq)) != 0 && p != null && p.color() == c.opponent() && p.kind() == PieceKind.PAWN) {
                        findings.add(FindingBuilder.of(this).positive().heuristic().severity(Severity.LOW)
                                .title("Zentraler Bauernhebel")
                                .square(c.move().to()).square(sq)
                                .explanation("Der Bauer auf " + Squares.name(c.move().to()) + " greift den gegnerischen Zentralbauern auf "
                                        + Squares.name(sq) + " an. Dieser Hebel stellt das gegnerische Zentrum in Frage.")
                                .build());
                    }
                }
            }
        }
        if (delta >= 2 || (occupiesCenter && delta >= 1)) {
            findings.add(FindingBuilder.of(this).positive().heuristic().severity(delta >= 3 ? Severity.MEDIUM : Severity.LOW)
                    .squares(gained.isEmpty() ? List.of(c.move().to()) : gained)
                    .explanation((occupiesCenter ? (c.movedPiece().kind().feminine() ? "Deine " : "Dein ") + moved + " besetzt das Zentrumsfeld " + Squares.name(c.move().to()) + ". " : "")
                            + (gained.isEmpty() ? "" : "Du kontrollierst jetzt zusätzlich " + GermanText.squares(gained) + ". ")
                            + "Zentrumskontrolle: du " + before + " → " + after + ", Gegner " + opponentAfter + ".")
                    .build());
        } else if (delta <= -2) {
            findings.add(FindingBuilder.of(this).negative().heuristic().severity(delta <= -3 ? Severity.MEDIUM : Severity.LOW)
                    .title("Weniger Einfluss im Zentrum")
                    .squares(lost).square(c.move().from())
                    .explanation((c.movedPiece().kind().feminine() ? "Deine " : "Dein ") + moved + " verlässt " + Squares.name(c.move().from()) + " und kontrolliert "
                            + (lost.isEmpty() ? "weniger Zentrumsfelder" : GermanText.squares(lost) + " nicht mehr")
                            + ". Zentrumskontrolle: du " + before + " → " + after + ", Gegner " + opponentBefore + " → " + opponentAfter + ".")
                    .build());
        }
        return findings;
    }
}
