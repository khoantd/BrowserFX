# AI Companion Feature - Design

## Context

BrowserFX is a JavaFX-based desktop web browser. The goal is to add an AI Companion feature that can read and understand the content of the currently loaded website, allowing users to ask questions about the page content and get AI-powered responses.

## Goals / Non-Goals

**Goals:**
- Add an AI Companion button to the toolbar
- Extract readable text content from the current web page using JavaScript
- Send extracted content to an AI API for analysis
- Display AI responses in a chat-like interface
- Provide a toggle to show/hide the AI Companion panel

**Non-Goals:**
- AI model training or fine-tuning
- Voice input/output
- Persistent chat history across sessions
- Multi-tab support for AI Companion
- AI-generated content (writing, code generation) - read-only for now

---

## Decisions

### 1. AI Companion Panel Layout

**Decision**: Add a collapsible side panel (drawer) on the right side of the browser.

**Rationale**: 
- Keeps the main browsing view unobstructed
- Similar to Edge Copilot, Claude, and other AI browser assistants
- Uses JavaFX `Drawer` or custom `HBox` with animation

---

### 2. Content Extraction Method

**Decision**: Use JavaScript injection to extract page content via `WebEngine.executeScript()`.

**Rationale**:
- Direct access to DOM allows precise content extraction
- Can target `<main>`, `<article>`, or fallback to `<body>` text
- JavaFX `WebEngine` supports `executeScript()`

**Script strategy**:
1. Try `<main>`, `<article>`, `.content`, `.post-content` selectors
2. Fall back to `<body>` text content
3. Strip scripts, styles, navigation elements
4. Limit to first 8000 characters to fit most AI context windows

---

### 3. AI Provider

**Decision**: Use LiteLLM proxy for AI API calls.

**Rationale**:
- Unified API that supports multiple LLM providers (OpenAI, Anthropic, Azure, local, etc.)
- Single endpoint configuration
- Easy to switch models/providers
- Can be self-hosted or use LiteLLM's managed service

**Trade-off**: Requires user to configure their LiteLLM endpoint URL and API key.

---

### 4. Configuration File Storage (Cross-Platform)

**Decision**: Use OS-appropriate config directories following platform conventions.

**Rationale**:
- Follows platform conventions for config file location
- Easy for users to find and modify
- Cross-platform consistent API via Java

**Config Location by OS**:
| OS | Location |
|---|---|
| Windows | `%APPDATA%\BrowserFX\config.properties` |
| macOS | `~/Library/Application Support/BrowserFX/config.properties` |
| Linux | `~/.config/browserfx/config.properties` (XDG compliant) |

**Implementation in ConfigManager**:
```java
private Path getConfigDir() {
    String os = System.getProperty("os.name").toLowerCase();
    
    if (os.contains("win")) {
        String appData = System.getenv("APPDATA");
        return appData != null ? Paths.get(appData, "BrowserFX") 
            : Paths.get(System.getProperty("user.home"), "BrowserFX");
    } else if (os.contains("mac")) {
        return Paths.get(System.getProperty("user.home"), 
            "Library/Application Support/BrowserFX");
    } else { // Linux/Unix
        String xdgConfig = System.getenv("XDG_CONFIG_HOME");
        if (xdgConfig != null && !xdgConfig.isBlank()) {
            return Paths.get(xdgConfig, "browserfx");
        }
        return Paths.get(System.getProperty("user.home"), ".config/browserfx");
    }
}
```

**Alternative: CLI Override**
Users can specify custom config path: `--config=/path/to/config.properties`

**First-run Setup**
On first launch with no config, show a setup dialog prompting for LiteLLM endpoint, model, and API key.

---

### 5. Architecture

**Decision**: Follow existing MVC pattern
- `AICompanionPanel.java` - View (FXML + Controller)
- `AICompanionController.java` - Controller handling UI logic
- `PageContentExtractor.java` - Service for JavaScript content extraction
- `AIChatService.java` - Service for AI API communication

**Rationale**: Consistent with existing BrowserFX architecture.

---

## UI Design

### Toolbar Addition
- Add "AI" button (🤖 icon) to the toolbar, next to fullscreen button
- Keyboard shortcut: `Ctrl+Shift+A` (Windows/Linux) / `Cmd+Shift+A` (macOS) to toggle panel

### Panel Design
- Width: 350px (collapsible)
- Header: "AI Companion" with close button
- Chat area: Scrollable list of messages (user question, AI response)
- Input area: TextField + Send button
- Page content is extracted but NOT displayed (hidden context)

### Visual States
- **Default**: Panel hidden
- **Active**: Panel slides in from right
- **Loading**: Show spinner while AI is processing
- **Error**: Show error message in chat area

---

## Risks / Trade-offs

- **API key exposure**: Users must provide their own key; warn about not committing it
- **Content extraction quality**: Some sites use heavy JS rendering - extraction may be incomplete
- **WebKit limitations**: Older WebKit may not support modern JavaScript-heavy sites
- **Rate limiting**: OpenAI API has rate limits - handle gracefully

---

## Migration Plan

1. Add ` okhttp3` dependency for HTTP requests to `pom.xml`
2. Create `config.properties` for API key storage
3. Add AI button to `browser-view.fxml`
4. Create `AICompanionPanel.fxml` and controller
5. Create `PageContentExtractor.java` service
6. Create `AIChatService.java` service
7. Integrate panel into main layout
8. Test with various websites
