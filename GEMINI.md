# AiCMM Project Instructions for Gemini

You are working on the **Agent Capability Maturity Model (a-CMM)** project — an open-source framework for classifying AI agent capabilities across 8 dimensions.

## Available Skills

### agent-card-creation
Generate Agent Cards from agent descriptions or URLs. Score across 8 dimensions (0-5): Autonomy, Reasoning, Learning, Memory, Tool Use, Collaboration, Embodiment, Domain Alignment.

### agent-inspection
Inspect AI agents from URLs/docs to gather evidence for scoring.

### aicmm-scoring
Apply the scoring rubric. Validate governance (Autonomy <= Domain Alignment + 1).

### catalog-management
Manage the Agent Card catalog — add, search, compare, validate cards.

## Key Commands

```bash
# Build
mvn clean package -DskipTests

# Run site
java -jar aicmm-site/target/aicmm-site-0.1.0-SNAPSHOT.jar --port 8090

# CLI (when implemented)
java -jar aicmm-cli/target/aicmm-cli-0.1.0-SNAPSHOT.jar inspect --url <agent-url>
java -jar aicmm-cli/target/aicmm-cli-0.1.0-SNAPSHOT.jar classify --card examples/my-agent-card.json
```

## Governance Rules
- Autonomy must NOT exceed Domain Alignment + 1
- Embodiment >= 3 requires Alignment >= 3
- Tool Use >= 4 requires Alignment >= 3

## Agent Card Output
Save generated cards to: `examples/<agent-name-kebab>-agent-card.json`
