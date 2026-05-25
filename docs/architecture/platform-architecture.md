# a•CMM Platform Architecture

This document describes the internal architecture of the a•CMM agent platform — how agents are defined, loaded, evaluated, and how the runtime orchestrates the entire lifecycle.

---

## High-Level System Overview

```mermaid
graph TB
    subgraph "User Interfaces"
        CLI["aicmm-cli<br/>Command Line"]
        Site["aicmm-site<br/>Web Dashboard"]
        API["REST API<br/>/api/v1/*"]
    end

    subgraph "Agent Runtime"
        Loader["Agent Loader<br/>Discovery & Registration"]
        Evaluator["Evaluation Engine<br/>Scoring & Governance"]
        CardGen["Card Generator<br/>Agent Card Output"]
        Runtime["Agent Runtime<br/>Lifecycle Manager"]
    end

    subgraph "Core Framework"
        Models["Domain Models<br/>Dimensions, Profiles"]
        Scoring["Scoring Engine<br/>Rubrics & Validation"]
        Governance["Governance Rules<br/>Constraints & Policies"]
    end

    subgraph "External Integrations"
        A2A["A2A Protocol"]
        MCP["MCP Servers"]
        REST_EXT["REST Endpoints"]
        FS["File System<br/>Agent Descriptors"]
    end

    CLI --> Runtime
    Site --> Runtime
    API --> Runtime

    Runtime --> Loader
    Runtime --> Evaluator
    Runtime --> CardGen

    Evaluator --> Models
    Evaluator --> Scoring
    Evaluator --> Governance

    Loader --> A2A
    Loader --> MCP
    Loader --> REST_EXT
    Loader --> FS

    CardGen --> |"Agent Card JSON"| Output["Agent Cards"]
```

---

## Directory Architecture

The platform organizes its concerns into four key directories that map to Maven modules:

```mermaid
graph LR
    subgraph "agents/"
        AD["Agent Descriptors"]
        AC["Agent Configurations"]
        AM["Agent Metadata"]
    end

    subgraph "aCMM/"
        FW["Framework Definitions"]
        RB["Scoring Rubrics"]
        GR["Governance Rules"]
        SC["Schemas"]
    end

    subgraph "agent_runtime/"
        LDR["Loader"]
        REG["Registry"]
        EVL["Evaluator"]
        LCM["Lifecycle Manager"]
    end

    subgraph "aicmm-core/"
        MDL["Domain Models"]
        SE["Scoring Engine"]
        ACS["Agent Card Serializer"]
    end

    AD --> LDR
    FW --> EVL
    RB --> SE
    GR --> SE
    LDR --> REG
    REG --> EVL
    EVL --> ACS
    MDL --> SE
    SC --> ACS
```

---

## `agents/` — Agent Definitions

The `agents/` directory holds **agent descriptors** — structured definitions of agents to be evaluated. Each agent is defined as a YAML or JSON file describing what the agent is, where it can be reached, and any known capabilities.

```
agents/
├── digital/
│   ├── copilot-cli.yaml          # GitHub Copilot CLI descriptor
│   ├── chatgpt-4o.yaml           # OpenAI ChatGPT-4o
│   └── autogpt.yaml              # AutoGPT
├── embodied/
│   ├── boston-dynamics-spot.yaml  # Spot robot
│   └── amazon-prime-air.yaml     # Prime Air drone
├── hybrid/
│   ├── tesla-fsd.yaml            # Tesla Full Self-Driving
│   └── warehouse-robot.yaml      # Warehouse automation
└── _templates/
    └── agent-descriptor.yaml     # Template for new agents
```

### Agent Descriptor Schema

```yaml
# agents/digital/copilot-cli.yaml
name: "GitHub Copilot CLI"
version: "1.0.40"
vendor: "GitHub / Microsoft"
category: digital
description: "Terminal-based AI coding assistant with tool use"

# How to reach/inspect this agent
endpoints:
  type: local-cli
  command: "copilot --version"
  documentation: "https://docs.github.com/copilot"

# Pre-known capabilities (optional, for seeding evaluation)
declared_capabilities:
  tools: [shell, file-edit, git, web-search, grep]
  protocols: [MCP]
  delegation: true

# Metadata
tags: [coding, developer-tools, cli]
last_assessed: "2026-05-24"
```

### How Agents Are Discovered

```mermaid
flowchart LR
    subgraph "Discovery Sources"
        FS["agents/ directory"]
        A2A["A2A .well-known"]
        MCP_D["MCP manifest"]
        Manual["Manual registration"]
    end

    subgraph "Agent Loader"
        Scan["Scanner"]
        Parse["Parser"]
        Validate["Validator"]
        Register["Registry"]
    end

    FS --> Scan
    A2A --> Scan
    MCP_D --> Scan
    Manual --> Register

    Scan --> Parse
    Parse --> Validate
    Validate --> Register
```

---

## `aCMM/` — Framework Definitions

The `aCMM/` directory contains the **framework specification** itself — the scoring rubrics, governance rules, dimension definitions, and evaluation criteria that form the a•CMM standard.

```
aCMM/
├── dimensions/
│   ├── autonomy.md               # Level 0-5 rubric with evidence criteria
│   ├── reasoning-planning.md
│   ├── memory-context.md
│   ├── learning-adaptation.md
│   ├── tool-use-integration.md
│   ├── collaboration-social.md
│   ├── embodiment.md
│   ├── explainability.md
│   ├── safety.md
│   ├── interoperability.md
│   ├── cost-efficiency.md
│   └── domain-alignment.md
├── governance/
│   ├── rules.yaml                # Formal governance constraints
│   ├── compliance-matrix.md      # Mapping to regulatory frameworks
│   └── safety-classes.md         # Safety classification definitions
├── schemas/
│   ├── agent-card.schema.json    # Agent Card JSON Schema
│   ├── capability-profile.schema.json
│   └── agent-descriptor.schema.json
└── versions/
    ├── v0.2.0.md                 # Current version specification
    └── CHANGELOG.md              # Framework version history
```

### Governance Rules Engine

```mermaid
flowchart TD
    Profile["Capability Profile<br/>[4,4,4,3,5,3,0,4,3,4,2,4]"] --> Rules

    subgraph Rules["Governance Rule Evaluation"]
        R1{"Autonomy gte 4 then<br/>Reasoning gte 4?"}
        R2{"Autonomy gte 4 then<br/>Explainability gte 3?"}
        R3{"Embodiment gte 3 then<br/>Safety gte 4?"}
        R4{"Collaboration gte 4 then<br/>Interoperability gte 3?"}
        R5{"Autonomy gte 4 then<br/>Cost Efficiency gte 2?"}
        R6{"Autonomy gte 4 then<br/>Domain Alignment gte 3?"}
        R7{"Autonomy gte 4 then<br/>Reasoning gte 3?"}
    end

    R1 --> Check2
    R2 --> Check2
    R3 --> Check2
    R4 --> Check2
    R5 --> Check2
    R6 --> Check2
    R7 --> Check2

    Check2{All Rules<br/>Passed?} -->|Yes| Compliant["COMPLIANT<br/>Generate Agent Card"]
    Check2 -->|No| Violation["GOVERNANCE VIOLATION<br/>Agent non-compliant"]

    Violation --> Remediate["Remediation Required<br/>Improve controls or<br/>reduce autonomy"]
```

---

## `agent_runtime/` — Evaluation & Lifecycle

The `agent_runtime/` is the execution engine that loads agent descriptors, runs inspectors, evaluates capabilities, and produces Agent Cards. It manages the full lifecycle from discovery to card generation.

```
agent_runtime/
├── loader/
│   ├── AgentLoader.java          # Main loader — discovers & registers agents
│   ├── FileSystemScanner.java    # Scans agents/ directory
│   ├── A2ADiscovery.java         # Discovers via A2A protocol
│   └── MCPDiscovery.java         # Discovers via MCP manifests
├── registry/
│   ├── AgentRegistry.java        # In-memory registry of known agents
│   └── AgentEntry.java           # Registry entry with metadata
├── evaluator/
│   ├── EvaluationPipeline.java   # Orchestrates full evaluation
│   ├── DimensionEvaluator.java   # Per-dimension scoring logic
│   ├── EvidenceCollector.java    # Gathers evaluation evidence
│   └── GovernanceChecker.java    # Applies governance rules
├── lifecycle/
│   ├── AgentLifecycleManager.java  # State machine for agent lifecycle
│   └── ScheduledReassessment.java  # Periodic re-evaluation
└── output/
    ├── AgentCardWriter.java      # Writes Agent Card JSON
    ├── ReportGenerator.java      # Generates evaluation reports
    └── A2AEmitter.java           # Embeds into A2A protocol
```

### Agent Evaluation Pipeline

```mermaid
sequenceDiagram
    participant User
    participant CLI as aicmm-cli
    participant RT as Agent Runtime
    participant Loader as Agent Loader
    participant Registry as Agent Registry
    participant Pipeline as Evaluation Pipeline
    participant Inspector as Agent Inspector
    participant Scoring as Scoring Engine
    participant Gov as Governance Checker
    participant Output as Card Generator

    User->>CLI: aicmm inspect --agent copilot-cli
    CLI->>RT: evaluate("copilot-cli")

    RT->>Loader: load("copilot-cli")
    Loader->>Loader: scan agents/ directory
    Loader->>Registry: register(descriptor)
    Registry-->>RT: AgentEntry

    RT->>Pipeline: evaluate(AgentEntry)

    loop For each Dimension (8x)
        Pipeline->>Inspector: inspect(dimension, agent)
        Inspector->>Inspector: gather evidence
        Inspector-->>Pipeline: DimensionEvidence
        Pipeline->>Scoring: score(dimension, evidence)
        Scoring-->>Pipeline: DimensionScore (0-5)
    end

    Pipeline->>Gov: validate(CapabilityProfile)
    Gov-->>Pipeline: GovernanceResult

    alt Compliant
        Pipeline->>Output: generateCard(profile, metadata)
        Output-->>CLI: AgentCard (JSON)
        CLI-->>User: Agent Card generated
    else Non-compliant
        Pipeline-->>CLI: Governance violations
        CLI-->>User: Violations + remediation steps
    end
```

### Agent Lifecycle State Machine

```mermaid
stateDiagram-v2
    [*] --> Discovered: Agent descriptor found

    Discovered --> Registered: Validated & added to registry
    Registered --> Evaluating: Evaluation triggered

    Evaluating --> Scored: All dimensions scored
    Scored --> GovernanceCheck: Apply governance rules

    GovernanceCheck --> Compliant: All rules pass
    GovernanceCheck --> NonCompliant: Rules violated

    Compliant --> CardGenerated: Agent Card produced
    NonCompliant --> Remediation: Fix violations
    Remediation --> Evaluating: Re-evaluate

    CardGenerated --> Published: Card distributed
    Published --> Monitoring: Periodic reassessment

    Monitoring --> Evaluating: Capabilities changed
    Monitoring --> Deprecated: Agent retired

    Deprecated --> [*]
```

---

## How the System Loads and Evaluates Agents

### 1. Discovery Phase

The runtime scans multiple sources for agent descriptors:

| Source | Method | Example |
|--------|--------|---------|
| **File System** | Scan `agents/` directory for YAML/JSON | `agents/digital/copilot-cli.yaml` |
| **A2A Protocol** | Query `.well-known/agent.json` endpoints | `https://agent.example.com/.well-known/agent.json` |
| **MCP Manifest** | Parse MCP server tool manifests | `mcp://server/tools` |
| **Manual** | User provides descriptor via CLI/API | `aicmm register --url ...` |

### 2. Registration Phase

Validated descriptors are added to the `AgentRegistry`:

```java
AgentEntry entry = AgentEntry.builder()
    .name(descriptor.getName())
    .version(descriptor.getVersion())
    .category(descriptor.getCategory())
    .endpoints(descriptor.getEndpoints())
    .status(AgentStatus.REGISTERED)
    .build();

registry.register(entry);
```

### 3. Evaluation Phase

The `EvaluationPipeline` orchestrates scoring across all 12 Level 0 dimensions:

```java
// For each dimension, select the appropriate inspector and score
for (Dimension dim : Dimension.values()) {
    Evidence evidence = inspector.gather(agent, dim);
    DimensionScore score = scoringEngine.evaluate(dim, evidence);
    profile.setScore(dim, score);
}
```

### 4. Governance Phase

The `GovernanceChecker` validates the profile against rules:

```java
List<GovernanceViolation> violations = governanceChecker.check(profile);
if (violations.isEmpty()) {
    // Proceed to card generation
} else {
    // Return violations with remediation guidance
}
```

### 5. Output Phase

Compliant profiles become Agent Cards:

```java
AgentCard card = AgentCard.builder()
    .name(entry.getName())
    .version(entry.getVersion())
    .category(entry.getCategory())
    .capabilityProfile(profile)
    .assessedBy(currentUser)
    .assessedDate(LocalDate.now())
    .build();

cardWriter.write(card, outputPath);
```

---

## Integration Points

```mermaid
graph TB
    subgraph "a•CMM Platform"
        Core["Core Framework"]
        RT["Agent Runtime"]
        Cards["Agent Cards"]
    end

    subgraph "Standards & Protocols"
        A2A["Google A2A<br/>Agent-to-Agent"]
        MCP_P["Anthropic MCP<br/>Model Context Protocol"]
        OAI["OpenAI<br/>Function Calling"]
    end

    subgraph "Ecosystem"
        GH["GitHub Actions<br/>CI/CD Scoring"]
        K8s["Kubernetes<br/>Agent Orchestration"]
        Reg["Agent Registries<br/>Discovery Catalogs"]
    end

    Cards -->|"embed capability metadata"| A2A
    Cards -->|"annotate tool manifests"| MCP_P
    Cards -->|"capability declarations"| OAI

    RT -->|"automated scoring in CI"| GH
    Cards -->|"deployment policies"| K8s
    Cards -->|"publish to catalog"| Reg
```

---

## Module Dependency Graph

```mermaid
graph BT
    Core["aicmm-core<br/>Models, Scoring, Cards"]
    Inspector["aicmm-inspector<br/>Agent Investigation"]
    CLI["aicmm-cli<br/>Picocli Commands"]
    Site["aicmm-site<br/>Javalin Web Server"]

    Inspector --> Core
    CLI --> Core
    CLI --> Inspector
    Site --> Core

    subgraph "External Dependencies"
        Jackson["Jackson<br/>JSON Serialization"]
        Picocli["Picocli<br/>CLI Framework"]
        Javalin["Javalin<br/>HTTP Server"]
        CommonMark["CommonMark<br/>Markdown Rendering"]
    end

    Core --> Jackson
    CLI --> Picocli
    Site --> Javalin
    Site --> CommonMark
```

---

## Configuration

The platform is configured via `aicmm.yaml` at the project root:

```yaml
# aicmm.yaml
aicmm:
  version: "0.2.0"
  dimensionSchema: "level0-v0.2"

  # Where to scan for agent descriptors
  agents:
    directories:
      - agents/
      - ~/.aicmm/agents/
    remote:
      - url: "https://registry.example.com/agents"
        refresh: 24h

  # Governance rules
  governance:
    rules:
      - autonomy_reasoning_foundation: 4     # if autonomy >= 4, reasoning must be >= 4
      - autonomy_explainability_gate: 3      # if autonomy >= 4, explainability must be >= 3
      - embodiment_safety_gate: 4            # if embodiment >= 3, safety must be >= 4
      - collaboration_interop_link: 3        # if collaboration >= 4, interoperability must be >= 3
      - autonomy_cost_awareness: 2           # if autonomy >= 4, cost efficiency must be >= 2
      - autonomy_domain_alignment: 3         # if autonomy >= 4, domain alignment must be >= 3
      - high_autonomy_reasoning_floor: 3     # if autonomy >= 4, reasoning must be >= 3

  # Output
  output:
    directory: output/cards/
    formats: [json, yaml]
    embed_in_a2a: true

  # Scoring
  scoring:
    rubrics_path: aCMM/dimensions/
    require_evidence: true
    minimum_evidence_sources: 2
```

---

## Summary

| Component | Responsibility |
|-----------|---------------|
| **`agents/`** | Agent descriptor definitions — what to evaluate |
| **`aCMM/`** | Framework specification — how to evaluate |
| **`agent_runtime/`** | Execution engine — orchestrates evaluation |
| **`aicmm-core/`** | Domain models & scoring — the evaluation logic |
| **`aicmm-inspector/`** | Investigation interface — gathers evidence |
| **`aicmm-cli/`** | User-facing commands — triggers evaluations |
| **`aicmm-site/`** | Web dashboard — visualizes results |
