package org.aicmm.site;

import org.commonmark.Extension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.ext.heading.anchor.HeadingAnchorExtension;
import org.commonmark.node.FencedCodeBlock;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.AttributeProvider;
import org.commonmark.renderer.html.HtmlRenderer;

import java.util.List;
import java.util.Map;

/**
 * Renders Markdown to HTML with support for:
 * - GitHub Flavored Markdown tables
 * - Heading anchors for navigation
 * - Mermaid diagram blocks (rendered client-side via mermaid.js)
 */
public class MarkdownRenderer {

    private final Parser parser;
    private final HtmlRenderer renderer;

    public MarkdownRenderer() {
        List<Extension> extensions = List.of(
                TablesExtension.create(),
                HeadingAnchorExtension.create()
        );

        this.parser = Parser.builder()
                .extensions(extensions)
                .build();

        this.renderer = HtmlRenderer.builder()
                .extensions(extensions)
                .attributeProviderFactory(ctx -> new MermaidAttributeProvider())
                .build();
    }

    /**
     * Render Markdown content to HTML.
     * Mermaid code blocks (```mermaid) are converted to <div class="mermaid"> elements.
     */
    public String render(String markdown) {
        // Pre-process: convert ```mermaid blocks to a special marker
        String processed = markdown.replaceAll(
                "```mermaid\\s*\\n(.*?)```",
                "<div class=\"mermaid\">\n$1</div>"
        );

        // Handle case where regex didn't match due to line endings
        if (processed.contains("```mermaid")) {
            processed = processMermaidBlocks(processed);
        }

        Node document = parser.parse(processed);
        return renderer.render(document);
    }

    private String processMermaidBlocks(String content) {
        StringBuilder result = new StringBuilder();
        String[] lines = content.split("\n");
        boolean inMermaid = false;
        StringBuilder mermaidContent = new StringBuilder();

        for (String line : lines) {
            if (line.trim().startsWith("```mermaid")) {
                inMermaid = true;
                mermaidContent = new StringBuilder();
                result.append("<div class=\"mermaid\">\n");
            } else if (inMermaid && line.trim().equals("```")) {
                inMermaid = false;
                result.append(mermaidContent);
                result.append("</div>\n");
            } else if (inMermaid) {
                mermaidContent.append(line).append("\n");
            } else {
                result.append(line).append("\n");
            }
        }
        return result.toString();
    }

    /**
     * Adds 'mermaid' class to fenced code blocks with 'mermaid' info string.
     */
    private static class MermaidAttributeProvider implements AttributeProvider {
        @Override
        public void setAttributes(Node node, String tagName, Map<String, String> attributes) {
            if (node instanceof FencedCodeBlock codeBlock) {
                if ("mermaid".equals(codeBlock.getInfo())) {
                    attributes.put("class", "mermaid");
                }
            }
        }
    }
}
