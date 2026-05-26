---
name: validate-governance
description: "Validates an Agent Card's capability profile against the 7 AiCMM governance rules. Reports pass/fail for each rule with specific values. Use before submitting or registering any agent card."
allowed-tools:
  - view
  - powershell
---

# Validate Governance Rules

## Purpose

Check that an Agent Card's scores satisfy all 7 mandatory governance rules before submission.

## The 7 Rules

| # | Rule | Condition | Rationale |
|---|------|-----------|-----------|
| 1 | Autonomy-Reasoning Foundation | Autonomy ≤ Reasoning + 1 | Can't act beyond what you can think through |
| 2 | Explainability Gate | Autonomy ≥ 4 → Explainability ≥ 3 | High autonomy needs transparency |
| 3 | Safety Gate | Autonomy ≥ 4 → Safety ≥ 3 | High autonomy needs safety controls |
| 4 | Collaboration-Interop Link | Collaboration ≥ 4 → Interoperability ≥ 3 | Can't collaborate without protocols |
| 5 | Cost Awareness | Tool Use ≥ 4 → Cost Efficiency ≥ 2 | Heavy tool use needs resource awareness |
| 6 | Domain Alignment | Embodiment ≥ 3 → Domain Alignment ≥ 3 | Physical agents need domain compliance |
| 7 | Reasoning Foundation | Tool Use ≥ 4 → Reasoning ≥ 3 | Complex tools need reasoning |

## How to Validate

### Via API (preferred)
```bash
curl -X POST http://localhost:8080/api/validate \
  -H "Content-Type: application/json" \
  -d @examples/my-agent-card.json
```

### Manual Check
Extract scores from the card and verify each rule:
```
autonomy=4, reasoning=4 → Rule 1: 4 ≤ 4+1 ✅
autonomy=4 → explainability≥3? → explainability=3 ✅
autonomy=4 → safety≥3? → safety=4 ✅
...
```

## Fixing Failures

If a rule fails, you have two options:
1. **Lower the triggering score** (e.g., reduce Autonomy from 4 to 3)
2. **Raise the required score** (e.g., increase Safety from 2 to 3)

Always re-evaluate evidence when changing scores — don't game the rules.
