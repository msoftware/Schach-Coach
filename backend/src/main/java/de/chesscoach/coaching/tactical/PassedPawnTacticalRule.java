package de.chesscoach.coaching.tactical;

import de.chesscoach.coaching.AbstractRule;
import de.chesscoach.coaching.EngineLine;
import de.chesscoach.coaching.FindingBuilder;
import de.chesscoach.coaching.PositionContext;
import de.chesscoach.coaching.TacticalRule;
import de.chesscoach.domain.coaching.CoachingFinding;
import de.chesscoach.domain.coaching.Confidence;
import de.chesscoach.domain.coaching.Severity;
import de.chesscoach.domain.position.Color;
import de.chesscoach.domain.position.PawnStructure;
import de.chesscoach.domain.position.Position;
import de.chesscoach.domain.position.Squares;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * TACT-014 Freibauer: Ein Bauer kann unter Berücksichtigung gegnerischer Antworten zur Umwandlung gelangen –
 * belegt durch eine Umwandlung in der Engine-Variante oder (im reinen Bauernendspiel) durch die Quadratregel.
 */
@Component
public class PassedPawnTacticalRule extends AbstractRule implements TacticalRule {

    private static final int HORIZON = 12;

    public PassedPawnTacticalRule() {
        super("TACT-014", "Freibauer läuft durch",
                "Erkennt Freibauern, die laut Variante oder Quadratregel umwandeln können.",
                List.of("Umwandlungen in der Engine-Variante", "Freibauern beider Seiten",
                        "Quadratregel im reinen Bauernendspiel", "Seite am Zug"),
                "Ein Freibauer muss laufen: Je weiter er vorrückt, desto mehr gegnerische Kräfte bindet er.");
    }

    @Override
    public List<CoachingFinding> evaluate(PositionContext c) {
        List<CoachingFinding> findings = new ArrayList<>();
        EngineLine line = c.line();
        int own = line.firstPromotion(c.user(), HORIZON);
        if (own >= 0 && c.hasEngineLine()) {
            findings.add(FindingBuilder.of(this).positive().verified()
                    .severity(Severity.HIGH)
                    .square(line.ply(own).move().to())
                    .explanation("In der berechneten Variante wandelt dein Bauer mit " + line.ply(own).numberedSan()
                            + " um: " + line.numbered(0, own + 1) + ".")
                    .lineEvidence(line, own + 1, null, null, "Umwandlung", Confidence.MEDIUM)
                    .build());
        }
        int theirs = line.firstPromotion(c.opponent(), HORIZON);
        if (theirs >= 0 && c.hasEngineLine()) {
            findings.add(FindingBuilder.of(this).negative().verified()
                    .title("Gegnerischer Freibauer läuft durch")
                    .severity(Severity.HIGH)
                    .square(line.ply(theirs).move().to())
                    .explanation("In der berechneten Variante wandelt der gegnerische Bauer mit " + line.ply(theirs).numberedSan()
                            + " um: " + line.numbered(0, theirs + 1) + ".")
                    .lineEvidence(line, theirs + 1, null, null, "gegnerische Umwandlung", Confidence.MEDIUM)
                    .build());
        }
        if (own < 0 && isPurePawnEnding(c.after())) {
            for (int pawn : new PawnStructure(c.after(), c.user()).passed()) {
                if (outsideSquare(c.after(), pawn, c.user())) {
                    findings.add(FindingBuilder.of(this).positive().heuristic()
                            .title("Freibauer außerhalb des Quadrats")
                            .severity(Severity.HIGH)
                            .square(pawn).square(c.after().kingSquare(c.opponent()))
                            .explanation("Dein Freibauer auf " + Squares.name(pawn) + " ist nach der Quadratregel vom gegnerischen König "
                                    + "auf " + Squares.name(c.after().kingSquare(c.opponent())) + " nicht mehr einzuholen, sofern "
                                    + "der Weg frei bleibt.")
                            .build());
                }
            }
        }
        return findings;
    }

    static boolean isPurePawnEnding(Position p) {
        return p.nonPawnMaterial(Color.WHITE) == 0 && p.nonPawnMaterial(Color.BLACK) == 0;
    }

    /** Quadratregel: Der gegnerische König (am Zug) erreicht das Umwandlungsfeld nicht vor dem Bauern. */
    static boolean outsideSquare(Position p, int pawn, Color owner) {
        int file = Squares.file(pawn);
        int promotion = Squares.of(file, owner.promotionRank());
        for (int r = Squares.rank(pawn) + owner.pawnDirection(); r != owner.promotionRank() + owner.pawnDirection(); r += owner.pawnDirection()) {
            if (p.pieceAt(Squares.of(file, r)) != null) {
                return false;
            }
        }
        int distance = Math.abs(owner.promotionRank() - Squares.rank(pawn));
        if (Squares.rank(pawn) == owner.pawnStartRank()) {
            distance--;
        }
        int kingDistance = Squares.distance(p.kingSquare(owner.opposite()), promotion);
        boolean opponentToMove = p.sideToMove() != owner;
        return kingDistance - (opponentToMove ? 1 : 0) > distance;
    }
}
