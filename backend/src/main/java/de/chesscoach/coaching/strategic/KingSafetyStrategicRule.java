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

/**
 * STRAT-003 Königssicherheit (Lasker: Der König gehört im Mittelspiel hinter eine intakte Bauernkette; jede
 * Lockerung der Bauern vor dem König schafft Angriffslinien). Ein nicht rochierter König gilt nicht pauschal als
 * schlecht – bewertet wird nur der Verlust der Rochade ohne Rochade bzw. konkrete offene Linien und Angreifer.
 */
@Component
public class KingSafetyStrategicRule extends AbstractRule implements StrategicRule {

    public KingSafetyStrategicRule() {
        super("STRAT-003", "Königssicherheit",
                "Bewertet Rochade, Verlust des Rochaderechts, Bauernschutz und offene Linien vor dem eigenen König.",
                List.of("Rochaderechte vor und nach dem Zug", "Bauernschutz vor dem König", "offene/halboffene Linien neben dem König",
                        "gegnerische Schwerfiguren und Läufer auf diesen Linien", "Damen auf dem Brett"),
                "Bringe deinen König rechtzeitig in Sicherheit und schwäche seine Bauerndeckung nur, wenn es einen konkreten Grund gibt.");
    }

    @Override
    public boolean matches(PositionContext c) {
        return c.movedPiece() != null && !c.after().isEndgame();
    }

    @Override
    public List<CoachingFinding> evaluate(PositionContext c) {
        List<CoachingFinding> findings = new ArrayList<>();
        Color user = c.user();
        boolean castling = c.movedPiece().kind() == PieceKind.KING
                && Math.abs(Squares.file(c.move().to()) - Squares.file(c.move().from())) == 2;
        if (castling) {
            boolean kingSide = Squares.file(c.move().to()) == 6;
            findings.add(FindingBuilder.of(this).positive().heuristic().severity(Severity.MEDIUM)
                    .title("Rochade")
                    .square(c.move().to())
                    .explanation("Mit der " + (kingSide ? "kurzen" : "langen") + " Rochade bringst du deinen König hinter die Bauern auf "
                            + Squares.name(c.move().to()) + " und bringst gleichzeitig einen Turm ins Spiel.")
                    .build());
            return findings;
        }
        boolean opponentHasQueen = StrategicFeatures.hasQueen(c.after(), c.opponent());
        if (c.before().canCastle(user) && !c.after().canCastle(user) && opponentHasQueen) {
            findings.add(FindingBuilder.of(this).negative().heuristic().severity(Severity.MEDIUM)
                    .title("Rochaderecht verloren")
                    .square(c.after().kingSquare(user))
                    .explanation("Durch " + c.move().san() + " kannst du nicht mehr rochieren. Solange die gegnerische Dame auf dem Brett ist, "
                            + "muss dein König " + Squares.name(c.after().kingSquare(user))
                            + " jetzt auf andere Weise geschützt werden – besonders, wenn sich Linien im Zentrum öffnen.")
                    .build());
        }
        if (c.movedPiece().kind() == PieceKind.PAWN && StrategicFeatures.kingOnWing(c.before(), user)) {
            List<Integer> shieldBefore = StrategicFeatures.pawnShield(c.before(), user);
            List<Integer> shieldAfter = StrategicFeatures.pawnShield(c.after(), user);
            if (shieldBefore.contains(c.move().from()) && shieldAfter.size() < shieldBefore.size() || weakensShield(c)) {
                List<String> threats = linesTowardKing(c.after(), user);
                findings.add(FindingBuilder.of(this).negative().heuristic()
                        .severity(threats.isEmpty() ? Severity.LOW : Severity.MEDIUM)
                        .title("Bauernschutz des Königs gelockert")
                        .square(c.move().from()).square(c.after().kingSquare(user))
                        .explanation("Der Bauernzug " + c.move().san() + " lockert die Bauernstellung vor deinem König auf "
                                + Squares.name(c.after().kingSquare(user)) + "."
                                + (threats.isEmpty() ? " Derzeit sind keine gegnerischen Figuren direkt auf diese Schwächen gerichtet."
                                : " Konkret: " + GermanText.join(threats) + "."))
                        .build());
            }
        }
        return findings;
    }

    private static boolean weakensShield(PositionContext c) {
        int king = c.before().kingSquare(c.user());
        return Math.abs(Squares.file(c.move().from()) - Squares.file(king)) <= 1
                && c.user().relativeRank(Squares.rank(c.move().from())) == 1
                && c.user().relativeRank(Squares.rank(c.move().to())) >= 3;
    }

    /** Konkrete Angriffslinien gegnerischer Figuren in Richtung des Königs. */
    static List<String> linesTowardKing(Position position, Color color) {
        List<String> result = new ArrayList<>();
        int king = position.kingSquare(color);
        PawnStructure own = new PawnStructure(position, color);
        for (int df = -1; df <= 1; df++) {
            int file = Squares.file(king) + df;
            if (file < 0 || file > 7) {
                continue;
            }
            boolean noOwnPawn = position.squaresOf(color, PieceKind.PAWN).stream().noneMatch(p -> Squares.file(p) == file);
            if (!noOwnPawn) {
                continue;
            }
            for (int sq : position.squaresOf(color.opposite())) {
                PieceKind kind = position.pieceAt(sq).kind();
                if ((kind == PieceKind.ROOK || kind == PieceKind.QUEEN) && Squares.file(sq) == file) {
                    result.add("die " + (own.isFileOpen(file) ? "offene" : "halboffene") + " " + Squares.fileName(file)
                            + "-Linie wird von " + GermanText.opponentPieceDat(position, sq) + " kontrolliert");
                }
            }
        }
        for (int zoneSquare : de.chesscoach.coaching.TacticsToolkit.kingZone(position, color)) {
            for (int attacker : position.attackersOf(zoneSquare, color.opposite())) {
                PieceKind kind = position.pieceAt(attacker).kind();
                if ((kind == PieceKind.BISHOP || kind == PieceKind.QUEEN) && Squares.distance(attacker, zoneSquare) >= 2
                        && Squares.file(attacker) != Squares.file(zoneSquare) && Squares.rank(attacker) != Squares.rank(zoneSquare)) {
                    String text = GermanText.opponentPieceOn(position, attacker) + " zielt diagonal auf " + Squares.name(zoneSquare);
                    if (!result.contains(text)) {
                        result.add(text);
                    }
                }
            }
        }
        return result.size() > 3 ? result.subList(0, 3) : result;
    }
}
