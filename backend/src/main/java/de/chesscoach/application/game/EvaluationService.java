package de.chesscoach.application.game;

import de.chesscoach.application.ErrorCode;
import de.chesscoach.application.GameException;
import de.chesscoach.domain.analysis.Score;
import de.chesscoach.domain.game.GameStatus;
import de.chesscoach.domain.position.Color;
import de.chesscoach.engine.AnalysisLimits;
import de.chesscoach.engine.EngineAnalysis;
import de.chesscoach.engine.EngineException;
import de.chesscoach.engine.EngineRegistry;
import de.chesscoach.engine.EngineTimeoutException;
import de.chesscoach.engine.EngineUnavailableException;
import de.chesscoach.infrastructure.config.AnalysisProperties;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Bewertet die aktuelle Stellung einer Partie für die Bewertungsleiste („wer liegt wie weit vorne“). Die Suche
 * verwendet feste Grenzen unabhängig von der Spielstärke des Computers, damit die Anzeige vergleichbar bleibt.
 * Die Bewertung wird immer aus Sicht von Weiß angegeben. Bereits bewertete Stellungen werden zwischengespeichert.
 */
@Service
public class EvaluationService {

    private static final int CACHE_SIZE = 2_000;

    /**
     * Bewertung der aktuellen Stellung.
     *
     * @param whiteScore Bewertung aus Sicht von Weiß; {@code null}, wenn die Partie beendet ist
     * @param whiteShare Anteil von Weiß an der Leiste (0 = Schwarz gewinnt, 0.5 = ausgeglichen, 1 = Weiß gewinnt)
     * @param display    Anzeige, z. B. "+0.35", "M3", "-M2" oder bei beendeter Partie das Ergebnis "1-0"
     * @param depth      erreichte Suchtiefe (0, wenn ohne Engine bewertet)
     */
    public record Evaluation(String gameId, int positionVersion, String fen, Score whiteScore, double whiteShare,
                            String display, int depth, boolean finished) {
    }

    private record Cached(Score whiteScore, int depth) {
    }

    private final GameService games;
    private final EngineRegistry engines;
    private final AnalysisProperties properties;
    private final Map<String, Cached> cache = Collections.synchronizedMap(new LinkedHashMap<>(256, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Cached> eldest) {
            return size() > CACHE_SIZE;
        }
    });

    public EvaluationService(GameService games, EngineRegistry engines, AnalysisProperties properties) {
        this.games = games;
        this.engines = engines;
        this.properties = properties;
    }

    public Evaluation evaluate(String gameId) {
        GameSnapshot s = games.get(gameId);
        GameStatus status = s.status();
        if (status.isFinished()) {
            double share = s.winner() == null ? 0.5 : s.winner() == Color.WHITE ? 1.0 : 0.0;
            String result = s.winner() == null ? "½-½" : s.winner() == Color.WHITE ? "1-0" : "0-1";
            return new Evaluation(s.gameId(), s.positionVersion(), s.fen(), null, share, result, 0, true);
        }
        Cached cached = cache.get(s.fen());
        if (cached == null) {
            cached = search(s.fen(), s.sideToMove());
            cache.put(s.fen(), cached);
        }
        return new Evaluation(s.gameId(), s.positionVersion(), s.fen(), cached.whiteScore(),
                cached.whiteScore().winProbability(), cached.whiteScore().display(), cached.depth(), false);
    }

    private Cached search(String fen, Color sideToMove) {
        try {
            EngineAnalysis analysis = engines.play().analyze(fen, new AnalysisLimits(null, properties.evaluationNodes(), null));
            if (analysis.score() == null) {
                throw new GameException(ErrorCode.ENGINE_ERROR, "Die Engine hat keine Bewertung geliefert.");
            }
            Score white = sideToMove == Color.WHITE ? analysis.score() : analysis.score().negate();
            return new Cached(white, analysis.depth());
        } catch (EngineUnavailableException e) {
            throw new GameException(ErrorCode.ENGINE_UNAVAILABLE, "Die Schach-Engine ist nicht verfügbar.");
        } catch (EngineTimeoutException e) {
            throw new GameException(ErrorCode.ENGINE_TIMEOUT, "Die Engine hat nicht rechtzeitig geantwortet.");
        } catch (EngineException e) {
            throw new GameException(ErrorCode.ENGINE_ERROR, "Die Engine hat einen Fehler gemeldet.");
        }
    }
}
