## ADDED Requirements

### Requirement: Link-inclusive content for user questions
When the user sends a question to the AI Companion, the system SHALL send page content to the AI that includes link text and URLs where available (e.g. "linkText: url" pairs), so the AI can answer requests for a specific link (e.g. "what is the link for product X?") with the actual URL.

#### Scenario: User asks for link of a specific product
- **WHEN** the user types a question such as "What's the link for the blue widget?" or "Give me the link for product X" and sends it
- **THEN** the content sent to the AI includes both page text and a list of link text and URL pairs from the page
- **AND** the AI can respond with the correct URL when the product or link text appears in that list

#### Scenario: Question path uses link-aware extraction
- **WHEN** the user sends any free-form question from the AI Companion input
- **THEN** the system uses content extracted with links (link text and URL pairs) as context for the AI
- **AND** not only plain text without URLs

---

### Requirement: Extract links action uses DOM links
The "Extract links" quick action SHALL use page content that includes DOM-derived link text and URL pairs, so that the listed links include links from the page structure (e.g. product links, navigation links), not only URLs that appear as literal text in the page.

#### Scenario: Extract links on product page returns product links
- **WHEN** the user is on a product catalog or product page and clicks the "Extract links" (or "Links") quick action
- **THEN** the system extracts links from the DOM (e.g. `<a href="...">` with their link text)
- **AND** the displayed list includes product links (link text and URL) that are present in the page structure

#### Scenario: Extract links does not rely on URLs in plain text only
- **WHEN** the "Extract links" action runs
- **THEN** the system does not rely solely on regex over plain text to find URLs
- **AND** it uses content that includes explicitly extracted link text and URL pairs from the DOM
