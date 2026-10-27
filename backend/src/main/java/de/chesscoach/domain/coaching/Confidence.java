package de.chesscoach.domain.coaching;

/** Zuverlässigkeit einer Analyse oder Erkennung. */
public enum Confidence {
    HIGH, MEDIUM, LOW;

    public String german() {
        return switch (this) {
            case HIGH -> "hoch";
            case MEDIUM -> "mittel";
            case LOW -> "niedrig";
        };
    }
}
