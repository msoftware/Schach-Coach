package de.chesscoach.coaching;

import de.chesscoach.domain.analysis.Score;
import de.chesscoach.domain.coaching.Confidence;
import de.chesscoach.domain.coaching.MoveClassification;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Ordnet einen Zug einer Qualitätsklasse zu (Requirements 6.2/6.3).
 *
 * <p>Die Klassifizierung beruht nicht allein auf festen Centipawn-Grenzen: Mattbewertungen werden gesondert
 * behandelt, ein Wechsel von Vorteil zu Nachteil wird verschärft, Verluste in bereits entschiedenen Stellungen
 * werden über die Gewinnwahrscheinlichkeit gedämpft, und unsichere Analysen werden als vorläufig markiert
 * bzw. als {@link MoveClassification#UNCERTAIN} ausgewiesen.</p>
 */
@Component
public class MistakeClassifier {

    private final CoachingProperties.Classification thresholds;

    public MistakeClassifier(CoachingProperties properties) {
        this.thresholds = properties.classification();
    }

    /**
     * @param scoreBefore      Bewertung der Stellung vor dem Zug (Sicht des Benutzers)
     * @param playedScore      Bewertung nach dem gespielten Zug (Sicht des Benutzers, gleiche Suchbedingungen wie bestScore)
     * @param bestScore        Bewertung nach dem besten gefundenen Zug
     * @param legalMoveCount   Anzahl legaler Züge in der Ausgangsstellung
     * @param playedIsBest     gespielter Zug ist der beste gefundene Zug
     * @param secondBestScore  Bewertung des zweitbesten Kandidaten (oder null)
     * @param confidence       Zuverlässigkeit der Engine-Analyse
     */
    public record Input(Score scoreBefore, Score playedScore, Score bestScore, int legalMoveCount, boolean playedIsBest,
                        Score secondBestScore, Confidence confidence) {
    }

    /**
     * @param classification       Qualitätsklasse
     * @param lossCentipawns       Verlust gegenüber dem besten Zug (null, wenn nicht zuverlässig berechenbar, z. B. bei Matt)
     * @param winProbabilityLoss   Verlust an Gewinnwahrscheinlichkeit (0..1)
     * @param provisional          true, wenn die Einordnung wegen geringer Analysezuverlässigkeit vorläufig ist
     * @param reasons              nachvollziehbare Gründe der Einordnung
     */
    public record Result(MoveClassification classification, Integer lossCentipawns, double winProbabilityLoss,
                         boolean provisional, List<String> reasons) {
    }

    public Result classify(Input in) {
        List<String> reasons = new ArrayList<>();
        if (in.playedScore() == null || in.bestScore() == null) {
            reasons.add("Keine verlässliche Engine-Bewertung des Zuges verfügbar.");
            return new Result(MoveClassification.UNCERTAIN, null, 0, true, reasons);
        }
        boolean provisional = in.confidence() == Confidence.LOW;
        if (provisional) {
            reasons.add("Die Analyse hat eine geringe Zuverlässigkeit; die Einordnung ist vorläufig.");
        }
        if (in.legalMoveCount() == 1) {
            reasons.add("Es gab nur einen legalen Zug.");
            return new Result(MoveClassification.FORCED, 0, 0, provisional, reasons);
        }
        Score played = in.playedScore();
        Score best = in.bestScore();
        double wpLoss = Math.max(0, best.winProbability() - played.winProbability());

        // Mattbewertungen gesondert behandeln.
        if (best.isWinningMate() && played.isWinningMate()) {
            int extra = played.value() - best.value();
            reasons.add("Beide Züge führen laut Engine zum Matt (bester Zug: Matt in " + best.value()
                    + ", gespielter Zug: Matt in " + played.value() + ").");
            MoveClassification cls = in.playedIsBest() || extra <= 0 ? MoveClassification.BEST
                    : extra <= 2 ? MoveClassification.EXCELLENT : MoveClassification.GOOD;
            return new Result(forcedIfOnlyMove(cls, in, reasons), null, 0, provisional, reasons);
        }
        if (best.isWinningMate() && !played.isWinningMate()) {
            reasons.add("Der beste Zug hätte ein erzwungenes Matt in " + best.value() + " Zügen ermöglicht.");
            int playedCp = played.centipawnsClamped(3000);
            MoveClassification cls = playedCp >= 500 ? MoveClassification.INACCURACY
                    : playedCp >= 200 ? MoveClassification.MISTAKE : MoveClassification.BLUNDER;
            return new Result(cls, null, wpLoss, provisional, reasons);
        }
        if (played.isLosingMate() && !best.isLosingMate()) {
            reasons.add("Nach dem gespielten Zug kann der Gegner ein Matt erzwingen, nach dem besten Zug nicht.");
            return new Result(MoveClassification.BLUNDER, null, wpLoss, provisional, reasons);
        }
        if (played.isLosingMate() && best.isLosingMate()) {
            reasons.add("Die Stellung ist laut Engine in jedem Fall verloren (Matt unvermeidbar).");
            int diff = best.value() - played.value();
            MoveClassification cls = in.playedIsBest() || diff <= 0 ? MoveClassification.BEST
                    : diff <= 2 ? MoveClassification.GOOD : MoveClassification.INACCURACY;
            return new Result(cls, null, 0, provisional, reasons);
        }

        int bestCp = best.centipawnsClamped(3000);
        int playedCp = played.centipawnsClamped(3000);
        int loss = in.playedIsBest() ? 0 : Math.max(0, bestCp - playedCp);
        MoveClassification cls;
        if (in.playedIsBest() || loss <= thresholds.bestMaxLoss()) {
            cls = MoveClassification.BEST;
        } else if (loss <= thresholds.excellentMaxLoss()) {
            cls = MoveClassification.EXCELLENT;
        } else if (loss <= thresholds.goodMaxLoss()) {
            cls = MoveClassification.GOOD;
        } else if (loss <= thresholds.inaccuracyMaxLoss()) {
            cls = MoveClassification.INACCURACY;
        } else if (loss <= thresholds.mistakeMaxLoss()) {
            cls = MoveClassification.MISTAKE;
        } else {
            cls = MoveClassification.BLUNDER;
        }
        reasons.add("Bewertungsverlust gegenüber dem besten Zug: " + loss + " Centipawns (gleiche Suchbedingungen).");

        // Wechsel von Vorteil zu Nachteil verschärft die Einordnung.
        boolean swing = bestCp >= thresholds.swingThreshold() && playedCp <= -thresholds.swingThreshold();
        if (swing && cls.compareTo(MoveClassification.MISTAKE) < 0) {
            cls = MoveClassification.MISTAKE;
            reasons.add("Der Zug verwandelt eine vorteilhafte Stellung in eine nachteilige.");
        }
        // Gedämpfte Einordnung, wenn sich am Ausgang der Partie praktisch nichts ändert.
        if (!swing && cls.isError() && wpLoss < thresholds.negligibleWinProbabilityLoss()) {
            cls = MoveClassification.GOOD;
            reasons.add("Die Stellung ist bereits entschieden; der Verlust ändert die Gewinnaussichten praktisch nicht.");
        } else if (!swing && (cls == MoveClassification.MISTAKE || cls == MoveClassification.BLUNDER)
                && wpLoss < thresholds.minorWinProbabilityLoss()) {
            cls = MoveClassification.INACCURACY;
            reasons.add("Der Centipawn-Verlust ist groß, die Gewinnaussichten ändern sich aber nur geringfügig.");
        }
        return new Result(forcedIfOnlyMove(cls, in, reasons), loss, wpLoss, provisional, reasons);
    }

    /** Erzwungen: bester Zug gespielt, alle Alternativen deutlich schlechter. */
    private MoveClassification forcedIfOnlyMove(MoveClassification cls, Input in, List<String> reasons) {
        if (cls != MoveClassification.BEST || !in.playedIsBest() || in.secondBestScore() == null) {
            return cls;
        }
        // In gewonnenen Stellungen ist der stärkste Zug ein Bestzug, kein "erzwungener" Zug.
        if (in.bestScore().isWinningMate() || in.bestScore().centipawnsClamped(3000) > 300) {
            return cls;
        }
        int gap = in.bestScore().centipawnsClamped(3000) - in.secondBestScore().centipawnsClamped(3000);
        if (gap >= thresholds.forcedGap()) {
            reasons.add("Alle anderen geprüften Züge sind um mindestens " + thresholds.forcedGap()
                    + " Centipawns schlechter – der Zug war praktisch erzwungen.");
            return MoveClassification.FORCED;
        }
        return cls;
    }
}
