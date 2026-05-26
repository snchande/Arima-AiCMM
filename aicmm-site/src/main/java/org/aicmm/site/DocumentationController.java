package org.aicmm.site;

import io.javalin.http.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Handles documentation routes — renders Markdown files and provides navigation.
 */
public class DocumentationController {

    private static final Logger log = LoggerFactory.getLogger(DocumentationController.class);

    private final Path docsPath;
    private final Path projectRoot;
    private final MarkdownRenderer renderer;

    public DocumentationController(Path docsPath, Path projectRoot, MarkdownRenderer renderer) {
        this.docsPath = docsPath;
        this.projectRoot = projectRoot;
        this.renderer = renderer;
    }

    public void home(Context ctx) {
        // Render README.md as home page
        Path readme = projectRoot.resolve("README.md");
        if (readme.toFile().exists()) {
            try {
                String content = Files.readString(readme);
                String html = renderer.render(content);
                ctx.html(wrapInLayout("AiCMM — Agent Capability Maturity Model", html, "home"));
            } catch (IOException e) {
                ctx.status(500).result("Error reading README: " + e.getMessage());
            }
        } else {
            ctx.html(wrapInLayout("AiCMM", "<h1>Welcome to AiCMM</h1>", "home"));
        }
    }

    public void framework(Context ctx) {
        // Render the Medium article as the framework page
        Path frameworkDoc = docsPath.resolve("articles").resolve("overview-medium.md");
        if (frameworkDoc.toFile().exists()) {
            try {
                String content = Files.readString(frameworkDoc);
                String html = renderer.render(content);
                ctx.html(wrapInLayout("AiCMM Framework", html, "framework"));
            } catch (IOException e) {
                ctx.status(500).result("Error: " + e.getMessage());
            }
        } else {
            ctx.status(404).result("Framework documentation not found");
        }
    }

    public void architecture(Context ctx) {
        Path archDoc = docsPath.resolve("architecture").resolve("platform-architecture.md");
        if (archDoc.toFile().exists()) {
            try {
                String content = Files.readString(archDoc);
                String html = renderer.render(content);
                ctx.html(wrapInLayout("Platform Architecture", html, "architecture"));
            } catch (IOException e) {
                ctx.status(500).result("Error: " + e.getMessage());
            }
        } else {
            ctx.status(404).result("Architecture documentation not found");
        }
    }

    public void releaseNotes(Context ctx) {
        Path releaseDoc = docsPath.resolve("RELEASE-NOTES.md");
        if (releaseDoc.toFile().exists()) {
            try {
                String content = Files.readString(releaseDoc);
                String html = renderer.render(content);
                ctx.html(wrapInLayout("Release Notes", html, "release-notes"));
            } catch (IOException e) {
                ctx.status(500).result("Error: " + e.getMessage());
            }
        } else {
            ctx.status(404).result("Release notes not found");
        }
    }

    public void userGuide(Context ctx) {
        Path guideDoc = docsPath.resolve("guides").resolve("user-guide.md");
        if (guideDoc.toFile().exists()) {
            try {
                String content = Files.readString(guideDoc);
                String html = renderer.render(content);
                ctx.html(wrapInLayout("User Guide", html, "user-guide"));
            } catch (IOException e) {
                ctx.status(500).result("Error: " + e.getMessage());
            }
        } else {
            ctx.status(404).result("User guide not found");
        }
    }

    public void brochure(Context ctx) {
        Path brochureDoc = docsPath.resolve("PRODUCT-BROCHURE.md");
        if (brochureDoc.toFile().exists()) {
            try {
                String content = Files.readString(brochureDoc);
                String html = renderer.render(content);
                ctx.html(wrapInLayout("Product Brochure", html, "brochure"));
            } catch (IOException e) {
                ctx.status(500).result("Error: " + e.getMessage());
            }
        } else {
            ctx.status(404).result("Brochure not found");
        }
    }

    public void renderDoc(Context ctx) {
        String path = ctx.pathParam("path");
        Path docFile = docsPath.resolve(path);

        // Security: prevent path traversal
        if (!docFile.normalize().startsWith(docsPath.normalize())) {
            ctx.status(403).result("Access denied");
            return;
        }

        if (!docFile.toFile().exists()) {
            // Try with .md extension
            docFile = docsPath.resolve(path + ".md");
        }

        if (docFile.toFile().exists() && docFile.toString().endsWith(".md")) {
            try {
                String content = Files.readString(docFile);
                String html = renderer.render(content);
                String title = extractTitle(content);
                ctx.html(wrapInLayout(title, html, "docs"));
            } catch (IOException e) {
                ctx.status(500).result("Error: " + e.getMessage());
            }
        } else if (docFile.toFile().exists()) {
            // Serve non-markdown files directly
            try {
                ctx.result(Files.newInputStream(docFile));
            } catch (IOException e) {
                ctx.status(500).result("Error: " + e.getMessage());
            }
        } else {
            ctx.status(404).result("Document not found: " + path);
        }
    }

    public void listDocs(Context ctx) {
        try {
            List<Map<String, String>> docs = new ArrayList<>();
            Files.walkFileTree(docsPath, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (file.toString().endsWith(".md")) {
                        String relative = docsPath.relativize(file).toString().replace("\\", "/");
                        docs.add(Map.of(
                                "path", relative,
                                "url", "/docs/" + relative,
                                "name", file.getFileName().toString().replace(".md", "")
                        ));
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
            ctx.json(docs);
        } catch (IOException e) {
            ctx.status(500).json(Map.of("error", e.getMessage()));
        }
    }

    private String extractTitle(String markdown) {
        for (String line : markdown.split("\n")) {
            if (line.startsWith("# ")) {
                return line.substring(2).trim();
            }
        }
        return "AiCMM Documentation";
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
                            <a href="/brochure" class="%s">Brochure</a>
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
                           <a href="https://github.com/snchande/Arima-AiCMM">GitHub</a> |
                           <a href="https://www.linkedin.com/in/sureshchande">LinkedIn</a> |
                           <a href="https://medium.com/@sureshchande">Medium</a>
                        </p>
                    </footer>
                    <script src="/js/app.js"></script>
                    <script>
                        mermaid.initialize({startOnLoad: false, theme: 'neutral', securityLevel: 'loose'});
                        document.addEventListener('DOMContentLoaded', function() {
                            var elements = document.querySelectorAll('.mermaid');
                            if (elements.length > 0) {
                                mermaid.run({nodes: elements});
                            }
                        });
                    </script>
                </body>
                </html>
                """.formatted(
                title,
                "home".equals(activePage) ? "active" : "",
                "framework".equals(activePage) ? "active" : "",
                "architecture".equals(activePage) ? "active" : "",
                "catalog".equals(activePage) || "cards".equals(activePage) ? "active" : "",
                "create".equals(activePage) ? "active" : "",
                "brochure".equals(activePage) ? "active" : "",
                "user-guide".equals(activePage) ? "active" : "",
                "release-notes".equals(activePage) ? "active" : "",
                "schema".equals(activePage) ? "active" : "",
                content
        );
    }
}
