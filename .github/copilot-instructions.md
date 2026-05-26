# AiCMM Copilot Instructions

This is the **Agent Capability Maturity Model (AiCMM)** project — an open-source Java framework for classifying AI agent capabilities across 12 dimensions (scored 0-5 at Level 0) + domain-specific Level 1 scoring.

## Available Agents (in-repo)

- **@aicmm** — Create, score, validate, and register Agent Cards (`.copilot/agents/aicmm.md`)

## Skills (in-repo `.copilot/skills/`)

- **create-agent-card** — Create full Agent Card from URL/description
- **register-agent-card** — Validate and register existing card into catalog
- **score-agent** — Score 12 dimensions with governance validation
- **inspect-agent** — Investigate agent capabilities from docs/URL

## MCP Server (Pure Java)

```bash
# Start AiCMM site (API server)
java -jar aicmm-site/target/aicmm-site-0.1.0-SNAPSHOT.jar

# Start MCP stdio server (connects to site API)
java -jar aicmm-cli/target/aicmm-cli-0.1.0-SNAPSHOT.jar --mcp
```

MCP config: `.mcp.json` (auto-detected by Claude Code, configurable for others)

### API Endpoints (http://localhost:8080/api)

| Method | Endpoint | Purpose |
|--------|----------|---------|
| POST | /api/agent-cards | Create new Agent Card |
| GET | /api/agent-cards | List all cards |
| GET | /api/agent-cards/{name} | Get card details |
| POST | /api/validate | Validate governance rules |
| POST | /api/agent-cards/_/score | Score breakdown |
| POST | /api/inspect | Inspect from URL/description |
| GET | /api/dimensions | Dimension definitions |
| GET | /api/schema | JSON Schema |

## Project Commands

```bash
# Build
mvn clean package -DskipTests

# Run documentation site
java -jar aicmm-site/target/aicmm-site-0.1.0-SNAPSHOT.jar

# CLI
java -jar aicmm-cli/target/aicmm-cli-0.1.0-SNAPSHOT.jar --help
java -jar aicmm-cli/target/aicmm-cli-0.1.0-SNAPSHOT.jar --mcp

# Quick: Create and register an agent card
curl -X POST http://localhost:8080/api/agent-cards -H "Content-Type: application/json" -d @examples/my-agent-card.json
```

## The 12 Level 0 Dimensions

**Cognitive Core (0-3)**
| Pos | Key | Dimension |
|-----|-----|-----------|
| 0 | autonomy | Autonomy |
| 1 | reasoning | Reasoning |
| 2 | memory | Memory |
| 3 | learning | Learning |

**Action & Integration (4-6)**
| Pos | Key | Dimension |
|-----|-----|-----------|
| 4 | toolUse | Tool Use |
| 5 | collaboration | Collaboration & Social Intelligence |
| 6 | embodiment | Embodiment |

**Trust & Deployment (7-11)**
| Pos | Key | Dimension |
|-----|-----|-----------|
| 7 | explainability | Explainability |
| 8 | safety | Safety |
| 9 | interoperability | Interoperability |
| 10 | costEfficiency | Cost Efficiency |
| 11 | domainAlignment | Domain Alignment |

## 7 Governance Rules (MUST ALL PASS)

1. Autonomy ≤ Reasoning + 1
2. Autonomy ≥ 4 → Explainability ≥ 3
3. Autonomy ≥ 4 → Safety ≥ 3
4. Collaboration ≥ 4 → Interoperability ≥ 3
5. Tool Use ≥ 4 → Cost Efficiency ≥ 2
6. Embodiment ≥ 3 → Domain Alignment ≥ 3
7. Tool Use ≥ 4 → Reasoning ≥ 3

## Project Structure

```
AiCMM/
├── .copilot/agents/    In-repo agents (aicmm.md)
├── .copilot/skills/    In-repo skills (create, register, score, inspect)
├── .mcp.json           MCP server config (auto-detected)
├── aicmm-core/         Core library (models, scoring, cards)
├── aicmm-inspector/    Agent investigation framework
├── aicmm-cli/          CLI + MCP stdio server (fat JAR)
├── aicmm-site/         Web server + REST API (Javalin)
├── mcp/                MCP config and tool definitions
├── docs/               Framework documentation
├── schemas/            JSON Schema definitions
├── examples/           Agent Cards (catalog source)
└── templates/          Reusable templates
```
