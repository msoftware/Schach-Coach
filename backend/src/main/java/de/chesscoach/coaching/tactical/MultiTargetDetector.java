package de.chesscoach.coaching.tactical;

import de.chesscoach.coaching.EngineLine;
import de.chesscoach.coaching.GermanText;
import de.chesscoach.coaching.LinePly;
import de.chesscoach.coaching.TacticsToolkit;
import de.chesscoach.domain.position.PieceKind;
import de.chesscoach.domain.position.Position;

import java.util.List;
import java.util.Set;

/** Gemeinsame Logik für Gabel (TACT-015) und Doppelangriff (TACT-005). */
final class MultiTargetDetector {

    private MultiTargetDetector() {
    }

    /** Relevante Ziele der von {@code from} nach {@code to} gezogenen Figur, die sie vorher noch nicht angriff. */
    static List<Integer> newTargets(Position before, Position after, int from, int to) {
        return TacticsToolkit.relevantTargets(after, to).stream()
                .filter(t -> !before.attacks(from, t))
                .toList();
    }

    /** Alle relevanten Ziele der Figur auf {@code to} (auch bereits vorher angegriffene). */
    static List<Integer> allTargets(Position after, int to) {
        return TacticsToolkit.relevantTargets(after, to);
    }

    static boolean isForkPiece(PieceKind kind) {
        return kind == PieceKind.KNIGHT || kind == PieceKind.PAWN;
    }

    /** Prüft, ob im Folgezug der angreifenden Seite (Index {@code plyIndex + 1}) eines der Ziele geschlagen wird. */
    static boolean followUpCaptures(EngineLine line, int plyIndex, List<Integer> targets) {
        int next = plyIndex + 2;
        if (line.size() <= next) {
            return false;
        }
        LinePly ply = line.ply(next);
        return ply.isCapture() && targets.contains(ply.move().to());
    }

    static String describeTargets(Position position, List<Integer> targets) {
        return GermanText.join(targets.stream().map(t -> {
            if (position.pieceAt(t).kind() == PieceKind.KING) {
                return "den König " + de.chesscoach.domain.position.Squares.name(t);
            }
            return (position.pieceAt(t).kind().feminine() ? "die " : "den ") + GermanText.pieceOn(position, t);
        }).toList());
    }

    static boolean containsKing(Position position, List<Integer> targets) {
        return targets.stream().anyMatch(t -> position.pieceAt(t).kind() == PieceKind.KING);
    }

    static Set<PieceKind> kinds(Position position, List<Integer> targets) {
        return targets.stream().map(t -> position.pieceAt(t).kind()).collect(java.util.stream.Collectors.toSet());
    }
}
