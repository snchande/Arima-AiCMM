package org.aicmm.scoring;

import org.aicmm.model.AgencyLevel;

/**
 * The Agency Qualification Layer classifier.
 *
 * <p>Derives an {@link AgencyLevel} (the 13th, position-12 dimension) from the twelve
 * core AiCMM dimension scores (each 0-5). The 12 dimensions are never modified; this
 * is a post-evaluation, evidence-derived classification.
 *
 * <p>Algorithm (single source of truth, mirrored in the site API and web client):
 * <pre>
 *   minCore = min(autonomy, reasoning, memory, learning)
 *   trustOk = governancePass AND safety &gt;= 3 AND explainability &gt;= 3
 *
 *   if autonomy &lt; 2 OR reasoning &lt; 2:                         level = (reasoning &gt;= 1) ? -1 : -2
 *   elif NOT trustOk:                                            level = 0
 *   elif embodiment &gt;= 5 AND minCore &gt;= 5 AND avg12 &gt;= 4.8:  level = 5
 *   elif minCore &gt;= 5 AND avgExclEmbodiment &gt;= 4.5:           level = 4
 *   elif avg12 &gt;= 4.0 AND reasoning &gt;= 4 AND autonomy &gt;= 4 AND memory &gt;= 4: level = 3
 *   elif reasoning &gt;= 4 AND safety &gt;= 3 AND explainability &gt;= 3 AND avg12 &gt;= 3.0: level = 2
 *   else:                                                        level = 1
 * </pre>
 */
public class AgencyClassifier {

    /** The twelve core dimension scores (each 0-5) in fixed position order 0-11. */
    public record Scores(
            int autonomy,
            int reasoning,
            int memory,
            int learning,
            int toolUse,
            int collaboration,
            int embodiment,
            int explainability,
            int safety,
            int interoperability,
            int costEfficiency,
            int domainAlignment
    ) {
        public double average() {
            return (autonomy + reasoning + memory + learning + toolUse + collaboration
                    + embodiment + explainability + safety + interoperability
                    + costEfficiency + domainAlignment) / 12.0;
        }

        /**
         * Average of the eleven non-embodiment dimensions. Used for the human-level tier,
         * so a purely digital agent (embodiment = 0) is not penalised for lacking a body.
         */
        public double averageExcludingEmbodiment() {
            return (autonomy + reasoning + memory + learning + toolUse + collaboration
                    + explainability + safety + interoperability
                    + costEfficiency + domainAlignment) / 11.0;
        }
    }

    /**
     * Weighted contribution of each dimension to the continuous {@link #agencyIndex(Scores)}
     * "barometer" reading, in fixed position order 0-11. Agentic drivers (autonomy, reasoning,
     * tool use) carry the most weight; embodiment and cost efficiency the least. Sums to 1.0.
     */
    private static final double[] INDEX_WEIGHTS = {
            0.20, // 0 autonomy
            0.18, // 1 reasoning
            0.10, // 2 memory
            0.08, // 3 learning
            0.12, // 4 toolUse
            0.07, // 5 collaboration
            0.02, // 6 embodiment
            0.07, // 7 explainability
            0.07, // 8 safety
            0.03, // 9 interoperability
            0.02, // 10 costEfficiency
            0.04  // 11 domainAlignment
    };

    /** Per-level [lo, hi] average-score bands used to position the barometer needle within a band. */
    private static final double[][] NEEDLE_BANDS = {
            {1.2, 2.5}, // level 0
            {2.0, 3.0}, // level 1
            {3.0, 4.0}, // level 2
            {4.0, 4.5}, // level 3
            {4.5, 4.9}, // level 4
            {4.9, 5.01} // level 5
    };

    /**
     * Result of an agency assessment.
     *
     * @param agencyLevel    the derived ladder level (the authoritative band)
     * @param governancePass whether all seven governance rules passed
     * @param rationale      human-readable explanation of why this level was assigned
     * @param index          weighted "Agency Index" barometer reading, 0-100
     * @param needle         continuous needle position on the signed -2..+5 ladder
     */
    public record Assessment(AgencyLevel agencyLevel, boolean governancePass, String rationale,
                             int index, double needle) {
        public int level() {
            return agencyLevel.getLevel();
        }

        public boolean isAgent() {
            return agencyLevel.isAgent();
        }
    }

    /**
     * Weighted "Agency Index" barometer reading (0-100) derived from the twelve scores. Unlike the
     * discrete {@link AgencyLevel} band, this is a continuous measure of how agentic a system is,
     * emphasising the agentic drivers (see {@link #INDEX_WEIGHTS}). It expresses momentum within a
     * band — how close a system is to scaling up to (or down from) the next ladder level.
     */
    public int agencyIndex(Scores s) {
        int[] v = {
                s.autonomy(), s.reasoning(), s.memory(), s.learning(), s.toolUse(), s.collaboration(),
                s.embodiment(), s.explainability(), s.safety(), s.interoperability(),
                s.costEfficiency(), s.domainAlignment()
        };
        double acc = 0;
        for (int i = 0; i < INDEX_WEIGHTS.length; i++) {
            acc += INDEX_WEIGHTS[i] * v[i];
        }
        return (int) Math.round(100.0 * acc / 5.0);
    }

    /** Continuous needle position on the signed -2..+5 ladder for the given level and scores. */
    private double needlePosition(int level, double avg) {
        if (level < 0) {
            return level + 0.5;
        }
        double[] band = NEEDLE_BANDS[level];
        double f = (avg - band[0]) / (band[1] - band[0]);
        f = Math.max(0.0, Math.min(0.96, f));
        return level + f;
    }

    /** Classify a system into an {@link AgencyLevel} from its twelve core scores. */
    public Assessment classify(Scores s) {
        boolean governancePass = governancePasses(s);
        int minCore = min(s.autonomy(), s.reasoning(), s.memory(), s.learning());
        boolean trustOk = governancePass && s.safety() >= 3 && s.explainability() >= 3;
        double avg = s.average();

        AgencyLevel level;
        String rationale;

        if (s.autonomy() < 2 || s.reasoning() < 2) {
            if (s.reasoning() >= 1) {
                level = AgencyLevel.REACTIVE_ASSISTANT;
                rationale = "Fails the agent threshold (Autonomy >= 2 and Reasoning >= 2): reactive, "
                        + "AI-driven responses with no self-directed goals.";
            } else {
                level = AgencyLevel.SCRIPTED_AUTOMATION;
                rationale = "Fails the agent threshold with no reasoning (Reasoning = 0): a deterministic, "
                        + "pre-scripted automation.";
            }
        } else if (!trustOk) {
            level = AgencyLevel.PROTO_AGENT;
            rationale = "Clears the cognitive thresholds but is not yet trustworthy "
                    + "(governance " + (governancePass ? "passes" : "fails")
                    + ", Safety=" + s.safety() + ", Explainability=" + s.explainability() + ").";
        } else if (s.embodiment() >= 5 && minCore >= 5 && avg >= 4.8) {
            level = AgencyLevel.HUMANOID_AGENT;
            rationale = "Full embodiment mastery atop human-level cognition — indistinguishable from a human.";
        } else if (minCore >= 5 && s.averageExcludingEmbodiment() >= 4.5) {
            level = AgencyLevel.HUMAN_LEVEL_AGENT;
            rationale = "Masters the full cognitive core (>=5) with broad, robust, governed capability.";
        } else if (avg >= 4.0 && s.reasoning() >= 4 && s.autonomy() >= 4 && s.memory() >= 4) {
            level = AgencyLevel.GENERALIZED_AGENT;
            rationale = "State-of-the-art across cognitive core and trust dimensions (avg "
                    + String.format("%.1f", avg) + ").";
        } else if (s.reasoning() >= 4 && s.safety() >= 3 && s.explainability() >= 3 && avg >= 3.0) {
            level = AgencyLevel.ADVANCED_AGENT;
            rationale = "Expert reasoning with strong trust controls and governance compliance.";
        } else {
            level = AgencyLevel.BASIC_AGENT;
            rationale = "Balanced, governed agent that clears the cognitive thresholds within a bounded domain.";
        }

        return new Assessment(level, governancePass, rationale,
                agencyIndex(s), needlePosition(level.getLevel(), avg));
    }

    /** Evaluate the seven AiCMM governance rules against the scores. */
    public boolean governancePasses(Scores s) {
        return s.autonomy() <= s.reasoning() + 1
                && (s.autonomy() < 4 || s.explainability() >= 3)
                && (s.autonomy() < 4 || s.safety() >= 3)
                && (s.collaboration() < 4 || s.interoperability() >= 3)
                && (s.toolUse() < 4 || s.costEfficiency() >= 2)
                && (s.embodiment() < 3 || s.domainAlignment() >= 3)
                && (s.toolUse() < 4 || s.reasoning() >= 3);
    }

    private static int min(int... values) {
        int m = Integer.MAX_VALUE;
        for (int v : values) {
            m = Math.min(m, v);
        }
        return m;
    }
}
