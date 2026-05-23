package org.aicmm.model;

/**
 * A score for a single capability dimension, with optional evidence and constraints.
 *
 * @param dimension   the capability dimension being scored
 * @param score       integer score from 0 (absent) to 5 (state of the art)
 * @param evidence    observable evidence justifying this score
 * @param constraints intentional constraints on this dimension
 */
public record DimensionScore(
        Dimension dimension,
        int score,
        String evidence,
        String constraints
) {
    public DimensionScore {
        if (score < 0 || score > 5) {
            throw new IllegalArgumentException("Score must be between 0 and 5, got: " + score);
        }
    }

    public DimensionScore(Dimension dimension, int score) {
        this(dimension, score, null, null);
    }

    public DimensionScore(Dimension dimension, int score, String evidence) {
        this(dimension, score, evidence, null);
    }
}
