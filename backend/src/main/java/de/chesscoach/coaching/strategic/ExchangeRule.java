package de.chesscoach.coaching.strategic;

import de.chesscoach.coaching.AbstractRule;
import de.chesscoach.coaching.FindingBuilder;
import de.chesscoach.coaching.LinePly;
import de.chesscoach.coaching.PositionContext;
import de.chesscoach.coaching.StrategicRule;
import de.chesscoach.domain.coaching.CoachingFinding;
import de.chesscoach.domain.coaching.Severity;
import de.chesscoach.domain.position.PieceKind;
import de.chesscoach.domain.position.Position;
import de.chesscoach.domain.position.Squares;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * STRAT-009 Figurenabtausch (Lasker: Tausche, wenn du im Vorteil bist oder ein aktiver gegnerischer Angreifer
 * verschwindet; behalte das Läuferpaar in offenen Stellungen; vermeide es, den "guten" Läufer zu tauschen).
 */
@Component
public class ExchangeRule extends AbstractRule implements StrategicRule {

    public ExchangeRule() {
        super("STRAT-009", "Figurenabtausch",
                "Bewertet Figurentausche nach Materiallage, Läuferpaar, gutem/schlechtem Läufer und Aktivität der getauschten Figuren.",
                List.of("getauschte Figuren (Zug und direkte Antwort)", "Materialbilanz", "Läuferpaar vor und nach dem Tausch",
                        "Anteil eigener Bauern auf der Feldfarbe des verbleibenden Läufers", "Mobilität der getauschten Figuren"),
                "Tausche Figuren, wenn du materiell vorne liegst; behalte sie, wenn du angreifen willst oder zurückliegst.");
    }

    @Override
    public boolean matches(PositionContext c) {
        return c.move().capture() && c.capturedPiece() != null && c.capturedPiece().kind() != PieceKind.PAWN
                && c.movedPiece().kind() != PieceKind.PAWN && c.movedPiece().kind() != PieceKind.KING;
    }

    @Override
    public List<CoachingFinding> evaluate(PositionContext c) {
        List<CoachingFinding> findings = new ArrayList<>();
        LinePly reply = c.opponentReply().orElse(null);
        boolean recaptured = reply != null && reply.isCapture() && reply.move().to() == c.move().to();
        if (!recaptured) {
            return findings;
        }
        PieceKind ours = c.movedPiece().kind();
        PieceKind theirs = c.capturedPiece().kind();
        Position afterExchange = reply.after();
        int materialBefore = c.before().materialBalance(c.user());
        String trade = (ours.feminine() ? "deine " : "dein ") + ours.germanName() + " gegen "
                + (theirs.feminine() ? "die gegnerische " : "den gegnerischen ") + theirs.germanName();

        if (c.before().count(c.user(), PieceKind.BISHOP) == 2 && afterExchange.count(c.user(), PieceKind.BISHOP) < 2
                && afterExchange.count(c.opponent(), PieceKind.BISHOP) == 2) {
            findings.add(FindingBuilder.of(this).negative().heuristic().severity(Severity.LOW)
                    .title("Läuferpaar abgegeben")
                    .square(c.move().to())
                    .explanation("Mit dem Tausch " + trade + " gibst du das Läuferpaar ab, während der Gegner beide Läufer behält. "
                            + "In offenen Stellungen sind zwei Läufer oft stärker als Läufer und Springer.")
                    .build());
        } else if (c.before().count(c.opponent(), PieceKind.BISHOP) == 2 && afterExchange.count(c.opponent(), PieceKind.BISHOP) < 2
                && afterExchange.count(c.user(), PieceKind.BISHOP) == 2) {
            findings.add(FindingBuilder.of(this).positive().heuristic().severity(Severity.LOW)
                    .title("Läuferpaar gewonnen")
                    .square(c.move().to())
                    .explanation("Nach dem Tausch " + trade + " besitzt nur noch du das Läuferpaar.")
                    .build());
        }
        if (Math.abs(materialBefore) >= 2 && ours != PieceKind.PAWN) {
            boolean ahead = materialBefore > 0;
            FindingBuilder b = FindingBuilder.of(this).heuristic().severity(Severity.LOW)
                    .title(ahead ? "Vereinfachung mit Materialvorteil" : "Tausch im Materialnachteil")
                    .square(c.move().to())
                    .explanation("Du tauschst " + trade + (ahead
                            ? ", während du materiell vorne liegst. Jeder Figurentausch macht deinen Vorteil relativ größer und verringert gegnerisches Gegenspiel."
                            : ", obwohl du materiell zurückliegst. Mit weniger Figuren auf dem Brett wird Gegenspiel schwieriger."));
            findings.add((ahead ? b.positive() : b.negative()).build());
        }
        for (int bishop : afterExchange.squaresOf(c.user(), PieceKind.BISHOP)) {
            double share = StrategicFeatures.pawnsOnBishopColor(afterExchange, bishop);
            if (afterExchange.count(c.user(), PieceKind.BISHOP) == 1 && share >= 0.6
                    && afterExchange.squaresOf(c.user(), PieceKind.PAWN).size() >= 4 && ours == PieceKind.BISHOP) {
                findings.add(FindingBuilder.of(this).negative().heuristic().severity(Severity.LOW)
                        .title("Schlechter Läufer bleibt übrig")
                        .square(bishop)
                        .explanation("Nach dem Tausch bleibt dir der Läufer auf " + Squares.name(bishop) + ", aber "
                                + Math.round(share * 100) + " % deiner Bauern stehen auf seiner Feldfarbe und schränken ihn ein.")
                        .build());
            }
        }
        int ourMobility = c.before().mobility(c.move().from());
        int theirMobility = c.before().withSideToMove(c.opponent()).mobility(c.move().to());
        if (ours == theirs && theirMobility - ourMobility >= 4) {
            findings.add(FindingBuilder.of(this).positive().heuristic().severity(Severity.LOW)
                    .title("Aktive gegnerische Figur getauscht")
                    .square(c.move().to())
                    .explanation("Du tauschst eine passive Figur (" + ourMobility + " Felder) gegen eine aktive gegnerische Figur ("
                            + theirMobility + " Felder).")
                    .build());
        } else if (ours == theirs && ourMobility - theirMobility >= 4) {
            findings.add(FindingBuilder.of(this).negative().heuristic().severity(Severity.LOW)
                    .title("Aktive eigene Figur getauscht")
                    .square(c.move().to())
                    .explanation("Du tauschst eine aktive Figur (" + ourMobility + " Felder) gegen eine passive gegnerische Figur ("
                            + theirMobility + " Felder). Prüfe, ob du die aktive Figur behalten kannst.")
                    .build());
        }
        return findings;
    }
}
