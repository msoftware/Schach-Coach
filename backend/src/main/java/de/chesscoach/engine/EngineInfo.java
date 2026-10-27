package de.chesscoach.engine;

/**
 * Status- und Versionsinformation einer Engine-Instanz.
 *
 * @param role      Aufgabe der Instanz ("play" für Computerzüge, "analysis" für das Coaching)
 * @param name      Name laut {@code id name}
 * @param author    Autor laut {@code id author}
 * @param version   konfigurierte Version der ausgelieferten Engine-Datei
 * @param state     aktueller Zustand
 * @param message   verständliche Statusmeldung (deutsch)
 * @param supportsMultiPv true, wenn die Engine die UCI-Option MultiPV anbietet
 */
public record EngineInfo(String role, String name, String author, String version, EngineState state,
                         String message, boolean supportsMultiPv) {

    public boolean available() {
        return state == EngineState.READY || state == EngineState.SEARCHING;
    }
}
