package de.chesscoach.engine;

/**
 * Suchgrenzen für Analysen. Nicht gesetzte Grenzen sind {@code null}.
 *
 * @param depth          maximale Suchtiefe in Halbzügen
 * @param nodes          maximale Knotenzahl
 * @param movetimeMillis maximale Suchzeit
 */
public record AnalysisLimits(Integer depth, Long nodes, Long movetimeMillis) {

    public AnalysisLimits {
        if (depth == null && nodes == null && movetimeMillis == null) {
            throw new IllegalArgumentException("Mindestens eine Suchgrenze ist erforderlich");
        }
        if (depth != null && (depth < 1 || depth > 64)) {
            throw new IllegalArgumentException("Suchtiefe außerhalb des erlaubten Bereichs");
        }
        if (nodes != null && (nodes < 1 || nodes > 1_000_000_000L)) {
            throw new IllegalArgumentException("Knotenzahl außerhalb des erlaubten Bereichs");
        }
        if (movetimeMillis != null && (movetimeMillis < 1 || movetimeMillis > 120_000)) {
            throw new IllegalArgumentException("Suchzeit außerhalb des erlaubten Bereichs");
        }
    }

    public static AnalysisLimits nodes(long nodes) {
        return new AnalysisLimits(null, nodes, null);
    }

    public static AnalysisLimits depth(int depth) {
        return new AnalysisLimits(depth, null, null);
    }

    public static AnalysisLimits movetime(long millis) {
        return new AnalysisLimits(null, null, millis);
    }

    public AnalysisLimits scaled(double factor) {
        return new AnalysisLimits(depth, nodes == null ? null : Math.max(1, Math.round(nodes * factor)),
                movetimeMillis == null ? null : Math.max(1, Math.round(movetimeMillis * factor)));
    }

    public EngineLimits toEngineLimits() {
        return new EngineLimits(depth, nodes, movetimeMillis);
    }

    public String describe() {
        StringBuilder sb = new StringBuilder();
        if (depth != null) {
            sb.append("Tiefe ").append(depth);
        }
        if (nodes != null) {
            sb.append(sb.isEmpty() ? "" : ", ").append(nodes).append(" Knoten");
        }
        if (movetimeMillis != null) {
            sb.append(sb.isEmpty() ? "" : ", ").append(movetimeMillis).append(" ms");
        }
        return sb.toString();
    }
}
