package de.chesscoach.coaching;

import de.chesscoach.chess.ChessRulesService;
import de.chesscoach.domain.analysis.Score;
import de.chesscoach.domain.coaching.CoachingFinding;
import de.chesscoach.domain.position.MoveInfo;
import de.chesscoach.domain.position.Position;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Testhilfe: baut Coaching-Kontexte aus FEN und geprüften Zugfolgen (ohne Engine). */
public final class Contexts {

    public static final ChessRulesService RULES = new ChessRulesService();

    private Contexts() {
    }

    /** Geprüfte Zugfolge ab der Stellung. Wirft, wenn ein Zug illegal ist, damit Testdaten selbst korrekt bleiben. */
    public static EngineLine line(String fen, String... uci) {
        ChessRulesService.ValidatedLine validated = RULES.validateLine(fen, Arrays.asList(uci), 40);
        if (validated.truncated() || validated.moves().size() != uci.length) {
            throw new IllegalArgumentException("Testvariante enthält illegale Züge: " + Arrays.toString(uci));
        }
        List<MoveInfo> moves = validated.moves().stream().map(ChessRulesService::toMoveInfo).toList();
        return EngineLine.of("v-" + uci[0], Position.fromFen(fen), moves, uci.length > 1);
    }

    public static Builder of(String fen, String... line) {
        return new Builder(fen, line);
    }

    public static final class Builder {
        private final String fen;
        private final EngineLine line;
        private Score before;
        private Score after;
        private Score best;
        private EngineLine bestLine;
        private LinePly previousOpponent;
        private LinePly previousOwn;
        private boolean played = true;

        private Builder(String fen, String[] line) {
            this.fen = fen;
            this.line = Contexts.line(fen, line);
        }

        public Builder scores(Score before, Score after, Score best) {
            this.before = before;
            this.after = after;
            this.best = best;
            return this;
        }

        public Builder cp(int before, int after, int best) {
            return scores(Score.cp(before), Score.cp(after), Score.cp(best));
        }

        public Builder best(String... uci) {
            this.bestLine = Contexts.line(fen, uci);
            return this;
        }

        public Builder previousOpponent(String fenBefore, String uci) {
            this.previousOpponent = Contexts.line(fenBefore, uci).ply(0);
            return this;
        }

        public Builder previousOwn(String fenBefore, String uci) {
            this.previousOwn = Contexts.line(fenBefore, uci).ply(0);
            return this;
        }

        public Builder candidate() {
            this.played = false;
            return this;
        }

        public PositionContext build() {
            return PositionContext.builder(Position.fromFen(fen), line.ply(0).move(), RULES)
                    .line(line)
                    .scores(before, after, best)
                    .bestLine(bestLine)
                    .previousOpponentMove(previousOpponent)
                    .previousOwnMove(previousOwn)
                    .playedMove(played)
                    .build();
        }
    }

    public static List<String> ids(List<CoachingFinding> findings) {
        return findings.stream().map(CoachingFinding::ruleId).toList();
    }

    public static List<String> texts(List<CoachingFinding> findings) {
        List<String> result = new ArrayList<>();
        findings.forEach(f -> result.add(f.title() + ": " + f.explanation()));
        return result;
    }
}
