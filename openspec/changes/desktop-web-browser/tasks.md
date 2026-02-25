## 1. Project Configuration

- [x] 1.1 Add `javafx-web` dependency to `pom.xml` (same version as existing javafx artifacts)
- [x] 1.2 Recreate `src/main/java/module-info.java` with `requires javafx.controls`, `requires javafx.fxml`, `requires javafx.web`, `requires org.slf4j`, and `opens com.kdue.browserfx to javafx.fxml`

## 2. Remove Hello World Skeleton

- [x] 2.1 Delete `src/main/java/com/kdue/browserfx/HelloController.java`
- [x] 2.2 Delete `src/main/resources/com/kdue/browserfx/hello-view.fxml`

## 3. Browser FXML Layout

- [x] 3.1 Create `src/main/resources/com/kdue/browserfx/browser-view.fxml` with a `BorderPane` root and `fx:controller="com.kdue.browserfx.BrowserController"`
- [x] 3.2 Add a `ToolBar` in the TOP region containing: Back button (`fx:id="backButton"`), Forward button (`fx:id="forwardButton"`), Reload button (`fx:id="reloadButton"`), Home button (`fx:id="homeButton"`), and address `TextField` (`fx:id="addressBar"`) with `HBox.hgrow="ALWAYS"`
- [x] 3.3 Add a `WebView` (`fx:id="webView"`) in the CENTER region
- [x] 3.4 Add a `Label` (`fx:id="statusLabel"`) in the BOTTOM region with padding

## 4. BrowserController Implementation

- [x] 4.1 Create `src/main/java/com/kdue/browserfx/BrowserController.java` with `@FXML` fields for all controls: `backButton`, `forwardButton`, `reloadButton`, `homeButton`, `addressBar`, `webView`, `statusLabel`
- [x] 4.2 Implement `initialize()`: obtain `WebEngine` from `webView.getEngine()`, enable JavaScript, load the home page (`https://www.google.com`)
- [x] 4.3 Bind `addressBar` text to `webEngine.locationProperty()` so the address bar always reflects the current URL (update on location change, not on every keystroke)
- [x] 4.4 Add focus listener on `addressBar` to select all text when the field gains focus
- [x] 4.5 Add `onAction` handler on `addressBar` (Enter key): read text, prepend `https://` if no scheme present, call `webEngine.load(url)`
- [x] 4.6 Implement `onBackClick()`: call `webEngine.getHistory().go(-1)`
- [x] 4.7 Implement `onForwardClick()`: call `webEngine.getHistory().go(1)`
- [x] 4.8 Implement `onReloadClick()`: call `webEngine.reload()`
- [x] 4.9 Implement `onHomeClick()`: call `webEngine.load("https://www.google.com")`
- [x] 4.10 Bind `backButton.disableProperty()` to a boolean expression: Back is disabled when history current index is 0
- [x] 4.11 Bind `forwardButton.disableProperty()` to a boolean expression: Forward is disabled when history current index equals `history.getEntries().size() - 1`
- [x] 4.12 Add listener on `webEngine.getLoadWorker().stateProperty()`: set `statusLabel` to "Loading…" on `RUNNING`, page title or URL on `SUCCEEDED`, "Failed to load page" on `FAILED`

## 5. Refactor Application Entry Point

- [x] 5.1 Rename `HelloApplication.java` to `BrowserApplication.java` (update class name, constants, and FXML resource reference to `browser-view.fxml`)
- [x] 5.2 Update window title constant to `"BrowserFX"` and default size to `1024 × 768`
- [x] 5.3 Update `pom.xml` `<mainClass>` to `com.kdue.browserfx/com.kdue.browserfx.BrowserApplication` if present

## 6. Verification

- [x] 6.1 Build the project with `mvn clean package` and confirm no compilation errors
- [ ] 6.2 Launch the app and verify the home page (`https://www.google.com`) loads on startup
- [ ] 6.3 Type a URL in the address bar, press Enter, and verify the page loads and the address bar updates
- [ ] 6.4 Type a bare hostname (e.g., `github.com`), press Enter, and verify `https://` is prepended and the page loads
- [ ] 6.5 Navigate to two pages then verify Back/Forward enable and disable correctly
- [ ] 6.6 Click Reload and verify the current page refreshes
- [ ] 6.7 Click Home and verify `https://www.google.com` loads
- [ ] 6.8 Click the address bar and verify all text is selected
- [ ] 6.9 Resize the window and verify the WebView expands to fill the available space
