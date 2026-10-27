package de.chesscoach.dto.request;

import jakarta.validation.constraints.Min;

/** Anfrage, die sich auf eine bestimmte Positionsversion bezieht (Computerzug, Undo, Redo, Aufgabe). */
public record VersionRequest(@Min(0) Integer expectedPositionVersion) {
}
