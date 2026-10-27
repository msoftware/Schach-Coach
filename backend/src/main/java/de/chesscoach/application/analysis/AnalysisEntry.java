package de.chesscoach.application.analysis;

import de.chesscoach.domain.analysis.MoveAnalysis;
import de.chesscoach.domain.game.AnalysisProfile;
import de.chesscoach.domain.game.MoveRecord;

import java.time.Instant;

/**
 * Analyseauftrag zu genau einem Benutzerzug. Status und Ergebnis werden vom Analyse-Thread gesetzt und von
 * API-Threads gelesen; alle veränderlichen Felder sind volatile, das Ergebnis selbst ist unveränderlich.
 */
public final class AnalysisEntry {

    private final String gameId;
    private final MoveRecord move;
    private final MoveRecord previousOpponentMove;
    private final MoveRecord previousOwnMove;
    private final AnalysisProfile profile;
    private final int settingsVersion;
    private final int depthLevel;
    private final Instant requestedAt = Instant.now();
    private final CancellationToken token = new CancellationToken();

    private volatile AnalysisStatus status = AnalysisStatus.PENDING;
    private volatile MoveAnalysis result;
    private volatile String errorMessage;
    private volatile String stage = "Wartet auf freie Analyse-Engine";
    private volatile int progressPercent;
    private volatile Instant completedAt;

    public AnalysisEntry(String gameId, MoveRecord move, MoveRecord previousOpponentMove, MoveRecord previousOwnMove,
                         AnalysisProfile profile, int settingsVersion, int depthLevel) {
        this.gameId = gameId;
        this.move = move;
        this.previousOpponentMove = previousOpponentMove;
        this.previousOwnMove = previousOwnMove;
        this.profile = profile;
        this.settingsVersion = settingsVersion;
        this.depthLevel = depthLevel;
    }

    public String gameId() {
        return gameId;
    }

    public MoveRecord move() {
        return move;
    }

    public MoveRecord previousOpponentMove() {
        return previousOpponentMove;
    }

    public MoveRecord previousOwnMove() {
        return previousOwnMove;
    }

    public AnalysisProfile profile() {
        return profile;
    }

    public int settingsVersion() {
        return settingsVersion;
    }

    public int depthLevel() {
        return depthLevel;
    }

    public Instant requestedAt() {
        return requestedAt;
    }

    public CancellationToken token() {
        return token;
    }

    public AnalysisStatus status() {
        return status;
    }

    public MoveAnalysis result() {
        return result;
    }

    public String errorMessage() {
        return errorMessage;
    }

    public String stage() {
        return stage;
    }

    public int progressPercent() {
        return progressPercent;
    }

    public Instant completedAt() {
        return completedAt;
    }

    void progress(String stage, int percent) {
        this.stage = stage;
        this.progressPercent = Math.max(0, Math.min(100, percent));
    }

    void running() {
        if (status == AnalysisStatus.PENDING) {
            status = AnalysisStatus.RUNNING;
        }
    }

    synchronized void complete(MoveAnalysis analysis) {
        if (status == AnalysisStatus.CANCELLED) {
            return;
        }
        this.result = analysis;
        this.status = AnalysisStatus.COMPLETED;
        this.progress("Abgeschlossen", 100);
        this.completedAt = Instant.now();
    }

    synchronized void fail(String message) {
        if (status == AnalysisStatus.CANCELLED) {
            return;
        }
        this.errorMessage = message;
        this.status = AnalysisStatus.FAILED;
        this.completedAt = Instant.now();
    }

    synchronized void cancel() {
        token.cancel();
        if (status == AnalysisStatus.PENDING || status == AnalysisStatus.RUNNING) {
            status = AnalysisStatus.CANCELLED;
            stage = "Verworfen";
            completedAt = Instant.now();
        }
    }

    public boolean isFinished() {
        return status == AnalysisStatus.COMPLETED || status == AnalysisStatus.FAILED || status == AnalysisStatus.CANCELLED;
    }
}
