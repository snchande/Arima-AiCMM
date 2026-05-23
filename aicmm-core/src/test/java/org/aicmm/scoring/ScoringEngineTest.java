package org.aicmm.scoring;

import org.aicmm.model.CapabilityProfile;
import org.aicmm.model.Dimension;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ScoringEngineTest {

    private final ScoringEngine engine = new ScoringEngine();

    @Test
    void testValidProfilePassesValidation() {
        CapabilityProfile profile = CapabilityProfile.builder()
                .score(Dimension.AUTONOMY, 3)
                .score(Dimension.DOMAIN_ALIGNMENT, 4)
                .score(Dimension.COLLABORATION, 3)
                .score(Dimension.EMBODIMENT, 0)
                .build();

        var result = engine.validate(profile);
        assertTrue(result.valid());
        assertTrue(result.violations().isEmpty());
    }

    @Test
    void testHighAutonomyLowAlignmentFails() {
        CapabilityProfile profile = CapabilityProfile.builder()
                .score(Dimension.AUTONOMY, 5)
                .score(Dimension.DOMAIN_ALIGNMENT, 2)
                .score(Dimension.COLLABORATION, 3)
                .score(Dimension.EMBODIMENT, 0)
                .build();

        var result = engine.validate(profile);
        assertFalse(result.valid());
        assertFalse(result.violations().isEmpty());
    }

    @Test
    void testEmbodiedAgentWarning() {
        CapabilityProfile profile = CapabilityProfile.builder()
                .score(Dimension.AUTONOMY, 3)
                .score(Dimension.DOMAIN_ALIGNMENT, 2)
                .score(Dimension.COLLABORATION, 3)
                .score(Dimension.EMBODIMENT, 4)
                .build();

        var result = engine.validate(profile);
        assertFalse(result.warnings().isEmpty());
    }

    @Test
    void testIdentifyBottleneck() {
        CapabilityProfile profile = CapabilityProfile.builder()
                .score(Dimension.AUTONOMY, 4)
                .score(Dimension.REASONING, 4)
                .score(Dimension.LEARNING, 1)
                .score(Dimension.MEMORY, 3)
                .score(Dimension.TOOL_USE, 3)
                .score(Dimension.COLLABORATION, 3)
                .score(Dimension.EMBODIMENT, 2)
                .score(Dimension.DOMAIN_ALIGNMENT, 4)
                .build();

        assertEquals(Dimension.LEARNING, engine.identifyBottleneck(profile));
    }
}
