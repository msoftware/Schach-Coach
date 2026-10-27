package de.chesscoach.domain.position;

/** Farbe einer Figur bzw. Seite am Zug. */
public enum Color {
    WHITE("Weiß", "weiße", 1),
    BLACK("Schwarz", "schwarze", -1);

    private final String germanName;
    private final String germanAdjective;
    private final int pawnDirection;

    Color(String germanName, String germanAdjective, int pawnDirection) {
        this.germanName = germanName;
        this.germanAdjective = germanAdjective;
        this.pawnDirection = pawnDirection;
    }

    public Color opposite() {
        return this == WHITE ? BLACK : WHITE;
    }

    public String germanName() {
        return germanName;
    }

    /** Adjektiv ohne Endung für den Nominativ mit bestimmtem Artikel, z. B. "der weiße Springer". */
    public String germanAdjective() {
        return germanAdjective;
    }

    /** +1 für Weiß (Bauern ziehen zu höheren Reihen), -1 für Schwarz. */
    public int pawnDirection() {
        return pawnDirection;
    }

    /** Reihe (0-basiert), auf der die Bauern dieser Farbe starten. */
    public int pawnStartRank() {
        return this == WHITE ? 1 : 6;
    }

    /** Grundreihe (0-basiert) dieser Farbe. */
    public int backRank() {
        return this == WHITE ? 0 : 7;
    }

    /** Umwandlungsreihe (0-basiert) dieser Farbe. */
    public int promotionRank() {
        return this == WHITE ? 7 : 0;
    }

    /** Relative Reihe aus Sicht dieser Farbe (0 = eigene Grundreihe, 7 = gegnerische Grundreihe). */
    public int relativeRank(int rank) {
        return this == WHITE ? rank : 7 - rank;
    }
}
