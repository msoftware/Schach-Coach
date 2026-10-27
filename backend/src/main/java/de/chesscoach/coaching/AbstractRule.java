package de.chesscoach.coaching;

import java.util.List;

/** Basisklasse mit den beschreibenden Pflichtangaben einer Regel. */
public abstract class AbstractRule implements CoachingRule {

    private final String id;
    private final String title;
    private final String purpose;
    private final List<String> inputFeatures;
    private final String learningPrinciple;

    protected AbstractRule(String id, String title, String purpose, List<String> inputFeatures, String learningPrinciple) {
        this.id = id;
        this.title = title;
        this.purpose = purpose;
        this.inputFeatures = List.copyOf(inputFeatures);
        this.learningPrinciple = learningPrinciple;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public String title() {
        return title;
    }

    @Override
    public String purpose() {
        return purpose;
    }

    @Override
    public List<String> inputFeatures() {
        return inputFeatures;
    }

    @Override
    public String learningPrinciple() {
        return learningPrinciple;
    }

    @Override
    public boolean matches(PositionContext context) {
        return true;
    }

    /** Maximale Variantenlänge, die für Materialaussagen ausgewertet wird. */
    protected static final int MATERIAL_HORIZON = 12;
}
