package de.chesscoach.dto.request;

import de.chesscoach.domain.game.AnalysisProfile;
import de.chesscoach.domain.game.Difficulty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Einstellungen der laufenden Sitzung. Eine Änderung des Analyseprofils markiert bestehende Analysen als veraltet.
 */
public record SettingsRequest(@NotBlank @Pattern(regexp = "^[0-9a-f]{32}$") String gameId,
                              Difficulty difficulty, AnalysisProfile analysisProfile) {
}
