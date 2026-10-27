package de.chesscoach.chess;

import de.chesscoach.domain.game.GameStatus;
import de.chesscoach.domain.position.PieceKind;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChessRulesServiceTest {

    private final ChessRulesService rules = new ChessRulesService();

    @Test
    void startPositionHasTwentyLegalMoves() {
        ChessPositionAdapter game = rules.newGame(null);
        assertThat(game.legalMoves()).hasSize(20);
        assertThat(game.fen()).isEqualTo(ChessRulesService.STANDARD_FEN);
        assertThat(game.status()).isEqualTo(GameStatus.IN_PROGRESS);
    }

    @Test
    void rejectsIllegalMovesAndKeepsPosition() {
        ChessPositionAdapter game = rules.newGame(null);
        assertThat(game.isLegal("e2e5")).isFalse();
        assertThatThrownBy(() -> game.play("e2e5")).isInstanceOf(IllegalMoveException.class);
        assertThat(game.fen()).isEqualTo(ChessRulesService.STANDARD_FEN);
    }

    @Test
    void producesCorrectSan() {
        ChessPositionAdapter game = rules.replay(ChessRulesService.STANDARD_FEN, List.of("e2e4", "e7e5", "g1f3"));
        assertThat(game.describe("b8c6").san()).isEqualTo("Nc6");
        ChessPositionAdapter scholar = rules.replay(ChessRulesService.STANDARD_FEN,
                List.of("e2e4", "e7e5", "f1c4", "b8c6", "d1h5", "g8f6"));
        LegalMove mate = scholar.describe("h5f7");
        assertThat(mate.san()).isEqualTo("Qxf7#");
        assertThat(mate.capture()).isTrue();
        assertThat(mate.check()).isTrue();
    }

    @Test
    void castlingBothSides() {
        ChessPositionAdapter game = rules.fromFen("r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1");
        assertThat(game.describe("e1g1").san()).isEqualTo("O-O");
        assertThat(game.describe("e1c1").san()).isEqualTo("O-O-O");
        ChessPositionAdapter after = game.play("e1g1");
        assertThat(after.fen()).startsWith("r3k2r/8/8/8/8/8/8/R4RK1 b kq");
    }

    @Test
    void castlingThroughCheckIsIllegal() {
        ChessPositionAdapter game = rules.fromFen("r3k2r/8/8/8/8/8/5r2/R3K2R w KQkq - 0 1");
        // Turm auf f2 gibt kein Schach, kontrolliert aber f1
        assertThat(game.isLegal("e1g1")).isFalse();
        assertThat(game.isLegal("e1c1")).isTrue();
    }

    @Test
    void enPassant() {
        ChessPositionAdapter game = rules.replay(ChessRulesService.STANDARD_FEN,
                List.of("e2e4", "a7a6", "e4e5", "d7d5"));
        assertThat(game.isLegal("e5d6")).isTrue();
        LegalMove ep = game.describe("e5d6");
        assertThat(ep.capture()).isTrue();
        assertThat(ep.san()).isEqualTo("exd6");
        ChessPositionAdapter after = game.play("e5d6");
        assertThat(after.position().pieceAt(de.chesscoach.domain.position.Squares.parse("d5"))).isNull();
    }

    @Test
    void promotionToAllPieces() {
        ChessPositionAdapter game = rules.fromFen("8/4P3/8/8/8/k7/8/4K3 w - - 0 1");
        assertThat(game.legalUciMoves()).contains("e7e8q", "e7e8r", "e7e8b", "e7e8n");
        LegalMove knight = game.describe("e7e8n");
        assertThat(knight.promotion()).isEqualTo(PieceKind.KNIGHT);
        assertThat(knight.san()).isEqualTo("e8=N");
        assertThat(game.play("e7e8r").position().pieceAt(de.chesscoach.domain.position.Squares.parse("e8")).kind())
                .isEqualTo(PieceKind.ROOK);
    }

    @Test
    void detectsCheckmate() {
        ChessPositionAdapter game = rules.replay(ChessRulesService.STANDARD_FEN,
                List.of("f2f3", "e7e5", "g2g4", "d8h4"));
        assertThat(game.status()).isEqualTo(GameStatus.CHECKMATE);
        assertThat(game.inCheck()).isTrue();
        assertThat(game.legalMoves()).isEmpty();
    }

    @Test
    void detectsStalemate() {
        ChessPositionAdapter game = rules.fromFen("7k/5Q2/6K1/8/8/8/8/8 b - - 0 1");
        assertThat(game.status()).isEqualTo(GameStatus.STALEMATE);
    }

    @Test
    void detectsInsufficientMaterial() {
        assertThat(rules.fromFen("8/8/4k3/8/8/3K4/8/8 w - - 0 1").status())
                .isEqualTo(GameStatus.DRAW_INSUFFICIENT_MATERIAL);
        assertThat(rules.fromFen("8/8/4k3/8/8/3KN3/8/8 w - - 0 1").status())
                .isEqualTo(GameStatus.DRAW_INSUFFICIENT_MATERIAL);
        assertThat(rules.fromFen("8/8/4k3/8/8/3K4/8/R7 w - - 0 1").status())
                .isEqualTo(GameStatus.IN_PROGRESS);
    }

    @Test
    void detectsThreefoldRepetition() {
        List<String> moves = List.of("g1f3", "g8f6", "f3g1", "f6g8", "g1f3", "g8f6", "f3g1", "f6g8");
        ChessPositionAdapter game = rules.replay(ChessRulesService.STANDARD_FEN, moves.subList(0, 7));
        assertThat(game.status()).isEqualTo(GameStatus.IN_PROGRESS);
        ChessPositionAdapter repeated = rules.replay(ChessRulesService.STANDARD_FEN, moves);
        assertThat(repeated.status()).isEqualTo(GameStatus.DRAW_THREEFOLD_REPETITION);
    }

    @Test
    void detectsFiftyMoveRule() {
        ChessPositionAdapter game = rules.fromFen("8/8/4k3/8/8/3K4/8/R7 w - - 99 80");
        assertThat(game.status()).isEqualTo(GameStatus.IN_PROGRESS);
        assertThat(game.play("a1a2").status()).isEqualTo(GameStatus.DRAW_FIFTY_MOVE_RULE);
    }

    @Test
    void undoByReplayRestoresPreviousPosition() {
        ChessPositionAdapter one = rules.replay(ChessRulesService.STANDARD_FEN, List.of("e2e4"));
        ChessPositionAdapter two = one.play("c7c5");
        ChessPositionAdapter undone = rules.replay(two.initialFen(), two.uciMoves().subList(0, 1));
        assertThat(undone.fen()).isEqualTo(one.fen());
    }

    @Test
    void validatesFen() {
        assertThatThrownBy(() -> rules.validateFen("garbage")).isInstanceOf(InvalidFenException.class);
        assertThatThrownBy(() -> rules.validateFen("8/8/8/8/8/8/8/8 w - - 0 1")).isInstanceOf(InvalidFenException.class);
        assertThatThrownBy(() -> rules.validateFen("4k3/8/8/8/8/8/8/4K2r b - - 0 1"))
                .as("Seite nicht am Zug steht im Schach")
                .isInstanceOf(InvalidFenException.class);
        assertThatThrownBy(() -> rules.validateFen("P3k3/8/8/8/8/8/8/4K3 w - - 0 1")).isInstanceOf(InvalidFenException.class);
    }

    @Test
    void validateLineStopsAtFirstIllegalMove() {
        ChessRulesService.ValidatedLine line = rules.validateLine(ChessRulesService.STANDARD_FEN,
                List.of("e2e4", "e7e5", "e4e5", "g1f3"), 10);
        assertThat(line.moves()).extracting(LegalMove::san).containsExactly("e4", "e5");
        assertThat(line.truncated()).isTrue();
    }
}
