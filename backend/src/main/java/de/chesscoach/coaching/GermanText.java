package de.chesscoach.coaching;

import de.chesscoach.domain.position.Piece;
import de.chesscoach.domain.position.Position;
import de.chesscoach.domain.position.Squares;

import java.util.Collection;
import java.util.List;

/** Hilfsfunktionen für deutschsprachige Formulierungen. */
public final class GermanText {

    private GermanText() {
    }

    /** "Springer f3" */
    public static String pieceOn(Position position, int square) {
        Piece piece = position.pieceAt(square);
        return piece == null ? Squares.name(square) : piece.kind().germanName() + " " + Squares.name(square);
    }

    /** "dein Springer f3" bzw. "deine Dame d1" */
    public static String yourPieceOn(Position position, int square) {
        Piece piece = position.pieceAt(square);
        if (piece == null) {
            return Squares.name(square);
        }
        return (piece.kind().feminine() ? "deine " : "dein ") + piece.kind().germanName() + " " + Squares.name(square);
    }

    /** "der gegnerische Springer f6" bzw. "die gegnerische Dame d8" */
    public static String opponentPieceOn(Position position, int square) {
        Piece piece = position.pieceAt(square);
        if (piece == null) {
            return Squares.name(square);
        }
        return (piece.kind().feminine() ? "die gegnerische " : "der gegnerische ") + piece.kind().germanName() + " "
                + Squares.name(square);
    }

    /** Akkusativ: "den gegnerischen Springer f6" bzw. "die gegnerische Dame d8" */
    public static String opponentPieceAcc(Position position, int square) {
        Piece piece = position.pieceAt(square);
        if (piece == null) {
            return Squares.name(square);
        }
        return (piece.kind().feminine() ? "die gegnerische " : "den gegnerischen ") + piece.kind().germanName() + " "
                + Squares.name(square);
    }

    /** Akkusativ: "deinen Springer f3" bzw. "deine Dame d1" */
    public static String yourPieceAcc(Position position, int square) {
        Piece piece = position.pieceAt(square);
        if (piece == null) {
            return Squares.name(square);
        }
        return (piece.kind().feminine() ? "deine " : "deinen ") + piece.kind().germanName() + " " + Squares.name(square);
    }

    /** Dativ: "dem gegnerischen Turm d8" bzw. "der gegnerischen Dame d8" */
    public static String opponentPieceDat(Position position, int square) {
        Piece piece = position.pieceAt(square);
        if (piece == null) {
            return Squares.name(square);
        }
        return (piece.kind().feminine() ? "der gegnerischen " : "dem gegnerischen ") + piece.kind().germanName() + " "
                + Squares.name(square);
    }

    /** Akkusativ mit bestimmtem Artikel: "den Turm a8" bzw. "die Dame d8". */
    public static String withArticleAcc(Position position, int square) {
        Piece piece = position.pieceAt(square);
        if (piece == null) {
            return Squares.name(square);
        }
        return (piece.kind().feminine() ? "die " : "den ") + piece.kind().germanName() + " " + Squares.name(square);
    }

    public static String capitalize(String text) {
        return text == null || text.isEmpty() ? text : Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }

    /** Material in Bauerneinheiten in einfachen Worten. */
    public static String material(int pawnUnits) {
        int units = Math.abs(pawnUnits);
        return switch (units) {
            case 0 -> "nichts";
            case 1 -> "einen Bauern";
            case 2 -> "so viel wie zwei Bauern";
            case 3 -> "so viel wie eine Figur (Springer oder Läufer)";
            case 5 -> "so viel wie einen Turm";
            case 9 -> "so viel wie eine Dame";
            default -> "so viel wie " + units + " Bauern";
        };
    }

    // ------------------------------------------------------------------
    // Einfache Sprache für Anfänger
    // ------------------------------------------------------------------

    private static final java.util.Map<de.chesscoach.domain.position.PieceKind, String[]> ARTICLES = java.util.Map.of(
            de.chesscoach.domain.position.PieceKind.PAWN, new String[]{"einen Bauern", "Bauern"},
            de.chesscoach.domain.position.PieceKind.KNIGHT, new String[]{"einen Springer", "Springer"},
            de.chesscoach.domain.position.PieceKind.BISHOP, new String[]{"einen Läufer", "Läufer"},
            de.chesscoach.domain.position.PieceKind.ROOK, new String[]{"einen Turm", "Türme"},
            de.chesscoach.domain.position.PieceKind.QUEEN, new String[]{"die Dame", "Damen"},
            de.chesscoach.domain.position.PieceKind.KING, new String[]{"den König", "Könige"});

    private static final String[] NUMBERS = {"null", "einen", "zwei", "drei", "vier", "fünf", "sechs", "sieben", "acht"};

    /** Zug in Worten, z. B. "Springer von f3 schlägt Bauer auf e5" oder "Kurze Rochade". */
    public static String moveWords(LinePly ply) {
        Piece moved = ply.moved();
        if (moved == null) {
            return ply.move().san();
        }
        int from = ply.move().from();
        int to = ply.move().to();
        String text;
        if (moved.kind() == de.chesscoach.domain.position.PieceKind.KING
                && Math.abs(Squares.file(from) - Squares.file(to)) == 2) {
            text = Squares.file(to) == 6 ? "kurze Rochade" : "lange Rochade";
        } else if (ply.captured() != null) {
            text = moved.kind().germanName() + " von " + Squares.name(from) + " schlägt " + ply.captured().kind().germanName()
                    + " auf " + Squares.name(to);
        } else {
            text = moved.kind().germanName() + " von " + Squares.name(from) + " nach " + Squares.name(to);
        }
        if (ply.move().promotion() != null) {
            text += " und wird zur " + (ply.move().promotion() == de.chesscoach.domain.position.PieceKind.QUEEN ? "Dame"
                    : ply.move().promotion().germanName());
        }
        String san = ply.move().san();
        if (san.endsWith("#")) {
            text += " – Schachmatt";
        } else if (san.endsWith("+")) {
            text += " mit Schach";
        }
        return capitalize(text);
    }

    /** Zug in Worten für einen legalen Zug in der angegebenen Stellung, mit Kurznotation in Klammern. */
    public static String moveWithNotation(Position before, de.chesscoach.domain.position.MoveInfo move) {
        LinePly ply = EngineLine.of("text", before, List.of(move), false).ply(0);
        return moveWords(ply) + " (" + move.san() + ")";
    }

    /** Zug in Worten mit Kurznotation in Klammern, z. B. "Springer von f3 nach e5 (3.Nxe5)". */
    public static String moveWithNotation(LinePly ply) {
        return moveWords(ply) + " (" + ply.numberedSan() + ")";
    }

    /** Wer zieht – aus Sicht des Benutzers. */
    public static String who(LinePly ply, de.chesscoach.domain.position.Color user) {
        return ply.mover() == user ? "du" : "der Gegner";
    }

    /**
     * Fortsetzung in einfachen Worten, z. B. "der Gegner: Springer von c6 schlägt Springer auf e5 → du: Springer von b1
     * nach c3". Höchstens {@code count} Halbzüge ab {@code from}.
     */
    public static String lineWords(EngineLine line, int from, int count, de.chesscoach.domain.position.Color user) {
        List<String> parts = new java.util.ArrayList<>();
        int end = Math.min(line.size(), from + count);
        for (int i = from; i < end; i++) {
            LinePly ply = line.ply(i);
            parts.add((ply.mover() == user ? "du: " : "Gegner: ") + moveWords(ply));
        }
        return String.join(" → ", parts);
    }

    /** "einen Springer", "zwei Bauern", "einen Springer und einen Bauern". */
    public static String pieces(List<de.chesscoach.domain.position.PieceKind> kinds) {
        if (kinds.isEmpty()) {
            return "nichts";
        }
        java.util.Map<de.chesscoach.domain.position.PieceKind, Integer> counts = new java.util.EnumMap<>(de.chesscoach.domain.position.PieceKind.class);
        kinds.forEach(k -> counts.merge(k, 1, Integer::sum));
        List<String> parts = new java.util.ArrayList<>();
        // Wertvollste Figuren zuerst nennen.
        List<de.chesscoach.domain.position.PieceKind> order = new java.util.ArrayList<>(counts.keySet());
        order.sort((a, b) -> Integer.compare(b.value(), a.value()));
        for (var kind : order) {
            int n = counts.get(kind);
            parts.add(n == 1 ? ARTICLES.get(kind)[0] : (n < NUMBERS.length ? NUMBERS[n] : String.valueOf(n)) + " " + ARTICLES.get(kind)[1]);
        }
        return join(parts);
    }

    /**
     * Beschreibt den Materialtausch einer Variante bis Halbzug {@code plies}, z. B. "Du verlierst einen Springer und
     * bekommst dafür nur einen Bauern." Leer, wenn nichts geschlagen wird.
     */
    public static String tradeWords(EngineLine line, de.chesscoach.domain.position.Color user, int plies) {
        return tradeWords(line, user, plies, null);
    }

    /**
     * Wie {@link #tradeWords(EngineLine, de.chesscoach.domain.position.Color, int)}; schlägt der erste Zug auf dem Feld
     * zurück, auf dem der Gegner gerade geschlagen hat, zählt die dort verlorene Figur mit. Sonst sähe ein bloßes
     * Zurückschlagen wie ein Gewinn aus („du gibst die Dame und bekommst Dame und Läufer“).
     */
    public static String tradeWords(EngineLine line, de.chesscoach.domain.position.Color user, int plies, LinePly previousOpponentMove) {
        List<de.chesscoach.domain.position.PieceKind> lost = new java.util.ArrayList<>();
        List<de.chesscoach.domain.position.PieceKind> won = new java.util.ArrayList<>();
        de.chesscoach.domain.position.PieceKind justLost = recapturedLoss(line, previousOpponentMove);
        if (justLost != null) {
            lost.add(justLost);
        }
        for (int i = 0; i < Math.min(plies, line.size()); i++) {
            LinePly ply = line.ply(i);
            if (ply.captured() == null) {
                continue;
            }
            (ply.mover() == user ? won : lost).add(ply.captured().kind());
        }
        if (lost.isEmpty() && won.isEmpty()) {
            return "";
        }
        String prefix = "";
        if (justLost != null) {
            // Akkusativ ohne Artikel, z. B. "Bauern", "Turm", "Dame".
            String noun = ARTICLES.get(justLost)[0].replaceFirst("^(einen|eine|die|den) ", "");
            prefix = "Rechnet man " + (justLost.feminine() ? "deine gerade geschlagene " : "deinen gerade geschlagenen ") + noun + " mit: ";
        }
        return prefix + tradeSentence(lost, won);
    }

    /** Figur, die der Gegner gerade auf dem Feld geschlagen hat, auf dem der erste Zug der Variante zurückschlägt. */
    public static de.chesscoach.domain.position.PieceKind recapturedLoss(EngineLine line, LinePly previousOpponentMove) {
        if (previousOpponentMove == null || !previousOpponentMove.isCapture() || line.size() == 0
                || !line.ply(0).isCapture() || line.ply(0).move().to() != previousOpponentMove.move().to()) {
            return null;
        }
        return previousOpponentMove.captured().kind();
    }

    private static String tradeSentence(List<de.chesscoach.domain.position.PieceKind> lost,
                                        List<de.chesscoach.domain.position.PieceKind> won) {
        int lostValue = lost.stream().mapToInt(de.chesscoach.domain.position.PieceKind::value).sum();
        int wonValue = won.stream().mapToInt(de.chesscoach.domain.position.PieceKind::value).sum();
        if (won.isEmpty()) {
            return "Du verlierst " + pieces(lost) + ", ohne etwas dafür zu bekommen.";
        }
        if (lost.isEmpty()) {
            return "Du gewinnst " + pieces(won) + ", ohne etwas dafür abzugeben.";
        }
        if (lostValue > wonValue) {
            return "Du gibst " + pieces(lost) + " ab und bekommst dafür nur " + pieces(won) + ".";
        }
        if (wonValue > lostValue) {
            return "Du gibst " + pieces(lost) + " ab und bekommst dafür " + pieces(won) + " – das lohnt sich für dich.";
        }
        return "Beide Seiten tauschen gleichwertig: Du gibst " + pieces(lost) + " und bekommst " + pieces(won) + ".";
    }

    /** Bewertung in Worten aus Sicht des Benutzers, z. B. "du stehst klar besser". */
    public static String evaluationWords(de.chesscoach.domain.analysis.Score score) {
        if (score == null) {
            return "unklar";
        }
        if (score.isMate()) {
            if (score.value() > 0) {
                return "du kannst in " + moves(score.value()) + " mattsetzen";
            }
            return score.value() == 0 ? "du bist matt" : "der Gegner kann dich in " + moves(-score.value()) + " mattsetzen";
        }
        int cp = score.value();
        int abs = Math.abs(cp);
        String side = cp > 0 ? "du" : "der Gegner";
        if (abs < 50) {
            return "die Stellung ist ausgeglichen";
        }
        if (abs < 120) {
            return side + (cp > 0 ? " stehst" : " steht") + " etwas besser";
        }
        if (abs < 250) {
            return side + (cp > 0 ? " stehst" : " steht") + " klar besser";
        }
        if (abs < 600) {
            return side + (cp > 0 ? " stehst" : " steht") + " deutlich besser – etwa so, als hätte "
                    + (cp > 0 ? "du" : "er") + " eine Figur mehr";
        }
        return side + (cp > 0 ? " stehst" : " steht") + " auf Gewinn";
    }

    /** Centipawns als ungefährer Bauernwert, z. B. 250 → "etwa 2½ Bauern". */
    public static String pawnsWords(int centipawns) {
        int halves = Math.round(Math.abs(centipawns) / 50f);
        if (halves <= 0) {
            return "fast nichts";
        }
        if (halves == 1) {
            return "etwa einen halben Bauern";
        }
        int whole = halves / 2;
        boolean half = halves % 2 == 1;
        String number = whole + (half ? "½" : "");
        return "etwa " + (whole == 1 && !half ? "einen Bauern" : number + " Bauern");
    }

    /** "1 Zug" bzw. "3 Zügen" (Dativ Plural) */
    public static String moves(int n) {
        return n == 1 ? "1 Zug" : n + " Zügen";
    }

    /** "+2", "0", "-1" */
    public static String balance(int balance) {
        return balance > 0 ? "+" + balance : String.valueOf(balance);
    }

    public static String join(Collection<String> items) {
        List<String> list = List.copyOf(items);
        if (list.isEmpty()) {
            return "";
        }
        if (list.size() == 1) {
            return list.get(0);
        }
        return String.join(", ", list.subList(0, list.size() - 1)) + " und " + list.get(list.size() - 1);
    }

    public static String squares(Collection<Integer> squares) {
        return join(squares.stream().map(Squares::name).toList());
    }
}
