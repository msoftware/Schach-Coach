package de.chesscoach.dto.request;

import de.chesscoach.domain.game.Difficulty;
import de.chesscoach.domain.position.Color;
import jakarta.validation.constraints.Min;

/** Partie mit gleicher Kennung neu starten; Farbe und Spielstärke können geändert werden. */
public record RestartRequest(@Min(0) Integer expectedPositionVersion, Color playerColor, Difficulty difficulty) {
}
