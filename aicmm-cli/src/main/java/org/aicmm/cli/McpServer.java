package org.aicmm.cli;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

/**
 * AiCMM MCP Server — Pure Java stdio transport implementation.
 * Exposes AiCMM Agent Card operations as MCP tools.
 * Connects to the AiCMM site API (default: http://localhost:8080/api).
 *
 * Usage: java -cp aicmm-cli.jar org.aicmm.cli.McpServer
 * Environment: AICMM_API_URL (default http://localhost:8080/api)
 */
public class McpServer {

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final String API_BASE = System.getenv().getOrDefault("AICMM_API_URL", "http://localhost:8080/api");

    public static void main(String[] args) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(System.out, StandardCharsets.UTF_8), true);

        String line;
        while ((line = reader.readLine()) != null) {
            if (line.isBlank()) continue;
            try {
                JsonNode msg = mapper.readTree(line);
                JsonNode response = handleMessage(msg);
                if (response != null) {
                    writer.println(mapper.writeValueAsString(response));
                }
            } catch (Exception e) {
                ObjectNode error = mapper.createObjectNode();
                error.put("jsonrpc", "2.0");
                error.putNull("id");
                ObjectNode errObj = error.putObject("error");
                errObj.put("code", -32700);
                errObj.put("message", e.getMessage());
                writer.println(mapper.writeValueAsString(error));
            }
        }
    }

    private static JsonNode handleMessage(JsonNode msg) throws Exception {
        String method = msg.path("method").asText("");
        JsonNode id = msg.get("id");

        switch (method) {
            case "initialize":
                return initializeResponse(id);
            case "notifications/initialized":
                return null; // No response for notifications
            case "tools/list":
                return toolsListResponse(id);
            case "tools/call":
                return toolCallResponse(id, msg.path("params"));
            default:
                return errorResponse(id, -32601, "Method not found: " + method);
        }
    }

    private static JsonNode initializeResponse(JsonNode id) {
        ObjectNode resp = mapper.createObjectNode();
        resp.put("jsonrpc", "2.0");
        resp.set("id", id);
        ObjectNode result = resp.putObject("result");
        result.put("protocolVersion", "2024-11-05");
        result.putObject("capabilities").putObject("tools");
        ObjectNode serverInfo = result.putObject("serverInfo");
        serverInfo.put("name", "aicmm-mcp-server");
        serverInfo.put("version", "0.2.0");
        return resp;
    }

    private static JsonNode toolsListResponse(JsonNode id) throws Exception {
        ObjectNode resp = mapper.createObjectNode();
        resp.put("jsonrpc", "2.0");
        resp.set("id", id);
        ObjectNode result = resp.putObject("result");

        // Load tools from mcp config
        ArrayNode tools = mapper.createArrayNode();
        tools.add(tool("aicmm_create_card",
                "Create a new AiCMM Agent Card with 12-dimension capability profile, avatar, tools, skills, plugins, MCPs",
                "{\"type\":\"object\",\"properties\":{\"agent\":{\"type\":\"object\"},\"capabilityProfile\":{\"type\":\"object\"},\"tools\":{\"type\":\"array\"},\"skills\":{\"type\":\"array\"},\"plugins\":{\"type\":\"array\"},\"mcps\":{\"type\":\"array\"}},\"required\":[\"agent\",\"capabilityProfile\"]}"));
        tools.add(tool("aicmm_inspect_agent",
                "Inspect an AI agent from URL or description, generate suggested AiCMM scores",
                "{\"type\":\"object\",\"properties\":{\"url\":{\"type\":\"string\"},\"description\":{\"type\":\"string\"},\"name\":{\"type\":\"string\"}}}"));
        tools.add(tool("aicmm_validate_card",
                "Validate an Agent Card against schema and 7 governance rules",
                "{\"type\":\"object\",\"properties\":{\"card\":{\"type\":\"object\"}},\"required\":[\"card\"]}"));
        tools.add(tool("aicmm_score_card",
                "Score/re-score an agent card, return governance validation and maturity level",
                "{\"type\":\"object\",\"properties\":{\"card\":{\"type\":\"object\"}},\"required\":[\"card\"]}"));
        tools.add(tool("aicmm_list_cards",
                "List all Agent Cards in the AiCMM catalog",
                "{\"type\":\"object\",\"properties\":{\"category\":{\"type\":\"string\"},\"minScore\":{\"type\":\"number\"}}}"));
        tools.add(tool("aicmm_get_card",
                "Get full details of a specific Agent Card by name",
                "{\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"}},\"required\":[\"name\"]}"));
        tools.add(tool("aicmm_get_dimensions",
                "Get AiCMM dimension definitions with positions, groups, and scoring rubrics",
                "{\"type\":\"object\",\"properties\":{\"level\":{\"type\":\"string\"},\"domain\":{\"type\":\"string\"}}}"));
        tools.add(tool("aicmm_get_schema",
                "Get the AiCMM Agent Card JSON schema (v0.2.0)",
                "{\"type\":\"object\",\"properties\":{}}"));

        result.set("tools", tools);
        return resp;
    }

    private static JsonNode toolCallResponse(JsonNode id, JsonNode params) {
        String toolName = params.path("name").asText("");
        JsonNode arguments = params.path("arguments");

        try {
            String resultText = executeToolCall(toolName, arguments);
            ObjectNode resp = mapper.createObjectNode();
            resp.put("jsonrpc", "2.0");
            resp.set("id", id);
            ObjectNode result = resp.putObject("result");
            ArrayNode content = result.putArray("content");
            ObjectNode textContent = content.addObject();
            textContent.put("type", "text");
            textContent.put("text", resultText);
            return resp;
        } catch (Exception e) {
            return errorResponse(id, -32603, "Tool execution failed: " + e.getMessage());
        }
    }

    private static String executeToolCall(String toolName, JsonNode arguments) throws Exception {
        switch (toolName) {
            case "aicmm_create_card":
                return httpPost("/agent-cards", mapper.writeValueAsString(arguments));
            case "aicmm_inspect_agent":
                return httpPost("/inspect", mapper.writeValueAsString(arguments));
            case "aicmm_validate_card":
                JsonNode card = arguments.has("card") ? arguments.get("card") : arguments;
                return httpPost("/validate", mapper.writeValueAsString(card));
            case "aicmm_score_card":
                JsonNode scoreCard = arguments.has("card") ? arguments.get("card") : arguments;
                return httpPost("/agent-cards/_/score", mapper.writeValueAsString(scoreCard));
            case "aicmm_list_cards":
                String category = arguments.path("category").asText("");
                String minScore = arguments.has("minScore") ? arguments.get("minScore").asText() : "";
                String query = "";
                if (!category.isEmpty()) query += "?category=" + category;
                if (!minScore.isEmpty()) query += (query.isEmpty() ? "?" : "&") + "minScore=" + minScore;
                return httpGet("/agent-cards" + query);
            case "aicmm_get_card":
                String name = arguments.path("name").asText("");
                return httpGet("/agent-cards/" + name);
            case "aicmm_get_dimensions":
                String level = arguments.path("level").asText("");
                String domain = arguments.path("domain").asText("");
                String dQuery = "";
                if (!level.isEmpty()) dQuery += "?level=" + level;
                if (!domain.isEmpty()) dQuery += (dQuery.isEmpty() ? "?" : "&") + "domain=" + domain;
                return httpGet("/dimensions" + dQuery);
            case "aicmm_get_schema":
                return httpGet("/schema");
            default:
                return "{\"error\":\"Unknown tool: " + toolName + "\"}";
        }
    }

    private static String httpGet(String path) throws Exception {
        URL url = new URL(API_BASE + path);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", "application/json");
        return readResponse(conn);
    }

    private static String httpPost(String path, String body) throws Exception {
        URL url = new URL(API_BASE + path);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Accept", "application/json");
        conn.setDoOutput(true);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.getBytes(StandardCharsets.UTF_8));
        }
        return readResponse(conn);
    }

    private static String readResponse(HttpURLConnection conn) throws Exception {
        int code = conn.getResponseCode();
        InputStream is = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();
        if (is == null) return "{\"status\":" + code + "}";
        try (Scanner scanner = new Scanner(is, StandardCharsets.UTF_8.name())) {
            scanner.useDelimiter("\\A");
            return scanner.hasNext() ? scanner.next() : "";
        }
    }

    private static ObjectNode tool(String name, String description, String schemaJson) throws Exception {
        ObjectNode t = mapper.createObjectNode();
        t.put("name", name);
        t.put("description", description);
        t.set("inputSchema", mapper.readTree(schemaJson));
        return t;
    }

    private static JsonNode errorResponse(JsonNode id, int code, String message) {
        ObjectNode resp = mapper.createObjectNode();
        resp.put("jsonrpc", "2.0");
        resp.set("id", id);
        ObjectNode errObj = resp.putObject("error");
        errObj.put("code", code);
        errObj.put("message", message);
        return resp;
    }
}
