package org.aicmm.inspector;

import org.aicmm.agentcard.AgentCard;
import org.aicmm.model.AgentCategory;
import org.aicmm.model.CapabilityProfile;

/**
 * Inspector interface for investigating agents and producing AiCMM Agent Cards.
 * Implementations can inspect different types of agents (A2A, MCP, REST APIs, etc.)
 */
public interface AgentInspector {

    /**
     * Inspect an agent and produce a capability profile.
     *
     * @param descriptor identifier or endpoint for the agent to inspect
     * @return the generated Agent Card
     */
    AgentCard inspect(String descriptor);

    /**
     * Check if this inspector can handle the given descriptor.
     */
    boolean canInspect(String descriptor);

    /**
     * Get the name of this inspector.
     */
    String getName();
}
