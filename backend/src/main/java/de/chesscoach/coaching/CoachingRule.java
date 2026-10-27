package de.chesscoach.coaching;

import de.chesscoach.domain.coaching.CoachingFinding;
import de.chesscoach.domain.coaching.FindingCategory;

import java.util.List;

/**
 * Gemeinsame Beschreibung aller Coaching-Regeln. Jede Regel besitzt eine eindeutige ID, dokumentiert ihre
 * Eingabemerkmale und ist einzeln test-, aktivier- und deaktivierbar (NFR-02).
 */
public interface CoachingRule {

    String id();

    String title();

    FindingCategory category();

    /** Version der Regel; Teil der Regelversion für den Analyse-Cache. */
    default String version() {
        return "1.0";
    }

    /** Zweck der Regel (für Dokumentation und API). */
    String purpose();

    /** Dokumentierte Eingabemerkmale. */
    List<String> inputFeatures();

    /** Schachprinzip, das der Benutzer sich merken soll. */
    String learningPrinciple();

    /** Schnelle Vorprüfung, ob die Regel für den Kontext relevant ist. */
    boolean matches(PositionContext context);

    /** Wertet die Regel aus und liefert strukturierte Befunde (leer, wenn nichts Relevantes vorliegt). */
    List<CoachingFinding> evaluate(PositionContext context);
}
