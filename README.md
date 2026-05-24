# a•CMM — Agent Capability Maturity Model

<p align="center">
  <strong>A unified, multi-dimensional framework for classifying AI agent capabilities</strong>
</p>

<p align="center">
  <a href="LICENSE"><img src="https://img.shields.io/badge/License-MIT-green.svg" alt="License: MIT"></a>
  <a href="https://openjdk.org/"><img src="https://img.shields.io/badge/Java-17%2B-orange.svg" alt="Java 17+"></a>
  <a href="CONTRIBUTING.md"><img src="https://img.shields.io/badge/contributions-welcome-brightgreen.svg" alt="Contributions Welcome"></a>
  <a href="https://github.com/snchande/AiCMM/actions"><img src="https://img.shields.io/badge/build-passing-brightgreen.svg" alt="Build"></a>
</p>

---

## Overview

**a•CMM (Agent Capability Maturity Model)** is an open-source framework that helps developers, architects, and organizations **classify, evaluate, and communicate** the capabilities of AI agents in a structured, comparable way.

Instead of treating all AI agents as equal — or relying on vague marketing terms — a•CMM provides an **8-dimension scoring model** (each scored 0–5) that produces a unique **capability fingerprint** for any agent, whether it's a simple chatbot, an autonomous coding assistant, or an embodied robot.

The result is an **Agent Card**: a standardized, machine-readable description of what an agent can and cannot do — enabling informed decisions about deployment, governance, and interoperability.

| Dimension | What It Measures |
|-----------|-----------------|
| **Autonomy** | Self-directed action without human intervention |
| **Reasoning & Planning** | Structured problem-solving under uncertainty |
| **Learning & Adaptation** | Ability to improve from experience safely |
| **Memory & Context** | Information retention, retrieval, temporal awareness |
| **Tool Use & Integration** | Orchestrating external tools and APIs |
| **Collaboration & Social** | Coordination with humans and other agents |
| **Embodiment** | Physical/virtual presence — perception, navigation, manipulation |
| **Domain Alignment** | Policy compliance, safety, auditability |

---

## Why a•CMM?

Today's AI agent ecosystem is exploding — but we lack a common language to describe what agents actually do. This creates real problems:

| Problem | How a•CMM Helps |
|---------|----------------|
| **"All agents are the same"** | Multi-dimensional scoring reveals that a coding agent and a robot are fundamentally different systems |
| **No way to compare agents** | Capability fingerprints enable apples-to-apples comparison across vendors |
| **Governance is an afterthought** | Domain Alignment is a first-class dimension; autonomy is constrained by alignment |
| **Vendor marketing is opaque** | Agent Cards provide evidence-based, verifiable capability claims |
| **Standards lack capability metadata** | a•CMM integrates with A2A, MCP, and other protocols via embeddable classifications |

### Key Design Principles

- **Multi-dimensional, not linear** — No single "maturity level"; agents have unique profiles
- **Evidence-based scoring** — Each level requires observable, testable indicators
- **Governance built-in** — Autonomy must not exceed Domain Alignment by more than 1 level
- **Protocol-agnostic** — Embeds into any agent communication standard
- **Community-driven** — Open for co-authoring, industry adaptations, and new dimensions

---

## Repo Structure

```
AiCMM/
├── docs/                              # Documentation
│   ├── articles/                      # Original articles (LinkedIn + Medium)
│   │   ├── introduction-linkedin.md   # "Not All AI Agents Are the Same"
│   │   ├── overview-medium.md         # Full framework specification
│   │   ├── introduction-linkedin.pdf  # PDF versions
│   │   └── overview-medium.pdf
│   └── diagrams/                      # Mermaid architecture diagrams
│
├── aicmm-core/                        # Core library
│   └── src/main/java/org/aicmm/
│       ├── model/                     # Dimension, DimensionScore, CapabilityProfile
│       ├── scoring/                   # ScoringEngine — validation & analysis
│       └── agentcard/                 # AgentCard record + JSON serializer
│
├── aicmm-inspector/                   # Agent inspection framework
│   └── src/main/java/org/aicmm/inspector/
│       └── AgentInspector.java        # Interface for agent investigation
│
├── aicmm-cli/                         # Command-line interface (Picocli)
│   └── src/main/java/org/aicmm/cli/
│       └── AicmmCli.java              # CLI entry point
│
├── aicmm-site/                        # Documentation web server (Javalin)
│   └── src/main/java/org/aicmm/site/ # Renders docs, agent cards, diagrams
│
├── schemas/
│   └── agent-card.schema.json         # JSON Schema for Agent Cards
│
├── examples/
│   └── copilot-cli-agent-card.json    # Example: GitHub Copilot CLI scored
│
├── pom.xml                            # Maven parent POM (Java 17)
├── LICENSE                            # MIT License
├── CONTRIBUTING.md                    # How to contribute
└── CHANGELOG.md                       # Release history
```

---

## How to Use

### Prerequisites

- **Java 17** or later
- **Maven 3.8+**

### Build the Project

```bash
git clone https://github.com/snchande/AiCMM.git
cd AiCMM
mvn clean install
```

### Run the Documentation Site

```bash
java -jar aicmm-site/target/aicmm-site-0.1.0-SNAPSHOT.jar
# → Open http://localhost:8080
```

The site renders all Markdown documentation with Mermaid diagram support, provides an interactive Agent Card browser with radar charts, and links to the original articles.

### Use as a Library

Add the core module to your project:

```xml
<dependency>
  <groupId>org.aicmm</groupId>
  <artifactId>aicmm-core</artifactId>
  <version>0.1.0-SNAPSHOT</version>
</dependency>
```

```java
import org.aicmm.model.*;
import org.aicmm.scoring.ScoringEngine;
import org.aicmm.agentcard.AgentCard;

// Build a capability profile
CapabilityProfile profile = CapabilityProfile.builder()
    .score(Dimension.AUTONOMY, 4)
    .score(Dimension.REASONING_AND_PLANNING, 4)
    .score(Dimension.LEARNING_AND_ADAPTATION, 2)
    .score(Dimension.MEMORY_AND_CONTEXT, 4)
    .score(Dimension.TOOL_USE_AND_INTEGRATION, 5)
    .score(Dimension.COLLABORATION_AND_SOCIAL_ABILITY, 3)
    .score(Dimension.EMBODIMENT, 0)
    .score(Dimension.DOMAIN_ALIGNMENT, 4)
    .build();

// Validate governance rules
ScoringEngine engine = new ScoringEngine();
List<String> warnings = engine.validate(profile);

// Create an Agent Card
AgentCard card = new AgentCard("My Agent", "1.0", profile, AgentCategory.DIGITAL);
```

---

## How to Classify an Agent

Classification follows a structured process:

### Step 1: Identify the Agent Category

| Category | Description | Examples |
|----------|-------------|----------|
| **Digital** | Software-only, no physical interaction | Copilot, ChatGPT, AutoGPT |
| **Embodied** | Has physical presence or sensors | Spot, surgical robots, drones |
| **Hybrid** | Combines digital reasoning with physical action | Tesla FSD, warehouse robots |

### Step 2: Score Each Dimension (0–5)

For each of the 8 dimensions, assign a level based on observable evidence:

| Level | Meaning | Indicator |
|-------|---------|-----------|
| **0** | None | Capability absent |
| **1** | Basic | Minimal, hardcoded behavior |
| **2** | Intermediate | Structured but limited scope |
| **3** | Advanced | Handles complexity with guardrails |
| **4** | Expert | Autonomous within defined boundaries |
| **5** | Mastery | Full autonomy with self-governance |

### Step 3: Apply Governance Rules

- **Constraint**: `Autonomy ≤ Domain Alignment + 1`
- If an agent scores Autonomy: 5 but Domain Alignment: 3, it is **non-compliant** — flag for review

### Step 4: Generate the Capability Fingerprint

The 8 scores form a fingerprint: `[4, 4, 2, 4, 5, 3, 0, 4]`

This can be visualized as a radar chart, compared across agents, and embedded into protocols.

### Step 5: Document as an Agent Card

See the next section ↓

---

## How to Generate an Agent Card

An **Agent Card** is the machine-readable output of the classification process. It follows the [JSON Schema](schemas/agent-card.schema.json).

### Using the CLI (coming soon)

```bash
# Interactive classification
java -jar aicmm-cli/target/aicmm-cli.jar classify --interactive

# Inspect an agent endpoint
java -jar aicmm-cli/target/aicmm-cli.jar inspect --url https://agent.example.com/.well-known/agent

# Validate an existing card
java -jar aicmm-cli/target/aicmm-cli.jar validate --card my-agent-card.json
```

### Manual Creation

Create a JSON file following this structure:

```json
{
  "name": "My AI Agent",
  "version": "2.1.0",
  "category": "DIGITAL",
  "description": "An autonomous coding assistant",
  "capabilityProfile": {
    "autonomy": 4,
    "reasoningAndPlanning": 4,
    "learningAndAdaptation": 2,
    "memoryAndContext": 4,
    "toolUseAndIntegration": 5,
    "collaborationAndSocialAbility": 3,
    "embodiment": 0,
    "domainAlignment": 4
  },
  "governanceCompliant": true,
  "totalScore": 26,
  "averageScore": 3.25,
  "assessedAt": "2026-05-24",
  "assessedBy": "human"
}
```

### Embedding in A2A / MCP

Agent Cards are designed to be embedded as capability metadata in agent communication protocols:

```json
{
  "agent": { "name": "My Agent", "url": "..." },
  "aicmm": { "$ref": "./my-agent-card.json" }
}
```

---

## Contributing

We welcome contributions from developers, researchers, and AI practitioners! You can:

- **Co-author the framework** — Refine scoring rubrics, propose new dimensions, create industry adaptations
- **Build tooling** — Inspector implementations, protocol integrations, visualizations
- **Add Agent Cards** — Classify real-world agents and submit examples
- **Improve documentation** — Clarifications, translations, tutorials

See [CONTRIBUTING.md](CONTRIBUTING.md) for full guidelines, code of conduct, and development setup.

### Quick Contribution Workflow

```bash
git checkout -b feature/your-feature
# Make changes
mvn clean verify
git commit -m "feat: your description"
git push origin feature/your-feature
# Open a Pull Request
```

---

## Background & Articles

The a•CMM framework originated from these published articles:

- 📰 [Not All AI Agents Are the Same — So Why Do We Treat Them Like It?](https://www.linkedin.com/pulse/all-ai-agents-same-so-why-do-we-treat-them-like-suresh-chande-oxgqc/) — LinkedIn
- 📖 [Agent Capability Maturity Model: A Unified Framework for Evaluating Modern AI Agents](https://medium.com/@sureshchande/agent-capability-maturity-model-a-unified-framework-for-evaluating-modern-ai-agents-bcb5b7a64bd7) — Medium

Local copies are available in [`docs/articles/`](docs/articles/) (Markdown and PDF).

---

## License

This project is licensed under the **MIT License** — see [LICENSE](LICENSE) for details.

You are free to use, modify, and distribute this framework in both open-source and commercial projects.

---

## Author

**Suresh Chande** — Principal Product Manager, Microsoft

- [LinkedIn](https://www.linkedin.com/in/sureshchande)
- [Medium](https://medium.com/@sureshchande)
- [GitHub](https://github.com/snchande)

---

<p align="center">
  <em>Not all agents are the same — now we have a way to prove it.</em>
</p>
