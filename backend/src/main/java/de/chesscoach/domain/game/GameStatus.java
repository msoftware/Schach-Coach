package de.chesscoach.domain.game;

/** Spielzustand einer Partie. */
public enum GameStatus {
    IN_PROGRESS("Partie läuft"),
    CHECKMATE("Schachmatt"),
    STALEMATE("Patt – Remis"),
    DRAW_INSUFFICIENT_MATERIAL("Remis durch unzureichendes Material"),
    DRAW_THREEFOLD_REPETITION("Remis durch dreifache Stellungswiederholung"),
    DRAW_FIFTY_MOVE_RULE("Remis nach der 50-Züge-Regel"),
    RESIGNED("Partie aufgegeben");

    private final String germanDescription;

    GameStatus(String germanDescription) {
        this.germanDescription = germanDescription;
    }

    public boolean isFinished() {
        return this != IN_PROGRESS;
    }

    public boolean isDraw() {
        return this == STALEMATE || this == DRAW_INSUFFICIENT_MATERIAL
                || this == DRAW_THREEFOLD_REPETITION || this == DRAW_FIFTY_MOVE_RULE;
    }

    public String germanDescription() {
        return germanDescription;
    }
}
