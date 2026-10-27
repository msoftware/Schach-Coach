package de.chesscoach.api;

import de.chesscoach.application.analysis.AnalysisEntry;
import de.chesscoach.application.analysis.AnalysisStatus;
import de.chesscoach.application.game.AnalysisQueryService;
import de.chesscoach.application.game.GameSnapshot;
import de.chesscoach.application.game.MoveService;
import de.chesscoach.domain.analysis.CandidateEvaluation;
import de.chesscoach.domain.analysis.MoveAnalysis;
import de.chesscoach.domain.coaching.CoachingFinding;
import de.chesscoach.domain.coaching.Confidence;
import de.chesscoach.domain.coaching.FindingCategory;
import de.chesscoach.domain.coaching.MoveClassification;
import de.chesscoach.domain.coaching.Polarity;
import de.chesscoach.domain.game.GameStatus;
import de.chesscoach.domain.game.MoveRecord;
import de.chesscoach.domain.position.Color;
import de.chesscoach.domain.position.Squares;
import de.chesscoach.dto.response.AnalysisResponse;
import de.chesscoach.dto.response.FindingDto;
import de.chesscoach.dto.response.GameResponse;
import de.chesscoach.dto.response.MoveResponse;
import de.chesscoach.dto.response.ScoreDto;
import de.chesscoach.engine.EngineRegistry;
import org.springframework.stereotype.Component;

import java.util.List;

/** Übersetzt interne Modelle in die versionierten API-DTOs; interne Java-Objekte werden nie direkt ausgegeben. */
@Component
public class ApiMapper {

    public static final String API_VERSION = "1";

    private final EngineRegistry engines;

    public ApiMapper(EngineRegistry engines) {
        this.engines = engines;
    }

    public GameResponse game(GameSnapshot s) {
        List<GameResponse.MoveDto> moves = s.moves().stream().map(m -> move(s, m)).toList();
        List<GameResponse.LegalMoveDto> legal = s.legalMoves().stream()
                .map(m -> new GameResponse.LegalMoveDto(m.uci(), m.san(), m.from(), m.to(),
                        m.promotion() == null ? null : String.valueOf(m.promotion().fenChar())))
                .toList();
        GameResponse.LastMove last = s.moves().isEmpty() ? null : new GameResponse.LastMove(
                s.moves().getLast().uci().substring(0, 2), s.moves().getLast().uci().substring(2, 4));
        String checkSquare = null;
        if (s.inCheck()) {
            int king = de.chesscoach.domain.position.Position.fromFen(s.fen()).kingSquare(s.sideToMove());
            checkSquare = Squares.name(king);
        }
        boolean engineReady = engines.play().getEngineInfo().available();
        return new GameResponse(API_VERSION, s.gameId(), s.status().name(), statusText(s), result(s),
                s.winner() == null ? null : s.winner().name(), s.playerColor().name(), s.sideToMove().name(), s.fen(),
                s.initialFen(), s.positionVersion(), s.difficulty().name(), s.analysisProfile().name(), s.settingsVersion(),
                s.inCheck(), checkSquare, last, moves, legal, s.canUndo(), s.canRedo(), s.engineThinking(),
                engineReady ? "READY" : "UNAVAILABLE", s.createdAt(), s.updatedAt());
    }

    private GameResponse.MoveDto move(GameSnapshot s, MoveRecord m) {
        AnalysisStatus status = s.analysisStatus().get(m.moveId());
        return new GameResponse.MoveDto(m.ply(), m.moveId(), m.uci(), m.san(), m.uci().substring(0, 2), m.uci().substring(2, 4),
                m.player().name(), m.byUser(), m.fenBefore(), m.fenAfter(), m.positionVersion(),
                status == null ? null : status.name(), s.analysisOutdated().contains(m.moveId()), m.timestamp());
    }

    static String statusText(GameSnapshot s) {
        GameStatus status = s.status();
        return switch (status) {
            case IN_PROGRESS -> s.inCheck() ? "Schach! " + colorName(s.sideToMove()) + " ist am Zug." : colorName(s.sideToMove()) + " ist am Zug.";
            case CHECKMATE -> "Schachmatt – " + colorName(s.winner()) + " gewinnt.";
            case RESIGNED -> "Partie aufgegeben – " + colorName(s.winner()) + " gewinnt.";
            default -> status.germanDescription() + ".";
        };
    }

    private static String colorName(Color color) {
        return color == null ? "" : color.germanName();
    }

    static String result(GameSnapshot s) {
        if (s.status().isDraw()) {
            return "1/2-1/2";
        }
        if (s.winner() != null) {
            return s.winner() == Color.WHITE ? "1-0" : "0-1";
        }
        return "*";
    }

    public MoveResponse move(MoveService.MoveResult result) {
        GameSnapshot g = result.game();
        AnalysisStatus status = g.analysisStatus().get(result.move().moveId());
        return new MoveResponse(g.gameId(), g.positionVersion(), result.move().ply(), result.move().uci(), result.move().san(),
                g.fen(), g.status().name(), status == null ? null : status.name(), result.replayed(), game(g));
    }

    public AnalysisResponse analysis(String gameId, AnalysisQueryService.AnalysisView view) {
        AnalysisEntry e = view.entry();
        MoveRecord m = view.move();
        MoveAnalysis a = e.status() == AnalysisStatus.COMPLETED ? e.result() : null;
        if (a == null) {
            return new AnalysisResponse(API_VERSION, gameId, m.ply(), m.moveId(), m.san(), m.uci(), m.fenBefore(), m.fenAfter(),
                    m.positionVersion(), view.positionVersion(), e.status().name(), e.stage(), e.progressPercent(), e.errorMessage(),
                    view.outdated(), e.settingsVersion(), e.depthLevel(), e.requestedAt(), e.completedAt(),
                    null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                    null, null, null, null, null, null, null, null, null, null, null, null);
        }
        CandidateEvaluation best = a.bestMove();
        boolean reliable = a.evaluationLoss() != null && a.confidence() != Confidence.LOW
                && a.classification() != MoveClassification.UNCERTAIN;
        String bestExplanation = a.sections().stream().filter(sec -> sec.key().equals("whyBetter")).findFirst()
                .flatMap(sec -> sec.statements().stream().findFirst()).map(st -> st.text()).orElse(null);
        return new AnalysisResponse(API_VERSION, gameId, m.ply(), m.moveId(), m.san(), m.uci(), m.fenBefore(), m.fenAfter(),
                m.positionVersion(), view.positionVersion(), e.status().name(), e.stage(), e.progressPercent(), null,
                view.outdated(), e.settingsVersion(), e.depthLevel(), e.requestedAt(), e.completedAt(),
                a.classification().name(), a.classification().german(), a.classificationProvisional(), a.classificationReasons(),
                a.explanationType().name(), a.explanationType().german(), a.headline(), a.mainReason(), a.mainReasonSources(),
                ScoreDto.of(a.scoreBefore()), ScoreDto.of(a.playedMove().score()), ScoreDto.of(best.score()),
                new AnalysisResponse.EvaluationLoss(a.evaluationLoss(), a.winProbabilityLoss(), reliable),
                best.san(), best.uci(), bestExplanation, best.line().stream().map(v -> v.san()).toList(),
                a.candidates().stream().map(c -> candidate(c, a)).toList(),
                a.findings().stream().map(ApiMapper::finding).toList(),
                a.strategicPlans().stream().map(p -> new AnalysisResponse.PlanDto(p.side(), p.title(), p.explanation(), p.squares(),
                        p.basis(), p.source().name())).toList(),
                a.sections().stream().map(sec -> new AnalysisResponse.SectionDto(sec.key(), sec.question(),
                        sec.statements().stream().map(st -> new AnalysisResponse.StatementDto(st.text(), st.sources())).toList())).toList(),
                new AnalysisResponse.TipDto(a.tip().title(), a.tip().observation(), a.tip().why(), a.tip().betterAlternative(),
                        a.tip().expectedConsequence(), a.tip().learningPrinciple(), a.tip().confidence().name()),
                a.learningTags(), new AnalysisResponse.Material(a.materialBefore(), a.materialAfter(), a.materialAfter() - a.materialBefore()),
                a.confidence().name(), a.confidenceReasons(), a.critical(),
                new AnalysisResponse.EngineDetails(a.engineName(), a.engineVersion(), a.searchLimits(), a.engineNodes(),
                        a.elapsedMillis(), a.rootDepth()),
                a.rulesVersion());
    }

    private static AnalysisResponse.CandidateDto candidate(CandidateEvaluation c, MoveAnalysis a) {
        List<String> motifs = c.findings().stream().filter(f -> f.category() == FindingCategory.TACTICAL && !f.overriddenByTactics())
                .map(CoachingFinding::title).distinct().toList();
        List<String> pros = c.findings().stream().filter(f -> f.category() == FindingCategory.STRATEGIC && f.polarity() == Polarity.POSITIVE)
                .map(CoachingFinding::title).distinct().toList();
        List<String> cons = c.findings().stream().filter(f -> f.category() == FindingCategory.STRATEGIC && f.polarity() == Polarity.NEGATIVE)
                .map(CoachingFinding::title).distinct().toList();
        String comparison;
        if (c.played()) {
            comparison = "Dein gespielter Zug.";
        } else if (c.deltaToPlayed() == null) {
            comparison = "Vergleich mit Mattbewertung: " + c.score().display() + " gegenüber " + a.playedMove().score().display() + ".";
        } else if (c.deltaToPlayed() > 0) {
            comparison = "Um " + c.deltaToPlayed() + " Centipawns besser als dein Zug.";
        } else if (c.deltaToPlayed() < 0) {
            comparison = "Um " + (-c.deltaToPlayed()) + " Centipawns schlechter als dein Zug.";
        } else {
            comparison = "Gleich bewertet wie dein Zug.";
        }
        return new AnalysisResponse.CandidateDto(c.variationId(), c.uci(), c.san(), ScoreDto.of(c.score()), c.depth(), c.nodes(),
                c.line().stream().map(v -> new AnalysisResponse.VariationMoveDto(v.uci(), v.san(), v.numbered(), v.fenAfter())).toList(),
                c.lineTruncated(), c.played(), c.best(), c.deltaToPlayed(), motifs, pros, cons,
                c.findings().stream().map(ApiMapper::finding).toList(), comparison, c.searchLine());
    }

    public static FindingDto finding(CoachingFinding f) {
        var ev = f.evidence();
        FindingDto.EvidenceDto evidence = ev == null ? null : new FindingDto.EvidenceDto(ev.startFen(), ev.triggerMove(), ev.line(),
                ev.materialBefore(), ev.materialAfter(), ev.outcome(), ev.certainty().name(), ev.confidence().name());
        int sign = f.polarity() == Polarity.POSITIVE ? 1 : f.polarity() == Polarity.NEGATIVE ? -1 : 0;
        return new FindingDto(f.ruleId(), f.category().name(), f.source().name(), f.severity().name(), f.polarity().name(),
                f.certainty().name(), f.title(), f.explanation(), f.affectedSquares(), f.affectedPieces(), evidence,
                f.relatedVariationIds(), f.learningPrinciple(), f.overriddenByTactics(), sign * f.severity().ordinal());
    }
}
