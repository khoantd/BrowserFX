## ADDED Requirements

### Requirement: Web page rendering
The browser SHALL render web pages using JavaFX `WebView` backed by `WebEngine`. The `WebView` SHALL occupy the full available content area of the window and resize with it.

#### Scenario: Page loads and renders
- **WHEN** the WebEngine loads a valid URL
- **THEN** the page content is rendered and visible in the WebView

#### Scenario: WebView fills the window
- **WHEN** the user resizes the application window
- **THEN** the WebView expands or contracts to fill the center region

---

### Requirement: Load progress reporting
The browser SHALL display the current load state in the status bar. During loading it SHALL show "Loading…"; when complete it SHALL show the page title or the final URL.

#### Scenario: Status bar shows loading state
- **WHEN** the WebEngine begins loading a page
- **THEN** the status bar displays "Loading…"

#### Scenario: Status bar shows page title after load
- **WHEN** a page finishes loading and has a non-empty `<title>`
- **THEN** the status bar displays the page title

#### Scenario: Status bar shows URL when no title
- **WHEN** a page finishes loading and has no `<title>`
- **THEN** the status bar displays the page URL

---

### Requirement: Load error handling
The browser SHALL display a user-visible error message in the status bar when the WebEngine fails to load a URL (e.g., network error, DNS failure).

#### Scenario: Load failure shows error in status bar
- **WHEN** the WebEngine encounters a load error
- **THEN** the status bar displays "Failed to load page"

---

### Requirement: JavaScript enabled
The WebEngine SHALL have JavaScript enabled to support modern web pages.

#### Scenario: JavaScript executes on loaded pages
- **WHEN** a page containing JavaScript is loaded
- **THEN** the JavaScript executes normally within the WebEngine sandbox

---

### Requirement: Initial home page load
The browser SHALL automatically load the home page (`https://www.google.com`) when the application starts.

#### Scenario: App starts and loads home page
- **WHEN** the application launches
- **THEN** the WebEngine loads `https://www.google.com` without any user interaction
