package de.chesscoach.coaching.tactical;

import de.chesscoach.coaching.AbstractRule;
import de.chesscoach.coaching.EngineLine;
import de.chesscoach.coaching.FindingBuilder;
import de.chesscoach.coaching.GermanText;
import de.chesscoach.coaching.LinePly;
import de.chesscoach.coaching.PositionContext;
import de.chesscoach.coaching.TacticalRule;
import de.chesscoach.coaching.TacticsToolkit;
import de.chesscoach.domain.coaching.CoachingFinding;
import de.chesscoach.domain.coaching.Confidence;
import de.chesscoach.domain.coaching.Severity;
import de.chesscoach.domain.position.MoveInfo;
import de.chesscoach.domain.position.Position;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * TACT-009 Zwischenzug: Nach einem gegnerischen Schlagzug wäre das Zurückschlagen naheliegend; ein Schach oder eine
 * starke Drohung dazwischen ist laut Engine aber mindestens gleichwertig (bzw. wurde verpasst).
 */
@Component
public class IntermezzoRule extends AbstractRule implements TacticalRule {

    private static final int TOLERANCE_CP = 30;
    private static final int MISSED_THRESHOLD_CP = 80;

    public IntermezzoRule() {
        super("TACT-009", "Zwischenzug",
                "Erkennt gespielte und verpasste Zwischenzüge nach einem gegnerischen Schlagzug.",
                List.of("vorheriger Zug des Gegners (Schlagzug)", "verfügbare Rückschläge", "Schach und Drohungen des Zuges",
                        "Engine-Bewertung des gespielten und des besten Zuges"),
                "Schlage nicht automatisch zurück: Prüfe zuerst, ob ein Schach oder eine stärkere Drohung dazwischen möglich ist.");
    }

    @Override
    public boolean matches(PositionContext c) {
        return c.previousOpponentMove().map(LinePly::isCapture).orElse(false);
    }

    @Override
    public List<CoachingFinding> evaluate(PositionContext c) {
        List<CoachingFinding> findings = new ArrayList<>();
        int square = c.previousOpponentMove().get().move().to();
        boolean recaptureAvailable = c.legalMovesBefore().stream().anyMatch(m -> m.to() == square && m.capture());
        if (!recaptureAvailable) {
            return findings;
        }
        boolean isRecapture = c.move().to() == square && c.move().capture();
        if (!isRecapture && isForcing(c.before(), c.after(), c.move())) {
            boolean good = c.lossCentipawns().map(l -> l <= TOLERANCE_CP).orElse(false);
            if (good) {
                findings.add(FindingBuilder.of(this).positive().verified()
                        .severity(Severity.MEDIUM)
                        .square(square).square(c.move().to())
                        .explanation("Clever: Statt sofort auf " + de.chesscoach.domain.position.Squares.name(square)
                                + " zurückzuschlagen, spielst du zuerst " + GermanText.moveWithNotation(c.line().ply(0))
                                + (c.move().check() ? " und gibst Schach" : " und stellst eine Drohung auf")
                                + ". Das nennt man Zwischenzug. Laut Computer ist das mindestens so gut wie das direkte Zurückschlagen.")
                        .lineEvidence(c.line(), 4, null, null, "Zwischenzug", Confidence.MEDIUM)
                        .build());
            }
        }
        if (isRecapture && c.isPlayedMove() && c.differsFromBest() && c.bestLine().isPresent()) {
            EngineLine best = c.bestLine().get();
            MoveInfo bestMove = best.ply(0).move();
            boolean bestIsIntermezzo = !(bestMove.to() == square && bestMove.capture())
                    && isForcing(best.start(), best.ply(0).after(), bestMove);
            if (bestIsIntermezzo && c.lossCentipawns().map(l -> l >= MISSED_THRESHOLD_CP).orElse(false)) {
                findings.add(FindingBuilder.of(this).negative().verified()
                        .title("Zwischenzug verpasst")
                        .severity(Severity.MEDIUM)
                        .square(square).square(bestMove.to())
                        .explanation("Du hast sofort zurückgeschlagen. Besser war es, zuerst " + GermanText.moveWithNotation(best.ply(0))
                                + " zu spielen" + (bestMove.check() ? " (mit Schach)" : " (mit einer Drohung)")
                                + " und erst danach zurückzuschlagen. Ein solcher Zug heißt Zwischenzug.")
                        .lineEvidence(best, 4, null, null, "verpasster Zwischenzug", Confidence.MEDIUM)
                        .build());
            }
        }
        return findings;
    }

    /** Schach oder neu erzeugter Angriff auf ein relevantes Ziel. */
    private static boolean isForcing(Position before, Position after, MoveInfo move) {
        if (move.check()) {
            return true;
        }
        return TacticsToolkit.relevantTargets(after, move.to()).stream().anyMatch(t -> !before.attacks(move.from(), t)
                && after.pieceAt(t).value() >= 5);
    }
}
