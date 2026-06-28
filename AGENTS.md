# AiCMM Project Agent Instructions

This repository implements the **Agent Capability Maturity Model (AiCMM)** — a multi-dimensional framework for evaluating AI agents across 12 Level 0 dimensions scored 0-5, with optional Level 1 domain-specific scoring.

## Key Conventions

- Java 17+ with records, sealed classes, pattern matching
- Package: `org.aicmm.*`
- Maven multi-module: `aicmm-core`, `aicmm-inspector`, `aicmm-cli`
- Agent Cards are JSON documents conforming to `schemas/agent-card.schema.json`
- Tests use JUnit 5

## Architecture

- **aicmm-core**: Domain models (`Dimension`, `DimensionScore`, `CapabilityProfile`, `AgentCard`), scoring engine, serialization
- **aicmm-inspector**: Interfaces and implementations for investigating agents (A2A, MCP, REST)
- **aicmm-cli**: Picocli-based CLI for `aicmm inspect`, `aicmm score`, `aicmm validate`

## Framework Rules

1. Level 0 uses 12 fixed positions grouped into Cognitive Core, Action & Integration, and Trust & Deployment
2. Level 1 adds domain-specific radar charts without replacing Level 0
3. Scores must be justified by observable evidence (logs, tests, red-team results)
4. Governance validation checks 7 rules covering reasoning, explainability, safety, interoperability, cost, and domain fit
5. Agent Cards track version history as "Capability Resumes"

## Agency Qualification Layer — Derived Position 12

Position 12 is a computed, never hand-authored layer derived from the 12 unchanged Level 0 dimensions. It distinguishes non-agent automations from qualified agents and supports the 3 eras narrative: Gen 1 Classical/Expert Systems, Gen 2 Distributed/Learning Systems, and Gen 3 Modern Agentic GenAI. The negative ladder covers scripted automations (RPA/ETL) and reactive assistants (FAQ bots, early Siri/Alexa, basic Q&A LLMs) without inflating them into agents.

| Level | Code | Label | Agent? |
|------:|------|-------|:------:|
| -2 | SCRIPTED_AUTOMATION | Non-Agent — Scripted Automation | No |
| -1 | REACTIVE_ASSISTANT | Non-Agent — Reactive Assistant | No |
| 0 | PROTO_AGENT | Proto-Agent — Emerging Agency | Yes |
| 1 | BASIC_AGENT | Basic Agent — Qualified | Yes |
| 2 | ADVANCED_AGENT | Advanced Agent — Autonomous & Trust-Aligned | Yes |
| 3 | GENERALIZED_AGENT | Generalized Agent — Cutting-Edge | Yes |
| 4 | HUMAN_LEVEL_AGENT | Human-Level Agent | Yes |
| 5 | HUMANOID_AGENT | Humanoid Agent — Indistinguishable from Human | Yes |

Agent threshold: a system is an agent (`level >= 0`) only when Autonomy >= 2 and Reasoning >= 2; otherwise it is non-agent -1 when Reasoning >= 1, or -2 when Reasoning = 0. Level 4 represents human-level general intelligence; Level 5 requires full humanoid embodiment mastery. Computed by `org.aicmm.scoring.AgencyClassifier`, site `buildAgencyQualification`, and web `computeAgency()`. Exposed via `GET /api/agency-levels` and MCP tool `aicmm_get_agency_levels`.

Classification algorithm: `minCore = min(autonomy, reasoning, memory, learning)`, `trustOk = governancePass && safety>=3 && explainability>=3`, `avg12 = mean(12 scores)`, `avgExEm = mean(11 non-embodiment scores)`. If Autonomy < 2 or Reasoning < 2, return -1 when Reasoning >= 1 else -2; else if not `trustOk`, 0; else if Embodiment >= 5 and `minCore >= 5` and `avg12 >= 4.8`, 5; else if `minCore >= 5` and `avgExEm >= 4.5`, 4; else if `avg12 >= 4.0` and Reasoning/Autonomy/Memory >= 4, 3; else if Reasoning >= 4 and Safety/Explainability >= 3 and `avg12 >= 3.0`, 2; else 1.
