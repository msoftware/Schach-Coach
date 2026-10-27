package de.chesscoach.engine;

/** Die Engine hat eine ungültige oder unvollständige UCI-Ausgabe geliefert (z. B. einen illegalen Zug). */
public class EngineProtocolException extends EngineException {

    public EngineProtocolException(String message) {
        super(message);
    }
}
