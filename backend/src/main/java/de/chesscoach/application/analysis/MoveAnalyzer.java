package de.chesscoach.application.analysis;

import de.chesscoach.chess.ChessPositionAdapter;
import de.chesscoach.chess.ChessRulesService;
import de.chesscoach.chess.LegalMove;
import de.chesscoach.coaching.CoachingProperties;
import de.chesscoach.coaching.EngineLine;
import de.chesscoach.coaching.ExplanationBuilder;
import de.chesscoach.coaching.LinePly;
import de.chesscoach.coaching.MistakeClassifier;
import de.chesscoach.coaching.MoveExplanation;
import de.chesscoach.coaching.PositionContext;
import de.chesscoach.coaching.StrategicRuleEngine;
import de.chesscoach.coaching.TacticalRuleEngine;
import de.chesscoach.domain.analysis.CandidateEvaluation;
import de.chesscoach.domain.analysis.MoveAnalysis;
import de.chesscoach.domain.analysis.Score;
import de.chesscoach.domain.analysis.VariationMove;
import de.chesscoach.domain.coaching.CoachingFinding;
import de.chesscoach.domain.coaching.Confidence;
import de.chesscoach.domain.coaching.StrategicPlan;
import de.chesscoach.domain.game.AnalysisProfile;
import de.chesscoach.domain.game.GameStatus;
import de.chesscoach.domain.game.MoveRecord;
import de.chesscoach.domain.position.MoveInfo;
import de.chesscoach.domain.position.Position;
import de.chesscoach.engine.AnalysisLimits;
import de.chesscoach.engine.ChessEngine;
import de.chesscoach.engine.EngineAnalysis;
import de.chesscoach.infrastructure.config.AnalysisProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;

/**
 * Analyse-Pipeline für einen Benutzerzug (Requirements 6.1):
 * <ol>
 *     <li>P<sub>0</sub>: Bewertung der Stellung vor dem Zug.</li>
 *     <li>Vorauswahl: Alle legalen Züge werden mit kleinem Knotenlimit über ihre Folgestellung bewertet.</li>
 *     <li>Kandidaten (inklusive gespieltem Zug P<sub>u</sub> und bestem Zug P<sub>b</sub>) werden unter identischen
 *     Suchbedingungen bewertet; daraus ergibt sich der Verlust L = E(P<sub>b</sub>) − E(P<sub>u</sub>).</li>
 *     <li>Varianten werden auf Legalität geprüft und in SAN übersetzt.</li>
 *     <li>Coaching-Ebenen 1–3: taktische und strategische Regeln, Klassifizierung, Erklärung.</li>
 * </ol>
 * Alle Bewertungen sind aus Sicht des Benutzers normalisiert.
 */
@Component
public class MoveAnalyzer {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MoveAnalyzer.class);

    private final ChessRulesService rules;
    private final TacticalRuleEngine tacticalRules;
    private final StrategicRuleEngine strategicRules;
    private final MistakeClassifier classifier;
    private final ExplanationBuilder explanationBuilder;
    private final AnalysisProperties properties;
    private final CoachingProperties coachingProperties;

    public MoveAnalyzer(ChessRulesService rules, TacticalRuleEngine tacticalRules, StrategicRuleEngine strategicRules,
                        MistakeClassifier classifier, ExplanationBuilder explanationBuilder, AnalysisProperties properties,
                        CoachingProperties coachingProperties) {
        this.rules = rules;
        this.tacticalRules = tacticalRules;
        this.strategicRules = strategicRules;
        this.classifier = classifier;
        this.explanationBuilder = explanationBuilder;
        this.properties = properties;
        this.coachingProperties = coachingProperties;
    }

    /**
     * @param fenBefore            Stellung vor dem Benutzerzug
     * @param playedUci            gespielter Zug
     * @param previousOpponentMove letzter Zug des Gegners vor dem Benutzerzug (oder null)
     * @param previousOwnMove      vorheriger eigener Zug (oder null)
     * @param profile              Analyseprofil
     * @param depthLevel           1 = Standardanalyse, 2 = vertiefte Analyse
     */
    public record Request(String fenBefore, String playedUci, MoveRecord previousOpponentMove, MoveRecord previousOwnMove,
                          AnalysisProfile profile, int depthLevel) {
    }

    private record Evaluated(MoveInfo move, Score score, EngineAnalysis analysis, boolean terminal) {
        int depth() {
            return analysis == null ? (terminal ? 99 : 0) : analysis.depth();
        }

        long nodes() {
            return analysis == null ? 0 : analysis.nodes();
        }

        List<String> continuation() {
            return analysis == null ? List.of() : analysis.principalVariation();
        }

        boolean completed() {
            return terminal || (analysis != null && analysis.completed() && analysis.score() != null);
        }
    }

    /** Höchstzahl zusätzlicher Suchen bei der Nachprüfung einer knappen Entscheidung. */
    private static final int MAX_VERIFICATIONS = 5;

    /**
     * Nachprüfung knapper Entscheidungen: Will der Coach einen anderen als den gespielten Zug empfehlen und ist der
     * Abstand klein, werden beide mit größerem Budget erneut bewertet – so lange, bis der Zug, der am Ende vorn liegt,
     * unter denselben Bedingungen geprüft ist wie der gespielte Zug. Flache Suchen übersehen sonst gelegentlich eine
     * Widerlegung und empfehlen einen schwächeren Zug; ein nur flach bewerteter Zug darf den Vergleich nicht gewinnen.
     *
     * @return zusätzlich verbrauchte Knoten
     */
    private long verifyCloseCall(ChessEngine engine, ChessPositionAdapter root, List<MoveInfo> legal, MoveInfo played,
                                 Map<String, Evaluated> deep, double candidateFactor, CancellationToken token,
                                 BiConsumer<String, Integer> progress) {
        if (properties.verifyFactor() <= 1.0 || deep.get(played.uci()).terminal()) {
            return 0;
        }
        AnalysisLimits verifyLimits = AnalysisLimits.nodes(scaled(properties.candidateNodes(), candidateFactor * properties.verifyFactor()));
        Set<String> verified = new LinkedHashSet<>();
        long nodes = 0;
        while (verified.size() < MAX_VERIFICATIONS) {
            Evaluated playedEval = deep.get(played.uci());
            Evaluated best = deep.values().stream().max(Comparator.comparingInt(e -> e.score().comparable())).orElseThrow();
            if (best.move().uci().equals(played.uci()) || playedEval.score().comparable() >= best.score().comparable()
                    || best.terminal()
                    || best.score().centipawnsClamped(3000) - playedEval.score().centipawnsClamped(3000) > properties.verifyMargin()) {
                break;
            }
            String next = !verified.contains(played.uci()) ? played.uci() : best.move().uci();
            if (verified.contains(next)) {
                break; // Beide unter gleichen Bedingungen geprüft: Die Empfehlung steht.
            }
            token.throwIfCancelled();
            progress.accept("Knappe Entscheidung wird genauer geprüft (" + (verified.size() + 1) + ")", 85 + verified.size());
            MoveInfo move = legal.stream().filter(m -> m.uci().equals(next)).findFirst().orElseThrow();
            Evaluated e = evaluateChild(engine, root, move, verifyLimits);
            nodes += e.nodes();
            deep.put(next, e);
            verified.add(next);
        }
        return nodes;
    }

    public MoveAnalysis analyze(ChessEngine engine, Request request, CancellationToken token, BiConsumer<String, Integer> progress) {
        long started = System.currentTimeMillis();
        ChessPositionAdapter root = rules.fromFen(request.fenBefore());
        Position rootPosition = root.position();
        List<MoveInfo> legal = root.legalMoves().stream().map(ChessRulesService::toMoveInfo).toList();
        MoveInfo played = legal.stream().filter(m -> m.uci().equals(request.playedUci())).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Der zu analysierende Zug ist nicht legal."));
        double factor = request.profile().factor() * (request.depthLevel() > 1 ? properties.deepenFactor() : 1.0);
        int maxCandidates = properties.maxCandidates() + (request.depthLevel() > 1 ? 2 : 0);
        long totalNodes = 0;

        progress.accept("Bewertung der Stellung vor dem Zug", 5);
        token.throwIfCancelled();
        EngineAnalysis rootAnalysis = engine.analyze(request.fenBefore(), AnalysisLimits.nodes(scaled(properties.rootNodes(), factor)));
        totalNodes += rootAnalysis.nodes();
        Score scoreBefore = rootAnalysis.score();

        // Vorauswahl aller legalen Züge.
        Map<String, Evaluated> screening = new LinkedHashMap<>();
        if (legal.size() > 1) {
            AnalysisLimits screenLimits = AnalysisLimits.nodes(scaled(properties.screeningNodes(), Math.max(1.0, factor / 2)));
            int i = 0;
            for (MoveInfo move : legal) {
                token.throwIfCancelled();
                progress.accept("Vorauswahl der Kandidatenzüge (" + (i + 1) + "/" + legal.size() + ")", 10 + 40 * i / legal.size());
                try {
                    Evaluated e = evaluateChild(engine, root, move, screenLimits);
                    totalNodes += e.nodes();
                    screening.put(move.uci(), e);
                } catch (de.chesscoach.engine.EngineProtocolException e) {
                    // Ein nicht bewertbarer Zug wird in der Vorauswahl ausgelassen und nicht als Kandidat angezeigt.
                    log.debug("Vorauswahl ohne Bewertung für {}", move.uci());
                }
                i++;
            }
        }

        Set<String> selection = new LinkedHashSet<>();
        if (rootAnalysis.bestMove() != null) {
            selection.add(rootAnalysis.bestMove());
        }
        selection.add(played.uci());
        screening.values().stream()
                .sorted(Comparator.comparingInt((Evaluated e) -> e.score().comparable()).reversed())
                .limit(maxCandidates)
                .forEach(e -> selection.add(e.move().uci()));

        boolean critical = isCritical(legal, selection, screening, scoreBefore, played);
        double candidateFactor = factor * (critical ? properties.criticalFactor() : 1.0);
        AnalysisLimits candidateLimits = AnalysisLimits.nodes(scaled(properties.candidateNodes(), candidateFactor));

        Map<String, Evaluated> deep = new LinkedHashMap<>();
        int index = 0;
        for (String uci : selection) {
            token.throwIfCancelled();
            progress.accept("Bewertung der Kandidaten (" + (index + 1) + "/" + selection.size() + ")", 50 + 35 * index / selection.size());
            MoveInfo move = legal.stream().filter(m -> m.uci().equals(uci)).findFirst().orElseThrow();
            Evaluated e = evaluateChild(engine, root, move, candidateLimits);
            totalNodes += e.nodes();
            deep.put(uci, e);
            index++;
        }
        token.throwIfCancelled();
        totalNodes += verifyCloseCall(engine, root, legal, played, deep, candidateFactor, token, progress);
        progress.accept("Coaching-Regeln werden ausgewertet", 88);

        Evaluated playedEval = deep.get(played.uci());
        Evaluated bestEval = deep.values().stream().max(Comparator.comparingInt(e -> e.score().comparable())).orElseThrow();
        boolean playedIsBest = bestEval.move().uci().equals(played.uci())
                || playedEval.score().comparable() >= bestEval.score().comparable();
        if (playedIsBest) {
            bestEval = playedEval;
        }
        Score bestScore = bestEval.score();

        Map<String, EngineLine> lines = new LinkedHashMap<>();
        Map<String, Boolean> truncated = new LinkedHashMap<>();
        for (Evaluated e : deep.values()) {
            List<String> uciLine = new ArrayList<>();
            uciLine.add(e.move().uci());
            uciLine.addAll(e.continuation());
            ChessRulesService.ValidatedLine validated = rules.validateLine(request.fenBefore(), uciLine, properties.maxLinePlies());
            String variationId = "v-" + e.move().uci();
            List<MoveInfo> moves = validated.moves().stream().map(ChessRulesService::toMoveInfo).toList();
            lines.put(e.move().uci(), EngineLine.of(variationId, rootPosition, moves, e.analysis() != null));
            truncated.put(e.move().uci(), validated.truncated());
        }

        List<String> confidenceReasons = new ArrayList<>();
        Confidence confidence = assessConfidence(deep.values(), rootAnalysis, bestEval, critical, request.depthLevel(), confidenceReasons);

        LinePly previousOpponent = toPly(request.previousOpponentMove());
        LinePly previousOwn = toPly(request.previousOwnMove());
        EngineLine bestLine = lines.get(bestEval.move().uci());
        PositionContext playedContext = context(rootPosition, played, lines.get(played.uci()), scoreBefore, playedEval.score(),
                bestScore, bestLine, previousOpponent, previousOwn, true);
        List<CoachingFinding> playedFindings = findings(playedContext);

        Optional<PositionContext> bestContext = Optional.empty();
        List<CoachingFinding> bestFindings = List.of();
        if (!playedIsBest) {
            PositionContext bc = context(rootPosition, bestEval.move(), bestLine, scoreBefore, bestScore, bestScore, bestLine,
                    previousOpponent, previousOwn, false);
            bestContext = Optional.of(bc);
            bestFindings = findings(bc);
        }

        Score secondBest = deep.values().stream()
                .filter(e -> !e.move().uci().equals(played.uci()))
                .map(Evaluated::score)
                .max(Comparator.comparingInt(Score::comparable))
                .orElse(null);
        MistakeClassifier.Result classification = classifier.classify(new MistakeClassifier.Input(scoreBefore, playedEval.score(),
                bestScore, legal.size(), playedIsBest, playedIsBest ? secondBest : null, confidence));
        if (!playedEval.completed()) {
            classification = new MistakeClassifier.Result(de.chesscoach.domain.coaching.MoveClassification.UNCERTAIN,
                    classification.lossCentipawns(), classification.winProbabilityLoss(), true,
                    List.of("Die Suche für den gespielten Zug wurde nicht regulär abgeschlossen."));
        }

        List<StrategicPlan> plans = strategicRules.plans(playedContext);
        final Evaluated bestFinal = bestEval;
        List<String> alternatives = deep.values().stream()
                .filter(e -> !e.move().uci().equals(played.uci()) && !e.move().uci().equals(bestFinal.move().uci()))
                .filter(e -> bestScore.centipawnsClamped(3000) - e.score().centipawnsClamped(3000) <= 60)
                .sorted(Comparator.comparingInt((Evaluated e) -> e.score().comparable()).reversed())
                .limit(3)
                .map(e -> de.chesscoach.coaching.GermanText.moveWithNotation(lines.get(e.move().uci()).ply(0)))
                .toList();
        int minDepth = deep.values().stream().filter(e -> !e.terminal()).mapToInt(Evaluated::depth).min().orElse(0);
        MoveExplanation explanation = explanationBuilder.build(new ExplanationBuilder.Input(playedContext, playedFindings,
                bestContext, bestFindings, classification, confidence, confidenceReasons, plans, alternatives, minDepth));

        List<CandidateEvaluation> candidates = new ArrayList<>();
        for (Evaluated e : deep.values()) {
            boolean isPlayed = e.move().uci().equals(played.uci());
            boolean isBest = e.move().uci().equals(bestEval.move().uci());
            List<CoachingFinding> candidateFindings;
            if (isPlayed) {
                candidateFindings = explanation.findings();
            } else if (isBest) {
                candidateFindings = bestFindings;
            } else {
                candidateFindings = findings(context(rootPosition, e.move(), lines.get(e.move().uci()), scoreBefore, e.score(),
                        bestScore, bestLine, previousOpponent, previousOwn, false));
            }
            candidates.add(toCandidate(e, lines.get(e.move().uci()), truncated.get(e.move().uci()), isPlayed, isBest,
                    playedEval.score(), candidateFindings));
        }
        candidates.sort(Comparator.comparingInt((CandidateEvaluation ce) -> ce.score().comparable()).reversed());
        CandidateEvaluation playedCandidate = candidates.stream().filter(CandidateEvaluation::played).findFirst().orElseThrow();
        CandidateEvaluation bestCandidate = candidates.stream().filter(CandidateEvaluation::best).findFirst().orElse(playedCandidate);

        EngineLine playedLine = lines.get(played.uci());
        int materialBefore = rootPosition.materialBalance(rootPosition.sideToMove());
        int materialAfter = playedLine.size() > 1
                ? playedLine.settledMaterialBalance(rootPosition.sideToMove(), 12)
                : playedLine.positionAfter(1).materialBalance(rootPosition.sideToMove());

        return new MoveAnalysis(UUID.randomUUID().toString(), request.fenBefore(), playedLine.positionAfter(1).toFen(),
                playedCandidate, bestCandidate, candidates, scoreBefore, rootAnalysis.depth(),
                classification.classification(), classification.provisional(), classification.reasons(),
                classification.lossCentipawns(), classification.winProbabilityLoss(), explanation.type(), explanation.headline(),
                explanation.mainReason(), explanation.mainReasonSources(), explanation.sections(), explanation.tip(),
                explanation.findings(), plans, explanation.learningTags(), materialBefore, materialAfter, confidence,
                confidenceReasons, critical, request.depthLevel(), rootAnalysis.engineName(), rootAnalysis.engineVersion(),
                "Ausgangsstellung: " + scaled(properties.rootNodes(), factor) + " Knoten; Kandidaten: " + candidateLimits.describe()
                        + " je Zug" + (critical ? " (erhöht wegen taktisch kritischer Stellung)" : ""),
                totalNodes, System.currentTimeMillis() - started, coachingProperties.rulesVersion());
    }

    private List<CoachingFinding> findings(PositionContext context) {
        List<CoachingFinding> result = new ArrayList<>(tacticalRules.evaluate(context));
        result.addAll(strategicRules.evaluate(context));
        return result;
    }

    private PositionContext context(Position root, MoveInfo move, EngineLine line, Score before, Score after, Score best,
                                    EngineLine bestLine, LinePly previousOpponent, LinePly previousOwn, boolean played) {
        return PositionContext.builder(root, move, rules)
                .line(line)
                .scores(before, after, best)
                .bestLine(bestLine)
                .previousOpponentMove(previousOpponent)
                .previousOwnMove(previousOwn)
                .playedMove(played)
                .build();
    }

    private LinePly toPly(MoveRecord record) {
        if (record == null) {
            return null;
        }
        LegalMove legalMove = rules.fromFen(record.fenBefore()).describe(record.uci());
        EngineLine line = EngineLine.of("prev", Position.fromFen(record.fenBefore()), List.of(ChessRulesService.toMoveInfo(legalMove)), false);
        return line.ply(0);
    }

    private Evaluated evaluateChild(ChessEngine engine, ChessPositionAdapter root, MoveInfo move, AnalysisLimits limits) {
        ChessPositionAdapter child = rules.fromFen(root.fen()).play(move.uci());
        GameStatus status = child.status();
        if (status == GameStatus.CHECKMATE) {
            return new Evaluated(move, Score.mate(1), null, true);
        }
        if (status == GameStatus.STALEMATE || status == GameStatus.DRAW_INSUFFICIENT_MATERIAL
                || status == GameStatus.DRAW_FIFTY_MOVE_RULE) {
            return new Evaluated(move, Score.cp(0), null, true);
        }
        EngineAnalysis analysis = engine.analyze(child.fen(), limits);
        if (analysis.score() == null) {
            // Sehr kurze Suchen enden gelegentlich ohne exakte Bewertung: einmal mit mehr Knoten wiederholen.
            log.debug("Keine exakte Bewertung für {} – Suche wird mit größerem Limit wiederholt", move.uci());
            analysis = engine.analyze(child.fen(), limits.scaled(4.0));
        }
        if (analysis.score() == null) {
            throw new de.chesscoach.engine.EngineProtocolException("Die Engine hat keine Bewertung geliefert.");
        }
        return new Evaluated(move, Score.fromChildPerspective(analysis.score()), analysis, false);
    }

    private boolean isCritical(List<MoveInfo> legal, Set<String> selection, Map<String, Evaluated> screening, Score before, MoveInfo played) {
        if (before != null && before.isMate()) {
            return true;
        }
        boolean forcingCandidates = legal.stream().filter(m -> selection.contains(m.uci())).anyMatch(m -> m.capture() || m.check());
        int spread = 0;
        if (!screening.isEmpty()) {
            int best = screening.values().stream().mapToInt(e -> e.score().centipawnsClamped(3000)).max().orElse(0);
            Evaluated playedScreen = screening.get(played.uci());
            spread = playedScreen == null ? 0 : best - playedScreen.score().centipawnsClamped(3000);
        }
        return forcingCandidates || spread >= 150;
    }

    private Confidence assessConfidence(java.util.Collection<Evaluated> evaluated, EngineAnalysis root, Evaluated best, boolean critical,
                                        int depthLevel, List<String> reasons) {
        Confidence confidence = Confidence.HIGH;
        int minDepth = evaluated.stream().filter(e -> !e.terminal()).mapToInt(Evaluated::depth).min().orElse(99);
        boolean incomplete = evaluated.stream().anyMatch(e -> !e.completed()) || !root.completed();
        if (incomplete) {
            reasons.add("mindestens eine Suche wurde nicht regulär abgeschlossen");
            confidence = Confidence.LOW;
        }
        if (minDepth < properties.minReliableDepth() / 2) {
            reasons.add("sehr geringe Suchtiefe (" + minDepth + ")");
            confidence = Confidence.LOW;
        } else if (minDepth < properties.minReliableDepth()) {
            reasons.add("Suchtiefe " + minDepth + " unter der Zuverlässigkeitsgrenze " + properties.minReliableDepth());
            confidence = min(confidence, Confidence.MEDIUM);
        }
        if (root.score() != null && !root.score().isMate() && !best.score().isMate()
                && Math.abs(root.score().value() - best.score().value()) > 150) {
            reasons.add("Bewertungen der Ausgangsstellung und des besten Kandidaten weichen deutlich voneinander ab");
            confidence = min(confidence, Confidence.MEDIUM);
        }
        if (critical && depthLevel == 1 && confidence == Confidence.HIGH && minDepth < properties.minReliableDepth() + 3) {
            reasons.add("taktisch kritische Stellung – eine vertiefte Analyse kann die Bewertung noch verändern");
            confidence = Confidence.MEDIUM;
        }
        return confidence;
    }

    private static Confidence min(Confidence a, Confidence b) {
        return a.ordinal() >= b.ordinal() ? a : b;
    }

    private CandidateEvaluation toCandidate(Evaluated e, EngineLine line, boolean truncated, boolean played, boolean best,
                                            Score playedScore, List<CoachingFinding> findings) {
        List<VariationMove> moves = new ArrayList<>();
        for (LinePly ply : line.plies()) {
            moves.add(new VariationMove(ply.move().uci(), ply.move().san(), ply.numberedSan(), ply.after().toFen()));
        }
        Integer delta = e.score().isMate() || playedScore.isMate() ? null
                : e.score().value() - playedScore.value();
        return new CandidateEvaluation(line.variationId(), e.move().uci(), e.move().san(), e.score(), e.depth(), e.nodes(),
                moves, truncated, played, best, delta, findings.stream().limit(8).toList(), true);
    }

    private static long scaled(long base, double factor) {
        return Math.max(100, Math.round(base * factor));
    }
}
