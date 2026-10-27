package de.chesscoach.coaching.strategic;

import de.chesscoach.coaching.AbstractRule;
import de.chesscoach.coaching.FindingBuilder;
import de.chesscoach.coaching.PositionContext;
import de.chesscoach.coaching.StrategicRule;
import de.chesscoach.domain.coaching.CoachingFinding;
import de.chesscoach.domain.coaching.Severity;
import de.chesscoach.domain.position.PawnStructure;
import de.chesscoach.domain.position.Piece;
import de.chesscoach.domain.position.PieceKind;
import de.chesscoach.domain.position.Squares;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * STRAT-007 Offene Linien und Diagonalen (Lasker: Türme gehören auf offene Linien; über sie dringen sie auf die
 * siebte Reihe ein. Läufer brauchen offene Diagonalen).
 */
@Component
public class OpenLinesRule extends AbstractRule implements StrategicRule {

    public OpenLinesRule() {
        super("STRAT-007", "Offene Linien und Diagonalen",
                "Bewertet Turm-, Damen- und Läuferzüge in Bezug auf offene Linien, Diagonalen und die siebte Reihe.",
                List.of("offene und halboffene Linien", "Ziel- und Startlinie der Schwerfigur", "Eindringen auf die siebte Reihe",
                        "Länge der kontrollierten Diagonale"),
                "Bringe deine Türme auf offene Linien und nutze sie, um auf die siebte Reihe einzudringen.");
    }

    @Override
    public boolean matches(PositionContext c) {
        return c.movedPiece() != null && (c.movedPiece().kind() == PieceKind.ROOK || c.movedPiece().kind() == PieceKind.QUEEN
                || c.movedPiece().kind() == PieceKind.BISHOP);
    }

    @Override
    public List<CoachingFinding> evaluate(PositionContext c) {
        List<CoachingFinding> findings = new ArrayList<>();
        PieceKind kind = c.movedPiece().kind();
        int to = c.move().to();
        int from = c.move().from();
        PawnStructure structure = new PawnStructure(c.after(), c.user());
        String piece = (kind.feminine() ? "Deine " : "Dein ") + kind.germanName();
        if (kind == PieceKind.ROOK || kind == PieceKind.QUEEN) {
            int toFile = Squares.file(to);
            boolean sameFile = Squares.file(from) == toFile;
            boolean open = structure.isFileOpen(toFile);
            boolean halfOpen = structure.isFileHalfOpen(toFile);
            if (!sameFile && (open || halfOpen)) {
                findings.add(FindingBuilder.of(this).positive().heuristic()
                        .severity(kind == PieceKind.ROOK && open ? Severity.MEDIUM : Severity.LOW)
                        .title(open ? "Offene Linie besetzt" : "Halboffene Linie besetzt")
                        .square(to)
                        .explanation(piece + " steht jetzt auf der " + (open ? "offenen" : "halboffenen") + " "
                                + Squares.fileName(toFile) + "-Linie (" + (open ? "keine Bauern darauf" : "keine eigenen Bauern darauf") + ") und übt von dort Druck "
                                + (open ? "bis in das gegnerische Lager aus." : "auf den gegnerischen Bauern dieser Linie aus."))
                        .build());
            } else if (kind == PieceKind.ROOK && !sameFile && structure.isFileOpen(Squares.file(from))
                    && !open && !halfOpen && !c.move().capture()) {
                findings.add(FindingBuilder.of(this).negative().heuristic().severity(Severity.LOW)
                        .title("Offene Linie verlassen")
                        .square(from).square(to)
                        .explanation("Dein Turm verlässt die offene " + Squares.fileName(Squares.file(from))
                                + "-Linie und steht auf " + Squares.name(to) + " hinter eigenen Bauern.")
                        .build());
            }
            if (c.user().relativeRank(Squares.rank(to)) == 6 && c.user().relativeRank(Squares.rank(from)) != 6) {
                boolean targets = hasTargetsOnSeventh(c, Squares.rank(to));
                if (targets) {
                    findings.add(FindingBuilder.of(this).positive().heuristic().severity(Severity.MEDIUM)
                            .title("Eindringen auf die siebte Reihe")
                            .square(to)
                            .explanation(piece + " dringt auf die siebte Reihe ein und greift dort gegnerische Bauern an bzw. "
                                    + "schränkt den gegnerischen König auf der Grundreihe ein.")
                            .build());
                }
            }
        }
        if (kind == PieceKind.BISHOP) {
            int mobility = c.after().mobility(to);
            int before = c.before().mobility(from);
            if (mobility >= 7 && mobility - before >= 2) {
                findings.add(FindingBuilder.of(this).positive().heuristic().severity(Severity.LOW)
                        .title("Offene Diagonale")
                        .square(to)
                        .explanation("Dein Läufer auf " + Squares.name(to) + " steht auf einer langen, freien Schräge und kann "
                                + mobility + " Felder erreichen.")
                        .build());
            }
        }
        return findings;
    }

    private static boolean hasTargetsOnSeventh(PositionContext c, int rank) {
        for (int file = 0; file < 8; file++) {
            Piece p = c.after().pieceAt(Squares.of(file, rank));
            if (p != null && p.color() == c.opponent() && p.kind() == PieceKind.PAWN) {
                return true;
            }
        }
        int king = c.after().kingSquare(c.opponent());
        return Squares.rank(king) == c.opponent().backRank();
    }
}
