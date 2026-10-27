package de.chesscoach.application.game;

import de.chesscoach.application.ErrorCode;
import de.chesscoach.application.GameException;
import de.chesscoach.application.analysis.AnalysisEntry;
import de.chesscoach.application.analysis.AnalysisService;
import de.chesscoach.chess.ChessPositionAdapter;
import de.chesscoach.chess.ChessRulesService;
import de.chesscoach.chess.InvalidFenException;
import de.chesscoach.chess.LegalMove;
import de.chesscoach.domain.game.AnalysisProfile;
import de.chesscoach.domain.game.Difficulty;
import de.chesscoach.domain.game.GameStatus;
import de.chesscoach.domain.game.MoveRecord;
import de.chesscoach.domain.position.Color;
import de.chesscoach.engine.EngineRegistry;
import de.chesscoach.infrastructure.config.ApiProperties;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;

/**
 * Verwaltet den Zustand laufender Partien: Start, Wiederherstellung, Zugreihenfolge, Partieende, Undo/Redo,
 * Aufgabe und Sitzungseinstellungen. Jede Zustandsänderung erhöht die Positionsversion.
 */
@Service
public class GameService {

    private final GameStore store;
    private final ChessRulesService rules;
    private final EngineRegistry engines;
    private final AnalysisService analysisService;
    private final ApiProperties apiProperties;

    public GameService(GameStore store, ChessRulesService rules, EngineRegistry engines, AnalysisService analysisService,
                       ApiProperties apiProperties) {
        this.store = store;
        this.rules = rules;
        this.engines = engines;
        this.analysisService = analysisService;
        this.apiProperties = apiProperties;
    }

    /** Parameter für den Start bzw. die Wiederherstellung einer Partie. */
    public record NewGame(Color playerColor, Difficulty difficulty, AnalysisProfile analysisProfile, String initialFen,
                          List<String> moves) {
    }

    public GameSnapshot create(NewGame request) {
        if (!engines.ensurePlayEngineAvailable()) {
            throw new GameException(ErrorCode.ENGINE_UNAVAILABLE,
                    "Die Schach-Engine konnte nicht gestartet werden. Eine Partie gegen den Computer ist derzeit nicht möglich.");
        }
        Color player = Objects.requireNonNullElse(request.playerColor(), Color.WHITE);
        Difficulty difficulty = Objects.requireNonNullElse(request.difficulty(), Difficulty.MEDIUM);
        AnalysisProfile profile = Objects.requireNonNullElse(request.analysisProfile(), AnalysisProfile.NORMAL);
        ChessPositionAdapter start;
        try {
            start = rules.newGame(request.initialFen());
        } catch (InvalidFenException e) {
            throw new GameException(ErrorCode.INVALID_FEN, e.getMessage());
        }
        GameSession session = new GameSession(store.newGameId(), start, player, difficulty, profile);
        List<String> moves = request.moves() == null ? List.of() : request.moves();
        if (moves.size() > apiProperties.maxRestoreMoves()) {
            throw new GameException(ErrorCode.INVALID_RESTORE, "Die Partie enthält zu viele Züge für eine Wiederherstellung.");
        }
        ChessPositionAdapter current = start;
        List<MoveRecord> records = new ArrayList<>();
        for (int i = 0; i < moves.size(); i++) {
            String uci = moves.get(i);
            if (current.status().isFinished()) {
                throw new GameException(ErrorCode.INVALID_RESTORE, "Nach Zug " + i + " ist die Partie bereits beendet.");
            }
            if (!rules.isWellFormedUci(uci) || !current.isLegal(uci)) {
                throw new GameException(ErrorCode.INVALID_RESTORE,
                        "Zug " + (i + 1) + " (" + uci + ") ist in der rekonstruierten Stellung nicht legal.");
            }
            LegalMove described = current.describe(uci);
            ChessPositionAdapter next = current.play(uci);
            records.add(new MoveRecord(UUID.randomUUID().toString(), i + 1, described.uci(), described.san(), current.fen(),
                    next.fen(), current.sideToMove(), current.sideToMove() == player, 1, Instant.now()));
            current = next;
        }
        session.lock().lock();
        try {
            for (MoveRecord record : records) {
                session.moves().add(record);
            }
            session.replacePosition(current);
            store.put(session);
            return session.snapshot(true);
        } finally {
            session.lock().unlock();
        }
    }

    public GameSnapshot get(String gameId) {
        return withLock(gameId, s -> s.snapshot(true));
    }

    GameSession session(String gameId) {
        return store.get(gameId);
    }

    /** Führt eine Aktion unter der Sperre der Partie aus. */
    <T> T withLock(String gameId, Function<GameSession, T> action) {
        GameSession session = store.get(gameId);
        session.lock().lock();
        try {
            return action.apply(session);
        } finally {
            session.lock().unlock();
        }
    }

    static void requireVersion(GameSession session, Integer expected) {
        if (expected != null && expected != session.positionVersion()) {
            throw new GameException(ErrorCode.POSITION_VERSION_CONFLICT,
                    "Die Partie wurde inzwischen verändert (aktuelle Version " + session.positionVersion() + ").",
                    session.snapshot(true));
        }
    }

    public GameSnapshot restart(String gameId, Integer expectedVersion, Color playerColor, Difficulty difficulty) {
        return withLock(gameId, s -> {
            requireVersion(s, expectedVersion);
            cancelAnalyses(s, s.analyses().values());
            ChessPositionAdapter start = rules.newGame(s.initialFen());
            s.reset(start, Objects.requireNonNullElse(playerColor, s.playerColor()),
                    Objects.requireNonNullElse(difficulty, s.difficulty()));
            return s.snapshot(true);
        });
    }

    /**
     * Nimmt den letzten eigenen Zug (und eine bereits erfolgte Computerantwort) zurück. Analysen der
     * zurückgenommenen Züge werden abgebrochen bzw. verworfen.
     */
    public GameSnapshot undo(String gameId, Integer expectedVersion) {
        return withLock(gameId, s -> {
            requireVersion(s, expectedVersion);
            if (s.pendingEngineMove() != null) {
                throw new GameException(ErrorCode.ENGINE_BUSY, "Der Computer berechnet gerade seinen Zug.", s.snapshot(true));
            }
            if (s.status() == GameStatus.RESIGNED) {
                throw new GameException(ErrorCode.GAME_FINISHED, "Eine aufgegebene Partie kann nicht zurückgenommen werden.");
            }
            List<MoveRecord> moves = s.moves();
            int lastUser = -1;
            for (int i = moves.size() - 1; i >= 0; i--) {
                if (moves.get(i).byUser()) {
                    lastUser = i;
                    break;
                }
            }
            if (lastUser < 0) {
                throw new GameException(ErrorCode.NOTHING_TO_UNDO, "Es gibt keinen eigenen Zug, der zurückgenommen werden kann.");
            }
            List<MoveRecord> removed = new ArrayList<>(moves.subList(lastUser, moves.size()));
            moves.subList(lastUser, moves.size()).clear();
            for (int i = removed.size() - 1; i >= 0; i--) {
                s.redoStack().push(removed.get(i).uci());
            }
            List<AnalysisEntry> stale = removed.stream().map(m -> s.analyses().remove(m.moveId())).filter(Objects::nonNull).toList();
            cancelAnalyses(s, stale);
            s.replacePosition(rules.replay(s.initialFen(), moves.stream().map(MoveRecord::uci).toList()));
            return s.snapshot(true);
        });
    }

    /** Stellt die zuletzt zurückgenommenen Züge wieder her (eigener Zug und ggf. Computerantwort). */
    public GameSnapshot redo(String gameId, Integer expectedVersion, java.util.function.BiConsumer<GameSession, MoveRecord> onUserMove) {
        return withLock(gameId, s -> {
            requireVersion(s, expectedVersion);
            if (s.redoStack().isEmpty()) {
                throw new GameException(ErrorCode.NOTHING_TO_REDO, "Es gibt keinen zurückgenommenen Zug.");
            }
            boolean userMoved = false;
            while (!s.redoStack().isEmpty()) {
                ChessPositionAdapter current = s.position();
                boolean userTurn = current.sideToMove() == s.playerColor();
                if (userMoved && userTurn) {
                    break;
                }
                String uci = s.redoStack().pop();
                MoveRecord record = MoveService.applyMove(s, current, uci, userTurn, rules);
                if (userTurn) {
                    userMoved = true;
                    onUserMove.accept(s, record);
                }
                if (s.status().isFinished()) {
                    s.redoStack().clear();
                }
            }
            return s.snapshot(true);
        });
    }

    public GameSnapshot resign(String gameId, Integer expectedVersion) {
        return withLock(gameId, s -> {
            requireVersion(s, expectedVersion);
            if (s.status().isFinished()) {
                throw new GameException(ErrorCode.GAME_FINISHED, "Die Partie ist bereits beendet.", s.snapshot(true));
            }
            s.resign();
            return s.snapshot(true);
        });
    }

    public GameSnapshot updateSettings(String gameId, Difficulty difficulty, AnalysisProfile profile) {
        return withLock(gameId, s -> {
            if (difficulty != null) {
                s.setDifficulty(difficulty);
            }
            if (profile != null) {
                s.setAnalysisProfile(profile);
            }
            return s.snapshot(true);
        });
    }

    /** Bricht Analysen ab; das Entfernen aus der Sitzung übernimmt der Aufrufer (bzw. {@code reset}). */
    void cancelAnalyses(GameSession session, java.util.Collection<AnalysisEntry> entries) {
        for (AnalysisEntry entry : List.copyOf(entries)) {
            analysisService.cancel(entry);
        }
    }
}
