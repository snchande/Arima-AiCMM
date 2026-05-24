package org.aicmm.site;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.javalin.http.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

/**
 * Handles Agent Card routes — lists, views, and renders Agent Cards as visual profiles.
 */
public class AgentCardController {

    private static final Logger log = LoggerFactory.getLogger(AgentCardController.class);
    private final Path examplesPath;
    private final Path schemasPath;
    private final ObjectMapper mapper;

    public AgentCardController(Path examplesPath, Path schemasPath) {
        this.examplesPath = examplesPath;
        this.schemasPath = schemasPath;
        this.mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    }

    public void listCards(Context ctx) {
        try {
            List<Map<String, Object>> cards = loadAllCards();
            StringBuilder html = new StringBuilder();
            html.append("<h1>Agent Cards</h1>");
            html.append("<p class='subtitle'>AiCMM capability fingerprints for evaluated agents</p>");
            html.append("<div class='card-grid'>");

            for (Map<String, Object> card : cards) {
                String name = (String) card.get("name");
                String fileName = (String) card.get("fileName");
                @SuppressWarnings("unchecked")
                Map<String, Object> profile = (Map<String, Object>) card.get("capabilityProfile");

                html.append("<div class='agent-card'>");
                html.append("<h3><a href='/agent-cards/").append(fileName).append("'>").append(name).append("</a></h3>");
                html.append("<p class='vendor'>").append(card.getOrDefault("vendor", "")).append("</p>");
                html.append("<p class='description'>").append(card.getOrDefault("description", "")).append("</p>");
                if (profile != null) {
                    html.append("<div class='scores'>");
                    for (Map.Entry<String, Object> entry : profile.entrySet()) {
                        if (entry.getValue() instanceof Map) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> score = (Map<String, Object>) entry.getValue();
                            int val = score.get("score") instanceof Number ? ((Number) score.get("score")).intValue() : 0;
                            html.append("<div class='score-bar'>");
                            html.append("<span class='dim-name'>").append(formatDimension(entry.getKey())).append("</span>");
                            html.append("<div class='bar-container'><div class='bar-fill' style='width:").append(val * 20).append("%'>").append(val).append("</div></div>");
                            html.append("</div>");
                        }
                    }
                    html.append("</div>");
                }
                html.append("</div>");
            }
            html.append("</div>");
            ctx.html(wrapInLayout("Agent Cards", html.toString(), "cards"));
        } catch (IOException e) {
            ctx.status(500).result("Error loading cards: " + e.getMessage());
        }
    }

    public void viewCard(Context ctx) {
        String name = ctx.pathParam("name");
        Path cardFile = examplesPath.resolve(name);
        if (!name.endsWith(".json")) cardFile = examplesPath.resolve(name + ".json");

        if (!cardFile.toFile().exists()) {
            ctx.status(404).result("Agent Card not found: " + name);
            return;
        }

        try {
            String json = Files.readString(cardFile);
            JsonNode node = mapper.readTree(json);
            String prettyJson = mapper.writeValueAsString(node);

            StringBuilder html = new StringBuilder();
            String agentName = node.has("agent") ? node.get("agent").path("name").asText("Unknown") : "Unknown";
            String category = node.has("agent") ? node.get("agent").path("category").asText("") : "";
            String vendor = node.has("agent") ? node.get("agent").path("vendor").asText("") : "";
            String description = node.has("agent") ? node.get("agent").path("description").asText("") : "";

            // Header with agent identity
            html.append("<div class='card-header'>");
            html.append("<h1>").append(escapeHtml(agentName)).append("</h1>");
            html.append("<div class='card-meta'>");
            if (!vendor.isEmpty()) html.append("<span class='badge vendor'>").append(escapeHtml(vendor)).append("</span>");
            if (!category.isEmpty()) html.append("<span class='badge category'>").append(escapeHtml(category.toUpperCase())).append("</span>");
            html.append("</div>");
            if (!description.isEmpty()) html.append("<p class='card-description'>").append(escapeHtml(description)).append("</p>");
            html.append("</div>");

            // Avatar section
            if (node.has("avatar")) {
                JsonNode avatar = node.get("avatar");
                html.append("<section class='card-section avatar-section'>");
                html.append("<h2>Agent Avatar</h2>");
                html.append("<div class='avatar-card'>");

                // SVG Avatar visualization
                html.append("<div class='avatar-visual' id='avatar-visual' data-profile='")
                        .append(node.has("capabilityProfile") ? mapper.writeValueAsString(node.get("capabilityProfile")) : "{}")
                        .append("' data-name='").append(escapeHtml(agentName))
                        .append("' data-archetype='").append(escapeHtml(avatar.path("archetype").asText("")))
                        .append("'></div>");

                html.append("<div class='avatar-details'>");
                html.append("<h3>").append(escapeHtml(avatar.path("archetype").asText("Unknown Archetype"))).append("</h3>");
                html.append("<p class='tagline'><em>").append(escapeHtml(avatar.path("tagline").asText(""))).append("</em></p>");

                // Personality traits
                if (avatar.has("personality")) {
                    html.append("<div class='traits'><strong>Personality:</strong> ");
                    avatar.get("personality").forEach(t -> html.append("<span class='trait-tag'>").append(escapeHtml(t.asText())).append("</span>"));
                    html.append("</div>");
                }

                // Strengths
                if (avatar.has("strengths")) {
                    html.append("<div class='strengths'><strong>Strengths:</strong><ul>");
                    avatar.get("strengths").forEach(s -> html.append("<li>").append(escapeHtml(s.asText())).append("</li>"));
                    html.append("</ul></div>");
                }

                // Weaknesses
                if (avatar.has("weaknesses")) {
                    html.append("<div class='weaknesses'><strong>Limitations:</strong><ul>");
                    avatar.get("weaknesses").forEach(w -> html.append("<li>").append(escapeHtml(w.asText())).append("</li>"));
                    html.append("</ul></div>");
                }

                html.append("</div>"); // avatar-details
                html.append("</div>"); // avatar-card
                html.append("</section>");
            }

            // Radar Chart
            if (node.has("capabilityProfile")) {
                html.append("<section class='card-section'>");
                html.append("<h2>Capability Fingerprint</h2>");
                html.append("<div class='radar-chart' id='radar-chart' data-profile='")
                        .append(mapper.writeValueAsString(node.get("capabilityProfile")))
                        .append("'></div>");
                html.append("</section>");
            }

            // Standards Integration
            if (node.has("standardsIntegration")) {
                JsonNode standards = node.get("standardsIntegration");
                html.append("<section class='card-section'>");
                html.append("<h2>Standards Integration</h2>");
                html.append("<p>How this agent can be incorporated into industry protocols:</p>");
                html.append("<div class='standards-grid'>");

                // A2A
                if (standards.has("a2a")) {
                    JsonNode a2a = standards.get("a2a");
                    html.append("<div class='standard-card'>");
                    html.append("<h3>Google A2A (Agent-to-Agent)</h3>");
                    html.append("<p class='compat'>Compatible: <strong>").append(a2a.path("compatible").asBoolean() ? "Yes" : "No").append("</strong></p>");
                    if (a2a.has("agentCardUrl")) {
                        html.append("<p>Discovery: <code>").append(escapeHtml(a2a.path("agentCardUrl").asText())).append("</code></p>");
                    }
                    if (a2a.has("capabilities")) {
                        html.append("<p>Capabilities: ");
                        a2a.get("capabilities").forEach(c -> html.append("<span class='trait-tag'>").append(escapeHtml(c.asText())).append("</span>"));
                        html.append("</p>");
                    }
                    if (a2a.has("embedding")) {
                        html.append("<p><strong>How to embed:</strong> Place a-CMM profile at <code>")
                                .append(escapeHtml(a2a.get("embedding").path("location").asText()))
                                .append("</code></p>");
                    }
                    html.append("<details><summary>A2A Embedding Example</summary>");
                    html.append("<pre class='json-view'><code>");
                    html.append(escapeHtml("{\n  \"name\": \"" + agentName + "\",\n  \"url\": \"https://agent.example.com\",\n  \"capabilities\": {...},\n  \"extensions\": {\n    \"aicmm\": {\n      \"schemaVersion\": \"0.1.0\",\n      \"capabilityProfile\": {...},\n      \"governanceCompliant\": true\n    }\n  }\n}"));
                    html.append("</code></pre></details>");
                    html.append("</div>");
                }

                // MCP
                if (standards.has("mcp")) {
                    JsonNode mcp = standards.get("mcp");
                    html.append("<div class='standard-card'>");
                    html.append("<h3>Anthropic MCP (Model Context Protocol)</h3>");
                    html.append("<p>Role: <strong>").append(escapeHtml(mcp.path("role").asText("client"))).append("</strong></p>");
                    if (mcp.has("connectedServers")) {
                        html.append("<p>Connected Servers: ");
                        mcp.get("connectedServers").forEach(s -> html.append("<code>").append(escapeHtml(s.asText())).append("</code> "));
                        html.append("</p>");
                    }
                    html.append("<p>Tools available: <strong>").append(mcp.path("toolCount").asInt(0)).append("</strong></p>");
                    html.append("<details><summary>MCP Manifest Annotation</summary>");
                    html.append("<pre class='json-view'><code>");
                    html.append(escapeHtml("{\n  \"tools\": [...],\n  \"metadata\": {\n    \"aicmm\": {\n      \"toolUse\": 5,\n      \"domainAlignment\": 4,\n      \"governanceCompliant\": true\n    }\n  }\n}"));
                    html.append("</code></pre></details>");
                    html.append("</div>");
                }

                // OpenAI
                if (standards.has("openai")) {
                    JsonNode openai = standards.get("openai");
                    html.append("<div class='standard-card'>");
                    html.append("<h3>OpenAI Function Calling</h3>");
                    html.append("<p>Compatible: <strong>").append(openai.path("functionCallingCompatible").asBoolean() ? "Yes" : "No").append("</strong></p>");
                    html.append("<p>Format: <code>").append(escapeHtml(openai.path("toolFormat").asText(""))).append("</code></p>");
                    html.append("<details><summary>Function Metadata Extension</summary>");
                    html.append("<pre class='json-view'><code>");
                    html.append(escapeHtml("{\n  \"type\": \"function\",\n  \"function\": {\n    \"name\": \"agent_task\",\n    \"metadata\": {\n      \"aicmm_profile\": [4,4,2,3,5,3,0,4],\n      \"governance_compliant\": true\n    }\n  }\n}"));
                    html.append("</code></pre></details>");
                    html.append("</div>");
                }

                html.append("</div>"); // standards-grid
                html.append("</section>");
            }

            // Full JSON
            html.append("<section class='card-section'>");
            html.append("<h2>Full Agent Card (JSON)</h2>");
            html.append("<pre class='json-view'><code>").append(escapeHtml(prettyJson)).append("</code></pre>");
            html.append("</section>");

            ctx.html(wrapInLayout("Agent Card: " + agentName, html.toString(), "cards"));
        } catch (IOException e) {
            ctx.status(500).result("Error: " + e.getMessage());
        }
    }

    public void viewSchema(Context ctx) {
        Path schemaFile = schemasPath.resolve("agent-card.schema.json");
        if (!schemaFile.toFile().exists()) {
            ctx.status(404).result("Schema not found");
            return;
        }
        try {
            String json = Files.readString(schemaFile);
            JsonNode node = mapper.readTree(json);
            String prettyJson = mapper.writeValueAsString(node);

            String html = "<h1>Agent Card JSON Schema</h1>" +
                    "<p>Version: 0.1.0 | Format: JSON Schema Draft 2020-12</p>" +
                    "<pre class='json-view'><code>" + escapeHtml(prettyJson) + "</code></pre>";
            ctx.html(wrapInLayout("Agent Card Schema", html, "schema"));
        } catch (IOException e) {
            ctx.status(500).result("Error: " + e.getMessage());
        }
    }

    public void listCardsJson(Context ctx) {
        try {
            ctx.json(loadAllCards());
        } catch (IOException e) {
            ctx.status(500).json(Map.of("error", e.getMessage()));
        }
    }

    private List<Map<String, Object>> loadAllCards() throws IOException {
        List<Map<String, Object>> cards = new ArrayList<>();
        if (!examplesPath.toFile().exists()) return cards;

        try (Stream<Path> files = Files.list(examplesPath)) {
            files.filter(f -> f.toString().endsWith(".json")).forEach(f -> {
                try {
                    JsonNode node = mapper.readTree(Files.readString(f));
                    Map<String, Object> card = new HashMap<>();
                    card.put("fileName", f.getFileName().toString());
                    if (node.has("agent")) {
                        card.put("name", node.get("agent").path("name").asText("Unknown"));
                        card.put("vendor", node.get("agent").path("vendor").asText(""));
                        card.put("description", node.get("agent").path("description").asText(""));
                        card.put("category", node.get("agent").path("category").asText(""));
                    }
                    if (node.has("capabilityProfile")) {
                        card.put("capabilityProfile", mapper.convertValue(node.get("capabilityProfile"), Map.class));
                    }
                    cards.add(card);
                } catch (IOException e) {
                    log.warn("Error reading card: {}", f, e);
                }
            });
        }
        return cards;
    }

    private String formatDimension(String key) {
        return switch (key) {
            case "autonomy" -> "Autonomy";
            case "reasoning" -> "Reasoning";
            case "learning" -> "Learning";
            case "memory" -> "Memory";
            case "toolUse" -> "Tool Use";
            case "collaboration" -> "Collaboration";
            case "embodiment" -> "Embodiment";
            case "domainAlignment" -> "Domain Alignment";
            default -> key;
        };
    }

    private String escapeHtml(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private String wrapInLayout(String title, String content, String activePage) {
        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>%s</title>
                    <link rel="stylesheet" href="/css/style.css">
                    <script src="https://cdn.jsdelivr.net/npm/mermaid@10/dist/mermaid.min.js"></script>
                    <script>mermaid.initialize({startOnLoad: true, theme: 'neutral'});</script>
                </head>
                <body>
                    <nav class="navbar">
                        <div class="nav-brand">
                            <a href="/">🤖 AiCMM</a>
                            <span class="nav-subtitle">Agent Capability Maturity Model</span>
                        </div>
                        <div class="nav-links">
                            <a href="/" class="%s">Home</a>
                            <a href="/framework" class="%s">Framework</a>
                            <a href="/architecture" class="%s">Architecture</a>
                            <a href="/docs/articles/introduction-linkedin.md" class="%s">Introduction</a>
                            <a href="/agent-cards" class="%s">Agent Cards</a>
                            <a href="/schema" class="%s">Schema</a>
                            <div class="nav-external">
                                <a href="https://medium.com/@sureshchande/agent-capability-maturity-model-a-unified-framework-for-evaluating-modern-ai-agents-bcb5b7a64bd7" target="_blank" title="Read on Medium">📝 Medium</a>
                                <a href="https://www.linkedin.com/pulse/all-ai-agents-same-so-why-do-we-treat-them-like-suresh-chande-oxgqc/" target="_blank" title="Read on LinkedIn">💼 LinkedIn</a>
                            </div>
                        </div>
                    </nav>
                    <main class="content">
                        %s
                    </main>
                    <footer class="footer">
                        <p>AiCMM — Agent Capability Maturity Model &copy; 2026 Suresh Chande |
                           <a href="https://github.com/snchande/AiCMM">GitHub</a> |
                           <a href="https://www.linkedin.com/in/sureshchande">LinkedIn</a> |
                           <a href="https://medium.com/@sureshchande">Medium</a>
                        </p>
                    </footer>
                    <script src="/js/app.js"></script>
                </body>
                </html>
                """.formatted(
                title,
                "home".equals(activePage) ? "active" : "",
                "framework".equals(activePage) ? "active" : "",
                "architecture".equals(activePage) ? "active" : "",
                "docs".equals(activePage) ? "active" : "",
                "cards".equals(activePage) ? "active" : "",
                "schema".equals(activePage) ? "active" : "",
                content
        );
    }
}
