---
name: compare-agents
description: "Compares two or more Agent Cards side-by-side across all 12 dimensions. Highlights strengths, weaknesses, and governance differences. Use when evaluating alternatives or benchmarking agents."
allowed-tools:
  - view
  - powershell
  - glob
---

# Compare Agents

## Purpose

Compare two or more Agent Cards side-by-side to identify relative strengths, weaknesses, and maturity differences across the 12 Level 0 dimensions.

## Workflow

1. Load agent cards from `examples/` or via API
2. Extract capability profiles
3. Create comparison table (dimensions × agents)
4. Highlight where agents differ by 2+ points
5. Note governance status for each
6. Summarize: which agent is stronger in which group

## Comparison Output Format

```
                          Agent A    Agent B    Δ
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
COGNITIVE CORE
  Autonomy (0)               4          3     +1
  Reasoning (1)              4          4      0
  Memory (2)                 3          2     +1
  Learning (3)               2          1     +1

ACTION & INTEGRATION
  Tool Use (4)               5          4     +1
  Collaboration (5)          3          2     +1
  Embodiment (6)             0          4     -4  ⚠️

TRUST & DEPLOYMENT
  Explainability (7)         3          3      0
  Safety (8)                 3          4     -1
  Interoperability (9)       4          3     +1
  Cost Efficiency (10)       3          2     +1
  Domain Alignment (11)      3          4     -1
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
AVERAGE                     3.08       3.00
MATURITY                   Advanced   Advanced
GOVERNANCE                 ✅ PASS    ✅ PASS
```

## Via API

```bash
# Get two cards and compare
curl -s http://localhost:8080/api/agent-cards/copilot-cli-agent-card | jq .capabilityProfile
curl -s http://localhost:8080/api/agent-cards/medassist-pro-agent-card | jq .capabilityProfile
```

## Key Insights to Report

- Which dimension groups each agent dominates
- Governance failures in either card
- Whether the comparison is fair (digital vs embodied)
- Level 1 domain differences if both have domain scoring
