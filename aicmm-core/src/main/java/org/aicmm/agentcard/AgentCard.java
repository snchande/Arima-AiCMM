package org.aicmm.agentcard;

import org.aicmm.model.AgentCategory;
import org.aicmm.model.CapabilityProfile;
import org.aicmm.model.OperationalConstraints;

import java.time.LocalDate;
import java.util.List;

/**
 * An AiCMM Agent Card — the complete capability description of an AI agent.
 * This is the primary output artifact of the AiCMM framework, suitable for
 * embedding into A2A protocol, MCP configurations, and governance documentation.
 *
 * @param schemaVersion       version of the AiCMM schema
 * @param name                agent name
 * @param version             agent version being evaluated
 * @param vendor              organization that created the agent
 * @param description         brief description of agent purpose
 * @param category            digital, embodied, or hybrid
 * @param capabilityProfile   the 8-dimension capability fingerprint
 * @param constraints         operational constraints
 * @param capabilityResume    historical scores across versions
 * @param assessedBy          who performed the assessment
 * @param assessedDate        when the assessment was performed
 * @param evidenceSources     sources used to justify scores
 */
public record AgentCard(
        String schemaVersion,
        String name,
        String version,
        String vendor,
        String description,
        AgentCategory category,
        CapabilityProfile capabilityProfile,
        OperationalConstraints constraints,
        List<CapabilityResumeEntry> capabilityResume,
        String assessedBy,
        LocalDate assessedDate,
        List<String> evidenceSources
) {
    public static final String CURRENT_SCHEMA_VERSION = "0.1.0";

    public AgentCard {
        if (schemaVersion == null) schemaVersion = CURRENT_SCHEMA_VERSION;
        capabilityResume = capabilityResume != null ? List.copyOf(capabilityResume) : List.of();
        evidenceSources = evidenceSources != null ? List.copyOf(evidenceSources) : List.of();
    }

    /**
     * An entry in the capability resume tracking score evolution.
     */
    public record CapabilityResumeEntry(
            String version,
            LocalDate date,
            CapabilityProfile profile,
            String notes
    ) {}
}
