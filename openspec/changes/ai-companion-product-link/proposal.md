## Why

The AI Companion cannot answer user requests like "provide the link for [product X]" because free-form questions send only text-only page content to the AI; the model never receives URLs or link-text-to-URL associations. A secondary issue: the "Extract links" button uses the same text-only content and regex-based URL extraction, so on product pages (where links live in the DOM as `<a href="...">`) it fails to find links. Fixing this improves usefulness on product catalogs and any page where users ask for specific links.

## What Changes

- When the user sends a question (free-form) to the AI Companion, the system SHALL send page content that includes link text and URLs where available (e.g. "Product A: https://...") so the AI can answer "what's the link for X?" with the actual URL.
- The "Extract links" quick action SHALL use content that includes DOM-derived link text and URL pairs, so the listed links include product/catalog links, not only URLs that appear as literal text on the page.
- Optionally: extend the QUESTION system prompt so the AI is instructed to use the links section when the user asks for a link or URL; optionally add product-oriented selectors to the link-extraction script for better coverage on product pages.

## Capabilities

### New Capabilities

- `ai-companion-links`: AI Companion SHALL receive link-inclusive page content for user questions and SHALL expose DOM links (link text + URL) for the Extract links action, so users can get the link for a specific product and the Links button returns real page links on product/catalog pages.

### Modified Capabilities

- (None. This change introduces a new capability; existing AI Companion specs live in other changes and are not being modified at the requirement level.)

## Impact

- **AICompanionController.java**: Use `extractContentWithLinksSafe()` instead of `extractContentSafe()` in the question path and in the Extract links handler.
- **AIChatService.java**: Optional update to QUESTION system prompt for link-request handling.
- **PageContentExtractor.java**: Optional addition of product selectors in `buildExtractionWithLinksScript()`.
- No new dependencies; no API contract changes.
