package de.chesscoach.engine;

/** Erzeugt Calvin-Instanzen für Integrationstests anderer Pakete. */
public final class CalvinEngineITAccess {

    private CalvinEngineITAccess() {
    }

    public static CalvinChessEngine create(String role) {
        return CalvinEngineIT.create(role);
    }
}
