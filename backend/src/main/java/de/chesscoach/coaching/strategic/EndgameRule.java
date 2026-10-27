package de.chesscoach.coaching.strategic;

import de.chesscoach.coaching.AbstractRule;
import de.chesscoach.coaching.FindingBuilder;
import de.chesscoach.coaching.PositionContext;
import de.chesscoach.coaching.StrategicRule;
import de.chesscoach.domain.coaching.CoachingFinding;
import de.chesscoach.domain.coaching.Severity;
import de.chesscoach.domain.position.Color;
import de.chesscoach.domain.position.PawnStructure;
import de.chesscoach.domain.position.PieceKind;
import de.chesscoach.domain.position.Position;
import de.chesscoach.domain.position.Squares;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * STRAT-012 Endspielprinzipien (Lasker: Im Endspiel wird der König zur aktiven Figur; Freibauern entscheiden;
 * Türme gehören hinter Freibauern; im Bauernendspiel ist die Opposition maßgeblich). Für präzise Endspielbewertungen
 * ist die Engine maßgeblich – die Regel erklärt die unterstützenden Merkmale.
 */
@Component
public class EndgameRule extends AbstractRule implements StrategicRule {

    public EndgameRule() {
        super("STRAT-012", "Endspielprinzipien",
                "Bewertet Königsaktivität, Unterstützung von Freibauern, Turm hinter dem Freibauern, Opposition und Umwandlungsrennen.",
                List.of("Spielphase", "Zentrumsabstand des Königs vor und nach dem Zug", "Freibauern beider Seiten",
                        "Turmstellung relativ zu Freibauern", "Königsstellung im reinen Bauernendspiel", "Umwandlungsdistanzen"),
                "Im Endspiel gilt: König aktivieren, Freibauern schaffen und unterstützen, Türme hinter die Freibauern stellen.");
    }

    @Override
    public boolean matches(PositionContext c) {
        return c.after().isEndgame() && c.movedPiece() != null;
    }

    @Override
    public List<CoachingFinding> evaluate(PositionContext c) {
        List<CoachingFinding> findings = new ArrayList<>();
        PieceKind kind = c.movedPiece().kind();
        Color user = c.user();
        if (kind == PieceKind.KING) {
            int before = Squares.centerDistance(c.move().from());
            int after = Squares.centerDistance(c.move().to());
            Set<Integer> passed = new PawnStructure(c.after(), user).passed();
            boolean supportsPassed = passed.stream().anyMatch(p -> Squares.distance(p, c.move().to()) <= 1
                    && Squares.distance(p, c.move().from()) > 1);
            if (after < before || supportsPassed) {
                findings.add(FindingBuilder.of(this).positive().heuristic().severity(Severity.MEDIUM)
                        .title("König aktiviert")
                        .square(c.move().to())
                        .explanation("Dein König geht nach " + Squares.name(c.move().to())
                                + (supportsPassed ? " und unterstützt deinen Freibauern." : " in Richtung Zentrum.")
                                + " Im Endspiel ist der aktive König eine wichtige Angriffs- und Verteidigungsfigur.")
                        .build());
            } else if (after > before && !c.before().isInCheck(user)) {
                findings.add(FindingBuilder.of(this).negative().heuristic().severity(Severity.LOW)
                        .title("König passiver")
                        .square(c.move().to())
                        .explanation("Dein König entfernt sich mit " + c.move().san() + " vom Zentrum. Im Endspiel sollte der König "
                                + "möglichst aktiv stehen.")
                        .build());
            }
            if (isOpposition(c.after(), user)) {
                findings.add(FindingBuilder.of(this).positive().verified().severity(Severity.MEDIUM)
                        .title("Opposition")
                        .square(c.after().kingSquare(user)).square(c.after().kingSquare(c.opponent()))
                        .explanation("Die Könige stehen sich mit einem Feld Abstand gegenüber und der Gegner ist am Zug: Du hast die "
                                + "Opposition. Der gegnerische König muss ausweichen und dir Felder überlassen.")
                        .build());
            }
        }
        if (kind == PieceKind.ROOK) {
            for (Color owner : Color.values()) {
                for (int pawn : new PawnStructure(c.after(), owner).passed()) {
                    if (Squares.file(pawn) == Squares.file(c.move().to()) && Squares.file(c.move().from()) != Squares.file(pawn)
                            && behind(c.move().to(), pawn, owner) && Position.between(c.move().to(), pawn).stream().allMatch(c.after()::isEmpty)) {
                        findings.add(FindingBuilder.of(this).positive().heuristic().severity(Severity.MEDIUM)
                                .title("Turm hinter dem Freibauern")
                                .square(c.move().to()).square(pawn)
                                .explanation("Dein Turm steht jetzt hinter dem " + (owner == user ? "eigenen" : "gegnerischen")
                                        + " Freibauern auf " + Squares.name(pawn) + ". Von dort "
                                        + (owner == user ? "unterstützt er dessen Vormarsch" : "hält er ihn auf")
                                        + ", und seine Wirkung wächst mit jedem Schritt des Bauern.")
                                .build());
                    }
                }
            }
        }
        if (kind == PieceKind.PAWN) {
            Set<Integer> ownPassed = new PawnStructure(c.after(), user).passed();
            Set<Integer> theirPassed = new PawnStructure(c.after(), c.opponent()).passed();
            if (ownPassed.contains(c.move().to()) && !theirPassed.isEmpty()) {
                int ownSteps = 7 - user.relativeRank(Squares.rank(c.move().to()));
                int theirSteps = theirPassed.stream().mapToInt(p -> 7 - c.opponent().relativeRank(Squares.rank(p))).min().orElse(8);
                findings.add(FindingBuilder.of(this).neutral().heuristic().severity(Severity.MEDIUM)
                        .title("Umwandlungsrennen")
                        .square(c.move().to()).squares(theirPassed)
                        .explanation("Beide Seiten haben Freibauern: Dein Bauer braucht noch " + ownSteps + " Schritte zur Umwandlung, "
                                + "der schnellste gegnerische " + theirSteps + ". Der Gegner ist am Zug. Für das genaue Ergebnis ist die "
                                + "Engine-Bewertung maßgeblich.")
                        .build());
            }
        }
        return findings;
    }

    static boolean behind(int rook, int pawn, Color pawnOwner) {
        return pawnOwner.relativeRank(Squares.rank(rook)) < pawnOwner.relativeRank(Squares.rank(pawn));
    }

    /** Direkte Opposition im reinen Bauernendspiel: Könige mit einem Feld Abstand auf Linie/Reihe, Gegner am Zug. */
    static boolean isOpposition(Position p, Color user) {
        if (p.nonPawnMaterial(Color.WHITE) > 0 || p.nonPawnMaterial(Color.BLACK) > 0 || p.sideToMove() == user) {
            return false;
        }
        int a = p.kingSquare(user);
        int b = p.kingSquare(user.opposite());
        int df = Math.abs(Squares.file(a) - Squares.file(b));
        int dr = Math.abs(Squares.rank(a) - Squares.rank(b));
        return (df == 0 && dr == 2) || (dr == 0 && df == 2);
    }
}
