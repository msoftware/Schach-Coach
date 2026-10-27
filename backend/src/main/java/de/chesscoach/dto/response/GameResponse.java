package de.chesscoach.dto.response;

import java.time.Instant;
import java.util.List;

/**
 * Zustand einer Partie (API-Version 1).
 *
 * @param apiVersion      Version des JSON-Vertrags
 * @param gameId          zufällige Sitzungskennung
 * @param status          IN_PROGRESS, CHECKMATE, STALEMATE, DRAW_INSUFFICIENT_MATERIAL, DRAW_THREEFOLD_REPETITION,
 *                        DRAW_FIFTY_MOVE_RULE oder RESIGNED
 * @param statusText      deutsche Beschreibung des Status
 * @param result          "1-0", "0-1", "1/2-1/2" oder "*"
 * @param winner          WHITE, BLACK oder null
 * @param positionVersion fortlaufende Version der Partie; jede Änderung erhöht sie
 * @param legalMoves      legale Züge des Benutzers (leer, wenn der Benutzer nicht am Zug ist)
 * @param checkSquare     Feld des Königs im Schach oder null
 * @param lastMove        letzter Zug (Start- und Zielfeld) oder null
 * @param engineStatus    READY oder UNAVAILABLE
 */
public record GameResponse(String apiVersion, String gameId, String status, String statusText, String result,
                           String winner, String playerColor, String sideToMove, String fen, String initialFen,
                           int positionVersion, String difficulty, String analysisProfile, int settingsVersion,
                           boolean inCheck, String checkSquare, LastMove lastMove, List<MoveDto> moves,
                           List<LegalMoveDto> legalMoves, boolean canUndo, boolean canRedo, boolean engineThinking,
                           String engineStatus, Instant createdAt, Instant updatedAt) {

    public record LastMove(String from, String to) {
    }

    /**
     * Ein Halbzug der Zugliste.
     *
     * @param analysisStatus PENDING, RUNNING, COMPLETED, FAILED, CANCELLED oder null (Computerzug bzw. keine Analyse)
     * @param analysisOutdated true, wenn die Analyse mit veralteten Einstellungen erstellt wurde
     */
    public record MoveDto(int ply, String moveId, String uci, String san, String from, String to, String color,
                          boolean byUser, String fenBefore, String fenAfter, int positionVersion, String analysisStatus,
                          boolean analysisOutdated, Instant timestamp) {
    }

    /** Legaler Zug; {@code promotion} ist "q", "r", "b", "n" oder null. */
    public record LegalMoveDto(String uci, String san, String from, String to, String promotion) {
    }
}
