package de.chesscoach.coaching;

import de.chesscoach.domain.position.Color;
import de.chesscoach.domain.position.MoveInfo;
import de.chesscoach.domain.position.Piece;
import de.chesscoach.domain.position.PieceKind;
import de.chesscoach.domain.position.Position;
import de.chesscoach.domain.position.Squares;

import java.util.ArrayList;
import java.util.List;

/**
 * Geprüfte Zugfolge, die mit dem betrachteten Zug beginnt und mit der Hauptvariante der Engine fortgesetzt wird.
 * Alle Züge sind von der Regelkomponente als legal bestätigt.
 *
 * @param variationId  Kennung der Variante in der Analyse
 * @param start        Ausgangsstellung vor dem ersten Zug
 * @param plies        Halbzüge
 * @param fromEngine   true, wenn die Fortsetzung aus einer Engine-Hauptvariante stammt
 */
public record EngineLine(String variationId, Position start, List<LinePly> plies, boolean fromEngine) {

    public EngineLine {
        plies = List.copyOf(plies);
    }

    /** Baut eine Variante aus bereits geprüften Zügen. */
    public static EngineLine of(String variationId, Position start, List<MoveInfo> moves, boolean fromEngine) {
        List<LinePly> plies = new ArrayList<>();
        Position current = start;
        for (MoveInfo move : moves) {
            Piece moved = current.pieceAt(move.from());
            Piece captured = current.pieceAt(move.to());
            if (captured == null && moved != null && moved.kind() == PieceKind.PAWN
                    && Squares.file(move.from()) != Squares.file(move.to())) {
                captured = current.pieceAt(Squares.of(Squares.file(move.to()), Squares.rank(move.from())));
            }
            Position next = current.play(move.from(), move.to(), move.promotion());
            plies.add(new LinePly(move, current.sideToMove(), moved, captured, current, next));
            current = next;
        }
        return new EngineLine(variationId, start, plies, fromEngine);
    }

    public boolean isEmpty() {
        return plies.isEmpty();
    }

    public int size() {
        return plies.size();
    }

    public LinePly ply(int index) {
        return plies.get(index);
    }

    public Position positionAfter(int plyCount) {
        if (plyCount <= 0 || plies.isEmpty()) {
            return start;
        }
        return plies.get(Math.min(plyCount, plies.size()) - 1).after();
    }

    public Position end() {
        return positionAfter(plies.size());
    }

    public List<String> san() {
        return plies.stream().map(p -> p.move().san()).toList();
    }

    public List<String> uci() {
        return plies.stream().map(p -> p.move().uci()).toList();
    }

    /** Zugfolge mit Zugnummern ab Index {@code from}, höchstens {@code count} Halbzüge. */
    public String numbered(int from, int count) {
        StringBuilder sb = new StringBuilder();
        int end = Math.min(plies.size(), from + count);
        for (int i = from; i < end; i++) {
            LinePly ply = plies.get(i);
            if (!sb.isEmpty()) {
                sb.append(' ');
            }
            if (ply.mover() == Color.WHITE || i == from) {
                sb.append(ply.numberedSan());
            } else {
                sb.append(ply.move().san());
            }
        }
        return sb.toString();
    }

    public List<String> numberedList(int from, int count) {
        List<String> result = new ArrayList<>();
        int end = Math.min(plies.size(), from + count);
        for (int i = from; i < end; i++) {
            result.add(plies.get(i).numberedSan());
        }
        return result;
    }

    public int materialBalanceAfter(int plyCount, Color perspective) {
        return positionAfter(plyCount).materialBalance(perspective);
    }

    /**
     * Materialbilanz an einem "ruhigen" Punkt der Variante innerhalb von {@code maxPlies}: Es wird der späteste
     * Punkt gewählt, nach dem nicht unmittelbar ein weiterer Schlagzug folgt, damit ein laufender Abtausch nicht
     * mitten im Schlagabtausch bewertet wird.
     */
    public int settledMaterialBalance(Color perspective, int maxPlies) {
        int n = Math.min(maxPlies, plies.size());
        for (int k = n; k >= 0; k--) {
            if (k == plies.size() || !plies.get(k).isCapture()) {
                return materialBalanceAfter(k, perspective);
            }
        }
        return materialBalanceAfter(n, perspective);
    }

    /** Index des ruhigen Punktes, der für {@link #settledMaterialBalance} verwendet wird. */
    public int settledIndex(int maxPlies) {
        int n = Math.min(maxPlies, plies.size());
        for (int k = n; k >= 0; k--) {
            if (k == plies.size() || !plies.get(k).isCapture()) {
                return k;
            }
        }
        return n;
    }

    /** Erster Halbzug-Index, an dem die Farbe einen Bauern umwandelt, sonst -1. */
    public int firstPromotion(Color color, int maxPlies) {
        int end = Math.min(maxPlies, plies.size());
        for (int i = 0; i < end; i++) {
            LinePly ply = plies.get(i);
            if (ply.mover() == color && ply.move().isPromotion()) {
                return i;
            }
        }
        return -1;
    }
}
