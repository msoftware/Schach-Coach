package de.chesscoach.coaching.tactical;

import de.chesscoach.coaching.AbstractRule;
import de.chesscoach.coaching.FindingBuilder;
import de.chesscoach.coaching.GermanText;
import de.chesscoach.coaching.PositionContext;
import de.chesscoach.coaching.TacticalRule;
import de.chesscoach.domain.coaching.CoachingFinding;
import de.chesscoach.domain.coaching.Severity;
import de.chesscoach.domain.position.LineMotifs;
import de.chesscoach.domain.position.MoveInfo;
import de.chesscoach.domain.position.Position;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * TACT-006 Fesselung: Eine Figur kann wegen eines Angriffs auf eine dahinterstehende höherwertige Figur oder den
 * König nicht frei ziehen. Fesselungen an den König werden über die legalen Züge des Gegners bestätigt.
 */
@Component
public class PinRule extends AbstractRule implements TacticalRule {

    public PinRule() {
        super("TACT-006", "Fesselung",
                "Erkennt neu entstandene Fesselungen für und gegen den Benutzer.",
                List.of("Linien langschrittiger Figuren", "Werte der vorderen und hinteren Figur",
                        "legale Züge der gefesselten Figur"),
                "Eine gefesselte Figur ist ein Angriffsziel: Greife sie mit weiteren Figuren, besonders mit Bauern, an.");
    }

    @Override
    public List<CoachingFinding> evaluate(PositionContext c) {
        List<CoachingFinding> findings = new ArrayList<>();
        List<LineMotifs.Motif> ownBefore = LineMotifs.find(c.before(), c.user());
        for (LineMotifs.Motif pin : LineMotifs.find(c.after(), c.user())) {
            if (pin.type() != LineMotifs.Type.PIN || containsSame(ownBefore, pin)) {
                continue;
            }
            if (!pin.absolute() && c.after().pieceAt(pin.front()).value() < 3) {
                continue;
            }
            boolean verified = pin.absolute() && pinnedPieceRestricted(c.after(), pin, c.legalMovesAfter());
            FindingBuilder b = FindingBuilder.of(this).positive()
                    .severity(pin.absolute() ? Severity.MEDIUM : Severity.LOW)
                    .piece(c.after(), pin.attacker()).piece(c.after(), pin.front()).piece(c.after(), pin.back())
                    .explanation(describe(c.after(), pin, true));
            if (verified) {
                b.verified();
            } else {
                b.heuristic();
            }
            findings.add(b.build());
        }
        List<LineMotifs.Motif> againstBefore = LineMotifs.find(c.before(), c.opponent());
        for (LineMotifs.Motif pin : LineMotifs.find(c.after(), c.opponent())) {
            if (pin.type() != LineMotifs.Type.PIN || containsSame(againstBefore, pin)) {
                continue;
            }
            if (!pin.absolute() && c.after().pieceAt(pin.front()).value() < 3) {
                continue;
            }
            findings.add(FindingBuilder.of(this).negative().heuristic()
                    .title("Eigene Figur gefesselt")
                    .severity(pin.absolute() ? Severity.MEDIUM : Severity.LOW)
                    .piece(c.after(), pin.attacker()).piece(c.after(), pin.front()).piece(c.after(), pin.back())
                    .explanation(describe(c.after(), pin, false))
                    .build());
        }
        return findings;
    }

    private static boolean containsSame(List<LineMotifs.Motif> list, LineMotifs.Motif motif) {
        return list.stream().anyMatch(m -> m.type() == motif.type() && m.front() == motif.front() && m.back() == motif.back());
    }

    /** Bestätigt eine Fesselung an den König: Kein legaler Zug führt die gefesselte Figur von der Linie. */
    static boolean pinnedPieceRestricted(Position position, LineMotifs.Motif pin, List<MoveInfo> legalMoves) {
        if (position.sideToMove() != position.pieceAt(pin.front()).color()) {
            return false;
        }
        List<Integer> line = new ArrayList<>(Position.between(pin.attacker(), pin.back()));
        line.add(pin.attacker());
        return legalMoves.stream().filter(m -> m.from() == pin.front()).allMatch(m -> line.contains(m.to()));
    }

    private static String describe(Position p, LineMotifs.Motif pin, boolean byUser) {
        String front = byUser ? GermanText.opponentPieceAcc(p, pin.front()) : GermanText.yourPieceAcc(p, pin.front());
        String attacker = GermanText.capitalize(byUser ? GermanText.yourPieceOn(p, pin.attacker())
                : GermanText.opponentPieceOn(p, pin.attacker()));
        if (pin.absolute()) {
            return attacker + " fesselt " + front + " an den König auf "
                    + de.chesscoach.domain.position.Squares.name(pin.back())
                    + ". Die gefesselte Figur darf die Linie nicht verlassen, weil sonst der König im Schach stünde.";
        }
        return attacker + " fesselt " + front + " an " + GermanText.withArticleAcc(p, pin.back())
                + ". Zieht die vordere Figur weg, geht die wertvollere Figur dahinter verloren.";
    }
}
