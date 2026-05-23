package org.aicmm.model;

/**
 * The eight capability dimensions used to evaluate an AI agent.
 * Each dimension is scored independently on a 0-5 scale.
 */
public enum Dimension {
    AUTONOMY("Autonomy", "Degree of self-directed action without human intervention"),
    REASONING("Reasoning & Planning", "Structured problem solving under uncertainty"),
    LEARNING("Learning & Adaptation", "Ability to improve from experience safely"),
    MEMORY("Memory & Context", "Information retention, retrieval, and temporal awareness"),
    TOOL_USE("Tool Use & Integration", "Proficiency in orchestrating external tools and APIs"),
    COLLABORATION("Collaboration & Social Ability", "Coordination with humans and other agents"),
    EMBODIMENT("Embodiment", "Physical/virtual presence — perception, navigation, manipulation"),
    DOMAIN_ALIGNMENT("Domain Alignment", "Policy compliance, regulatory fitness, safety, auditability");

    private final String displayName;
    private final String description;

    Dimension(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
}
