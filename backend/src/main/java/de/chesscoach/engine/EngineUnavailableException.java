package de.chesscoach.engine;

/** Die Engine konnte nicht gestartet werden oder ist abgestürzt. */
public class EngineUnavailableException extends EngineException {

    public EngineUnavailableException(String message) {
        super(message);
    }

    public EngineUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
