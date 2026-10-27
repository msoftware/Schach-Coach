package de.chesscoach.coaching.strategic;

import de.chesscoach.coaching.AbstractRule;
import de.chesscoach.coaching.FindingBuilder;
import de.chesscoach.coaching.PositionContext;
import de.chesscoach.coaching.StrategicRule;
import de.chesscoach.domain.coaching.CoachingFinding;
import de.chesscoach.domain.coaching.Severity;
import de.chesscoach.domain.position.PieceKind;
import de.chesscoach.domain.position.Squares;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * STRAT-004 Figurenaktivität und Mobilität (Lasker: Der Wert einer Figur hängt von ihrer Wirkung ab; eine Figur
 * ohne Bewegungsfreiheit ist weniger wert als ihr nomineller Materialwert).
 */
@Component
public class ActivityRule extends AbstractRule implements StrategicRule {

    private static final int THRESHOLD = 3;

    public ActivityRule() {
        super("STRAT-004", "Figurenaktivität",
                "Bewertet die Mobilitätsänderung der gezogenen Figur, Randspringer und durch eigene Bauern blockierte Läufer.",
                List.of("Pseudomobilität der gezogenen Figur vor und nach dem Zug", "Randlage von Springern",
                        "Mobilität eigener Läufer vor und nach einem Bauernzug", "Gesamtmobilität beider Seiten"),
                "Stelle deine Figuren dorthin, wo sie viele wichtige Felder kontrollieren; eine passive Figur ist eine halbe Figur.");
    }

    @Override
    public boolean matches(PositionContext c) {
        return c.movedPiece() != null && c.movedPiece().kind() != PieceKind.KING;
    }

    @Override
    public List<CoachingFinding> evaluate(PositionContext c) {
        List<CoachingFinding> findings = new ArrayList<>();
        PieceKind kind = c.movedPiece().kind();
        if (kind == PieceKind.PAWN) {
            for (int bishop : c.after().squaresOf(c.user(), PieceKind.BISHOP)) {
                int before = c.before().mobility(bishop);
                int after = c.after().mobility(bishop);
                if (before - after >= 2 && after <= 3 && c.after().attacks(bishop, c.move().to())) {
                    findings.add(FindingBuilder.of(this).negative().heuristic().severity(Severity.LOW)
                            .title("Eigener Läufer blockiert")
                            .piece(c.after(), bishop).square(c.move().to())
                            .explanation("Der Bauer auf " + Squares.name(c.move().to()) + " verstellt deinem Läufer auf "
                                    + Squares.name(bishop) + " die Diagonale: Er hat nur noch " + after + " statt " + before
                                    + " Felder zur Verfügung.")
                            .build());
                }
            }
            return findings;
        }
        int before = c.before().mobility(c.move().from());
        int after = c.after().mobility(c.move().to());
        String piece = (kind.feminine() ? "Deine " : "Dein ") + kind.germanName();
        if (kind == PieceKind.KNIGHT && (Squares.file(c.move().to()) == 0 || Squares.file(c.move().to()) == 7)
                && !c.move().capture() && !c.after().isEndgame()) {
            findings.add(FindingBuilder.of(this).negative().heuristic().severity(Severity.LOW)
                    .title("Springer am Rand")
                    .square(c.move().to())
                    .explanation("Dein Springer steht auf " + Squares.name(c.move().to()) + " am Brettrand und kontrolliert dort nur "
                            + after + " Felder. Ein zentraler Springer wirkt in mehr Richtungen.")
                    .build());
        } else if (after - before >= THRESHOLD) {
            findings.add(FindingBuilder.of(this).positive().heuristic().severity(after - before >= 5 ? Severity.MEDIUM : Severity.LOW)
                    .title("Figur aktiviert")
                    .square(c.move().to())
                    .explanation(piece + " kontrolliert auf " + Squares.name(c.move().to()) + " " + after + " Felder statt bisher "
                            + before + " auf " + Squares.name(c.move().from()) + ".")
                    .build());
        } else if (before - after >= THRESHOLD && !c.move().capture() && !c.move().check()) {
            findings.add(FindingBuilder.of(this).negative().heuristic().severity(before - after >= 5 ? Severity.MEDIUM : Severity.LOW)
                    .title("Figur passiver gestellt")
                    .square(c.move().from()).square(c.move().to())
                    .explanation(piece + " kontrolliert auf " + Squares.name(c.move().to()) + " nur noch " + after + " Felder statt "
                            + before + " auf " + Squares.name(c.move().from()) + ". Prüfe, ob es ein aktiveres Feld gibt.")
                    .build());
        }
        int totalBefore = StrategicFeatures.totalMobility(c.before(), c.user());
        int totalAfter = StrategicFeatures.totalMobility(c.after(), c.user());
        if (findings.isEmpty() && totalAfter - totalBefore >= 6) {
            findings.add(FindingBuilder.of(this).positive().heuristic().severity(Severity.LOW)
                    .title("Mehr Bewegungsfreiheit")
                    .square(c.move().to())
                    .explanation("Nach " + c.move().san() + " haben deine Figuren zusammen " + totalAfter + " statt " + totalBefore
                            + " erreichbare Felder.")
                    .build());
        }
        return findings;
    }
}
