package de.chesscoach.coaching;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.util.Set;

/**
 * Konfiguration der Coaching-Logik.
 *
 * @param rulesVersion    Version des Regelwerks (Teil des Analyse-Cache-Schlüssels)
 * @param disabledRules   deaktivierte Regel-IDs
 * @param classification  Schwellenwerte der Qualitätsklassen
 */
@ConfigurationProperties("chesscoach.coaching")
public record CoachingProperties(@DefaultValue("1.0.0") String rulesVersion,
                                 @DefaultValue Set<String> disabledRules,
                                 @DefaultValue Classification classification) {

    public CoachingProperties {
        disabledRules = disabledRules == null ? Set.of() : Set.copyOf(disabledRules);
        classification = classification == null ? Classification.defaults() : classification;
    }

    public static CoachingProperties defaults() {
        return new CoachingProperties("1.0.0", Set.of(), Classification.defaults());
    }

    /**
     * Schwellenwerte in Centipawns bzw. Gewinnwahrscheinlichkeit (konfigurierbar, Requirements 6.3).
     *
     * @param bestMaxLoss          maximaler Verlust, der noch als Bestzug gilt
     * @param excellentMaxLoss     maximaler Verlust für "Sehr gut"
     * @param goodMaxLoss          maximaler Verlust für "Gut"
     * @param inaccuracyMaxLoss    maximaler Verlust für "Ungenau"
     * @param mistakeMaxLoss       maximaler Verlust für "Fehler"; darüber "Grober Fehler"
     * @param minorWinProbabilityLoss  unterhalb dieses Verlusts an Gewinnwahrscheinlichkeit höchstens "Ungenau"
     * @param negligibleWinProbabilityLoss unterhalb dieses Verlusts höchstens "Gut"
     * @param forcedGap            Abstand zum zweitbesten Zug, ab dem der beste Zug als erzwungen gilt
     * @param swingThreshold       Bewertungsgrenze für einen Wechsel von Vorteil zu Nachteil
     */
    public record Classification(@DefaultValue("10") int bestMaxLoss,
                                 @DefaultValue("25") int excellentMaxLoss,
                                 @DefaultValue("60") int goodMaxLoss,
                                 @DefaultValue("120") int inaccuracyMaxLoss,
                                 @DefaultValue("250") int mistakeMaxLoss,
                                 @DefaultValue("0.06") double minorWinProbabilityLoss,
                                 @DefaultValue("0.02") double negligibleWinProbabilityLoss,
                                 @DefaultValue("200") int forcedGap,
                                 @DefaultValue("150") int swingThreshold) {

        public static Classification defaults() {
            return new Classification(10, 25, 60, 120, 250, 0.06, 0.02, 200, 150);
        }
    }
}
