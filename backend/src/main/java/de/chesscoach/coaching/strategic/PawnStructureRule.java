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
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;

/**
 * STRAT-005 Bauernstruktur (Lasker: Bauernschwächen sind dauerhaft, weil Bauern nicht zurückziehen können;
 * isolierte, doppelte, rückständige und hängende Bauern werden zu Angriffszielen, Freibauern zu Trümpfen).
 */
@Component
public class PawnStructureRule extends AbstractRule implements StrategicRule {

    private record Feature(String name, String singularAcc, String pluralAcc, String consequence, Function<PawnStructure, Set<Integer>> detector, boolean weakness) {
    }

    private static final List<Feature> FEATURES = List.of(
            new Feature("isolierter Bauer", "einen isolierten Bauern", "isolierte Bauern", "Er kann von keinem eigenen Bauern gedeckt werden und muss von Figuren geschützt werden.",
                    PawnStructure::isolated, true),
            new Feature("Doppelbauer", "einen Doppelbauern", "Doppelbauern", "Doppelbauern behindern sich gegenseitig und können sich nicht gegenseitig decken.",
                    PawnStructure::doubled, true),
            new Feature("rückständiger Bauer", "einen rückständigen Bauern", "rückständige Bauern", "Er kann nicht gefahrlos vorrücken; das Feld vor ihm ist ein mögliches Vorpostenfeld für den Gegner.",
                    PawnStructure::backward, true),
            new Feature("hängende Bauern", "hängende Bauern", "hängende Bauern", "Sie stehen auf halboffenen Linien und können frontal angegriffen werden.",
                    PawnStructure::hanging, true),
            new Feature("Freibauer", "einen Freibauern", "Freibauern", "Kein gegnerischer Bauer kann ihn mehr aufhalten; er bindet gegnerische Figuren.",
                    PawnStructure::passed, false));

    public PawnStructureRule() {
        super("STRAT-005", "Bauernstruktur",
                "Erkennt neu entstandene Bauernschwächen und Freibauern beider Seiten.",
                List.of("isolierte, doppelte, rückständige und hängende Bauern vor und nach dem Zug",
                        "Freibauern", "halboffene Linien als Angriffsrichtung"),
                "Bauernzüge sind endgültig: Prüfe vor jedem Bauernzug und jedem Schlagen mit einem Bauern, welche dauerhaften Schwächen entstehen.");
    }

    @Override
    public boolean matches(PositionContext c) {
        return c.movedPiece() != null;
    }

    @Override
    public List<CoachingFinding> evaluate(PositionContext c) {
        List<CoachingFinding> findings = new ArrayList<>();
        for (Feature feature : FEATURES) {
            compare(c, feature, c.user(), findings);
            compare(c, feature, c.opponent(), findings);
        }
        return findings;
    }

    private void compare(PositionContext c, Feature feature, Color side, List<CoachingFinding> findings) {
        Set<Integer> before = feature.detector().apply(new PawnStructure(c.before(), side));
        Set<Integer> after = new TreeSet<>(feature.detector().apply(new PawnStructure(c.after(), side)));
        Set<Integer> fresh = new TreeSet<>();
        for (int sq : after) {
            if (!before.contains(sq) && !(sq == c.move().to() && before.contains(c.move().from()))) {
                fresh.add(sq);
            }
        }
        if (fresh.isEmpty()) {
            return;
        }
        boolean own = side == c.user();
        boolean good = own != feature.weakness();
        String whose = own ? "Du hast" : "Der Gegner hat";
        String direction = feature.weakness() ? attackDirection(c.after(), fresh, side) : "";
        FindingBuilder b = FindingBuilder.of(this).heuristic()
                .severity(feature.weakness() && fresh.size() >= 2 || !feature.weakness() ? Severity.MEDIUM : Severity.LOW)
                .title((own ? "Eigene Struktur: " : "Gegnerische Struktur: ") + feature.name())
                .squares(fresh)
                .explanation(whose + " jetzt " + (fresh.size() > 1 ? feature.pluralAcc() : feature.singularAcc())
                        + " auf " + GermanText.squares(fresh) + ". " + feature.consequence() + direction);
        if (good) {
            b.positive();
        } else {
            b.negative();
        }
        findings.add(b.build());
    }

    /** Angriffsrichtung: halboffene Linie für die Gegenseite und dort bereits stehende Schwerfiguren. */
    private static String attackDirection(Position position, Set<Integer> weaknesses, Color owner) {
        PawnStructure attacker = new PawnStructure(position, owner.opposite());
        List<String> files = new ArrayList<>();
        for (int sq : weaknesses) {
            int file = Squares.file(sq);
            if (attacker.isFileHalfOpen(file)) {
                files.add(Squares.fileName(file) + "-Linie");
            }
        }
        if (files.isEmpty()) {
            return "";
        }
        return " Der Gegner kann ihn über die " + GermanText.join(files.stream().distinct().toList()) + " angreifen.";
    }
}
