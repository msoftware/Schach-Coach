package de.chesscoach.domain.position;

/**
 * Legaler Zug mit den für die Coaching-Logik relevanten Eigenschaften.
 * Instanzen stammen ausschließlich vom {@link MoveOracle} und sind damit regelkonform geprüft.
 */
public record MoveInfo(String uci, String san, int from, int to, PieceKind promotion, boolean capture, boolean check) {

    public boolean isPromotion() {
        return promotion != null;
    }
}
