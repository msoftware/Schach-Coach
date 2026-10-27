package de.chesscoach.domain.position;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PositionModelTest {

    @Test
    void fenRoundTrip() {
        String fen = "r1bqkbnr/pppp1ppp/2n5/4p3/4P3/5N2/PPPP1PPP/RNBQKB1R w KQkq - 2 3";
        assertThat(Position.fromFen(fen).toFen()).isEqualTo(fen);
        assertThatThrownBy(() -> Position.fromFen("8/8 w - -")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void squareColoursFollowBoardConvention() {
        assertThat(Squares.isLight(Squares.parse("h1"))).isTrue();
        assertThat(Squares.isLight(Squares.parse("a1"))).isFalse();
        assertThat(Squares.name(Squares.parse("e4"))).isEqualTo("e4");
        assertThat(Squares.centerDistance(Squares.parse("d4"))).isZero();
        assertThat(Squares.centerDistance(Squares.parse("a1"))).isEqualTo(3);
    }

    @Test
    void attacksRespectBlockers() {
        Position p = Position.fromFen("4k3/8/8/8/8/8/4P3/4RK2 w - - 0 1");
        int rook = Squares.parse("e1");
        assertThat(p.attacks(rook, Squares.parse("e2"))).isTrue();
        assertThat(p.attacks(rook, Squares.parse("e3"))).isFalse();
        assertThat(p.attackersOf(Squares.parse("d3"), Color.WHITE)).containsExactly(Squares.parse("e2"));
        assertThat(p.isInCheck(Color.BLACK)).isFalse();
    }

    @Test
    void playHandlesCastlingEnPassantAndPromotion() {
        Position castle = Position.fromFen("r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1").play(Squares.parse("e1"), Squares.parse("g1"), null);
        assertThat(castle.pieceAt(Squares.parse("f1")).kind()).isEqualTo(PieceKind.ROOK);
        assertThat(castle.castling()).isEqualTo("kq");
        Position ep = Position.fromFen("4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 1").play(Squares.parse("e5"), Squares.parse("d6"), null);
        assertThat(ep.pieceAt(Squares.parse("d5"))).isNull();
        Position promo = Position.fromFen("4k3/1P6/8/8/8/8/8/4K3 w - - 0 1").play(Squares.parse("b7"), Squares.parse("b8"), PieceKind.QUEEN);
        assertThat(promo.pieceAt(Squares.parse("b8"))).isEqualTo(new Piece(Color.WHITE, PieceKind.QUEEN));
    }

    @Test
    void staticExchangeEvaluation() {
        // Springer e5 nur vom Springer c6 angegriffen und ungedeckt: Gewinn eines Springers.
        Position p = Position.fromFen("r1bqkbnr/pppp1ppp/2n5/4N3/4P3/8/PPPP1PPP/RNBQKB1R b KQkq - 0 3");
        assertThat(StaticExchange.evaluate(p, Squares.parse("e5"), Color.BLACK)).isEqualTo(300);
        // Gedeckter Bauer, vom Springer angegriffen: Schlagen verliert Material.
        Position q = Position.fromFen("4k3/8/2n5/4P3/3P4/8/8/4K3 b - - 0 1");
        assertThat(StaticExchange.evaluate(q, Squares.parse("e5"), Color.BLACK)).isNegative();
        assertThat(StaticExchange.bestCaptureGain(q, Squares.parse("e5"), Color.BLACK)).isZero();
    }

    @Test
    void pawnStructureFeatures() {
        Position p = Position.fromFen("4k3/8/8/8/2P5/2P1P3/8/4K3 w - - 0 1");
        PawnStructure white = new PawnStructure(p, Color.WHITE);
        assertThat(white.doubled()).containsExactlyInAnyOrder(Squares.parse("c3"), Squares.parse("c4"));
        assertThat(white.isolated()).hasSize(3);
        assertThat(white.passed()).hasSize(3);
        Position iso = Position.fromFen("4k3/pp3ppp/8/3p4/8/8/PP3PPP/4K3 w - - 0 1");
        assertThat(new PawnStructure(iso, Color.BLACK).isolated()).containsExactly(Squares.parse("d5"));
        assertThat(new PawnStructure(iso, Color.WHITE).isFileHalfOpen(3)).isTrue();
        Position backward = Position.fromFen("4k3/8/3p4/2p1P3/2P5/8/8/4K3 b - - 0 1");
        assertThat(new PawnStructure(backward, Color.BLACK).backward()).containsExactly(Squares.parse("d6"));
        Position hanging = Position.fromFen("4k3/p4ppp/8/2pp4/8/8/PP3PPP/4K3 w - - 0 1");
        assertThat(new PawnStructure(hanging, Color.BLACK).hanging()).containsExactlyInAnyOrder(Squares.parse("c5"), Squares.parse("d5"));
    }

    @Test
    void pinsAndSkewers() {
        Position pin = Position.fromFen("4k3/8/2n5/1B6/8/8/8/4K3 b - - 0 1");
        List<LineMotifs.Motif> motifs = LineMotifs.find(pin, Color.WHITE);
        assertThat(motifs).singleElement().satisfies(m -> {
            assertThat(m.type()).isEqualTo(LineMotifs.Type.PIN);
            assertThat(m.absolute()).isTrue();
        });
        Position skewer = Position.fromFen("4q3/8/8/4k3/8/8/8/4R1K1 b - - 0 1");
        assertThat(LineMotifs.find(skewer, Color.WHITE)).extracting(LineMotifs.Motif::type).containsExactly(LineMotifs.Type.SKEWER);
    }

    @Test
    void phases() {
        assertThat(Position.fromFen("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1").isOpening()).isTrue();
        assertThat(Position.fromFen("4k3/8/8/8/8/8/4P3/4K3 w - - 0 60").isEndgame()).isTrue();
        assertThat(Position.fromFen("4k3/8/8/8/8/8/4P3/4K3 w - - 0 60").materialBalance(Color.WHITE)).isEqualTo(1);
    }
}
