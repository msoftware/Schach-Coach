package de.chesscoach.dto.response;

import de.chesscoach.domain.analysis.Score;

/**
 * Bewertung mit Typ (CP oder MATE) aus Sicht des Benutzers – nur die Bewertungsleiste verwendet die Sicht von Weiß.
 *
 * @param type        CP (Centipawns) oder MATE (Züge bis Matt; positiv = die betrachtete Seite setzt matt)
 * @param value       Wert
 * @param display     Anzeigeform, z. B. "+0.35" oder "M3"
 * @param perspective "USER" (Coaching-Analysen) oder "WHITE" (Bewertungsleiste)
 */
public record ScoreDto(String type, int value, String display, String perspective) {

    public static ScoreDto of(Score score) {
        return score == null ? null : new ScoreDto(score.type().name(), score.value(), score.display(), "USER");
    }

    /** Bewertung aus Sicht von Weiß (Bewertungsleiste). */
    public static ScoreDto ofWhite(Score score) {
        return score == null ? null : new ScoreDto(score.type().name(), score.value(), score.display(), "WHITE");
    }
}
