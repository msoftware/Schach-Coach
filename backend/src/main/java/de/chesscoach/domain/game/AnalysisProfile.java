package de.chesscoach.domain.game;

/** Gründlichkeit der Coaching-Analyse; skaliert die konfigurierten Suchgrenzen. */
public enum AnalysisProfile {
    FAST("Schnell", 0.5),
    NORMAL("Normal", 1.0),
    DEEP("Gründlich", 2.0);

    private final String german;
    private final double factor;

    AnalysisProfile(String german, double factor) {
        this.german = german;
        this.factor = factor;
    }

    public String german() {
        return german;
    }

    public double factor() {
        return factor;
    }
}
