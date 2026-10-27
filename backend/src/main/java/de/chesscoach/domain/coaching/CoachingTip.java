package de.chesscoach.domain.coaching;

/** Kompakte Lernkarte zu einem Zug (Requirements 10.1). */
public record CoachingTip(String title, String observation, String why, String betterAlternative,
                          String expectedConsequence, String learningPrinciple, Confidence confidence) {
}
