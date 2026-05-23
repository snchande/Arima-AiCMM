package org.aicmm.scoring;

import org.aicmm.model.CapabilityProfile;
import org.aicmm.model.Dimension;
import org.aicmm.model.DimensionScore;

import java.util.EnumMap;
import java.util.Map;

/**
 * Engine for scoring agents against the AiCMM framework.
 * Provides validation, analysis, and governance recommendations.
 */
public class ScoringEngine {

    /**
     * Validate that a capability profile meets minimum governance requirements.
     * Key rule: higher autonomy must be paired with proportional domain alignment.
     *
     * @return validation result with any warnings or violations
     */
    public ValidationResult validate(CapabilityProfile profile) {
        var warnings = new java.util.ArrayList<String>();
        var violations = new java.util.ArrayList<String>();

        int autonomy = profile.getScoreValue(Dimension.AUTONOMY);
        int alignment = profile.getScoreValue(Dimension.DOMAIN_ALIGNMENT);

        // Core governance rule: autonomy should not exceed domain alignment by more than 1
        if (autonomy > alignment + 1) {
            violations.add(String.format(
                    "Autonomy (%d) exceeds Domain Alignment (%d) by more than 1. " +
                    "Higher autonomy requires stronger governance controls.", autonomy, alignment));
        }

        // Warning if high autonomy with low collaboration (no human-in-the-loop)
        int collaboration = profile.getScoreValue(Dimension.COLLABORATION);
        if (autonomy >= 4 && collaboration <= 1) {
            warnings.add(String.format(
                    "High Autonomy (%d) with low Collaboration (%d) — consider adding " +
                    "human-in-the-loop or multi-agent oversight.", autonomy, collaboration));
        }

        // Warning if embodied agent lacks strong domain alignment
        int embodiment = profile.getScoreValue(Dimension.EMBODIMENT);
        if (embodiment >= 3 && alignment < 3) {
            warnings.add(String.format(
                    "Significant Embodiment (%d) with modest Domain Alignment (%d) — " +
                    "physical agents require strong safety cases.", embodiment, alignment));
        }

        return new ValidationResult(violations.isEmpty(), violations, warnings);
    }

    /**
     * Identify the bottleneck dimension — the weakest link relative to the profile.
     */
    public Dimension identifyBottleneck(CapabilityProfile profile) {
        Dimension bottleneck = null;
        int minScore = Integer.MAX_VALUE;

        for (Dimension d : Dimension.values()) {
            int score = profile.getScoreValue(d);
            if (score < minScore) {
                minScore = score;
                bottleneck = d;
            }
        }
        return bottleneck;
    }

    /**
     * Result of validation including pass/fail, violations, and warnings.
     */
    public record ValidationResult(
            boolean valid,
            java.util.List<String> violations,
            java.util.List<String> warnings
    ) {}
}
