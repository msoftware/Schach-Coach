package de.chesscoach.application;

import de.chesscoach.application.game.GameSnapshot;

/** Fachlicher Fehler mit Fehlercode und optional dem aktuellen Partiezustand (z. B. bei Versionskonflikten). */
public class GameException extends RuntimeException {

    private final ErrorCode code;
    private final transient GameSnapshot current;

    public GameException(ErrorCode code, String detail) {
        this(code, detail, null);
    }

    public GameException(ErrorCode code, String detail, GameSnapshot current) {
        super(detail);
        this.code = code;
        this.current = current;
    }

    public ErrorCode code() {
        return code;
    }

    public GameSnapshot current() {
        return current;
    }
}
