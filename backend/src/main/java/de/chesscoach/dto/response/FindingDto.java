package de.chesscoach.dto.response;

import java.util.List;

/**
 * Befund einer Coaching-Regel bzw. der Engine.
 *
 * @param source     ENGINE oder COACHING_RULE (FR-04: Herkunft der Erkenntnis)
 * @param certainty  VERIFIED (durch legale Züge/Variante belegt) oder HEURISTIC
 * @param score      abgestufte Bewertung von -4 (stark nachteilig) bis +4 (stark vorteilhaft)
 */
public record FindingDto(String ruleId, String category, String source, String severity, String polarity,
                         String certainty, String title, String explanation, List<String> affectedSquares,
                         List<String> affectedPieces, EvidenceDto evidence, List<String> relatedVariationIds,
                         String learningPrinciple, boolean overriddenByTactics, int score) {

    public record EvidenceDto(String startFen, String triggerMove, List<String> line, Integer materialBefore,
                              Integer materialAfter, String outcome, String certainty, String confidence) {
    }
}
