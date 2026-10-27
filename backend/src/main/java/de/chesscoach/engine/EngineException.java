package de.chesscoach.engine;

/** Basisklasse aller Engine-Fehler. Meldungen sind für Benutzer verständlich und enthalten keine Interna. */
public class EngineException extends RuntimeException {

    public EngineException(String message) {
        super(message);
    }

    public EngineException(String message, Throwable cause) {
        super(message, cause);
    }
}
