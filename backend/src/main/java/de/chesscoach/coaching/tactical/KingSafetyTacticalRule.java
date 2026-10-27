package de.chesscoach.coaching.tactical;

import de.chesscoach.coaching.AbstractRule;
import de.chesscoach.coaching.FindingBuilder;
import de.chesscoach.coaching.GermanText;
import de.chesscoach.coaching.LinePly;
import de.chesscoach.coaching.PositionContext;
import de.chesscoach.coaching.TacticalRule;
import de.chesscoach.coaching.TacticsToolkit;
import de.chesscoach.domain.coaching.CoachingFinding;
import de.chesscoach.domain.coaching.Confidence;
import de.chesscoach.domain.coaching.Severity;
import de.chesscoach.domain.position.Color;
import de.chesscoach.domain.position.PieceKind;
import de.chesscoach.domain.position.Position;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * TACT-012 Königssicherheit (taktisch): Der Zug öffnet Linien zum eigenen König, über die gegnerische
 * langschrittige Figuren die Königszone angreifen. Bestätigt durch Schachgebote in der Engine-Variante.
 */
@Component
public class KingSafetyTacticalRule extends AbstractRule implements TacticalRule {

    public KingSafetyTacticalRule() {
        super("TACT-012", "Geöffnete Linien zum König",
                "Erkennt Züge, die gegnerischen Langschrittlern neuen Zugriff auf die eigene Königszone geben.",
                List.of("Königszone (König und Nachbarfelder)", "Angriffe gegnerischer Läufer, Türme und Damen vor und nach dem Zug",
                        "Schachgebote in der Engine-Variante"),
                "Ziehe Bauern vor dem eigenen König nur mit gutem Grund: Jede geöffnete Linie kann zum Angriffsweg werden.");
    }

    @Override
    public boolean matches(PositionContext c) {
        return c.movedPiece() != null && c.movedPiece().kind() != PieceKind.KING;
    }

    @Override
    public List<CoachingFinding> evaluate(PositionContext c) {
        List<CoachingFinding> findings = new ArrayList<>();
        Set<Integer> newlyAttacked = new LinkedHashSet<>();
        Set<Integer> attackers = new LinkedHashSet<>();
        List<Integer> zone = TacticsToolkit.kingZone(c.after(), c.user());
        for (int sq : zone) {
            for (int attacker : sliderAttackers(c.after(), sq, c.opponent())) {
                if (!c.before().attacks(attacker, sq) && (sq == c.move().from() || Position.between(attacker, sq).contains(c.move().from()))) {
                    newlyAttacked.add(sq);
                    attackers.add(attacker);
                }
            }
        }
        // Relevant sind zwei neue Felder, oder ein Feld, das eine Schwerfigur (Turm/Dame) neu erreicht.
        boolean heavyAttacker = attackers.stream().map(a -> c.after().pieceAt(a).kind())
                .anyMatch(k -> k == PieceKind.ROOK || k == PieceKind.QUEEN);
        if (newlyAttacked.isEmpty() || newlyAttacked.size() < 2 && !heavyAttacker) {
            return findings;
        }
        boolean checksInLine = false;
        for (int i = 1; i < Math.min(c.line().size(), 6); i++) {
            LinePly ply = c.line().ply(i);
            if (ply.mover() == c.opponent() && ply.move().check()) {
                checksInLine = true;
                break;
            }
        }
        FindingBuilder b = FindingBuilder.of(this).negative()
                .severity(checksInLine ? Severity.HIGH : Severity.MEDIUM)
                .squares(newlyAttacked).pieces(c.after(), attackers).square(c.move().from())
                .explanation("Vorsicht mit deinem König: Dein Zug öffnet einen Weg über " + de.chesscoach.domain.position.Squares.name(c.move().from())
                        + ". Jetzt kann " + GermanText.join(attackers.stream().map(a -> GermanText.opponentPieceOn(c.after(), a)).toList())
                        + " die Felder " + GermanText.squares(newlyAttacked) + " direkt neben deinem König erreichen."
                        + (checksInLine ? " Der Computer zeigt, dass der Gegner das für Schachgebote nutzt." : ""));
        if (checksInLine) {
            b.verified().lineEvidence(c.line(), 5, null, null, "Angriff auf den König", Confidence.MEDIUM);
        } else {
            b.heuristic();
        }
        findings.add(b.build());
        return findings;
    }

    private static List<Integer> sliderAttackers(Position position, int square, Color by) {
        return position.attackersOf(square, by).stream()
                .filter(a -> position.pieceAt(a).kind().isSlider())
                .toList();
    }
}
