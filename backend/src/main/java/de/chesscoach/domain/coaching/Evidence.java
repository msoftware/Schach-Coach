package de.chesscoach.domain.coaching;

import java.util.List;

/**
 * Beweiskette eines Befunds (Requirements 5.1).
 *
 * @param startFen        Ausgangsstellung
 * @param triggerMove     auslösender Zug (SAN)
 * @param line            gegnerische Antwort und Folgezüge (SAN), ausschließlich legale Züge
 * @param materialBefore  Materialbilanz aus Sicht des Benutzers vor dem Zug (Bauerneinheiten)
 * @param materialAfter   Materialbilanz am Ende der belegten Folge
 * @param outcome         resultierende Folge in Worten (z. B. "Matt" oder "Figurengewinn")
 * @param certainty       gesichert oder heuristisch
 * @param confidence      Konfidenz der Erkennung
 */
public record Evidence(String startFen, String triggerMove, List<String> line, Integer materialBefore,
                       Integer materialAfter, String outcome, Certainty certainty, Confidence confidence) {

    public Evidence {
        line = line == null ? List.of() : List.copyOf(line);
    }

    public static Evidence heuristic(String startFen, String triggerMove, String outcome) {
        return new Evidence(startFen, triggerMove, List.of(), null, null, outcome, Certainty.HEURISTIC, Confidence.MEDIUM);
    }
}
