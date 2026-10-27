package de.chesscoach.engine;

/**
 * Engine-unabhängige Schnittstelle zur Schach-Engine (Architekturvorgabe aus den Requirements, Abschnitt 3.2).
 * Implementierungen sind threadsicher; Suchen einer Instanz werden serialisiert.
 */
public interface ChessEngine {

    /** Startet bzw. initialisiert die Engine. Wirft {@link EngineUnavailableException}, falls dies scheitert. */
    void start();

    EngineInfo getEngineInfo();

    /** Analysiert eine Stellung. Die Bewertung ist aus Sicht der Seite am Zug. */
    EngineAnalysis analyze(String fen, AnalysisLimits limits);

    /** Berechnet den besten Zug für die Seite am Zug. */
    EngineMove findBestMove(String fen, EngineLimits limits);

    /** Bricht eine laufende Suche ab; das Teilergebnis wird als nicht vollständig gekennzeichnet. */
    void stopAnalysis();

    void shutdown();
}
