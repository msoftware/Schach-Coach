package de.chesscoach.coaching.tactical;

import de.chesscoach.coaching.AbstractRule;
import de.chesscoach.coaching.FindingBuilder;
import de.chesscoach.coaching.GermanText;
import de.chesscoach.coaching.PositionContext;
import de.chesscoach.coaching.TacticalRule;
import de.chesscoach.coaching.TacticsToolkit;
import de.chesscoach.domain.coaching.CoachingFinding;
import de.chesscoach.domain.coaching.Confidence;
import de.chesscoach.domain.coaching.Severity;
import de.chesscoach.domain.position.Color;
import de.chesscoach.domain.position.PieceKind;
import de.chesscoach.domain.position.Position;
import de.chesscoach.domain.position.StaticExchange;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * TACT-013 Verteidigung: Eine kritische Verteidigungsfigur wird entfernt (eigener Verteidiger zieht weg bzw.
 * gegnerischer Verteidiger wird geschlagen) oder eine Figur ist mit mehreren Verteidigungsaufgaben überlastet.
 */
@Component
public class DefenseRule extends AbstractRule implements TacticalRule {

    public DefenseRule() {
        super("TACT-013", "Verteidiger",
                "Erkennt entfernte und überlastete Verteidiger.",
                List.of("Deckungsbeziehungen vor und nach dem Zug", "statische Abtauschbewertung", "geschlagene Figur",
                        "Antwort der Engine-Variante"),
                "Bevor eine Figur zieht, prüfe, welche Aufgaben sie gerade erfüllt – vielleicht deckt sie etwas Wichtiges.");
    }

    @Override
    public List<CoachingFinding> evaluate(PositionContext c) {
        List<CoachingFinding> findings = new ArrayList<>();
        int from = c.move().from();
        // (a) Eigener Verteidiger zieht weg.
        for (int sq : c.after().squaresOf(c.user())) {
            if (sq == c.move().to() || c.after().pieceAt(sq).kind() == PieceKind.KING) {
                continue;
            }
            boolean wasDefendedByMover = c.before().attacks(from, sq);
            if (!wasDefendedByMover || c.after().attacks(c.move().to(), sq)) {
                continue;
            }
            int seeBefore = StaticExchange.bestCaptureGain(c.before(), sq, c.opponent());
            int seeAfter = StaticExchange.bestCaptureGain(c.after(), sq, c.opponent());
            if (seeBefore == 0 && seeAfter >= 100 && TacticsToolkit.hasLegalCaptureOn(c.legalMovesAfter(), sq)) {
                boolean confirmed = c.opponentReply().map(r -> r.move().to() == sq && r.isCapture()).orElse(false);
                FindingBuilder b = FindingBuilder.of(this).negative()
                        .title("Verteidiger abgezogen")
                        .severity(seeAfter >= 300 ? Severity.HIGH : Severity.MEDIUM)
                        .piece(c.after(), c.move().to()).piece(c.after(), sq)
                        .explanation("Die Figur, die du gezogen hast, hat vorher " + GermanText.withArticleAcc(c.after(), sq)
                                + " beschützt. Jetzt ist diese Figur ungeschützt."
                                + (confirmed ? " Der Gegner schlägt sie sofort: " + GermanText.moveWithNotation(c.opponentReply().get()) + "." : ""));
                if (confirmed) {
                    b.verified().lineEvidence(c.line(), 3, null, null, "Figur geht verloren", Confidence.HIGH);
                } else {
                    b.heuristic();
                }
                findings.add(b.build());
            }
        }
        // (b) Gegnerischen Verteidiger geschlagen.
        if (c.capturedPiece() != null) {
            int capturedSquare = c.move().to();
            for (int sq : c.after().squaresOf(c.opponent())) {
                if (c.after().pieceAt(sq).kind() == PieceKind.KING || !c.before().attacks(capturedSquare, sq)) {
                    continue;
                }
                int seeBefore = StaticExchange.bestCaptureGain(c.before().withSideToMove(c.user()), sq, c.user());
                int seeAfter = StaticExchange.bestCaptureGain(c.after(), sq, c.user());
                if (seeBefore == 0 && seeAfter >= 200) {
                    findings.add(FindingBuilder.of(this).positive().heuristic()
                            .title("Verteidiger beseitigt")
                            .severity(Severity.MEDIUM)
                            .square(capturedSquare).piece(c.after(), sq)
                            .explanation("Mit diesem Schlagzug entfernst du einen Beschützer: "
                                    + GermanText.opponentPieceOn(c.after(), sq) + " ist jetzt nicht mehr ausreichend geschützt.")
                            .build());
                }
            }
        }
        // (c) Überlastung: ein gegnerischer Verteidiger deckt allein zwei angegriffene Figuren.
        Map<Integer, List<Integer>> duties = overloadedDefenders(c.after(), c.opponent());
        Map<Integer, List<Integer>> dutiesBefore = overloadedDefenders(c.before(), c.opponent());
        duties.forEach((defender, protectedSquares) -> {
            if (dutiesBefore.containsKey(defender)) {
                return;
            }
            findings.add(FindingBuilder.of(this).positive().heuristic()
                    .title("Überlasteter Verteidiger")
                    .severity(Severity.MEDIUM)
                    .piece(c.after(), defender).pieces(c.after(), protectedSquares)
                    .explanation(GermanText.capitalize(GermanText.opponentPieceOn(c.after(), defender))
                            + " muss gleichzeitig die Figuren auf " + GermanText.squares(protectedSquares)
                            + " beschützen, die du beide angreifst. Beides auf einmal schafft sie nicht (überlastete Figur).")
                    .build());
        });
        return findings;
    }

    /** Verteidiger von {@code side}, die als einzige zwei oder mehr angegriffene eigene Figuren decken. */
    static Map<Integer, List<Integer>> overloadedDefenders(Position position, Color side) {
        Map<Integer, List<Integer>> result = new TreeMap<>();
        for (int sq : position.squaresOf(side)) {
            if (position.pieceAt(sq).kind() == PieceKind.KING || position.pieceAt(sq).kind() == PieceKind.PAWN) {
                continue;
            }
            if (position.attackersOf(sq, side.opposite()).isEmpty()) {
                continue;
            }
            List<Integer> defenders = position.attackersOf(sq, side);
            if (defenders.size() == 1 && position.pieceAt(defenders.get(0)).kind() != PieceKind.KING) {
                result.computeIfAbsent(defenders.get(0), k -> new ArrayList<>()).add(sq);
            }
        }
        result.values().removeIf(list -> list.size() < 2);
        return result;
    }
}
