package de.chesscoach.coaching;

import de.chesscoach.domain.coaching.MoveClassification;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Ein Bestzug innerhalb der Toleranz hat nur gleich gute Alternativen – nie „bessere“. */
class ExplanationWordingTest {

    @Test
    void alternativeIsOnlyCalledBetterWhenItIs() {
        assertThat(ExplanationBuilder.alternativeLead(MoveClassification.BEST)).isEqualTo("Gleich gut war");
        assertThat(ExplanationBuilder.alternativeLead(MoveClassification.EXCELLENT)).isEqualTo("Etwas genauer war");
        assertThat(ExplanationBuilder.alternativeLead(MoveClassification.GOOD)).isEqualTo("Besser war");
        assertThat(ExplanationBuilder.alternativeLead(MoveClassification.BLUNDER)).isEqualTo("Besser war");
    }
}
