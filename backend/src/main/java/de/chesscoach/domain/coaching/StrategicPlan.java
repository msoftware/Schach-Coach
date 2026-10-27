package de.chesscoach.domain.coaching;

import java.util.List;

/**
 * Langfristiger Plan, der an konkrete Stellungsmerkmale gebunden ist (STRAT-011).
 *
 * @param side        "USER" oder "OPPONENT"
 * @param title       Kurzbeschreibung
 * @param explanation Begründung mit Stellungsbezug
 * @param squares     relevante Felder
 * @param basis       Merkmal, auf dem der Plan beruht (z. B. "isolierter Bauer d5")
 * @param source      Regel oder Engine
 */
public record StrategicPlan(String side, String title, String explanation, List<String> squares, String basis,
                            FindingSource source) {

    public StrategicPlan {
        squares = squares == null ? List.of() : List.copyOf(squares);
    }
}
