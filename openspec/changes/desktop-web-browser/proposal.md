## Why

The current codebase is a JavaFX Hello World skeleton with no real functionality. We need to replace it with a functional desktop web browser that renders web pages using JavaFX's built-in WebKit engine (WebView/WebEngine), giving users a native desktop browsing experience.

## What Changes

- **BREAKING**: Remove the Hello World UI (`hello-view.fxml`, `HelloController.java`) and replace with a full browser layout
- **BREAKING**: Rename `HelloApplication` to `BrowserApplication` as the new entry point
- Add an address bar for URL input and navigation
- Add navigation toolbar with Back, Forward, Reload, and Home buttons
- Add a WebView pane that occupies the main content area
- Add a status bar showing page load status and current URL
- Wire navigation controls to the WebEngine for page loading and history traversal

## Capabilities

### New Capabilities
- `browser-navigation`: Back, Forward, Reload, and Home button controls wired to WebEngine history and load actions
- `address-bar`: URL input field that accepts user input, triggers page load on Enter, and updates to reflect the current page URL
- `web-renderer`: JavaFX WebView/WebEngine integration that loads and renders web pages, handles redirects, and reports load status

### Modified Capabilities
<!-- No existing specs to modify — this is a greenfield replacement -->

## Impact

- **Removed files**: `HelloController.java`, `hello-view.fxml`
- **Modified files**: `HelloApplication.java` (renamed/refactored to `BrowserApplication.java`)
- **New files**: `BrowserController.java`, `browser-view.fxml`
- **Dependencies**: JavaFX WebView module (`javafx.web`) must be added to `pom.xml` and `module-info.java`
- **Module system**: `module-info.java` needs `requires javafx.web` and `opens` directive for the new controller package
