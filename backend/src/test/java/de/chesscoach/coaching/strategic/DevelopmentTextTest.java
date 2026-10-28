package de.chesscoach.coaching.strategic;

import de.chesscoach.domain.position.Squares;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DevelopmentTextTest {

    @Test
    void singleBishopIsNamedInSingular() {
        assertThat(DevelopmentRule.bishopText(List.of(Squares.parse("c1")), 0, 5))
                .isEqualTo("Dein Bauernzug macht den Weg frei für deinen Läufer auf c1 (er erreicht jetzt 5 statt 0 Felder).");
        assertThat(DevelopmentRule.bishopText(List.of(Squares.parse("c1"), Squares.parse("f1")), 0, 10))
                .contains("deine noch unentwickelten Läufer").contains("zusammen 10 statt 0");
    }
}
