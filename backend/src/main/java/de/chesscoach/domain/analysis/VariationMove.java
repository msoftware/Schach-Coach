package de.chesscoach.domain.analysis;

/**
 * Ein geprüfter Halbzug einer Variante.
 *
 * @param uci      Zug in UCI-Notation
 * @param san      Zug in SAN
 * @param numbered SAN mit Zugnummer (z. B. "12...Nf6")
 * @param fenAfter Stellung nach dem Zug (für die Variantenanzeige im Frontend)
 */
public record VariationMove(String uci, String san, String numbered, String fenAfter) {
}
