## Context

The project is a Maven-based JavaFX desktop app currently scaffolded as a Hello World. JavaFX ships with a `WebView` component backed by a WebKit engine (`WebEngine`) that can load and render full web pages. This built-in capability makes JavaFX an appropriate platform for a lightweight desktop browser without introducing a third-party rendering engine.

The existing MVC structure (`Application` → `FXMLLoader` → `Controller`) is retained; only the view and controller are replaced.

## Goals / Non-Goals

**Goals:**
- Render arbitrary web pages using `javafx.web.WebView` / `WebEngine`
- Provide an address bar that accepts URL input and reflects the current loaded URL
- Provide Back, Forward, Reload, and Home navigation buttons
- Show a status bar with page load progress and current page title
- Clean up the Hello World skeleton entirely

**Non-Goals:**
- Tabbed browsing (out of scope for v1)
- Bookmarks or browsing history UI
- Custom download manager
- Extension/plugin support
- Cookie or session management UI
- Cross-platform packaging/distribution

## Decisions

### 1. Use JavaFX `WebView` / `WebEngine` (built-in, no external dependency)

**Decision**: Use `javafx.web.WebView` as the rendering component.

**Rationale**: It is bundled with the JavaFX SDK, requires no additional dependency, and supports HTML5/CSS3/JavaScript via WebKit. Alternatives (e.g., embedding Chromium via JCEF) would add significant complexity and binary size for a v1 browser.

**Trade-off**: WebKit in JavaFX is older than a standalone Chromium and may not support the very latest web APIs, but is sufficient for general browsing.

---

### 2. FXML + single `BrowserController` for the UI

**Decision**: Define the full browser layout in `browser-view.fxml` and wire all controls through a single `BrowserController`.

**Rationale**: Keeps the existing pattern from the scaffold. A single controller is appropriate for the scope — address bar, nav buttons, WebView, and status bar are tightly coupled through `WebEngine`.

---

### 3. `BorderPane` as root layout

**Decision**: Use `BorderPane` as the FXML root:
- `TOP`: `ToolBar` containing nav buttons and address bar (`TextField`)
- `CENTER`: `WebView` (grows to fill all available space)
- `BOTTOM`: `Label` for status/progress

**Rationale**: `BorderPane` naturally separates toolbar, content, and status regions and automatically stretches the center region, making the WebView fill the window without manual layout math.

---

### 4. URL normalization in the controller

**Decision**: If the user types a bare hostname (e.g., `google.com`), prepend `https://` before passing to `WebEngine.load()`.

**Rationale**: Matches user expectations from commercial browsers. No external library needed — a simple `startsWith("http")` check suffices for v1.

---

### 5. Module system — add `javafx.web`

**Decision**: Add `requires javafx.web;` to `module-info.java` and add the `javafx-web` artifact to `pom.xml` dependencies.

**Rationale**: JavaFX modules are split; `WebView` is not part of `javafx.controls`. Without this, the module graph will fail at runtime.

## Risks / Trade-offs

- **WebKit version lag** → Some modern sites may render incorrectly. Mitigation: out of scope for v1; document as a known limitation.
- **Thread safety** — `WebEngine.load()` must be called on the JavaFX Application Thread. Mitigation: all calls are made from FXML event handlers or `Platform.runLater()`, which already run on the FX thread.
- **`module-info.java` missing** — The repo currently has `module-info.java` deleted (shown as `AD` in git status). Mitigation: recreate it as part of this change with the correct `requires` and `opens` directives.

## Migration Plan

1. Add `javafx-web` dependency to `pom.xml`
2. Recreate `module-info.java` with `requires javafx.web` and `opens com.kdue.browserfx`
3. Delete `HelloController.java` and `hello-view.fxml`
4. Create `BrowserController.java` and `browser-view.fxml`
5. Refactor `HelloApplication` → `BrowserApplication` pointing to the new FXML
6. Verify the app launches and can load a URL

## Open Questions

- Should the default home page be configurable (e.g., read from a properties file) or hardcoded for v1? → Hardcode `https://www.google.com` for v1.
- Should the status bar show load progress as a `ProgressBar` or just text? → Text only for v1 to keep the layout simple.
