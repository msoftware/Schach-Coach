package de.chesscoach.dto.request;

import de.chesscoach.domain.game.AnalysisProfile;
import de.chesscoach.domain.game.Difficulty;
import de.chesscoach.domain.position.Color;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Neue Partie starten oder eine lokal gespeicherte Partie wiederherstellen.
 *
 * @param playerColor     Farbe des Benutzers (Standard: WHITE)
 * @param difficulty      Spielstärke des Computers (Standard: MEDIUM)
 * @param analysisProfile Gründlichkeit der Analyse (Standard: NORMAL)
 * @param initialFen      optionale Ausgangsstellung (Standard: reguläre Grundstellung)
 * @param moves           optionale Zughistorie in UCI-Notation zur Wiederherstellung
 */
public record CreateGameRequest(
        @Schema(example = "WHITE") Color playerColor,
        @Schema(example = "MEDIUM") Difficulty difficulty,
        AnalysisProfile analysisProfile,
        @Size(max = 100) String initialFen,
        @Size(max = 600) List<@Pattern(regexp = "^[a-h][1-8][a-h][1-8][qrbn]?$") String> moves) {
}
