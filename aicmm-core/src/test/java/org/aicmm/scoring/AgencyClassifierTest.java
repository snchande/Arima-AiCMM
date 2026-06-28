package org.aicmm.scoring;

import org.aicmm.model.AgencyLevel;
import org.aicmm.scoring.AgencyClassifier.Scores;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgencyClassifierTest {

    private final AgencyClassifier classifier = new AgencyClassifier();

    private Scores scores(int a, int r, int m, int l, int t, int c, int em,
                          int x, int s, int i, int co, int d) {
        return new Scores(a, r, m, l, t, c, em, x, s, i, co, d);
    }

    @Test
    void scriptedAutomationIsLevelMinusTwo() {
        // RPA macro: no autonomy, no reasoning
        var result = classifier.classify(scores(0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0));
        assertEquals(AgencyLevel.SCRIPTED_AUTOMATION, result.agencyLevel());
        assertEquals(-2, result.level());
        assertFalse(result.isAgent());
    }

    @Test
    void reactiveAssistantIsLevelMinusOne() {
        // FAQ bot: some pattern-matching reasoning but no autonomy
        var result = classifier.classify(scores(1, 1, 0, 0, 1, 1, 0, 1, 1, 1, 1, 1));
        assertEquals(AgencyLevel.REACTIVE_ASSISTANT, result.agencyLevel());
        assertEquals(-1, result.level());
        assertFalse(result.isAgent());
    }

    @Test
    void autoGptIsProtoAgent() {
        // High autonomy/reasoning but weak safety & explainability -> not trustworthy
        var result = classifier.classify(scores(5, 4, 2, 1, 4, 2, 0, 2, 2, 2, 2, 2));
        assertEquals(AgencyLevel.PROTO_AGENT, result.agencyLevel());
        assertEquals(0, result.level());
        assertTrue(result.isAgent());
    }

    @Test
    void balancedAgentIsBasic() {
        // Clears thresholds, passes governance, moderate reasoning
        var result = classifier.classify(scores(3, 3, 3, 2, 3, 3, 0, 3, 3, 3, 2, 3));
        assertEquals(AgencyLevel.BASIC_AGENT, result.agencyLevel());
        assertEquals(1, result.level());
    }

    @Test
    void copilotCliIsAdvancedAgent() {
        // GitHub Copilot CLI profile from examples
        var result = classifier.classify(scores(4, 4, 3, 2, 5, 3, 0, 4, 3, 4, 2, 4));
        assertEquals(AgencyLevel.ADVANCED_AGENT, result.agencyLevel());
        assertEquals(2, result.level());
        assertTrue(result.governancePass());
    }

    @Test
    void cappedAutonomyExpertIsAdvancedAgent() {
        // MedAssist-style: capped autonomy (3) but top reasoning/safety/explainability
        var result = classifier.classify(scores(3, 5, 4, 3, 4, 4, 0, 5, 5, 4, 3, 5));
        assertEquals(AgencyLevel.ADVANCED_AGENT, result.agencyLevel());
        assertEquals(2, result.level());
    }

    @Test
    void topTierIsGeneralizedAgent() {
        var result = classifier.classify(scores(4, 5, 4, 4, 5, 4, 2, 4, 4, 4, 4, 4));
        assertEquals(AgencyLevel.GENERALIZED_AGENT, result.agencyLevel());
        assertEquals(3, result.level());
    }

    @Test
    void humanLevelAgent() {
        var result = classifier.classify(scores(5, 5, 5, 5, 5, 5, 0, 5, 5, 5, 4, 4));
        assertEquals(AgencyLevel.HUMAN_LEVEL_AGENT, result.agencyLevel());
        assertEquals(4, result.level());
    }

    @Test
    void humanoidAgent() {
        var result = classifier.classify(scores(5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5));
        assertEquals(AgencyLevel.HUMANOID_AGENT, result.agencyLevel());
        assertEquals(5, result.level());
    }

    @Test
    void fromLevelRoundTrips() {
        for (AgencyLevel a : AgencyLevel.values()) {
            assertEquals(a, AgencyLevel.fromLevel(a.getLevel()));
        }
    }
}
