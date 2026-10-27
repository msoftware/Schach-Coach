package de.chesscoach.dto.response;

import java.util.List;
import java.util.Map;

/** Antworten für Gesundheits-, Engine-, Einstellungs- und Regelabfragen. */
public final class StatusResponses {

    private StatusResponses() {
    }

    public record HealthResponse(String status, boolean engineAvailable, int activeGames, String version) {
    }

    public record EngineStatusResponse(boolean available, List<EngineInstance> engines, int analysisQueue) {
    }

    public record EngineInstance(String role, String name, String author, String version, String state, String message,
                                 boolean supportsMultiPv) {
    }

    /** Verfügbare Einstellungen inklusive der überprüfbaren Engine-Parameter je Spielstärke. */
    public record SettingsOptionsResponse(List<DifficultyOption> difficulties, List<ProfileOption> analysisProfiles,
                                          Map<String, Object> classificationThresholds, String rulesVersion) {
    }

    public record DifficultyOption(String id, String label, Long nodes, Integer depth, Long movetimeMillis,
                                   Double sampleFraction, String description) {
    }

    public record ProfileOption(String id, String label, double factor) {
    }

    public record SettingsResponse(String gameId, String difficulty, String analysisProfile, int settingsVersion,
                                   List<String> outdatedAnalyses) {
    }

    public record HintResponse(String uci, String san, String from, String pieceName, String score, int depth, String source) {
    }

    /**
     * Bewertung der aktuellen Stellung für die Bewertungsleiste, immer aus Sicht von Weiß.
     *
     * @param score      Bewertung (CP oder MATE, positiv = Weiß steht besser); {@code null} bei beendeter Partie
     * @param whiteShare Anteil von Weiß an der Leiste (0 = Schwarz gewinnt, 0.5 = ausgeglichen, 1 = Weiß gewinnt)
     * @param display    Anzeige, z. B. "+0.35", "M3", "-M2" oder bei beendeter Partie das Ergebnis
     */
    public record EvaluationResponse(String gameId, int positionVersion, String fen, ScoreDto score, double whiteShare,
                                     String display, int depth, boolean finished) {
    }

    public record RuleDto(String id, String title, String category, String version, String purpose, List<String> inputFeatures,
                          String learningPrinciple, boolean enabled) {
    }
}
