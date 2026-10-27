package de.chesscoach.domain.position;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/** Strukturmerkmale der Bauernstellung einer Farbe (Grundlage für STRAT-005, STRAT-006, STRAT-011, STRAT-012). */
public final class PawnStructure {

    private final Position position;
    private final Color color;

    public PawnStructure(Position position, Color color) {
        this.position = position;
        this.color = color;
    }

    public List<Integer> pawns() {
        return position.squaresOf(color, PieceKind.PAWN);
    }

    private List<Integer> pawnsOn(Color side, int file) {
        List<Integer> result = new ArrayList<>();
        for (int sq : position.squaresOf(side, PieceKind.PAWN)) {
            if (Squares.file(sq) == file) {
                result.add(sq);
            }
        }
        return result;
    }

    public Set<Integer> isolated() {
        Set<Integer> result = new TreeSet<>();
        for (int sq : pawns()) {
            int f = Squares.file(sq);
            boolean neighbor = (f > 0 && !pawnsOn(color, f - 1).isEmpty()) || (f < 7 && !pawnsOn(color, f + 1).isEmpty());
            if (!neighbor) {
                result.add(sq);
            }
        }
        return result;
    }

    /** Alle Bauern auf Linien mit mindestens zwei eigenen Bauern. */
    public Set<Integer> doubled() {
        Set<Integer> result = new TreeSet<>();
        for (int f = 0; f < 8; f++) {
            List<Integer> onFile = pawnsOn(color, f);
            if (onFile.size() >= 2) {
                result.addAll(onFile);
            }
        }
        return result;
    }

    public boolean isPassed(int sq) {
        int f = Squares.file(sq);
        int rel = color.relativeRank(Squares.rank(sq));
        for (int ef = Math.max(0, f - 1); ef <= Math.min(7, f + 1); ef++) {
            for (int enemy : pawnsOn(color.opposite(), ef)) {
                if (color.relativeRank(Squares.rank(enemy)) > rel) {
                    return false;
                }
            }
        }
        return true;
    }

    public Set<Integer> passed() {
        Set<Integer> result = new TreeSet<>();
        for (int sq : pawns()) {
            if (isPassed(sq)) {
                result.add(sq);
            }
        }
        return result;
    }

    /**
     * Rückständiger Bauer: Kein eigener Bauer auf den Nachbarlinien steht auf gleicher Höhe oder dahinter,
     * und das Feld vor dem Bauern wird von einem gegnerischen Bauern kontrolliert.
     */
    public Set<Integer> backward() {
        Set<Integer> result = new TreeSet<>();
        for (int sq : pawns()) {
            int f = Squares.file(sq);
            int rel = color.relativeRank(Squares.rank(sq));
            boolean supportPossible = false;
            boolean hasNeighbors = false;
            for (int nf : new int[]{f - 1, f + 1}) {
                if (nf < 0 || nf > 7) {
                    continue;
                }
                for (int own : pawnsOn(color, nf)) {
                    hasNeighbors = true;
                    if (color.relativeRank(Squares.rank(own)) <= rel) {
                        supportPossible = true;
                    }
                }
            }
            if (!hasNeighbors || supportPossible || isPassed(sq)) {
                continue;
            }
            int stopRank = Squares.rank(sq) + color.pawnDirection();
            if (stopRank < 0 || stopRank > 7) {
                continue;
            }
            int stop = Squares.of(f, stopRank);
            if (pawnControls(color.opposite(), stop)) {
                result.add(sq);
            }
        }
        return result;
    }

    /** Hängende Bauern: zwei nebeneinanderstehende, von eigenen Bauern isolierte Bauern auf halboffenen Linien. */
    public Set<Integer> hanging() {
        Set<Integer> result = new TreeSet<>();
        for (int f = 0; f < 7; f++) {
            List<Integer> left = pawnsOn(color, f);
            List<Integer> right = pawnsOn(color, f + 1);
            if (left.size() != 1 || right.size() != 1) {
                continue;
            }
            boolean outerLeft = f > 0 && !pawnsOn(color, f - 1).isEmpty();
            boolean outerRight = f + 2 <= 7 && !pawnsOn(color, f + 2).isEmpty();
            boolean halfOpen = pawnsOn(color.opposite(), f).isEmpty() && pawnsOn(color.opposite(), f + 1).isEmpty();
            int leftSq = left.get(0);
            int rightSq = right.get(0);
            if (!outerLeft && !outerRight && halfOpen && Squares.rank(leftSq) == Squares.rank(rightSq)
                    && color.relativeRank(Squares.rank(leftSq)) >= 3) {
                result.add(leftSq);
                result.add(rightSq);
            }
        }
        return result;
    }

    public boolean pawnControls(Color side, int square) {
        int f = Squares.file(square);
        int r = Squares.rank(square) - side.pawnDirection();
        if (r < 0 || r > 7) {
            return false;
        }
        for (int df : new int[]{-1, 1}) {
            if (Squares.onBoard(f + df, r)) {
                Piece p = position.pieceAt(Squares.of(f + df, r));
                if (p != null && p.color() == side && p.kind() == PieceKind.PAWN) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Kann ein Bauer von {@code side} das Feld jetzt oder nach Vorrücken angreifen? */
    public boolean pawnCanEverAttack(Color side, int square) {
        int f = Squares.file(square);
        int rel = side.relativeRank(Squares.rank(square));
        for (int nf : new int[]{f - 1, f + 1}) {
            if (nf < 0 || nf > 7) {
                continue;
            }
            for (int pawn : pawnsOn(side, nf)) {
                if (side.relativeRank(Squares.rank(pawn)) < rel) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Schwache Felder im eigenen Lager (relative Reihen 3 bis 4, d. h. dritte und vierte Reihe),
     * die von eigenen Bauern nie mehr angegriffen werden können.
     */
    public Set<Integer> holes() {
        Set<Integer> result = new TreeSet<>();
        for (int sq = 0; sq < 64; sq++) {
            int rel = color.relativeRank(Squares.rank(sq));
            int f = Squares.file(sq);
            if ((rel == 2 || rel == 3) && f >= 1 && f <= 6 && !pawnCanEverAttack(color, sq)) {
                Piece p = position.pieceAt(sq);
                if (p == null || p.kind() != PieceKind.PAWN) {
                    result.add(sq);
                }
            }
        }
        return result;
    }

    /** Anzahl eigener Bauern auf dem Damenflügel (a–d) bzw. Königsflügel (e–h). */
    public int countOnWing(boolean queenside) {
        int count = 0;
        for (int sq : pawns()) {
            int f = Squares.file(sq);
            if (queenside ? f <= 3 : f >= 4) {
                count++;
            }
        }
        return count;
    }

    public boolean isFileOpen(int file) {
        return pawnsOn(Color.WHITE, file).isEmpty() && pawnsOn(Color.BLACK, file).isEmpty();
    }

    /** Halboffen aus Sicht dieser Farbe: keine eigenen Bauern, aber gegnerische Bauern auf der Linie. */
    public boolean isFileHalfOpen(int file) {
        return pawnsOn(color, file).isEmpty() && !pawnsOn(color.opposite(), file).isEmpty();
    }
}
