package org.aicmm.model;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * A capability fingerprint representing an agent's scores across all eight dimensions.
 * This is the core data structure of the AiCMM framework.
 */
public record CapabilityProfile(Map<Dimension, DimensionScore> scores) {

    public CapabilityProfile {
        scores = Collections.unmodifiableMap(new EnumMap<>(scores));
    }

    /**
     * Get the score for a specific dimension.
     */
    public DimensionScore getScore(Dimension dimension) {
        return scores.get(dimension);
    }

    /**
     * Get the numeric score value for a specific dimension, or 0 if not scored.
     */
    public int getScoreValue(Dimension dimension) {
        DimensionScore ds = scores.get(dimension);
        return ds != null ? ds.score() : 0;
    }

    /**
     * Calculate the total score across all dimensions (max 40).
     */
    public int totalScore() {
        return scores.values().stream()
                .mapToInt(DimensionScore::score)
                .sum();
    }

    /**
     * Calculate the average score across all scored dimensions.
     */
    public double averageScore() {
        if (scores.isEmpty()) return 0.0;
        return (double) totalScore() / scores.size();
    }

    /**
     * Builder for constructing a CapabilityProfile.
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final EnumMap<Dimension, DimensionScore> scores = new EnumMap<>(Dimension.class);

        public Builder score(Dimension dimension, int score) {
            scores.put(dimension, new DimensionScore(dimension, score));
            return this;
        }

        public Builder score(Dimension dimension, int score, String evidence) {
            scores.put(dimension, new DimensionScore(dimension, score, evidence));
            return this;
        }

        public Builder score(DimensionScore dimensionScore) {
            scores.put(dimensionScore.dimension(), dimensionScore);
            return this;
        }

        public CapabilityProfile build() {
            return new CapabilityProfile(scores);
        }
    }
}
