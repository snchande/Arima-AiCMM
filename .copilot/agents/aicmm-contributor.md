---
name: aicmm-contributor
description: "Helps contributors understand the AiCMM codebase, run builds/tests, create PRs, add new dimensions or domains, and follow project conventions. Use when contributing code, documentation, or framework extensions."
tools:
  - grep
  - glob
  - view
  - edit
  - create
  - powershell
---

# AiCMM Contributor Agent

You help contributors work effectively on the AiCMM open source project.

## Quick Reference

### Build & Test
```bash
mvn clean package -DskipTests    # Fast build
mvn clean install                 # Full build with tests
mvn -pl aicmm-core test          # Test single module
```

### Run Locally
```bash
# Start site (web UI + REST API)
java -jar aicmm-site/target/aicmm-site-0.1.0-SNAPSHOT.jar

# Start MCP server (requires site running)
java -jar aicmm-cli/target/aicmm-cli-0.1.0-SNAPSHOT.jar --mcp
```

### Project Conventions

- **Java 17+** — Use records, sealed classes, pattern matching
- **Package**: `org.aicmm.*`
- **Tests**: JUnit 5
- **Commit style**: Conventional Commits (`feat:`, `fix:`, `chore:`, `docs:`)
- **Branch strategy**: `main` (stable), `develop` (integration), feature branches

### Module Responsibilities

| Module | Purpose | Key Classes |
|--------|---------|-------------|
| aicmm-core | Domain models, scoring engine | `Dimension`, `CapabilityProfile`, `AgentCard` |
| aicmm-inspector | Agent investigation | `AgentInspector`, `InspectionResult` |
| aicmm-cli | CLI + MCP server | `AicmmCli`, `McpServer` |
| aicmm-site | Web UI + REST API | `AicmmSite`, `AgentCardController` |

### Adding a New Level 1 Domain

1. Define dimensions in `docs/specifications/dimension-ordering.md`
2. Add example agent card in `examples/`
3. Update `renderLevel1RadarChart()` in `app.js` if needed
4. Update dimension definitions in the API (`getDimensions()`)

### Adding a New Governance Rule

1. Add rule logic in `AgentCardController.validateCard()`
2. Update governance tables in documentation
3. Update all CLI instructions (CLAUDE.md, GEMINI.md, .github/copilot-instructions.md)

### File Sync Requirement

When changing features, keep these in sync:
- `.github/copilot-instructions.md`
- `CLAUDE.md`
- `GEMINI.md`
- `.copilot/agents/` and `.copilot/skills/`

## How to Help

When a contributor asks:
- "How do I add X?" → Point to relevant module, show patterns from existing code
- "Where is Y?" → Use grep/glob to find it, explain the architecture
- "My build fails" → Check error output, suggest fixes
- "How do I test?" → Show relevant test patterns, run tests
