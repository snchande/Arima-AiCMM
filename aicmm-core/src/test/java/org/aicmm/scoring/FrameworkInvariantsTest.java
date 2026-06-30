package org.aicmm.scoring;

import org.aicmm.model.AgencyLevel;
import org.aicmm.scoring.AgencyClassifier.Scores;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Foundational integrity tests for the structural invariants of the AiCMM framework.
 *
 * <p>These tests pin the things that define AiCMM and must not drift silently: the twelve
 * core dimension positions, the signed Agency Qualification ladder (-2..+5), the stable
 * serialization codes, the agent threshold, and the bounded Agency Index. Any contribution
 * that changes the shape of the framework must update — and consciously justify — these
 * assertions before it can reach a Pull Request.
 */
class FrameworkInvariantsTest {

    private final AgencyClassifier classifier = new AgencyClassifier();

    @Test
    void coreProfileHasExactlyTwelveDimensions() {
        assertEquals(12, Scores.class.getRecordComponents().length,
                "AiCMM Level 0 must have exactly 12 core dimensions");
    }

    @Test
    void scoresAreOrderedAutonomyFirstDomainAlignmentLast() {
        var names = Arrays.stream(Scores.class.getRecordComponents())
                .map(java.lang.reflect.RecordComponent::getName).toList();
        assertEquals("autonomy", names.get(0), "Position 0 must be autonomy");
        assertEquals("reasoning", names.get(1), "Position 1 must be reasoning");
        assertEquals("domainAlignment", names.get(11), "Position 11 must be domainAlignment");
    }

    @Test
    void agencyLadderIsContiguousFromMinusTwoToFive() {
        int[] levels = Arrays.stream(AgencyLevel.values()).mapToInt(AgencyLevel::getLevel).sorted().toArray();
        assertEquals(8, levels.length, "The ladder must have exactly 8 rungs");
        for (int i = 0; i < levels.length; i++) {
            assertEquals(-2 + i, levels[i], "Ladder must be contiguous from -2 to +5");
        }
    }

    @Test
    void isAgentFlagMatchesSignOfLevel() {
        for (AgencyLevel a : AgencyLevel.values()) {
            assertEquals(a.getLevel() >= 0, a.isAgent(),
                    a.name() + " isAgent() must be true iff level >= 0");
        }
    }

    @Test
    void serializationCodesAreStable() {
        assertEquals("SCRIPTED_AUTOMATION", AgencyLevel.fromLevel(-2).getCode());
        assertEquals("REACTIVE_ASSISTANT", AgencyLevel.fromLevel(-1).getCode());
        assertEquals("PROTO_AGENT", AgencyLevel.fromLevel(0).getCode());
        assertEquals("BASIC_AGENT", AgencyLevel.fromLevel(1).getCode());
        assertEquals("ADVANCED_AGENT", AgencyLevel.fromLevel(2).getCode());
        assertEquals("GENERALIZED_AGENT", AgencyLevel.fromLevel(3).getCode());
        assertEquals("HUMAN_LEVEL_AGENT", AgencyLevel.fromLevel(4).getCode());
        assertEquals("HUMANOID_AGENT", AgencyLevel.fromLevel(5).getCode());
    }

    @Test
    void agentThresholdRequiresAutonomyAndReasoningAtLeastTwo() {
        // Below threshold on autonomy -> non-agent
        assertFalse(classifier.classify(new Scores(1, 4, 3, 3, 3, 3, 0, 3, 3, 3, 3, 3)).isAgent());
        // Below threshold on reasoning -> non-agent
        assertFalse(classifier.classify(new Scores(4, 1, 3, 3, 3, 3, 0, 3, 3, 3, 3, 3)).isAgent());
        // At threshold -> agent
        assertTrue(classifier.classify(new Scores(2, 2, 3, 3, 3, 3, 0, 3, 3, 3, 3, 3)).isAgent());
    }

    @Test
    void noReasoningIsScriptedReasoningWithoutAutonomyIsReactive() {
        assertEquals(AgencyLevel.SCRIPTED_AUTOMATION,
                classifier.classify(new Scores(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)).agencyLevel());
        assertEquals(AgencyLevel.REACTIVE_ASSISTANT,
                classifier.classify(new Scores(1, 1, 1, 1, 1, 1, 0, 1, 1, 1, 1, 1)).agencyLevel());
    }

    @Test
    void agencyIndexIsBoundedZeroToHundred() {
        assertEquals(0, classifier.agencyIndex(new Scores(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)));
        assertEquals(100, classifier.agencyIndex(new Scores(5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5)));
        int mid = classifier.agencyIndex(new Scores(3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3));
        assertTrue(mid >= 0 && mid <= 100, "Agency Index must always fall within [0, 100]");
    }

    @Test
    void averagesMatchTwelveAndElevenDimensionDenominators() {
        Scores all3 = new Scores(3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3);
        assertEquals(3.0, all3.average(), 1e-9);
        assertEquals(3.0, all3.averageExcludingEmbodiment(), 1e-9);
        // Embodiment is excluded from the 11-dimension average used for the human-level tier.
        Scores embodimentOnly = new Scores(0, 0, 0, 0, 0, 0, 5, 0, 0, 0, 0, 0);
        assertEquals(5.0 / 12.0, embodimentOnly.average(), 1e-9);
        assertEquals(0.0, embodimentOnly.averageExcludingEmbodiment(), 1e-9);
    }
}
