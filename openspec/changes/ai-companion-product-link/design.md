## Context

BrowserFX AI Companion sends page content to the AI when the user asks a question. Currently, the question path uses `PageContentExtractor.extractContent()`, which returns only plain text (e.g. from `clone.textContent`); all `<a href="...">` are stripped to their visible text, so no URLs are sent. The "Extract links" button uses the same text-only content and then regex-extracts URLs from that text, so on product pages (where links exist only in the DOM) it finds no or few links. `PageContentExtractor.extractContentWithLinks()` already exists and returns main content plus an "Additional links found on page" section with "linkText: url" lines (up to 30); it is not used for the question path or for the Extract links action.

## Goals / Non-Goals

**Goals:**
- Enable the AI to answer "provide the link for [product X]" by sending link-inclusive content for all user questions.
- Make the "Extract links" action return DOM-derived links (link text + URL) on product and catalog pages.
- Reuse existing `extractContentWithLinks()`; no new extraction logic required.

**Non-Goals:**
- Changing the LiteLLM API contract or adding new API endpoints.
- Detecting "link-related" intent to switch extraction mode (we send link-inclusive content for every question).
- Increasing the number of links beyond the current extractContentWithLinks limit (e.g. 30).

## Decisions

### 1. Use link-inclusive content for all user questions

**Decision:** In `AICompanionController.executeQuestion()`, call `extractContentWithLinksSafe()` instead of `extractContentSafe()` so that every QUESTION-type request receives page text plus the "Additional links found on page" block (link text and URL pairs).

**Rationale:** The AI can then answer link-related questions without intent detection. Slightly larger context is acceptable (existing `extractContentWithLinks` already uses a higher character cap). Alternative considered: detect keywords like "link" or "url" and only then use link-inclusive content; rejected to avoid missing paraphrased requests.

**Files:** `AICompanionController.java` (single call site in `executeQuestion()`).

---

### 2. Use link-inclusive content for Extract links action

**Decision:** In `AICompanionController.onExtractLinks()`, use `extractContentWithLinksSafe()` instead of `extractContentSafe()`. Then either (a) send that content to the AI with `PromptType.EXTRACT_LINKS` and display the AI response, or (b) parse the "Additional links found on page" section (lines of the form "linkText: url") and display them directly. Prefer (b) for predictability and no extra API call; use (a) if formatted narrative is desired.

**Rationale:** The current flow uses text-only content and regex, which fails on DOM-only links. Using content that already contains "linkText: url" lines ensures product links appear. Parsing the section is simple and avoids latency/cost of another AI call.

**Files:** `AICompanionController.java` (`onExtractLinks()`).

---

### 3. Optional: QUESTION system prompt for link requests

**Decision:** Optionally extend the QUESTION system prompt in `AIChatService` to state that when the user asks for a link or URL, the assistant should use the "Additional links found on page" section (if present) and return the exact URL.

**Rationale:** Improves consistency of link answers; low risk. Can be done in a follow-up if time-constrained.

**Files:** `AIChatService.java` (QUESTION `PromptType` system prompt).

---

### 4. Optional: Product selectors in link-extraction script

**Decision:** Optionally add `.product-item` and `.product-card` to the `contentSelectors` array in `PageContentExtractor.buildExtractionWithLinksScript()` so product blocks are preferred when extracting main content and links from product pages.

**Rationale:** Aligns with `buildExtractionScript()`, which already includes these selectors; increases likelihood that product links appear in the first 30 links on catalog pages.

**Files:** `PageContentExtractor.java` (`buildExtractionWithLinksScript()`).

## Risks / Trade-offs

- **Larger context for every question:** Link-inclusive content is longer than text-only. Mitigation: existing `MAX_CONTENT_LENGTH * 2` cap in `extractContentWithLinks()` already limits size; monitor token usage if needed.
- **Extract links UX:** If we parse "Additional links found on page" and display raw "linkText: url" lines, the list may be long. Mitigation: keep current display (e.g. list in chat); optional future improvement to truncate or group.
- **No migration:** In-app only; no persisted data or API contract change. No rollback plan needed beyond reverting the code change.
