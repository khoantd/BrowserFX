# Root Cause: AI Cannot Provide Product Info as Described

## Expected behavior (from prompts and design)

- **Help me choose the right product** and **Purchase advice** send page content that includes:
  - Product names and prices (in the main content)
  - An **"Additional links found on page"** section with lines like `linkText: url` so the AI can reference products and include URLs in its answer.

- The AI is instructed (e.g. in `AIChatService.PromptType.HELP_CHOOSE_PRODUCT`) to use that section and to "include the URL so the user can click through."

## Root causes

### 1. Truncation removes the "Additional links found on page" section (primary)

**Where:** `PageContentExtractor.extractContentWithLinks()` (lines 51–55).

The JavaScript returns a single string: **main content** + `"\n\nAdditional links found on page:\n"` + **links**. In Java, the full result is then truncated from the **start**:

```java
if (content.length() > MAX_CONTENT_LENGTH * 2) {
    content = content.substring(0, MAX_CONTENT_LENGTH * 2) + "...";
}
```

So we keep the **first** 16,000 characters and drop the rest. The links block is always at the **end**. On long pages (e.g. category or article with lots of text), the entire "Additional links found on page" section is cut off, so the AI never receives product (or any) links. The model is told to use that section but it is often missing.

**Fix:** Truncate only the main content, then append the links section in Java so the links block is never dropped. Alternatively, reserve a budget for links (e.g. main content up to 14,000 chars, then always append the full links section up to 2,000 chars).

---

### 2. Main content often doesn’t contain product names and prices

**Where:** `PageContentExtractor.buildExtractionWithLinksScript()`.

Content is chosen by "first container with ≥100 characters": `main` → `article` → first match from `contentSelectors` (e.g. `.content`, `.main-content`, `.product-list`, …). So:

- If `main` or `article` has 100+ characters of intro/category text, that block is used and we **never** use `.product-list` / `.products` / `.product-card`.
- The text sent to the AI may then be generic copy with **no** product names or prices. The AI is supposed to use "product names, prices, and … Additional links" but the main content may have neither.

**Fix:** For product-oriented flows, prefer product-specific containers when the page has product cards (e.g. run a lightweight check for `.product-card` / `.product-item` and, if present, prefer content from a product list container). Alternatively, always append a small "Products on page" block built from the same data as `extractProducts()` (see below).

---

### 3. Rich product data (cards) is never sent to the AI

**Where:** `AICompanionController` uses `extractContentWithLinksSafe()` for the prompt; product cards come from `extractProducts()` only for the UI.

- **Help me choose** / **Purchase advice** send only: `extractContentWithLinks()` → main content + "Additional links found on page".
- **Product cards** (title, url, imageUrl, price) from `extractProducts()` are used only in the chat UI (`appendProductCardsIfAvailable()`). That structured list is **never** included in the payload to the model.

So even when the UI shows product cards, the AI has no structured product list. It only sees whatever text and links made it into the truncated content. If the main content has no product list and the links section is truncated or has generic link text ("View product", "Shop now"), the AI cannot reliably provide product info or correct URLs.

**Fix:** For HELP_CHOOSE_PRODUCT and PURCHASE_ADVICE, optionally (or always) append a short "Products on page" section derived from `extractProducts()` (e.g. "Title | URL | Price" lines) so the model has explicit product info and URLs even when the main content or links block is poor or truncated.

---

## Summary

| Issue | Effect |
|-------|--------|
| Truncation of full string (content + links) | "Additional links found on page" is often missing; AI can’t use or return product URLs. |
| Content selector order (first ≥100 chars) | Main content may omit product names/prices. |
| Product cards not in prompt | AI never sees the structured product list the UI shows; can’t reliably compare or link products. |

Recommended order of fixes: (1) ensure the links section is never truncated, (2) include structured product info for product flows, (3) optionally prefer product containers when extracting main content on product pages.
