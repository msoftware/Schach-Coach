package de.chesscoach.engine;

/** Suchgrenzen für die Berechnung eines Computerzuges. Nicht gesetzte Grenzen sind {@code null}. */
public record EngineLimits(Integer depth, Long nodes, Long movetimeMillis) {

    public EngineLimits {
        new AnalysisLimits(depth, nodes, movetimeMillis);
    }

    public AnalysisLimits toAnalysisLimits() {
        return new AnalysisLimits(depth, nodes, movetimeMillis);
    }
}
