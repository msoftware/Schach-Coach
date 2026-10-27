package de.chesscoach.domain.position;

/** Eine konkrete Figur (Farbe und Art). */
public record Piece(Color color, PieceKind kind) {

    public static Piece fromFenChar(char c) {
        return new Piece(Character.isUpperCase(c) ? Color.WHITE : Color.BLACK, PieceKind.fromChar(c));
    }

    public char fenChar() {
        char c = kind.fenChar();
        return color == Color.WHITE ? Character.toUpperCase(c) : c;
    }

    public int value() {
        return kind.value();
    }

    /** z. B. "weißer Springer" (Nominativ ohne Artikel). */
    public String germanName() {
        String adjective = color.germanAdjective();
        String ending = kind.feminine() ? "" : "r";
        return adjective + ending + " " + kind.germanName();
    }

    /** z. B. "der weiße Springer" bzw. "die schwarze Dame". */
    public String withArticle() {
        return (kind.feminine() ? "die " : "der ") + color.germanAdjective() + " " + kind.germanName();
    }

    /** Kurzbezeichnung wie "Springer e5" für Erklärungen. */
    public String describeOn(int square) {
        return kind.germanName() + " " + Squares.name(square);
    }
}
