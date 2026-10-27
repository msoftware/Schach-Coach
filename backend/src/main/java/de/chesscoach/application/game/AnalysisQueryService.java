package de.chesscoach.application.game;

import de.chesscoach.application.ErrorCode;
import de.chesscoach.application.GameException;
import de.chesscoach.application.analysis.AnalysisEntry;
import de.chesscoach.application.analysis.AnalysisStatus;
import de.chesscoach.domain.game.MoveRecord;
import org.springframework.stereotype.Service;

import java.util.List;

/** Abruf, erneute Anforderung und Vertiefung von Zuganalysen, jeweils gebunden an Zug und Partieversion. */
@Service
public class AnalysisQueryService {

    private final GameService games;
    private final MoveService moves;

    public AnalysisQueryService(GameService games, MoveService moves) {
        this.games = games;
        this.moves = moves;
    }

    /**
     * Analyse eines Halbzuges.
     *
     * @param move           analysierter Zug
     * @param entry          Analyseauftrag (oder null, wenn keiner existiert)
     * @param outdated       true, wenn die Analyse mit älteren Analyse-Einstellungen erstellt wurde
     * @param positionVersion aktuelle Positionsversion der Partie
     */
    public record AnalysisView(MoveRecord move, AnalysisEntry entry, boolean outdated, int positionVersion) {
    }

    public AnalysisView get(String gameId, int ply) {
        return games.withLock(gameId, s -> {
            MoveRecord move = userMove(s, ply);
            AnalysisEntry entry = s.analyses().get(move.moveId());
            if (entry == null) {
                throw new GameException(ErrorCode.ANALYSIS_NOT_FOUND, "Für diesen Zug wurde noch keine Analyse angefordert.");
            }
            return new AnalysisView(move, entry, entry.settingsVersion() != s.settingsVersion(), s.positionVersion());
        });
    }

    /** Fordert eine Analyse an bzw. aktualisiert sie (z. B. nach geänderten Einstellungen oder nach einem Fehler). */
    public AnalysisView request(String gameId, int ply, boolean force) {
        return games.withLock(gameId, s -> {
            MoveRecord move = userMove(s, ply);
            AnalysisEntry existing = s.analyses().get(move.moveId());
            boolean reuse = existing != null && !force && existing.settingsVersion() == s.settingsVersion()
                    && existing.status() != AnalysisStatus.FAILED && existing.status() != AnalysisStatus.CANCELLED;
            AnalysisEntry entry = reuse ? existing : moves.scheduleAnalysis(s, move, 1);
            return new AnalysisView(move, entry, false, s.positionVersion());
        });
    }

    /** Vertieft Kandidaten und Varianten mit größeren Suchgrenzen und mehr Kandidaten. */
    public AnalysisView deepen(String gameId, int ply) {
        return games.withLock(gameId, s -> {
            MoveRecord move = userMove(s, ply);
            AnalysisEntry existing = s.analyses().get(move.moveId());
            if (existing != null && existing.depthLevel() > 1 && !existing.isFinished()) {
                return new AnalysisView(move, existing, false, s.positionVersion());
            }
            AnalysisEntry entry = moves.scheduleAnalysis(s, move, 2);
            return new AnalysisView(move, entry, false, s.positionVersion());
        });
    }

    private static MoveRecord userMove(GameSession s, int ply) {
        List<MoveRecord> list = s.moves();
        if (ply < 1 || ply > list.size()) {
            throw new GameException(ErrorCode.ANALYSIS_NOT_FOUND, "Halbzug " + ply + " existiert in dieser Partie nicht.");
        }
        MoveRecord move = list.get(ply - 1);
        if (!move.byUser()) {
            throw new GameException(ErrorCode.ANALYSIS_NOT_AVAILABLE, "Coaching-Analysen gibt es nur für eigene Züge.");
        }
        return move;
    }
}
