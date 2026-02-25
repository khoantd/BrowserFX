package com.kdue.browserfx;

import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

/**
 * Converts markdown to a full HTML document with embedded CSS for use in WebView.
 * No external resources are loaded; safe for loadContent(html, "text/html").
 */
public final class MarkdownRenderer {

    private static final Parser PARSER = Parser.builder().build();
    private static final HtmlRenderer RENDERER = HtmlRenderer.builder().build();

    private static final String CSS = """
        * { box-sizing: border-box; }
        body {
            margin: 0;
            padding: 6px 8px;
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
            font-size: 13px;
            line-height: 1.5;
            color: #2d3748;
            word-wrap: break-word;
        }
        h1, h2, h3, h4 { margin: 0.6em 0 0.3em; font-weight: 600; color: #1a202c; }
        h1 { font-size: 1.25em; }
        h2 { font-size: 1.15em; }
        h3, h4 { font-size: 1.05em; }
        p { margin: 0.4em 0; }
        ul, ol { margin: 0.4em 0; padding-left: 1.4em; }
        li { margin: 0.2em 0; }
        blockquote {
            margin: 0.5em 0;
            padding: 0.25em 0 0.25em 10px;
            border-left: 3px solid #cbd5e0;
            color: #4a5568;
        }
        code {
            font-family: ui-monospace, "Cascadia Code", "Source Code Pro", Menlo, monospace;
            font-size: 12px;
            background: #edf2f7;
            padding: 2px 5px;
            border-radius: 4px;
        }
        pre {
            margin: 0.5em 0;
            padding: 10px;
            background: #2d3748;
            color: #e2e8f0;
            border-radius: 6px;
            overflow-x: auto;
        }
        pre code {
            background: none;
            padding: 0;
            color: inherit;
            font-size: 12px;
        }
        a { color: #4299e1; text-decoration: none; cursor: pointer; }
        a:hover { text-decoration: underline; }
        hr { border: none; border-top: 1px solid #e2e8f0; margin: 0.6em 0; }
        table { border-collapse: collapse; width: 100%; margin: 0.5em 0; }
        th, td { border: 1px solid #e2e8f0; padding: 4px 8px; text-align: left; }
        th { background: #f7fafc; font-weight: 600; }
        """;

    private MarkdownRenderer() {}

    /**
     * Converts markdown to a complete HTML document with embedded styles.
     * Intended for WebEngine.loadContent(html, "text/html") with no base URL.
     */
    public static String toHtmlDocument(String markdown) {
        return toHtmlDocument(markdown, null);
    }

    /**
     * Same as {@link #toHtmlDocument(String)} but appends {@code extraCss} to the document styles.
     * Use for reading mode or other contexts that need custom typography (e.g. font-size, max-width).
     */
    public static String toHtmlDocument(String markdown, String extraCss) {
        String styles = CSS + (extraCss != null && !extraCss.isBlank() ? extraCss : "");
        if (markdown == null || markdown.isBlank()) {
            return "<html><head><meta charset=\"UTF-8\"><style>" + styles + "</style></head><body></body></html>";
        }
        Node document = PARSER.parse(markdown);
        String bodyHtml = RENDERER.render(document);
        return "<html><head><meta charset=\"UTF-8\"><style>" + styles + "</style></head><body>" + bodyHtml + "</body></html>";
    }
}
