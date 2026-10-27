package de.chesscoach.domain.coaching;

import java.util.List;

/** Abschnitt der Erklärung; beantwortet eine der sieben Leitfragen (Requirements 5.4). */
public record ExplanationSection(String key, String question, List<ExplanationStatement> statements) {

    public ExplanationSection {
        statements = statements == null ? List.of() : List.copyOf(statements);
    }
}
