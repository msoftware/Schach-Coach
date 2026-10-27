package de.chesscoach.coaching;

import de.chesscoach.domain.analysis.Score;
import de.chesscoach.domain.coaching.Confidence;
import de.chesscoach.domain.coaching.MoveClassification;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Klassifizierung anhand definierter Bewertungs-Konstellationen (Requirements 6.2/6.3). */
class MistakeClassifierTest {

    private final MistakeClassifier classifier = new MistakeClassifier(CoachingProperties.defaults());

    private MistakeClassifier.Result classify(Score before, Score played, Score best, boolean playedIsBest) {
        return classifier.classify(new MistakeClassifier.Input(before, played, best, 30, playedIsBest, null, Confidence.HIGH));
    }

    @ParameterizedTest(name = "best={0}, played={1} -> {2}")
    @CsvSource({
            "30, 25, BEST",
            "30, 10, EXCELLENT",
            "30, -10, GOOD",
            "30, -60, INACCURACY",
            "30, -170, MISTAKE",
            "30, -400, BLUNDER"
    })
    void centipawnThresholds(int best, int played, MoveClassification expected) {
        assertThat(classify(Score.cp(30), Score.cp(played), Score.cp(best), false).classification()).isEqualTo(expected);
    }

    @Test
    void bestMoveIsBest() {
        var r = classify(Score.cp(30), Score.cp(30), Score.cp(30), true);
        assertThat(r.classification()).isEqualTo(MoveClassification.BEST);
        assertThat(r.lossCentipawns()).isZero();
    }

    @Test
    void allowingMateIsBlunderAndLossIsNotACentipawnValue() {
        var r = classify(Score.cp(0), Score.mate(-2), Score.cp(10), false);
        assertThat(r.classification()).isEqualTo(MoveClassification.BLUNDER);
        assertThat(r.lossCentipawns()).isNull();
    }

    @Test
    void missingForcedMateWhileStillWinningIsInaccuracy() {
        var r = classify(Score.mate(3), Score.cp(900), Score.mate(3), false);
        assertThat(r.classification()).isEqualTo(MoveClassification.INACCURACY);
        assertThat(r.reasons()).anyMatch(s -> s.contains("Matt in 3"));
    }

    @Test
    void slowerMateIsStillGood() {
        assertThat(classify(Score.mate(2), Score.mate(3), Score.mate(2), false).classification()).isEqualTo(MoveClassification.EXCELLENT);
    }

    @Test
    void winningToLosingSwingIsAtLeastMistake() {
        var r = classify(Score.cp(200), Score.cp(-160), Score.cp(200), false);
        assertThat(r.classification()).isIn(MoveClassification.MISTAKE, MoveClassification.BLUNDER);
    }

    @Test
    void largeLossInDecidedPositionIsDampened() {
        var r = classify(Score.cp(1500), Score.cp(1150), Score.cp(1500), false);
        assertThat(r.classification()).isEqualTo(MoveClassification.GOOD);
        assertThat(r.reasons()).anyMatch(s -> s.contains("entschieden"));
    }

    @Test
    void onlyLegalMoveIsForced() {
        var r = classifier.classify(new MistakeClassifier.Input(Score.cp(0), Score.cp(0), Score.cp(0), 1, true, null, Confidence.HIGH));
        assertThat(r.classification()).isEqualTo(MoveClassification.FORCED);
    }

    @Test
    void onlyGoodMoveIsForcedButNotWhenAlternativesAreEqual() {
        var forced = classifier.classify(new MistakeClassifier.Input(Score.cp(0), Score.cp(0), Score.cp(0), 20, true, Score.cp(-400), Confidence.HIGH));
        assertThat(forced.classification()).isEqualTo(MoveClassification.FORCED);
        var notForced = classifier.classify(new MistakeClassifier.Input(Score.cp(0), Score.cp(0), Score.cp(0), 20, true, Score.cp(-10), Confidence.HIGH));
        assertThat(notForced.classification()).isEqualTo(MoveClassification.BEST);
    }

    @Test
    void missingEvaluationIsUncertain() {
        var r = classifier.classify(new MistakeClassifier.Input(Score.cp(0), null, Score.cp(0), 20, false, null, Confidence.LOW));
        assertThat(r.classification()).isEqualTo(MoveClassification.UNCERTAIN);
    }

    @Test
    void lowConfidenceIsProvisional() {
        var r = classifier.classify(new MistakeClassifier.Input(Score.cp(0), Score.cp(-300), Score.cp(0), 20, false, null, Confidence.LOW));
        assertThat(r.provisional()).isTrue();
    }

    @Test
    void scoreConversionFromChildPerspective() {
        assertThat(Score.fromChildPerspective(Score.cp(-40))).isEqualTo(Score.cp(40));
        assertThat(Score.fromChildPerspective(Score.mate(-2))).isEqualTo(Score.mate(3));
        assertThat(Score.fromChildPerspective(Score.mate(0))).isEqualTo(Score.mate(1));
        assertThat(Score.fromChildPerspective(Score.mate(4))).isEqualTo(Score.mate(-4));
        assertThat(Score.mate(1).comparable()).isGreaterThan(Score.mate(3).comparable());
        assertThat(Score.mate(-5).comparable()).isGreaterThan(Score.mate(-1).comparable());
        assertThat(Score.cp(20000).comparable()).isLessThan(Score.mate(20).comparable());
    }
}
