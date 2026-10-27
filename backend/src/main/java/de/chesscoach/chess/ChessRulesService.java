package de.chesscoach.chess;

import com.github.bhlangonijr.chesslib.Board;
import de.chesscoach.domain.position.Color;
import de.chesscoach.domain.position.MoveInfo;
import de.chesscoach.domain.position.MoveOracle;
import de.chesscoach.domain.position.PieceKind;
import de.chesscoach.domain.position.Position;
import de.chesscoach.domain.position.Squares;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Adapter für Chesslib: einzige Stelle des Systems, an der Schachregeln ausgewertet werden.
 * Frontend, Engine-Integration und Coaching-Logik verwenden diesen Dienst für Legalitätsprüfungen.
 */
@Service
public class ChessRulesService implements MoveOracle {

    public static final String STANDARD_FEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";

    private static final Pattern FEN_PATTERN = Pattern.compile(
            "^[pnbrqkPNBRQK1-8/]{15,90} [wb] (-|[KQkq]{1,4}) (-|[a-h][36]) \\d{1,3} \\d{1,4}$");
    private static final Pattern UCI_PATTERN = Pattern.compile("^[a-h][1-8][a-h][1-8][qrbn]?$");

    /** Ergebnis einer Variantenprüfung: legale Züge bis zum ersten ungültigen Zug. */
    public record ValidatedLine(String startFen, List<LegalMove> moves, List<String> fens, boolean truncated) {
    }

    public ChessPositionAdapter newGame(String initialFen) {
        String fen = initialFen == null || initialFen.isBlank() ? STANDARD_FEN : initialFen.trim();
        validateFen(fen);
        return ChessPositionAdapter.replay(fen, List.of());
    }

    public ChessPositionAdapter replay(String initialFen, List<String> uciMoves) {
        validateFen(initialFen);
        for (String uci : uciMoves) {
            if (!isWellFormedUci(uci)) {
                throw new IllegalMoveException(uci, "Ungültige Zugnotation: " + uci);
            }
        }
        return ChessPositionAdapter.replay(initialFen, uciMoves);
    }

    public ChessPositionAdapter fromFen(String fen) {
        validateFen(fen);
        return ChessPositionAdapter.replay(fen, List.of());
    }

    public boolean isWellFormedUci(String uci) {
        return uci != null && UCI_PATTERN.matcher(uci).matches();
    }

    /** Prüft Syntax und schachliche Plausibilität einer FEN, bevor sie an Chesslib oder die Engine geht. */
    public void validateFen(String fen) {
        if (fen == null || fen.length() > 100 || !FEN_PATTERN.matcher(fen.trim()).matches()) {
            throw new InvalidFenException("Die Stellung (FEN) ist syntaktisch ungültig.");
        }
        Position position;
        try {
            position = Position.fromFen(fen);
        } catch (IllegalArgumentException e) {
            throw new InvalidFenException("Die Stellung (FEN) ist ungültig: " + e.getMessage());
        }
        for (Color color : Color.values()) {
            if (position.count(color, PieceKind.KING) != 1) {
                throw new InvalidFenException("Jede Seite muss genau einen König besitzen.");
            }
            for (int sq : position.squaresOf(color, PieceKind.PAWN)) {
                int rank = Squares.rank(sq);
                if (rank == 0 || rank == 7) {
                    throw new InvalidFenException("Bauern dürfen nicht auf der ersten oder achten Reihe stehen.");
                }
            }
        }
        if (position.isInCheck(position.sideToMove().opposite())) {
            throw new InvalidFenException("Die Seite, die nicht am Zug ist, darf nicht im Schach stehen.");
        }
    }

    /** Prüft eine Engine-Variante Zug für Zug und bricht beim ersten illegalen Zug ab. */
    public ValidatedLine validateLine(String startFen, List<String> uciMoves, int maxPlies) {
        ChessPositionAdapter current = fromFen(startFen);
        List<LegalMove> moves = new ArrayList<>();
        List<String> fens = new ArrayList<>();
        boolean truncated = false;
        for (String uci : uciMoves) {
            if (moves.size() >= maxPlies) {
                break;
            }
            if (!isWellFormedUci(uci) || !current.isLegal(uci)) {
                truncated = true;
                break;
            }
            moves.add(current.describe(uci));
            current = ChessPositionAdapter.replay(current.fen(), List.of(uci));
            fens.add(current.fen());
        }
        return new ValidatedLine(startFen, List.copyOf(moves), List.copyOf(fens), truncated);
    }

    @Override
    public List<MoveInfo> legalMoves(String fen) {
        List<MoveInfo> result = new ArrayList<>();
        for (LegalMove move : fromFen(fen).legalMoves()) {
            result.add(toMoveInfo(move));
        }
        return result;
    }

    public static MoveInfo toMoveInfo(LegalMove move) {
        return new MoveInfo(move.uci(), move.san(), Squares.parse(move.from()), Squares.parse(move.to()),
                move.promotion(), move.capture(), move.check());
    }

    @Override
    public boolean isCheckmate(String fen) {
        Board board = new Board();
        board.loadFromFen(fen);
        return board.isMated();
    }

    @Override
    public boolean isStalemate(String fen) {
        Board board = new Board();
        board.loadFromFen(fen);
        return board.isStaleMate();
    }
}
