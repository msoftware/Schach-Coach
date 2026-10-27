package de.chesscoach.engine;

/** Die Engine hat nicht innerhalb der erlaubten Zeit geantwortet. */
public class EngineTimeoutException extends EngineException {

    public EngineTimeoutException(String message) {
        super(message);
    }
}
