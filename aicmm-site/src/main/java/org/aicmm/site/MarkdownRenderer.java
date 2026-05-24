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
     * Mermaid code blocks (```mermaid) are extracted before parsing to prevent
     * CommonMark from HTML-encoding their content (which breaks Mermaid syntax).
     */
    public String render(String markdown) {
        // Extract mermaid blocks and replace with placeholders
        java.util.List<String> mermaidBlocks = new java.util.ArrayList<>();
        String processed = extractMermaidBlocks(markdown, mermaidBlocks);

        // Parse remaining markdown normally
        Node document = parser.parse(processed);
        String html = renderer.render(document);

        // Re-insert raw mermaid blocks into the rendered HTML
        for (int i = 0; i < mermaidBlocks.size(); i++) {
            String placeholder = "<!--MERMAID_PLACEHOLDER_" + i + "-->";
            String mermaidDiv = "<div class=\"mermaid\">\n" + mermaidBlocks.get(i) + "</div>\n";
            html = html.replace("<p>" + placeholder + "</p>", mermaidDiv);
            html = html.replace(placeholder, mermaidDiv);
        }

        return html;
    }

    private String extractMermaidBlocks(String content, java.util.List<String> blocks) {
        StringBuilder result = new StringBuilder();
        String[] lines = content.split("\n");
        boolean inMermaid = false;
        StringBuilder mermaidContent = new StringBuilder();

        for (String line : lines) {
            if (line.trim().startsWith("```mermaid")) {
                inMermaid = true;
                mermaidContent = new StringBuilder();
            } else if (inMermaid && line.trim().equals("```")) {
                inMermaid = false;
                blocks.add(mermaidContent.toString());
                result.append("<!--MERMAID_PLACEHOLDER_").append(blocks.size() - 1).append("-->\n");
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
