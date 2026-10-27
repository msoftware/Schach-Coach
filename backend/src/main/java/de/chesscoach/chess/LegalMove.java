package de.chesscoach.chess;

import de.chesscoach.domain.position.PieceKind;

/**
 * Ein von Chesslib als legal bestätigter Zug.
 *
 * @param uci       Zug in UCI-Notation (z. B. "e7e8q")
 * @param san       Zug in algebraischer Notation (SAN) inklusive Schach-/Mattsymbol
 * @param from      Startfeld als Name (z. B. "e7")
 * @param to        Zielfeld als Name
 * @param promotion Umwandlungsfigur oder {@code null}
 * @param capture   true bei Schlagzügen (inklusive En passant)
 * @param check     true, wenn der Zug Schach gibt
 */
public record LegalMove(String uci, String san, String from, String to, PieceKind promotion,
                        boolean capture, boolean check) {
}
