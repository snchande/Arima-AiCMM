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
            html.append("<div class='catalog-actions'>");
            html.append("<a href='/create-card' class='btn btn-primary'>+ Create New Agent Card</a>");
            html.append("<a href='/catalog' class='btn btn-secondary'>View Full Catalog</a>");
            html.append("</div>");
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
                    html.append("<div class='mini-radar' data-profile='").append(miniProfileJson(profile)).append("'></div>");
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

    public void catalog(Context ctx) {
        try {
            List<Map<String, Object>> cards = loadAllCards();
            StringBuilder html = new StringBuilder();

            // Catalog header with stats
            html.append("<div class='catalog-header'>");
            html.append("<h1>Agent Card Catalog</h1>");
            html.append("<p class='subtitle'>Central registry of all evaluated AI agents and tools</p>");
            html.append("<div class='catalog-stats'>");
            html.append("<div class='stat'><span class='stat-value'>").append(cards.size()).append("</span><span class='stat-label'>Agents Evaluated</span></div>");

            // Count by category
            long digital = cards.stream().filter(c -> "digital".equals(c.get("category"))).count();
            long embodied = cards.stream().filter(c -> "embodied".equals(c.get("category"))).count();
            long hybrid = cards.stream().filter(c -> "hybrid".equals(c.get("category"))).count();
            html.append("<div class='stat'><span class='stat-value'>").append(digital).append("</span><span class='stat-label'>Digital</span></div>");
            html.append("<div class='stat'><span class='stat-value'>").append(embodied).append("</span><span class='stat-label'>Embodied</span></div>");
            html.append("<div class='stat'><span class='stat-value'>").append(hybrid).append("</span><span class='stat-label'>Hybrid</span></div>");
            html.append("</div>"); // catalog-stats
            html.append("<div class='catalog-actions'>");
            html.append("<a href='/create-card' class='btn btn-primary'>+ Create New Agent Card</a>");
            html.append("</div>");
            html.append("</div>"); // catalog-header

            // Search and filter bar
            html.append("<div class='catalog-search'>");
            html.append("<input type='text' id='catalog-search' class='form-input' placeholder='Search agents by name, vendor, category, tools...' oninput='filterCatalog()'/>");
            html.append("<div class='filter-row'>");
            html.append("<select id='filter-category' class='form-input filter-select' onchange='filterCatalog()'>");
            html.append("<option value=''>All Categories</option>");
            html.append("<option value='digital'>Digital</option>");
            html.append("<option value='embodied'>Embodied</option>");
            html.append("<option value='hybrid'>Hybrid</option>");
            html.append("</select>");
            html.append("<select id='filter-min-score' class='form-input filter-select' onchange='filterCatalog()'>");
            html.append("<option value='0'>Min Avg Score: Any</option>");
            html.append("<option value='1'>Min Avg >= 1</option>");
            html.append("<option value='2'>Min Avg >= 2</option>");
            html.append("<option value='3'>Min Avg >= 3</option>");
            html.append("<option value='4'>Min Avg >= 4</option>");
            html.append("</select>");
            html.append("<span id='search-results-count' class='search-count'></span>");
            html.append("</div>");
            html.append("</div>");

            // Catalog table
            html.append("<div class='catalog-table-wrap'>");
            html.append("<table class='catalog-table'>");
            html.append("<thead><tr>");
            html.append("<th>Agent</th><th>Vendor</th><th>Category</th>");
            html.append("<th>AUT</th><th>REA</th><th>MEM</th><th>LRN</th><th>TUL</th><th>COL</th><th>EMB</th><th>EXP</th><th>SAF</th><th>INT</th><th>CST</th><th>DOM</th>");
            html.append("<th>Total</th><th>Avg</th>");
            html.append("</tr></thead><tbody>");

            for (Map<String, Object> card : cards) {
                String name = (String) card.get("name");
                String fileName = (String) card.get("fileName");
                String vendor = (String) card.getOrDefault("vendor", "");
                String category = (String) card.getOrDefault("category", "");
                @SuppressWarnings("unchecked")
                Map<String, Object> profile = (Map<String, Object>) card.get("capabilityProfile");

                html.append("<tr>");
                html.append("<td><a href='/agent-cards/").append(fileName).append("'>").append(escapeHtml(name)).append("</a></td>");
                html.append("<td>").append(escapeHtml(vendor)).append("</td>");
                html.append("<td><span class='badge category'>").append(escapeHtml(category.toUpperCase())).append("</span></td>");

                int total = 0;
                int scored = 0;
                String[] dims = {"autonomy", "reasoning", "memory", "learning", "toolUse", "collaboration", "embodiment", "explainability", "safety", "interoperability", "costEfficiency", "domainAlignment"};
                for (String dim : dims) {
                    int val = getScoreFromProfile(profile, dim);
                    if (val >= 0) {
                        scored++;
                        total += val;
                        String colorClass = val >= 4 ? "high" : val >= 2 ? "mid" : "low";
                        html.append("<td class='score-cell ").append(colorClass).append("'>").append(val).append("</td>");
                    } else {
                        html.append("<td class='score-cell'>-</td>");
                    }
                }
                double avg = scored > 0 ? total / (double) scored : 0;
                html.append("<td class='total-cell'><strong>").append(total).append("</strong></td>");
                html.append("<td>").append(String.format("%.1f", avg)).append("</td>");
                html.append("</tr>");
            }

            html.append("</tbody></table>");
            html.append("</div>");

            // Card grid with radar charts
            html.append("<h2 style='margin-top:2rem'>Visual Profiles</h2>");
            html.append("<div class='card-grid'>");
            for (Map<String, Object> card : cards) {
                String name = (String) card.get("name");
                String fileName = (String) card.get("fileName");
                @SuppressWarnings("unchecked")
                Map<String, Object> profile = (Map<String, Object>) card.get("capabilityProfile");

                html.append("<div class='agent-card catalog-card'>");
                html.append("<h3><a href='/agent-cards/").append(fileName).append("'>").append(escapeHtml(name)).append("</a></h3>");
                html.append("<p class='vendor'>").append(escapeHtml((String) card.getOrDefault("vendor", ""))).append("</p>");
                if (profile != null) {
                    html.append("<div class='radar-chart mini-radar-chart' data-profile='").append(miniProfileJson(profile)).append("'></div>");
                }
                html.append("</div>");
            }
            html.append("</div>");

            ctx.html(wrapInLayout("Agent Card Catalog", html.toString(), "catalog"));
        } catch (IOException e) {
            ctx.status(500).result("Error: " + e.getMessage());
        }
    }

    public void createCardForm(Context ctx) {
        StringBuilder html = new StringBuilder();
        html.append("<h1>Create Agent Card</h1>");
        html.append("<p class='subtitle'>Point to an AI agent or tool to generate its a-CMM Agent Card</p>");

        html.append("<div class='create-card-form'>");
        html.append("<form id='agent-card-form'>");

        // Agent URL / Description input
        html.append("<fieldset class='form-section'>");
        html.append("<legend>Agent Source</legend>");
        html.append("<p class='form-help'>Provide a URL to the agent's documentation, homepage, or API spec. The system will analyze it and generate a capability profile.</p>");
        html.append("<div class='form-group'>");
        html.append("<label for='agent-url'>Agent URL or Documentation Link</label>");
        html.append("<input type='url' id='agent-url' name='agentUrl' placeholder='https://docs.example.com/my-agent' class='form-input'/>");
        html.append("</div>");
        html.append("<div class='form-group'>");
        html.append("<label for='agent-desc'>Or paste agent description</label>");
        html.append("<textarea id='agent-desc' name='agentDescription' rows='6' class='form-input' placeholder='Describe the agent capabilities, tools it uses, how it works...'></textarea>");
        html.append("</div>");
        html.append("</fieldset>");

        // Agent Identity
        html.append("<fieldset class='form-section'>");
        html.append("<legend>Agent Identity</legend>");
        html.append("<div class='form-row'>");
        html.append("<div class='form-group'><label for='name'>Name *</label><input type='text' id='name' name='name' required class='form-input'/></div>");
        html.append("<div class='form-group'><label for='version'>Version</label><input type='text' id='version' name='version' placeholder='1.0.0' class='form-input'/></div>");
        html.append("</div>");
        html.append("<div class='form-row'>");
        html.append("<div class='form-group'><label for='vendor'>Creator / Vendor *</label><input type='text' id='vendor' name='vendor' required class='form-input'/></div>");
        html.append("<div class='form-group'><label for='category'>Category</label><select id='category' name='category' class='form-input'><option value='digital'>Digital</option><option value='embodied'>Embodied</option><option value='hybrid'>Hybrid</option></select></div>");
        html.append("</div>");
        html.append("<div class='form-group'><label for='description'>Description</label><textarea id='description' name='description' rows='3' class='form-input'></textarea></div>");
        html.append("<div class='form-group'><label for='url'>Homepage URL</label><input type='url' id='url' name='url' class='form-input'/></div>");
        html.append("</fieldset>");

        // Tools, Skills, Plugins
        html.append("<fieldset class='form-section'>");
        html.append("<legend>Capabilities</legend>");
        html.append("<div class='form-group'><label for='tools'>Tools (comma-separated)</label><input type='text' id='tools' name='tools' placeholder='shell, file-edit, web-search, git...' class='form-input'/></div>");
        html.append("<div class='form-group'><label for='skills'>Skills (comma-separated)</label><input type='text' id='skills' name='skills' placeholder='code-generation, debugging, documentation...' class='form-input'/></div>");
        html.append("<div class='form-group'><label for='plugins'>Plugins / Extensions (comma-separated)</label><input type='text' id='plugins' name='plugins' placeholder='code-interpreter, image-gen...' class='form-input'/></div>");
        html.append("<div class='form-group'><label for='mcps'>MCP Connections (comma-separated)</label><input type='text' id='mcps' name='mcps' placeholder='github-mcp, filesystem-mcp...' class='form-input'/></div>");
        html.append("</fieldset>");

        // Agent Relationships
        html.append("<fieldset class='form-section'>");
        html.append("<legend>Agent Relationships</legend>");
        html.append("<div class='form-group'><label for='delegates-to'>Delegates To (other agents)</label><input type='text' id='delegates-to' name='delegatesTo' placeholder='sub-agent-1, research-agent...' class='form-input'/></div>");
        html.append("<div class='form-group'><label for='used-by'>Used By (other agents)</label><input type='text' id='used-by' name='usedBy' placeholder='orchestrator-agent, pipeline...' class='form-input'/></div>");
        html.append("<div class='form-group'><label for='depends-on'>Depends On (services/models)</label><input type='text' id='depends-on' name='dependsOn' placeholder='GPT-4, Claude, vector-db...' class='form-input'/></div>");
        html.append("</fieldset>");

        // Scoring
        html.append("<fieldset class='form-section'>");
        html.append("<legend>a-CMM Level 0 Scores (0-5)</legend>");
        html.append("<p class='form-help'>Score each dimension based on observable evidence. Positions are fixed (0-11) for consistent radar chart comparison.</p>");
        html.append("<div class='score-inputs'>");
        html.append("<h4 class='score-group-label'>Cognitive Core (Positions 0-3)</h4>");
        String[][] cogDims = {
            {"autonomy", "0 — Autonomy", "Self-directed action without human intervention"},
            {"reasoning", "1 — Reasoning & Planning", "Structured problem-solving under uncertainty"},
            {"memory", "2 — Memory & Context", "Information retention and temporal awareness"},
            {"learning", "3 — Learning & Adaptation", "Ability to improve from experience"}
        };
        for (String[] dim : cogDims) {
            html.append("<div class='score-input-row'>");
            html.append("<label for='score-").append(dim[0]).append("'>").append(dim[1]).append("</label>");
            html.append("<input type='range' id='score-").append(dim[0]).append("' name='score_").append(dim[0]).append("' min='0' max='5' value='0' oninput='updateScoreDisplay(this)'/>");
            html.append("<span class='score-display' id='display-").append(dim[0]).append("'>0</span>");
            html.append("<span class='score-hint'>").append(dim[2]).append("</span>");
            html.append("</div>");
        }
        html.append("<h4 class='score-group-label'>Action & Integration (Positions 4-6)</h4>");
        String[][] actDims = {
            {"toolUse", "4 — Tool Use & Integration", "Orchestrating external tools and APIs"},
            {"collaboration", "5 — Collaboration & Social Intelligence", "Coordination with humans/agents, empathy, age-appropriate communication, inclusivity"},
            {"embodiment", "6 — Embodiment", "Physical/virtual presence (0 for software-only)"}
        };
        for (String[] dim : actDims) {
            html.append("<div class='score-input-row'>");
            html.append("<label for='score-").append(dim[0]).append("'>").append(dim[1]).append("</label>");
            html.append("<input type='range' id='score-").append(dim[0]).append("' name='score_").append(dim[0]).append("' min='0' max='5' value='0' oninput='updateScoreDisplay(this)'/>");
            html.append("<span class='score-display' id='display-").append(dim[0]).append("'>0</span>");
            html.append("<span class='score-hint'>").append(dim[2]).append("</span>");
            html.append("</div>");
        }
        html.append("<h4 class='score-group-label'>Trust & Deployment (Positions 7-11)</h4>");
        String[][] trustDims = {
            {"explainability", "7 — Explainability & Transparency", "Can you understand why it did what it did?"},
            {"safety", "8 — Safety & Robustness", "Graceful degradation, adversarial resistance"},
            {"interoperability", "9 — Interoperability & Standards", "Protocol support (A2A, MCP, OpenAI)"},
            {"costEfficiency", "10 — Cost & Resource Efficiency", "Budget-aware, optimized resource use"},
            {"domainAlignment", "11 — Domain Alignment & Governance", "Regulatory compliance, auditability"}
        };
        for (String[] dim : trustDims) {
            html.append("<div class='score-input-row'>");
            html.append("<label for='score-").append(dim[0]).append("'>").append(dim[1]).append("</label>");
            html.append("<input type='range' id='score-").append(dim[0]).append("' name='score_").append(dim[0]).append("' min='0' max='5' value='0' oninput='updateScoreDisplay(this)'/>");
            html.append("<span class='score-display' id='display-").append(dim[0]).append("'>0</span>");
            html.append("<span class='score-hint'>").append(dim[2]).append("</span>");
            html.append("</div>");
        }
        html.append("</div>");
        html.append("</fieldset>");

        // Preview & Generate
        html.append("<div class='form-actions'>");
        html.append("<button type='button' class='btn btn-primary' onclick='generateCard()'>Generate Agent Card</button>");
        html.append("<button type='button' class='btn btn-secondary' onclick='previewRadar()'>Preview Radar Chart</button>");
        html.append("</div>");
        html.append("</form>");

        // Preview area
        html.append("<div id='preview-area' class='preview-area' style='display:none'>");
        html.append("<h2>Generated Agent Card</h2>");
        html.append("<div class='preview-split'>");
        html.append("<div class='radar-chart' id='preview-radar'></div>");
        html.append("<pre class='json-view' id='preview-json'><code></code></pre>");
        html.append("</div>");
        html.append("<div class='preview-actions'>");
        html.append("<button class='btn btn-primary' onclick='downloadCard()'>Download JSON</button>");
        html.append("<button class='btn btn-secondary' onclick='copyCard()'>Copy to Clipboard</button>");
        html.append("</div>");
        html.append("</div>");

        html.append("</div>"); // create-card-form

        ctx.html(wrapInLayout("Create Agent Card", html.toString(), "create"));
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

            // Radar Chart - Level 0
            if (node.has("capabilityProfile")) {
                html.append("<section class='card-section'>");
                html.append("<h2>Level 0 — Universal Capability Fingerprint</h2>");
                html.append("<p class='section-subtitle'>12 dimensions across Cognitive Core, Action & Integration, and Trust & Deployment</p>");
                html.append("<div class='radar-chart' id='radar-chart' data-profile='")
                        .append(mapper.writeValueAsString(node.get("capabilityProfile")))
                        .append("'></div>");
                html.append("</section>");
            }

            // Radar Chart - Level 1 (Domain-Specific)
            if (node.has("level1Profile")) {
                JsonNode l1 = node.get("level1Profile");
                String domain = l1.path("domain").asText("domain");
                html.append("<section class='card-section level1-section'>");
                html.append("<h2>Level 1 — ").append(escapeHtml(domain.substring(0, 1).toUpperCase() + domain.substring(1))).append(" Domain Deep-Dive</h2>");
                html.append("<p class='section-subtitle'>Domain-specific dimensions with specialized scoring criteria</p>");
                html.append("<div class='level1-radar-chart' data-level1='")
                        .append(escapeHtml(mapper.writeValueAsString(l1)))
                        .append("'></div>");
                html.append("</section>");
            }

            // Governance Validation
            if (node.has("governanceValidation")) {
                JsonNode gov = node.get("governanceValidation");
                boolean compliant = gov.path("overallCompliant").asBoolean(false);
                html.append("<section class='card-section'>");
                html.append("<h2>Governance Validation</h2>");
                html.append("<div class='governance-badge ").append(compliant ? "compliant" : "non-compliant").append("'>");
                html.append(compliant ? "✓ ALL RULES PASSED" : "✗ GOVERNANCE VIOLATION");
                html.append("</div>");
                if (gov.has("rules")) {
                    html.append("<table class='governance-table'><thead><tr><th>Rule</th><th>Constraint</th><th>Result</th></tr></thead><tbody>");
                    gov.get("rules").forEach(rule -> {
                        String result = rule.path("result").asText("");
                        String cls = "PASS".equals(result) ? "pass" : "N/A".equals(result) ? "na" : "fail";
                        html.append("<tr><td>").append(escapeHtml(rule.path("rule").asText("")))
                                .append("</td><td><code>").append(escapeHtml(rule.path("constraint").asText("")))
                                .append("</code></td><td class='gov-").append(cls).append("'>").append(result).append("</td></tr>");
                    });
                    html.append("</tbody></table>");
                }
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
            case "memory" -> "Memory";
            case "learning" -> "Learning";
            case "toolUse" -> "Tool Use";
            case "collaboration" -> "Social Intelligence";
            case "embodiment" -> "Embodiment";
            case "explainability" -> "Explainability";
            case "safety" -> "Safety";
            case "interoperability" -> "Interoperability";
            case "costEfficiency" -> "Cost Efficiency";
            case "domainAlignment" -> "Domain Alignment";
            default -> key;
        };
    }

    private int getScoreFromProfile(Map<String, Object> profile, String dim) {
        if (profile == null) return 0;
        Object val = profile.get(dim);
        if (val == null) return -1; // Not scored
        if (val instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> scoreMap = (Map<String, Object>) val;
            Object score = scoreMap.get("score");
            if (score == null) return -1;
            return score instanceof Number ? ((Number) score).intValue() : 0;
        }
        return val instanceof Number ? ((Number) val).intValue() : 0;
    }

    private String miniProfileJson(Map<String, Object> profile) {
        StringBuilder json = new StringBuilder("{");
        // Fixed positions: Cognitive Core → Action & Integration → Trust & Deployment
        String[] dims = {"autonomy", "reasoning", "memory", "learning", "toolUse", "collaboration", "embodiment", "explainability", "safety", "interoperability", "costEfficiency", "domainAlignment"};
        boolean first = true;
        for (String dim : dims) {
            int score = getScoreFromProfile(profile, dim);
            if (score < 0) continue; // Skip unscored
            if (!first) json.append(",");
            first = false;
            json.append("\"").append(dim).append("\":{\"score\":").append(score).append("}");
        }
        json.append("}");
        return escapeHtml(json.toString());
    }

    private String escapeHtml(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
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
                            <a href="/catalog" class="%s">Catalog</a>
                            <a href="/create-card" class="%s">Create Card</a>
                            <a href="/user-guide" class="%s">Guide</a>
                            <a href="/release-notes" class="%s">Releases</a>
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
                "catalog".equals(activePage) || "cards".equals(activePage) ? "active" : "",
                "create".equals(activePage) ? "active" : "",
                "user-guide".equals(activePage) ? "active" : "",
                "release-notes".equals(activePage) ? "active" : "",
                "schema".equals(activePage) ? "active" : "",
                content
        );
    }
}
