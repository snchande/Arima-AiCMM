# AiCMM Copilot Instructions

This is the **Agent Capability Maturity Model (a-CMM)** project — an open-source Java framework for classifying AI agent capabilities across 12 dimensions (scored 0-5 at Level 0).

## Available Custom Agents

Use `@aicmm-project-agent` for all AiCMM framework development tasks.

## Skills

The following skills are registered in `~/.copilot/skills/`:

- **agent-card-creation** — Generate Agent Cards from descriptions/URLs
- **agent-inspection** — Inspect agents to gather capability evidence
- **aicmm-scoring** — Score agents using the 12-dimension rubric
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

## The 12 Level 0 Dimensions

**Cognitive Core**
1. **Autonomy** — Self-directed action (0=none, 5=full goal autonomy)
2. **Reasoning & Planning** — Problem solving under uncertainty
3. **Memory & Context** — Information retention and temporal awareness
4. **Learning & Adaptation** — Improvement from experience

**Action & Integration**
5. **Tool Use & Integration** — Orchestrating external tools/APIs
6. **Collaboration & Social Intelligence** — Coordination with humans/agents, empathy, inclusivity
7. **Embodiment** — Physical/virtual presence (0 for software-only)

**Trust & Deployment**
8. **Explainability & Transparency** — Decision visibility and reviewability
9. **Safety & Containment** — Bounded operation and harm prevention
10. **Interoperability** — Cross-protocol and multi-agent compatibility
11. **Cost Efficiency** — Resource-aware execution at scale
12. **Domain Alignment** — Policy compliance, safety, auditability

## Governance Rules (CRITICAL)

- Autonomy >= 4 requires Reasoning >= 4
- Autonomy >= 4 requires Explainability >= 3
- Embodiment >= 3 requires Safety >= 4
- Collaboration >= 4 requires Interoperability >= 3
- Autonomy >= 4 requires Cost Efficiency >= 2
- Autonomy >= 4 requires Domain Alignment >= 3
- Autonomy >= 4 requires Reasoning >= 3

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
