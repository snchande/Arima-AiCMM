package org.aicmm.site;

import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;
import org.aicmm.site.faa.AssistController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Map;

/**
 * AiCMM Documentation Site - local web server that renders all framework
 * documentation, Markdown files with Mermaid diagram support, and Agent Cards.
 *
 * Run: java -jar aicmm-site.jar [--port 8080] [--docs-root ../docs]
 */
public class AicmmSite {

    private static final Logger log = LoggerFactory.getLogger(AicmmSite.class);
    private static final int DEFAULT_PORT = 8080;
    /** Shared secret used to ask a running instance to shut down so a new one can take over. */
    private static final String ADMIN_TOKEN = envOr("AICMM_ADMIN_TOKEN", "aicmm-secret-restart");

    public static void main(String[] args) {
        int port = DEFAULT_PORT;
        String docsRoot = null;

        for (int i = 0; i < args.length; i++) {
            if ("--port".equals(args[i]) && i + 1 < args.length) {
                port = Integer.parseInt(args[++i]);
            } else if ("--docs-root".equals(args[i]) && i + 1 < args.length) {
                docsRoot = args[++i];
            }
        }

        // Resolve docs root relative to project
        Path projectRoot = resolveProjectRoot();
        Path docsPath = docsRoot != null ? Paths.get(docsRoot) : projectRoot.resolve("docs");
        Path examplesPath = projectRoot.resolve("examples");
        Path schemasPath = projectRoot.resolve("schemas");

        log.info("AiCMM Site starting...");
        log.info("Project root: {}", projectRoot);
        log.info("Docs path: {}", docsPath);

        MarkdownRenderer renderer = new MarkdownRenderer();
        DocumentationController docsController = new DocumentationController(docsPath, projectRoot, renderer);
        AgentCardController cardController = new AgentCardController(examplesPath, schemasPath);
        AssistController assistController = new AssistController(projectRoot);

        Javalin app = Javalin.create(config -> {
            config.staticFiles.add("/static", Location.CLASSPATH);
            Path imagesDir = docsPath.resolve("images");
            if (imagesDir.toFile().isDirectory()) {
                config.staticFiles.add(staticFiles -> {
                    staticFiles.hostedPath = "/images";
                    staticFiles.directory = imagesDir.toString();
                    staticFiles.location = Location.EXTERNAL;
                });
            }
        });

        // Routes
        app.get("/", docsController::home);
        app.get("/docs/<path>", docsController::renderDoc);
        app.get("/framework", docsController::framework);
        app.get("/architecture", docsController::architecture);
        app.get("/catalog", cardController::catalog);
        app.get("/create-card", cardController::createCardForm);
        app.get("/agent-cards", cardController::listCards);
        app.get("/agent-cards/{name}", cardController::viewCard);
        app.get("/schema", cardController::viewSchema);
        app.get("/release-notes", docsController::releaseNotes);
        app.get("/user-guide", docsController::userGuide);
        app.get("/classify-by-prompting", docsController::classifyByPrompting);
        app.get("/classify-by-prompting.pdf", docsController::classifyByPromptingPdf);
        app.get("/brochure", docsController::brochure);
        app.get("/api/docs", docsController::listDocs);
        app.get("/api/agent-cards", cardController::listCardsJson);
        app.post("/api/agent-cards", cardController::createCard);
        app.get("/api/agent-cards/{name}", cardController::getCardJson);
        app.post("/api/agent-cards/{name}/score", cardController::scoreCard);
        app.post("/api/inspect", cardController::inspectAgent);
        app.get("/api/schema", cardController::getSchema);
        app.get("/api/dimensions", cardController::getDimensions);
        app.get("/api/agency-levels", cardController::getAgencyLevels);
        app.post("/api/validate", cardController::validateCard);
        app.post("/api/assist", assistController::assist);
        app.get("/api/assist/providers", assistController::providers);
        app.get("/api/assist/settings", assistController::getSettings);
        app.post("/api/assist/settings", assistController::saveSettings);

        // Secret-protected shutdown so a freshly launched instance can replace this one.
        app.post("/api/admin/shutdown", ctx -> {
            String token = ctx.header("X-AiCMM-Token");
            if (token == null) token = ctx.queryParam("token");
            if (!ADMIN_TOKEN.equals(token)) { ctx.status(403).json(Map.of("error", "forbidden")); return; }
            ctx.json(Map.of("status", "shutting down"));
            log.info("Received authenticated shutdown request — stopping.");
            new Thread(() -> {
                try { Thread.sleep(300); } catch (InterruptedException ignored) {}
                app.stop();
                System.exit(0);
            }, "aicmm-shutdown").start();
        });

        // Easy restart: if something is already on this port, ask it to step aside first.
        takeOverPort(port);

        app.start(port);
        log.info("AiCMM Site running at http://localhost:{}", port);
        log.info("Press Ctrl+C to stop.");
    }

    private static String envOr(String key, String def) {
        String v = System.getenv(key);
        return (v == null || v.isBlank()) ? def : v;
    }

    private static boolean portInUse(int port) {
        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress("127.0.0.1", port), 300);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /** If a server already holds {@code port}, send the secret shutdown code and wait for it to free. */
    private static void takeOverPort(int port) {
        if (!portInUse(port)) return;
        log.info("Port {} is busy — asking the running AiCMM instance to shut down...", port);
        try {
            HttpClient http = HttpClient.newHttpClient();
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + port + "/api/admin/shutdown"))
                    .header("X-AiCMM-Token", ADMIN_TOKEN)
                    .timeout(Duration.ofSeconds(5))
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();
            http.send(req, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            log.warn("Shutdown request failed ({}). A non-AiCMM process may hold the port.", e.getMessage());
        }
        for (int i = 0; i < 40 && portInUse(port); i++) {
            try { Thread.sleep(250); } catch (InterruptedException ignored) {}
        }
        if (portInUse(port)) {
            log.warn("Port {} is still in use; startup may fail to bind.", port);
        } else {
            log.info("Previous instance stopped — taking over port {}.", port);
        }
    }

    private static Path resolveProjectRoot() {
        // Try to find pom.xml walking up from CWD
        Path current = Paths.get(System.getProperty("user.dir"));
        while (current != null) {
            if (current.resolve("pom.xml").toFile().exists()
                    && current.resolve("docs").toFile().exists()) {
                return current;
            }
            current = current.getParent();
        }
        return Paths.get(System.getProperty("user.dir"));
    }
}
