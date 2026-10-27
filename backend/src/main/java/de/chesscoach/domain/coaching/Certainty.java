package de.chesscoach.domain.coaching;

/** Unterscheidung zwischen gesicherter Erkenntnis und strategischer Heuristik (Requirements 5.3). */
public enum Certainty {
    /** Durch legale Züge und/oder eine Engine-Variante belegt. */
    VERIFIED,
    /** Regelbasierte Einschätzung ohne konkrete Variantenbestätigung. */
    HEURISTIC
}
