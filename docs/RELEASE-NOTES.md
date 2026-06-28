# AiCMM Release Notes

## v0.2.0 — Level 0 / Level 1 Architecture (2026-05-24)

### Overview

Introduces the **Agent Capability Maturity Model (AiCMM)** 12-dimension, two-level architecture: a universal Level 0 baseline plus Level 1 domain-specific radar charts for specialized deployment contexts.

---

### Features

#### Core Framework
- **12-Dimension Two-Level Architecture** — Level 0 universal dimensions plus Level 1 domain-specific radar charts (each score 0-5)
- **Capability Fingerprint** — 12-value profile per agent enabling cross-type comparison
- **Expanded Governance Rules** — 7 built-in constraints for autonomy, explainability, safety, interoperability, cost, and domain fit
- **Agent Categories** — Digital, Embodied, Hybrid classification
- **Scoring Engine** — Validation, bottleneck detection, governance compliance checking

#### Agent Cards
- **Standardized JSON format** — Machine-readable capability descriptions
- **JSON Schema validation** — `schemas/agent-card.schema.json` (Draft 2020-12)
- **Rich metadata** — Tools, Skills, Plugins, MCP connections, Agent Relationships
- **Avatar system** — Archetype, tagline, personality traits, visual representation
- **Standards integration** — Embeddable in A2A, MCP, OpenAI protocols
- **Capability Resume** — Historical score tracking across versions

#### Documentation Site (`aicmm-site`)
- **Full Markdown rendering** with Mermaid diagram support
- **Agent Card Catalog** — Central registry with table view, score bars, mini radar charts
- **Create Card page** — Form-based Agent Card generation with live radar preview
- **Architecture page** — 10 Mermaid diagrams showing platform design
- **Framework page** — Complete AiCMM specification
- **Standards Integration Guide** — A2A, MCP, OpenAI embedding examples
- **Responsive design** — Works on desktop and mobile

#### CLI (`aicmm-cli`)
- **Picocli-based** command-line interface
- Commands: `inspect`, `classify`, `validate`, `score`
- Interactive and batch modes
- JSON output for pipeline integration

#### Java Library (`aicmm-core`)
- **Domain models** — `Dimension`, `DimensionScore`, `CapabilityProfile`, `AgentCard`
- **Builder pattern** — Fluent API for profile construction
- **ScoringEngine** — Programmatic validation and analysis
- **AgentCardSerializer** — Jackson-based JSON serialization
- **AgentInspector interface** — Extensible investigation framework

---

### Architecture

```
AiCMM/
├── aicmm-core/        Core library (models, scoring, cards)
├── aicmm-inspector/   Agent investigation framework
├── aicmm-cli/         Command-line interface
├── aicmm-site/        Documentation web server
├── docs/              Framework documentation
├── schemas/           JSON Schema definitions
├── examples/          Example Agent Cards
└── templates/         Reusable templates
```

---

### Agent Card Schema Highlights

| Field | Type | Description |
|-------|------|-------------|
| `schemaVersion` | string | Schema version `0.2.0` |
| `_dimensionSchema` | string | Fixed Level 0 schema identifier (`level0-v0.2`) |
| `agent.name` | string | Agent identifier |
| `agent.version` | string | Version being evaluated |
| `agent.vendor` | string | Creator organization |
| `agent.category` | enum | digital / embodied / hybrid |
| `capabilityProfile.*` | object | 12 dimension entries with `position`, `score`, and `confidence` |
| `level1Profile` | object | Domain-specific radar chart and scoring metadata |
| `governanceValidation` | object | 7-rule validation results and violations |
| `avatar` | object | Visual identity and personality |
| `tools` | array | External tools available |
| `assessmentMetadata` | object | Who, when, how assessed |

---

### Governance Rules

| Rule | Constraint | Rationale |
|------|-----------|-----------|
| Autonomy-Reasoning Foundation | Autonomy >= 4 requires Reasoning >= 4 | Advanced autonomy needs strong planning |
| Explainability Gate | Autonomy >= 4 requires Explainability >= 3 | High-autonomy systems must support review |
| Safety Gate | Embodiment >= 3 requires Safety >= 4 | Physical agents need robust containment |
| Collaboration-Interop Link | Collaboration >= 4 requires Interoperability >= 3 | Strong coordination needs shared protocols |
| Cost Awareness | Autonomy >= 4 requires CostEfficiency >= 2 | Advanced autonomy must stay economically bounded |
| Domain Alignment | Autonomy >= 4 requires DomainAlignment >= 3 | Deployment fit is mandatory |
| Reasoning Foundation | Autonomy >= 4 requires Reasoning >= 3 | Baseline reasoning floor remains enforced |

---

### Skills & Agents (Cross-CLI)

Registered at `~/.copilot/skills/` and `~/.copilot/agents/` for use across Copilot, Claude, and Gemini CLI tools:

**Skills:**
- `pdf-text-extraction` — Extract text from PDFs
- `article-to-markdown` — Convert articles to Markdown
- `java-project-scaffolding` — Scaffold Maven projects
- `json-schema-design` — Design JSON Schemas
- `markdown-to-pdf` — Generate PDFs from Markdown
- `agent-card-creation` — Generate AiCMM Agent Cards
- `agent-inspection` — Inspect agents from URLs
- `aicmm-scoring` — Score agents on 12 dimensions
- `catalog-management` — Manage Agent Card catalog

**Agents:**
- `aicmm-project-agent` — Full AiCMM domain knowledge
- `open-source-maintainer` — OSS project management
- `framework-evaluator` — Agent classification specialist

---

### Standards Integration

| Standard | Integration Method | Status |
|----------|--------------------|--------|
| **Google A2A** | `extensions.aicmm` in agent.json | Supported |
| **Anthropic MCP** | `metadata.aicmm` in server manifest | Supported |
| **OpenAI Functions** | `metadata.aicmm` in function def | Supported |
| **Custom Protocols** | Compact array or full JSON | Supported |

---

### Technical Details

- **Java 17+** required
- **Maven 3.8+** for building
- **Javalin 6** web framework (fat JAR via Shade plugin)
- **Jackson 2.17** for JSON processing
- **CommonMark** for Markdown rendering
- **Mermaid.js 10** for diagram rendering (client-side)
- **Picocli 4.7** for CLI framework

---

### Known Limitations

- Agent inspection from URL is manual (auto-scoring from descriptions planned for v0.2)
- Catalog search is client-side only (backend search planned for v0.2)
- No database backend yet (file-based storage in `examples/`)
- CLI commands are stubs pending full implementation

---

### Roadmap (v0.3+)

- [ ] Auto-scoring from agent documentation URLs
- [ ] Backend search with Elasticsearch/PostgreSQL FTS
- [ ] Agent Card federation (aggregate from multiple registries)
- [ ] Comparison view (side-by-side Level 0 and Level 1 radar charts)
- [ ] Export to A2A agent.json format
- [ ] Batch import/export
- [ ] REST API for CRUD operations
- [ ] Authentication and multi-user support
- [ ] Agent Card versioning and diff

---

### Contributors

- **Suresh Chande** — Framework author, architecture, implementation
- **Copilot CLI** — Code generation, documentation, tooling

---

### License

MIT License — free for commercial and open-source use.
