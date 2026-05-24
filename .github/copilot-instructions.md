# AiCMM Copilot Instructions

This is the **Agent Capability Maturity Model (a-CMM)** project — an open-source Java framework for classifying AI agent capabilities across 8 dimensions (scored 0-5).

## Available Custom Agents

Use `@aicmm-project-agent` for all AiCMM framework development tasks.

## Skills

The following skills are registered in `~/.copilot/skills/`:

- **agent-card-creation** — Generate Agent Cards from descriptions/URLs
- **agent-inspection** — Inspect agents to gather capability evidence
- **aicmm-scoring** — Score agents using the 8-dimension rubric
- **catalog-management** — Manage the Agent Card catalog
- **article-to-markdown** — Convert articles to Markdown
- **pdf-text-extraction** — Extract text from PDFs
- **java-project-scaffolding** — Scaffold Maven modules
- **json-schema-design** — Design JSON Schemas
- **markdown-to-pdf** — Generate PDFs from Markdown

## Project Commands

```bash
# Build
mvn clean package -DskipTests

# Run documentation site
java -jar aicmm-site/target/aicmm-site-0.1.0-SNAPSHOT.jar --port 8090

# CLI
java -jar aicmm-cli/target/aicmm-cli-0.1.0-SNAPSHOT.jar inspect --url <url>
java -jar aicmm-cli/target/aicmm-cli-0.1.0-SNAPSHOT.jar classify --card <path>
java -jar aicmm-cli/target/aicmm-cli-0.1.0-SNAPSHOT.jar score --card <path>
```

## The 8 Dimensions

1. **Autonomy** — Self-directed action (0=none, 5=full goal autonomy)
2. **Reasoning & Planning** — Problem solving under uncertainty
3. **Learning & Adaptation** — Improvement from experience
4. **Memory & Context** — Information retention and temporal awareness
5. **Tool Use & Integration** — Orchestrating external tools/APIs
6. **Collaboration & Social** — Coordination with humans/agents
7. **Embodiment** — Physical/virtual presence (0 for software-only)
8. **Domain Alignment** — Policy compliance, safety, auditability

## Governance Rules (CRITICAL)

- Autonomy must NOT exceed Domain Alignment + 1
- Embodiment >= 3 requires Alignment >= 3
- Tool Use >= 4 requires Alignment >= 3

## Agent Card Output

Save cards to: `examples/<agent-name-kebab>-agent-card.json`

## Project Structure

```
AiCMM/
├── aicmm-core/        Core library (models, scoring, cards)
├── aicmm-inspector/   Agent investigation framework
├── aicmm-cli/         Command-line interface (Picocli)
├── aicmm-site/        Documentation web server (Javalin)
├── docs/              Framework documentation
├── schemas/           JSON Schema definitions
├── examples/          Example Agent Cards
└── templates/         Reusable templates
```
