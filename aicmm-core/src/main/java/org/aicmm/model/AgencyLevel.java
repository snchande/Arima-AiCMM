package org.aicmm.model;

/**
 * The Agency Qualification Layer — a derived 13th dimension (position 12) appended
 * after the 12 core AiCMM dimensions. It interprets the 0-5 dimension scores to
 * classify how "agentic" a system is, on a ladder that runs from non-agent
 * automations (negative levels) up to humanoid agents indistinguishable from people.
 *
 * <p>Unlike the 12 core dimensions (each scored 0-5), the Agency Level is
 * <strong>derived</strong>, never authored by hand, and uses a signed ladder
 * from -2 to +5. The negative "Non-Agent" levels let AiCMM formally describe
 * scripted automations and reactive assistants without inflating them into agents.
 *
 * @see AgencyClassifier
 */
public enum AgencyLevel {

    SCRIPTED_AUTOMATION(-2, "Non-Agent — Scripted Automation",
            "Deterministic, pre-scripted tool with no autonomy and no reasoning "
                    + "(e.g. an RPA macro or ETL pipeline). A smart tool, not an agent."),
    REACTIVE_ASSISTANT(-1, "Non-Agent — Reactive Assistant",
            "AI-driven but purely reactive: answers or acts only on direct human input "
                    + "with no self-initiated goals (e.g. a FAQ bot, early Siri/Alexa, a basic Q&A LLM)."),
    PROTO_AGENT(0, "Proto-Agent — Emerging Agency",
            "Meets the minimal autonomy and reasoning thresholds but is brittle and not yet "
                    + "trustworthy — typically failing safety, explainability, or governance gates (e.g. AutoGPT)."),
    BASIC_AGENT(1, "Basic Agent — Qualified",
            "A fully qualified agent with a balanced, moderate capability profile that clears "
                    + "the cognitive thresholds and passes governance within a bounded domain."),
    ADVANCED_AGENT(2, "Advanced Agent — Autonomous & Trust-Aligned",
            "High reasoning with strong trust controls (safety, explainability) and governance "
                    + "compliance — a practical autonomous agent (e.g. Copilot CLI, MedAssist Pro, Tesla FSD)."),
    GENERALIZED_AGENT(3, "Generalized Agent — Cutting-Edge",
            "State-of-the-art autonomous agent scoring highly across cognitive core and trust "
                    + "dimensions, with world models, long-term memory, and multi-agent coordination."),
    HUMAN_LEVEL_AGENT(4, "Human-Level Agent",
            "Human-level general intelligence: masters the full cognitive core (autonomy, reasoning, "
                    + "memory, learning) with broad, robust, governed capability across domains."),
    HUMANOID_AGENT(5, "Humanoid Agent — Indistinguishable from Human",
            "Indistinguishable from a human in look, feel, and appearance — full embodiment mastery "
                    + "with synthetic skin, touch, and taste atop human-level cognition.");

    private final int level;
    private final String label;
    private final String description;

    AgencyLevel(int level, String label, String description) {
        this.level = level;
        this.label = label;
        this.description = description;
    }

    /** Signed ladder value from -2 (scripted automation) to +5 (humanoid agent). */
    public int getLevel() {
        return level;
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }

    /** A short stable code (enum name) suitable for serialization, e.g. {@code "PROTO_AGENT"}. */
    public String getCode() {
        return name();
    }

    /** True when the system clears the agent threshold (level &gt;= 0). */
    public boolean isAgent() {
        return level >= 0;
    }

    /** Resolve an {@link AgencyLevel} from its signed ladder value. */
    public static AgencyLevel fromLevel(int level) {
        for (AgencyLevel a : values()) {
            if (a.level == level) {
                return a;
            }
        }
        throw new IllegalArgumentException("No AgencyLevel for level " + level);
    }
}
