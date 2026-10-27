package de.chesscoach.chess;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Piece;
import com.github.bhlangonijr.chesslib.Side;
import com.github.bhlangonijr.chesslib.move.Move;
import com.github.bhlangonijr.chesslib.move.MoveConversionException;
import com.github.bhlangonijr.chesslib.move.MoveList;
import de.chesscoach.domain.game.GameStatus;
import de.chesscoach.domain.position.Color;
import de.chesscoach.domain.position.PieceKind;
import de.chesscoach.domain.position.Position;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Adapter um ein Chesslib-{@link Board}. Kapselt die veränderliche Chesslib-Datenstruktur:
 * Jede Zugausführung erzeugt einen neuen Adapter, der aus Ausgangsstellung und Zughistorie
 * rekonstruiert wird. Dadurch bleibt die Historie für die Wiederholungserkennung korrekt.
 */
public final class ChessPositionAdapter {

    private final String initialFen;
    private final List<String> uciMoves;
    private final Board board;

    ChessPositionAdapter(String initialFen, List<String> uciMoves, Board board) {
        this.initialFen = initialFen;
        this.uciMoves = List.copyOf(uciMoves);
        this.board = board;
    }

    static ChessPositionAdapter replay(String initialFen, List<String> uciMoves) {
        Board board = new Board();
        board.loadFromFen(initialFen);
        for (String uci : uciMoves) {
            Move move = findLegal(board, uci)
                    .orElseThrow(() -> new IllegalMoveException(uci, "Zug " + uci + " ist nicht legal"));
            board.doMove(move);
        }
        return new ChessPositionAdapter(initialFen, uciMoves, board);
    }

    private static Optional<Move> findLegal(Board board, String uci) {
        if (uci == null) {
            return Optional.empty();
        }
        String normalized = uci.trim().toLowerCase(Locale.ROOT);
        for (Move move : board.legalMoves()) {
            if (toUci(move).equals(normalized)) {
                return Optional.of(move);
            }
        }
        return Optional.empty();
    }

    static String toUci(Move move) {
        String base = move.getFrom().value().toLowerCase(Locale.ROOT) + move.getTo().value().toLowerCase(Locale.ROOT);
        Piece promotion = move.getPromotion();
        if (promotion != null && promotion != Piece.NONE) {
            base += promotion.getFenSymbol().toLowerCase(Locale.ROOT);
        }
        return base;
    }

    public String initialFen() {
        return initialFen;
    }

    public List<String> uciMoves() {
        return uciMoves;
    }

    public String fen() {
        return board.getFen();
    }

    public Position position() {
        return Position.fromFen(fen());
    }

    public Color sideToMove() {
        return board.getSideToMove() == Side.WHITE ? Color.WHITE : Color.BLACK;
    }

    public boolean inCheck() {
        return board.isKingAttacked();
    }

    public boolean isLegal(String uci) {
        return findLegal(board, uci).isPresent();
    }

    public List<LegalMove> legalMoves() {
        List<LegalMove> result = new ArrayList<>();
        String fen = fen();
        for (Move move : board.legalMoves()) {
            result.add(describe(fen, move));
        }
        return result;
    }

    public List<String> legalUciMoves() {
        List<String> result = new ArrayList<>();
        for (Move move : board.legalMoves()) {
            result.add(toUci(move));
        }
        return result;
    }

    public LegalMove describe(String uci) {
        Move move = findLegal(board, uci)
                .orElseThrow(() -> new IllegalMoveException(uci, "Zug " + uci + " ist in dieser Stellung nicht legal"));
        return describe(fen(), move);
    }

    private LegalMove describe(String fen, Move move) {
        boolean capture = board.getPiece(move.getTo()) != Piece.NONE
                || (board.getPiece(move.getFrom()).getPieceType() == com.github.bhlangonijr.chesslib.PieceType.PAWN
                && move.getFrom().getFile() != move.getTo().getFile());
        board.doMove(move);
        boolean check = board.isKingAttacked();
        board.undoMove();
        PieceKind promotion = null;
        if (move.getPromotion() != null && move.getPromotion() != Piece.NONE) {
            promotion = PieceKind.fromChar(move.getPromotion().getFenSymbol().charAt(0));
        }
        return new LegalMove(toUci(move), san(fen, move), move.getFrom().value().toLowerCase(Locale.ROOT),
                move.getTo().value().toLowerCase(Locale.ROOT), promotion, capture, check);
    }

    private static String san(String fen, Move move) {
        try {
            MoveList list = new MoveList(fen);
            list.add(move);
            return list.toSanArray()[0];
        } catch (MoveConversionException e) {
            throw new IllegalStateException("SAN-Konvertierung fehlgeschlagen für " + toUci(move), e);
        }
    }

    public ChessPositionAdapter play(String uci) {
        if (!isLegal(uci)) {
            throw new IllegalMoveException(uci, "Zug " + uci + " ist in dieser Stellung nicht legal");
        }
        List<String> next = new ArrayList<>(uciMoves);
        next.add(uci.trim().toLowerCase(Locale.ROOT));
        return replay(initialFen, next);
    }

    public GameStatus status() {
        if (board.isMated()) {
            return GameStatus.CHECKMATE;
        }
        if (board.isStaleMate()) {
            return GameStatus.STALEMATE;
        }
        if (board.isInsufficientMaterial()) {
            return GameStatus.DRAW_INSUFFICIENT_MATERIAL;
        }
        if (board.isRepetition()) {
            return GameStatus.DRAW_THREEFOLD_REPETITION;
        }
        if (board.getHalfMoveCounter() != null && board.getHalfMoveCounter() >= 100) {
            return GameStatus.DRAW_FIFTY_MOVE_RULE;
        }
        return GameStatus.IN_PROGRESS;
    }
}
