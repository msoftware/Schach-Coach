package de.chesscoach.application.game;

import de.chesscoach.chess.ChessPositionAdapter;
import de.chesscoach.domain.analysis.Score;
import de.chesscoach.domain.game.Difficulty;
import de.chesscoach.domain.game.GameStatus;
import de.chesscoach.engine.AnalysisLimits;
import de.chesscoach.engine.ChessEngine;
import de.chesscoach.engine.EngineAnalysis;
import de.chesscoach.engine.EngineLimits;
import de.chesscoach.engine.EngineMove;
import de.chesscoach.infrastructure.config.DifficultyProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Berechnet Computerzüge über die Engine. Die Spielstärke wird ausschließlich über überprüfbare Engine-Parameter
 * gesteuert: Suchgrenzen (Knoten, Tiefe, Zeit) und – für die schwächsten Stufen – eine zufällige Teilmenge
 * der legalen Züge, aus der die Engine den besten auswählt. Jeder Zug wird von der Engine bewertet; es werden
 * keine Züge ohne Engine-Berechnung gespielt.
 */
@Component
public class ComputerPlayer {

    private final DifficultyProperties properties;
    private final Random random;

    public ComputerPlayer(DifficultyProperties properties) {
        this.properties = properties;
        this.random = properties.randomSeed() == 0 ? new Random() : new Random(properties.randomSeed());
    }

    /** Ergebnis: Zug und Beschreibung der verwendeten Parameter. */
    public record Choice(String uci, Score score, int depth, long nodes, String parameters) {
    }

    public Choice choose(ChessEngine engine, ChessPositionAdapter position, Difficulty difficulty) {
        DifficultyProperties.Level level = properties.level(difficulty);
        if (level.sampleFraction() == null) {
            EngineMove move = engine.findBestMove(position.fen(),
                    new EngineLimits(level.depth(), level.nodes(), level.movetimeMillis()));
            EngineAnalysis a = move.analysis();
            return new Choice(move.uciMove(), a.score(), a.depth(), a.nodes(), level.describe());
        }
        List<String> legal = new ArrayList<>(position.legalUciMoves());
        synchronized (random) {
            Collections.shuffle(legal, random);
        }
        int size = Math.max(level.minSample() == null ? 1 : level.minSample(), (int) Math.ceil(legal.size() * level.sampleFraction()));
        List<String> sample = legal.subList(0, Math.min(size, legal.size()));
        AnalysisLimits limits = new AnalysisLimits(level.depth(), level.nodes(), level.movetimeMillis());
        String bestMove = null;
        Score bestScore = null;
        int depth = 0;
        long nodes = 0;
        for (String uci : sample) {
            ChessPositionAdapter child = position.play(uci);
            Score score;
            GameStatus status = child.status();
            if (status == GameStatus.CHECKMATE) {
                score = Score.mate(1);
            } else if (status.isDraw()) {
                score = Score.cp(0);
            } else {
                EngineAnalysis analysis = engine.analyze(child.fen(), limits);
                if (analysis.score() == null) {
                    continue;
                }
                score = Score.fromChildPerspective(analysis.score());
                depth = Math.max(depth, analysis.depth());
                nodes += analysis.nodes();
            }
            if (bestScore == null || score.comparable() > bestScore.comparable()) {
                bestScore = score;
                bestMove = uci;
            }
        }
        if (bestMove == null) {
            throw new de.chesscoach.engine.EngineProtocolException("Die Engine konnte keinen Zug bewerten.");
        }
        return new Choice(bestMove, bestScore, depth, nodes, level.describe() + " (" + sample.size() + " Züge geprüft)");
    }
}
