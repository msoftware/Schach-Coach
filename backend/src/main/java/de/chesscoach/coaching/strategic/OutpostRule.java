package de.chesscoach.coaching.strategic;

import de.chesscoach.coaching.AbstractRule;
import de.chesscoach.coaching.FindingBuilder;
import de.chesscoach.coaching.GermanText;
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
import java.util.TreeSet;

/**
 * STRAT-006 Schwache Felder und Vorposten (Lasker: Ein Feld, das von gegnerischen Bauern nicht mehr angegriffen
 * werden kann, ist ein dauerhafter Stützpunkt – besonders für Springer).
 */
@Component
public class OutpostRule extends AbstractRule implements StrategicRule {

    public OutpostRule() {
        super("STRAT-006", "Vorposten und schwache Felder",
                "Erkennt Springer- und Läuferzüge auf Vorposten sowie durch Bauernzüge neu entstandene schwache Felder im eigenen Lager.",
                List.of("Felder, die gegnerische Bauern nie mehr angreifen können", "Deckung durch eigene Bauern",
                        "schwache Felder im eigenen Lager vor und nach einem Bauernzug", "gegnerische Leichtfiguren, die diese Felder nutzen können"),
                "Suche stabile Vorposten für deine Springer: Felder in der gegnerischen Hälfte, die kein gegnerischer Bauer mehr angreifen kann.");
    }

    @Override
    public boolean matches(PositionContext c) {
        return c.movedPiece() != null;
    }

    @Override
    public List<CoachingFinding> evaluate(PositionContext c) {
        List<CoachingFinding> findings = new ArrayList<>();
        PieceKind kind = c.movedPiece().kind();
        int to = c.move().to();
        if (kind.isMinor() && isOutpost(c.after(), to, c.user())) {
            findings.add(FindingBuilder.of(this).positive().heuristic()
                    .severity(kind == PieceKind.KNIGHT ? Severity.MEDIUM : Severity.LOW)
                    .title("Vorposten besetzt")
                    .square(to)
                    .explanation((kind.feminine() ? "Deine " : "Dein ") + kind.germanName() + " steht auf " + Squares.name(to)
                            + " auf einem Vorposten: Das Feld wird von einem eigenen Bauern gedeckt und kann von keinem gegnerischen "
                            + "Bauern mehr angegriffen werden. Die Figur ist dort schwer zu vertreiben.")
                    .build());
        }
        if (kind == PieceKind.PAWN && !c.after().isEndgame()) {
            Set<Integer> before = new PawnStructure(c.before(), c.user()).holes();
            Set<Integer> fresh = new TreeSet<>(new PawnStructure(c.after(), c.user()).holes());
            fresh.removeAll(before);
            fresh.removeIf(sq -> !relevantHole(c.after(), sq, c.user()));
            if (!fresh.isEmpty()) {
                List<Integer> users = new ArrayList<>(c.after().squaresOf(c.opponent(), PieceKind.KNIGHT));
                users.addAll(c.after().squaresOf(c.opponent(), PieceKind.BISHOP));
                findings.add(FindingBuilder.of(this).negative().heuristic().severity(Severity.LOW)
                        .title("Schwache Felder im eigenen Lager")
                        .squares(fresh).square(c.move().to())
                        .explanation("Nach " + c.move().san() + " kann kein eigener Bauer mehr " + GermanText.squares(fresh)
                                + " kontrollieren. Gegnerische Figuren"
                                + (users.isEmpty() ? "" : " wie " + GermanText.join(users.stream().map(sq -> GermanText.pieceOn(c.after(), sq)).toList()))
                                + " könnten sich dort festsetzen.")
                        .build());
            }
        }
        return findings;
    }

    /** Vorposten: in der gegnerischen Hälfte (4.–6. Reihe), von eigenem Bauern gedeckt, nie von gegnerischen Bauern angreifbar. */
    public static boolean isOutpost(Position position, int square, Color owner) {
        int rel = owner.relativeRank(Squares.rank(square));
        if (rel < 3 || rel > 5) {
            return false;
        }
        PawnStructure own = new PawnStructure(position, owner);
        PawnStructure enemy = new PawnStructure(position, owner.opposite());
        return own.pawnControls(owner, square) && !enemy.pawnCanEverAttack(owner.opposite(), square);
    }

    /** Relevante Löcher: im Zentrum (c–f) oder vor dem eigenen König. */
    private static boolean relevantHole(Position position, int square, Color owner) {
        int file = Squares.file(square);
        int king = position.kingSquare(owner);
        return (file >= 2 && file <= 5) || Math.abs(file - Squares.file(king)) <= 1;
    }
}
