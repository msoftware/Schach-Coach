package de.chesscoach.coaching;

import de.chesscoach.domain.analysis.Score;
import de.chesscoach.domain.position.Color;
import de.chesscoach.domain.position.MoveInfo;
import de.chesscoach.domain.position.MoveOracle;
import de.chesscoach.domain.position.Piece;
import de.chesscoach.domain.position.Position;

import java.util.List;
import java.util.Optional;

/**
 * Eingabe aller Coaching-Regeln: Stellung vor und nach einem Zug, die geprüfte Variante und – sofern
 * vorhanden – Engine-Bewertungen aus Sicht des Benutzers.
 *
 * <p>Die Klasse ist unveränderlich bis auf intern zwischengespeicherte, deterministisch berechnete Werte
 * (z. B. legale Züge), und wird nie zwischen parallelen Analysen geteilt.</p>
 */
public final class PositionContext {

    private final Color user;
    private final Position before;
    private final MoveInfo move;
    private final EngineLine line;
    private final Score scoreBefore;
    private final Score scoreAfter;
    private final Score bestScore;
    private final EngineLine bestLine;
    private final LinePly previousOpponentMove;
    private final LinePly previousOwnMove;
    private final MoveOracle oracle;
    private final boolean playedMove;

    private List<MoveInfo> legalMovesAfter;
    private List<MoveInfo> legalMovesBefore;

    private PositionContext(Builder b) {
        this.user = b.before.sideToMove();
        this.before = b.before;
        this.move = b.move;
        this.line = b.line != null ? b.line : EngineLine.of("played", b.before, List.of(b.move), false);
        this.scoreBefore = b.scoreBefore;
        this.scoreAfter = b.scoreAfter;
        this.bestScore = b.bestScore;
        this.bestLine = b.bestLine;
        this.previousOpponentMove = b.previousOpponentMove;
        this.previousOwnMove = b.previousOwnMove;
        this.oracle = b.oracle;
        this.playedMove = b.playedMove;
        if (this.line.isEmpty() || !this.line.ply(0).move().uci().equals(move.uci())) {
            throw new IllegalArgumentException("Variante muss mit dem betrachteten Zug beginnen");
        }
    }

    public static Builder builder(Position before, MoveInfo move, MoveOracle oracle) {
        return new Builder(before, move, oracle);
    }

    public Color user() {
        return user;
    }

    public Color opponent() {
        return user.opposite();
    }

    public Position before() {
        return before;
    }

    public Position after() {
        return line.ply(0).after();
    }

    public MoveInfo move() {
        return move;
    }

    public Piece movedPiece() {
        return line.ply(0).moved();
    }

    public Piece capturedPiece() {
        return line.ply(0).captured();
    }

    /** Variante, die mit dem betrachteten Zug beginnt (Index 0) und mit der Engine-Hauptvariante fortgesetzt wird. */
    public EngineLine line() {
        return line;
    }

    /** Antwort des Gegners laut Variante, falls vorhanden. */
    public Optional<LinePly> opponentReply() {
        return line.size() > 1 ? Optional.of(line.ply(1)) : Optional.empty();
    }

    public boolean hasEngineLine() {
        return line.fromEngine() && line.size() > 1;
    }

    public Optional<Score> scoreBefore() {
        return Optional.ofNullable(scoreBefore);
    }

    public Optional<Score> scoreAfter() {
        return Optional.ofNullable(scoreAfter);
    }

    public Optional<Score> bestScore() {
        return Optional.ofNullable(bestScore);
    }

    public Optional<EngineLine> bestLine() {
        return Optional.ofNullable(bestLine);
    }

    public Optional<MoveInfo> bestMove() {
        return bestLine == null || bestLine.isEmpty() ? Optional.empty() : Optional.of(bestLine.ply(0).move());
    }

    /** true, wenn der betrachtete Zug nicht der beste gefundene Zug ist. */
    public boolean differsFromBest() {
        return bestMove().map(b -> !b.uci().equals(move.uci())).orElse(false);
    }

    /** Bewertungsverlust in Centipawns gegenüber dem besten Zug (Mattwerte auf ±3000 begrenzt). */
    public Optional<Integer> lossCentipawns() {
        if (scoreAfter == null || bestScore == null) {
            return Optional.empty();
        }
        return Optional.of(Math.max(0, bestScore.centipawnsClamped(3000) - scoreAfter.centipawnsClamped(3000)));
    }

    public Optional<LinePly> previousOpponentMove() {
        return Optional.ofNullable(previousOpponentMove);
    }

    public Optional<LinePly> previousOwnMove() {
        return Optional.ofNullable(previousOwnMove);
    }

    public MoveOracle oracle() {
        return oracle;
    }

    /** true für den tatsächlich gespielten Zug, false für einen Kandidatenzug. */
    public boolean isPlayedMove() {
        return playedMove;
    }

    /** Legale Züge des Gegners nach dem Zug. */
    public List<MoveInfo> legalMovesAfter() {
        if (legalMovesAfter == null) {
            legalMovesAfter = oracle.legalMoves(after().toFen());
        }
        return legalMovesAfter;
    }

    /** Legale Züge des Benutzers vor dem Zug. */
    public List<MoveInfo> legalMovesBefore() {
        if (legalMovesBefore == null) {
            legalMovesBefore = oracle.legalMoves(before.toFen());
        }
        return legalMovesBefore;
    }

    public static final class Builder {
        private final Position before;
        private final MoveInfo move;
        private final MoveOracle oracle;
        private EngineLine line;
        private Score scoreBefore;
        private Score scoreAfter;
        private Score bestScore;
        private EngineLine bestLine;
        private LinePly previousOpponentMove;
        private LinePly previousOwnMove;
        private boolean playedMove = true;

        private Builder(Position before, MoveInfo move, MoveOracle oracle) {
            this.before = before;
            this.move = move;
            this.oracle = oracle;
        }

        public Builder line(EngineLine line) {
            this.line = line;
            return this;
        }

        public Builder scores(Score before, Score after, Score best) {
            this.scoreBefore = before;
            this.scoreAfter = after;
            this.bestScore = best;
            return this;
        }

        public Builder bestLine(EngineLine bestLine) {
            this.bestLine = bestLine;
            return this;
        }

        public Builder previousOpponentMove(LinePly ply) {
            this.previousOpponentMove = ply;
            return this;
        }

        public Builder previousOwnMove(LinePly ply) {
            this.previousOwnMove = ply;
            return this;
        }

        public Builder playedMove(boolean playedMove) {
            this.playedMove = playedMove;
            return this;
        }

        public PositionContext build() {
            return new PositionContext(this);
        }
    }
}
