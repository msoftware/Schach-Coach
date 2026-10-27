package de.chesscoach.application;

/** Fachliche Fehlercodes der API mit HTTP-Status und deutschem Titel. */
public enum ErrorCode {
    GAME_NOT_FOUND(404, "Partie nicht gefunden"),
    ILLEGAL_MOVE(422, "Ungültiger Zug"),
    INVALID_FEN(422, "Ungültige Stellung"),
    INVALID_RESTORE(422, "Partie kann nicht wiederhergestellt werden"),
    POSITION_VERSION_CONFLICT(409, "Veraltete Positionsversion"),
    NOT_YOUR_TURN(409, "Nicht am Zug"),
    GAME_FINISHED(409, "Partie ist beendet"),
    ENGINE_BUSY(409, "Der Computer rechnet bereits"),
    NOTHING_TO_UNDO(409, "Kein Zug zum Zurücknehmen"),
    NOTHING_TO_REDO(409, "Kein Zug zum Wiederholen"),
    ANALYSIS_NOT_FOUND(404, "Keine Analyse vorhanden"),
    ANALYSIS_NOT_AVAILABLE(409, "Analyse nicht möglich"),
    ENGINE_UNAVAILABLE(503, "Schach-Engine nicht verfügbar"),
    ENGINE_TIMEOUT(504, "Zeitüberschreitung der Engine"),
    ENGINE_ERROR(502, "Fehlerhafte Engine-Antwort"),
    TOO_MANY_GAMES(503, "Zu viele aktive Partien"),
    RATE_LIMITED(429, "Zu viele Anfragen"),
    VALIDATION_FAILED(400, "Ungültige Anfrage"),
    INTERNAL_ERROR(500, "Interner Fehler");

    private final int status;
    private final String title;

    ErrorCode(int status, String title) {
        this.status = status;
        this.title = title;
    }

    public int status() {
        return status;
    }

    public String title() {
        return title;
    }

    public String type() {
        return "urn:chess-coach:error:" + name().toLowerCase().replace('_', '-');
    }
}
