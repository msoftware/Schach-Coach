package de.chesscoach.dto.response;

import de.chesscoach.domain.analysis.Score;

/**
 * Bewertung mit Typ (CP oder MATE), immer aus Sicht des Benutzers.
 *
 * @param type        CP (Centipawns) oder MATE (Züge bis Matt; positiv = Benutzer setzt matt)
 * @param value       Wert
 * @param display     Anzeigeform, z. B. "+0.35" oder "M3"
 * @param perspective immer "USER"
 */
public record ScoreDto(String type, int value, String display, String perspective) {

    public static ScoreDto of(Score score) {
        return score == null ? null : new ScoreDto(score.type().name(), score.value(), score.display(), "USER");
    }
}
