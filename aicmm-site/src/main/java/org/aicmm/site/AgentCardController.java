package org.aicmm.site;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
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

    public void createCard(Context ctx) {
        ctx.contentType("application/json");
        try {
            JsonNode card = mapper.readTree(ctx.body());
            if (!(card instanceof ObjectNode cardObj)) {
                ctx.status(400).json(Map.of("error", "Card body must be a JSON object"));
                return;
            }

            if (!cardObj.has("agent") || !cardObj.path("agent").has("name")) {
                ctx.status(400).json(Map.of("error", "Missing required field: agent.name"));
                return;
            }

            if (!cardObj.has("schemaVersion")) {
                cardObj.put("schemaVersion", "0.2.0");
            }

            List<Map<String, Object>> schemaChecks = evaluateSchemaChecks(cardObj);
            boolean schemaValid = schemaChecks.stream().allMatch(check -> Boolean.TRUE.equals(check.get("passed")));
            if (!schemaValid) {
                ctx.status(400).json(Map.of(
                        "error", "Card failed schema validation",
                        "schemaValid", false,
                        "checks", schemaChecks
                ));
                return;
            }

            Map<String, Object> governanceValidation = buildGovernanceValidation(cardObj);
            cardObj.set("governanceValidation", mapper.valueToTree(governanceValidation));
            cardObj.set("agencyQualification", mapper.valueToTree(buildAgencyQualification(cardObj)));

            String name = cardObj.path("agent").path("name").asText().toLowerCase()
                    .replaceAll("[^a-z0-9]+", "-")
                    .replaceAll("^-|-$", "");
            if (name.isBlank()) {
                ctx.status(400).json(Map.of("error", "agent.name must contain at least one alphanumeric character"));
                return;
            }
            String fileName = name + "-agent-card.json";

            Files.createDirectories(examplesPath);
            Path filePath = examplesPath.resolve(fileName);
            mapper.writerWithDefaultPrettyPrinter().writeValue(filePath.toFile(), cardObj);

            ctx.status(201).json(Map.of(
                    "status", "created",
                    "fileName", fileName,
                    "url", "/agent-cards/" + fileName.replace(".json", ""),
                    "card", mapper.convertValue(cardObj, Map.class)
            ));
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage()));
        }
    }

    public void getCardJson(Context ctx) {
        ctx.contentType("application/json");
        String name = ctx.pathParam("name");
        String fileName = name.endsWith(".json") ? name : name + ".json";
        Path cardPath = examplesPath.resolve(fileName);
        if (!cardPath.toFile().exists()) {
            ctx.status(404).json(Map.of("error", "Card not found: " + name));
            return;
        }
        try {
            JsonNode card = mapper.readTree(cardPath.toFile());
            ctx.json(mapper.convertValue(card, Map.class));
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage()));
        }
    }

    public void scoreCard(Context ctx) {
        ctx.contentType("application/json");
        try {
            JsonNode card = mapper.readTree(ctx.body());
            JsonNode profile = card.path("capabilityProfile");

            Map<String, Object> scoring = new LinkedHashMap<>();
            int total = 0;
            int count = 0;
            for (Map<String, Object> dimension : buildLevel0Dimensions()) {
                String key = (String) dimension.get("key");
                int score = getScore(profile, key);
                total += score;
                count++;
                scoring.put(key, Map.of(
                        "position", dimension.get("position"),
                        "label", dimension.get("label"),
                        "score", score,
                        "group", dimension.get("group")
                ));
            }

            double average = count > 0 ? (double) total / count : 0;
            String maturityLevel;
            if (average >= 4.5) maturityLevel = "Mastery";
            else if (average >= 3.5) maturityLevel = "Expert";
            else if (average >= 2.5) maturityLevel = "Advanced";
            else if (average >= 1.5) maturityLevel = "Intermediate";
            else if (average >= 0.5) maturityLevel = "Basic";
            else maturityLevel = "Nascent";

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("scores", scoring);
            response.put("totalScore", total);
            response.put("maxPossible", 60);
            response.put("average", Math.round(average * 100.0) / 100.0);
            response.put("maturityLevel", maturityLevel);
            response.put("dimensionCount", count);
            response.put("governanceValidation", buildGovernanceValidation(card));
            response.put("agencyQualification", buildAgencyQualification(card));
            ctx.json(response);
        } catch (Exception e) {
            ctx.status(400).json(Map.of("error", e.getMessage()));
        }
    }

    public void inspectAgent(Context ctx) {
        ctx.contentType("application/json");
        try {
            JsonNode body = mapper.readTree(ctx.body());
            String url = body.path("url").asText("");
            String description = body.path("description").asText("");
            String name = body.path("name").asText("Unknown Agent");

            Map<String, Object> template = new LinkedHashMap<>();
            template.put("schemaVersion", "0.2.0");
            template.put("agent", Map.of(
                    "name", name,
                    "version", "1.0.0",
                    "vendor", "",
                    "category", "digital",
                    "description", description.isEmpty() ? "Agent at " + url : description
            ));

            Map<String, Object> profile = new LinkedHashMap<>();
            for (Map<String, Object> dimension : buildLevel0Dimensions()) {
                profile.put((String) dimension.get("key"), Map.of(
                        "position", dimension.get("position"),
                        "score", 0,
                        "confidence", "low",
                        "evidence", "Needs assessment"
                ));
            }
            template.put("capabilityProfile", profile);
            template.put("tools", List.of());
            template.put("skills", List.of());
            template.put("plugins", List.of());
            template.put("mcps", List.of());
            template.put("agentRelationships", Map.of(
                    "delegatesTo", List.of(),
                    "usedBy", List.of(),
                    "dependsOn", List.of()
            ));
            template.put("_inspectionSource", Map.of("url", url, "description", description));

            ctx.json(template);
        } catch (Exception e) {
            ctx.status(400).json(Map.of("error", e.getMessage()));
        }
    }

    public void getSchema(Context ctx) {
        ctx.contentType("application/json");
        Path schemaFile = schemasPath.resolve("agent-card.schema.json");
        if (!schemaFile.toFile().exists()) {
            ctx.status(404).json(Map.of("error", "Schema not found"));
            return;
        }
        try {
            JsonNode schema = mapper.readTree(schemaFile.toFile());
            ctx.json(mapper.convertValue(schema, Map.class));
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage()));
        }
    }

    public void getDimensions(Context ctx) {
        ctx.contentType("application/json");
        String level = Optional.ofNullable(ctx.queryParam("level")).orElse("all");
        String domain = ctx.queryParam("domain");

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("schemaVersion", "0.2.0");
        response.put("requestedLevel", level);
        if (domain != null && !domain.isBlank()) {
            response.put("requestedDomain", domain);
        }

        if (!"1".equals(level)) {
            response.put("level0", buildLevel0Dimensions());
        }
        if (!"0".equals(level)) {
            Map<String, List<Map<String, Object>>> level1 = buildLevel1Dimensions();
            if (domain != null && !domain.isBlank()) {
                response.put("level1", Map.of(domain, level1.getOrDefault(domain, List.of())));
            } else {
                response.put("level1", level1);
            }
        }

        // Agency Qualification Layer — derived 13th dimension (position 12)
        Map<String, Object> agencyLayer = new LinkedHashMap<>();
        agencyLayer.put("position", 12);
        agencyLayer.put("dimension", "Agency Qualification");
        agencyLayer.put("derived", true);
        agencyLayer.put("scale", "-2..5");
        agencyLayer.put("description", "Derived classification appended after the 12 core dimensions, "
                + "indicating whether a system is an agent and how agentic it is.");
        agencyLayer.put("ladder", buildAgencyLadder());
        response.put("agencyLayer", agencyLayer);

        ctx.json(response);
    }

    /** GET /api/agency-levels — the Agency Qualification ladder definitions. */
    public void getAgencyLevels(Context ctx) {
        ctx.contentType("application/json");
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("position", 12);
        response.put("dimension", "Agency Qualification");
        response.put("derived", true);
        response.put("scale", "-2..5");
        response.put("threshold", "Agent (level >= 0) requires Autonomy >= 2 and Reasoning >= 2");
        response.put("ladder", buildAgencyLadder());
        ctx.json(response);
    }

    public void validateCard(Context ctx) {
        ctx.contentType("application/json");
        try {
            JsonNode card = mapper.readTree(ctx.body());
            List<Map<String, Object>> schemaChecks = evaluateSchemaChecks(card);
            boolean schemaValid = schemaChecks.stream().allMatch(check -> Boolean.TRUE.equals(check.get("passed")));
            Map<String, Object> governance = buildGovernanceValidation(card);
            boolean governanceValid = Boolean.TRUE.equals(governance.get("valid"));

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("valid", schemaValid && governanceValid);
            response.put("schemaValid", schemaValid);
            response.put("governanceValid", governanceValid);
            response.put("checks", schemaChecks);
            response.put("rules", governance.get("rules"));
            response.put("rulesChecked", governance.get("rulesChecked"));
            response.put("agencyQualification", buildAgencyQualification(card));
            ctx.json(response);
        } catch (Exception e) {
            ctx.status(400).json(Map.of("error", e.getMessage()));
        }
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
            html.append("<th>Total</th><th>Avg</th><th>Agency</th>");
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
                Map<String, Object> agency = agencyForProfile(profile);
                int aqLevel = ((Number) agency.getOrDefault("level", 0)).intValue();
                boolean aqIsAgent = Boolean.TRUE.equals(agency.get("isAgent"));
                int aqIndex = ((Number) agency.getOrDefault("index", 0)).intValue();
                double aqNeedle = ((Number) agency.getOrDefault("needle", (double) aqLevel)).doubleValue();
                html.append("<td class='agency-cell ").append(aqIsAgent ? "agency-agent" : "agency-nonagent")
                        .append("' title='").append(escapeHtml(String.valueOf(agency.get("label"))))
                        .append(" · Index ").append(aqIndex).append("/100'>")
                        .append("<span class='agency-cell-level'>").append(aqLevel >= 0 ? "+" + aqLevel : String.valueOf(aqLevel)).append("</span>")
                        .append(renderAgencyStripMini(aqLevel, aqNeedle))
                        .append("</td>");
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
        html.append("<p class='subtitle'>Point to an AI agent or tool to generate its AiCMM Agent Card</p>");

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
        html.append("<legend>AiCMM Level 0 Scores (0-5)</legend>");
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
        html.append("<div id='preview-agency'></div>");
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
            if (node instanceof ObjectNode obj && node.has("capabilityProfile") && !node.has("agencyQualification")) {
                obj.set("agencyQualification", mapper.valueToTree(buildAgencyQualification(node)));
            }
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

            // Agency Qualification Layer — derived 13th dimension (position 12)
            if (node.has("agencyQualification")) {
                JsonNode aq = node.get("agencyQualification");
                int level = aq.path("level").asInt(0);
                boolean isAgent = aq.path("isAgent").asBoolean(level >= 0);
                String aqLabel = aq.path("label").asText("");
                int index = aq.path("index").asInt(0);
                double needle = aq.path("needle").asDouble(level);
                html.append("<section class='card-section'>");
                html.append("<h2>Agency Qualification <span class='dim-position'>Position 12 · Derived</span></h2>");
                html.append("<p class='section-subtitle'>A derived barometer over the 12 core dimensions: the colored band is the strict agency level; the needle is the weighted Agency Index, showing momentum toward the next level.</p>");
                html.append("<div class='agency-strip-wrap agency-").append(isAgent ? "agent" : "nonagent").append("'>");
                html.append(renderAgencyStrip(level, needle, index, isAgent, aqLabel));
                html.append("<div class='agency-strip-meta'>");
                html.append("<span class='agency-flag'>").append(isAgent ? "AGENT" : "NON-AGENT").append("</span>");
                html.append("<span class='agency-index'>Agency Index <strong>").append(index).append("</strong>/100</span>");
                html.append("</div></div>");
                if (aq.has("rationale")) {
                    html.append("<p class='agency-rationale'>").append(escapeHtml(aq.path("rationale").asText(""))).append("</p>");
                }
                html.append("</section>");
            }

            // Tools, Skills, Plugins & MCPs
            renderCapabilitiesSection(html, node);

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
                        html.append("<p><strong>How to embed:</strong> Place AiCMM profile at <code>")
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

    private void renderCapabilitiesSection(StringBuilder html, JsonNode node) {
        // Gather from agent object or root level
        JsonNode agent = node.has("agent") ? node.get("agent") : node;
        JsonNode tools = agent.has("tools") ? agent.get("tools") : node.path("tools");
        JsonNode skills = agent.has("skills") ? agent.get("skills") : node.path("skills");
        JsonNode plugins = agent.has("plugins") ? agent.get("plugins") : node.path("plugins");
        JsonNode mcps = agent.has("mcps") ? agent.get("mcps") : node.path("mcps");
        JsonNode delegatesTo = agent.has("delegatesTo") ? agent.get("delegatesTo") : node.path("agentRelationships").path("delegatesTo");
        JsonNode usedBy = agent.has("usedBy") ? agent.get("usedBy") : node.path("agentRelationships").path("usedBy");

        boolean hasAny = (tools.isArray() && tools.size() > 0)
                || (skills.isArray() && skills.size() > 0)
                || (plugins.isArray() && plugins.size() > 0)
                || (mcps.isArray() && mcps.size() > 0)
                || (delegatesTo.isArray() && delegatesTo.size() > 0)
                || (usedBy.isArray() && usedBy.size() > 0);

        if (!hasAny) return;

        html.append("<section class='card-section capabilities-section'>");
        html.append("<h2>Capabilities & Integrations</h2>");
        html.append("<div class='capabilities-grid'>");

        // Tools
        if (tools.isArray() && tools.size() > 0) {
            html.append("<div class='capability-card'>");
            html.append("<div class='capability-header'><span class='capability-icon'>🔧</span><h3>Tools</h3><span class='capability-count'>").append(tools.size()).append("</span></div>");
            html.append("<div class='capability-tags'>");
            tools.forEach(t -> html.append("<span class='cap-tag tool-tag'>").append(escapeHtml(t.asText())).append("</span>"));
            html.append("</div></div>");
        }

        // Skills
        if (skills.isArray() && skills.size() > 0) {
            html.append("<div class='capability-card'>");
            html.append("<div class='capability-header'><span class='capability-icon'>⚡</span><h3>Skills</h3><span class='capability-count'>").append(skills.size()).append("</span></div>");
            html.append("<div class='capability-tags'>");
            skills.forEach(s -> html.append("<span class='cap-tag skill-tag'>").append(escapeHtml(s.asText())).append("</span>"));
            html.append("</div></div>");
        }

        // Plugins
        if (plugins.isArray() && plugins.size() > 0) {
            html.append("<div class='capability-card'>");
            html.append("<div class='capability-header'><span class='capability-icon'>🧩</span><h3>Plugins</h3><span class='capability-count'>").append(plugins.size()).append("</span></div>");
            html.append("<div class='capability-tags'>");
            plugins.forEach(p -> html.append("<span class='cap-tag plugin-tag'>").append(escapeHtml(p.asText())).append("</span>"));
            html.append("</div></div>");
        }

        // MCPs
        if (mcps.isArray() && mcps.size() > 0) {
            html.append("<div class='capability-card'>");
            html.append("<div class='capability-header'><span class='capability-icon'>🔌</span><h3>MCP Connections</h3><span class='capability-count'>").append(mcps.size()).append("</span></div>");
            html.append("<div class='capability-tags'>");
            mcps.forEach(m -> html.append("<span class='cap-tag mcp-tag'>").append(escapeHtml(m.asText())).append("</span>"));
            html.append("</div></div>");
        }

        // Delegates To
        if (delegatesTo.isArray() && delegatesTo.size() > 0) {
            html.append("<div class='capability-card'>");
            html.append("<div class='capability-header'><span class='capability-icon'>➡️</span><h3>Delegates To</h3><span class='capability-count'>").append(delegatesTo.size()).append("</span></div>");
            html.append("<div class='capability-tags'>");
            delegatesTo.forEach(d -> html.append("<span class='cap-tag delegate-tag'>").append(escapeHtml(d.asText())).append("</span>"));
            html.append("</div></div>");
        }

        // Used By
        if (usedBy.isArray() && usedBy.size() > 0) {
            html.append("<div class='capability-card'>");
            html.append("<div class='capability-header'><span class='capability-icon'>⬅️</span><h3>Used By</h3><span class='capability-count'>").append(usedBy.size()).append("</span></div>");
            html.append("<div class='capability-tags'>");
            usedBy.forEach(u -> html.append("<span class='cap-tag usedby-tag'>").append(escapeHtml(u.asText())).append("</span>"));
            html.append("</div></div>");
        }

        html.append("</div>"); // capabilities-grid
        html.append("</section>");
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
                    "<p>Version: 0.2.0 | Format: JSON Schema Draft 2020-12</p>" +
                    "<pre class='json-view'><code>" + escapeHtml(prettyJson) + "</code></pre>";
            ctx.html(wrapInLayout("Agent Card Schema", html, "schema"));
        } catch (IOException e) {
            ctx.status(500).result("Error: " + e.getMessage());
        }
    }

    public void listCardsJson(Context ctx) {
        ctx.contentType("application/json");
        try {
            String category = ctx.queryParam("category");
            String minScoreParam = ctx.queryParam("minScore");
            double minScore = minScoreParam != null ? Double.parseDouble(minScoreParam) : Double.NEGATIVE_INFINITY;

            List<Map<String, Object>> cards = loadAllCards().stream()
                    .filter(card -> category == null || category.isBlank() || "all".equalsIgnoreCase(category)
                            || category.equalsIgnoreCase(String.valueOf(card.getOrDefault("category", ""))))
                    .filter(card -> Double.isInfinite(minScore) || averageCardScore(card) >= minScore)
                    .toList();
            ctx.json(cards);
        } catch (IOException e) {
            ctx.status(500).json(Map.of("error", e.getMessage()));
        } catch (NumberFormatException e) {
            ctx.status(400).json(Map.of("error", "minScore must be numeric"));
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

    private List<Map<String, Object>> buildLevel0Dimensions() {
        return List.of(
                dimensionDefinition(0, "autonomy", "Autonomy", "Cognitive Core", "How self-directed is it?"),
                dimensionDefinition(1, "reasoning", "Reasoning", "Cognitive Core", "Can it solve problems under uncertainty?"),
                dimensionDefinition(2, "memory", "Memory", "Cognitive Core", "Does it retain and use information over time?"),
                dimensionDefinition(3, "learning", "Learning", "Cognitive Core", "Does it improve from experience safely?"),
                dimensionDefinition(4, "toolUse", "Tool Use", "Action & Integration", "Can it orchestrate tools and APIs?"),
                dimensionDefinition(5, "collaboration", "Collaboration & Social Intelligence", "Action & Integration", "Can it coordinate well with people and agents?"),
                dimensionDefinition(6, "embodiment", "Embodiment", "Action & Integration", "Does it operate in physical or virtual environments?"),
                dimensionDefinition(7, "explainability", "Explainability", "Trust & Deployment", "Can it justify actions and support review?"),
                dimensionDefinition(8, "safety", "Safety", "Trust & Deployment", "Can it operate within safe bounded controls?"),
                dimensionDefinition(9, "interoperability", "Interoperability", "Trust & Deployment", "Can it work across protocols and ecosystems?"),
                dimensionDefinition(10, "costEfficiency", "Cost Efficiency", "Trust & Deployment", "Can it stay resource-aware and economical?"),
                dimensionDefinition(11, "domainAlignment", "Domain Alignment", "Trust & Deployment", "Is it compliant, safe, auditable, and deployable?"));
    }

    private Map<String, Object> dimensionDefinition(int position, String key, String label, String group, String question) {
        Map<String, Object> definition = new LinkedHashMap<>();
        definition.put("position", position);
        definition.put("key", key);
        definition.put("label", label);
        definition.put("group", group);
        definition.put("question", question);
        definition.put("maxScore", 5);
        definition.put("rubric", Map.of(
                "0", "Absent",
                "1", "Basic / hardcoded",
                "2", "Intermediate / structured",
                "3", "Advanced with guardrails",
                "4", "Expert within boundaries",
                "5", "Mastery with self-governance"
        ));
        return definition;
    }

    private Map<String, List<Map<String, Object>>> buildLevel1Dimensions() {
        Map<String, List<Map<String, Object>>> domains = new LinkedHashMap<>();
        domains.put("healthcare", List.of(
                level1Dimension(0, "clinicalSafety", "Clinical Safety", "Patient safety controls and escalation readiness"),
                level1Dimension(1, "evidenceTraceability", "Evidence Traceability", "Citations, audit trails, and provenance"),
                level1Dimension(2, "privacyCompliance", "Privacy Compliance", "HIPAA and patient-data handling"),
                level1Dimension(3, "workflowIntegration", "Workflow Integration", "EHR, triage, and care team integration")));
        domains.put("transportation", List.of(
                level1Dimension(0, "routeOptimization", "Route Optimization", "Planning efficiency under dynamic conditions"),
                level1Dimension(1, "operationalSafety", "Operational Safety", "Vehicle, fleet, and incident safety controls"),
                level1Dimension(2, "trafficInterop", "Traffic Interoperability", "Signals, telematics, and fleet-system integration"),
                level1Dimension(3, "regulatoryReadiness", "Regulatory Readiness", "Compliance with transport standards and reporting")));
        domains.put("finance", List.of(
                level1Dimension(0, "riskControls", "Risk Controls", "Exposure limits, approvals, and kill-switches"),
                level1Dimension(1, "decisionAttribution", "Decision Attribution", "Transparent factor attribution for decisions"),
                level1Dimension(2, "complianceCoverage", "Compliance Coverage", "Support for audit and financial regulations"),
                level1Dimension(3, "marketIntegration", "Market Integration", "Brokerage, FIX, and portfolio workflow interoperability")));
        domains.put("manufacturing", List.of(
                level1Dimension(0, "productionCoordination", "Production Coordination", "Scheduling and line orchestration"),
                level1Dimension(1, "predictiveMaintenance", "Predictive Maintenance", "Failure prediction and maintenance planning"),
                level1Dimension(2, "humanRobotSafety", "Human-Robot Safety", "Safe operation around people and equipment"),
                level1Dimension(3, "industrialInterop", "Industrial Interoperability", "OPC-UA, MES, PLC, and SCADA integration")));
        domains.put("education", List.of(
                level1Dimension(0, "pedagogicalAlignment", "Pedagogical Alignment", "Instruction quality and curriculum fit"),
                level1Dimension(1, "learnerAdaptation", "Learner Adaptation", "Personalization for learner context"),
                level1Dimension(2, "assessmentIntegrity", "Assessment Integrity", "Safe, fair, and explainable assessment support"),
                level1Dimension(3, "accessibility", "Accessibility", "Inclusive and accessible learning delivery")));
        domains.put("customer-service", List.of(
                level1Dimension(0, "resolutionQuality", "Resolution Quality", "Ability to resolve issues accurately"),
                level1Dimension(1, "handoffReadiness", "Handoff Readiness", "Escalation and human handoff quality"),
                level1Dimension(2, "channelConsistency", "Channel Consistency", "Consistent support across channels"),
                level1Dimension(3, "policyCompliance", "Policy Compliance", "Adherence to brand, privacy, and support policy")));
        return domains;
    }

    private Map<String, Object> level1Dimension(int position, String key, String label, String description) {
        Map<String, Object> dimension = new LinkedHashMap<>();
        dimension.put("position", position);
        dimension.put("key", key);
        dimension.put("label", label);
        dimension.put("description", description);
        dimension.put("maxScore", 5);
        dimension.put("rubric", Map.of(
                "0", "Absent",
                "1", "Ad hoc",
                "2", "Repeatable",
                "3", "Operationalized",
                "4", "Robust",
                "5", "Best-in-class"
        ));
        return dimension;
    }

    private List<Map<String, Object>> evaluateSchemaChecks(JsonNode card) {
        List<Map<String, Object>> checks = new ArrayList<>();
        checks.add(validationCheck("schemaVersion", "Schema version must be 0.2.0", "0.2.0".equals(card.path("schemaVersion").asText())));

        JsonNode agent = card.path("agent");
        checks.add(validationCheck("agent", "Agent object is required", agent.isObject()));
        checks.add(validationCheck("agent.name", "agent.name is required", !agent.path("name").asText("").isBlank()));
        checks.add(validationCheck("agent.category", "agent.category must be digital, embodied, or hybrid",
                Set.of("digital", "embodied", "hybrid").contains(agent.path("category").asText(""))));
        checks.add(validationCheck("agent.description", "agent.description is required", !agent.path("description").asText("").isBlank()));

        JsonNode profile = card.path("capabilityProfile");
        checks.add(validationCheck("capabilityProfile", "capabilityProfile object is required", profile.isObject()));
        if (profile.isObject()) {
            for (Map<String, Object> dimension : buildLevel0Dimensions()) {
                String key = (String) dimension.get("key");
                JsonNode scoreNode = profile.path(key);
                boolean present = !scoreNode.isMissingNode() && !scoreNode.isNull();
                checks.add(validationCheck("capabilityProfile." + key, key + " score is required", present));
                if (present) {
                    int score = getScore(profile, key);
                    checks.add(validationCheck("capabilityProfile." + key + ".score", key + " score must be between 0 and 5", score >= 0 && score <= 5));
                }
            }
        }
        return checks;
    }

    private Map<String, Object> validationCheck(String field, String detail, boolean passed) {
        Map<String, Object> check = new LinkedHashMap<>();
        check.put("field", field);
        check.put("detail", detail);
        check.put("passed", passed);
        return check;
    }

    private Map<String, Object> buildGovernanceValidation(JsonNode card) {
        JsonNode profile = card.path("capabilityProfile");
        int autonomy = getScore(profile, "autonomy");
        int reasoning = getScore(profile, "reasoning");
        int safety = getScore(profile, "safety");
        int explainability = getScore(profile, "explainability");
        int domainAlignment = getScore(profile, "domainAlignment");
        int toolUse = getScore(profile, "toolUse");
        int embodiment = getScore(profile, "embodiment");
        int collaboration = getScore(profile, "collaboration");
        int interoperability = getScore(profile, "interoperability");
        int costEfficiency = getScore(profile, "costEfficiency");

        List<Map<String, Object>> rules = new ArrayList<>();
        rules.add(governanceRule("Autonomy-Reasoning Foundation", autonomy <= reasoning + 1,
                "Autonomy(" + autonomy + ") <= Reasoning(" + reasoning + ") + 1"));
        rules.add(governanceRule("Explainability Gate", autonomy < 4 || explainability >= 3,
                "Autonomy >= 4 requires Explainability >= 3"));
        rules.add(governanceRule("Safety Gate", autonomy < 4 || safety >= 3,
                "Autonomy >= 4 requires Safety >= 3"));
        rules.add(governanceRule("Collaboration-Interop Link", collaboration < 4 || interoperability >= 3,
                "Collaboration >= 4 requires Interoperability >= 3"));
        rules.add(governanceRule("Cost Awareness", toolUse < 4 || costEfficiency >= 2,
                "Tool Use >= 4 requires Cost Efficiency >= 2"));
        rules.add(governanceRule("Domain Alignment", embodiment < 3 || domainAlignment >= 3,
                "Embodiment >= 3 requires Domain Alignment >= 3"));
        rules.add(governanceRule("Reasoning Foundation", toolUse < 4 || reasoning >= 3,
                "Tool Use >= 4 requires Reasoning >= 3"));

        boolean allPassed = rules.stream().allMatch(rule -> Boolean.TRUE.equals(rule.get("passed")));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("valid", allPassed);
        result.put("overallCompliant", allPassed);
        result.put("rules", rules);
        result.put("rulesChecked", rules.size());
        return result;
    }

    private Map<String, Object> governanceRule(String rule, boolean passed, String detail) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("rule", rule);
        result.put("passed", passed);
        result.put("detail", detail);
        result.put("constraint", detail);
        result.put("result", passed ? "PASS" : "FAIL");
        return result;
    }

    /**
     * Agency Qualification Layer — the derived 13th dimension (position 12).
     * Interprets the twelve 0-5 scores to classify how agentic a system is, on a
     * signed ladder from -2 (scripted non-agent) to +5 (humanoid agent). The twelve
     * core dimensions are never modified; this is a post-evaluation classification.
     */
    private Map<String, Object> buildAgencyQualification(JsonNode card) {
        JsonNode profile = card.path("capabilityProfile");
        int a = getScore(profile, "autonomy");
        int r = getScore(profile, "reasoning");
        int m = getScore(profile, "memory");
        int l = getScore(profile, "learning");
        int em = getScore(profile, "embodiment");
        int x = getScore(profile, "explainability");
        int s = getScore(profile, "safety");

        boolean govPass = Boolean.TRUE.equals(buildGovernanceValidation(card).get("valid"));
        int minCore = Math.min(Math.min(a, r), Math.min(m, l));
        boolean trustOk = govPass && s >= 3 && x >= 3;

        double avg12 = (a + r + m + l
                + getScore(profile, "toolUse") + getScore(profile, "collaboration") + em
                + x + s + getScore(profile, "interoperability")
                + getScore(profile, "costEfficiency") + getScore(profile, "domainAlignment")) / 12.0;
        double avgExEm = (avg12 * 12.0 - em) / 11.0;

        int level;
        String rationale;
        if (a < 2 || r < 2) {
            if (r >= 1) {
                level = -1;
                rationale = "Fails the agent threshold (Autonomy >= 2 and Reasoning >= 2): reactive, "
                        + "AI-driven responses with no self-directed goals.";
            } else {
                level = -2;
                rationale = "Fails the agent threshold with no reasoning (Reasoning = 0): a deterministic, "
                        + "pre-scripted automation.";
            }
        } else if (!trustOk) {
            level = 0;
            rationale = "Clears the cognitive thresholds but is not yet trustworthy (governance "
                    + (govPass ? "passes" : "fails") + ", Safety=" + s + ", Explainability=" + x + ").";
        } else if (em >= 5 && minCore >= 5 && avg12 >= 4.8) {
            level = 5;
            rationale = "Full embodiment mastery atop human-level cognition — indistinguishable from a human.";
        } else if (minCore >= 5 && avgExEm >= 4.5) {
            level = 4;
            rationale = "Masters the full cognitive core (>=5) with broad, robust, governed capability.";
        } else if (avg12 >= 4.0 && r >= 4 && a >= 4 && m >= 4) {
            level = 3;
            rationale = "State-of-the-art across cognitive core and trust dimensions (avg "
                    + String.format("%.1f", avg12) + ").";
        } else if (r >= 4 && s >= 3 && x >= 3 && avg12 >= 3.0) {
            level = 2;
            rationale = "Expert reasoning with strong trust controls and governance compliance.";
        } else {
            level = 1;
            rationale = "Balanced, governed agent that clears the cognitive thresholds within a bounded domain.";
        }

        Map<String, Object> ladderEntry = agencyLadderEntry(level);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("position", 12);
        result.put("dimension", "Agency Qualification");
        result.put("derived", true);
        result.put("level", level);
        result.put("code", ladderEntry.get("code"));
        result.put("label", ladderEntry.get("label"));
        result.put("isAgent", level >= 0);
        result.put("governancePass", govPass);
        result.put("index", agencyIndexFor(profile));
        result.put("needle", agencyNeedle(level, avg12));
        result.put("rationale", rationale);
        return result;
    }

    /** Per-dimension weights for the continuous Agency Index barometer (positions 0-11); sums to 1.0. */
    private static final double[] AGENCY_INDEX_WEIGHTS = {
            0.20, 0.18, 0.10, 0.08, 0.12, 0.07, 0.02, 0.07, 0.07, 0.03, 0.02, 0.04
    };
    private static final String[] AGENCY_DIM_ORDER = {
            "autonomy", "reasoning", "memory", "learning", "toolUse", "collaboration",
            "embodiment", "explainability", "safety", "interoperability", "costEfficiency", "domainAlignment"
    };
    private static final double[][] AGENCY_NEEDLE_BANDS = {
            {1.2, 2.5}, {2.0, 3.0}, {3.0, 4.0}, {4.0, 4.5}, {4.5, 4.9}, {4.9, 5.01}
    };

    /** Weighted 0-100 Agency Index barometer reading derived from the twelve scores. */
    private int agencyIndexFor(JsonNode profile) {
        double acc = 0;
        for (int i = 0; i < AGENCY_DIM_ORDER.length; i++) {
            acc += AGENCY_INDEX_WEIGHTS[i] * getScore(profile, AGENCY_DIM_ORDER[i]);
        }
        return (int) Math.round(100.0 * acc / 5.0);
    }

    /** Continuous needle position on the signed -2..+5 ladder for the given level and average. */
    private double agencyNeedle(int level, double avg12) {
        if (level < 0) {
            return level + 0.5;
        }
        double[] band = AGENCY_NEEDLE_BANDS[level];
        double f = (avg12 - band[0]) / (band[1] - band[0]);
        f = Math.max(0.0, Math.min(0.96, f));
        return Math.round((level + f) * 100.0) / 100.0;
    }

    /** Hex colors for ladder levels -2..+5, used by the barometer strip. */
    private static final String[] AGENCY_ZONE_COLORS = {
            "#b91c1c", "#ea580c", "#d97706", "#65a30d", "#059669", "#0891b2", "#6d28d9", "#be185d"
    };
    private static final String[] AGENCY_ZONE_LABELS = {
            "Scripted", "Reactive", "Proto", "Basic", "Advanced", "Generalized", "Human-Level", "Humanoid"
    };

    /** Renders the Agency Barometer as a horizontal ladder strip (static SVG, signed -2..+5). */
    private String renderAgencyStrip(int level, double needle, int index, boolean isAgent, String label) {
        int w = 520, h = 78, pad = 16, top = 30, bh = 20;
        double lo = -2, hi = 5;
        java.util.function.DoubleUnaryOperator x =
                v -> pad + ((v - lo) / (hi - lo)) * (w - 2 * pad);
        StringBuilder s = new StringBuilder();
        s.append("<svg class='agency-strip' width='").append(w).append("' height='").append(h)
                .append("' viewBox='0 0 ").append(w).append(" ").append(h).append("' xmlns='http://www.w3.org/2000/svg'>");
        for (int v = -2; v <= 5; v++) {
            double x0 = x.applyAsDouble(v), x1 = x.applyAsDouble(v + 1);
            String color = AGENCY_ZONE_COLORS[v + 2];
            double op = (v == level) ? 1.0 : 0.45;
            s.append("<rect x='").append(fmt(x0)).append("' y='").append(top).append("' width='")
                    .append(fmt(x1 - x0)).append("' height='").append(bh).append("' fill='").append(color)
                    .append("' opacity='").append(op).append("'/>");
            s.append("<text x='").append(fmt((x0 + x1) / 2)).append("' y='").append(top + bh + 13)
                    .append("' font-size='8' fill='#475569' text-anchor='middle'>")
                    .append(v >= 0 ? "+" + v : v).append("</text>");
        }
        // agent threshold marker at 0
        double xz = x.applyAsDouble(0);
        s.append("<line x1='").append(fmt(xz)).append("' y1='").append(top - 4).append("' x2='").append(fmt(xz))
                .append("' y2='").append(top + bh + 4).append("' stroke='#0f172a' stroke-width='1.5' stroke-dasharray='3 2'/>");
        // needle
        double nx = x.applyAsDouble(needle);
        s.append("<polygon points='").append(fmt(nx)).append(",").append(top - 1).append(" ")
                .append(fmt(nx - 6)).append(",").append(top - 12).append(" ")
                .append(fmt(nx + 6)).append(",").append(top - 12).append("' fill='#0f172a'/>");
        // caption
        String zoneLabel = (label != null && !label.isEmpty()) ? label : AGENCY_ZONE_LABELS[level + 2];
        s.append("<text x='").append(pad).append("' y='16' font-size='12' font-weight='700' fill='")
                .append(AGENCY_ZONE_COLORS[level + 2]).append("'>")
                .append(level >= 0 ? "+" + level : level).append(" · ").append(escapeHtml(zoneLabel)).append("</text>");
        s.append("</svg>");
        return s.toString();
    }

    private static String fmt(double v) {
        return String.format(java.util.Locale.US, "%.1f", v);
    }

    /** Compact barometer strip for the catalog table cell (no labels, just zones + needle). */
    private String renderAgencyStripMini(int level, double needle) {
        int w = 120, h = 16;
        double lo = -2, hi = 5;
        java.util.function.DoubleUnaryOperator x = v -> ((v - lo) / (hi - lo)) * w;
        StringBuilder s = new StringBuilder();
        s.append("<svg class='agency-strip-mini' width='").append(w).append("' height='").append(h)
                .append("' viewBox='0 0 ").append(w).append(" ").append(h).append("' xmlns='http://www.w3.org/2000/svg'>");
        for (int v = -2; v <= 5; v++) {
            double x0 = x.applyAsDouble(v), x1 = x.applyAsDouble(v + 1);
            double op = (v == level) ? 1.0 : 0.4;
            s.append("<rect x='").append(fmt(x0)).append("' y='4' width='").append(fmt(x1 - x0))
                    .append("' height='8' fill='").append(AGENCY_ZONE_COLORS[v + 2]).append("' opacity='").append(op).append("'/>");
        }
        double nx = x.applyAsDouble(needle);
        s.append("<polygon points='").append(fmt(nx)).append(",3 ").append(fmt(nx - 4)).append(",0 ")
                .append(fmt(nx + 4)).append(",0' fill='#0f172a'/>");
        s.append("</svg>");
        return s.toString();
    }

    /** The full Agency Qualification ladder, from -2 (non-agent) to +5 (humanoid agent). */
    private List<Map<String, Object>> buildAgencyLadder() {
        return List.of(
                agencyLevel(-2, "SCRIPTED_AUTOMATION", "Non-Agent — Scripted Automation", false,
                        "Deterministic, pre-scripted tool with no autonomy and no reasoning (e.g. RPA macro, ETL pipeline)."),
                agencyLevel(-1, "REACTIVE_ASSISTANT", "Non-Agent — Reactive Assistant", false,
                        "AI-driven but purely reactive: answers only on direct input (e.g. FAQ bot, early Siri/Alexa, basic Q&A LLM)."),
                agencyLevel(0, "PROTO_AGENT", "Proto-Agent — Emerging Agency", true,
                        "Meets minimal autonomy/reasoning thresholds but is brittle and not yet trustworthy (e.g. AutoGPT)."),
                agencyLevel(1, "BASIC_AGENT", "Basic Agent — Qualified", true,
                        "A fully qualified agent with a balanced, moderate capability profile that passes governance."),
                agencyLevel(2, "ADVANCED_AGENT", "Advanced Agent — Autonomous & Trust-Aligned", true,
                        "Expert reasoning with strong trust controls (e.g. Copilot CLI, MedAssist Pro, Tesla FSD)."),
                agencyLevel(3, "GENERALIZED_AGENT", "Generalized Agent — Cutting-Edge", true,
                        "State-of-the-art autonomous agent with world models, long-term memory, and multi-agent coordination."),
                agencyLevel(4, "HUMAN_LEVEL_AGENT", "Human-Level Agent", true,
                        "Human-level general intelligence: masters the full cognitive core with broad, governed capability."),
                agencyLevel(5, "HUMANOID_AGENT", "Humanoid Agent — Indistinguishable from Human", true,
                        "Indistinguishable from a human in look, feel, and appearance — full embodiment with synthetic skin, touch, and taste."));
    }

    private Map<String, Object> agencyLevel(int level, String code, String label, boolean isAgent, String description) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("level", level);
        entry.put("code", code);
        entry.put("label", label);
        entry.put("isAgent", isAgent);
        entry.put("description", description);
        return entry;
    }

    private Map<String, Object> agencyLadderEntry(int level) {
        for (Map<String, Object> entry : buildAgencyLadder()) {
            if (Integer.valueOf(level).equals(entry.get("level"))) {
                return entry;
            }
        }
        return agencyLevel(level, "UNKNOWN", "Unknown", level >= 0, "");
    }

    private Map<String, Object> agencyForProfile(Map<String, Object> profile) {
        ObjectNode card = mapper.createObjectNode();
        card.set("capabilityProfile", mapper.valueToTree(profile == null ? Map.of() : profile));
        return buildAgencyQualification(card);
    }

    private double averageCardScore(Map<String, Object> card) {
        @SuppressWarnings("unchecked")
        Map<String, Object> profile = (Map<String, Object>) card.get("capabilityProfile");
        if (profile == null) {
            return 0;
        }

        int total = 0;
        int count = 0;
        for (Map<String, Object> dimension : buildLevel0Dimensions()) {
            int score = getScoreFromProfile(profile, (String) dimension.get("key"));
            if (score >= 0) {
                total += score;
                count++;
            }
        }
        return count == 0 ? 0 : total / (double) count;
    }

    private int getScore(JsonNode profile, String key) {
        JsonNode dim = profile.path(key);
        if (dim.has("score")) {
            return dim.path("score").asInt(0);
        }
        return dim.asInt(0);
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
                    <link rel="icon" href="/img/favicon.ico" sizes="any">
                    <link rel="icon" type="image/svg+xml" href="/img/aicmm-icon.svg">
                    <script src="https://cdn.jsdelivr.net/npm/mermaid@10/dist/mermaid.min.js"></script>
                    <script>mermaid.initialize({startOnLoad: true, theme: 'neutral'});</script>
                    <link rel="stylesheet" href="/css/faa.css">
                </head>
                <body>
                    <nav class="navbar">
                        <a class="nav-brand" href="/">
                            <img class="brand-logo" src="/img/aicmm-icon.svg" alt="AiCMM logo">
                            <span class="brand-text">
                                <span class="brand-name">AiCMM</span>
                                <span class="nav-subtitle">Agent Capability Maturity Model</span>
                            </span>
                        </a>
                        <div class="nav-links">
                            <a href="/" class="%s">Home</a>
                            <a href="/framework" class="%s">Framework</a>
                            <a href="/architecture" class="%s">Architecture</a>
                            <a href="/catalog" class="%s">Catalog</a>
                            <a href="/create-card" class="%s">Create Card</a>
                            <a href="/user-guide" class="%s">Guide</a>
                            <a href="/classify-by-prompting" class="%s">Prompt Guide</a>
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
                           <a href="https://github.com/snchande/Arima-AiCMM">GitHub</a> |
                           <a href="https://www.linkedin.com/in/sureshchande">LinkedIn</a> |
                           <a href="https://medium.com/@sureshchande">Medium</a>
                        </p>
                    </footer>
                    <script src="/js/app.js"></script>
                    <script src="/js/faa.js"></script>
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
                "classify-by-prompting".equals(activePage) ? "active" : "",
                "release-notes".equals(activePage) ? "active" : "",
                "schema".equals(activePage) ? "active" : "",
                content
        );
    }
}
