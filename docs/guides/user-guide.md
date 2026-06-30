# AiCMM User Guide

How to use the AiCMM CLI and documentation site together to create, manage, and search Agent Cards.

---

## Quick Start

### 1. Build the Project

```bash
git clone https://github.com/snchande/Arima-AiCMM.git
cd AiCMM
mvn clean install
```

### 2. Start the Documentation Site

```bash
java -jar aicmm-site/target/aicmm-site-0.1.0-SNAPSHOT.jar --port 8090
```

Open **http://localhost:8090** in your browser.

### 3. Create Your First Agent Card

**Via Web UI:**
1. Click **"Create Card"** in the top navigation
2. Enter the agent URL or paste its description
3. Fill in identity fields (name, vendor, category)
4. List tools, skills, plugins, and MCP connections
5. Score each of the 12 Level 0 dimensions (0-5)
6. Click **"Generate Agent Card"**
7. Download the JSON or copy to clipboard

**Via CLI:**
```bash
# Interactive classification
java -jar aicmm-cli/target/aicmm-cli.jar classify --interactive

# From a URL (reads agent description automatically)
java -jar aicmm-cli/target/aicmm-cli.jar inspect --url https://docs.example.com/agent

# Validate an existing card
java -jar aicmm-cli/target/aicmm-cli.jar validate --card my-agent-card.json
```

---

## Web UI Pages

| Page | URL | Purpose |
|------|-----|---------|
| **Home** | `/` | Project overview and README |
| **Framework** | `/framework` | Full AiCMM specification |
| **Architecture** | `/architecture` | Platform architecture with Mermaid diagrams |
| **Catalog** | `/catalog` | Central registry of all Agent Cards — searchable table + visual profiles |
| **Create Card** | `/create-card` | Form to generate new Agent Cards from agent descriptions |
| **Schema** | `/schema` | JSON Schema for Agent Card validation |

---

## FAA — The Floating Assistant

Every page carries a floating **AiCMM Assistant** (bottom-right). Open or close it any time with **Alt+A** (or click the button); press **Esc** to close it when it isn't pinned.

- **Ask about the page** — it knows the 12 dimensions, the 7 governance rules, the Agency ladder, and how to create/score cards. With a local LLM CLI installed (GitHub Copilot, or Claude Code / Gemini) it answers agentically; otherwise a built-in knowledge base still helps offline.
- **Let it fill the form** — on **Create Card**, ask *"fill this for a fictional scheduling agent"* and watch it **type** each field in live and ramp the 12 score sliders, then refresh the radar. It can do this on any page that has a form.
- **Reword on the spot** — *"reword this heading to …"* updates the visible text immediately so you can preview wording before saving.
- **Pin (📌)** to keep it open (upright, highlighted when pinned; tilted/grey when not). It auto-tucks away when you click elsewhere.
- **Assistant Engine (⚙)** — choose which CLI/provider powers it, pick a model, adjust the generation tuning the CLI supports, and enable **power-user mode** to reveal **Develop & Extend** (edit code/docs, rebuild, restart, and open PRs from the browser). Settings persist to `~/.aicmm/faa-settings.json`.

> GitHub Copilot is driven through the official `@github/copilot-sdk`, so filling forms and longer requests work reliably and no extra browser tab is opened.

---

## Creating Agent Cards

### Method 1: Web UI (Create Card Page)

The **Create Card** page at `/create-card` provides a form-based workflow:

1. **Agent Source** — Paste a URL or description of the agent
2. **Agent Identity** — Name, version, creator/vendor, category, homepage
3. **Capabilities** — Tools, skills, plugins, MCP connections (comma-separated)
4. **Agent Relationships** — What agents it delegates to, what uses it, dependencies
5. **AiCMM Level 0 Scores** — Slide each universal dimension 0-5 based on evidence
6. **Level 1 Domain Profile** — Add domain-specific scoring when the agent operates in healthcare, transportation, finance, manufacturing, or another specialized environment
7. **Generate** — Produces a full Agent Card JSON with radar chart preview

### Method 2: CLI (Command Line)

```bash
# Inspect an agent from its URL/docs and auto-generate scores
aicmm inspect --url https://docs.github.com/copilot \
              --name "GitHub Copilot" \
              --vendor "GitHub" \
              --category digital \
              --output copilot-card.json

# Classify interactively with prompts for each dimension
aicmm classify --interactive --output my-agent.json

# Batch classify from a descriptor file
aicmm classify --descriptor agents/digital/my-agent.yaml

# Validate governance compliance
aicmm validate --card my-agent.json
```

### Method 3: Copilot/Claude/Gemini CLI Agent

When using the `aicmm-project-agent` in any AI CLI:

```
> Point aicmm at https://docs.anthropic.com/claude and create an Agent Card

> Classify the agent at this URL: https://openai.com/chatgpt
  Include its tools, skills, and what other agents it works with

> Update the copilot-cli agent card - increase reasoning to 5
```

---

## Agent Card Fields

Every Agent Card contains:

| Section | Fields | Purpose |
|---------|--------|---------|
| **Identity** | name, version, vendor, category, description, url | Who is this agent? |
| **Capability Profile** | Autonomy, Reasoning, Memory, Learning, Tool Use, Collaboration, Embodiment, Explainability, Safety, Interoperability, Cost Efficiency, Domain Alignment (0-5 each) with position + confidence metadata | What can it do? |
| **Level 1 Profile** | Domain-specific radar chart (8 dimensions per domain) | How does it perform in a specific sector? |
| **Governance Validation** | 7 rule checks with violations/remediation | Is it deployable under the new architecture? |
| **Agency Qualification** | Derived agency level (−2…+5), code, label, isAgent, rationale | Is it a genuine agent, and how agentic? |
| **Avatar** | archetype, tagline, personality, strengths, weaknesses | Visual representation |
| **Tools** | List of external tools it can invoke | What does it use? |
| **Skills** | Core competencies without tools | What is it good at? |
| **Plugins** | Extensions that add capabilities | What extends it? |
| **MCP Connections** | MCP servers it connects to | What protocols? |
| **Agent Relationships** | delegatesTo, usedBy, dependsOn | Who does it work with? |
| **Standards Integration** | A2A, MCP, OpenAI embedding | How to interoperate? |
| **Assessment Metadata** | assessedBy, date, methodology, sources | Who scored it and when? |
| **Capability Resume** | Historical scores per version | How has it evolved? |

---

## The Catalog (Designed for Scale)

The **Catalog** page at `/catalog` is the central registry for all evaluated agents. It's designed to support discovery at scale:

### Searching and Filtering

| Method | How |
|--------|-----|
| **By Category** | Filter by Digital / Embodied / Hybrid |
| **By Score** | Find agents with specific minimum scores (e.g., reasoning >= 4) |
| **By Vendor** | Group by creator/organization |
| **By Tools** | Find agents that use specific tools |
| **By Relationships** | Find agents that delegate to or depend on specific agents |
| **Full-text** | Search names, descriptions, evidence fields |

### Scale Design (Billions of Agents)

The catalog is designed to grow:

- **Indexed storage** — Agent Cards stored in JSON with indexes on name, category, vendor, scores
- **Faceted search** — Multiple filter dimensions applied simultaneously
- **Pagination** — Cursor-based for efficient traversal
- **Score-based ranking** — Sort by total score, average, or specific dimension
- **API-first** — REST API at `/api/agent-cards` for programmatic access
- **Federated** — Can aggregate cards from multiple registries

### API Endpoints

```bash
# List all cards
GET /api/agent-cards

# Search (future)
GET /api/agent-cards?category=digital&min_reasoning=4&vendor=Microsoft

# Get single card
GET /agent-cards/{filename}

# Submit new card (future)
POST /api/agent-cards
```

---

## Agent Card Storage

Cards are stored as JSON files in the `examples/` directory:

```
examples/
├── copilot-cli-agent-card.json
├── chatgpt-4o-agent-card.json
├── claude-sonnet-agent-card.json
├── autogpt-agent-card.json
└── ...
```

After creating a card via the web UI, save the downloaded JSON to this directory. It will appear in the catalog on next page load.

For production scale, cards would be stored in a database with full-text search (e.g., Elasticsearch, PostgreSQL with FTS).

---

## Using with AI CLI Tools

### GitHub Copilot CLI

The `aicmm-project-agent` is registered at `~/.copilot/agents/aicmm-project-agent.md`:

```bash
# In Copilot CLI, the agent has full AiCMM knowledge
> Create an agent card for Claude Sonnet 4
> Compare Copilot CLI vs ChatGPT on the reasoning dimension
> Which agents in the catalog score highest on tool use?

# Brochure → Agent Card → Footprint (radar + Agency)
> @aicmm Create an AiCMM Agent Card from ./mybot-brochure.pdf and save to examples/
> @aicmm Score examples/mybot-agent-card.json; show the radar fingerprint
> @aicmm What Agency level + Index does mybot reach, and which dimensions cap it?
```

### Claude CLI (claude.ai/code)

Copy the agent definition to Claude's config:
```bash
cp ~/.copilot/agents/aicmm-project-agent.md ~/.claude/agents/
```

### Gemini CLI

Copy to Gemini's agent directory:
```bash
cp ~/.copilot/agents/aicmm-project-agent.md ~/.gemini/agents/
```

---

## Skills Available

These reusable skills are registered for cross-CLI use:

| Skill | Purpose | Location |
|-------|---------|----------|
| `agent-card-creation` | Generate Agent Cards from descriptions | `~/.copilot/skills/` |
| `agent-inspection` | Inspect agents via URL/API | `~/.copilot/skills/` |
| `aicmm-scoring` | Score agents across the 12-dimension rubric | `~/.copilot/skills/` |
| `catalog-management` | Add/update/search catalog | `~/.copilot/skills/` |

---

## Governance Rules

Every generated card is automatically checked:

1. **Autonomy >= 4 requires Reasoning >= 4** — advanced autonomy needs strong planning
2. **Autonomy >= 4 requires Explainability >= 3** — high-autonomy systems must support review
3. **Embodiment >= 3 requires Safety >= 4** — physical agents need robust containment
4. **Collaboration >= 4 requires Interoperability >= 3** — strong coordination needs shared interfaces
5. **Autonomy >= 4 requires Cost Efficiency >= 2** — advanced autonomy must be economically bounded
6. **Autonomy >= 4 requires Domain Alignment >= 3** — deployment fit is mandatory
7. **Autonomy >= 4 requires Reasoning >= 3** — baseline reasoning floor remains enforced

Non-compliant cards are flagged with warnings and remediation steps.

---

## Agency Qualification (Derived — Position 12)

Every card also receives a **derived Agency Level**, computed automatically from the 12 scores
plus the governance result. It answers *"Is this a genuine agent, and how agentic is it?"* on a
signed ladder, and is never hand-authored.

A system counts as an **agent** (level ≥ 0) only when **Autonomy ≥ 2 and Reasoning ≥ 2**.

| Level | Code | Label |
|------:|------|-------|
| −2 | `SCRIPTED_AUTOMATION` | Non-Agent — Scripted Automation (RPA/ETL) |
| −1 | `REACTIVE_ASSISTANT` | Non-Agent — Reactive Assistant (FAQ bot, early Siri/Alexa) |
| 0 | `PROTO_AGENT` | Proto-Agent — Emerging Agency (e.g. AutoGPT) |
| 1 | `BASIC_AGENT` | Basic Agent — Qualified |
| 2 | `ADVANCED_AGENT` | Advanced Agent — Autonomous & Trust-Aligned |
| 3 | `GENERALIZED_AGENT` | Generalized Agent — Cutting-Edge |
| 4 | `HUMAN_LEVEL_AGENT` | Human-Level Agent (human-level general intelligence) |
| 5 | `HUMANOID_AGENT` | Humanoid Agent — Indistinguishable from Human |

Retrieve the full ladder via `GET /api/agency-levels` or the MCP tool `aicmm_get_agency_levels`.

---

## Workflow: End-to-End

```
1. Discover Agent      →  URL, docs, or description
2. Create Card         →  Web UI form or CLI command
3. Score Level 0       →  Evidence-based 0-5 across 12 universal dimensions
4. Add Level 1         →  Domain-specific radar chart when deployment context requires it
5. Validate            →  7 governance rules checked
6. Generate            →  JSON Agent Card + Radar Charts
7. Store               →  Save to examples/ or registry
8. Catalog             →  Appears in searchable catalog
9. Integrate           →  Embed in A2A, MCP, OpenAI protocols
10. Monitor            →  Reassess when agent evolves
```
