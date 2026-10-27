package de.chesscoach.domain.analysis;

import de.chesscoach.domain.coaching.CoachingFinding;
import de.chesscoach.domain.coaching.CoachingTip;
import de.chesscoach.domain.coaching.Confidence;
import de.chesscoach.domain.coaching.ExplanationSection;
import de.chesscoach.domain.coaching.ExplanationType;
import de.chesscoach.domain.coaching.MoveClassification;
import de.chesscoach.domain.coaching.StrategicPlan;

import java.util.List;

/**
 * Vollständiges, unveränderliches Ergebnis der Analyse eines Benutzerzuges (MoveAssessment, Requirements 10.1).
 */
public record MoveAnalysis(
        String analysisId,
        String fenBefore,
        String fenAfter,
        CandidateEvaluation playedMove,
        CandidateEvaluation bestMove,
        List<CandidateEvaluation> candidates,
        Score scoreBefore,
        int rootDepth,
        MoveClassification classification,
        boolean classificationProvisional,
        List<String> classificationReasons,
        Integer evaluationLoss,
        double winProbabilityLoss,
        ExplanationType explanationType,
        String headline,
        String mainReason,
        List<String> mainReasonSources,
        List<ExplanationSection> sections,
        CoachingTip tip,
        List<CoachingFinding> findings,
        List<StrategicPlan> strategicPlans,
        List<String> learningTags,
        int materialBefore,
        int materialAfter,
        Confidence confidence,
        List<String> confidenceReasons,
        boolean critical,
        int depthLevel,
        String engineName,
        String engineVersion,
        String searchLimits,
        long engineNodes,
        long elapsedMillis,
        String rulesVersion) {
}
