---
name: score-agent
description: Scores an AI agent using the AiCMM 12-dimension Level 0 framework. Validates governance rules and calculates maturity level. Can also perform Level 1 domain-specific scoring.
allowed-tools:
  - view
  - powershell
  - web_fetch
---

# Score Agent

## Purpose

Score an AI agent across 12 Level 0 dimensions (0-5 scale) with evidence-based justification. Validates governance rules and determines overall maturity level.

## Workflow

1. **Gather evidence** — Read docs, test agent, review capabilities
2. **Score each dimension** — 0-5 with confidence (high/medium/low) and evidence string
3. **Validate governance** — All 7 rules must pass
4. **Calculate maturity** — Average score determines maturity level
5. **Optionally score Level 1** — Domain-specific dimensions if applicable

## Maturity Levels

| Average Score | Maturity Level |
|---------------|---------------|
| 0.0 – 0.4 | Nascent |
| 0.5 – 1.4 | Basic |
| 1.5 – 2.4 | Intermediate |
| 2.5 – 3.4 | Advanced |
| 3.5 – 4.4 | Expert |
| 4.5 – 5.0 | Mastery |

## Scoring via API

```bash
curl -X POST http://localhost:8080/api/agent-cards/_/score \
  -H "Content-Type: application/json" \
  -d @examples/agent-card.json
```

## Level 1 Domain Scoring

For domain-specific agents, add `level1Profile`:
- **Healthcare**: Clinical Accuracy, Patient Safety, Diagnostic Support, EHR Integration, Empathy, Inclusivity, etc.
- **Finance**: Risk Assessment, Fraud Detection, Regulatory Compliance, Market Analysis, etc.
- **Manufacturing**: Process Optimization, Quality Control, Predictive Maintenance, etc.
- **Transportation**: Route Optimization, Obstacle Detection, V2X Communication, etc.
- **Education**: Curriculum Adaptation, Learning Assessment, Engagement, Empathy, Inclusivity, etc.
- **Customer Service**: Response Quality, Resolution Rate, Sentiment Analysis, Empathy, Inclusivity, etc.
