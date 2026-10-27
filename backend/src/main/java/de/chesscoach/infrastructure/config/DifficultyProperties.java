package de.chesscoach.infrastructure.config;

import de.chesscoach.domain.game.Difficulty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.util.EnumMap;
import java.util.Map;

/**
 * Überprüfbare Engine-Parameter je Spielstärke (Präfix {@code chesscoach.difficulty}).
 *
 * @param levels     Parameter je Stufe
 * @param randomSeed optionaler Startwert des Zufallsgenerators (für reproduzierbare Tests); 0 = zufällig
 */
@ConfigurationProperties("chesscoach.difficulty")
public record DifficultyProperties(Map<Difficulty, Level> levels, @DefaultValue("0") long randomSeed) {

    public DifficultyProperties {
        Map<Difficulty, Level> merged = new EnumMap<>(defaults());
        if (levels != null) {
            merged.putAll(levels);
        }
        levels = Map.copyOf(merged);
    }

    /**
     * Engine-Parameter einer Stufe.
     *
     * @param nodes          Knotenlimit der Suche (null = nicht begrenzt)
     * @param depth          Tiefenlimit der Suche (null = nicht begrenzt)
     * @param movetimeMillis Suchzeit (null = nicht begrenzt)
     * @param sampleFraction Anteil der legalen Züge, aus denen die Engine wählt (null = alle Züge, volle Suche).
     *                       Die Engine bewertet jeden Zug der zufälligen Auswahl und spielt den besten davon.
     * @param minSample      Mindestgröße der Auswahl
     */
    public record Level(Long nodes, Integer depth, Long movetimeMillis, Double sampleFraction, Integer minSample) {

        public String describe() {
            StringBuilder sb = new StringBuilder();
            if (sampleFraction != null) {
                sb.append("Auswahl aus ").append(Math.round(sampleFraction * 100)).append(" % der legalen Züge, ");
            }
            if (nodes != null) {
                sb.append(nodes).append(" Knoten");
            }
            if (depth != null) {
                sb.append(sb.isEmpty() || sb.toString().endsWith(", ") ? "" : ", ").append("Tiefe ").append(depth);
            }
            if (movetimeMillis != null) {
                sb.append(sb.isEmpty() || sb.toString().endsWith(", ") ? "" : ", ").append(movetimeMillis).append(" ms Suchzeit");
            }
            return sb.toString();
        }
    }

    public static Map<Difficulty, Level> defaults() {
        Map<Difficulty, Level> map = new EnumMap<>(Difficulty.class);
        map.put(Difficulty.BEGINNER, new Level(200L, null, null, 0.25, 2));
        map.put(Difficulty.EASY, new Level(1500L, null, null, 0.5, 3));
        map.put(Difficulty.MEDIUM, new Level(20000L, null, null, null, null));
        map.put(Difficulty.STRONG, new Level(250000L, null, null, null, null));
        map.put(Difficulty.MAXIMUM, new Level(null, null, 3000L, null, null));
        return map;
    }

    public Level level(Difficulty difficulty) {
        return levels.get(difficulty);
    }
}
