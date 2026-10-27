package de.chesscoach.coaching.tactical;

import de.chesscoach.coaching.AbstractRule;
import de.chesscoach.coaching.FindingBuilder;
import de.chesscoach.coaching.GermanText;
import de.chesscoach.coaching.LinePly;
import de.chesscoach.coaching.PositionContext;
import de.chesscoach.coaching.TacticalRule;
import de.chesscoach.coaching.TacticsToolkit;
import de.chesscoach.domain.coaching.CoachingFinding;
import de.chesscoach.domain.coaching.Severity;
import de.chesscoach.domain.position.PieceKind;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * TACT-008 Abzugsangriff: Durch das Wegziehen einer Figur wird eine dahinterstehende Angriffslinie
 * einer langschrittigen Figur geöffnet.
 */
@Component
public class DiscoveredAttackRule extends AbstractRule implements TacticalRule {

    public DiscoveredAttackRule() {
        super("TACT-008", "Abzugsangriff",
                "Erkennt Abzugsangriffe und Abzugsschach durch den eigenen Zug und durch die gegnerische Antwort.",
                List.of("Linien eigener langschrittiger Figuren vor und nach dem Zug", "Startfeld der gezogenen Figur",
                        "relevante Ziele der geöffneten Linie"),
                "Steht eine eigene Figur zwischen deinem Läufer, Turm oder deiner Dame und einem gegnerischen Ziel, kann ihr Wegzug einen doppelten Angriff erzeugen.");
    }

    @Override
    public List<CoachingFinding> evaluate(PositionContext c) {
        List<CoachingFinding> findings = new ArrayList<>();
        for (int[] attack : TacticsToolkit.discoveredAttacks(c.before(), c.after(), c.user(), c.move().from(), c.move().to())) {
            boolean check = c.after().pieceAt(attack[1]).kind() == PieceKind.KING;
            findings.add(FindingBuilder.of(this).positive().verified()
                    .title(check ? "Abzugsschach" : "Abzugsangriff")
                    .severity(check ? Severity.HIGH : Severity.MEDIUM)
                    .piece(c.after(), attack[0]).piece(c.after(), attack[1]).square(c.move().from())
                    .explanation("Durch den Zug " + c.move().san() + " öffnet sich die Linie für deine Figur "
                            + GermanText.pieceOn(c.after(), attack[0]) + ". Diese Figur greift jetzt "
                            + (check ? "den gegnerischen König an (Abzugsschach)"
                            : GermanText.opponentPieceAcc(c.after(), attack[1]) + " an")
                            + ". Die gezogene Figur kann gleichzeitig eine eigene Drohung aufstellen.")
                    .build());
        }
        if (c.opponentReply().isPresent()) {
            LinePly reply = c.opponentReply().get();
            for (int[] attack : TacticsToolkit.discoveredAttacks(reply.before(), reply.after(), c.opponent(),
                    reply.move().from(), reply.move().to())) {
                findings.add(FindingBuilder.of(this).negative().verified()
                        .title("Gegnerischer Abzugsangriff")
                        .severity(Severity.MEDIUM)
                        .piece(reply.after(), attack[0]).piece(reply.after(), attack[1])
                        .explanation("In der Engine-Variante folgt " + reply.numberedSan() + " mit Abzugsangriff: "
                                + GermanText.opponentPieceOn(reply.after(), attack[0]) + " greift danach "
                                + GermanText.withArticleAcc(reply.after(), attack[1]) + " an.")
                        .build());
            }
        }
        return findings;
    }
}
