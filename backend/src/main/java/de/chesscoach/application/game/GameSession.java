package de.chesscoach.application.game;

import de.chesscoach.application.analysis.AnalysisEntry;
import de.chesscoach.application.analysis.AnalysisStatus;
import de.chesscoach.chess.ChessPositionAdapter;
import de.chesscoach.domain.game.AnalysisProfile;
import de.chesscoach.domain.game.Difficulty;
import de.chesscoach.domain.game.GameStatus;
import de.chesscoach.domain.game.MoveRecord;
import de.chesscoach.domain.position.Color;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Laufende Partie im Arbeitsspeicher. Alle Zugriffe erfolgen unter {@link #lock()}; nach außen werden nur
 * unveränderliche {@link GameSnapshot}s und {@link MoveRecord}s gegeben.
 */
public final class GameSession {

    private static final AtomicLong ACCESS_COUNTER = new AtomicLong();

    private final String gameId;
    private final Instant createdAt = Instant.now();
    private final ReentrantLock lock = new ReentrantLock();

    private String initialFen;
    private Color playerColor;
    private Difficulty difficulty;
    private AnalysisProfile analysisProfile;
    private ChessPositionAdapter position;
    private final List<MoveRecord> moves = new ArrayList<>();
    private final Deque<String> redoStack = new ArrayDeque<>();
    private final Map<String, AnalysisEntry> analyses = new LinkedHashMap<>();
    private GameStatus status = GameStatus.IN_PROGRESS;
    private Color winner;
    private int positionVersion;
    private int settingsVersion;
    private Instant updatedAt = createdAt;
    private volatile Instant lastAccess = createdAt;
    private volatile long accessSequence = ACCESS_COUNTER.incrementAndGet();
    private CompletableFuture<MoveRecord> pendingEngineMove;
    private int pendingEngineVersion = -1;

    GameSession(String gameId, ChessPositionAdapter position, Color playerColor, Difficulty difficulty, AnalysisProfile profile) {
        this.gameId = gameId;
        this.initialFen = position.initialFen();
        this.position = position;
        this.playerColor = playerColor;
        this.difficulty = difficulty;
        this.analysisProfile = profile;
    }

    public ReentrantLock lock() {
        return lock;
    }

    public String gameId() {
        return gameId;
    }

    public Instant lastAccess() {
        return lastAccess;
    }

    /** Fortlaufende Zugriffsnummer: eindeutige Reihenfolge für die Verdrängung, auch bei gleichen Zeitstempeln. */
    long accessSequence() {
        return accessSequence;
    }

    void touch() {
        lastAccess = Instant.now();
        accessSequence = ACCESS_COUNTER.incrementAndGet();
    }

    String initialFen() {
        return initialFen;
    }

    Color playerColor() {
        return playerColor;
    }

    Difficulty difficulty() {
        return difficulty;
    }

    AnalysisProfile analysisProfile() {
        return analysisProfile;
    }

    ChessPositionAdapter position() {
        return position;
    }

    List<MoveRecord> moves() {
        return moves;
    }

    Deque<String> redoStack() {
        return redoStack;
    }

    Map<String, AnalysisEntry> analyses() {
        return analyses;
    }

    GameStatus status() {
        return status;
    }

    int positionVersion() {
        return positionVersion;
    }

    int settingsVersion() {
        return settingsVersion;
    }

    CompletableFuture<MoveRecord> pendingEngineMove() {
        return pendingEngineMove;
    }

    int pendingEngineVersion() {
        return pendingEngineVersion;
    }

    void setPendingEngineMove(CompletableFuture<MoveRecord> future, int version) {
        this.pendingEngineMove = future;
        this.pendingEngineVersion = version;
    }

    void clearPendingEngineMove() {
        this.pendingEngineMove = null;
        this.pendingEngineVersion = -1;
    }

    void reset(ChessPositionAdapter start, Color playerColor, Difficulty difficulty) {
        this.initialFen = start.initialFen();
        this.position = start;
        this.playerColor = playerColor;
        this.difficulty = difficulty;
        this.moves.clear();
        this.redoStack.clear();
        this.analyses.clear();
        this.status = start.status();
        this.winner = null;
        bumpVersion();
    }

    void apply(ChessPositionAdapter next, MoveRecord record) {
        this.position = next;
        this.moves.add(record);
        this.status = next.status();
        this.winner = status == GameStatus.CHECKMATE ? record.player() : null;
        bumpVersion();
    }

    void replacePosition(ChessPositionAdapter next) {
        this.position = next;
        this.status = next.status();
        this.winner = status == GameStatus.CHECKMATE && !moves.isEmpty() ? moves.get(moves.size() - 1).player() : null;
        bumpVersion();
    }

    void resign() {
        this.status = GameStatus.RESIGNED;
        this.winner = playerColor.opposite();
        bumpVersion();
    }

    void setDifficulty(Difficulty difficulty) {
        this.difficulty = difficulty;
    }

    void setAnalysisProfile(AnalysisProfile profile) {
        if (profile != this.analysisProfile) {
            this.analysisProfile = profile;
            this.settingsVersion++;
        }
    }

    int nextVersion() {
        return positionVersion + 1;
    }

    private void bumpVersion() {
        positionVersion++;
        updatedAt = Instant.now();
    }

    GameSnapshot snapshot(boolean includeLegalMoves) {
        Map<String, AnalysisStatus> statuses = new LinkedHashMap<>();
        List<String> outdated = new ArrayList<>();
        analyses.forEach((moveId, entry) -> {
            statuses.put(moveId, entry.status());
            if (entry.settingsVersion() != settingsVersion) {
                outdated.add(moveId);
            }
        });
        boolean userTurn = position.sideToMove() == playerColor && !status.isFinished();
        boolean canUndo = moves.stream().anyMatch(MoveRecord::byUser) && status != GameStatus.RESIGNED;
        return new GameSnapshot(gameId, initialFen, position.fen(), playerColor, position.sideToMove(), status, winner,
                positionVersion, difficulty, analysisProfile, settingsVersion, List.copyOf(moves),
                includeLegalMoves && userTurn ? position.legalMoves() : List.of(), position.inCheck(), canUndo,
                !redoStack.isEmpty() && status != GameStatus.RESIGNED, pendingEngineMove != null, statuses, outdated,
                createdAt, updatedAt);
    }
}
