package de.chesscoach.domain.analysis;

/**
 * Stellungsbewertung mit Typ, damit Centipawn- und Mattwerte nicht verwechselt werden.
 *
 * <p>Bei {@link ScoreType#MATE} ist {@code value} die Anzahl eigener Züge bis zum Matt aus Sicht
 * der bewerteten Seite: positiv = die Seite setzt matt, negativ = die Seite wird matt gesetzt,
 * 0 = die Seite ist bereits matt.</p>
 */
public record Score(ScoreType type, int value) {

    public static final int MATE_BASE = 100_000;
    private static final int CP_CLAMP = 20_000;

    public static Score cp(int centipawns) {
        return new Score(ScoreType.CP, centipawns);
    }

    public static Score mate(int moves) {
        return new Score(ScoreType.MATE, moves);
    }

    public boolean isMate() {
        return type == ScoreType.MATE;
    }

    /** Die bewertete Seite setzt matt. */
    public boolean isWinningMate() {
        return isMate() && value > 0;
    }

    /** Die bewertete Seite wird matt gesetzt. */
    public boolean isLosingMate() {
        return isMate() && value <= 0;
    }

    /** Gleiche Bewertung aus Sicht des Gegners (ohne Zug dazwischen). */
    public Score negate() {
        return new Score(type, -value);
    }

    /**
     * Bewertung eines Zuges aus Sicht der ziehenden Seite, abgeleitet aus der Bewertung der
     * Folgestellung (dort ist der Gegner am Zug).
     */
    public static Score fromChildPerspective(Score child) {
        if (child.type == ScoreType.CP) {
            return cp(-child.value);
        }
        if (child.value > 0) {
            return mate(-child.value);
        }
        return mate(-child.value + 1);
    }

    /** Monotone Vergleichsgröße: schnelleres eigenes Matt ist besser, späteres eigenes Mattwerden ist besser. */
    public int comparable() {
        if (type == ScoreType.CP) {
            return Math.max(-CP_CLAMP, Math.min(CP_CLAMP, value));
        }
        return value > 0 ? MATE_BASE - value : -MATE_BASE - value;
    }

    /** Centipawn-Wert, Mattwerte auf ±{@code clamp} begrenzt. */
    public int centipawnsClamped(int clamp) {
        if (type == ScoreType.MATE) {
            return value > 0 ? clamp : -clamp;
        }
        return Math.max(-clamp, Math.min(clamp, value));
    }

    /** Gewinnwahrscheinlichkeit (0..1) aus Sicht der bewerteten Seite über eine logistische Funktion. */
    public double winProbability() {
        if (type == ScoreType.MATE) {
            return value > 0 ? 1.0 : 0.0;
        }
        return 1.0 / (1.0 + Math.pow(10.0, -value / 400.0));
    }

    /** Darstellung wie "+0.35", "-1.20", "M3" oder "-M2". */
    public String display() {
        if (type == ScoreType.MATE) {
            if (value == 0) {
                return "Matt";
            }
            return (value > 0 ? "M" : "-M") + Math.abs(value);
        }
        return String.format(java.util.Locale.ROOT, "%+.2f", value / 100.0);
    }
}
