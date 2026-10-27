package de.chesscoach.application.analysis;

import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicBoolean;

/** Kooperativer Abbruch einer laufenden Analyse. */
public final class CancellationToken {

    private final AtomicBoolean cancelled = new AtomicBoolean();

    public static CancellationToken none() {
        return new CancellationToken();
    }

    public void cancel() {
        cancelled.set(true);
    }

    public boolean isCancelled() {
        return cancelled.get();
    }

    public void throwIfCancelled() {
        if (cancelled.get()) {
            throw new CancellationException("Analyse wurde abgebrochen");
        }
    }
}
