package de.chesscoach.coaching.strategic;

import de.chesscoach.coaching.AbstractRule;
import de.chesscoach.coaching.FindingBuilder;
import de.chesscoach.coaching.GermanText;
import de.chesscoach.coaching.LinePly;
import de.chesscoach.coaching.PositionContext;
import de.chesscoach.coaching.StrategicRule;
import de.chesscoach.coaching.TacticsToolkit;
import de.chesscoach.domain.coaching.CoachingFinding;
import de.chesscoach.domain.coaching.Severity;
import de.chesscoach.domain.position.MoveInfo;
import de.chesscoach.domain.position.PieceKind;
import de.chesscoach.domain.position.Squares;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * STRAT-010 Initiative und Tempo (Lasker: Wer Drohungen aufstellt, zwingt den Gegner zur Reaktion und gewinnt
 * Zeit; Hin- und Herziehen verschenkt Tempi). Initiative wird nur bei einer konkreten, relevanten Drohung erkannt.
 */
@Component
public class InitiativeRule extends AbstractRule implements StrategicRule {

    public InitiativeRule() {
        super("STRAT-010", "Initiative und Tempo",
                "Erkennt konkrete Drohungen, die den Gegner zur Reaktion zwingen, sowie Tempoverluste durch Hin- und Herziehen.",
                List.of("Schachgebote", "neu angegriffene relevante Ziele", "Mattdrohungen", "vorheriger eigener Zug derselben Figur"),
                "Spiele mit Tempo: Züge, die gleichzeitig entwickeln und drohen, zwingen den Gegner, nach deiner Pfeife zu tanzen.");
    }

    @Override
    public List<CoachingFinding> evaluate(PositionContext c) {
        List<CoachingFinding> findings = new ArrayList<>();
        Optional<LinePly> previous = c.previousOwnMove();
        if (previous.isPresent()) {
            MoveInfo prev = previous.get().move();
            if (prev.to() == c.move().from() && prev.from() == c.move().to() && !c.move().capture() && !c.move().check()) {
                findings.add(FindingBuilder.of(this).negative().verified().severity(Severity.MEDIUM)
                        .title("Tempoverlust")
                        .square(c.move().from()).square(c.move().to())
                        .explanation("Die Figur kehrt mit " + c.move().san() + " auf " + Squares.name(c.move().to())
                                + " zurück, von wo sie gerade erst gekommen ist. Damit hast du zwei Züge ohne Fortschritt verbraucht.")
                        .build());
                return findings;
            }
        }
        List<String> threats = new ArrayList<>();
        List<Integer> squares = new ArrayList<>();
        if (c.move().check()) {
            threats.add("Schach");
            squares.add(c.after().kingSquare(c.opponent()));
        }
        for (int target : TacticsToolkit.relevantTargets(c.after(), c.move().to())) {
            if (c.after().pieceAt(target).kind() == PieceKind.KING || c.before().attacks(c.move().from(), target)) {
                continue;
            }
            threats.add("Angriff auf " + GermanText.opponentPieceAcc(c.after(), target));
            squares.add(target);
        }
        TacticsToolkit.mateThreat(c.after(), c.user(), c.oracle()).ifPresent(m -> {
            threats.add("Mattdrohung " + m.san());
            squares.add(m.to());
        });
        if (!threats.isEmpty()) {
            boolean engineConfirms = c.lossCentipawns().map(l -> l <= 50).orElse(false);
            FindingBuilder b = FindingBuilder.of(this).positive()
                    .severity(threats.size() >= 2 ? Severity.MEDIUM : Severity.LOW)
                    .squares(squares)
                    .explanation("Dein Zug stellt eine konkrete Drohung auf (" + GermanText.join(threats)
                            + "). Der Gegner muss darauf reagieren und kann seine eigenen Pläne nicht ungestört verfolgen."
                            + (engineConfirms ? " Laut Engine-Bewertung ist das auch objektiv ein guter Weg." : ""));
            findings.add((engineConfirms ? b.verified() : b.heuristic()).build());
        }
        return findings;
    }
}
