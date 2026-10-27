package de.chesscoach.coaching.strategic;

import de.chesscoach.coaching.AbstractRule;
import de.chesscoach.coaching.FindingBuilder;
import de.chesscoach.coaching.GermanText;
import de.chesscoach.coaching.PositionContext;
import de.chesscoach.coaching.StrategicRule;
import de.chesscoach.domain.coaching.CoachingFinding;
import de.chesscoach.domain.coaching.Severity;
import de.chesscoach.domain.position.Color;
import de.chesscoach.domain.position.PawnStructure;
import de.chesscoach.domain.position.Position;
import de.chesscoach.domain.position.Squares;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * STRAT-008 Material und positionelle Kompensation (Lasker: Material ist nur ein Faktor; Entwicklungsvorsprung,
 * Königssicherheit und Initiative können ein Materialopfer rechtfertigen). Ein Materialnachteil wird nicht als
 * Fehler gewertet, wenn die Engine ausreichende Kompensation sieht.
 */
@Component
public class MaterialCompensationRule extends AbstractRule implements StrategicRule {

    public MaterialCompensationRule() {
        super("STRAT-008", "Material und Kompensation",
                "Erklärt Materialungleichgewichte und benennt konkrete Kompensationsfaktoren.",
                List.of("Materialbilanz am ruhigen Punkt der Variante", "Engine-Bewertung nach dem Zug",
                        "Entwicklungsvorsprung", "Königssicherheit beider Seiten", "Freibauern", "Schachgebote/Drohungen in der Variante"),
                "Zähle nicht nur Material: Entwicklungsvorsprung, ein unsicherer gegnerischer König oder ein starker Freibauer können mehr wert sein als ein Bauer.");
    }

    @Override
    public List<CoachingFinding> evaluate(PositionContext c) {
        List<CoachingFinding> findings = new ArrayList<>();
        int balance = c.hasEngineLine() ? c.line().settledMaterialBalance(c.user(), MATERIAL_HORIZON)
                : c.after().materialBalance(c.user());
        Position end = c.hasEngineLine() ? c.line().positionAfter(c.line().settledIndex(MATERIAL_HORIZON)) : c.after();
        // Nur Materialveränderungen durch den Zug bzw. seine Variante werden kommentiert.
        if (balance == 0 || balance == c.before().materialBalance(c.user()) || c.scoreAfter().isEmpty()) {
            return findings;
        }
        int score = c.scoreAfter().get().centipawnsClamped(3000);
        if (balance < 0 && score >= -50) {
            List<String> factors = compensationFactors(c, end, c.user());
            findings.add(FindingBuilder.of(this).positive().verified().severity(Severity.MEDIUM)
                    .title("Kompensation für Material")
                    .explanation("Du stehst materiell mit " + GermanText.balance(balance) + " Bauerneinheiten zurück, die Engine bewertet die Stellung "
                            + "aber mit " + c.scoreAfter().get().display() + " aus deiner Sicht. "
                            + (factors.isEmpty() ? "Die Kompensation ist dynamischer Natur und zeigt sich in der Engine-Variante."
                            : "Erkennbare Kompensation: " + GermanText.join(factors) + "."))
                    .build());
        } else if (balance > 0 && score <= 50) {
            List<String> factors = compensationFactors(c, end, c.opponent());
            findings.add(FindingBuilder.of(this).neutral().verified().severity(Severity.LOW)
                    .title("Materialvorteil ohne Bewertungsvorteil")
                    .explanation("Du hast " + GermanText.balance(balance) + " Bauerneinheiten mehr, die Engine sieht aber keinen Vorteil ("
                            + c.scoreAfter().get().display() + "). "
                            + (factors.isEmpty() ? "Der Gegner hat Gegenspiel, das sich in der Variante zeigt."
                            : "Gegenspiel des Gegners: " + GermanText.join(factors) + "."))
                    .build());
        }
        return findings;
    }

    static List<String> compensationFactors(PositionContext c, Position position, Color side) {
        List<String> factors = new ArrayList<>();
        Color other = side.opposite();
        int developmentLead = StrategicFeatures.developedMinors(position, side) - StrategicFeatures.developedMinors(position, other);
        if (developmentLead >= 2) {
            factors.add("Entwicklungsvorsprung von " + developmentLead + " Leichtfiguren");
        }
        int otherKing = position.kingSquare(other);
        if (StrategicFeatures.hasQueen(position, side) && !position.canCastle(other)
                && Squares.file(otherKing) >= 3 && Squares.file(otherKing) <= 4 && !position.isEndgame()) {
            factors.add("der gegnerische König steht ohne Rochademöglichkeit im Zentrum (" + Squares.name(otherKing) + ")");
        }
        var passed = new PawnStructure(position, side).passed();
        if (!passed.isEmpty()) {
            factors.add("Freibauer auf " + GermanText.squares(passed));
        }
        long checks = c.line().plies().stream().skip(1).limit(6).filter(p -> p.mover() == side && p.move().check()).count();
        if (checks >= 1) {
            factors.add("Initiative mit Schachgeboten in der Variante (" + checks + ")");
        }
        int mobility = StrategicFeatures.totalMobility(position, side) - StrategicFeatures.totalMobility(position, other);
        if (mobility >= 8) {
            factors.add("deutlich aktivere Figuren (+" + mobility + " Felder Bewegungsfreiheit)");
        }
        return factors;
    }
}
