package de.chesscoach.coaching.strategic;

import de.chesscoach.coaching.AbstractRule;
import de.chesscoach.coaching.FindingBuilder;
import de.chesscoach.coaching.GermanText;
import de.chesscoach.coaching.LinePly;
import de.chesscoach.coaching.PositionContext;
import de.chesscoach.coaching.StrategicRule;
import de.chesscoach.domain.coaching.CoachingFinding;
import de.chesscoach.domain.coaching.FindingSource;
import de.chesscoach.domain.coaching.Severity;
import de.chesscoach.domain.coaching.StrategicPlan;
import de.chesscoach.domain.position.Color;
import de.chesscoach.domain.position.PawnStructure;
import de.chesscoach.domain.position.PieceKind;
import de.chesscoach.domain.position.Position;
import de.chesscoach.domain.position.Squares;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * STRAT-011 Langfristige Pläne: Leitet für beide Seiten Pläne aus konkreten Stellungsmerkmalen und der
 * Engine-Hauptvariante ab. Jeder Plan nennt das Merkmal, auf dem er beruht; ohne Merkmal kein Plan.
 */
@Component
public class LongTermPlanRule extends AbstractRule implements StrategicRule {

    private static final int MAX_PLANS_PER_SIDE = 3;

    public LongTermPlanRule() {
        super("STRAT-011", "Langfristige Pläne",
                "Leitet stellungsbezogene Pläne für beide Seiten ab (schwache Bauern, offene Linien, passive Figuren, "
                        + "Freibauern, Bauernmehrheiten, Königsaktivität, Abtausch).",
                List.of("Bauernschwächen beider Seiten", "offene Linien ohne Turm", "Mobilität der Figuren", "Freibauern und Bauernmehrheiten",
                        "Spielphase und Königsstellung", "Materialbilanz", "nächster eigener Zug der Engine-Hauptvariante"),
                "Spiele nach einem Plan, der sich aus der Stellung ergibt: Schwächen angreifen, Linien besetzen, die schlechteste Figur verbessern.");
    }

    @Override
    public List<CoachingFinding> evaluate(PositionContext c) {
        List<CoachingFinding> findings = new ArrayList<>();
        List<StrategicPlan> plans = plans(c);
        plans.stream().filter(p -> p.side().equals("USER")).findFirst().ifPresent(plan -> findings.add(
                FindingBuilder.of(this).neutral().heuristic().severity(Severity.INFO)
                        .title("Plan: " + plan.title())
                        .explanation(plan.explanation())
                        .squares(plan.squares().stream().map(Squares::parse).toList())
                        .build()));
        return findings;
    }

    /** Pläne für beide Seiten nach dem Zug. */
    public List<StrategicPlan> plans(PositionContext c) {
        List<StrategicPlan> result = new ArrayList<>();
        result.addAll(plansFor(c, c.after(), c.user(), "USER"));
        result.addAll(plansFor(c, c.after(), c.opponent(), "OPPONENT"));
        return result;
    }

    private List<StrategicPlan> plansFor(PositionContext c, Position p, Color side, String label) {
        List<StrategicPlan> plans = new ArrayList<>();
        Color other = side.opposite();
        boolean you = side == c.user();
        PawnStructure theirs = new PawnStructure(p, other);
        PawnStructure ours = new PawnStructure(p, side);

        Set<Integer> weak = new TreeSet<>(theirs.isolated());
        weak.addAll(theirs.backward());
        for (int sq : weak) {
            if (ours.isFileHalfOpen(Squares.file(sq)) || ours.isFileOpen(Squares.file(sq))) {
                plans.add(plan(label, "Schwachen Bauern " + Squares.name(sq) + " angreifen",
                        (you ? "Der gegnerische" : "Dein") + " Bauer auf " + Squares.name(sq) + " ist "
                                + (theirs.isolated().contains(sq) ? "isoliert" : "rückständig") + " und steht auf einer für "
                                + (you ? "dich" : "den Gegner") + " halboffenen Linie. Türme und Dame können ihn frontal unter Druck setzen.",
                        List.of(sq), (theirs.isolated().contains(sq) ? "isolierter" : "rückständiger") + " Bauer " + Squares.name(sq)));
                break;
            }
        }
        if (!p.isEndgame()) {
            for (int file = 0; file < 8; file++) {
                if (!ours.isFileOpen(file)) {
                    continue;
                }
                final int f = file;
                boolean occupied = p.squaresOf(side, PieceKind.ROOK).stream().anyMatch(r -> Squares.file(r) == f)
                        || p.squaresOf(other, PieceKind.ROOK).stream().anyMatch(r -> Squares.file(r) == f);
                if (!occupied && !p.squaresOf(side, PieceKind.ROOK).isEmpty()) {
                    plans.add(plan(label, "Offene " + Squares.fileName(file) + "-Linie besetzen",
                            "Die " + Squares.fileName(file) + "-Linie ist offen und von keinem Turm besetzt. Wer sie zuerst mit einem Turm "
                                    + "besetzt, kann über sie in das gegnerische Lager eindringen.",
                            List.of(Squares.of(file, side.backRank())), "offene " + Squares.fileName(file) + "-Linie"));
                    break;
                }
            }
        }
        int worst = -1;
        int worstMobility = Integer.MAX_VALUE;
        for (int sq : p.squaresOf(side)) {
            PieceKind kind = p.pieceAt(sq).kind();
            if ((kind.isMinor() || kind == PieceKind.ROOK) && !StrategicFeatures.isMinorHome(sq, p.pieceAt(sq))) {
                int m = p.mobility(sq);
                if (m < worstMobility) {
                    worstMobility = m;
                    worst = sq;
                }
            }
        }
        if (worst >= 0 && worstMobility <= 2 && !p.isOpening()) {
            plans.add(plan(label, "Passive Figur verbessern",
                    (you ? "Dein " : "Der gegnerische ") + GermanText.pieceOn(p, worst) + " hat nur " + worstMobility
                            + " Felder zur Verfügung. Eine Umgruppierung auf ein aktiveres Feld stärkt die Stellung.",
                    List.of(worst), "geringe Mobilität " + GermanText.pieceOn(p, worst)));
        }
        Set<Integer> passed = ours.passed();
        if (!passed.isEmpty()) {
            int most = passed.stream().max((a, b) -> Integer.compare(side.relativeRank(Squares.rank(a)), side.relativeRank(Squares.rank(b)))).get();
            plans.add(plan(label, "Freibauer " + Squares.name(most) + " vorrücken",
                    (you ? "Dein" : "Der gegnerische") + " Freibauer auf " + Squares.name(most)
                            + " sollte mit Figurenunterstützung vorrücken; der Gegner muss Kräfte zu seiner Blockade abstellen.",
                    List.of(most), "Freibauer " + Squares.name(most)));
        } else {
            for (boolean queenside : new boolean[]{true, false}) {
                int own = ours.countOnWing(queenside);
                int their = theirs.countOnWing(queenside);
                if (own > their && their >= 0 && own >= 2) {
                    plans.add(plan(label, "Bauernmehrheit am " + (queenside ? "Damenflügel" : "Königsflügel") + " nutzen",
                            (you ? "Du hast" : "Der Gegner hat") + " am " + (queenside ? "Damenflügel" : "Königsflügel") + " " + own
                                    + " gegen " + their + " Bauern. Durch Vorrücken der Mehrheit kann ein Freibauer entstehen.",
                            List.of(), "Bauernmehrheit " + own + ":" + their));
                    break;
                }
            }
        }
        if (p.isEndgame()) {
            int king = p.kingSquare(side);
            if (Squares.centerDistance(king) >= 2) {
                plans.add(plan(label, "König aktivieren",
                        "Im Endspiel ist der König eine starke Figur. " + (you ? "Dein" : "Der gegnerische") + " König auf "
                                + Squares.name(king) + " sollte in Richtung Zentrum bzw. zu den Bauern gehen.",
                        List.of(king), "Endspiel, König auf " + Squares.name(king)));
            }
        }
        int balance = p.materialBalance(side);
        if (balance >= 2 && !p.isEndgame()) {
            plans.add(plan(label, "Vorteilhaften Abtausch anstreben",
                    (you ? "Du hast" : "Der Gegner hat") + " " + GermanText.balance(balance)
                            + " Bauerneinheiten Materialvorteil. Figurentausch (nicht Bauerntausch) vereinfacht die Verwertung.",
                    List.of(), "Materialvorteil " + GermanText.balance(balance)));
        }
        List<StrategicPlan> limited = new ArrayList<>(plans.subList(0, Math.min(MAX_PLANS_PER_SIDE, plans.size())));
        int nextOwnPly = side == c.user() ? 2 : 1;
        if (c.hasEngineLine() && c.line().size() > nextOwnPly) {
            LinePly ply = c.line().ply(nextOwnPly);
            limited.add(new StrategicPlan(label, "Fortsetzung laut Engine: " + ply.numberedSan(),
                    "In der berechneten Hauptvariante folgt für " + (you ? "dich" : "den Gegner") + " " + ply.numberedSan()
                            + ". Dies ist die von der Suche gefundene Fortsetzung, keine erzwungene Zugfolge.",
                    List.of(Squares.name(ply.move().to())), "Engine-Hauptvariante", FindingSource.ENGINE));
        }
        return limited;
    }

    private static StrategicPlan plan(String side, String title, String explanation, List<Integer> squares, String basis) {
        return new StrategicPlan(side, title, explanation, squares.stream().map(Squares::name).toList(), basis,
                FindingSource.COACHING_RULE);
    }
}
