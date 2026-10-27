package de.chesscoach.chess;

/** Ein Zug ist in der gegebenen Stellung nicht legal. */
public class IllegalMoveException extends RuntimeException {

    private final String move;

    public IllegalMoveException(String move, String message) {
        super(message);
        this.move = move;
    }

    public String move() {
        return move;
    }
}
