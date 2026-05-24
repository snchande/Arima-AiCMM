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
            html.append("<h1>Agent Card: ").append(agentName).append("</h1>");

            // Radar chart placeholder using Mermaid
            if (node.has("capabilityProfile")) {
                html.append("<h2>Capability Fingerprint</h2>");
                html.append("<div class='radar-chart' id='radar-chart' data-profile='")
                        .append(mapper.writeValueAsString(node.get("capabilityProfile")))
                        .append("'></div>");
            }

            // Card details
            html.append("<h2>Full Agent Card</h2>");
            html.append("<pre class='json-view'><code>").append(escapeHtml(prettyJson)).append("</code></pre>");

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
                "docs".equals(activePage) ? "active" : "",
                "cards".equals(activePage) ? "active" : "",
                "schema".equals(activePage) ? "active" : "",
                content
        );
    }
}
