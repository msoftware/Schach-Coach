package de.chesscoach.coaching.strategic;

import de.chesscoach.domain.position.Color;
import de.chesscoach.domain.position.Piece;
import de.chesscoach.domain.position.PieceKind;
import de.chesscoach.domain.position.Position;
import de.chesscoach.domain.position.Squares;

import java.util.ArrayList;
import java.util.List;

/** Stellungsmerkmale, die von mehreren strategischen Regeln verwendet werden. */
public final class StrategicFeatures {

    /** Zentrumsfelder d4, e4, d5, e5. */
    public static final List<Integer> CENTER = List.of(Squares.parse("d4"), Squares.parse("e4"),
            Squares.parse("d5"), Squares.parse("e5"));

    private StrategicFeatures() {
    }

    /** Startfelder der Leichtfiguren einer Farbe. */
    public static List<Integer> minorHomeSquares(Color color) {
        int rank = color.backRank();
        return List.of(Squares.of(1, rank), Squares.of(2, rank), Squares.of(5, rank), Squares.of(6, rank));
    }

    public static boolean isMinorHome(int square, Piece piece) {
        if (piece == null || !piece.kind().isMinor()) {
            return false;
        }
        int rank = piece.color().backRank();
        int file = Squares.file(square);
        if (Squares.rank(square) != rank) {
            return false;
        }
        return piece.kind() == PieceKind.KNIGHT ? file == 1 || file == 6 : file == 2 || file == 5;
    }

    /** Steht die Figur auf (einem) ihrer Ausgangsfelder? Für Bauern: auf der Grundreihe der Bauern. */
    public static boolean isHomeSquare(int square, Piece piece) {
        if (piece == null) {
            return false;
        }
        if (piece.kind() == PieceKind.PAWN) {
            return Squares.rank(square) == piece.color().pawnStartRank();
        }
        if (Squares.rank(square) != piece.color().backRank()) {
            return false;
        }
        int file = Squares.file(square);
        return switch (piece.kind()) {
            case KNIGHT -> file == 1 || file == 6;
            case BISHOP -> file == 2 || file == 5;
            case ROOK -> file == 0 || file == 7;
            case QUEEN -> file == 3;
            case KING -> file == 4;
            default -> false;
        };
    }

    /** Leichtfiguren, die noch auf ihrem Startfeld stehen. */
    public static List<Integer> undevelopedMinors(Position position, Color color) {
        List<Integer> result = new ArrayList<>();
        for (int sq : minorHomeSquares(color)) {
            if (isMinorHome(sq, position.pieceAt(sq)) && position.pieceAt(sq).color() == color) {
                result.add(sq);
            }
        }
        return result;
    }

    public static int developedMinors(Position position, Color color) {
        int minors = position.count(color, PieceKind.KNIGHT) + position.count(color, PieceKind.BISHOP);
        return minors - undevelopedMinors(position, color).size();
    }

    /** Gewichtete Zentrumskontrolle: Angriffe auf d4/e4/d5/e5, Besetzung zählt zusätzlich. */
    public static int centerControl(Position position, Color color) {
        int score = 0;
        for (int sq : CENTER) {
            score += position.attackCount(sq, color);
            Piece occupant = position.pieceAt(sq);
            if (occupant != null && occupant.color() == color) {
                score += occupant.kind() == PieceKind.PAWN ? 2 : 1;
            }
        }
        return score;
    }

    public static int totalMobility(Position position, Color color) {
        int sum = 0;
        for (int sq : position.squaresOf(color)) {
            PieceKind kind = position.pieceAt(sq).kind();
            if (kind != PieceKind.PAWN && kind != PieceKind.KING) {
                sum += position.mobility(sq);
            }
        }
        return sum;
    }

    /** true, wenn der König auf einem typischen Rochadefeld des Königs- oder Damenflügels steht. */
    public static boolean kingOnWing(Position position, Color color) {
        int king = position.kingSquare(color);
        return king != Squares.NONE && Squares.rank(king) == color.backRank()
                && (Squares.file(king) >= 6 || Squares.file(king) <= 2);
    }

    /** Bauern des Königsschutzes: eigene Bauern auf den Linien neben und vor dem König, bis zwei Reihen davor. */
    public static List<Integer> pawnShield(Position position, Color color) {
        List<Integer> result = new ArrayList<>();
        int king = position.kingSquare(color);
        if (king == Squares.NONE) {
            return result;
        }
        for (int df = -1; df <= 1; df++) {
            int f = Squares.file(king) + df;
            for (int step = 1; step <= 2; step++) {
                int r = Squares.rank(king) + step * color.pawnDirection();
                if (!Squares.onBoard(f, r)) {
                    continue;
                }
                Piece p = position.pieceAt(Squares.of(f, r));
                if (p != null && p.color() == color && p.kind() == PieceKind.PAWN) {
                    result.add(Squares.of(f, r));
                }
            }
        }
        return result;
    }

    public static boolean hasQueen(Position position, Color color) {
        return position.count(color, PieceKind.QUEEN) > 0;
    }

    /** Anteil der eigenen Bauern auf der Feldfarbe des Läufers (Kennzeichen eines "schlechten" Läufers). */
    public static double pawnsOnBishopColor(Position position, int bishopSquare) {
        Piece bishop = position.pieceAt(bishopSquare);
        List<Integer> pawns = position.squaresOf(bishop.color(), PieceKind.PAWN);
        if (pawns.isEmpty()) {
            return 0;
        }
        boolean light = Squares.isLight(bishopSquare);
        long same = pawns.stream().filter(p -> Squares.isLight(p) == light).count();
        return (double) same / pawns.size();
    }
}
