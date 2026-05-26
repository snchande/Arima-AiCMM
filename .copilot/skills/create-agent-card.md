---
name: create-agent-card
description: Creates an AiCMM Agent Card for any AI agent or tool. Scores 12 Level 0 dimensions (0-5), generates avatar, documents tools/skills/plugins/MCPs, validates governance rules, and registers the card in the AiCMM catalog.
allowed-tools:
  - web_fetch
  - create
  - edit
  - view
  - powershell
---

# Create Agent Card

## Purpose

Create a complete AiCMM Agent Card for any AI agent, tool, or agentic system. The card captures:
- Identity (name, version, vendor, category)
- 12-dimension capability profile with evidence
- Tools, skills, plugins, MCP connections
- Agent relationships (delegates to, used by, depends on)
- Avatar (archetype, personality, strengths, weaknesses)
- Standards integration (A2A, MCP, OpenAI)
- Governance validation (7 rules)

## Workflow

1. **Gather information** — Read the agent's docs, URL, or user-provided description
2. **Classify** — Determine category: `digital` | `embodied` | `hybrid`
3. **Score 12 dimensions** — Each scored 0-5 with position, confidence, and evidence
4. **Document capabilities** — List tools, skills, plugins, MCPs
5. **Map relationships** — What does it delegate to? What uses it?
6. **Generate avatar** — Archetype + personality traits + strengths/weaknesses
7. **Validate governance** — Check all 7 rules pass
8. **Save** — Write to `examples/<name>-agent-card.json`
9. **Register** — If AiCMM server is running, POST to `http://localhost:8080/api/agent-cards`

## 12 Level 0 Dimensions (Fixed Positions)

| Pos | Key | Dimension | Group |
|-----|-----|-----------|-------|
| 0 | autonomy | Autonomy | Cognitive Core |
| 1 | reasoning | Reasoning | Cognitive Core |
| 2 | memory | Memory | Cognitive Core |
| 3 | learning | Learning | Cognitive Core |
| 4 | toolUse | Tool Use | Action & Integration |
| 5 | collaboration | Collaboration & Social Intelligence | Action & Integration |
| 6 | embodiment | Embodiment | Action & Integration |
| 7 | explainability | Explainability | Trust & Deployment |
| 8 | safety | Safety | Trust & Deployment |
| 9 | interoperability | Interoperability | Trust & Deployment |
| 10 | costEfficiency | Cost Efficiency | Trust & Deployment |
| 11 | domainAlignment | Domain Alignment | Trust & Deployment |

## Scoring Scale

| Score | Level | Meaning |
|-------|-------|---------|
| 0 | Absent | Capability not present |
| 1 | Basic | Single hardcoded behavior |
| 2 | Intermediate | Structured but limited |
| 3 | Advanced | Handles complexity with guardrails |
| 4 | Expert | Autonomous within boundaries |
| 5 | Mastery | Full autonomy with self-governance |

## 7 Governance Rules (ALL MUST PASS)

1. **Autonomy-Reasoning Foundation** — Autonomy ≤ Reasoning + 1
2. **Explainability Gate** — Autonomy ≥ 4 → Explainability ≥ 3
3. **Safety Gate** — Autonomy ≥ 4 → Safety ≥ 3
4. **Collaboration-Interop Link** — Collaboration ≥ 4 → Interoperability ≥ 3
5. **Cost Awareness** — Tool Use ≥ 4 → Cost Efficiency ≥ 2
6. **Domain Alignment** — Embodiment ≥ 3 → Domain Alignment ≥ 3
7. **Reasoning Foundation** — Tool Use ≥ 4 → Reasoning ≥ 3

## Output Format

```json
{
  "schemaVersion": "0.2.0",
  "agent": {
    "name": "Agent Name",
    "version": "1.0.0",
    "vendor": "Vendor Name",
    "category": "digital|embodied|hybrid",
    "description": "What this agent does"
  },
  "capabilityProfile": {
    "autonomy": { "position": 0, "score": 3, "confidence": "high", "evidence": "..." },
    ...all 12 dimensions...
  },
  "tools": ["tool1", "tool2"],
  "skills": ["skill1", "skill2"],
  "plugins": ["plugin1"],
  "mcps": ["mcp-server-1"],
  "agentRelationships": {
    "delegatesTo": ["sub-agent-1"],
    "usedBy": ["parent-system"],
    "dependsOn": ["model-provider"]
  },
  "avatar": {
    "archetype": "The Analyst|The Builder|The Guardian|...",
    "personality": ["trait1", "trait2"],
    "strengths": ["strength1"],
    "weaknesses": ["weakness1"]
  },
  "standardsIntegration": { "a2a": {...}, "mcp": {...} },
  "governanceValidation": { "overallStatus": "PASSED", "rules": [...] },
  "assessmentMetadata": {
    "assessedBy": "Your Name",
    "assessedDate": "YYYY-MM-DD",
    "methodology": "AiCMM Level 0 v0.2 - Evidence-based scoring"
  }
}
```

## Registration via API

If the AiCMM site is running at http://localhost:8080:
```bash
curl -X POST http://localhost:8080/api/agent-cards \
  -H "Content-Type: application/json" \
  -d @examples/my-agent-card.json
```

## Registration via CLI

```bash
cd AiCMM
java -jar aicmm-cli/target/aicmm-cli-0.1.0-SNAPSHOT.jar register --card examples/my-agent-card.json
```
