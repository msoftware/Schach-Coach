package de.chesscoach.domain.coaching;

/** Erklärungstypen der pädagogischen Ebene (Requirements 5.4). */
public enum ExplanationType {
    TACTICAL_BLUNDER("Taktischer Fehler"),
    MATERIAL_LOSS("Vermeidbarer Materialverlust"),
    MISSED_WIN("Gewinnchance verpasst"),
    MISSED_DEFENSE("Verteidigung übersehen"),
    STRATEGIC_INACCURACY("Strategische Ungenauigkeit"),
    GOOD_MOVE("Guter Zug"),
    BEST_MOVE("Bester Zug"),
    FORCED_MOVE("Erzwungener Zug"),
    DRAWING_RESOURCE("Remisressource"),
    ENDGAME_TECHNIQUE("Endspieltechnik"),
    UNCERTAIN_ANALYSIS("Unsichere Analyse");

    private final String german;

    ExplanationType(String german) {
        this.german = german;
    }

    public String german() {
        return german;
    }
}
