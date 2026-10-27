package de.chesscoach.domain.coaching;

/** Herkunftsbereich eines Befunds. */
public enum FindingCategory {
    /** Ebene 1: Schachregeln und Taktik. */
    TACTICAL,
    /** Ebene 2: Strategische Prinzipien. */
    STRATEGIC,
    /** Direkt aus der Engine-Analyse abgeleitet. */
    ENGINE
}
