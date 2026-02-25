## ADDED Requirements

### Requirement: URL entry and load
The address bar SHALL accept a URL typed by the user and load that URL in the WebView when the user presses Enter.

#### Scenario: User enters a full URL and presses Enter
- **WHEN** the user types a full URL (e.g., `https://example.com`) into the address bar and presses Enter
- **THEN** the browser loads that URL in the WebView

#### Scenario: User enters a bare hostname
- **WHEN** the user types a hostname without a scheme (e.g., `example.com`) and presses Enter
- **THEN** the browser prepends `https://` and loads `https://example.com`

---

### Requirement: Address bar reflects current URL
The address bar SHALL always display the URL of the currently loaded page, updating automatically after navigation (including back/forward, redirects, and link clicks).

#### Scenario: Address bar updates after navigation button click
- **WHEN** the user navigates using Back, Forward, or Home
- **THEN** the address bar text updates to show the new page's URL

#### Scenario: Address bar updates after redirect
- **WHEN** the WebEngine follows an HTTP redirect
- **THEN** the address bar displays the final resolved URL

#### Scenario: Address bar updates after in-page link click
- **WHEN** the user clicks a hyperlink inside the rendered page
- **THEN** the address bar updates to reflect the new URL

---

### Requirement: Address bar text selection on focus
The address bar SHALL select all text when it receives focus, so the user can immediately type a new URL without manually clearing the field.

#### Scenario: Focus selects all text
- **WHEN** the user clicks the address bar or tabs into it
- **THEN** all existing text in the address bar is selected
