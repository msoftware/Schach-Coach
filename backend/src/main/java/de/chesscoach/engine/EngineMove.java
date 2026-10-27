package de.chesscoach.engine;

/** Von der Engine berechneter Zug mit der zugehörigen Suche. */
public record EngineMove(String uciMove, EngineAnalysis analysis) {
}
