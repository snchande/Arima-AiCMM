# AiCMM Framework Architecture

This document provides architectural diagrams for the Agent Capability Maturity Model framework.

## System Architecture

```mermaid
graph TB
    subgraph "AiCMM Framework"
        Core["aicmm-core<br/>Domain Models & Scoring"]
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

## Twelve Level 0 Dimensions

```mermaid
graph LR
    subgraph "Cognitive Core"
        A[Autonomy]
        R[Reasoning & Planning]
        M[Memory & Context]
        L[Learning & Adaptation]
    end

    subgraph "Action & Integration"
        T[Tool Use & Integration]
        C[Collaboration]
        E[Embodiment]
    end

    subgraph "Trust & Deployment"
        X[Explainability & Transparency]
        S[Safety & Containment]
        I[Interoperability]
        CE[Cost Efficiency]
        D[Domain Alignment]
    end

    Agent((AI Agent)) --> A
    Agent --> R
    Agent --> M
    Agent --> L
    Agent --> T
    Agent --> C
    Agent --> E
    Agent --> X
    Agent --> S
    Agent --> I
    Agent --> CE
    Agent --> D

    A --> FP[Level 0 Capability<br/>Fingerprint]
    R --> FP
    M --> FP
    L --> FP
    T --> FP
    C --> FP
    E --> FP
    X --> FP
    S --> FP
    I --> FP
    CE --> FP
    D --> FP
```

## Scoring & Governance Flow

```mermaid
flowchart TD
    Start[Identify Agent] --> Gather[Gather Evidence<br/>Logs, Tests, Red-team]
    Gather --> Score[Score 12 Level 0 Dimensions<br/>0-5 each]
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
        Rule1["Autonomy 4+ requires Reasoning 4+"]
        Rule2["Autonomy 4+ requires Explainability 3+"]
        Rule3["Embodiment 3+ requires Safety 4+"]
        Rule4["Collaboration 4+ requires Interoperability 3+"]
        Rule5["Autonomy 4+ requires Cost Efficiency 2+"]
        Rule6["Autonomy 4+ requires Domain Alignment 3+"]
        Rule7["Autonomy 4+ requires Reasoning 3+"]
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
