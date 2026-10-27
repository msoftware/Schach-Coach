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
 * TACT-003 Ungedeckte Figuren: Nach dem Zug ist eine eigene Figur angegriffen und unzureichend verteidigt,
 * die es vorher nicht war. Bestätigt wird der Befund, wenn der Gegner in der Engine-Variante tatsächlich schlägt.
 */
@Component
public class HangingPiecesRule extends AbstractRule implements TacticalRule {

    public HangingPiecesRule() {
        super("TACT-003", "Ungedeckte Figur",
                "Erkennt eigene Figuren, die durch den Zug angegriffen und unzureichend gedeckt sind.",
                List.of("Angreifer und Verteidiger jedes Feldes", "statische Abtauschbewertung",
                        "legale Schlagzüge des Gegners", "erste gegnerische Antwort der Engine-Variante"),
                "Zähle bei jeder angegriffenen Figur Angreifer und Verteidiger – und beachte dabei den Wert der beteiligten Figuren.");
    }

    @Override
    public List<CoachingFinding> evaluate(PositionContext c) {
        List<CoachingFinding> findings = new ArrayList<>();
        List<Integer> hangingBefore = TacticsToolkit.enPrise(c.before(), c.user(), 100);
        Optional<LinePly> reply = c.opponentReply();
        for (int sq : TacticsToolkit.enPrise(c.after(), c.user(), 100)) {
            boolean newlyHanging = sq == c.move().to() || !hangingBefore.contains(sq);
            if (!newlyHanging || !TacticsToolkit.hasLegalCaptureOn(c.legalMovesAfter(), sq)) {
                continue;
            }
            int gain = StaticExchange.bestCaptureGain(c.after(), sq, c.opponent());
            boolean confirmed = reply.isPresent() && reply.get().move().to() == sq && reply.get().isCapture();
            PieceKind kind = c.after().pieceAt(sq).kind();
            Severity severity = gain >= 300 ? Severity.HIGH : gain >= 200 ? Severity.MEDIUM : Severity.LOW;
            String attackers = GermanText.join(c.after().attackersOf(sq, c.opponent()).stream()
                    .map(a -> GermanText.pieceOn(c.after(), a)).toList());
            int defenders = c.after().attackersOf(sq, c.user()).size();
            FindingBuilder builder = FindingBuilder.of(this)
                    .negative()
                    .piece(c.after(), sq)
                    .squares(c.after().attackersOf(sq, c.opponent()));
            String base = GermanText.yourPieceOn(c.after(), sq) + " wird von " + attackers + " angegriffen und ist "
                    + (defenders == 0 ? "ungedeckt" : "nur unzureichend gedeckt (" + defenders + " Verteidiger)") + ".";
            if (confirmed) {
                builder.verified().severity(severity)
                        .explanation(base + " Die Engine schlägt in ihrer Hauptvariante tatsächlich mit "
                                + reply.get().numberedSan() + ".")
                        .lineEvidence(c.line(), 3, null, null, "Figur geht verloren", Confidence.HIGH);
            } else {
                // Ohne Bestätigung durch die Variante höchstens mittlere Gewichtung.
                Severity capped = severity.atLeast(Severity.MEDIUM) ? Severity.MEDIUM : severity;
                builder.heuristic().severity(kind == PieceKind.PAWN ? Severity.LOW : capped)
                        .explanation(base + " Nach statischer Abtauschrechnung wäre das Schlagen für den Gegner günstig; "
                                + "die Engine-Variante schlägt allerdings nicht sofort, was auf eine taktische Gegenchance hindeuten kann.");
            }
            findings.add(builder.build());
        }
        return findings;
    }
}
