package de.chesscoach.coaching;

import de.chesscoach.domain.position.Piece;
import de.chesscoach.domain.position.Position;
import de.chesscoach.domain.position.Squares;

import java.util.Collection;
import java.util.List;

/** Hilfsfunktionen für deutschsprachige Formulierungen. */
public final class GermanText {

    private GermanText() {
    }

    /** "Springer f3" */
    public static String pieceOn(Position position, int square) {
        Piece piece = position.pieceAt(square);
        return piece == null ? Squares.name(square) : piece.kind().germanName() + " " + Squares.name(square);
    }

    /** "dein Springer f3" bzw. "deine Dame d1" */
    public static String yourPieceOn(Position position, int square) {
        Piece piece = position.pieceAt(square);
        if (piece == null) {
            return Squares.name(square);
        }
        return (piece.kind().feminine() ? "deine " : "dein ") + piece.kind().germanName() + " " + Squares.name(square);
    }

    /** "der gegnerische Springer f6" bzw. "die gegnerische Dame d8" */
    public static String opponentPieceOn(Position position, int square) {
        Piece piece = position.pieceAt(square);
        if (piece == null) {
            return Squares.name(square);
        }
        return (piece.kind().feminine() ? "die gegnerische " : "der gegnerische ") + piece.kind().germanName() + " "
                + Squares.name(square);
    }

    /** Akkusativ: "den gegnerischen Springer f6" bzw. "die gegnerische Dame d8" */
    public static String opponentPieceAcc(Position position, int square) {
        Piece piece = position.pieceAt(square);
        if (piece == null) {
            return Squares.name(square);
        }
        return (piece.kind().feminine() ? "die gegnerische " : "den gegnerischen ") + piece.kind().germanName() + " "
                + Squares.name(square);
    }

    /** Akkusativ: "deinen Springer f3" bzw. "deine Dame d1" */
    public static String yourPieceAcc(Position position, int square) {
        Piece piece = position.pieceAt(square);
        if (piece == null) {
            return Squares.name(square);
        }
        return (piece.kind().feminine() ? "deine " : "deinen ") + piece.kind().germanName() + " " + Squares.name(square);
    }

    /** Dativ: "dem gegnerischen Turm d8" bzw. "der gegnerischen Dame d8" */
    public static String opponentPieceDat(Position position, int square) {
        Piece piece = position.pieceAt(square);
        if (piece == null) {
            return Squares.name(square);
        }
        return (piece.kind().feminine() ? "der gegnerischen " : "dem gegnerischen ") + piece.kind().germanName() + " "
                + Squares.name(square);
    }

    /** Akkusativ mit bestimmtem Artikel: "den Turm a8" bzw. "die Dame d8". */
    public static String withArticleAcc(Position position, int square) {
        Piece piece = position.pieceAt(square);
        if (piece == null) {
            return Squares.name(square);
        }
        return (piece.kind().feminine() ? "die " : "den ") + piece.kind().germanName() + " " + Squares.name(square);
    }

    public static String capitalize(String text) {
        return text == null || text.isEmpty() ? text : Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }

    /** Material in Bauerneinheiten in Worten. */
    public static String material(int pawnUnits) {
        int units = Math.abs(pawnUnits);
        return switch (units) {
            case 0 -> "kein Material";
            case 1 -> "einen Bauern";
            case 2 -> "Material im Wert von zwei Bauerneinheiten";
            case 3 -> "Material im Wert einer Leichtfigur (3 Bauerneinheiten)";
            case 5 -> "Material im Wert eines Turms (5 Bauerneinheiten)";
            case 9 -> "Material im Wert einer Dame (9 Bauerneinheiten)";
            default -> "Material im Wert von " + units + " Bauerneinheiten";
        };
    }

    /** "1 Zug" bzw. "3 Zügen" (Dativ Plural) */
    public static String moves(int n) {
        return n == 1 ? "1 Zug" : n + " Zügen";
    }

    /** "+2", "0", "-1" */
    public static String balance(int balance) {
        return balance > 0 ? "+" + balance : String.valueOf(balance);
    }

    public static String join(Collection<String> items) {
        List<String> list = List.copyOf(items);
        if (list.isEmpty()) {
            return "";
        }
        if (list.size() == 1) {
            return list.get(0);
        }
        return String.join(", ", list.subList(0, list.size() - 1)) + " und " + list.get(list.size() - 1);
    }

    public static String squares(Collection<Integer> squares) {
        return join(squares.stream().map(Squares::name).toList());
    }
}
