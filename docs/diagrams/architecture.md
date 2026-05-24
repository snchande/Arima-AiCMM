# AiCMM Framework Architecture

This document provides architectural diagrams for the Agent Capability Maturity Model framework.

## System Architecture

```mermaid
graph TB
    subgraph "AiCMM Framework"
        Core["aicmm-core<br/>Domain Models &amp; Scoring"]
        Inspector["aicmm-inspector<br/>Agent Investigation"]
        CLI["aicmm-cli<br/>Command Line"]
        Site["aicmm-site<br/>Documentation Server"]
    end

    subgraph "External Systems"
        A2A["A2A Protocol"]
        MCP["MCP Servers"]
        REST["REST APIs"]
        Docs["Agent Docs"]
    end

    subgraph "Outputs"
        Card["Agent Card (JSON)"]
        Report["Governance Report"]
        Resume["Capability Resume"]
    end

    Inspector --> Core
    CLI --> Core
    CLI --> Inspector
    Site --> Core

    Inspector --> A2A
    Inspector --> MCP
    Inspector --> REST
    Inspector --> Docs

    Core --> Card
    Core --> Report
    Core --> Resume
```

## Eight Dimensions Model

```mermaid
graph LR
    subgraph "Capability Dimensions (0-5)"
        A[Autonomy]
        R[Reasoning &amp; Planning]
        L[Learning &amp; Adaptation]
        M[Memory &amp; Context]
        T[Tool Use &amp; Integration]
        C[Collaboration]
        E[Embodiment]
        D[Domain Alignment]
    end

    Agent((AI Agent)) --> A
    Agent --> R
    Agent --> L
    Agent --> M
    Agent --> T
    Agent --> C
    Agent --> E
    Agent --> D

    A --> FP[Capability<br/>Fingerprint]
    R --> FP
    L --> FP
    M --> FP
    T --> FP
    C --> FP
    E --> FP
    D --> FP
```

## Scoring & Governance Flow

```mermaid
flowchart TD
    Start[Identify Agent] --> Gather[Gather Evidence<br/>Logs, Tests, Red-team]
    Gather --> Score[Score 8 Dimensions<br/>0-5 each]
    Score --> Validate{Governance<br/>Rules Pass?}
    
    Validate -->|Yes| Card[Generate Agent Card]
    Validate -->|No| Fix[Address Violations]
    Fix --> Score
    
    Card --> Resume[Update Capability Resume]
    Resume --> Deploy{Ready for<br/>Deployment?}
    
    Deploy -->|Yes| Prod[Production]
    Deploy -->|No| Improve[Improve Bottleneck]
    Improve --> Score

    subgraph "Key Governance Rules"
        Rule1["Autonomy ≤ Domain Alignment + 1"]
        Rule2["Embodiment ≥ 3 → Alignment ≥ 3"]
        Rule3["High Autonomy → Strong Controls"]
    end
```

## Agent Categories

```mermaid
graph TB
    subgraph "Digital Agents"
        LLM["LLM Assistants<br/>Copilots, Chatbots"]
        Auto["Automation Agents<br/>Workflow, DevOps"]
        Multi["Multi-Agent Systems<br/>Orchestrators"]
    end

    subgraph "Embodied Agents"
        Robot["Robots<br/>Spot, AMRs"]
        Drone["Drones<br/>Prime Air, Inspection"]
        Vehicle["Vehicles<br/>FSD, Autonomous"]
    end

    subgraph "Hybrid Agents"
        IoT["IoT + AI<br/>Smart Buildings"]
        Digital_Twin["Digital Twins<br/>Simulation + Physical"]
    end

    LLM --> Profile["AiCMM Profile"]
    Auto --> Profile
    Multi --> Profile
    Robot --> Profile
    Drone --> Profile
    Vehicle --> Profile
    IoT --> Profile
    Digital_Twin --> Profile
```

## Capability Resume Timeline

```mermaid
gantt
    title Agent Evolution - Capability Resume
    dateFormat YYYY-MM
    section Autonomy
        Level 1 (Reactive)      :done, a1, 2024-01, 2024-06
        Level 2 (Triggered)     :done, a2, 2024-06, 2025-01
        Level 4 (Goal-driven)   :active, a3, 2025-01, 2025-12
    section Reasoning
        Level 1 (Basic)         :done, r1, 2024-01, 2024-04
        Level 2 (Structured)    :done, r2, 2024-04, 2024-10
        Level 3 (Multi-step)    :done, r3, 2024-10, 2025-06
        Level 5 (Strategic)     :active, r4, 2025-06, 2025-12
    section Domain Alignment
        Level 2 (Basic audit)   :done, d1, 2024-01, 2024-08
        Level 3 (Compliant)     :done, d2, 2024-08, 2025-03
        Level 4 (Certified)     :active, d3, 2025-03, 2025-12
```
