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
