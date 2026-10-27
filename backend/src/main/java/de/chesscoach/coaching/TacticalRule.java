package de.chesscoach.coaching;

import de.chesscoach.domain.coaching.FindingCategory;

/** Regel der Ebene 1 (Schachregeln und Taktik). */
public interface TacticalRule extends CoachingRule {

    @Override
    default FindingCategory category() {
        return FindingCategory.TACTICAL;
    }
}
