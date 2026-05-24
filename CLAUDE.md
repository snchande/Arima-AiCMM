# AiCMM Project Instructions

You are working on the **Agent Capability Maturity Model (a-CMM)** project — an open-source framework for classifying AI agent capabilities across 8 dimensions.

## Key Skills Available

Use these skills when working on a-CMM tasks:

### agent-card-creation
Generate Agent Cards from agent descriptions or URLs. Score across 8 dimensions (0-5): Autonomy, Reasoning, Learning, Memory, Tool Use, Collaboration, Embodiment, Domain Alignment.

### agent-inspection
Inspect AI agents from URLs/docs to gather evidence for scoring. Identify tools, skills, plugins, MCP connections, delegation patterns.

### aicmm-scoring
Apply the scoring rubric to evaluate agents. Validate governance (Autonomy <= Domain Alignment + 1).

### catalog-management
Manage the Agent Card catalog — add, search, compare, validate cards.

## Project Structure
- `aicmm-core/` — Java library with domain models and scoring engine
- `aicmm-inspector/` — Agent investigation framework
- `aicmm-cli/` — Picocli command-line interface
- `aicmm-site/` — Javalin web documentation server
- `docs/` — Framework documentation
- `schemas/` — JSON Schema definitions
- `examples/` — Example Agent Cards
- `templates/` — Reusable templates

## Governance Rules
- Autonomy must NOT exceed Domain Alignment + 1
- Embodiment >= 3 requires Alignment >= 3
- Tool Use >= 4 requires Alignment >= 3

## Agent Card Output
Save generated cards to: `examples/<agent-name-kebab>-agent-card.json`

## Site
Run with: `java -jar aicmm-site/target/aicmm-site-0.1.0-SNAPSHOT.jar --port 8090`
