package de.chesscoach.coaching;

import de.chesscoach.domain.coaching.CoachingFinding;
import de.chesscoach.domain.coaching.CoachingTip;
import de.chesscoach.domain.coaching.ExplanationSection;
import de.chesscoach.domain.coaching.ExplanationType;

import java.util.List;

/**
 * Ergebnis der pädagogischen Ebene (Ebene 3).
 *
 * @param type          Erklärungstyp
 * @param headline      Überschrift, z. B. "Ungenau: 18.h3"
 * @param mainReason    wichtigste Begründung (eine Aussage)
 * @param mainReasonSources Quellen der wichtigsten Begründung
 * @param sections      Antworten auf die sieben Leitfragen
 * @param tip           kompakte Lernkarte
 * @param learningTags  Regel-IDs für die lokale Lernstatistik (wiederkehrende Fehler)
 * @param findings      priorisierte Befunde des gespielten Zuges (inkl. Kennzeichnung überstimmter Heuristiken)
 */
public record MoveExplanation(ExplanationType type, String headline, String mainReason, List<String> mainReasonSources,
                              List<ExplanationSection> sections, CoachingTip tip, List<String> learningTags,
                              List<CoachingFinding> findings) {
}
