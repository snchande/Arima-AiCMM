# a-CMM User Guide

How to use the a-CMM CLI and documentation site together to create, manage, and search Agent Cards.

---

## Quick Start

### 1. Build the Project

```bash
git clone https://github.com/snchande/AiCMM.git
cd AiCMM
mvn clean install
```

### 2. Start the Documentation Site

```bash
java -jar aicmm-site/target/aicmm-site-0.1.0-SNAPSHOT.jar --port 8080
```

Open **http://localhost:8080** in your browser.

### 3. Create Your First Agent Card

**Via Web UI:**
1. Click **"Create Card"** in the top navigation
2. Enter the agent URL or paste its description
3. Fill in identity fields (name, vendor, category)
4. List tools, skills, plugins, and MCP connections
5. Score each of the 8 dimensions (0-5)
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
| **Framework** | `/framework` | Full a-CMM specification |
| **Architecture** | `/architecture` | Platform architecture with Mermaid diagrams |
| **Catalog** | `/catalog` | Central registry of all Agent Cards — searchable table + visual profiles |
| **Create Card** | `/create-card` | Form to generate new Agent Cards from agent descriptions |
| **Schema** | `/schema` | JSON Schema for Agent Card validation |

---

## Creating Agent Cards

### Method 1: Web UI (Create Card Page)

The **Create Card** page at `/create-card` provides a form-based workflow:

1. **Agent Source** — Paste a URL or description of the agent
2. **Agent Identity** — Name, version, creator/vendor, category, homepage
3. **Capabilities** — Tools, skills, plugins, MCP connections (comma-separated)
4. **Agent Relationships** — What agents it delegates to, what uses it, dependencies
5. **a-CMM Scores** — Slide each dimension 0-5 based on evidence
6. **Generate** — Produces a full Agent Card JSON with radar chart preview

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
| **Capability Profile** | 8 dimensions (0-5 each) with evidence | What can it do? |
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
# In Copilot CLI, the agent has full a-CMM knowledge
> Create an agent card for Claude Sonnet 4
> Compare Copilot CLI vs ChatGPT on the reasoning dimension
> Which agents in the catalog score highest on tool use?
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
| `aicmm-scoring` | Score agents across 8 dimensions | `~/.copilot/skills/` |
| `catalog-management` | Add/update/search catalog | `~/.copilot/skills/` |

---

## Governance Rules

Every generated card is automatically checked:

1. **Autonomy <= Domain Alignment + 1** — No ungoverned autonomy
2. **Embodiment >= 3 requires Alignment >= 3** — Physical agents need safety
3. **Tool Use >= 4 requires Alignment >= 3** — Powerful tools need oversight

Non-compliant cards are flagged with warnings and remediation steps.

---

## Workflow: End-to-End

```
1. Discover Agent     →  URL, docs, or description
2. Create Card        →  Web UI form or CLI command
3. Score Dimensions   →  Evidence-based 0-5 per dimension
4. Validate           →  Governance rules checked
5. Generate           →  JSON Agent Card + Radar Chart
6. Store              →  Save to examples/ or registry
7. Catalog            →  Appears in searchable catalog
8. Integrate          →  Embed in A2A, MCP, OpenAI protocols
9. Monitor            →  Reassess when agent evolves
```
