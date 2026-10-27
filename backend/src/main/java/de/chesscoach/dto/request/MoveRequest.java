package de.chesscoach.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * Benutzerzug.
 *
 * @param uciMove                 Zug in UCI-Notation (Umwandlung mit Kleinbuchstabe, z. B. "e7e8q")
 * @param expectedPositionVersion Positionsversion, auf der der Zug beruht (Schutz vor veralteten und doppelten Anfragen)
 */
public record MoveRequest(
        @Schema(example = "e2e4") @NotBlank @Pattern(regexp = "^[a-h][1-8][a-h][1-8][qrbn]?$") String uciMove,
        @Schema(example = "0") @NotNull @Min(0) Integer expectedPositionVersion) {
}
