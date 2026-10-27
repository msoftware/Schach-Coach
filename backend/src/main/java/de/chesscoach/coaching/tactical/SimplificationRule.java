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
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * TACT-016 Abwicklung: Ein durch den Zug eingeleiteter Schlagabtausch verändert Material oder Stellungscharakter
 * wesentlich (z. B. Übergang ins Endspiel).
 */
@Component
public class SimplificationRule extends AbstractRule implements TacticalRule {

    public SimplificationRule() {
        super("TACT-016", "Abwicklung",
                "Fasst einen durch den Zug eingeleiteten Schlagabtausch zusammen und bewertet seine Folgen.",
                List.of("ununterbrochene Schlagfolge ab dem Zug", "Materialbilanz vor und nach der Abwicklung",
                        "Spielphase vor und nach der Abwicklung"),
                "Wickle ab, wenn du Material oder eine bessere Struktur mitnimmst – besonders mit Materialvorteil ins Endspiel.");
    }

    @Override
    public boolean matches(PositionContext c) {
        return c.move().capture() && c.hasEngineLine();
    }

    @Override
    public List<CoachingFinding> evaluate(PositionContext c) {
        List<CoachingFinding> findings = new ArrayList<>();
        EngineLine line = c.line();
        int length = TacticsToolkit.captureSequenceLength(line, 0);
        if (length < 2) {
            return findings;
        }
        int before = c.before().materialBalance(c.user());
        int after = line.materialBalanceAfter(length, c.user());
        boolean intoEndgame = !c.before().isEndgame() && line.positionAfter(length).isEndgame();
        int net = after - before;
        List<String> userLost = new ArrayList<>();
        List<String> opponentLost = new ArrayList<>();
        for (int i = 0; i < length; i++) {
            LinePly ply = line.ply(i);
            if (ply.mover() == c.user()) {
                opponentLost.add(ply.captured().kind().germanName());
            } else {
                userLost.add(ply.captured().kind().germanName());
            }
        }
        FindingBuilder b = FindingBuilder.of(this).verified()
                .severity(intoEndgame || Math.abs(net) >= 2 ? Severity.MEDIUM : Severity.LOW)
                .square(c.move().to())
                .lineEvidence(line, length, before, after, net > 0 ? "Materialgewinn" : net < 0 ? "Materialverlust" : "ausgeglichener Tausch",
                        Confidence.HIGH);
        if (net > 0) {
            b.positive();
        } else if (net < 0) {
            b.negative();
        } else {
            b.neutral();
        }
        b.explanation("Dein Zug leitet die Abwicklung " + line.numbered(0, length) + " ein. Du gibst "
                + (userLost.isEmpty() ? "nichts" : GermanText.join(userLost)) + " und erhältst "
                + GermanText.join(opponentLost) + "; die Materialbilanz ändert sich von " + GermanText.balance(before)
                + " auf " + GermanText.balance(after) + "."
                + (intoEndgame ? " Danach ist ein Endspiel erreicht, in dem Königsaktivität und Bauernstruktur besonders wichtig werden." : ""));
        findings.add(b.build());
        return findings;
    }
}
