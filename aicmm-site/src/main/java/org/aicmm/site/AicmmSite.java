package org.aicmm.site;

import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * AiCMM Documentation Site - local web server that renders all framework
 * documentation, Markdown files with Mermaid diagram support, and Agent Cards.
 *
 * Run: java -jar aicmm-site.jar [--port 8080] [--docs-root ../docs]
 */
public class AicmmSite {

    private static final Logger log = LoggerFactory.getLogger(AicmmSite.class);
    private static final int DEFAULT_PORT = 8080;

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

        app.start(port);
        log.info("AiCMM Site running at http://localhost:{}", port);
        log.info("Press Ctrl+C to stop.");
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
