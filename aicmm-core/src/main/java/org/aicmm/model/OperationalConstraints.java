package org.aicmm.model;

import java.util.List;

/**
 * Operational constraints defining where and how an agent is allowed to operate.
 *
 * @param domain               operating domain (e.g., healthcare, logistics, finance)
 * @param safetyClass          safety classification level
 * @param approvalRequirements human approval gates required
 * @param regulatoryFrameworks applicable regulatory frameworks (e.g., HIPAA, ISO 26262)
 */
public record OperationalConstraints(
        String domain,
        String safetyClass,
        List<String> approvalRequirements,
        List<String> regulatoryFrameworks
) {
    public OperationalConstraints {
        approvalRequirements = approvalRequirements != null ? List.copyOf(approvalRequirements) : List.of();
        regulatoryFrameworks = regulatoryFrameworks != null ? List.copyOf(regulatoryFrameworks) : List.of();
    }
}
