package de.chesscoach.coaching;

import de.chesscoach.domain.coaching.CoachingFinding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Wertet alle aktivierten taktischen Regeln (Ebene 1) aus. */
@Component
public class TacticalRuleEngine {

    private static final Logger log = LoggerFactory.getLogger(TacticalRuleEngine.class);

    private final List<TacticalRule> rules;
    private final CoachingProperties properties;

    public TacticalRuleEngine(List<TacticalRule> rules, CoachingProperties properties) {
        this.rules = rules.stream().sorted(Comparator.comparing(CoachingRule::id)).toList();
        this.properties = properties;
    }

    public List<TacticalRule> rules() {
        return rules;
    }

    public List<CoachingFinding> evaluate(PositionContext context) {
        return RuleEvaluation.run(rules, properties, context, log);
    }

    /** Gemeinsame, fehlertolerante Auswertung: Eine fehlerhafte Regel darf die Analyse nicht abbrechen. */
    static final class RuleEvaluation {
        private RuleEvaluation() {
        }

        static List<CoachingFinding> run(List<? extends CoachingRule> rules, CoachingProperties properties,
                                         PositionContext context, Logger log) {
            List<CoachingFinding> findings = new ArrayList<>();
            for (CoachingRule rule : rules) {
                if (properties.disabledRules().contains(rule.id())) {
                    continue;
                }
                try {
                    if (rule.matches(context)) {
                        findings.addAll(rule.evaluate(context));
                    }
                } catch (RuntimeException e) {
                    log.warn("Regel {} konnte nicht ausgewertet werden: {}", rule.id(), e.toString());
                }
            }
            return findings;
        }
    }
}
