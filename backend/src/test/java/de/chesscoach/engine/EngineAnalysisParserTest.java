package de.chesscoach.engine;

import de.chesscoach.domain.analysis.Score;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EngineAnalysisParserTest {

    @Test
    void parsesCalvinInfoLine() {
        var info = EngineAnalysisParser.parseInfo(
                "info depth 13 seldepth 20 score cp -30 nodes 49152 time 965 nps 50000 hashfull 4 pv e7e5 g1f3 b8c6").orElseThrow();
        assertThat(info.depth()).isEqualTo(13);
        assertThat(info.selectiveDepth()).isEqualTo(20);
        assertThat(info.score()).isEqualTo(Score.cp(-30));
        assertThat(info.nodes()).isEqualTo(49152);
        assertThat(info.timeMillis()).isEqualTo(965);
        assertThat(info.pv()).containsExactly("e7e5", "g1f3", "b8c6");
        assertThat(info.isComplete()).isTrue();
    }

    @Test
    void parsesMateAndBounds() {
        var mate = EngineAnalysisParser.parseInfo("info depth 1 seldepth 2 score mate 1 nodes 20 time 0 pv a1a8").orElseThrow();
        assertThat(mate.score()).isEqualTo(Score.mate(1));
        var bound = EngineAnalysisParser.parseInfo("info depth 9 score cp 40 lowerbound nodes 1 pv e2e4").orElseThrow();
        assertThat(bound.bound()).isEqualTo(EngineAnalysisParser.Bound.LOWER);
        assertThat(bound.isComplete()).as("Schranken sind keine exakten Bewertungen").isFalse();
        var multi = EngineAnalysisParser.parseInfo("info depth 9 multipv 2 score cp 12 pv d2d4 d7d5").orElseThrow();
        assertThat(multi.multipv()).isEqualTo(2);
    }

    @Test
    void ignoresIncompleteOrBrokenLines() {
        assertThat(EngineAnalysisParser.parseInfo("info string Hash 32").orElseThrow().text()).isEqualTo("string Hash 32");
        assertThat(EngineAnalysisParser.parseInfo("info depth x score cp")).isEmpty();
        assertThat(EngineAnalysisParser.parseInfo("info currmove e2e4 currmovenumber 1").orElseThrow().isComplete()).isFalse();
        assertThat(EngineAnalysisParser.parseInfo("bestmove e2e4")).isEmpty();
        var pv = EngineAnalysisParser.parseInfo("info depth 3 score cp 1 pv e2e4 garbage e7e5").orElseThrow();
        assertThat(pv.pv()).containsExactly("e2e4");
    }

    @Test
    void parsesBestMove() {
        var best = EngineAnalysisParser.parseBestMove("bestmove e7e8q ponder a2a3").orElseThrow();
        assertThat(best.move()).contains("e7e8q");
        assertThat(best.ponder()).contains("a2a3");
        assertThat(EngineAnalysisParser.parseBestMove("bestmove (none)").orElseThrow().move()).isEmpty();
        assertThat(EngineAnalysisParser.parseBestMove("bestmove 0000").orElseThrow().move()).isEmpty();
        assertThat(EngineAnalysisParser.parseBestMove("bestmove xyz")).isEmpty();
        assertThat(EngineAnalysisParser.parseBestMove("bestmove")).isEmpty();
    }

    @Test
    void limits() {
        assertThat(AnalysisLimits.nodes(1000).scaled(2.0).nodes()).isEqualTo(2000);
        assertThat(AnalysisLimits.depth(5).describe()).isEqualTo("Tiefe 5");
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> new AnalysisLimits(null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> AnalysisLimits.movetime(500_000))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
