package de.chesscoach.coaching.tactical;

import de.chesscoach.coaching.AbstractRule;
import de.chesscoach.coaching.EngineLine;
import de.chesscoach.coaching.FindingBuilder;
import de.chesscoach.coaching.GermanText;
import de.chesscoach.coaching.LinePly;
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
 * TACT-002 Materialverlust: Der Zug ermöglicht einen relevanten Materialverlust, der mit dem besten Zug
 * vermeidbar gewesen wäre. Ein Materialopfer mit Kompensation laut Engine wird nicht als Fehler gewertet.
 */
@Component
public class MaterialLossRule extends AbstractRule implements TacticalRule {

    public MaterialLossRule() {
        super("TACT-002", "Materialverlust",
                "Erkennt vermeidbaren Materialverlust anhand der Engine-Variante des gespielten Zuges.",
                List.of("Materialbilanz vor dem Zug", "Materialbilanz am ruhigen Punkt der Variante",
                        "Materialbilanz der Variante des besten Zuges", "Bewertung vor und nach dem Zug"),
                "Frage dich vor jedem Zug: Welche Figuren stehen nach meinem Zug angegriffen, und sind sie ausreichend gedeckt?");
    }

    @Override
    public List<CoachingFinding> evaluate(PositionContext c) {
        List<CoachingFinding> findings = new ArrayList<>();
        int before = c.before().materialBalance(c.user());
        if (!c.hasEngineLine()) {
            int see = StaticExchange.bestCaptureGain(c.after(), c.move().to(), c.opponent());
            if (see >= 200 && c.movedPiece() != null) {
                findings.add(FindingBuilder.of(this)
                        .negative().heuristic().severity(Severity.MEDIUM)
                        .explanation(GermanText.yourPieceOn(c.after(), c.move().to())
                                + " kann geschlagen werden, und du kannst nicht gleichwertig zurückschlagen. "
                                + "Der Computer hat das hier nicht nachgerechnet.")
                        .piece(c.after(), c.move().to())
                        .build());
            }
            return findings;
        }
        EngineLine line = c.line();
        int settled = line.settledMaterialBalance(c.user(), MATERIAL_HORIZON);
        int loss = before - settled;
        if (loss < 1) {
            return findings;
        }
        Integer bestSettled = c.bestLine().filter(l -> l.size() > 1)
                .map(l -> l.settledMaterialBalance(c.user(), MATERIAL_HORIZON)).orElse(null);
        // Ist der Zug selbst der beste gefundene Zug, ist der Materialverlust unvermeidbar bzw. ein bewusstes Opfer.
        boolean avoidable = c.bestMove().isEmpty() || c.differsFromBest()
                && (bestSettled == null || bestSettled >= settled + 1);
        if (!avoidable) {
            return findings;
        }
        int end = Math.max(line.settledIndex(MATERIAL_HORIZON), 2);
        List<Integer> captureSquares = new ArrayList<>();
        for (int i = 1; i < Math.min(end, line.size()); i++) {
            LinePly ply = line.ply(i);
            if (ply.mover() == c.opponent() && ply.isCapture()) {
                captureSquares.add(ply.move().to());
            }
        }
        int scoreDrop = c.lossCentipawns().orElse(Integer.MAX_VALUE);
        boolean compensated = c.scoreAfter().isPresent() && c.scoreBefore().isPresent()
                && c.scoreAfter().get().centipawnsClamped(3000) >= c.scoreBefore().get().centipawnsClamped(3000) - 60
                && scoreDrop < 80;
        if (compensated) {
            findings.add(FindingBuilder.of(this)
                    .neutral().verified().severity(Severity.LOW)
                    .title("Materialopfer mit Kompensation")
                    .explanation("Du gibst hier " + GermanText.material(loss) + " ab – aber das ist in Ordnung: "
                            + "Laut Computer bekommst du dafür genug Ausgleich, zum Beispiel aktivere Figuren oder Angriff. "
                            + "Man nennt das ein Opfer. (Berechnet: " + line.numbered(0, end) + ")")
                    .squares(captureSquares)
                    .lineEvidence(line, end, before, settled, "Materialopfer mit Kompensation", Confidence.MEDIUM)
                    .build());
            return findings;
        }
        if (loss < 2 && scoreDrop < 60) {
            return findings;
        }
        Severity severity = loss >= 3 ? Severity.HIGH : loss == 2 ? Severity.MEDIUM : Severity.LOW;
        if (loss >= 5) {
            severity = Severity.CRITICAL;
        }
        String reply = c.opponentReply().map(LinePly::numberedSan).orElse("");
        findings.add(FindingBuilder.of(this)
                .negative().verified().severity(severity)
                .explanation("Nach deinem Zug verlierst du " + GermanText.material(loss) + ". "
                        + c.opponentReply().map(r -> "Der Gegner antwortet: " + GermanText.moveWithNotation(r) + ". ").orElse("")
                        + GermanText.tradeWords(line, c.user(), end)
                        + (bestSettled != null && c.differsFromBest() && bestSettled >= before
                        ? " Mit dem besten Zug hättest du kein Material verloren." : ""))
                .squares(captureSquares)
                .lineEvidence(line, end, before, settled, "Materialverlust", Confidence.HIGH)
                .build());
        return findings;
    }
}
