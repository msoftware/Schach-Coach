package de.chesscoach.coaching;

import de.chesscoach.coaching.strategic.LongTermPlanRule;
import de.chesscoach.domain.coaching.CoachingFinding;
import de.chesscoach.domain.coaching.StrategicPlan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/** Wertet alle aktivierten strategischen Regeln (Ebene 2) aus und leitet langfristige Pläne ab. */
@Component
public class StrategicRuleEngine {

    private static final Logger log = LoggerFactory.getLogger(StrategicRuleEngine.class);

    private final List<StrategicRule> rules;
    private final CoachingProperties properties;

    public StrategicRuleEngine(List<StrategicRule> rules, CoachingProperties properties) {
        this.rules = rules.stream().sorted(Comparator.comparing(CoachingRule::id)).toList();
        this.properties = properties;
    }

    public List<StrategicRule> rules() {
        return rules;
    }

    public List<CoachingFinding> evaluate(PositionContext context) {
        return TacticalRuleEngine.RuleEvaluation.run(rules, properties, context, log);
    }

    public List<StrategicPlan> plans(PositionContext context) {
        if (properties.disabledRules().contains("STRAT-011")) {
            return List.of();
        }
        for (StrategicRule rule : rules) {
            if (rule instanceof LongTermPlanRule planRule) {
                try {
                    return planRule.plans(context);
                } catch (RuntimeException e) {
                    log.warn("Pläne konnten nicht abgeleitet werden: {}", e.toString());
                    return List.of();
                }
            }
        }
        return List.of();
    }
}
