package de.chesscoach.domain.position;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Unveränderliches Stellungsmodell der Coaching-Logik.
 *
 * <p>Die Klasse ist bewusst unabhängig von Chesslib und der Engine, damit die Coaching-Regeln
 * deterministisch und isoliert testbar bleiben. Sie prüft keine Legalität: Züge, die über
 * {@link #play(int, int, PieceKind)} ausgeführt werden, müssen vorher von der Regelkomponente
 * als legal bestätigt worden sein.</p>
 */
public final class Position {

    static final int[][] KNIGHT_OFFSETS = {{1, 2}, {2, 1}, {2, -1}, {1, -2}, {-1, -2}, {-2, -1}, {-2, 1}, {-1, 2}};
    static final int[][] KING_OFFSETS = {{1, 0}, {1, 1}, {0, 1}, {-1, 1}, {-1, 0}, {-1, -1}, {0, -1}, {1, -1}};
    static final int[][] ROOK_DIRECTIONS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
    static final int[][] BISHOP_DIRECTIONS = {{1, 1}, {1, -1}, {-1, 1}, {-1, -1}};

    private final Piece[] board;
    private final Color sideToMove;
    private final String castling;
    private final int enPassantSquare;
    private final int halfmoveClock;
    private final int fullmoveNumber;

    private Position(Piece[] board, Color sideToMove, String castling, int enPassantSquare,
                     int halfmoveClock, int fullmoveNumber) {
        this.board = board;
        this.sideToMove = sideToMove;
        this.castling = castling;
        this.enPassantSquare = enPassantSquare;
        this.halfmoveClock = halfmoveClock;
        this.fullmoveNumber = fullmoveNumber;
    }

    public static Position fromFen(String fen) {
        if (fen == null) {
            throw new IllegalArgumentException("FEN fehlt");
        }
        String[] parts = fen.trim().split("\\s+");
        if (parts.length < 4) {
            throw new IllegalArgumentException("FEN unvollständig: " + fen);
        }
        Piece[] board = new Piece[64];
        String[] rows = parts[0].split("/");
        if (rows.length != 8) {
            throw new IllegalArgumentException("FEN muss acht Reihen enthalten: " + fen);
        }
        for (int row = 0; row < 8; row++) {
            int rank = 7 - row;
            int file = 0;
            for (char c : rows[row].toCharArray()) {
                if (Character.isDigit(c)) {
                    file += c - '0';
                } else {
                    if (file > 7) {
                        throw new IllegalArgumentException("FEN-Reihe zu lang: " + rows[row]);
                    }
                    board[Squares.of(file, rank)] = Piece.fromFenChar(c);
                    file++;
                }
            }
            if (file != 8) {
                throw new IllegalArgumentException("FEN-Reihe hat nicht acht Felder: " + rows[row]);
            }
        }
        Color side = switch (parts[1]) {
            case "w" -> Color.WHITE;
            case "b" -> Color.BLACK;
            default -> throw new IllegalArgumentException("Ungültige Seite am Zug: " + parts[1]);
        };
        int ep = "-".equals(parts[3]) ? Squares.NONE : Squares.parse(parts[3]);
        int halfmove = parts.length > 4 ? Integer.parseInt(parts[4]) : 0;
        int fullmove = parts.length > 5 ? Integer.parseInt(parts[5]) : 1;
        return new Position(board, side, parts[2], ep, halfmove, fullmove);
    }

    public String toFen() {
        StringBuilder sb = new StringBuilder();
        for (int rank = 7; rank >= 0; rank--) {
            int empty = 0;
            for (int file = 0; file < 8; file++) {
                Piece p = board[Squares.of(file, rank)];
                if (p == null) {
                    empty++;
                } else {
                    if (empty > 0) {
                        sb.append(empty);
                        empty = 0;
                    }
                    sb.append(p.fenChar());
                }
            }
            if (empty > 0) {
                sb.append(empty);
            }
            if (rank > 0) {
                sb.append('/');
            }
        }
        sb.append(' ').append(sideToMove == Color.WHITE ? 'w' : 'b');
        sb.append(' ').append(castling.isEmpty() ? "-" : castling);
        sb.append(' ').append(enPassantSquare == Squares.NONE ? "-" : Squares.name(enPassantSquare));
        sb.append(' ').append(halfmoveClock).append(' ').append(fullmoveNumber);
        return sb.toString();
    }

    public Piece pieceAt(int square) {
        return board[square];
    }

    public boolean isEmpty(int square) {
        return board[square] == null;
    }

    public Color sideToMove() {
        return sideToMove;
    }

    public String castling() {
        return castling;
    }

    public int enPassantSquare() {
        return enPassantSquare;
    }

    public int halfmoveClock() {
        return halfmoveClock;
    }

    public int fullmoveNumber() {
        return fullmoveNumber;
    }

    public boolean canCastle(Color color) {
        return color == Color.WHITE
                ? castling.contains("K") || castling.contains("Q")
                : castling.contains("k") || castling.contains("q");
    }

    public List<Integer> squaresOf(Color color) {
        List<Integer> result = new ArrayList<>();
        for (int sq = 0; sq < 64; sq++) {
            Piece p = board[sq];
            if (p != null && p.color() == color) {
                result.add(sq);
            }
        }
        return result;
    }

    public List<Integer> squaresOf(Color color, PieceKind kind) {
        List<Integer> result = new ArrayList<>();
        for (int sq = 0; sq < 64; sq++) {
            Piece p = board[sq];
            if (p != null && p.color() == color && p.kind() == kind) {
                result.add(sq);
            }
        }
        return result;
    }

    public int count(Color color, PieceKind kind) {
        return squaresOf(color, kind).size();
    }

    public int kingSquare(Color color) {
        for (int sq = 0; sq < 64; sq++) {
            Piece p = board[sq];
            if (p != null && p.color() == color && p.kind() == PieceKind.KING) {
                return sq;
            }
        }
        return Squares.NONE;
    }

    /** Materialsumme in Bauerneinheiten ohne König. */
    public int material(Color color) {
        int sum = 0;
        for (Piece p : board) {
            if (p != null && p.color() == color && p.kind() != PieceKind.KING) {
                sum += p.value();
            }
        }
        return sum;
    }

    /** Materialbilanz aus Sicht der angegebenen Farbe. */
    public int materialBalance(Color perspective) {
        return material(perspective) - material(perspective.opposite());
    }

    /** Material ohne Bauern und König (für Phasenerkennung). */
    public int nonPawnMaterial(Color color) {
        int sum = 0;
        for (Piece p : board) {
            if (p != null && p.color() == color && p.kind() != PieceKind.KING && p.kind() != PieceKind.PAWN) {
                sum += p.value();
            }
        }
        return sum;
    }

    /** Endspiel: keine Damen mehr oder insgesamt wenig Figurenmaterial. */
    public boolean isEndgame() {
        int queens = count(Color.WHITE, PieceKind.QUEEN) + count(Color.BLACK, PieceKind.QUEEN);
        int nonPawn = nonPawnMaterial(Color.WHITE) + nonPawnMaterial(Color.BLACK);
        return nonPawn <= 26 && (queens == 0 || nonPawn <= 18);
    }

    /** Eröffnungsphase: frühe Zugnummer und noch weitgehend vollständiges Material. */
    public boolean isOpening() {
        return fullmoveNumber <= 15 && nonPawnMaterial(Color.WHITE) + nonPawnMaterial(Color.BLACK) >= 52;
    }

    /** Felder, die die Figur auf {@code from} angreift (bzw. deckt), unter Berücksichtigung von Blockaden. */
    public long attacksFrom(int from) {
        Piece piece = board[from];
        if (piece == null) {
            return 0L;
        }
        int file = Squares.file(from);
        int rank = Squares.rank(from);
        long result = 0L;
        switch (piece.kind()) {
            case PAWN -> {
                int r = rank + piece.color().pawnDirection();
                for (int df : new int[]{-1, 1}) {
                    if (Squares.onBoard(file + df, r)) {
                        result |= 1L << Squares.of(file + df, r);
                    }
                }
            }
            case KNIGHT -> result = stepAttacks(file, rank, KNIGHT_OFFSETS);
            case KING -> result = stepAttacks(file, rank, KING_OFFSETS);
            case BISHOP -> result = slideAttacks(file, rank, BISHOP_DIRECTIONS);
            case ROOK -> result = slideAttacks(file, rank, ROOK_DIRECTIONS);
            case QUEEN -> result = slideAttacks(file, rank, BISHOP_DIRECTIONS) | slideAttacks(file, rank, ROOK_DIRECTIONS);
        }
        return result;
    }

    private long stepAttacks(int file, int rank, int[][] offsets) {
        long result = 0L;
        for (int[] o : offsets) {
            int f = file + o[0];
            int r = rank + o[1];
            if (Squares.onBoard(f, r)) {
                result |= 1L << Squares.of(f, r);
            }
        }
        return result;
    }

    private long slideAttacks(int file, int rank, int[][] directions) {
        long result = 0L;
        for (int[] d : directions) {
            int f = file + d[0];
            int r = rank + d[1];
            while (Squares.onBoard(f, r)) {
                int sq = Squares.of(f, r);
                result |= 1L << sq;
                if (board[sq] != null) {
                    break;
                }
                f += d[0];
                r += d[1];
            }
        }
        return result;
    }

    public boolean attacks(int from, int target) {
        return (attacksFrom(from) & (1L << target)) != 0;
    }

    /** Alle Felder mit Figuren der Farbe {@code by}, die {@code target} angreifen. */
    public List<Integer> attackersOf(int target, Color by) {
        List<Integer> result = new ArrayList<>();
        for (int sq = 0; sq < 64; sq++) {
            Piece p = board[sq];
            if (p != null && p.color() == by && attacks(sq, target)) {
                result.add(sq);
            }
        }
        return result;
    }

    public boolean isAttackedBy(int target, Color by) {
        for (int sq = 0; sq < 64; sq++) {
            Piece p = board[sq];
            if (p != null && p.color() == by && attacks(sq, target)) {
                return true;
            }
        }
        return false;
    }

    /** Anzahl Angriffe der Farbe auf das Feld (Bauern und Figuren). */
    public int attackCount(int target, Color by) {
        return attackersOf(target, by).size();
    }

    public boolean isInCheck(Color color) {
        int king = kingSquare(color);
        return king != Squares.NONE && isAttackedBy(king, color.opposite());
    }

    /** Pseudomobilität: angegriffene Felder, die nicht von eigenen Figuren besetzt sind. */
    public int mobility(int from) {
        Piece piece = board[from];
        if (piece == null) {
            return 0;
        }
        long attacks = attacksFrom(from);
        int count = 0;
        for (int sq = 0; sq < 64; sq++) {
            if ((attacks & (1L << sq)) != 0) {
                Piece target = board[sq];
                if (target == null || target.color() != piece.color()) {
                    count++;
                }
            }
        }
        return count;
    }

    /** Liste der Felder auf einer Linie zwischen a und b (exklusiv); leer, wenn nicht auf einer Linie. */
    public static List<Integer> between(int a, int b) {
        List<Integer> result = new ArrayList<>();
        int df = Integer.signum(Squares.file(b) - Squares.file(a));
        int dr = Integer.signum(Squares.rank(b) - Squares.rank(a));
        int fileDiff = Math.abs(Squares.file(b) - Squares.file(a));
        int rankDiff = Math.abs(Squares.rank(b) - Squares.rank(a));
        if (!(fileDiff == 0 || rankDiff == 0 || fileDiff == rankDiff) || a == b) {
            return result;
        }
        int f = Squares.file(a) + df;
        int r = Squares.rank(a) + dr;
        while (Squares.of(f, r) != b) {
            result.add(Squares.of(f, r));
            f += df;
            r += dr;
        }
        return result;
    }

    /**
     * Führt einen bereits als legal bestätigten Zug aus und liefert die Folgestellung.
     * Behandelt Rochade, En passant und Umwandlung.
     */
    public Position play(int from, int to, PieceKind promotion) {
        Piece moving = board[from];
        if (moving == null) {
            throw new IllegalArgumentException("Kein Stein auf " + Squares.name(from));
        }
        Piece[] next = Arrays.copyOf(board, 64);
        Piece captured = next[to];
        next[from] = null;
        boolean pawnMove = moving.kind() == PieceKind.PAWN;
        if (pawnMove && to == enPassantSquare && captured == null && Squares.file(from) != Squares.file(to)) {
            int capturedSquare = Squares.of(Squares.file(to), Squares.rank(from));
            captured = next[capturedSquare];
            next[capturedSquare] = null;
        }
        if (moving.kind() == PieceKind.KING && Math.abs(Squares.file(to) - Squares.file(from)) == 2) {
            int rank = Squares.rank(from);
            boolean kingSide = Squares.file(to) == 6;
            int rookFrom = Squares.of(kingSide ? 7 : 0, rank);
            int rookTo = Squares.of(kingSide ? 5 : 3, rank);
            next[rookTo] = next[rookFrom];
            next[rookFrom] = null;
        }
        next[to] = promotion != null && pawnMove ? new Piece(moving.color(), promotion) : moving;

        String newCastling = updateCastling(castling, from, to, moving);
        int newEp = Squares.NONE;
        if (pawnMove && Math.abs(Squares.rank(to) - Squares.rank(from)) == 2) {
            newEp = Squares.of(Squares.file(from), (Squares.rank(from) + Squares.rank(to)) / 2);
        }
        int newHalfmove = pawnMove || captured != null ? 0 : halfmoveClock + 1;
        int newFullmove = sideToMove == Color.BLACK ? fullmoveNumber + 1 : fullmoveNumber;
        return new Position(next, sideToMove.opposite(), newCastling, newEp, newHalfmove, newFullmove);
    }

    private static String updateCastling(String rights, int from, int to, Piece moving) {
        String result = rights;
        if (moving.kind() == PieceKind.KING) {
            result = moving.color() == Color.WHITE ? result.replace("K", "").replace("Q", "")
                    : result.replace("k", "").replace("q", "");
        }
        for (int sq : new int[]{from, to}) {
            switch (Squares.name(sq)) {
                case "h1" -> result = result.replace("K", "");
                case "a1" -> result = result.replace("Q", "");
                case "h8" -> result = result.replace("k", "");
                case "a8" -> result = result.replace("q", "");
                default -> {
                }
            }
        }
        String normalized = result.replace("-", "");
        return normalized.isEmpty() ? "-" : normalized;
    }

    /** Stellung mit entfernter Figur (für Röntgen- und Abtauschberechnungen). */
    public Position without(int square) {
        Piece[] next = Arrays.copyOf(board, 64);
        next[square] = null;
        return new Position(next, sideToMove, castling, enPassantSquare, halfmoveClock, fullmoveNumber);
    }

    /** Versetzt eine Figur ohne weitere Zustandsänderung (nur für Abtauschberechnungen). */
    Position relocate(int from, int to) {
        Piece[] next = Arrays.copyOf(board, 64);
        next[to] = next[from];
        next[from] = null;
        return new Position(next, sideToMove, castling, enPassantSquare, halfmoveClock, fullmoveNumber);
    }

    /** Dieselbe Stellung mit anderer Seite am Zug (Nullzug-Betrachtung für Drohungen). */
    public Position withSideToMove(Color color) {
        return new Position(board, color, castling, Squares.NONE, halfmoveClock, fullmoveNumber);
    }

    @Override
    public String toString() {
        return toFen();
    }
}
