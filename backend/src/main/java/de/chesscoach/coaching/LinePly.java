package de.chesscoach.coaching;

import de.chesscoach.domain.position.Color;
import de.chesscoach.domain.position.MoveInfo;
import de.chesscoach.domain.position.Piece;
import de.chesscoach.domain.position.Position;

/** Ein Halbzug einer geprüften Variante mit Stellung davor und danach. */
public record LinePly(MoveInfo move, Color mover, Piece moved, Piece captured, Position before, Position after) {

    public boolean isCapture() {
        return captured != null;
    }

    /** SAN mit Zugnummer, z. B. "12.Nf3" oder "12...Nf6". */
    public String numberedSan() {
        int number = before.fullmoveNumber();
        return mover == Color.WHITE ? number + "." + move.san() : number + "..." + move.san();
    }
}
