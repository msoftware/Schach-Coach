package de.chesscoach.domain.game;

import de.chesscoach.domain.position.Color;

import java.time.Instant;

/**
 * Ein ausgeführter Halbzug der Partie (unveränderlich).
 *
 * @param moveId    eindeutige Kennung des Zuges (bindet Analysen an genau diesen Zug)
 * @param ply       Halbzugnummer (1 = erster Zug der Partie)
 * @param uci       Zug in UCI-Notation
 * @param san       Zug in SAN
 * @param fenBefore Stellung vor dem Zug
 * @param fenAfter  Stellung nach dem Zug
 * @param player    ziehende Farbe
 * @param byUser    true für Benutzerzüge, false für Computerzüge
 * @param positionVersion Positionsversion der Partie nach diesem Zug
 * @param timestamp Zeitpunkt
 */
public record MoveRecord(String moveId, int ply, String uci, String san, String fenBefore, String fenAfter,
                         Color player, boolean byUser, int positionVersion, Instant timestamp) {
}
