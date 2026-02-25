# BrowserFX

A desktop web browser built with JavaFX.

## Features

- Web browsing with JavaFX WebView (WebKit)
- Navigation controls (Back, Forward, Reload, Home)
- Address bar with URL auto-completion
- Fullscreen mode (F11 or Ctrl+Shift+F)
- **AI Companion** - Ask questions about the current webpage using AI

## Build & Run

```bash
mvn javafx:run
```

## Dependencies

- JavaFX 23.0.1 (controls, fxml, web)
- OkHttp 4.12.0 (HTTP client for AI API)
- Java 23

## Project Structure

```
src/main/java/com/kdue/browserfx/
├── BrowserApplication.java      # Main application entry point
├── BrowserController.java      # Browser UI controller
├── AICompanionController.java  # AI Companion panel controller
├── ConfigManager.java         # Configuration management
├── PageContentExtractor.java  # Web page content extraction
└── AIChatService.java         # LiteLLM API integration

src/main/resources/com/kdue/browserfx/
├── browser-view.fxml         # Main browser UI
└── ai-companion-panel.fxml   # AI Companion panel UI
```

## AI Companion

The AI Companion feature allows you to ask questions about the current webpage using an AI model via LiteLLM proxy.

### Configuration

The config file is stored in an OS-appropriate location:

| OS | Config Path |
|----|-------------|
| Windows | `%APPDATA%\BrowserFX\config.properties` |
| macOS | `~/Library/Application Support/BrowserFX/config.properties` |
| Linux | `~/.config/browserfx/config.properties` |

**Custom config path**: `java -Dbrowserfx.config=/path/to/config.properties -jar app.jar`

Edit the config file to set your LiteLLM credentials:

```properties
# BrowserFX Configuration
litellm.endpoint=https://your-litellm-proxy.com
litellm.model=gpt-4o-mini
litellm.api_key=your-api-key
```

### Usage

1. Navigate to any webpage
2. Click the 🤖 button in the toolbar **OR** press `Ctrl+Shift+A` (Windows/Linux) / `Cmd+Shift+A` (macOS)
3. Type your question about the page content
4. Press Enter or click Send

### Keyboard Shortcuts

| Shortcut | Action |
|----------|--------|
| `Ctrl+Shift+A` / `Cmd+Shift+A` | Toggle AI Companion |
| `F11` / `Ctrl+Shift+F` | Toggle fullscreen |
| `Alt+Left` | Back |
| `Alt+Right` | Forward |

## Native Build

Build native installers:

```bash
mvn clean package -Pnative
```

Output:
- macOS: `target/BrowserFX-*.dmg`
- Windows: `target/BrowserFX-*.exe`
- Linux: `target/BrowserFX-*.deb`
