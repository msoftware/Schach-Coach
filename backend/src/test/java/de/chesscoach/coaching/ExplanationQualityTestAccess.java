package de.chesscoach.coaching;

/** Stellt die vollständig bestückten Regel-Engines aus den Coaching-Tests für andere Testpakete bereit. */
public final class ExplanationQualityTestAccess {

    private ExplanationQualityTestAccess() {
    }

    public static TacticalRuleEngine tactical() {
        return ExplanationQualityTest.TACTICAL;
    }

    public static StrategicRuleEngine strategic() {
        return ExplanationQualityTest.STRATEGIC;
    }
}
