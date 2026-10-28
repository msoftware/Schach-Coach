package de.chesscoach.coaching.strategic;

import de.chesscoach.coaching.AbstractRule;
import de.chesscoach.coaching.FindingBuilder;
import de.chesscoach.coaching.GermanText;
import de.chesscoach.coaching.PositionContext;
import de.chesscoach.coaching.StrategicRule;
import de.chesscoach.coaching.TacticsToolkit;
import de.chesscoach.domain.coaching.CoachingFinding;
import de.chesscoach.domain.coaching.Severity;
import de.chesscoach.domain.position.Color;
import de.chesscoach.domain.position.PieceKind;
import de.chesscoach.domain.position.Position;
import de.chesscoach.domain.position.Squares;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * STRAT-001 Entwicklung der Figuren (Lasker: schnelle Mobilisierung der Figuren in der Eröffnung; keine Figur
 * zweimal ziehen, solange andere unentwickelt sind; die Dame nicht zu früh ins Spiel bringen).
 */
@Component
public class DevelopmentRule extends AbstractRule implements StrategicRule {

    public DevelopmentRule() {
        super("STRAT-001", "Entwicklung der Figuren",
                "Bewertet, ob ein Eröffnungszug die Entwicklung fördert oder Tempo verliert.",
                List.of("Leichtfiguren auf Startfeldern (beide Seiten)", "gezogene Figur und ihr Startfeld",
                        "Zugnummer und Material (Eröffnungsphase)", "Mobilität unentwickelter Läufer"),
                "Entwickle in der Eröffnung zuerst Springer und Läufer, rochiere früh und ziehe keine Figur zweimal ohne Grund.");
    }

    @Override
    public boolean matches(PositionContext c) {
        return c.before().isOpening() && c.movedPiece() != null;
    }

    @Override
    public List<CoachingFinding> evaluate(PositionContext c) {
        List<CoachingFinding> findings = new ArrayList<>();
        Color user = c.user();
        int from = c.move().from();
        PieceKind kind = c.movedPiece().kind();
        List<Integer> undevelopedBefore = StrategicFeatures.undevelopedMinors(c.before(), user);
        List<Integer> undevelopedAfter = StrategicFeatures.undevelopedMinors(c.after(), user);
        int ownDeveloped = StrategicFeatures.developedMinors(c.after(), user);
        int opponentDeveloped = StrategicFeatures.developedMinors(c.after(), c.opponent());
        String comparison = " Springer und Läufer im Spiel: du " + ownDeveloped + ", Gegner " + opponentDeveloped + ".";

        if (kind.isMinor() && undevelopedBefore.contains(from)) {
            findings.add(FindingBuilder.of(this).positive().heuristic().severity(Severity.LOW)
                    .title("Figur entwickelt")
                    .square(c.move().to())
                    .explanation("Gut: Du bringst " + (kind.feminine() ? "deine " : "deinen ") + kind.germanName()
                            + " von " + Squares.name(from) + " nach " + Squares.name(c.move().to()) + " ins Spiel."
                            + (undevelopedAfter.isEmpty() ? " Damit sind alle Springer und Läufer im Spiel."
                            : " Noch nicht im Spiel: " + GermanText.join(undevelopedAfter.stream().map(sq -> GermanText.pieceOn(c.after(), sq)).toList()) + ".")
                            + comparison)
                    .build());
            return findings;
        }
        boolean responding = TacticsToolkit.enPrise(c.before(), user, 100).contains(from);
        boolean forcing = c.move().capture() || c.move().check();
        if (kind != PieceKind.PAWN && kind != PieceKind.KING && !StrategicFeatures.isHomeSquare(from, c.movedPiece())
                && undevelopedBefore.size() >= 2 && !responding && !forcing && !isCastlingRookMove(c)) {
            findings.add(FindingBuilder.of(this).negative().heuristic().severity(Severity.MEDIUM)
                    .title("Bereits entwickelte Figur erneut gezogen")
                    .piece(c.after(), c.move().to()).squares(undevelopedBefore)
                    .explanation("Du ziehst " + (kind.feminine() ? "deine " : "deinen ") + kind.germanName()
                            + " schon wieder, obwohl noch " + undevelopedBefore.size() + " Springer und Läufer auf ihren Startfeldern stehen ("
                            + GermanText.squares(undevelopedBefore) + "). In der Eröffnung ist es besser, zuerst alle Figuren ins Spiel zu bringen, statt eine Figur mehrmals zu ziehen."
                            + comparison)
                    .build());
        }
        if (kind == PieceKind.QUEEN && c.before().fullmoveNumber() <= 8 && undevelopedBefore.size() >= 3 && !forcing && !responding) {
            findings.add(FindingBuilder.of(this).negative().heuristic().severity(Severity.LOW)
                    .title("Frühe Damenzüge")
                    .piece(c.after(), c.move().to())
                    .explanation("Die Dame zieht früh ins Spiel, während " + undevelopedBefore.size()
                            + " Springer und Läufer noch nicht im Spiel sind. Der Gegner kann sie angreifen und dabei seine eigenen Figuren "
                            + "ins Spiel bringen – du verlierst dadurch Zeit.")
                    .build());
        }
        if (kind == PieceKind.PAWN) {
            int bishopMobilityBefore = homeBishopMobility(c.before(), user);
            int bishopMobilityAfter = homeBishopMobility(c.after(), user);
            if (bishopMobilityAfter - bishopMobilityBefore >= 2) {
                findings.add(FindingBuilder.of(this).positive().heuristic().severity(Severity.LOW)
                        .title("Entwicklung vorbereitet")
                        .square(c.move().to())
                        .explanation(bishopText(homeBishops(c.after(), user), bishopMobilityBefore, bishopMobilityAfter))
                        .build());
            }
        }
        if (findings.isEmpty() && opponentDeveloped - ownDeveloped >= 2) {
            findings.add(FindingBuilder.of(this).negative().heuristic().severity(Severity.LOW)
                    .title("Entwicklungsrückstand")
                    .squares(undevelopedAfter)
                    .explanation("Der Gegner hat schon " + opponentDeveloped + " Springer und Läufer im Spiel, du erst " + ownDeveloped
                            + ". Bring deine Figuren schnell ins Spiel, sonst kann der Gegner dich mit mehr Figuren angreifen.")
                    .build());
        }
        return findings;
    }

    private static boolean isCastlingRookMove(PositionContext c) {
        return c.movedPiece().kind() == PieceKind.ROOK && c.before().canCastle(c.user()) && !c.after().canCastle(c.user());
    }

    /** Läufer, die noch auf ihrem Ausgangsfeld stehen. */
    private static List<Integer> homeBishops(Position position, Color color) {
        return StrategicFeatures.undevelopedMinors(position, color).stream()
                .filter(sq -> position.pieceAt(sq).kind() == PieceKind.BISHOP)
                .toList();
    }

    /** Ein Läufer im Singular mit Feld, mehrere im Plural – damit der Text zur Stellung passt. */
    static String bishopText(List<Integer> bishops, int before, int after) {
        if (bishops.size() == 1) {
            return "Dein Bauernzug macht den Weg frei für deinen Läufer auf " + Squares.name(bishops.getFirst())
                    + " (er erreicht jetzt " + after + " statt " + before + " Felder).";
        }
        return "Dein Bauernzug macht den Weg frei für deine noch unentwickelten Läufer "
                + "(sie erreichen jetzt zusammen " + after + " statt " + before + " Felder).";
    }

    private static int homeBishopMobility(Position position, Color color) {
        int sum = 0;
        for (int sq : StrategicFeatures.undevelopedMinors(position, color)) {
            if (position.pieceAt(sq).kind() == PieceKind.BISHOP) {
                sum += position.mobility(sq);
            }
        }
        return sum;
    }
}
