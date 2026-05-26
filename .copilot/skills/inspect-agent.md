---
name: inspect-agent
description: Inspects an AI agent from a URL, documentation, or description to gather evidence about its capabilities, tools, skills, MCPs, and relationships. Produces structured input for AiCMM scoring and card creation.
allowed-tools:
  - web_fetch
  - view
  - powershell
  - grep
---

# Inspect Agent

## Purpose

Investigate an AI agent to produce structured capability evidence for AiCMM scoring and Agent Card creation. Works from URLs, documentation files, or textual descriptions.

## Workflow

1. **Fetch source** — Download agent docs from URL or read from local files
2. **Extract identity** — Name, version, vendor, deployment model
3. **Classify** — digital / embodied / hybrid
4. **Discover capabilities**:
   - Tools it uses (APIs, databases, file systems, code execution)
   - Skills it provides (what can it do?)
   - Plugins/extensions available
   - MCP servers it connects to
5. **Map relationships**:
   - Sub-agents it delegates to
   - Systems/agents that use it
   - External dependencies (models, services, hardware)
6. **Preliminary scoring** — Estimate 12-dimension scores with evidence
7. **Output template** — Produce Agent Card JSON ready for review/refinement

## What to Look For

| Dimension | Evidence Sources |
|-----------|-----------------|
| Autonomy | Goal decomposition, self-scheduling, approval gates |
| Reasoning | Multi-step planning, comparison, error recovery |
| Memory | Session persistence, cross-session recall, context windows |
| Learning | Fine-tuning, feedback loops, adaptation |
| Tool Use | API calls, file operations, tool chaining, error handling |
| Collaboration | Multi-agent coordination, human handoff, empathy signals |
| Embodiment | Sensors, actuators, physical interaction, robotics |
| Explainability | Reasoning traces, audit logs, confidence scores |
| Safety | Guardrails, content filtering, graceful degradation |
| Interoperability | A2A, MCP, OpenAI, standard protocols |
| Cost Efficiency | Token management, caching, resource pooling |
| Domain Alignment | Compliance, certifications, policy enforcement |

## Inspection via API

```bash
curl -X POST http://localhost:8080/api/inspect \
  -H "Content-Type: application/json" \
  -d '{"url":"https://agent-docs.example.com","name":"My Agent"}'
```

Returns a template Agent Card structure ready for scoring refinement.

## Output

Produces a JSON structure matching AiCMM Agent Card schema v0.2.0, with preliminary scores marked as `"confidence": "low"` for human review.
