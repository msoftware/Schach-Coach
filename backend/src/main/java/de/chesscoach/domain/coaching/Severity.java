package de.chesscoach.domain.coaching;

/** Gewichtung eines Befunds. */
public enum Severity {
    INFO, LOW, MEDIUM, HIGH, CRITICAL;

    public boolean atLeast(Severity other) {
        return compareTo(other) >= 0;
    }
}
