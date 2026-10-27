package de.chesscoach.domain.position;

/** Hilfsfunktionen für Feldindizes (a1 = 0, b1 = 1, ..., h8 = 63). */
public final class Squares {

    public static final int NONE = -1;

    private Squares() {
    }

    public static int of(int file, int rank) {
        return rank * 8 + file;
    }

    public static int file(int square) {
        return square & 7;
    }

    public static int rank(int square) {
        return square >> 3;
    }

    public static boolean onBoard(int file, int rank) {
        return file >= 0 && file < 8 && rank >= 0 && rank < 8;
    }

    public static String name(int square) {
        return "" + (char) ('a' + file(square)) + (char) ('1' + rank(square));
    }

    public static int parse(String name) {
        if (name == null || name.length() != 2) {
            throw new IllegalArgumentException("Ungültiges Feld: " + name);
        }
        int file = name.charAt(0) - 'a';
        int rank = name.charAt(1) - '1';
        if (!onBoard(file, rank)) {
            throw new IllegalArgumentException("Ungültiges Feld: " + name);
        }
        return of(file, rank);
    }

    public static String fileName(int file) {
        return String.valueOf((char) ('a' + file));
    }

    /** Hell oder dunkel: a1 ist dunkel, h1 ist hell. */
    public static boolean isLight(int square) {
        return (file(square) + rank(square)) % 2 == 1;
    }

    public static int distance(int a, int b) {
        return Math.max(Math.abs(file(a) - file(b)), Math.abs(rank(a) - rank(b)));
    }

    /** Abstand zum nächsten der vier Zentralfelder (0 = Zentrum). */
    public static int centerDistance(int square) {
        int f = file(square);
        int r = rank(square);
        int df = f <= 3 ? 3 - f : f - 4;
        int dr = r <= 3 ? 3 - r : r - 4;
        return Math.max(df, dr);
    }
}
