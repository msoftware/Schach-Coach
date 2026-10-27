package de.chesscoach.coaching.tactical;

import de.chesscoach.coaching.AbstractRule;
import de.chesscoach.coaching.EngineLine;
import de.chesscoach.coaching.FindingBuilder;
import de.chesscoach.coaching.GermanText;
import de.chesscoach.coaching.PositionContext;
import de.chesscoach.coaching.TacticalRule;
import de.chesscoach.coaching.TacticsToolkit;
import de.chesscoach.domain.analysis.Score;
import de.chesscoach.domain.coaching.Certainty;
import de.chesscoach.domain.coaching.CoachingFinding;
import de.chesscoach.domain.coaching.Confidence;
import de.chesscoach.domain.coaching.Evidence;
import de.chesscoach.domain.coaching.Severity;
import de.chesscoach.domain.position.MoveInfo;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * TACT-011 Mattvermeidung: Der Zug übersieht ein unmittelbares oder erzwungenes Matt – oder wehrt eine
 * bestehende Mattdrohung erfolgreich ab.
 */
@Component
public class MateDefenseRule extends AbstractRule implements TacticalRule {

    public MateDefenseRule() {
        super("TACT-011", "Mattgefahr",
                "Erkennt übersehene Mattgefahr und erfolgreich abgewehrte Mattdrohungen.",
                List.of("legale Antworten des Gegners nach dem Zug", "Nullzug-Prüfung vor dem Zug",
                        "Mattbewertungen der Engine vor und nach dem Zug"),
                "Bevor du angreifst, prüfe: Welche Schachgebote hat mein Gegner, und droht er Matt?");
    }

    @Override
    public List<CoachingFinding> evaluate(PositionContext c) {
        List<CoachingFinding> findings = new ArrayList<>();
        Optional<MoveInfo> mateNow = TacticsToolkit.mateInOne(c.after(), c.oracle());
        if (mateNow.isPresent()) {
            findings.add(FindingBuilder.of(this).negative().verified().severity(Severity.CRITICAL)
                    .title("Matt in einem Zug übersehen")
                    .square(mateNow.get().to()).square(c.after().kingSquare(c.user()))
                    .explanation("Nach deinem Zug kann der Gegner mit " + mateNow.get().san() + " sofort matt setzen.")
                    .evidence(new Evidence(c.after().toFen(), c.move().san(), List.of(mateNow.get().san()), null, null,
                            "Matt", Certainty.VERIFIED, Confidence.HIGH))
                    .build());
        } else {
            Optional<Score> after = c.scoreAfter();
            boolean bestAvoidsMate = c.bestScore().map(s -> !s.isLosingMate()).orElse(true);
            if (after.isPresent() && after.get().isLosingMate() && bestAvoidsMate && c.differsFromBest()) {
                EngineLine line = c.line();
                findings.add(FindingBuilder.of(this).negative().verified().severity(Severity.CRITICAL)
                        .title("Erzwungenes Matt zugelassen")
                        .square(c.after().kingSquare(c.user()))
                        .explanation("Laut Engine kann der Gegner nach deinem Zug in " + GermanText.moves(Math.abs(after.get().value()))
                                + " matt setzen: " + line.numbered(0, line.size())
                                + ". Mit dem besten Zug wäre das vermeidbar gewesen.")
                        .lineEvidence(line, line.size(), null, null, "Matt gegen dich", Confidence.HIGH)
                        .engineSource()
                        .build());
            }
        }
        Optional<MoveInfo> threatBefore = TacticsToolkit.mateThreat(c.before(), c.opponent(), c.oracle());
        if (threatBefore.isPresent() && mateNow.isEmpty()
                && c.scoreAfter().map(s -> !s.isLosingMate()).orElse(true)) {
            findings.add(FindingBuilder.of(this).positive().verified().severity(Severity.HIGH)
                    .title("Mattdrohung abgewehrt")
                    .square(threatBefore.get().to())
                    .explanation("Der Gegner drohte " + threatBefore.get().san() + " mit Matt. Dein Zug "
                            + c.move().san() + " verhindert diese Drohung.")
                    .build());
        }
        return findings;
    }
}
