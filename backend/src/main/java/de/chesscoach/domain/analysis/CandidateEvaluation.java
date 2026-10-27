package de.chesscoach.domain.analysis;

import de.chesscoach.domain.coaching.CoachingFinding;

import java.util.List;

/**
 * Bewerteter Kandidatenzug (Requirements 6.4).
 *
 * @param variationId    Kennung der Variante
 * @param uci            Zug in UCI-Notation
 * @param san            Zug in SAN
 * @param score          Bewertung aus Sicht des Benutzers
 * @param depth          erreichte Suchtiefe der Folgestellung
 * @param nodes          durchsuchte Knoten
 * @param line           Hauptvariante beginnend mit dem Kandidatenzug (nur legale Züge)
 * @param lineTruncated  true, wenn die Engine-Variante wegen eines ungültigen Zuges gekürzt wurde
 * @param played         Kandidat ist der gespielte Zug
 * @param best           Kandidat ist der beste gefundene Zug
 * @param deltaToPlayed  Bewertungsunterschied zum gespielten Zug in Centipawns (positiv = besser als gespielt)
 * @param findings       taktische Motive sowie strategische Vor- und Nachteile dieses Kandidaten
 * @param searchLine     immer true: Die Variante ist eine Hauptvariante der Suche, keine erzwungene Zugfolge
 */
public record CandidateEvaluation(String variationId, String uci, String san, Score score, int depth, long nodes,
                                  List<VariationMove> line, boolean lineTruncated, boolean played, boolean best,
                                  Integer deltaToPlayed, List<CoachingFinding> findings, boolean searchLine) {

    public CandidateEvaluation {
        line = line == null ? List.of() : List.copyOf(line);
        findings = findings == null ? List.of() : List.copyOf(findings);
    }
}
