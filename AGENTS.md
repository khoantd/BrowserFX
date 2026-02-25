# BrowserFX

A desktop web browser built with JavaFX with AI Companion feature.

## Project Structure

- **BrowserApplication.java** - Main JavaFX application entry point
- **BrowserController.java** - Browser UI controller (toolbar, navigation, WebView)
- **AICompanionController.java** - AI Companion panel controller
- **ConfigManager.java** - Cross-platform configuration management
- **PageContentExtractor.java** - Web page content extraction via JavaScript
- **AIChatService.java** - LiteLLM API integration

## Build & Run

```bash
mvn javafx:run
```

## Dependencies

- JavaFX 23.0.1 (controls, fxml, web)
- OkHttp 4.12.0
- Java 23

## Configuration

- Config stored in OS-appropriate location (see README.md)
- Main class: `com.kdue.browserfx.BrowserApplication`

## AI Companion

- Toggle: Click 🤖 button or `Ctrl+Shift+A`
- Config: Edit `~/.browserfx/config.properties` (or OS-specific path)
