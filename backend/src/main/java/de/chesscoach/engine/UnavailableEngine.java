package de.chesscoach.engine;

/**
 * Platzhalter für eine nicht startbare Engine (z. B. fehlende Engine-Datei). Simuliert keine Züge oder Analysen,
 * sondern meldet bei jedem Aufruf einen verständlichen Fehler (Requirements 3.2).
 */
public class UnavailableEngine implements ChessEngine {

    private final String role;
    private final String version;
    private final String reason;

    public UnavailableEngine(String role, String version, String reason) {
        this.role = role;
        this.version = version;
        this.reason = reason;
    }

    @Override
    public void start() {
        throw new EngineUnavailableException(reason);
    }

    @Override
    public EngineInfo getEngineInfo() {
        return new EngineInfo(role, "nicht verfügbar", "-", version, EngineState.FAILED, reason, false);
    }

    @Override
    public EngineAnalysis analyze(String fen, AnalysisLimits limits) {
        throw new EngineUnavailableException(reason);
    }

    @Override
    public EngineMove findBestMove(String fen, EngineLimits limits) {
        throw new EngineUnavailableException(reason);
    }

    @Override
    public void stopAnalysis() {
        // Keine laufende Suche.
    }

    @Override
    public void shutdown() {
        // Kein Prozess vorhanden.
    }
}
