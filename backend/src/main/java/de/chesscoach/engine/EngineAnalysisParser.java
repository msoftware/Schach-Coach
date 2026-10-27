package de.chesscoach.engine;

import de.chesscoach.domain.analysis.Score;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/** Zerlegt UCI-Ausgaben ({@code info}, {@code bestmove}) in strukturierte Daten. */
public final class EngineAnalysisParser {

    private static final Pattern UCI_MOVE = Pattern.compile("^[a-h][1-8][a-h][1-8][qrbn]?$");

    /** Bound-Typ einer Bewertung: exakt, untere oder obere Schranke (Aspirationsfenster). */
    public enum Bound {
        EXACT, LOWER, UPPER
    }

    /** Eine geparste {@code info}-Zeile; nicht vorhandene Werte sind {@code null}. */
    public record InfoLine(Integer depth, Integer selectiveDepth, Integer multipv, Score score, Bound bound,
                           Long nodes, Long timeMillis, List<String> pv, String text) {

        /** Verwertbar für die Analyse: enthält Tiefe, Bewertung und Hauptvariante. */
        public boolean isComplete() {
            return depth != null && score != null && pv != null && !pv.isEmpty() && bound == Bound.EXACT;
        }
    }

    /** Ergebnis der {@code bestmove}-Zeile; {@code move} ist leer bei "(none)" oder "0000". */
    public record BestMoveLine(Optional<String> move, Optional<String> ponder) {
    }

    private EngineAnalysisParser() {
    }

    public static boolean isUciMove(String token) {
        return token != null && UCI_MOVE.matcher(token).matches();
    }

    public static Optional<InfoLine> parseInfo(String line) {
        if (line == null || !line.startsWith("info")) {
            return Optional.empty();
        }
        String[] tokens = line.trim().split("\\s+");
        Integer depth = null;
        Integer seldepth = null;
        Integer multipv = null;
        Score score = null;
        Bound bound = Bound.EXACT;
        Long nodes = null;
        Long time = null;
        List<String> pv = null;
        String text = null;
        try {
            for (int i = 1; i < tokens.length; i++) {
                switch (tokens[i]) {
                    case "depth" -> depth = Integer.parseInt(tokens[++i]);
                    case "seldepth" -> seldepth = Integer.parseInt(tokens[++i]);
                    case "multipv" -> multipv = Integer.parseInt(tokens[++i]);
                    case "nodes" -> nodes = Long.parseLong(tokens[++i]);
                    case "time" -> time = Long.parseLong(tokens[++i]);
                    case "nps", "hashfull", "tbhits", "currmovenumber", "cpuload", "currmove" -> i++;
                    case "score" -> {
                        String type = tokens[++i];
                        int value = Integer.parseInt(tokens[++i]);
                        score = switch (type) {
                            case "cp" -> Score.cp(value);
                            case "mate" -> Score.mate(value);
                            default -> throw new IllegalArgumentException("Unbekannter Score-Typ " + type);
                        };
                        if (i + 1 < tokens.length && ("lowerbound".equals(tokens[i + 1]) || "upperbound".equals(tokens[i + 1]))) {
                            bound = "lowerbound".equals(tokens[++i]) ? Bound.LOWER : Bound.UPPER;
                        }
                    }
                    case "pv" -> {
                        pv = new ArrayList<>();
                        while (i + 1 < tokens.length && isUciMove(tokens[i + 1])) {
                            pv.add(tokens[++i]);
                        }
                    }
                    case "string", "error" -> {
                        text = String.join(" ", java.util.Arrays.copyOfRange(tokens, i, tokens.length));
                        i = tokens.length;
                    }
                    default -> {
                        // Unbekannte Schlüssel werden ignoriert (UCI erlaubt Erweiterungen).
                    }
                }
            }
        } catch (RuntimeException e) {
            // Unvollständige oder fehlerhafte Zeile: nicht verwenden.
            return Optional.empty();
        }
        return Optional.of(new InfoLine(depth, seldepth, multipv, score, bound, nodes, time,
                pv == null ? null : List.copyOf(pv), text));
    }

    public static Optional<BestMoveLine> parseBestMove(String line) {
        if (line == null || !line.startsWith("bestmove")) {
            return Optional.empty();
        }
        String[] tokens = line.trim().split("\\s+");
        if (tokens.length < 2) {
            return Optional.empty();
        }
        Optional<String> move = isUciMove(tokens[1]) ? Optional.of(tokens[1]) : Optional.empty();
        if (move.isEmpty() && !"(none)".equals(tokens[1]) && !"0000".equals(tokens[1])) {
            return Optional.empty();
        }
        Optional<String> ponder = Optional.empty();
        if (tokens.length >= 4 && "ponder".equals(tokens[2]) && isUciMove(tokens[3])) {
            ponder = Optional.of(tokens[3]);
        }
        return Optional.of(new BestMoveLine(move, ponder));
    }
}
