package de.chesscoach.domain.coaching;

/** Qualitätsklassen eines Zuges (Requirements 6.3). */
public enum MoveClassification {
    BEST("Bestzug"),
    EXCELLENT("Sehr gut"),
    GOOD("Gut"),
    INACCURACY("Ungenau"),
    MISTAKE("Fehler"),
    BLUNDER("Grober Fehler"),
    FORCED("Erzwungen"),
    /** Keine ausreichend zuverlässige Einordnung möglich. */
    UNCERTAIN("Unsicher");

    private final String german;

    MoveClassification(String german) {
        this.german = german;
    }

    public String german() {
        return german;
    }

    public boolean isError() {
        return this == INACCURACY || this == MISTAKE || this == BLUNDER;
    }

    public boolean isGood() {
        return this == BEST || this == EXCELLENT || this == GOOD || this == FORCED;
    }
}
