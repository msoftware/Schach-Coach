package de.chesscoach.chess;

/** Eine FEN ist syntaktisch oder schachlich ungültig. */
public class InvalidFenException extends RuntimeException {

    public InvalidFenException(String message) {
        super(message);
    }
}
