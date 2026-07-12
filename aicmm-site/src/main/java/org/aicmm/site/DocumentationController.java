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

    public void classifyByPrompting(Context ctx) {
        Path doc = docsPath.resolve("guides").resolve("classify-by-prompting.md");
        if (doc.toFile().exists()) {
            try {
                String content = Files.readString(doc);
                String html = renderer.render(content);
                ctx.html(wrapInLayout("Classify by Prompting", PROMPT_TOOLBAR + html + PROMPT_COPY_SCRIPT,
                        "classify-by-prompting"));
            } catch (IOException e) {
                ctx.status(500).result("Error: " + e.getMessage());
            }
        } else {
            ctx.status(404).result("Guide not found");
        }
    }

    /** Serves the companion PDF of the prompt-only classification guide as a download. */
    public void classifyByPromptingPdf(Context ctx) {
        Path pdf = docsPath.resolve("guides").resolve("classify-by-prompting.pdf");
        if (pdf.toFile().exists()) {
            try {
                ctx.contentType("application/pdf");
                ctx.header("Content-Disposition",
                        "attachment; filename=\"aicmm-classify-by-prompting.pdf\"");
                ctx.result(Files.readAllBytes(pdf));
            } catch (IOException e) {
                ctx.status(500).result("Error: " + e.getMessage());
            }
        } else {
            ctx.status(404).result("PDF not found");
        }
    }

    /** Action bar rendered above the prompt guide: copy the prompt + download the PDF. */
    private static final String PROMPT_TOOLBAR = """
            <div class="prompt-actions">
                <button id="copy-prompt-btn" type="button" class="prompt-btn prompt-btn-primary">
                    📋 Copy Prompt
                </button>
                <a href="/classify-by-prompting.pdf" class="prompt-btn prompt-btn-secondary"
                   download="aicmm-classify-by-prompting.pdf">⬇ Download PDF</a>
            </div>
            <style>
                .prompt-actions { display:flex; gap:.75rem; flex-wrap:wrap; align-items:center;
                    margin:0 0 1.5rem; padding:.85rem 1rem; border:1px solid #e2e6ef;
                    border-radius:10px; background:#f7f9fc; }
                .prompt-btn { display:inline-flex; align-items:center; gap:.4rem; cursor:pointer;
                    font:600 .95rem/1 system-ui,Segoe UI,Arial,sans-serif; text-decoration:none;
                    padding:.6rem 1.05rem; border-radius:8px; border:1px solid transparent;
                    transition:transform .05s ease, box-shadow .15s ease; }
                .prompt-btn:active { transform:translateY(1px); }
                .prompt-btn-primary { background:#3b5bdb; color:#fff; }
                .prompt-btn-primary:hover { box-shadow:0 3px 10px rgba(59,91,219,.35); }
                .prompt-btn-primary.copied { background:#2b8a3e; }
                .prompt-btn-secondary { background:#fff; color:#3b5bdb; border-color:#c3ccf0; }
                .prompt-btn-secondary:hover { box-shadow:0 3px 10px rgba(0,0,0,.08); }
            </style>
            """;

    /** Copies the largest fenced code block (the classifier prompt) to the clipboard. */
    private static final String PROMPT_COPY_SCRIPT = """
            <script>
            (function () {
              var btn = document.getElementById('copy-prompt-btn');
              if (!btn) return;
              function promptText() {
                var blocks = document.querySelectorAll('.content pre'), best = null;
                blocks.forEach(function (b) {
                  if (!best || b.innerText.length > best.innerText.length) best = b;
                });
                return best ? best.innerText : '';
              }
              btn.addEventListener('click', function () {
                var text = promptText(), done = function () {
                  btn.classList.add('copied');
                  var label = btn.textContent; btn.textContent = '✅ Copied!';
                  setTimeout(function () { btn.textContent = label; btn.classList.remove('copied'); }, 2000);
                };
                if (navigator.clipboard && navigator.clipboard.writeText) {
                  navigator.clipboard.writeText(text).then(done, function () { fallback(text, done); });
                } else { fallback(text, done); }
              });
              function fallback(text, done) {
                var ta = document.createElement('textarea');
                ta.value = text; ta.style.position = 'fixed'; ta.style.opacity = '0';
                document.body.appendChild(ta); ta.select();
                try { document.execCommand('copy'); done(); } catch (e) {}
                document.body.removeChild(ta);
              }
            })();
            </script>
            """;

    public void brochure(Context ctx) {        Path brochureDoc = docsPath.resolve("PRODUCT-BROCHURE.md");
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
                    <link rel="icon" href="/img/favicon.ico" sizes="any">
                    <link rel="icon" type="image/svg+xml" href="/img/aicmm-icon.svg">
                    <script src="https://cdn.jsdelivr.net/npm/mermaid@10/dist/mermaid.min.js"></script>
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
                            <a href="/brochure" class="%s">Brochure</a>
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
                "classify-by-prompting".equals(activePage) ? "active" : "",
                "release-notes".equals(activePage) ? "active" : "",
                "schema".equals(activePage) ? "active" : "",
                content
        );
    }
}
