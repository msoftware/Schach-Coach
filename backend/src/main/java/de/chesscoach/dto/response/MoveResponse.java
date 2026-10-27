package de.chesscoach.dto.response;

/**
 * Ergebnis eines Benutzer- oder Computerzuges. Die Coaching-Analyse läuft asynchron: {@code analysisStatus} ist
 * zunächst PENDING und wird über GET /api/games/{gameId}/analysis/{ply} abgefragt.
 *
 * @param replayed true, wenn die Anfrage eine Wiederholung war und der Zug nicht erneut ausgeführt wurde
 */
public record MoveResponse(String gameId, int positionVersion, int ply, String playedMove, String san, String fen,
                           String status, String analysisStatus, boolean replayed, GameResponse game) {
}
