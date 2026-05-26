# AiCMM Tools

Tools available for use with the AiCMM framework. These are exposed via the MCP server
and REST API for programmatic access.

## MCP Tools (via `java -jar aicmm-cli.jar --mcp`)

| Tool | Purpose | Input |
|------|---------|-------|
| `aicmm_create_card` | Create a new Agent Card | agent object + capabilityProfile |
| `aicmm_inspect_agent` | Inspect agent from URL/description | url, description, name |
| `aicmm_validate_card` | Validate governance rules | full card JSON |
| `aicmm_score_card` | Score breakdown + maturity level | full card JSON |
| `aicmm_list_cards` | List catalog with filters | category, minScore |
| `aicmm_get_card` | Get specific card details | card name |
| `aicmm_get_dimensions` | Get dimension definitions | level, domain |
| `aicmm_get_schema` | Get JSON schema | none |

## REST API Tools (http://localhost:8080/api)

| Method | Endpoint | Purpose |
|--------|----------|---------|
| POST | `/api/agent-cards` | Create and register card |
| GET | `/api/agent-cards` | List all cards |
| GET | `/api/agent-cards/{name}` | Get specific card |
| POST | `/api/validate` | Validate card |
| POST | `/api/agent-cards/_/score` | Score card |
| POST | `/api/inspect` | Inspect agent |
| GET | `/api/dimensions` | Get dimensions |
| GET | `/api/schema` | Get schema |

## Configuration

### For Claude Code (auto-detected)
`.mcp.json` in project root:
```json
{
  "mcpServers": {
    "aicmm": {
      "command": "java",
      "args": ["-jar", "aicmm-cli/target/aicmm-cli-0.1.0-SNAPSHOT.jar", "--mcp"],
      "env": { "AICMM_API_URL": "http://localhost:8080/api" }
    }
  }
}
```

### For Copilot CLI
Use the in-repo `.copilot/agents/aicmm.md` agent which calls the API directly.

### For Gemini CLI
Reference `GEMINI.md` in project root for framework context.

### Environment Variables

| Variable | Default | Purpose |
|----------|---------|---------|
| `AICMM_API_URL` | `http://localhost:8080/api` | MCP server target API |

## Usage Examples

### Create a card via MCP stdio
```bash
echo '{"jsonrpc":"2.0","id":1,"method":"tools/call","params":{"name":"aicmm_create_card","arguments":{"agent":{"name":"My Agent","category":"digital","description":"A test agent"},"capabilityProfile":{"autonomy":{"position":0,"score":2,"confidence":"medium","evidence":"Basic task execution"}}}}}' | java -jar aicmm-cli/target/aicmm-cli-0.1.0-SNAPSHOT.jar --mcp
```

### Validate via curl
```bash
curl -s -X POST http://localhost:8080/api/validate \
  -H "Content-Type: application/json" \
  -d @examples/copilot-cli-agent-card.json | jq .
```
