---
name: add-level1-domain
description: "Adds a new Level 1 domain-specific scoring dimension set to the AiCMM framework. Guides through defining domain dimensions, positions, and creating example agent cards. Use when extending AiCMM to new industry verticals."
allowed-tools:
  - view
  - edit
  - create
  - powershell
---

# Add Level 1 Domain

## Purpose

Extend AiCMM with a new Level 1 domain-specific scoring dimension set (e.g., Legal, Agriculture, Retail, Energy).

## Workflow

1. **Define the domain** — Name, description, key regulatory requirements
2. **Identify 6-10 domain dimensions** — What matters specifically in this domain?
3. **Assign positions** — Starting from 0 within the domain
4. **Define scoring rubrics** — What does 0-5 mean for each domain dimension?
5. **Create example agent** — A fictional agent card demonstrating the domain scoring
6. **Update documentation** — Add to dimension-ordering.md and guides
7. **Update API** — Add to getDimensions() Level 1 response

## Domain Dimension Guidelines

Good domain dimensions are:
- Specific to the industry (not covered by Level 0)
- Measurable/observable
- Relevant to deployment decisions
- 6-10 dimensions per domain (avoid bloat)

## Existing Domains (for reference)

| Domain | Dimensions | Special |
|--------|-----------|---------|
| Healthcare | 10 | Includes Empathy, Inclusivity |
| Transportation | 8 | V2X, Fleet Coordination |
| Finance | 8 | Fraud Detection, Audit Trail |
| Manufacturing | 8 | Predictive Maintenance, Quality |
| Education | 8 | Includes Empathy, Inclusivity |
| Customer Service | 8 | Includes Empathy, Inclusivity |

## Template for New Domain

```json
{
  "domain": "Your Domain",
  "version": "1.0",
  "dimensions": [
    { "position": 0, "key": "dimKey", "label": "Dimension Name", "description": "What this measures" },
    ...
  ]
}
```

## Files to Update

1. `docs/specifications/dimension-ordering.md` — Add domain definition
2. `examples/<domain>-agent-card.json` — Create example card
3. `aicmm-site/.../AgentCardController.java` — Add to getDimensions()
4. `CLAUDE.md`, `GEMINI.md`, `.github/copilot-instructions.md` — Mention new domain
