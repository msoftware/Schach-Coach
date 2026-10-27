package de.chesscoach.domain.coaching;

import java.util.List;

/**
 * Strukturierter Befund einer Coaching-Regel.
 *
 * @param ruleId              eindeutige Regel-ID (z. B. TACT-002, STRAT-005)
 * @param category            Ebene der Regel
 * @param source              Engine oder eigene Coaching-Logik
 * @param severity            Gewichtung
 * @param polarity            Wirkung aus Sicht des Benutzers
 * @param certainty           gesichert oder heuristisch
 * @param title               kurzer Titel
 * @param explanation         konkrete Begründung mit Bezug auf Felder und Figuren
 * @param affectedSquares     beteiligte Felder (z. B. "e5")
 * @param affectedPieces      beteiligte Figuren (z. B. "Springer e5")
 * @param evidence            Beweiskette
 * @param relatedVariationIds zugehörige Varianten-IDs der Analyse
 * @param learningPrinciple   Schachprinzip zum Merken
 * @param overriddenByTactics true, wenn eine nachgewiesene taktische Folge diese Heuristik überstimmt
 */
public record CoachingFinding(String ruleId, FindingCategory category, FindingSource source, Severity severity,
                              Polarity polarity, Certainty certainty, String title, String explanation,
                              List<String> affectedSquares, List<String> affectedPieces, Evidence evidence,
                              List<String> relatedVariationIds, String learningPrinciple, boolean overriddenByTactics) {

    public CoachingFinding {
        affectedSquares = affectedSquares == null ? List.of() : List.copyOf(affectedSquares);
        affectedPieces = affectedPieces == null ? List.of() : List.copyOf(affectedPieces);
        relatedVariationIds = relatedVariationIds == null ? List.of() : List.copyOf(relatedVariationIds);
    }

    public CoachingFinding withOverriddenByTactics() {
        return new CoachingFinding(ruleId, category, source, severity, polarity, certainty, title, explanation,
                affectedSquares, affectedPieces, evidence, relatedVariationIds, learningPrinciple, true);
    }

    public CoachingFinding withVariation(String variationId) {
        return new CoachingFinding(ruleId, category, source, severity, polarity, certainty, title, explanation,
                affectedSquares, affectedPieces, evidence, List.of(variationId), learningPrinciple, overriddenByTactics);
    }

    public boolean isNegative() {
        return polarity == Polarity.NEGATIVE;
    }

    public boolean isPositive() {
        return polarity == Polarity.POSITIVE;
    }

    public boolean isVerified() {
        return certainty == Certainty.VERIFIED;
    }
}
