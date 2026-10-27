package de.chesscoach.engine;

/** Lebenszyklus einer Engine-Instanz. */
public enum EngineState {
    NOT_STARTED,
    STARTING,
    READY,
    SEARCHING,
    FAILED,
    STOPPED
}
