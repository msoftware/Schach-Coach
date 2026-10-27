package de.chesscoach.domain.position;

import java.util.List;

/**
 * Zugriff der Coaching-Logik auf die geprüfte Regelimplementierung.
 * Die Coaching-Regeln generieren selbst keine Züge, sondern fragen legale Züge hier ab.
 */
public interface MoveOracle {

    List<MoveInfo> legalMoves(String fen);

    boolean isCheckmate(String fen);

    boolean isStalemate(String fen);
}
