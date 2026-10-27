package de.chesscoach.domain.position;

import java.util.ArrayList;
import java.util.List;

/** Erkennung von Linienmotiven (Fesselung, Spieß) entlang von Linien langschrittiger Figuren. */
public final class LineMotifs {

    public enum Type {
        /** Vordere Figur ist weniger wert als die dahinterstehende oder dahinter steht der König. */
        PIN,
        /** Vordere Figur ist wertvoller (oder König) und muss ausweichen; dahinter steht Material. */
        SKEWER
    }

    /**
     * @param attacker   Feld der angreifenden langschrittigen Figur
     * @param front      Feld der vorderen (angegriffenen) Figur
     * @param back       Feld der dahinterstehenden Figur
     * @param absolute   true, wenn bei einer Fesselung der König dahinter steht
     */
    public record Motif(Type type, int attacker, int front, int back, boolean absolute) {
    }

    private LineMotifs() {
    }

    /** Alle Fesselungen und Spieße, die {@code attackerSide} gegen die andere Seite ausübt. */
    public static List<Motif> find(Position position, Color attackerSide) {
        List<Motif> result = new ArrayList<>();
        Color victim = attackerSide.opposite();
        for (int sq : position.squaresOf(attackerSide)) {
            Piece slider = position.pieceAt(sq);
            if (!slider.kind().isSlider()) {
                continue;
            }
            for (int[] dir : directionsOf(slider.kind())) {
                int front = Squares.NONE;
                int f = Squares.file(sq) + dir[0];
                int r = Squares.rank(sq) + dir[1];
                while (Squares.onBoard(f, r)) {
                    int cur = Squares.of(f, r);
                    Piece p = position.pieceAt(cur);
                    if (p != null) {
                        if (p.color() != victim) {
                            break;
                        }
                        if (front == Squares.NONE) {
                            front = cur;
                        } else {
                            Motif motif = classify(position, sq, front, cur);
                            if (motif != null) {
                                result.add(motif);
                            }
                            break;
                        }
                    }
                    f += dir[0];
                    r += dir[1];
                }
            }
        }
        return result;
    }

    private static Motif classify(Position position, int attacker, int front, int back) {
        Piece frontPiece = position.pieceAt(front);
        Piece backPiece = position.pieceAt(back);
        Piece attackerPiece = position.pieceAt(attacker);
        if (backPiece.kind() == PieceKind.KING) {
            return new Motif(Type.PIN, attacker, front, back, true);
        }
        if (frontPiece.kind() == PieceKind.KING) {
            // Spieß durch Schachgebot: Material hinter dem König geht verloren, wenn es nicht gedeckt ist
            // oder wertvoller als der Angreifer ist.
            boolean worthIt = backPiece.value() >= attackerPiece.value()
                    || position.attackersOf(back, backPiece.color()).isEmpty();
            return worthIt && backPiece.kind() != PieceKind.PAWN ? new Motif(Type.SKEWER, attacker, front, back, false) : null;
        }
        if (backPiece.value() > frontPiece.value()) {
            return new Motif(Type.PIN, attacker, front, back, false);
        }
        if (frontPiece.value() > backPiece.value() && frontPiece.value() > attackerPiece.value()
                && backPiece.kind() != PieceKind.PAWN
                && (backPiece.value() >= attackerPiece.value() || position.attackersOf(back, backPiece.color()).isEmpty())) {
            return new Motif(Type.SKEWER, attacker, front, back, false);
        }
        return null;
    }

    static int[][] directionsOf(PieceKind kind) {
        return switch (kind) {
            case BISHOP -> Position.BISHOP_DIRECTIONS;
            case ROOK -> Position.ROOK_DIRECTIONS;
            case QUEEN -> new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {1, -1}, {-1, 1}, {-1, -1}};
            default -> new int[0][];
        };
    }
}
