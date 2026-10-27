package de.chesscoach.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Suchparameter der Coaching-Analyse (Präfix {@code chesscoach.analysis}). Werte gelten für das Profil NORMAL
 * und werden für FAST/DEEP skaliert.
 *
 * @param rootNodes            Knoten für die Bewertung der Stellung vor dem Zug (P0)
 * @param screeningNodes       Knoten für die Vorauswahl aller legalen Züge
 * @param candidateNodes       Knoten für die Bewertung jedes ausgewählten Kandidaten (P_u, P_b, ...)
 * @param maxCandidates        Anzahl Kandidaten zusätzlich zu gespieltem und bestem Zug
 * @param criticalFactor       Multiplikator der Kandidatenknoten in taktisch kritischen Stellungen
 * @param deepenFactor         Multiplikator bei der Vertiefung über /variations
 * @param minReliableDepth     Mindesttiefe für eine zuverlässige Einordnung
 * @param maxLinePlies         maximale Länge der angezeigten Varianten
 * @param maxQueuedAnalyses    maximale Anzahl wartender Analysen
 * @param stalenessCheckMillis Intervall, in dem laufende Analysen auf Veraltung geprüft werden
 * @param evaluationNodes      Knoten für die Stellungsbewertung der Bewertungsleiste (unabhängig vom Profil)
 */
@ConfigurationProperties("chesscoach.analysis")
public record AnalysisProperties(@DefaultValue("150000") long rootNodes,
                                 @DefaultValue("3000") long screeningNodes,
                                 @DefaultValue("80000") long candidateNodes,
                                 @DefaultValue("4") int maxCandidates,
                                 @DefaultValue("2.0") double criticalFactor,
                                 @DefaultValue("3.0") double deepenFactor,
                                 @DefaultValue("9") int minReliableDepth,
                                 @DefaultValue("12") int maxLinePlies,
                                 @DefaultValue("32") int maxQueuedAnalyses,
                                 @DefaultValue("200") long stalenessCheckMillis,
                                 @DefaultValue("40000") long evaluationNodes) {
}
