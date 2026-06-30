package org.aicmm.scoring;

import org.aicmm.scoring.AgencyClassifier.Scores;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Foundational integrity tests for the seven AiCMM governance rules.
 *
 * <p>These tests pin the exact semantics of {@link AgencyClassifier#governancePasses}. They
 * exist to keep the framework's integrity: any contribution that silently weakens, removes,
 * or reorders a governance rule must fail here before it can reach a Pull Request. Each rule
 * is exercised at its pass/fail boundary in isolation, on top of an otherwise fully-compliant
 * baseline profile, so a failure points unambiguously at the rule that changed.
 */
class GovernanceRulesTest {

    private final AgencyClassifier classifier = new AgencyClassifier();

    /**
     * A baseline profile that passes all seven governance rules. Position order (0-11):
     * autonomy, reasoning, memory, learning, toolUse, collaboration, embodiment,
     * explainability, safety, interoperability, costEfficiency, domainAlignment.
     */
    private Scores baseline() {
        return new Scores(3, 3, 3, 3, 3, 3, 0, 3, 3, 3, 3, 3);
    }

    private Scores with(int autonomy, int reasoning, int memory, int learning, int toolUse,
                        int collaboration, int embodiment, int explainability, int safety,
                        int interoperability, int costEfficiency, int domainAlignment) {
        return new Scores(autonomy, reasoning, memory, learning, toolUse, collaboration,
                embodiment, explainability, safety, interoperability, costEfficiency, domainAlignment);
    }

    @Test
    void baselineProfilePassesAllSevenRules() {
        assertTrue(classifier.governancePasses(baseline()),
                "The baseline profile must satisfy every governance rule");
    }

    // Rule 1: Autonomy <= Reasoning + 1
    @Test
    void rule1_autonomyMayNotExceedReasoningByMoreThanOne() {
        Scores ok = with(4, 3, 3, 3, 3, 3, 0, 3, 3, 3, 3, 3); // 4 <= 3+1
        Scores bad = with(5, 3, 3, 3, 3, 3, 0, 3, 3, 3, 3, 3); // 5 > 3+1
        assertTrue(classifier.governancePasses(ok), "Autonomy == Reasoning + 1 must pass");
        assertFalse(classifier.governancePasses(bad), "Autonomy > Reasoning + 1 must fail");
    }

    // Rule 2: Autonomy >= 4 requires Explainability >= 3
    @Test
    void rule2_highAutonomyRequiresExplainability() {
        Scores ok = with(4, 3, 3, 3, 3, 3, 0, 3, 3, 3, 3, 3);
        Scores bad = with(4, 3, 3, 3, 3, 3, 0, 2, 3, 3, 3, 3);
        assertTrue(classifier.governancePasses(ok));
        assertFalse(classifier.governancePasses(bad), "Autonomy >= 4 with Explainability < 3 must fail");
    }

    // Rule 3: Autonomy >= 4 requires Safety >= 3
    @Test
    void rule3_highAutonomyRequiresSafety() {
        Scores ok = with(4, 3, 3, 3, 3, 3, 0, 3, 3, 3, 3, 3);
        Scores bad = with(4, 3, 3, 3, 3, 3, 0, 3, 2, 3, 3, 3);
        assertTrue(classifier.governancePasses(ok));
        assertFalse(classifier.governancePasses(bad), "Autonomy >= 4 with Safety < 3 must fail");
    }

    // Rule 4: Collaboration >= 4 requires Interoperability >= 3
    @Test
    void rule4_highCollaborationRequiresInteroperability() {
        Scores ok = with(3, 3, 3, 3, 3, 4, 0, 3, 3, 3, 3, 3);
        Scores bad = with(3, 3, 3, 3, 3, 4, 0, 3, 3, 2, 3, 3);
        assertTrue(classifier.governancePasses(ok));
        assertFalse(classifier.governancePasses(bad), "Collaboration >= 4 with Interoperability < 3 must fail");
    }

    // Rule 5: Tool Use >= 4 requires Cost Efficiency >= 2
    @Test
    void rule5_highToolUseRequiresCostEfficiency() {
        Scores ok = with(3, 3, 3, 3, 4, 3, 0, 3, 3, 3, 2, 3);
        Scores bad = with(3, 3, 3, 3, 4, 3, 0, 3, 3, 3, 1, 3);
        assertTrue(classifier.governancePasses(ok));
        assertFalse(classifier.governancePasses(bad), "Tool Use >= 4 with Cost Efficiency < 2 must fail");
    }

    // Rule 6: Embodiment >= 3 requires Domain Alignment >= 3
    @Test
    void rule6_embodimentRequiresDomainAlignment() {
        Scores ok = with(3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3);
        Scores bad = with(3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 2);
        assertTrue(classifier.governancePasses(ok));
        assertFalse(classifier.governancePasses(bad), "Embodiment >= 3 with Domain Alignment < 3 must fail");
    }

    // Rule 7: Tool Use >= 4 requires Reasoning >= 3
    @Test
    void rule7_highToolUseRequiresReasoning() {
        Scores ok = with(3, 3, 3, 3, 4, 3, 0, 3, 3, 3, 3, 3);
        Scores bad = with(2, 2, 3, 3, 4, 3, 0, 3, 3, 3, 3, 3); // autonomy capped to keep rule 1 satisfied
        assertTrue(classifier.governancePasses(ok));
        assertFalse(classifier.governancePasses(bad), "Tool Use >= 4 with Reasoning < 3 must fail");
    }
}
