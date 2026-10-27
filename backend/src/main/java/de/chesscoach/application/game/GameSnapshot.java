package de.chesscoach.application.game;

import de.chesscoach.application.analysis.AnalysisStatus;
import de.chesscoach.chess.LegalMove;
import de.chesscoach.domain.game.AnalysisProfile;
import de.chesscoach.domain.game.Difficulty;
import de.chesscoach.domain.game.GameStatus;
import de.chesscoach.domain.game.MoveRecord;
import de.chesscoach.domain.position.Color;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Unveränderliche Momentaufnahme einer Partie (GameState, Requirements 10.1), unter der Sperre der Sitzung erzeugt.
 *
 * @param analysisStatus Analysestatus je Zug-ID der Benutzerzüge
 * @param analysisOutdated Zug-IDs, deren Analyse mit veralteten Einstellungen erstellt wurde
 */
public record GameSnapshot(String gameId, String initialFen, String fen, Color playerColor, Color sideToMove,
                           GameStatus status, Color winner, int positionVersion, Difficulty difficulty,
                           AnalysisProfile analysisProfile, int settingsVersion, List<MoveRecord> moves,
                           List<LegalMove> legalMoves, boolean inCheck, boolean canUndo, boolean canRedo,
                           boolean engineThinking, Map<String, AnalysisStatus> analysisStatus,
                           List<String> analysisOutdated, Instant createdAt, Instant updatedAt) {

    public boolean userToMove() {
        return sideToMove == playerColor && !status.isFinished();
    }
}
