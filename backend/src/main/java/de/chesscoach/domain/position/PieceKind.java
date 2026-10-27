package de.chesscoach.domain.position;

/**
 * Figurenart mit grobem Vergleichswert in Bauerneinheiten.
 * Die Werte dienen ausschließlich als grobe Vergleichsgröße (STRAT-008), nicht als Stellungsbewertung.
 */
public enum PieceKind {
    PAWN('p', 1, "Bauer", "Bauern", ""),
    KNIGHT('n', 3, "Springer", "Springer", "N"),
    BISHOP('b', 3, "Läufer", "Läufer", "B"),
    ROOK('r', 5, "Turm", "Turm", "R"),
    QUEEN('q', 9, "Dame", "Dame", "Q"),
    KING('k', 100, "König", "König", "K");

    private final char fenChar;
    private final int value;
    private final String germanName;
    private final String germanAccusative;
    private final String sanLetter;

    PieceKind(char fenChar, int value, String germanName, String germanAccusative, String sanLetter) {
        this.fenChar = fenChar;
        this.value = value;
        this.germanName = germanName;
        this.germanAccusative = germanAccusative;
        this.sanLetter = sanLetter;
    }

    public char fenChar() {
        return fenChar;
    }

    /** Grober Materialwert in Bauerneinheiten (König: 100 als Platzhalter für "unersetzlich"). */
    public int value() {
        return value;
    }

    public String germanName() {
        return germanName;
    }

    public String germanAccusative() {
        return germanAccusative;
    }

    public String sanLetter() {
        return sanLetter;
    }

    /** Grammatisches Geschlecht für Artikel: Dame ist feminin, alle anderen maskulin. */
    public boolean feminine() {
        return this == QUEEN;
    }

    public boolean isSlider() {
        return this == BISHOP || this == ROOK || this == QUEEN;
    }

    public boolean isMinor() {
        return this == KNIGHT || this == BISHOP;
    }

    public static PieceKind fromChar(char c) {
        char lower = Character.toLowerCase(c);
        for (PieceKind kind : values()) {
            if (kind.fenChar == lower) {
                return kind;
            }
        }
        throw new IllegalArgumentException("Unbekannte Figur: " + c);
    }
}
