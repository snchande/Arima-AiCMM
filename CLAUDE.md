# AiCMM Project Instructions

You are working on the **Agent Capability Maturity Model (AiCMM)** project — an open-source Java framework for classifying AI agent capabilities across 12 dimensions (Level 0) + domain-specific Level 1 scoring.

## You Are: aicmm-project-agent

A specialized agent for all AiCMM framework tasks: development, agent classification, capability profiling, Agent Card creation, and catalog management.

## Level 0 — 12 Universal Dimensions

| Group | Pos | Dimension | Key Question |
|---|---:|-----------|--------------|
| Cognitive Core | 0 | Autonomy | How self-directed is it? |
| Cognitive Core | 1 | Reasoning & Planning | Can it solve problems under uncertainty? |
| Cognitive Core | 2 | Memory & Context | Does it retain and use information over time? |
| Cognitive Core | 3 | Learning & Adaptation | Does it improve from experience safely? |
| Action & Integration | 4 | Tool Use & Integration | Can it orchestrate tools and handle failures? |
| Action & Integration | 5 | Collaboration & Social Intelligence | Coordination with humans/agents, empathy, inclusivity |
| Action & Integration | 6 | Embodiment | Does it interact with the physical world? |
| Trust & Deployment | 7 | Explainability & Transparency | Can it justify actions and support review? |
| Trust & Deployment | 8 | Safety & Containment | Can it operate within safe bounded controls? |
| Trust & Deployment | 9 | Interoperability | Can it work across protocols and agent ecosystems? |
| Trust & Deployment | 10 | Cost Efficiency | Can it stay resource-aware and economical? |
| Trust & Deployment | 11 | Domain Alignment | Is it compliant, safe, auditable, deployable? |

## Agency Qualification Layer — Derived Position 12

Position 12 is a computed, never hand-authored layer derived from the 12 unchanged Level 0 dimensions. It distinguishes non-agent automations from qualified agents and supports the 3 eras narrative: Gen 1 Classical/Expert Systems, Gen 2 Distributed/Learning Systems, and Gen 3 Modern Agentic GenAI. The negative ladder covers scripted automations (RPA/ETL) and reactive assistants (FAQ bots, early Siri/Alexa, basic Q&A LLMs) without inflating them into agents.

| Level | Code | Label | Agent? |
|------:|------|-------|:------:|
| -2 | SCRIPTED_AUTOMATION | Non-Agent — Scripted Automation | No |
| -1 | REACTIVE_ASSISTANT | Non-Agent — Reactive Assistant | No |
| 0 | PROTO_AGENT | Proto-Agent — Emerging Agency | Yes |
| 1 | BASIC_AGENT | Basic Agent — Qualified | Yes |
| 2 | ADVANCED_AGENT | Advanced Agent — Autonomous & Trust-Aligned | Yes |
| 3 | GENERALIZED_AGENT | Generalized Agent — Cutting-Edge | Yes |
| 4 | HUMAN_LEVEL_AGENT | Human-Level Agent | Yes |
| 5 | HUMANOID_AGENT | Humanoid Agent — Indistinguishable from Human | Yes |

Agent threshold: a system is an agent (`level >= 0`) only when Autonomy >= 2 and Reasoning >= 2; otherwise it is non-agent -1 when Reasoning >= 1, or -2 when Reasoning = 0. Level 4 represents human-level general intelligence; Level 5 requires full humanoid embodiment mastery. Computed by `org.aicmm.scoring.AgencyClassifier`, site `buildAgencyQualification`, and web `computeAgency()`. Exposed via `GET /api/agency-levels` and MCP tool `aicmm_get_agency_levels`.

Classification algorithm: `minCore = min(autonomy, reasoning, memory, learning)`, `trustOk = governancePass && safety>=3 && explainability>=3`, `avg12 = mean(12 scores)`, `avgExEm = mean(11 non-embodiment scores)`. If Autonomy < 2 or Reasoning < 2, return -1 when Reasoning >= 1 else -2; else if not `trustOk`, 0; else if Embodiment >= 5 and `minCore >= 5` and `avg12 >= 4.8`, 5; else if `minCore >= 5` and `avgExEm >= 4.5`, 4; else if `avg12 >= 4.0` and Reasoning/Autonomy/Memory >= 4, 3; else if Reasoning >= 4 and Safety/Explainability >= 3 and `avg12 >= 3.0`, 2; else 1.

## Level 1 — Domain-Specific Scoring

Use Level 1 radar charts for domains such as Healthcare, Transportation, Finance, and Manufacturing. These are drill-downs layered on top of Level 0, not replacements for it.

## Scoring Levels (0-5)

| Level | Meaning |
|-------|---------|
| 0 | Absent |
| 1 | Basic/hardcoded |
| 2 | Intermediate/structured |
| 3 | Advanced with guardrails |
| 4 | Expert within boundaries |
| 5 | Mastery with self-governance |

## Governance Rules (CRITICAL)

- Autonomy >= 4 requires Reasoning >= 4
- Autonomy >= 4 requires Explainability >= 3
- Embodiment >= 3 requires Safety >= 4
- Collaboration >= 4 requires Interoperability >= 3
- Autonomy >= 4 requires Cost Efficiency >= 2
- Autonomy >= 4 requires Domain Alignment >= 3
- Autonomy >= 4 requires Reasoning >= 3

## Key Skills (in-repo `.copilot/skills/`)

- **create-agent-card** — Create full Agent Card from URL/description
- **register-agent-card** — Validate and register existing card into catalog
- **score-agent** — Score 12 dimensions with governance validation
- **inspect-agent** — Investigate agent capabilities from docs/URL

## MCP Server (Pure Java)

AiCMM exposes a full MCP server via stdio transport:
```bash
java -jar aicmm-cli/target/aicmm-cli-0.1.0-SNAPSHOT.jar --mcp
```

MCP config for Claude Code: `.mcp.json` in project root (auto-detected).

API endpoints at http://localhost:8080/api:
- POST /api/agent-cards — Create card
- GET /api/agent-cards — List catalog
- GET /api/agent-cards/{name} — Get card
- POST /api/validate — Validate governance
- POST /api/agent-cards/_/score — Score breakdown
- POST /api/inspect — Inspect from URL/description
- GET /api/dimensions — Dimension definitions
- GET /api/agency-levels — Agency Qualification ladder
- GET /api/schema — JSON Schema

MCP tools include `aicmm_get_agency_levels` for the Agency Qualification ladder.

## Key Capabilities

### Agent Card Creation
- Inspect agent from URL or description
- Score 12 Level 0 dimensions with evidence
- Add Level 1 domain scoring when deployment context requires it
- Generate avatar (archetype, personality, strengths, weaknesses)
- Add tools, skills, plugins, MCP connections
- Document agent relationships (delegatesTo, usedBy, dependsOn)
- Embed in standards (A2A, MCP, OpenAI)
- Save to `examples/<name>-agent-card.json`

### Documentation Site (port 8090)
- Pages: Home, Framework, Architecture, Catalog, Create Card, Guide, Releases, Schema
- Mermaid diagrams, radar charts, avatar rendering
- Catalog with search/filter (name, category, min score)
- Create Card form with live preview + download/copy

### CLI Commands
- `--mcp` — Start MCP stdio server (for Claude/Copilot/Gemini integration)
- `inspect --url <url>` — Analyze agent from documentation
- `classify --card <path>` — Classify agent category
- `validate --card <path>` — Check schema and governance
- `score --card <path>` — Display scoring breakdown

## Project Commands

```bash
# Build
mvn clean package -DskipTests

# Run site (serves web UI + REST API)
java -jar aicmm-site/target/aicmm-site-0.1.0-SNAPSHOT.jar

# Start MCP server (requires site running)
java -jar aicmm-cli/target/aicmm-cli-0.1.0-SNAPSHOT.jar --mcp

# Create and register a card via API
curl -X POST http://localhost:8080/api/agent-cards -H "Content-Type: application/json" -d @examples/my-agent-card.json
```

## Project Structure

```
AiCMM/
├── .copilot/agents/    In-repo agents (aicmm.md)
├── .copilot/skills/    In-repo skills (create, register, score, inspect)
├── .mcp.json           MCP server config (auto-detected by Claude Code)
├── aicmm-core/         Core library (models, scoring, cards)
├── aicmm-inspector/    Agent investigation framework
├── aicmm-cli/          CLI + MCP stdio server (fat JAR, pure Java)
├── aicmm-site/         Web server + REST API (Javalin)
├── mcp/                MCP config and tool definitions
├── docs/               Framework documentation
├── schemas/            JSON Schema definitions
├── examples/           Agent Cards (catalog source)
└── templates/          Reusable templates
```

## Cross-CLI Sync

Keep these files in sync when features change:
- `.github/copilot-instructions.md` — Copilot CLI project instructions
- `CLAUDE.md` — Claude Code project instructions (this file)
- `GEMINI.md` — Gemini CLI project instructions
- `~/.copilot/agents/aicmm-project-agent.md` — Copilot agent definition
