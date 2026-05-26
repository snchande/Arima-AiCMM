---
name: build-and-test
description: "Builds the AiCMM project, runs tests, starts the server, and verifies all endpoints work. Use when checking code changes or setting up the development environment."
allowed-tools:
  - powershell
  - view
---

# Build and Test

## Purpose

Build the AiCMM Maven project, run tests, start the server, and verify everything works.

## Quick Commands

### Build (fast, no tests)
```bash
mvn clean package -DskipTests
```

### Build with tests
```bash
mvn clean install
```

### Build single module
```bash
mvn -pl aicmm-core test           # Core models/scoring
mvn -pl aicmm-cli -am package     # CLI with dependencies
mvn -pl aicmm-site -am package    # Site with dependencies
```

### Start server
```bash
java -jar aicmm-site/target/aicmm-site-0.1.0-SNAPSHOT.jar
```

### Start MCP server
```bash
java -jar aicmm-cli/target/aicmm-cli-0.1.0-SNAPSHOT.jar --mcp
```

## Verification Steps

After building, verify:
1. Site starts: `curl http://localhost:8080/` → 200
2. Catalog works: `curl http://localhost:8080/catalog` → 200
3. API works: `curl http://localhost:8080/api/agent-cards` → JSON array
4. Dimensions: `curl http://localhost:8080/api/dimensions` → 12 dimensions
5. Validate: `curl -X POST http://localhost:8080/api/validate -H "Content-Type: application/json" -d @examples/copilot-cli-agent-card.json`
6. MCP: `echo '{"jsonrpc":"2.0","id":1,"method":"initialize"}' | java -jar aicmm-cli/target/aicmm-cli-0.1.0-SNAPSHOT.jar --mcp`

## Common Issues

| Problem | Solution |
|---------|----------|
| JAR locked (can't clean) | Stop running server first |
| Port 8080 in use | Kill existing java process or use `--port 9090` |
| Tests fail | Check if examples/ JSON files are valid |
| Shade plugin slow | Normal for first build (~10s extra) |

## Prerequisites

- Java 17+
- Maven 3.8+
- No Node.js needed (project is 100% Java)
