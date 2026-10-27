package de.chesscoach.domain.position;

import java.util.List;

/**
 * Statische Abtauschbewertung (Static Exchange Evaluation) auf einem Feld.
 *
 * <p>Die Berechnung ist eine Heuristik: Sie berücksichtigt Röntgenangriffe durch das Entfernen
 * bereits schlagender Figuren, aber keine Fesselungen, Zwischenzüge oder Schachgebote. Ergebnisse
 * werden deshalb in den Coaching-Regeln nur zusammen mit legalen Zügen bzw. Engine-Varianten als
 * gesicherte Aussage verwendet.</p>
 */
public final class StaticExchange {

    private StaticExchange() {
    }

    public static int centipawns(PieceKind kind) {
        return kind == PieceKind.KING ? 10_000 : kind.value() * 100;
    }

    /**
     * Materialgewinn in Centipawns für {@code side}, wenn {@code side} auf {@code target} schlägt und
     * beide Seiten anschließend optimal weiterschlagen oder aufhören. 0, wenn kein Schlagzug möglich ist.
     */
    public static int evaluate(Position position, int target, Color side) {
        Piece targetPiece = position.pieceAt(target);
        if (targetPiece == null || targetPiece.color() == side) {
            return 0;
        }
        int[] gain = new int[40];
        int depth = 0;
        gain[0] = centipawns(targetPiece.kind());
        Position current = position;
        Color stm = side;
        int attacker = leastValuableAttacker(current, target, stm);
        while (attacker != Squares.NONE) {
            Piece attackingPiece = current.pieceAt(attacker);
            if (attackingPiece.kind() == PieceKind.KING
                    && !current.without(attacker).attackersOf(target, stm.opposite()).isEmpty()) {
                break;
            }
            depth++;
            gain[depth] = centipawns(attackingPiece.kind()) - gain[depth - 1];
            if (Math.max(-gain[depth - 1], gain[depth]) < 0) {
                break;
            }
            current = current.relocate(attacker, target);
            stm = stm.opposite();
            attacker = leastValuableAttacker(current, target, stm);
        }
        while (--depth > 0) {
            gain[depth - 1] = -Math.max(-gain[depth - 1], gain[depth]);
        }
        return gain[0];
    }

    /** Wie {@link #evaluate}, aber 0, falls die Seite nicht schlagen kann; nie negativ (Schlagen ist freiwillig). */
    public static int bestCaptureGain(Position position, int target, Color side) {
        if (position.attackersOf(target, side).isEmpty()) {
            return 0;
        }
        return Math.max(0, evaluate(position, target, side));
    }

    public static int leastValuableAttacker(Position position, int target, Color side) {
        List<Integer> attackers = position.attackersOf(target, side);
        int best = Squares.NONE;
        int bestValue = Integer.MAX_VALUE;
        for (int sq : attackers) {
            int value = centipawns(position.pieceAt(sq).kind());
            if (value < bestValue) {
                bestValue = value;
                best = sq;
            }
        }
        return best;
    }
}
