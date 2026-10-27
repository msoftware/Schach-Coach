package de.chesscoach.domain.coaching;

import java.util.List;

/** Ein Satz der Erklärung mit seinen Quellen (Regel-IDs oder "ENGINE"), damit jede Aussage zurückverfolgbar ist. */
public record ExplanationStatement(String text, List<String> sources) {

    public ExplanationStatement {
        sources = sources == null ? List.of() : List.copyOf(sources);
    }
}
