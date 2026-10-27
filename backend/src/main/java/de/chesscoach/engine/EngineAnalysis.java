package de.chesscoach.engine;

import de.chesscoach.domain.analysis.Score;

import java.util.List;

/**
 * Ergebnis einer Engine-Suche für eine Stellung. Die Bewertung ist aus Sicht der Seite am Zug angegeben.
 *
 * @param analysisId         eindeutige Kennung der Suche
 * @param fen                analysierte Stellung
 * @param engineName         Name der Engine laut UCI-Handshake
 * @param engineVersion      konfigurierte Engine-Version
 * @param depth              erreichte Suchtiefe der letzten vollständigen Info-Zeile
 * @param selectiveDepth     selektive Suchtiefe
 * @param nodes              durchsuchte Knoten
 * @param elapsedMillis      gemessene Suchzeit
 * @param score              Bewertung aus Sicht der Seite am Zug
 * @param bestMove           bester Zug laut {@code bestmove}
 * @param principalVariation Hauptvariante in UCI-Notation (ungeprüft; Legalität prüft der Aufrufer)
 * @param candidateMoves     weitere Kandidaten, falls die Engine MultiPV unterstützt (sonst leer)
 * @param completed          true, wenn die Suche regulär mit {@code bestmove} und Bewertung endete
 * @param limits             verwendete Suchgrenzen
 */
public record EngineAnalysis(String analysisId, String fen, String engineName, String engineVersion,
                             int depth, int selectiveDepth, long nodes, long elapsedMillis, Score score,
                             String bestMove, List<String> principalVariation, List<String> candidateMoves,
                             boolean completed, AnalysisLimits limits) {
}
