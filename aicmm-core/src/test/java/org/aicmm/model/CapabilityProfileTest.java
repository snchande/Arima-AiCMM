package org.aicmm.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CapabilityProfileTest {

    @Test
    void testBuilderCreatesValidProfile() {
        CapabilityProfile profile = CapabilityProfile.builder()
                .score(Dimension.AUTONOMY, 4, "Can decompose goals into sub-tasks")
                .score(Dimension.REASONING, 3)
                .score(Dimension.LEARNING, 2)
                .score(Dimension.MEMORY, 3)
                .score(Dimension.TOOL_USE, 5, "Orchestrates 30+ tools")
                .score(Dimension.COLLABORATION, 3)
                .score(Dimension.EMBODIMENT, 0)
                .score(Dimension.DOMAIN_ALIGNMENT, 4)
                .build();

        assertEquals(4, profile.getScoreValue(Dimension.AUTONOMY));
        assertEquals(5, profile.getScoreValue(Dimension.TOOL_USE));
        assertEquals(0, profile.getScoreValue(Dimension.EMBODIMENT));
        assertEquals(24, profile.totalScore());
    }

    @Test
    void testScoreValidation() {
        assertThrows(IllegalArgumentException.class, () ->
                new DimensionScore(Dimension.AUTONOMY, -1));
        assertThrows(IllegalArgumentException.class, () ->
                new DimensionScore(Dimension.AUTONOMY, 6));
    }

    @Test
    void testAverageScore() {
        CapabilityProfile profile = CapabilityProfile.builder()
                .score(Dimension.AUTONOMY, 4)
                .score(Dimension.REASONING, 4)
                .build();

        assertEquals(4.0, profile.averageScore(), 0.001);
    }
}
