package de.chesscoach.coaching.tactical;

import de.chesscoach.coaching.AbstractRule;
import de.chesscoach.coaching.EngineLine;
import de.chesscoach.coaching.FindingBuilder;
import de.chesscoach.coaching.GermanText;
import de.chesscoach.coaching.PositionContext;
import de.chesscoach.coaching.TacticalRule;
import de.chesscoach.coaching.TacticsToolkit;
import de.chesscoach.domain.analysis.Score;
import de.chesscoach.domain.coaching.CoachingFinding;
import de.chesscoach.domain.coaching.Confidence;
import de.chesscoach.domain.coaching.Severity;
import de.chesscoach.domain.position.MoveInfo;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * TACT-010 Mattdrohung: Der Zug setzt matt, erzeugt eine konkrete Mattdrohung (Matt im nächsten Zug, falls der
 * Gegner nichts unternimmt) oder erzwingt laut Engine Matt – bzw. ein erzwungenes Matt wurde verpasst.
 */
@Component
public class MateThreatRule extends AbstractRule implements TacticalRule {

    public MateThreatRule() {
        super("TACT-010", "Mattdrohung",
                "Erkennt Matt, Mattdrohungen und erzwungene Mattfolgen sowie verpasste Matts.",
                List.of("legale Züge nach dem Zug", "Nullzug-Prüfung auf Matt in einem Zug",
                        "Mattbewertungen der Engine für gespielten und besten Zug"),
                "Suche bei jedem Zug zuerst nach Schachgeboten: Sie sind die forcierendsten Züge und führen oft zum Matt.");
    }

    @Override
    public List<CoachingFinding> evaluate(PositionContext c) {
        List<CoachingFinding> findings = new ArrayList<>();
        if (c.move().check() && c.oracle().isCheckmate(c.after().toFen())) {
            findings.add(FindingBuilder.of(this).positive().verified().severity(Severity.CRITICAL)
                    .title("Schachmatt")
                    .square(c.after().kingSquare(c.opponent()))
                    .explanation("Schachmatt! Mit " + GermanText.moveWithNotation(c.line().ply(0)) + " kann der gegnerische König nicht mehr entkommen.")
                    .build());
            return findings;
        }
        Optional<Score> after = c.scoreAfter();
        if (after.isPresent() && after.get().isWinningMate()) {
            EngineLine line = c.line();
            boolean reached = c.oracle().isCheckmate(line.end().toFen());
            findings.add(FindingBuilder.of(this).positive().verified().severity(Severity.CRITICAL)
                    .title("Erzwungenes Matt")
                    .square(c.after().kingSquare(c.opponent()))
                    .explanation("Stark: Laut Computer kannst du den Gegner nach diesem Zug in " + GermanText.moves(after.get().value())
                            + " mattsetzen, egal was er tut."
                            + (reached ? " So geht es: " + GermanText.lineWords(line, 0, 5, c.user()) + "." : ""))
                    .lineEvidence(line, line.size(), null, null, "Matt", Confidence.HIGH)
                    .engineSource()
                    .build());
        } else {
            // Nur neue Drohungen loben: War das Matt schon vor dem Zug möglich, hat der Zug nichts dazu beigetragen.
            boolean mateExistedBefore = TacticsToolkit.mateInOne(c.before(), c.oracle()).isPresent();
            Optional<MoveInfo> threat = mateExistedBefore ? Optional.empty()
                    : TacticsToolkit.mateThreat(c.after(), c.user(), c.oracle());
            threat.ifPresent(mate -> findings.add(FindingBuilder.of(this).positive().verified().severity(Severity.HIGH)
                    .square(mate.to()).square(c.after().kingSquare(c.opponent()))
                    .explanation("Dein Zug droht Matt: Wenn der Gegner nicht aufpasst, spielst du als Nächstes "
                            + GermanText.moveWithNotation(c.after().withSideToMove(c.user()), mate) + ". Er muss das jetzt verhindern.")
                    .evidence(new de.chesscoach.domain.coaching.Evidence(c.after().toFen(), c.move().san(),
                            List.of(mate.san()), null, null, "Mattdrohung", de.chesscoach.domain.coaching.Certainty.VERIFIED,
                            Confidence.HIGH))
                    .build()));
        }
        if (c.isPlayedMove() && c.differsFromBest() && c.bestScore().map(Score::isWinningMate).orElse(false)
                && !(after.isPresent() && after.get().isWinningMate())) {
            EngineLine best = c.bestLine().orElseThrow();
            findings.add(FindingBuilder.of(this).negative().verified().severity(Severity.CRITICAL)
                    .title("Matt verpasst")
                    .square(best.ply(0).move().to())
                    .explanation("Hier hättest du den Gegner in " + GermanText.moves(c.bestScore().get().value()) + " mattsetzen können: "
                            + GermanText.lineWords(best, 0, 5, c.user()) + " (" + best.numbered(0, best.size()) + "). "
                            + "Nach deinem Zug geht das nicht mehr.")
                    .lineEvidence(best, best.size(), null, null, "verpasstes Matt", Confidence.HIGH)
                    .engineSource()
                    .build());
        }
        return findings;
    }
}
