package com.kdue.browserfx;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextField;
import javafx.scene.control.ToolBar;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebHistory;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class BrowserController {

    private static final String HOME_URL = "https://edinvn.com/collections";
    private static final double AI_PANEL_WIDTH = 350;
    private static final double AI_PANEL_DIVIDER = 0.72;

    @FXML private Button    backButton;
    @FXML private Button    forwardButton;
    @FXML private Button    reloadButton;
    @FXML private Button    homeButton;
    @FXML private Button    aiButton;
    @FXML private Button    fullscreenButton;
    @FXML private ToolBar   toolBar;
    @FXML private TextField addressBar;
    @FXML private WebView   webView;
    @FXML private Label     statusLabel;
    @FXML private SplitPane mainSplitPane;
    @FXML private VBox      aiPanel;
    @FXML private VBox      loadingOverlay;

    private WebEngine webEngine;
    private AICompanionController aiCompanionController;
    private boolean aiPanelVisible = false;
    private boolean aiPanelVisibleBeforeFullscreen = false;

    @FXML
    public void initialize() {
        webEngine = webView.getEngine();
        webEngine.setJavaScriptEnabled(true);
        
        // Work around a JavaFX WebView bug where pages with image-based custom cursors
        // can cause a NullPointerException in PrismGraphicsManager / CursorManagerImpl.
        // Force all content to use the default cursor behavior instead of custom images.
        String cursorCss = "* { cursor: auto !important; }";
        String cursorCssEncoded = URLEncoder.encode(cursorCss, StandardCharsets.UTF_8)
            .replace("+", "%20");
        String cursorCssDataUri = "data:text/css," + cursorCssEncoded;
        webEngine.setUserStyleSheetLocation(cursorCssDataUri);
        
        webView.setPrefHeight(0);
        
        webView.setCache(true);
        
        webEngine.setUserDataDirectory(new java.io.File(
            System.getProperty("user.home"), ".browserfx/cache"));

        webEngine.locationProperty().addListener((obs, oldLoc, newLoc) -> {
            // Keep address bar as a passive display of the current URL.
            addressBar.setText(newLoc);
        });

        WebHistory history = webEngine.getHistory();
        backButton.disableProperty().bind(
            Bindings.createBooleanBinding(
                () -> history.getCurrentIndex() == 0,
                history.currentIndexProperty()
            )
        );
        forwardButton.disableProperty().bind(
            Bindings.createBooleanBinding(
                () -> history.getCurrentIndex() >= history.getEntries().size() - 1,
                history.currentIndexProperty(),
                history.getEntries()
            )
        );

        webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.RUNNING) {
                statusLabel.setText("Loading\u2026");
                setLoadingOverlayVisible(true);
            } else if (newState == Worker.State.SUCCEEDED) {
                String title = webEngine.getTitle();
                statusLabel.setText((title != null && !title.isBlank()) ? title : webEngine.getLocation());
                addressBar.setText(webEngine.getLocation());
                applySiteTweaks();
                setLoadingOverlayVisible(false);
            } else if (newState == Worker.State.FAILED) {
                statusLabel.setText("Failed to load page");
                setLoadingOverlayVisible(false);
            }
        });

        webView.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.addEventHandler(KeyEvent.KEY_PRESSED, e -> {
                    boolean isFullscreenShortcut =
                        e.getCode() == KeyCode.F11 ||
                        (e.getCode() == KeyCode.F && e.isShortcutDown() && e.isShiftDown());
                    if (isFullscreenShortcut) {
                        toggleFullscreen();
                        e.consume();
                    }
                    
                    boolean isAIShortcut =
                        (e.getCode() == KeyCode.A && e.isShortcutDown() && e.isShiftDown());
                    if (isAIShortcut) {
                        toggleAICompanion();
                        e.consume();
                    }
                });
                newScene.windowProperty().addListener((wo, oldWin, newWin) -> {
                    if (newWin instanceof Stage stage) {
                        stage.fullScreenProperty().addListener((fo, wasFS, isFS) -> {
                            // Toolbar follows fullscreen state
                            toolBar.setVisible(!isFS);
                            toolBar.setManaged(!isFS);
                            fullscreenButton.setText(isFS ? "\u2715" : "\u26F6");

                            if (isFS) {
                                // Remember current AI visibility and ensure AI is shown by default
                                aiPanelVisibleBeforeFullscreen = aiPanelVisible;
                                if (!aiPanelVisible) {
                                    toggleAICompanion();
                                }
                            } else {
                                // Restore AI visibility to what it was before entering fullscreen,
                                // but don't override any explicit user changes made while in fullscreen.
                                if (!aiPanelVisibleBeforeFullscreen && aiPanelVisible) {
                                    toggleAICompanion();
                                }
                            }
                        });

                        stage.setOnCloseRequest(e -> {
                            webEngine.load(null);
                        });
                    }
                });
            }
        });

        Platform.runLater(() -> {
            webView.setPrefHeight(-1);
            webEngine.load(HOME_URL);
            // Lazily show the AI Companion only when the user asks for it,
            // to avoid extra work on startup. The panel is still fully
            // initialized so it can open instantly on first use.
            initAICompanion();
        });
    }

    /**
     * Show or hide the loading overlay that covers the web content while a page is loading
     * and site tweaks (like header hiding) are being applied.
     */
    private void setLoadingOverlayVisible(boolean visible) {
        if (loadingOverlay == null) {
            return;
        }
        loadingOverlay.setVisible(visible);
        loadingOverlay.setManaged(visible);
    }

    /**
     * Apply small, domain-specific tweaks to the currently loaded page.
     * Runs after each successful load and is scoped by hostname.
     */
    private void applySiteTweaks() {
        if (webEngine == null) {
            return;
        }
        String location = webEngine.getLocation();
        if (location == null || location.isBlank()) {
            return;
        }

        URI uri;
        try {
            uri = URI.create(location);
        } catch (IllegalArgumentException e) {
            return;
        }

        String host = uri.getHost();
        if (host == null) {
            return;
        }

        if (host.endsWith("edinvn.com")) {
            hideEdinvnHeader();
        }
    }

    /**
     * Hide the tall header/navigation bar and footer on edinvn.com pages to free up vertical space.
     * Uses defensive DOM queries and is safe to run multiple times.
     */
    private void hideEdinvnHeader() {
        if (webEngine == null) {
            return;
        }

        String script = """
            (function() {
              try {
                // Hide main header / navbar
                var header =
                  document.querySelector('header.site-header, header#shopify-section-header, header') ||
                  document.querySelector('.site-header, .header, #shopify-section-header');
                if (header && !header.__browserfxHidden) {
                  header.__browserfxHidden = true;
                  header.style.display = 'none';

                  var next = header.nextElementSibling;
                  if (next && next.style) {
                    if (next.style.marginTop) {
                      next.style.marginTop = '0';
                    }
                    if (next.style.paddingTop) {
                      next.style.paddingTop = '0';
                    }
                  }
                }

                // Hide site footer
                var footer =
                  document.querySelector('footer.site-footer, footer#shopify-section-footer, footer') ||
                  document.querySelector('.site-footer, .footer, #shopify-section-footer');
                if (footer && !footer.__browserfxHidden) {
                  footer.__browserfxHidden = true;
                  footer.style.display = 'none';

                  var prev = footer.previousElementSibling;
                  if (prev && prev.style) {
                    if (prev.style.marginBottom) {
                      prev.style.marginBottom = '0';
                    }
                    if (prev.style.paddingBottom) {
                      prev.style.paddingBottom = '0';
                    }
                  }
                }
              } catch (e) {
                console && console.debug && console.debug('BrowserFX header/footer tweak failed', e);
              }
            })();
            """;

        try {
            webEngine.executeScript(script);
        } catch (Exception ignored) {
            // Ignore JavaScript errors; the page should continue to function normally.
        }
    }

    // 4.5 — address bar Enter handler
    @FXML
    private void onAddressBarAction() {
        String input = addressBar.getText().trim();
        if (!input.startsWith("http://") && !input.startsWith("https://")) {
            input = "https://" + input;
        }
        webEngine.load(input);
    }

    // 4.6
    @FXML
    private void onBackClick() {
        webEngine.getHistory().go(-1);
    }

    // 4.7
    @FXML
    private void onForwardClick() {
        webEngine.getHistory().go(1);
    }

    // 4.8
    @FXML
    private void onReloadClick() {
        webEngine.reload();
    }

    // 4.9
    @FXML
    private void onHomeClick() {
        webEngine.load(HOME_URL);
    }

    @FXML
    public void onFullscreenClick() {
        toggleFullscreen();
    }

    @FXML
    public void onAIClick() {
        toggleAICompanion();
    }

    private void toggleAICompanion() {
        aiPanelVisible = !aiPanelVisible;
        aiPanel.setVisible(aiPanelVisible);
        aiPanel.setManaged(aiPanelVisible);
        
        Platform.runLater(() -> {
            if (aiPanelVisible) {
                mainSplitPane.setDividerPositions(AI_PANEL_DIVIDER);
                if (aiCompanionController != null) {
                    aiCompanionController.refreshPageContent();
                }
            } else {
                mainSplitPane.setDividerPositions(1.0);
            }
        });
    }

    private void initAICompanion() {
        try {
            FXMLLoader aiLoader = new FXMLLoader(
                Objects.requireNonNull(
                    BrowserController.class.getResource("ai-companion-panel.fxml")
                )
            );
            VBox aiPanelContent = aiLoader.load();
            aiCompanionController = aiLoader.getController();
            aiCompanionController.initializeWithEngine(webEngine);
            aiCompanionController.setOnCloseCallback(this::toggleAICompanion);
            
            aiPanel.getChildren().add(aiPanelContent);
            VBox.setVgrow(aiPanelContent, javafx.scene.layout.Priority.ALWAYS);
        } catch (IOException e) {
            org.slf4j.LoggerFactory.getLogger(BrowserController.class)
                .error("Failed to load AI companion panel", e);
        }
    }

    private void toggleFullscreen() {
        Stage stage = (Stage) webView.getScene().getWindow();
        if (stage != null) {
            // Toggling the Stage's fullScreen property will trigger the
            // fullScreenProperty listener set up in initialize(), which is
            // responsible for updating the toolbar visibility and button text.
            stage.setFullScreen(!stage.isFullScreen());
        }
    }
}
