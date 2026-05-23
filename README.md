# AiCMM — Agent Capability Maturity Model

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-17%2B-orange.svg)](https://openjdk.org/)
[![Contributions Welcome](https://img.shields.io/badge/contributions-welcome-brightgreen.svg)](CONTRIBUTING.md)

## Overview

The **Agent Capability Maturity Model (AiCMM)** is an open-source framework for evaluating, classifying, and describing the capabilities of modern AI agents — whether they are purely digital, embodied, or hybrid systems.

Unlike single-linear maturity ladders, AiCMM uses a **multi-dimensional capability profile** across eight dimensions, producing a unique **capability fingerprint** for each agent that makes trade-offs explicit and governance actionable.

## The Eight Dimensions

| # | Dimension | What It Measures |
|---|-----------|-----------------|
| 1 | **Autonomy** | Degree of self-directed action without human intervention |
| 2 | **Reasoning & Planning** | Structured problem solving under uncertainty |
| 3 | **Learning & Adaptation** | Ability to improve from experience safely |
| 4 | **Memory & Context** | Information retention, retrieval, and temporal awareness |
| 5 | **Tool Use & Integration** | Proficiency in orchestrating external tools and APIs |
| 6 | **Collaboration & Social Ability** | Coordination with humans and other agents |
| 7 | **Embodiment** | Physical/virtual presence — perception, navigation, manipulation |
| 8 | **Domain Alignment** | Policy compliance, regulatory fitness, safety, auditability |

Each dimension is scored **0–5**, producing a radar-chart-style fingerprint that enables comparison across fundamentally different agent types.

## Project Goals

This project provides:

1. **Framework Specification** — Formal definitions, scoring rubrics, and evidence guidelines for each dimension
2. **Agent Card Generator** — Tools to inspect agents/systems and produce standardized AiCMM Agent Cards
3. **Integration with Standards** — AiCMM classifications embeddable into A2A (Agent-to-Agent), MCP, and other emerging agent protocols
4. **CLI & Library** — Java-based tooling for programmatic agent evaluation and classification

## Project Structure

```
AiCMM/
├── docs/                          # Documentation and articles
│   ├── articles/                  # Original articles (introduction & overview)
│   ├── specifications/            # Formal framework specifications
│   └── diagrams/                  # Architecture and framework diagrams
├── aicmm-core/                    # Core library — models, scoring, agent cards
│   └── src/main/java/org/aicmm/
│       ├── core/                  # Framework core classes
│       ├── model/                 # Domain model (dimensions, scores, profiles)
│       ├── scoring/               # Scoring engine and rubrics
│       └── agentcard/             # Agent Card generation and serialization
├── aicmm-inspector/               # Agent inspector — investigates agents/systems
│   └── src/main/java/org/aicmm/inspector/
├── aicmm-cli/                     # Command-line interface
│   └── src/main/java/org/aicmm/cli/
├── schemas/                       # JSON Schema definitions for Agent Cards
├── examples/                      # Example agent cards and scoring profiles
├── .github/                       # GitHub Actions, issue templates
├── pom.xml                        # Maven parent POM
├── LICENSE                        # Apache 2.0
├── CONTRIBUTING.md                # Contribution guidelines
└── CHANGELOG.md                   # Release history
```

## Quick Start

### Prerequisites

- Java 17 or later
- Maven 3.8+

### Build

```bash
mvn clean install
```

### Generate an Agent Card

```bash
java -jar aicmm-cli/target/aicmm-cli.jar inspect --agent <agent-descriptor>
```

## Contributing

We welcome contributions! Whether you want to:

- **Co-author the framework** — refine dimensions, scoring rubrics, or industry adaptations
- **Build tooling** — inspectors, integrations, visualizations
- **Add examples** — score real-world agents and share Agent Cards
- **Improve documentation** — clarifications, translations, diagrams

Please see [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines.

## Background & Articles

- [Not All AI Agents Are the Same — So Why Do We Treat Them Like It?](docs/articles/introduction-linkedin.md) — LinkedIn introduction
- [Agent Capability Maturity Model: A Unified Framework](docs/articles/overview-medium.md) — Detailed Medium article

## License

This project is licensed under the [Apache License 2.0](LICENSE).

## Author

**Suresh Chande** — Principal Product Manager, Microsoft  
- [LinkedIn](https://www.linkedin.com/in/sureshchande)
- [Medium](https://medium.com/@sureshchande)
