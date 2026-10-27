package de.chesscoach.dto.response;

import java.time.Instant;
import java.util.List;

/**
 * Coaching-Ergebnis eines Benutzerzuges (API-Version 1). Felder des Ergebnisses sind nur bei
 * {@code status = COMPLETED} gesetzt; die UI darf sie in anderen Zuständen nicht voraussetzen.
 *
 * @param positionVersion        Positionsversion unmittelbar nach dem analysierten Zug
 * @param currentPositionVersion aktuelle Positionsversion der Partie
 * @param outdated               Analyse wurde mit veralteten Analyse-Einstellungen erstellt
 * @param classification         BEST, EXCELLENT, GOOD, INACCURACY, MISTAKE, BLUNDER, FORCED oder UNCERTAIN
 * @param explanationType        Erklärungstyp, z. B. TACTICAL_BLUNDER oder GOOD_MOVE
 * @param analysisConfidence     HIGH, MEDIUM oder LOW
 */
public record AnalysisResponse(String apiVersion, String gameId, int ply, String moveId, String playedMove,
                               String playedMoveUci, String fenBefore, String fenAfter, int positionVersion,
                               int currentPositionVersion, String status, String stage, int progressPercent,
                               String errorMessage, boolean outdated, int settingsVersion, int depthLevel,
                               Instant requestedAt, Instant completedAt,
                               String classification, String classificationLabel, Boolean classificationProvisional,
                               List<String> classificationReasons, String explanationType, String explanationTypeLabel,
                               String headline, String mainReason, List<String> mainReasonSources,
                               ScoreDto scoreBefore, ScoreDto scoreAfter, ScoreDto bestScore, EvaluationLoss evaluationLoss,
                               String bestMove, String bestMoveUci, String bestMoveExplanation, List<String> principalVariation,
                               List<CandidateDto> candidates, List<FindingDto> findings, List<PlanDto> strategicPlans,
                               List<SectionDto> sections, TipDto tip, List<String> learningTags, Material material,
                               String analysisConfidence, List<String> confidenceReasons, Boolean critical,
                               EngineDetails engine, String rulesVersion) {

    /**
     * @param centipawns          Verlust gegenüber dem besten Zug (null, wenn nicht zuverlässig berechenbar, z. B. bei Matt)
     * @param winProbabilityLoss  Verlust an Gewinnerwartung (0..1)
     * @param reliable            true, wenn der Wert auf vergleichbaren Suchbedingungen beruht und die Analyse nicht unsicher ist
     */
    public record EvaluationLoss(Integer centipawns, double winProbabilityLoss, boolean reliable) {
    }

    public record Material(int before, int after, int delta) {
    }

    public record EngineDetails(String name, String version, String searchLimits, long nodes, long elapsedMillis, int rootDepth) {
    }

    /**
     * Kandidatenzug mit Variante.
     *
     * @param line        Hauptvariante beginnend mit dem Kandidaten (nur legale Züge)
     * @param searchLine  immer true: Hauptvariante der Suche, keine erzwungene Zugfolge
     * @param comparison  Vergleich mit dem gespielten Zug in Worten
     */
    public record CandidateDto(String variationId, String uci, String san, ScoreDto score, int depth, long nodes,
                               List<VariationMoveDto> line, boolean lineTruncated, boolean played, boolean best,
                               Integer deltaToPlayed, List<String> motifs, List<String> strategicPros,
                               List<String> strategicCons, List<FindingDto> findings, String comparison, boolean searchLine) {
    }

    public record VariationMoveDto(String uci, String san, String numbered, String fenAfter) {
    }

    public record PlanDto(String side, String title, String explanation, List<String> squares, String basis, String source) {
    }

    public record SectionDto(String key, String question, List<StatementDto> statements) {
    }

    public record StatementDto(String text, List<String> sources) {
    }

    public record TipDto(String title, String observation, String why, String betterAlternative, String expectedConsequence,
                         String learningPrinciple, String confidence) {
    }
}
