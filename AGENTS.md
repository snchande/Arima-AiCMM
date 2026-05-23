# AiCMM Project Agent Instructions

This repository implements the **Agent Capability Maturity Model (AiCMM)** — a multi-dimensional framework for evaluating AI agents across 8 dimensions scored 0-5.

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

1. Autonomy must never exceed Domain Alignment by more than 1 (governance rule)
2. Scores must be justified by observable evidence (logs, tests, red-team results)
3. Embodied agents (score ≥ 3) require strong Domain Alignment (≥ 3)
4. Agent Cards track version history as "Capability Resumes"
