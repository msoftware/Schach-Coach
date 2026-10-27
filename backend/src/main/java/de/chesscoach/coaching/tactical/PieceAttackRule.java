package de.chesscoach.coaching.tactical;

import de.chesscoach.coaching.AbstractRule;
import de.chesscoach.coaching.FindingBuilder;
import de.chesscoach.coaching.GermanText;
import de.chesscoach.coaching.LinePly;
import de.chesscoach.coaching.PositionContext;
import de.chesscoach.coaching.TacticalRule;
import de.chesscoach.coaching.TacticsToolkit;
import de.chesscoach.domain.coaching.CoachingFinding;
import de.chesscoach.domain.coaching.Confidence;
import de.chesscoach.domain.coaching.Severity;
import de.chesscoach.domain.position.PieceKind;
import de.chesscoach.domain.position.StaticExchange;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * TACT-004 Figurenangriffe: Der Zug erzeugt einen konkreten Angriff auf eine gegnerische Figur oder ignoriert
 * einen bestehenden Angriff auf eine eigene Figur.
 */
@Component
public class PieceAttackRule extends AbstractRule implements TacticalRule {

    public PieceAttackRule() {
        super("TACT-004", "Figurenangriff",
                "Erkennt neu erzeugte Angriffe der gezogenen Figur und übersehene Angriffe des Gegners.",
                List.of("Angriffe der gezogenen Figur vor und nach dem Zug", "Werte von Angreifer und Ziel",
                        "statische Abtauschbewertung vor und nach dem Zug", "Antwort der Engine-Variante"),
                "Reagiere auf jeden gegnerischen Angriff: Ziehe die Figur weg, decke sie oder schaffe eine stärkere Gegendrohung.");
    }

    @Override
    public List<CoachingFinding> evaluate(PositionContext c) {
        List<CoachingFinding> findings = new ArrayList<>();
        int to = c.move().to();
        List<Integer> newTargets = new ArrayList<>();
        for (int target : TacticsToolkit.relevantTargets(c.after(), to)) {
            if (c.after().pieceAt(target).kind() == PieceKind.KING) {
                continue;
            }
            boolean attackedBefore = c.before().pieceAt(c.move().from()) != null && c.before().attacks(c.move().from(), target);
            if (!attackedBefore) {
                newTargets.add(target);
            }
        }
        if (newTargets.size() == 1) {
            int target = newTargets.get(0);
            Optional<LinePly> follow = c.line().size() > 2 ? Optional.of(c.line().ply(2)) : Optional.empty();
            boolean wins = follow.isPresent() && follow.get().move().to() == target && follow.get().isCapture();
            FindingBuilder b = FindingBuilder.of(this).positive()
                    .title("Angriff auf " + c.after().pieceAt(target).kind().germanAccusative())
                    .piece(c.after(), to).piece(c.after(), target)
                    .severity(wins ? Severity.MEDIUM : Severity.LOW);
            String text = GermanText.yourPieceOn(c.after(), to) + " greift " + GermanText.opponentPieceAcc(c.after(), target)
                    + " an. Der Gegner muss jetzt etwas tun, sonst verliert er diese Figur.";
            if (wins) {
                b.verified().explanation(text + " Der Computer zeigt, dass du sie danach tatsächlich schlagen kannst: "
                        + GermanText.lineWords(c.line(), 1, 2, c.user()) + ".").lineEvidence(c.line(), 3, null, null, "Angriff führt zu Materialgewinn", Confidence.HIGH);
            } else {
                b.heuristic().explanation(text);
            }
            findings.add(b.build());
        }

        // Ignorierter Angriff: Figur war vorher en prise, ist es nachher noch, und der Zug bietet nichts Gleichwertiges.
        if (c.move().check()) {
            return findings;
        }
        int capturedValue = c.capturedPiece() == null ? 0 : StaticExchange.centipawns(c.capturedPiece().kind());
        for (int sq : TacticsToolkit.enPrise(c.before(), c.user(), 100)) {
            if (sq == c.move().from() || c.after().pieceAt(sq) == null || c.after().pieceAt(sq).color() != c.user()) {
                continue;
            }
            int gainAfter = StaticExchange.bestCaptureGain(c.after(), sq, c.opponent());
            if (gainAfter < 100 || capturedValue >= gainAfter || !TacticsToolkit.hasLegalCaptureOn(c.legalMovesAfter(), sq)) {
                continue;
            }
            Optional<LinePly> reply = c.opponentReply();
            boolean confirmed = reply.isPresent() && reply.get().move().to() == sq && reply.get().isCapture();
            FindingBuilder b = FindingBuilder.of(this).negative()
                    .title("Angriff ignoriert")
                    .piece(c.after(), sq)
                    .squares(c.after().attackersOf(sq, c.opponent()))
                    .severity(gainAfter >= 300 ? Severity.HIGH : Severity.MEDIUM);
            String text = GermanText.yourPieceOn(c.after(), sq) + " war schon vor deinem Zug bedroht – und dein Zug hat daran nichts geändert.";
            if (confirmed) {
                b.verified().explanation(text + " Der Gegner kann sie jetzt einfach schlagen: " + GermanText.moveWithNotation(reply.get())
                        + ". Bevor du einen anderen Zug machst, prüfe immer zuerst, welche deiner Figuren bedroht sind.")
                        .lineEvidence(c.line(), 3, null, null, "Figur geht verloren", Confidence.HIGH);
            } else {
                b.heuristic().explanation(text + " Der Gegner könnte sie mit Gewinn schlagen.");
            }
            findings.add(b.build());
        }
        return findings;
    }
}
