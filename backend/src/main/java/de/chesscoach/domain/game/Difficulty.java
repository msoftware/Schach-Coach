package de.chesscoach.domain.game;

/** Spielstärke des Computergegners. Die konkreten Engine-Parameter sind konfigurierbar (application.yml). */
public enum Difficulty {
    BEGINNER("Anfänger"),
    EASY("Leicht"),
    MEDIUM("Mittel"),
    STRONG("Stark"),
    MAXIMUM("Maximal");

    private final String german;

    Difficulty(String german) {
        this.german = german;
    }

    public String german() {
        return german;
    }
}
