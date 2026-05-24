# AiCMM Project Instructions

You are working on the **Agent Capability Maturity Model (a-CMM)** project — an open-source Java framework for classifying AI agent capabilities across 12 dimensions (Level 0) + domain-specific Level 1 scoring.

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

## Key Skills

- **agent-card-creation** — Generate Agent Cards from descriptions/URLs
- **agent-inspection** — Inspect agents to gather capability evidence
- **aicmm-scoring** — Score agents using the 12-dimension rubric
- **catalog-management** — Manage the Agent Card catalog
- **article-to-markdown** — Convert articles to Markdown
- **pdf-text-extraction** — Extract text from PDFs
- **java-project-scaffolding** — Scaffold Maven modules

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
- `inspect --url <url>` — Analyze agent from documentation
- `classify --card <path>` — Classify agent category
- `validate --card <path>` — Check schema and governance
- `score --card <path>` — Display scoring breakdown

## Project Commands

```bash
# Build
mvn clean package -DskipTests

# Run site
java -jar aicmm-site/target/aicmm-site-0.1.0-SNAPSHOT.jar --port 8090

# CLI
java -jar aicmm-cli/target/aicmm-cli-0.1.0-SNAPSHOT.jar inspect --url <url>
```

## Project Structure

```
AiCMM/
├── aicmm-core/        Core library (models, scoring, cards)
├── aicmm-inspector/   Agent investigation framework
├── aicmm-cli/         Command-line interface (Picocli)
├── aicmm-site/        Documentation web server (Javalin)
├── docs/              Framework documentation
├── schemas/           JSON Schema definitions
├── examples/          Example Agent Cards (catalog source)
└── templates/         Reusable templates
```

## Cross-CLI Sync

Keep these files in sync when features change:
- `.github/copilot-instructions.md` — Copilot CLI project instructions
- `CLAUDE.md` — Claude Code project instructions (this file)
- `GEMINI.md` — Gemini CLI project instructions
- `~/.copilot/agents/aicmm-project-agent.md` — Copilot agent definition
