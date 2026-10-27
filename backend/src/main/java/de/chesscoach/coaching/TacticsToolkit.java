package de.chesscoach.coaching;

import de.chesscoach.domain.position.Color;
import de.chesscoach.domain.position.MoveInfo;
import de.chesscoach.domain.position.MoveOracle;
import de.chesscoach.domain.position.Piece;
import de.chesscoach.domain.position.PieceKind;
import de.chesscoach.domain.position.Position;
import de.chesscoach.domain.position.Squares;
import de.chesscoach.domain.position.StaticExchange;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Gemeinsame taktische Hilfsfunktionen der Regeln. Alle Zugfolgen stammen aus dem {@link MoveOracle}. */
public final class TacticsToolkit {

    private TacticsToolkit() {
    }

    /**
     * Figuren von {@code owner}, die der Gegner nach statischer Abtauschrechnung mit Gewinn schlagen könnte.
     * Könige werden nicht berücksichtigt.
     */
    public static List<Integer> enPrise(Position position, Color owner, int minGainCentipawns) {
        List<Integer> result = new ArrayList<>();
        for (int sq : position.squaresOf(owner)) {
            if (position.pieceAt(sq).kind() == PieceKind.KING) {
                continue;
            }
            if (StaticExchange.bestCaptureGain(position, sq, owner.opposite()) >= minGainCentipawns) {
                result.add(sq);
            }
        }
        return result;
    }

    /** Hat die Seite am Zug einen legalen Schlagzug auf das Feld? */
    public static boolean hasLegalCaptureOn(List<MoveInfo> legalMoves, int square) {
        return legalMoves.stream().anyMatch(m -> m.to() == square && m.capture());
    }

    /**
     * Relevante Ziele der Figur auf {@code attacker}: gegnerische Figuren, die wertvoller als der Angreifer
     * oder ungedeckt sind, sowie der gegnerische König (Schach).
     */
    public static List<Integer> relevantTargets(Position position, int attacker) {
        Piece piece = position.pieceAt(attacker);
        List<Integer> result = new ArrayList<>();
        if (piece == null) {
            return result;
        }
        long attacks = position.attacksFrom(attacker);
        for (int sq = 0; sq < 64; sq++) {
            if ((attacks & (1L << sq)) == 0) {
                continue;
            }
            Piece target = position.pieceAt(sq);
            if (target == null || target.color() == piece.color()) {
                continue;
            }
            if (target.kind() == PieceKind.KING) {
                result.add(sq);
                continue;
            }
            boolean undefended = position.attackersOf(sq, target.color()).isEmpty();
            boolean moreValuable = target.value() > piece.value();
            if (moreValuable || (undefended && target.kind() != PieceKind.PAWN)) {
                result.add(sq);
            }
        }
        return result;
    }

    /** Ein legaler Zug der Seite am Zug, der sofort matt setzt. */
    public static Optional<MoveInfo> mateInOne(Position position, MoveOracle oracle) {
        for (MoveInfo move : oracle.legalMoves(position.toFen())) {
            if (!move.check()) {
                continue;
            }
            Position next = position.play(move.from(), move.to(), move.promotion());
            if (oracle.isCheckmate(next.toFen())) {
                return Optional.of(move);
            }
        }
        return Optional.empty();
    }

    /**
     * Drohung der Seite {@code threatening}: Hätte sie in der Stellung noch einmal den Zug (Nullzug-Betrachtung),
     * könnte sie dann matt setzen? Nur zulässig, wenn die andere Seite nicht im Schach steht.
     */
    public static Optional<MoveInfo> mateThreat(Position position, Color threatening, MoveOracle oracle) {
        if (position.isInCheck(threatening.opposite()) || position.isInCheck(threatening)) {
            return Optional.empty();
        }
        return mateInOne(position.withSideToMove(threatening), oracle);
    }

    /** Liste der Halbzüge ab {@code from}, solange ununterbrochen geschlagen wird. */
    public static int captureSequenceLength(EngineLine line, int from) {
        int count = 0;
        for (int i = from; i < line.size(); i++) {
            if (!line.ply(i).isCapture()) {
                break;
            }
            count++;
        }
        return count;
    }

    /** Durch das Wegziehen von {@code from} neu entstandene Angriffe langschrittiger Figuren auf relevante Ziele. */
    public static List<int[]> discoveredAttacks(Position before, Position after, Color mover, int from, int movedTo) {
        List<int[]> result = new ArrayList<>();
        for (int sq : after.squaresOf(mover)) {
            if (sq == movedTo || !after.pieceAt(sq).kind().isSlider()) {
                continue;
            }
            for (int target : relevantTargets(after, sq)) {
                if (before.attacks(sq, target) || !Position.between(sq, target).contains(from)) {
                    continue;
                }
                result.add(new int[]{sq, target});
            }
        }
        return result;
    }

    /** Einfache Prüfung, ob ein Feld zur Königszone (König und Nachbarfelder) gehört. */
    public static List<Integer> kingZone(Position position, Color color) {
        List<Integer> zone = new ArrayList<>();
        int king = position.kingSquare(color);
        if (king == Squares.NONE) {
            return zone;
        }
        for (int df = -1; df <= 1; df++) {
            for (int dr = -1; dr <= 1; dr++) {
                int f = Squares.file(king) + df;
                int r = Squares.rank(king) + dr;
                if (Squares.onBoard(f, r)) {
                    zone.add(Squares.of(f, r));
                }
            }
        }
        return zone;
    }
}
