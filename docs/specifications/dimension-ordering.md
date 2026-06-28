# AiCMM Dimension Ordering Specification

## Design Principles

1. **Fixed positions** — Each dimension has a permanent index (0...N) that NEVER changes
2. **Radar chart stability** — Adding new dimensions extends the polygon; existing positions are immutable
3. **Logical progression** — Ordered from fundamental agentic capabilities → deployment readiness
4. **Two-level architecture**:
   - **Level 0 (Universal)** — Applies to ALL agents regardless of domain
   - **Level 1 (Domain-Specific)** — Deep-dive radar for specific verticals (healthcare, transport, etc.)

---

## Level 0 — Universal Dimensions (Fixed Positions)

Ordered by logical progression: *"What makes it an agent?"* → *"What can it reach?"* → *"Is it safe to deploy?"*

### Cognitive Core (Positions 0–3)
*The thinking foundation — what the agent can reason about and retain*

| Position | Dimension | Key Question | Rationale for Order |
|----------|-----------|--------------|---------------------|
| **0** | **Autonomy** | Can it act independently? | Most fundamental — without autonomy, it's not an agent |
| **1** | **Reasoning & Planning** | Can it figure out *what* to do? | Autonomy without reasoning is just random action |
| **2** | **Memory & Context** | Can it remember and track state? | Reasoning over time requires memory |
| **3** | **Learning & Adaptation** | Can it improve from experience? | Learning builds on memory — hardest cognitive capability |

### Action & Integration (Positions 4–6)
*How the agent extends beyond itself into the world*

| Position | Dimension | Key Question | Rationale for Order |
|----------|-----------|--------------|---------------------|
| **4** | **Tool Use & Integration** | Can it use external tools/APIs? | First way an agent extends its reach |
| **5** | **Collaboration & Social Intelligence** | Can it work with humans and other agents with empathy and inclusivity? | More complex than tools — requires social modeling, emotional awareness, and age-appropriate communication |
| **6** | **Embodiment** | Does it have physical/virtual presence? | Most specialized action capability (0 for pure software) |

> **Note on Position 5**: "Collaboration & Social Intelligence" explicitly encompasses **empathy** (emotional awareness, adaptive tone, compassion) and **inclusivity** (age-appropriate communication — e.g., simpler language for children, patience for elderly users). These are scored as part of Collaboration at Level 0; domains requiring deep empathy (healthcare, education, customer service) have dedicated Level 1 dimensions for each.

### Trust & Deployment (Positions 7–11)
*Is it safe, explainable, and practical to deploy at scale?*

| Position | Dimension | Key Question | Rationale for Order |
|----------|-----------|--------------|---------------------|
| **7** | **Explainability & Transparency** | Can you understand *why* it did what it did? | First requirement for trust |
| **8** | **Safety & Robustness** | Can it fail gracefully? Resist adversarial attacks? | Trust requires proven safety |
| **9** | **Interoperability & Standards** | Does it fit into the ecosystem (A2A, MCP, etc.)? | Deployment requires integration |
| **10** | **Cost & Resource Efficiency** | Is it practical at scale? | Production viability |
| **11** | **Domain Alignment & Governance** | Does it meet regulatory/compliance needs? | Final gate before deployment — wraps everything |

---

## Position 12 — Agency Qualification Layer (Derived)

A **derived 13th dimension** appended after the twelve core dimensions. It does **not**
use the 0–5 capability scale and is **never authored by hand** — it is computed from the
twelve core scores plus the seven governance rules. Positions 0–11 stay fixed; the Agency
Qualification simply *follows* position 11 without re-indexing anything, so radar charts of
the twelve core dimensions remain directly comparable across agents and over time.

Its purpose: answer *"Is this system a genuine agent, and how agentic is it?"* — preventing
trivial automations from being mislabeled as agents while recognizing truly autonomous ones.

### The Agency Ladder (signed scale, −2 … +5)

| Level | Code | Label | Agent? | Typical Profile / Example |
|------:|------|-------|:------:|---------------------------|
| **−2** | `SCRIPTED_AUTOMATION` | Non-Agent — Scripted Automation | No | Autonomy 0–1, **Reasoning 0**. RPA macro, ETL pipeline |
| **−1** | `REACTIVE_ASSISTANT` | Non-Agent — Reactive Assistant | No | Autonomy 0–1, Reasoning ~1. FAQ bot, early Siri/Alexa, basic Q&A LLM |
| **0** | `PROTO_AGENT` | Proto-Agent — Emerging Agency | Yes | Clears thresholds but fails trust/governance. AutoGPT |
| **1** | `BASIC_AGENT` | Basic Agent — Qualified | Yes | Balanced, governed, bounded domain |
| **2** | `ADVANCED_AGENT` | Advanced Agent — Autonomous & Trust-Aligned | Yes | Expert reasoning + strong trust. Copilot CLI, MedAssist Pro, Tesla FSD |
| **3** | `GENERALIZED_AGENT` | Generalized Agent — Cutting-Edge | Yes | High across core + trust; world models, multi-agent coordination |
| **4** | `HUMAN_LEVEL_AGENT` | Human-Level Agent | Yes | Human-level general intelligence across the cognitive core |
| **5** | `HUMANOID_AGENT` | Humanoid Agent — Indistinguishable from Human | Yes | Full embodiment mastery — synthetic skin/touch/taste, indistinguishable from a person |

### Agent threshold

A system qualifies as an **agent** (level ≥ 0) only when **Autonomy ≥ 2 and Reasoning ≥ 2** —
the Cognitive Core litmus test. Below that it lands on the negative "non-agent" ladder:
`Reasoning = 0` → −2 (pure script), `Reasoning ≥ 1` → −1 (reactive but AI-driven).

### Classification algorithm (single source of truth)

```
minCore = min(autonomy, reasoning, memory, learning)
trustOk = governancePass AND safety >= 3 AND explainability >= 3
avg12   = mean of the twelve scores
avgExEm = mean of the eleven non-embodiment scores

if   autonomy < 2 OR reasoning < 2:                                 level = (reasoning >= 1) ? -1 : -2
elif NOT trustOk:                                                   level = 0   # Proto-Agent
elif embodiment >= 5 AND minCore >= 5 AND avg12 >= 4.8:             level = 5   # Humanoid
elif minCore >= 5 AND avgExEm >= 4.5:                               level = 4   # Human-Level
elif avg12 >= 4.0 AND reasoning >= 4 AND autonomy >= 4 AND memory >= 4: level = 3   # Generalized
elif reasoning >= 4 AND safety >= 3 AND explainability >= 3 AND avg12 >= 3.0: level = 2   # Advanced
else:                                                              level = 1   # Basic
```

> Level 4 uses the **non-embodiment** average so a purely digital agent (Embodiment = 0)
> is not penalized for lacking a body; Level 5 explicitly requires full embodiment (≥ 5).

### The Agency Barometer (weighted index + needle)

The discrete `level` is the authoritative band. For visualization, two continuous values are also derived:

- **`index`** — a weighted **Agency Index (0–100)** emphasizing the agentic drivers, used as the barometer reading:
  ```text
  W = {autonomy .20, reasoning .18, toolUse .12, memory .10, learning .08,
       collaboration .07, explainability .07, safety .07, domainAlignment .04,
       interoperability .03, costEfficiency .02, embodiment .02}   # sums to 1.0
  index = round( 100 × Σ(Wᵢ · scoreᵢ) / 5 )
  ```
- **`needle`** — a continuous position on the signed −2…+5 ladder. For agents it is
  `level + within-band fraction` (from `avg12` against per-level bands); for non-agents it
  parks mid-band. Both render as a horizontal **barometer strip** on the Agent Card.

This mirrors `org.aicmm.scoring.AgencyClassifier` (Java core), the site API
(`buildAgencyQualification`, `GET /api/agency-levels`), and `computeAgency()` in the web client.

---

## Radar Chart Layout

The 12 dimensions form a clock-like layout, always in this fixed order:

```
                    Autonomy (0)
                      /    \
    Domain Align (11)       Reasoning (1)
                |               |
    Cost Eff (10)            Memory (2)
                |               |
    Interop (9)             Learning (3)
                |               |
    Safety (8)              Tool Use (4)
                \             /
    Explainability (7)   Collaboration (5)
                      \  /
                  Embodiment (6)
```

**Angular spacing**: Each dimension occupies exactly `360° / N` degrees, evenly distributed.
- With 12 dimensions: 30° per dimension
- Position 0 (Autonomy) is always at 12 o'clock (90° or top)
- Positions proceed clockwise

**When comparing agents**: The shape (polygon) is directly comparable because positions never move.

---

## Backward Compatibility

The original 8 dimensions map to Level 0 positions as follows:

| Original Name | Original Order | New Position | Change |
|---------------|---------------|--------------|--------|
| Autonomy | 1 | **0** | Moved to position 0 |
| Reasoning & Planning | 2 | **1** | Same relative position |
| Learning & Adaptation | 3 | **3** | Swapped with Memory |
| Memory & Context | 4 | **2** | Swapped with Learning |
| Tool Use & Integration | 5 | **4** | Same relative position |
| Collaboration & Social | 6 | **5** | Renamed to "Collaboration & Social Intelligence" — now includes empathy & inclusivity |
| Embodiment | 7 | **6** | Same relative position |
| Domain Alignment | 8 | **11** | Moved to final position |

**New dimensions inserted:**
- Position 7: Explainability & Transparency (NEW)
- Position 8: Safety & Robustness (NEW)
- Position 9: Interoperability & Standards (NEW)
- Position 10: Cost & Resource Efficiency (NEW)

**Migration**: Existing Agent Cards with 8 dimensions are auto-mapped to the new positions. New dimensions default to `null` (not scored) rather than 0.

---

## Level 1 — Domain-Specific Radar Charts

When an agent operates in a specific domain, a **separate** Level 1 radar chart provides deep-dive scoring. Level 1 charts have their OWN fixed positions within each domain.

### Healthcare (Level 1)

| Position | Dimension | Key Question |
|----------|-----------|--------------|
| 0 | Clinical Accuracy | How accurate are diagnoses/recommendations? |
| 1 | Patient Safety | Can it prevent harm to patients? |
| 2 | Care Coordination | Can it coordinate across care teams? |
| 3 | Medical Knowledge | Depth and currency of medical knowledge |
| 4 | Regulatory Compliance | HIPAA, FDA, clinical trial regulations |
| 5 | Consent & Privacy | Patient consent management, data handling |
| 6 | Clinical Workflow Integration | Fits into EHR, PACS, lab systems |
| 7 | Outcome Tracking | Measures and reports patient outcomes |
| 8 | **Empathy** | Compassionate communication, emotional awareness of patient state, sensitivity to distress |
| 9 | **Inclusivity** | Age-appropriate interactions (pediatric vs geriatric), accessibility, health literacy adaptation |

### Transportation & Mobility (Level 1)

| Position | Dimension | Key Question |
|----------|-----------|--------------|
| 0 | Navigation Accuracy | Can it determine and follow optimal paths? |
| 1 | Obstacle Detection & Avoidance | Can it perceive and avoid hazards? |
| 2 | Passenger/Cargo Safety | Protection of what it carries |
| 3 | Traffic Rule Compliance | Adherence to regulations |
| 4 | Environmental Perception | Weather, road conditions, visibility |
| 5 | Route Optimization | Efficiency of path planning |
| 6 | V2X Communication | Vehicle-to-everything integration |
| 7 | Emergency Response | Behavior in accident/emergency scenarios |

### Financial Services (Level 1)

| Position | Dimension | Key Question |
|----------|-----------|--------------|
| 0 | Risk Assessment Accuracy | How well does it quantify risk? |
| 1 | Regulatory Compliance | SOX, MiFID II, Basel III adherence |
| 2 | Fraud Detection | Identify and prevent fraudulent activity |
| 3 | Audit Trail Completeness | Full traceability of decisions |
| 4 | Market Analysis | Accuracy of market predictions/signals |
| 5 | Client Suitability | Appropriate recommendations for risk profile |
| 6 | Settlement & Reconciliation | Accuracy of financial operations |
| 7 | Systemic Risk Awareness | Understanding of cascading effects |

### Manufacturing & Industrial (Level 1)

| Position | Dimension | Key Question |
|----------|-----------|--------------|
| 0 | Process Control Accuracy | Precision of manufacturing operations |
| 1 | Safety Certification | ISO 13849, IEC 61508 compliance |
| 2 | Quality Assurance | Defect detection and prevention |
| 3 | Predictive Maintenance | Anticipate failures before they occur |
| 4 | Human Proximity Awareness | Safe coexistence with workers |
| 5 | Supply Chain Integration | Coordination across supply network |
| 6 | Environmental Compliance | Emissions, waste, sustainability |
| 7 | Production Optimization | Throughput and efficiency |

### Education & Coaching (Level 1)

| Position | Dimension | Key Question |
|----------|-----------|--------------|
| 0 | Pedagogical Accuracy | Correctness and curriculum alignment of content |
| 1 | Adaptive Difficulty | Adjusts complexity to learner level |
| 2 | Progress Tracking | Tracks and reports learning milestones |
| 3 | Engagement Quality | Maintains learner interest and motivation |
| 4 | Assessment Fairness | Unbiased evaluation of student work |
| 5 | Curriculum Alignment | Fits into educational standards and frameworks |
| 6 | **Empathy** | Patience, encouragement, emotional support during frustration, awareness of learner emotional state |
| 7 | **Inclusivity** | Age-appropriate communication (child vs adult learner), accessibility for learning disabilities, multilingual support |

### Customer Service (Level 1)

| Position | Dimension | Key Question |
|----------|-----------|--------------|
| 0 | Issue Resolution Accuracy | Correctly identifies and resolves problems |
| 1 | Response Timeliness | Speed and efficiency of responses |
| 2 | Escalation Judgment | Knows when to involve a human agent |
| 3 | Knowledge Base Integration | Leverages documentation and FAQs effectively |
| 4 | Multi-channel Consistency | Consistent experience across chat, voice, email |
| 5 | Customer Satisfaction | Measurable impact on CSAT/NPS |
| 6 | **Empathy** | Emotional awareness, tone adaptation, active listening, de-escalation of frustrated users |
| 7 | **Inclusivity** | Age-appropriate language (child calling about a game vs elderly user with tech issues), accessibility, cultural sensitivity |

---

## JSON Schema for Dimension Positions

```json
{
  "capabilityProfile": {
    "level0": {
      "autonomy": { "position": 0, "score": 4, "confidence": "high", "evidence": "..." },
      "reasoning": { "position": 1, "score": 4, "confidence": "high", "evidence": "..." },
      "memory": { "position": 2, "score": 3, "confidence": "medium", "evidence": "..." },
      "learning": { "position": 3, "score": 2, "confidence": "medium", "evidence": "..." },
      "toolUse": { "position": 4, "score": 5, "confidence": "high", "evidence": "..." },
      "collaboration": { "position": 5, "score": 4, "confidence": "high", "evidence": "..." },
      "embodiment": { "position": 6, "score": 0, "confidence": "high", "evidence": "..." },
      "explainability": { "position": 7, "score": 3, "confidence": "medium", "evidence": "..." },
      "safety": { "position": 8, "score": 3, "confidence": "medium", "evidence": "..." },
      "interoperability": { "position": 9, "score": 4, "confidence": "high", "evidence": "..." },
      "costEfficiency": { "position": 10, "score": null, "confidence": null, "evidence": null },
      "domainAlignment": { "position": 11, "score": 4, "confidence": "high", "evidence": "..." }
    },
    "level1": {
      "domain": "healthcare",
      "dimensions": {
        "clinicalAccuracy": { "position": 0, "score": 4, "evidence": "..." },
        "patientSafety": { "position": 1, "score": 5, "evidence": "..." },
        "careCoordination": { "position": 2, "score": 3, "evidence": "..." }
      }
    }
  }
}
```

---

## Governance Rules (Updated for 12 Dimensions)

| Rule | Constraint | Positions Involved |
|------|-----------|-------------------|
| Autonomy Gap | Autonomy(0) <= DomainAlignment(11) + 1 | 0, 11 |
| Explainability Gate | Autonomy(0) >= 4 requires Explainability(7) >= 3 | 0, 7 |
| Embodiment Safety | Embodiment(6) >= 3 requires Safety(8) >= 4 | 6, 8 |
| Tool Oversight | ToolUse(4) >= 4 requires DomainAlignment(11) >= 3 | 4, 11 |
| Collaboration Interop | Collaboration(5) >= 4 requires Interop(9) >= 3 | 5, 9 |
| Cost Awareness | Autonomy(0) >= 4 requires CostEfficiency(10) >= 2 | 0, 10 |
| Reasoning Foundation | Autonomy(0) >= 4 requires Reasoning(1) >= 3 | 0, 1 |

---

## Implementation Notes

### Radar Chart Rendering
```javascript
// Fixed angle calculation — NEVER changes per dimension
function getDimensionAngle(position, totalDimensions) {
    return (position * (2 * Math.PI / totalDimensions)) - Math.PI / 2; // Start at top
}

// Level 0 always has these positions
const LEVEL_0_DIMENSIONS = [
    { position: 0, key: 'autonomy', label: 'Autonomy', short: 'AUT' },
    { position: 1, key: 'reasoning', label: 'Reasoning & Planning', short: 'REA' },
    { position: 2, key: 'memory', label: 'Memory & Context', short: 'MEM' },
    { position: 3, key: 'learning', label: 'Learning & Adaptation', short: 'LRN' },
    { position: 4, key: 'toolUse', label: 'Tool Use & Integration', short: 'TUL' },
    { position: 5, key: 'collaboration', label: 'Collaboration & Social Intelligence', short: 'COL' },
    { position: 6, key: 'embodiment', label: 'Embodiment', short: 'EMB' },
    { position: 7, key: 'explainability', label: 'Explainability', short: 'EXP' },
    { position: 8, key: 'safety', label: 'Safety & Robustness', short: 'SAF' },
    { position: 9, key: 'interoperability', label: 'Interoperability', short: 'INT' },
    { position: 10, key: 'costEfficiency', label: 'Cost & Efficiency', short: 'CST' },
    { position: 11, key: 'domainAlignment', label: 'Domain Alignment', short: 'DOM' }
];
```

### Null Handling
- `null` score = dimension not yet assessed (shown as dashed line on radar)
- `0` score = dimension assessed as absent (shown as point at center)
- This distinction matters: "not scored" ≠ "scored zero"
