package de.chesscoach.coaching.tactical;

import de.chesscoach.coaching.AbstractRule;
import de.chesscoach.coaching.EngineLine;
import de.chesscoach.coaching.FindingBuilder;
import de.chesscoach.coaching.GermanText;
import de.chesscoach.coaching.PositionContext;
import de.chesscoach.coaching.TacticalRule;
import de.chesscoach.domain.coaching.CoachingFinding;
import de.chesscoach.domain.coaching.Confidence;
import de.chesscoach.domain.coaching.Severity;
import de.chesscoach.domain.position.StaticExchange;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * TACT-001 Materialgewinn: Der Zug gewinnt laut geprüfter Variante Material – oder der beste Zug hätte
 * Material gewonnen, der gespielte nicht.
 */
@Component
public class MaterialGainRule extends AbstractRule implements TacticalRule {

    public MaterialGainRule() {
        super("TACT-001", "Materialgewinn",
                "Erkennt Materialgewinn durch den Zug bzw. verpassten Materialgewinn im Vergleich zum besten Zug.",
                List.of("Materialbilanz vor dem Zug", "Materialbilanz am ruhigen Punkt der Engine-Variante",
                        "Variante des besten Zuges", "statische Abtauschbewertung des Zielfelds"),
                "Prüfe vor jedem Zug, ob der Gegner Figuren ungedeckt gelassen hat – und ob du sie wirklich sicher schlagen kannst.");
    }

    @Override
    public List<CoachingFinding> evaluate(PositionContext c) {
        List<CoachingFinding> findings = new ArrayList<>();
        int before = c.before().materialBalance(c.user());
        EngineLine line = c.line();
        int gain = 0;
        if (c.hasEngineLine()) {
            int settled = line.settledMaterialBalance(c.user(), MATERIAL_HORIZON);
            gain = settled - before;
            boolean scoreSupports = c.scoreAfter().isEmpty() || c.scoreBefore().isEmpty()
                    || c.scoreAfter().get().centipawnsClamped(3000) >= c.scoreBefore().get().centipawnsClamped(3000) - 100;
            if (gain >= 1 && scoreSupports) {
                int end = line.settledIndex(MATERIAL_HORIZON);
                findings.add(FindingBuilder.of(this)
                        .positive().verified()
                        .severity(gain >= 3 ? Severity.HIGH : Severity.MEDIUM)
                        .explanation("Gut gesehen: Mit diesem Zug gewinnst du " + GermanText.material(gain) + ". "
                                + GermanText.tradeWords(line, c.user(), Math.max(end, 1))
                                + " So rechnet der Computer weiter: " + GermanText.lineWords(line, 0, Math.min(Math.max(end, 1), 4), c.user())
                                + " (" + line.numbered(0, Math.max(end, 1)) + ").")
                        .square(c.move().to())
                        .lineEvidence(line, Math.max(end, 1), before, settled, "Materialgewinn", Confidence.HIGH)
                        .build());
            }
        } else if (c.capturedPiece() != null) {
            int see = StaticExchange.evaluate(c.before(), c.move().to(), c.user());
            if (see >= 100) {
                findings.add(FindingBuilder.of(this)
                        .positive().heuristic().severity(Severity.LOW)
                        .explanation("Du schlägst " + GermanText.opponentPieceAcc(c.before(), c.move().to())
                                + ". Nach einfachem Durchzählen der Figuren, die auf dieses Feld schlagen können, behältst du "
                                + "danach mehr Material. Der Computer hat das hier nicht nachgerechnet.")
                        .piece(c.before(), c.move().to())
                        .build());
            }
        }

        if (c.isPlayedMove() && c.differsFromBest() && c.bestLine().isPresent() && c.bestLine().get().size() > 1) {
            EngineLine best = c.bestLine().get();
            int bestSettled = best.settledMaterialBalance(c.user(), MATERIAL_HORIZON);
            int bestGain = bestSettled - before;
            if (bestGain >= 2 && bestGain >= gain + 2) {
                int end = best.settledIndex(MATERIAL_HORIZON);
                findings.add(FindingBuilder.of(this)
                        .negative().verified()
                        .title("Materialgewinn verpasst")
                        .severity(bestGain >= 3 ? Severity.HIGH : Severity.MEDIUM)
                        .explanation("Hier war mehr drin: Mit " + GermanText.moveWithNotation(best.ply(0)) + " hättest du "
                                + GermanText.material(bestGain) + " gewinnen können. " + GermanText.tradeWords(best, c.user(), Math.max(end, 1))
                                + " So hätte es weitergehen können: " + GermanText.lineWords(best, 0, Math.min(Math.max(end, 1), 4), c.user()) + ".")
                        .square(best.ply(0).move().to())
                        .lineEvidence(best, Math.max(end, 1), before, bestSettled, "verpasster Materialgewinn", Confidence.HIGH)
                        .build());
            }
        }
        return findings;
    }
}
