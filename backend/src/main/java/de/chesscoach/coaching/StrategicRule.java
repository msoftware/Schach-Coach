package de.chesscoach.coaching;

import de.chesscoach.domain.coaching.FindingCategory;

/**
 * Regel der Ebene 2 (strategische Prinzipien nach Edward Lasker, "Chess Strategy").
 * Strategische Regeln greifen nicht auf Engine-spezifische Klassen zu, sondern nur auf Stellungsmerkmale.
 */
public interface StrategicRule extends CoachingRule {

    @Override
    default FindingCategory category() {
        return FindingCategory.STRATEGIC;
    }
}
