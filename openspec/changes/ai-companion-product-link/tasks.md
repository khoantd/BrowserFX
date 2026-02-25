## 1. Question path: link-inclusive content

- [x] 1.1 In `AICompanionController.executeQuestion()`, replace `extractContentSafe()` with `extractContentWithLinksSafe()` so the AI receives page text plus "Additional links found on page" (link text and URL pairs).

## 2. Extract links action: DOM links

- [x] 2.1 In `AICompanionController.onExtractLinks()`, use `extractContentWithLinksSafe()` instead of `extractContentSafe()`.
- [x] 2.2 Parse the "Additional links found on page" section from the extracted content (lines of the form "linkText: url") and display them in the chat (or send the link-inclusive content to the AI with EXTRACT_LINKS and show the AI response). Prefer parsing to avoid an extra API call.

## 3. Optional: system prompt and selectors

- [x] 3.1 (Optional) In `AIChatService`, extend the QUESTION system prompt to state that when the user asks for a link or URL, the assistant should use the "Additional links found on page" section if present and return the exact URL.
- [x] 3.2 (Optional) In `PageContentExtractor.buildExtractionWithLinksScript()`, add `.product-item` and `.product-card` to the `contentSelectors` array for better product-page coverage.
