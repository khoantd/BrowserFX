package com.kdue.browserfx;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.animation.PauseTransition;
import javafx.util.Duration;

import java.text.Normalizer;
import java.util.List;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AICompanionController {

    private static final Logger LOGGER = LoggerFactory.getLogger(AICompanionController.class);

    @FXML private ListView<ChatEntry> chatListView;
    @FXML private TextField questionInput;
    @FXML private Button sendButton;
    @FXML private Button clearButton;
    @FXML private Button closeButton;
    @FXML private Button copyConversationButton;
    @FXML private Button summarizeBtn;
    @FXML private Button linksBtn;
    @FXML private Button contactBtn;
    @FXML private Button readingModeBtn;
    @FXML private Button helpChooseProductBtn;
    @FXML private Button purchaseAdviceBtn;
    @FXML private ProgressIndicator loadingIndicator;
    @FXML private Label statusLabel;
    @FXML private ComboBox<String> modelSelector;
    @FXML private VBox readingModePanel;
    @FXML private WebView readingModeWebView;
    @FXML private VBox virtualKeyboardPane;
    @FXML private GridPane virtualKeyboardGrid;
    @FXML private HBox keywordSuggestionsBox;
    @FXML private Button toggleKeyboardButton;
    @FXML private VBox inputSection;
    @FXML private VBox root;

    private WebEngine webEngine;
    private PageContentExtractor contentExtractor;
    private AIChatService chatService;
    private ConfigManager configManager;
    private String currentPageContent;
    private String currentPageTitle;
    /** Last page URL we extracted content for, to avoid redundant work. */
    private String currentPageUrl;
    private ObservableList<ChatEntry> messages = FXCollections.observableArrayList();
    private boolean initialized = false;
    private boolean autoSummaryShown = false;
    private double fontSize = 14.0;
    /** Content last shown in reading mode; used to reload with new font size on A+/A-. */
    private String lastReadingModeContent;

    private static final double MARKDOWN_VIEW_MAX_HEIGHT = 280;

    /** Idle timeout in seconds (1.3 minutes) before auto-resetting the AI session. */
    private static final double IDLE_RESET_SECONDS = 78;

    private PauseTransition idleResetTimer;

    @FXML
    public void initialize() {
        chatListView.setItems(messages);
        chatListView.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(ChatEntry item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else if (item instanceof ChatEntry.ProductCards cards) {
                    VBox productCardsBox = createProductCardsView(cards.products());
                    productCardsBox.setPrefWidth(320);
                    setGraphic(productCardsBox);
                    setText(null);
                } else if (item instanceof ChatEntry.SuggestionButtons suggestions) {
                    VBox suggestionsBox = createSuggestionButtonsView(suggestions.suggestions());
                    suggestionsBox.setPrefWidth(320);
                    setGraphic(suggestionsBox);
                    setText(null);
                } else {
                    javafx.scene.Node graphic = createMessageCell(item.getDisplayText());
                    setGraphic(graphic);
                    setText(null);
                }
            }
        });

        modelSelector.setItems(FXCollections.observableArrayList(ConfigManager.AVAILABLE_MODELS));
        buildVirtualKeyboard();
        if (keywordSuggestionsBox != null) {
            keywordSuggestionsBox.setVisible(false);
            keywordSuggestionsBox.setManaged(false);
        }
        // Update keyword suggestions when text or caret position changes.
        questionInput.textProperty().addListener((obs, oldText, newText) -> updateKeywordSuggestions());
        questionInput.caretPositionProperty().addListener((obs, oldPos, newPos) -> updateKeywordSuggestions());

        setupIdleResetTimer();
    }

    private static final String KEYBOARD_ROW_NUMBERS = "1234567890";
    private static final String KEYBOARD_ROW_1 = "qwertyuiop";
    private static final String KEYBOARD_ROW_2 = "asdfghjkl";
    private static final String KEYBOARD_ROW_3 = "zxcvbnm";

    /** Static contact info for De ED from https://edinvn.com/blogs/ve-de-ed/lien-he */
    private static final String DE_ED_CONTACT_INFO = """
            Công ty TNHH Thương Mại Đầu Tư De-ED
            Văn phòng: L17-11, Vincom Center, 72 Lê Thánh Tôn, Phường Sài Gòn, TPHCM.
            Điện thoại: (+84) 708 641 247 – (+84) 908 873 173
            Email: edcominvn@gmail.com
            Website: https://edinvn.com
            """;

    /** Simple list of common AI companion keywords for suggestion when typing (English + Vietnamese). */
    private static final List<String> KEYWORD_SUGGESTIONS = List.of(
            // English
            "summarize",
            "summary",
            "explain",
            "translate",
            "improve",
            "shorten",
            "expand",
            "fix grammar",
            "links",
            "contact",
            "reading mode",
            "find product",
            "find price",
            "find price low",
            "find price high",
            "find price average",
            "find price lowest",
            "find price highest",
            "find price average",
            "find price lowest",
            // Vietnamese (shown to user)
            "tóm tắt",
            "giải thích",
            "dịch",
            "cải thiện",
            "rút gọn",
            "mở rộng",
            "sửa ngữ pháp",
            "liên kết",
            "liên hệ",
            "chế độ đọc",
            "tìm sản phẩm",
            "tìm giá rẻ",
            "tìm giá cao",
            "tìm giá trung bình",
            "tìm giá thấp nhất",
            "tìm giá cao nhất",
            "tìm giá trung bình",
            "tìm giá thấp nhất",
            "tìm giá cao nhất",
            "rẻ nhất",
            "cao nhất",
            "trung bình",
            "thấp nhất"
    );

    private boolean virtualKeyboardShift = false;

    private Button shiftButtonRef; // for updating Shift visual after one-shot use

    private void buildVirtualKeyboard() {
        String[] letterRows = { KEYBOARD_ROW_NUMBERS, KEYBOARD_ROW_1, KEYBOARD_ROW_2, KEYBOARD_ROW_3 };
        int row = 0;
        for (String keys : letterRows) {
            for (int col = 0; col < keys.length(); col++) {
                String ch = String.valueOf(keys.charAt(col));
                Button keyBtn = keyButton(ch);
                virtualKeyboardGrid.add(keyBtn, col, row);
            }
            row++;
        }
        Button shiftBtn = keyButton("⇧");
        shiftButtonRef = shiftBtn;
        shiftBtn.setStyle("-fx-font-size: 11; -fx-min-width: 36; -fx-min-height: 28; -fx-background-color: #cbd5e0; -fx-background-radius: 4;");
        shiftBtn.setOnAction(e -> {
            virtualKeyboardShift = true;
            shiftBtn.setStyle("-fx-font-size: 11; -fx-min-width: 36; -fx-min-height: 28; -fx-background-color: #4299e1; -fx-text-fill: white; -fx-background-radius: 4;");
        });
        Button spaceBtn = keyButton(" ");
        spaceBtn.setMinWidth(120);
        spaceBtn.setStyle("-fx-font-size: 11; -fx-min-height: 28; -fx-background-color: #e2e8f0; -fx-background-radius: 4;");
        Button backspaceBtn = keyButton("⌫");
        backspaceBtn.setStyle("-fx-font-size: 12; -fx-min-width: 44; -fx-min-height: 28; -fx-background-color: #fc8181; -fx-text-fill: white; -fx-background-radius: 4;");
        backspaceBtn.setOnAction(e -> backspaceInQuestionInput());
        virtualKeyboardGrid.add(shiftBtn, 0, row);
        virtualKeyboardGrid.add(spaceBtn, 1, row);
        GridPane.setColumnSpan(spaceBtn, 6);
        virtualKeyboardGrid.add(backspaceBtn, 7, row);
        GridPane.setColumnSpan(backspaceBtn, 2);
    }

    private void updateShiftButtonStyle() {
        if (shiftButtonRef != null) {
            shiftButtonRef.setStyle(virtualKeyboardShift
                ? "-fx-font-size: 11; -fx-min-width: 36; -fx-min-height: 28; -fx-background-color: #4299e1; -fx-text-fill: white; -fx-background-radius: 4;"
                : "-fx-font-size: 11; -fx-min-width: 36; -fx-min-height: 28; -fx-background-color: #cbd5e0; -fx-background-radius: 4;");
        }
    }

    private Button keyButton(String ch) {
        Button btn = new Button(ch);
        btn.setFocusTraversable(false);
        btn.setStyle("-fx-font-size: 11; -fx-min-width: 28; -fx-min-height: 28; -fx-background-color: #e2e8f0; -fx-background-radius: 4;");
        if (ch.length() == 1 && ch.charAt(0) != ' ') {
            final String character = ch;
            btn.setOnAction(e -> {
                String toInsert = character;
                if (KEYBOARD_ROW_1.contains(character) || KEYBOARD_ROW_2.contains(character) || KEYBOARD_ROW_3.contains(character)) {
                    toInsert = virtualKeyboardShift ? character.toUpperCase() : character;
                    virtualKeyboardShift = false;
                    updateShiftButtonStyle();
                }
                insertIntoQuestionInput(toInsert);
            });
        } else if (ch.equals(" ")) {
            btn.setOnAction(e -> insertIntoQuestionInput(" "));
        }
        return btn;
    }

    /** Insert at caret or replace current selection, like a physical keyboard. */
    private void insertIntoQuestionInput(String s) {
        questionInput.requestFocus();
        questionInput.replaceSelection(s);
    }

    /** Delete selection or one character before caret, like physical Backspace. */
    private void backspaceInQuestionInput() {
        questionInput.requestFocus();
        int anchor = questionInput.getAnchor();
        int caret = questionInput.getCaretPosition();
        if (anchor != caret) {
            questionInput.replaceSelection("");
        } else if (caret > 0) {
            questionInput.selectRange(caret - 1, caret);
            questionInput.replaceSelection("");
        }
    }

    /** Hide keyword suggestions bar. */
    private void hideKeywordSuggestions() {
        if (keywordSuggestionsBox == null) return;
        keywordSuggestionsBox.getChildren().clear();
        keywordSuggestionsBox.setVisible(false);
        keywordSuggestionsBox.setManaged(false);
    }

    /** Update keyword suggestions based on the current word at the caret when using the virtual keyboard. */
    private void updateKeywordSuggestions() {
        if (keywordSuggestionsBox == null || questionInput == null) return;

        // Only show suggestions when virtual keyboard is visible.
        if (!virtualKeyboardGrid.isVisible()) {
            hideKeywordSuggestions();
            return;
        }

        String text = questionInput.getText();
        if (text == null || text.isBlank()) {
            hideKeywordSuggestions();
            return;
        }

        int caret = questionInput.getCaretPosition();
        if (caret < 0) caret = 0;
        if (caret > text.length()) caret = text.length();

        int start = caret;
        while (start > 0 && !Character.isWhitespace(text.charAt(start - 1))) {
            start--;
        }
        int end = caret;
        while (end < text.length() && !Character.isWhitespace(text.charAt(end))) {
            end++;
        }

        if (end <= start) {
            hideKeywordSuggestions();
            return;
        }

        String currentWord = text.substring(start, end);
        if (currentWord.length() < 2) {
            hideKeywordSuggestions();
            return;
        }

        String normalizedWord = normalizeForMatch(currentWord);
        List<String> matches = new ArrayList<>();
        for (String keyword : KEYWORD_SUGGESTIONS) {
            String normalizedKeyword = normalizeForMatch(keyword);
            if (normalizedKeyword.startsWith(normalizedWord) && !normalizedKeyword.equals(normalizedWord)) {
                matches.add(keyword);
                if (matches.size() >= 5) break;
            }
        }

        if (matches.isEmpty()) {
            hideKeywordSuggestions();
            return;
        }

        keywordSuggestionsBox.getChildren().clear();
        final int wordStart = start;
        final int wordEnd = end;
        for (String match : matches) {
            Button btn = new Button(match);
            btn.setFocusTraversable(false);
            btn.setStyle("-fx-font-size: 10; -fx-padding: 2 6; -fx-background-color: #e2e8f0; -fx-background-radius: 12;");
            btn.setOnAction(e -> replaceCurrentWord(wordStart, wordEnd, match));
            keywordSuggestionsBox.getChildren().add(btn);
        }

        keywordSuggestionsBox.setVisible(true);
        keywordSuggestionsBox.setManaged(true);
    }

    /** Replace the word at the given range with the chosen suggestion and keep caret after it. */
    private void replaceCurrentWord(int start, int end, String replacement) {
        if (questionInput == null) return;
        questionInput.requestFocus();
        questionInput.selectRange(start, end);
        // Add a trailing space so the user can immediately type the next word.
        questionInput.replaceSelection(replacement + " ");
        // Trigger a refresh of suggestions (likely hiding them if the word is now complete).
        updateKeywordSuggestions();
    }

    /** Normalize text for keyword matching: lowercase + remove Vietnamese diacritics. */
    private static String normalizeForMatch(String s) {
        if (s == null) return "";
        String normalized = Normalizer.normalize(s, Normalizer.Form.NFD);
        // Strip combining diacritical marks so "tóm tắt" matches "tom tat".
        normalized = normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return normalized.toLowerCase();
    }

    /**
     * Format raw AI text for easier reading in the chat view.
     * Currently:
     * - moves URLs / markdown links onto their own line after a colon.
     */
    private static String formatAiTextForReadability(String text) {
        if (text == null || text.isBlank()) return text;
        String formatted = text;
        // Example: "tại đây: https://example.com" -> "tại đây:\n\nhttps://example.com"
        formatted = formatted.replaceAll(":(?= https?://)", ":\n\n");
        // Example: "tại đây: [Link](https://example.com)" -> "tại đây:\n\n[Link](https://example.com)"
        formatted = formatted.replaceAll(":(?= \\[)", ":\n\n");
        // Fallback: if there's still a bare URL following a space, move it to next line.
        formatted = formatted.replaceAll(" (?=https?://)", "\n");
        return formatted;
    }

    @FXML
    public void onToggleVirtualKeyboard() {
        boolean visible = virtualKeyboardGrid.isVisible();
        virtualKeyboardGrid.setVisible(!visible);
        virtualKeyboardGrid.setManaged(!visible);
        toggleKeyboardButton.setText(visible ? "Show keyboard" : "Hide keyboard");
        if (!virtualKeyboardGrid.isVisible()) {
            hideKeywordSuggestions();
        } else {
            updateKeywordSuggestions();
        }
    }

    private VBox createProductCardsView(List<ProductInfo> products) {
        VBox container = new VBox(8);
        container.setStyle("-fx-padding: 6; -fx-background-color: #f0f4f8; -fx-background-radius: 6;");
        for (ProductInfo p : products) {
            HBox card = new HBox(8);
            card.setStyle("-fx-padding: 6; -fx-background-color: white; -fx-background-radius: 4; -fx-cursor: hand;");
            card.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

            StackPane thumbContainer = new StackPane();
            thumbContainer.setMinSize(56, 56);
            thumbContainer.setMaxSize(56, 56);
            String imgUrl = (p.imageUrl() != null && !p.imageUrl().isBlank()) ? p.imageUrl().trim() : null;
            if (imgUrl != null) {
                ImageView thumb = new ImageView();
                thumb.setFitWidth(56);
                thumb.setFitHeight(56);
                thumb.setPreserveRatio(true);
                Image img = new Image(imgUrl, true);
                thumb.setImage(img);
                img.errorProperty().addListener((o, prev, err) -> {
                    if (Boolean.TRUE.equals(err)) {
                        Platform.runLater(() -> thumbContainer.getChildren().setAll(createThumbPlaceholder()));
                    }
                });
                thumbContainer.getChildren().setAll(thumb);
            } else {
                thumbContainer.getChildren().setAll(createThumbPlaceholder());
            }

            VBox info = new VBox(2);
            Label titleLabel = new Label(truncate(p.title(), 40));
            titleLabel.setStyle("-fx-font-size: 12; -fx-font-weight: bold; -fx-wrap-text: true;");
            titleLabel.setMaxWidth(220);
            String priceDisplay = p.price().isBlank() ? "Chưa có giá" : p.price();
            Label priceLabel = new Label("Giá: " + priceDisplay);
            priceLabel.setStyle("-fx-font-size: 11; -fx-text-fill: #2d3748;");
            Hyperlink viewLink = new Hyperlink("Xem sản phẩm");
            viewLink.setStyle("-fx-font-size: 10;");
            viewLink.setOnAction(e -> openInBrowser(p.url()));
            String addToCartText = (p.addToCartLabel() != null && !p.addToCartLabel().isBlank()) ? p.addToCartLabel() : "Thêm vào giỏ";
            Hyperlink addToCartLink = new Hyperlink(addToCartText);
            addToCartLink.setStyle("-fx-font-size: 10;");
            addToCartLink.setOnAction(e -> openInBrowser(p.url()));
            VBox linksBox = new VBox(2, viewLink, addToCartLink);
            linksBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            info.getChildren().addAll(titleLabel, priceLabel, linksBox);

            card.getChildren().addAll(thumbContainer, info);
            card.setOnMouseClicked(e -> {
                if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 1) {
                    openInBrowser(p.url());
                }
            });
            container.getChildren().add(card);
        }
        return container;
    }

    private String truncate(String s, int maxLen) {
        if (s == null || s.length() <= maxLen) return s == null ? "" : s;
        return s.substring(0, maxLen) + "…";
    }

    /** Placeholder when product has no image URL or image failed to load. */
    private javafx.scene.Node createThumbPlaceholder() {
        VBox box = new VBox();
        box.setMinSize(56, 56);
        box.setMaxSize(56, 56);
        box.setAlignment(javafx.geometry.Pos.CENTER);
        box.setStyle("-fx-background-color: #e2e8f0; -fx-background-radius: 4;");
        Label lbl = new Label("Không có ảnh");
        lbl.setStyle("-fx-font-size: 9; -fx-text-fill: #64748b; -fx-wrap-text: true;");
        box.getChildren().add(lbl);
        return box;
    }

    private void openInBrowser(String url) {
        if (webEngine != null && url != null && !url.isBlank()) {
            Platform.runLater(() -> webEngine.load(url));
        }
    }

    /**
     * Build a compact "AI" bubble that contains follow-up suggestion buttons
     * similar to quick actions. Clicking a button sends that question immediately.
     */
    private VBox createSuggestionButtonsView(List<String> suggestions) {
        VBox box = new VBox(4);
        box.setPrefWidth(320);
        box.setStyle("-fx-padding: 6 8; -fx-background-radius: 8; -fx-background-color: #f7fafc;");

        Label titleLabel = new Label("AI");
        titleLabel.setStyle("-fx-font-size: 12; -fx-font-weight: bold; -fx-text-fill: #2f855a;");

        Label subtitleLabel = new Label("Thử hỏi:");
        subtitleLabel.setStyle("-fx-font-size: 11; -fx-text-fill: #4a5568;");

        FlowPane buttonsPane = new FlowPane(4, 4);
        buttonsPane.setPrefWrapLength(300);

        for (String suggestion : suggestions) {
            if (suggestion == null || suggestion.isBlank()) continue;
            Button btn = new Button(suggestion);
            btn.setFocusTraversable(false);
            btn.getStyleClass().add("ai-quick-action-button");
            btn.setOnAction(e -> handleSuggestionClick(suggestion));
            buttonsPane.getChildren().add(btn);
        }

        if (buttonsPane.getChildren().isEmpty()) {
            // Fallback: nothing to show.
            box.getChildren().add(titleLabel);
            return box;
        }

        Button copyBtn = new Button("📋 Copy");
        copyBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #4299e1; -fx-font-size: 10;");
        copyBtn.setOnAction(e -> {
            String joined = String.join("\n", suggestions);
            copyToClipboard("Thử hỏi:\n" + joined);
        });

        box.getChildren().addAll(titleLabel, subtitleLabel, buttonsPane, copyBtn);
        return box;
    }

    /**
     * Handle click on a follow-up suggestion button.
     * - For plain questions, send immediately.
     * - For questions that contain placeholders (e.g. "X" or "[tên sản phẩm]"),
     *   just prefill the input so the user can edit and submit.
     */
    private void handleSuggestionClick(String question) {
        if (!initialized || question == null || question.isBlank()) {
            return;
        }
        questionInput.setText(question);
        questionInput.requestFocus();
        questionInput.positionCaret(question.length());

        boolean requiresUserInput = question.contains("X") || question.contains("[");
        if (!requiresUserInput) {
            onSendQuestion();
        }
    }

    /** Result of parsing sender prefix; null label and prefixLen 0 means use TextFlow for full text. */
    private record MessagePrefix(String label, int prefixLength, boolean useMarkdown, boolean showCopy) {}

    private MessagePrefix parseMessagePrefix(String text) {
        if (text == null || text.isEmpty()) return new MessagePrefix(null, 0, false, false);
        if (text.startsWith("Summary:")) return new MessagePrefix("Summary:", 8, true, false);
        if (text.startsWith("You:")) return new MessagePrefix("You:", 4, false, false);
        if (text.startsWith("AI:")) return new MessagePrefix("AI:", 3, true, true);
        if (text.startsWith("📎 Links:")) return new MessagePrefix("📎 Links:", 9, true, true);
        if (text.startsWith("📧 Contacts:")) return new MessagePrefix(null, 0, false, false);
        if (text.startsWith("AI") && text.length() >= 2) return new MessagePrefix("AI", 2, true, true);
        if (text.startsWith("You") && text.length() >= 3) return new MessagePrefix("You", 3, false, false);
        return new MessagePrefix(null, 0, false, false);
    }

    /** Returns either a WebView-based cell (for AI/Summary markdown) or a TextFlow (otherwise). */
    private javafx.scene.Node createMessageCell(String text) {
        MessagePrefix prefix = parseMessagePrefix(text);
        if (!prefix.useMarkdown()) {
            TextFlow flow = createTextFlow(text);
            flow.setPrefWidth(320);
            VBox bubble = new VBox(flow);
            bubble.setStyle("-fx-padding: 6 8; -fx-background-radius: 8; -fx-background-color: #edf2f7;");
            return bubble;
        }
        String rawBody = text.substring(prefix.prefixLength());
        final String body = rawBody.startsWith(" ") ? rawBody.substring(1) : rawBody;
        String html = MarkdownRenderer.toHtmlDocument(body);
        WebView webView = new WebView();
        webView.setPrefWidth(320);
        webView.setMaxHeight(MARKDOWN_VIEW_MAX_HEIGHT);
        webView.setContextMenuEnabled(false);
        webView.getProperties().put("markdownContent", html);
        webView.getEngine().loadContent(html, "text/html");
        webView.getEngine().locationProperty().addListener((obs, oldLoc, newLoc) -> {
            if (newLoc != null && (newLoc.startsWith("http://") || newLoc.startsWith("https://"))) {
                openInBrowser(newLoc);
                String content = (String) webView.getProperties().get("markdownContent");
                if (content != null) {
                    webView.getEngine().loadContent(content, "text/html");
                }
            }
        });
        VBox box = new VBox(4);
        box.setPrefWidth(320);
        box.setStyle("-fx-padding: 6 8; -fx-background-radius: 8; -fx-background-color: #f7fafc;");
        Label prefixLabel = new Label(prefix.label());
        String labelColor = prefix.label().startsWith("Summary") ? "#805ad5" : "#2f855a";
        prefixLabel.setStyle("-fx-font-size: 12; -fx-font-weight: bold; -fx-text-fill: " + labelColor + ";");
        box.getChildren().add(prefixLabel);
        box.getChildren().add(webView);
        if (prefix.showCopy()) {
            Button copyBtn = new Button("📋 Copy");
            copyBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #4299e1; -fx-font-size: 10;");
            copyBtn.setOnAction(e -> copyToClipboard(body));
            box.getChildren().add(copyBtn);
        }
        return box;
    }

    private TextFlow createTextFlow(String text) {
        TextFlow flow = new TextFlow();
        if (text.startsWith("You:")) {
            Text label = new Text(text.substring(0, 4));
            label.setFont(Font.font(12));
            label.setStyle("-fx-font-weight: bold; -fx-fill: #2b6cb0;");
            flow.getChildren().add(label);
            
            String rest = text.substring(4);
            Text content = new Text(rest);
            content.setFont(Font.font(12));
            flow.getChildren().add(content);
        } else if (text.startsWith("AI:")) {
            Text label = new Text(text.substring(0, 3));
            label.setFont(Font.font(12));
            label.setStyle("-fx-font-weight: bold; -fx-fill: #2f855a;");
            flow.getChildren().add(label);
            
            String rest = text.substring(3);
            Text content = new Text(rest);
            content.setFont(Font.font(12));
            flow.getChildren().add(content);
            
            Button copyBtn = new Button("📋 Copy");
            copyBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #4299e1; -fx-font-size: 10;");
            copyBtn.setOnAction(e -> copyToClipboard(rest));
            flow.getChildren().add(copyBtn);
        } else if (text.startsWith("Summary:")) {
            Text label = new Text(text.substring(0, 8));
            label.setFont(Font.font(12));
            label.setStyle("-fx-font-weight: bold; -fx-fill: #805ad5;");
            flow.getChildren().add(label);
            
            String rest = text.substring(8);
            Text content = new Text(rest);
            content.setFont(Font.font(12));
            flow.getChildren().add(content);
        } else if (text.startsWith("📎 Links:") || text.startsWith("📧 Contacts:")) {
            Text content = new Text(text);
            content.setFont(Font.font(11));
            flow.getChildren().add(content);
        } else {
            Text content = new Text(text);
            content.setFont(Font.font(12));
            flow.getChildren().add(content);
        }
        return flow;
    }

    private void copyToClipboard(String text) {
        Clipboard clipboard = Clipboard.getSystemClipboard();
        ClipboardContent content = new ClipboardContent();
        content.putString(text);
        clipboard.setContent(content);
        showTemporaryMessage("Copied!");
    }

    public void initializeWithEngine(WebEngine webEngine) {
        this.webEngine = webEngine;
        this.contentExtractor = new PageContentExtractor(webEngine);
        this.configManager = new ConfigManager();
        this.chatService = new AIChatService(configManager);
        this.initialized = true;

        String savedModel = configManager.getLitellmModel();
        modelSelector.getSelectionModel().select(savedModel);
        chatService.setModel(savedModel);

        Platform.runLater(() -> {
            statusLabel.setText("Ready. Configure API key in " + ConfigManager.getConfigDirDescription());
        });
    }

    @FXML
    public void onModelChanged() {
        String selectedModel = modelSelector.getSelectionModel().getSelectedItem();
        if (selectedModel != null) {
            chatService.setModel(selectedModel);
            configManager.setLitellmModel(selectedModel);
            configManager.save();
            showTemporaryMessage("Model: " + selectedModel);
        }
    }

    @FXML
    public void onClearChat() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Clear Chat");
        alert.setHeaderText("Clear all messages?");
        alert.setContentText("This action cannot be undone.");
        
        alert.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                messages.clear();
                autoSummaryShown = false;
            }
        });
    }

    @FXML
    public void onCopyConversation() {
        if (messages == null || messages.isEmpty()) {
            showTemporaryMessage("No conversation to copy");
            return;
        }

        StringBuilder sb = new StringBuilder();
        for (ChatEntry entry : messages) {
            if (entry instanceof ChatEntry.TextMessage tm) {
                String text = tm.displayText();
                if (text != null && !text.isBlank()) {
                    sb.append(text.trim()).append("\n\n");
                }
            } else if (entry instanceof ChatEntry.ProductCards cards) {
                List<ProductInfo> products = cards.products();
                if (products != null && !products.isEmpty()) {
                    sb.append("Products:\n");
                    for (ProductInfo p : products) {
                        String title = p.title() != null && !p.title().isBlank() ? p.title().trim() : "Product";
                        String price = p.price() != null && !p.price().isBlank() ? (" — " + p.price().trim()) : "";
                        String url = p.url() != null && !p.url().isBlank() ? (" (" + p.url().trim() + ")") : "";
                        sb.append("- ").append(title).append(price).append(url).append("\n");
                    }
                    sb.append("\n");
                }
            } else if (entry instanceof ChatEntry.SuggestionButtons buttons) {
                List<String> suggestions = buttons.suggestions();
                if (suggestions != null && !suggestions.isEmpty()) {
                    sb.append("Suggestions:\n");
                    for (String s : suggestions) {
                        if (s != null && !s.isBlank()) {
                            sb.append("- ").append(s.trim()).append("\n");
                        }
                    }
                    sb.append("\n");
                }
            }
        }

        String conversation = sb.toString().trim();
        if (conversation.isEmpty()) {
            showTemporaryMessage("No conversation to copy");
        } else {
            copyToClipboard(conversation);
        }
    }

    @FXML
    public void onSendQuestion() {
        if (!initialized) return;

        String question = questionInput.getText().trim();
        if (question.isEmpty()) {
            return;
        }

        if (!configManager.isConfigured()) {
            addMessage("AI", "Please configure your LiteLLM API key in:\n" + configManager.getConfigPath());
            return;
        }

        questionInput.clear();
        addMessage("You", question);
        setInteractionEnabled(false);
        executeQuestion(question, AIChatService.PromptType.QUESTION);
    }

    @FXML
    public void onSummarize() {
        if (!initialized || !configManager.isConfigured()) {
            showConfigError();
            return;
        }

        addMessage("You", "📝 Summarize this page");
        setInteractionEnabled(false);
        loadingIndicator.setVisible(true);
        loadingIndicator.setManaged(true);
        statusLabel.setText("Summarizing...");

        String content = extractContentSafe();
        chatService.sendMessage(content, "", AIChatService.PromptType.SUMMARIZE, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Platform.runLater(() -> handleError(e));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    if (!response.isSuccessful()) {
                        Platform.runLater(() -> addMessage("AI", "Error: " + response));
                        return;
                    }
                    String body = response.body() != null ? response.body().string() : "";
                    String parsedResult = chatService.parseAIResponse(body);
                    String formattedResult = formatAiTextForReadability(parsedResult);
                    Platform.runLater(() -> {
                        addMessage("Summary:", formattedResult);
                        showSuggestedQuestions();
                    });
                } finally {
                    response.close();
                    Platform.runLater(() -> {
                        loadingIndicator.setVisible(false);
                        setInteractionEnabled(true);
                    });
                }
            }
        });
    }

    @FXML
    public void onExtractLinks() {
        if (!initialized || !configManager.isConfigured()) {
            showConfigError();
            return;
        }

        addMessage("You", "🔗 Extract all links");
        setInteractionEnabled(false);
        loadingIndicator.setVisible(true);
        statusLabel.setText("Extracting links...");

        String content = extractContentWithLinksSafe();
        
        new Thread(() -> {
            try {
                String links = extractLinksFromContent(content);
                Platform.runLater(() -> {
                    addMessage("📎 Links:", "\n" + linksListToMarkdown(links));
                    loadingIndicator.setVisible(false);
                    statusLabel.setText("Found links");
                    setInteractionEnabled(true);
                });
            } catch (Exception e) {
                Platform.runLater(() -> handleError(e));
            }
        }).start();
    }

    @FXML
    public void onExtractContact() {
        if (!initialized) {
            return;
        }

        addMessage("You", "📧 Thông tin liên hệ");
        setInteractionEnabled(false);
        loadingIndicator.setVisible(true);
        loadingIndicator.setManaged(true);
        statusLabel.setText("Đang hiển thị thông tin liên hệ...");

        Platform.runLater(() -> {
            addMessage("📧 Contacts:", "\n" + DE_ED_CONTACT_INFO);
            loadingIndicator.setVisible(false);
            loadingIndicator.setManaged(false);
            statusLabel.setText("Đã hiển thị thông tin liên hệ De ED");
            setInteractionEnabled(true);
        });
    }

    @FXML
    public void onHelpChooseProduct() {
        if (!initialized || !configManager.isConfigured()) {
            showConfigError();
            return;
        }

        addMessage("You", "🛒 Help me choose the right product");
        setInteractionEnabled(false);
        loadingIndicator.setVisible(true);
        loadingIndicator.setManaged(true);
        statusLabel.setText("Comparing products...");

        String content = appendProductInfoToContent(extractContentWithLinksSafe());
        String userContext = questionInput.getText().trim();
        if (!userContext.isEmpty()) {
            questionInput.clear();
        }
        chatService.sendMessage(content, userContext.isEmpty() ? "Compare options and recommend the best fit for a typical buyer." : userContext, AIChatService.PromptType.HELP_CHOOSE_PRODUCT, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Platform.runLater(() -> handleError(e));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    if (!response.isSuccessful()) {
                        Platform.runLater(() -> addMessage("AI", "Error: " + response));
                        return;
                    }
                    String body = response.body() != null ? response.body().string() : "";
                    String result = chatService.parseAIResponse(body);
                    result = formatAiTextForReadability(result);
                    List<ProductInfo> cardProducts = productsForResponse(result);
                    String finalText = appendProductsMarkdownToResponse(result);
                    Platform.runLater(() -> {
                        addMessage("AI", finalText);
                        if (!cardProducts.isEmpty()) {
                            messages.add(new ChatEntry.ProductCards(cardProducts));
                            chatListView.scrollTo(messages.size() - 1);
                        }
                        showProductSuggestedQuestions();
                    });
                } finally {
                    response.close();
                    Platform.runLater(() -> {
                        loadingIndicator.setVisible(false);
                        statusLabel.setText("Ready");
                        setInteractionEnabled(true);
                    });
                }
            }
        });
    }

    @FXML
    public void onPurchaseAdvice() {
        if (!initialized || !configManager.isConfigured()) {
            showConfigError();
            return;
        }

        addMessage("You", "💰 Should I buy? Give me purchase advice");
        setInteractionEnabled(false);
        loadingIndicator.setVisible(true);
        loadingIndicator.setManaged(true);
        statusLabel.setText("Getting purchase advice...");

        String content = appendProductInfoToContent(extractContentWithLinksSafe());
        String userContext = questionInput.getText().trim();
        if (!userContext.isEmpty()) {
            questionInput.clear();
        }
        chatService.sendMessage(content, userContext.isEmpty() ? "Give honest purchase advice: value, any real urgency, and whether this is a good buy." : userContext, AIChatService.PromptType.PURCHASE_ADVICE, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Platform.runLater(() -> handleError(e));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    if (!response.isSuccessful()) {
                        Platform.runLater(() -> addMessage("AI", "Error: " + response));
                        return;
                    }
                    String body = response.body() != null ? response.body().string() : "";
                    String result = chatService.parseAIResponse(body);
                    result = formatAiTextForReadability(result);
                    List<ProductInfo> cardProducts = productsForResponse(result);
                    String finalText = appendProductsMarkdownToResponse(result);
                    Platform.runLater(() -> {
                        addMessage("AI", finalText);
                        if (!cardProducts.isEmpty()) {
                            messages.add(new ChatEntry.ProductCards(cardProducts));
                            chatListView.scrollTo(messages.size() - 1);
                        }
                        showProductSuggestedQuestions();
                    });
                } finally {
                    response.close();
                    Platform.runLater(() -> {
                        loadingIndicator.setVisible(false);
                        statusLabel.setText("Ready");
                        setInteractionEnabled(true);
                    });
                }
            }
        });
    }

    @FXML
    public void onReadingMode() {
        if (!initialized) return;

        setInteractionEnabled(false);
        loadingIndicator.setVisible(true);
        statusLabel.setText("Loading reading mode...");

        String content = extractContentSafe();
        chatService.sendMessage(content, "", AIChatService.PromptType.READING_MODE, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Platform.runLater(() -> handleError(e));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    if (!response.isSuccessful()) {
                        Platform.runLater(() -> showError("Error: " + response));
                        return;
                    }
                    String body = response.body() != null ? response.body().string() : "";
                    String cleanText = chatService.parseAIResponse(body);
                    Platform.runLater(() -> {
                        showReadingMode(cleanText);
                    });
                } finally {
                    response.close();
                    Platform.runLater(() -> {
                        loadingIndicator.setVisible(false);
                        setInteractionEnabled(true);
                    });
                }
            }
        });
    }

    private void showReadingMode(String content) {
        readingModePanel.setVisible(true);
        readingModePanel.setManaged(true);
        chatListView.setVisible(false);
        chatListView.setManaged(false);

        lastReadingModeContent = content;
        String html = MarkdownRenderer.toHtmlDocument(content, getReadingModeCss());
        readingModeWebView.getEngine().loadContent(html, "text/html");
        readingModeWebView.setContextMenuEnabled(false);
        readingModeWebView.getEngine().locationProperty().addListener((obs, oldLoc, newLoc) -> {
            if (newLoc != null && (newLoc.startsWith("http://") || newLoc.startsWith("https://"))) {
                openInBrowser(newLoc);
                if (lastReadingModeContent != null) {
                    readingModeWebView.getEngine().loadContent(
                            MarkdownRenderer.toHtmlDocument(lastReadingModeContent, getReadingModeCss()), "text/html");
                }
            }
        });
        statusLabel.setText("Reading Mode - Use A+/A- to resize");
    }

    private String getReadingModeCss() {
        return """
            body {
                font-size: %dpx;
                max-width: 42em;
                margin: 0 auto;
                padding: 1em 12px;
                line-height: 1.65;
                font-family: Georgia, "Times New Roman", serif;
                color: #1a1a1a;
            }
            h1, h2, h3, h4 { margin: 1em 0 0.5em; font-weight: 600; color: #111; }
            p { margin: 0.75em 0; }
            ul, ol { margin: 0.75em 0; }
            """.formatted((int) fontSize);
    }

    @FXML
    public void onCloseReadingMode() {
        readingModePanel.setVisible(false);
        readingModePanel.setManaged(false);
        chatListView.setVisible(true);
        chatListView.setManaged(true);
        statusLabel.setText("Ready");
    }

    @FXML
    public void onIncreaseFont() {
        fontSize = Math.min(fontSize + 2, 28);
        updateReadingFont();
    }

    @FXML
    public void onDecreaseFont() {
        fontSize = Math.max(fontSize - 2, 8);
        updateReadingFont();
    }

    private void updateReadingFont() {
        if (!readingModePanel.isVisible() || lastReadingModeContent == null) return;
        readingModeWebView.getEngine().loadContent(
                MarkdownRenderer.toHtmlDocument(lastReadingModeContent, getReadingModeCss()), "text/html");
    }

    @FXML
    public void onClose() {
        if (onCloseCallback != null) {
            onCloseCallback.run();
        }
    }

    public void refreshPageContent() {
        if (!initialized || webEngine == null) return;

        Platform.runLater(() -> {
            String pageUrl = contentExtractor.getPageUrl();
            // Skip re-extracting if we already have content for this URL.
            if (pageUrl != null
                    && pageUrl.equals(currentPageUrl)
                    && currentPageContent != null
                    && !currentPageContent.isBlank()) {
                statusLabel.setText("Page loaded: " + currentPageTitle);
                return;
            }

            loadingIndicator.setVisible(true);
            statusLabel.setText("Reading page...");

            try {
                currentPageContent = contentExtractor.extractContent();
                currentPageTitle = contentExtractor.getPageTitle();
                currentPageUrl = pageUrl;
                loadingIndicator.setVisible(false);
                if (currentPageContent != null && !currentPageContent.isBlank()) {
                    statusLabel.setText("Page loaded: " + currentPageTitle);
                    if (!autoSummaryShown) {
                        autoSummaryShown = true;
                    }
                } else {
                    statusLabel.setText("No page content available");
                }
            } catch (Exception e) {
                LOGGER.error("Failed to extract page content", e);
                loadingIndicator.setVisible(false);
                statusLabel.setText("Error reading page");
            }
        });
    }

    private void executeQuestion(String question, AIChatService.PromptType promptType) {
        loadingIndicator.setVisible(true);
        loadingIndicator.setManaged(true);
        statusLabel.setText("Getting answer...");

        String content = appendProductInfoToContent(extractContentWithLinksSafe());
        chatService.sendMessage(content, question, promptType, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Platform.runLater(() -> handleError(e));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    if (!response.isSuccessful()) {
                        Platform.runLater(() -> addMessage("AI", "Error: " + response));
                        return;
                    }
                    String body = response.body() != null ? response.body().string() : "";
                    String result = chatService.parseAIResponse(body);
                    result = formatAiTextForReadability(result);
                    List<ProductInfo> cardProducts = productsForResponse(result);
                    String finalText = appendProductsMarkdownToResponse(result);
                    Platform.runLater(() -> {
                        addMessage("AI", finalText);
                        if (!cardProducts.isEmpty()) {
                            messages.add(new ChatEntry.ProductCards(cardProducts));
                            chatListView.scrollTo(messages.size() - 1);
                        }
                    });
                } finally {
                    response.close();
                    Platform.runLater(() -> {
                        loadingIndicator.setVisible(false);
                        statusLabel.setText("Ready");
                        setInteractionEnabled(true);
                    });
                }
            }
        });
    }

    /**
     * Enable/disable primary interaction controls while a request is in flight.
     * Keeps quick actions usable only when the assistant is ready.
     */
    private void setInteractionEnabled(boolean enabled) {
        if (questionInput != null) {
            questionInput.setDisable(!enabled);
        }
        if (sendButton != null) {
            sendButton.setDisable(!enabled);
        }
        if (summarizeBtn != null) {
            summarizeBtn.setDisable(!enabled);
        }
        if (linksBtn != null) {
            linksBtn.setDisable(!enabled);
        }
        if (contactBtn != null) {
            contactBtn.setDisable(!enabled);
        }
        if (readingModeBtn != null) {
            readingModeBtn.setDisable(!enabled);
        }
        if (helpChooseProductBtn != null) {
            helpChooseProductBtn.setDisable(!enabled);
        }
        if (purchaseAdviceBtn != null) {
            purchaseAdviceBtn.setDisable(!enabled);
        }
    }

    private String extractContentSafe() {
        try {
            return contentExtractor.extractContent();
        } catch (Exception e) {
            LOGGER.error("Failed to extract content", e);
            return "";
        }
    }

    private String extractContentWithLinksSafe() {
        try {
            return contentExtractor.extractContentWithLinks();
        } catch (Exception e) {
            LOGGER.error("Failed to extract content with links", e);
            return extractContentSafe();
        }
    }

    /**
     * Appends a "Products on page" section (title | URL | price) to content when product cards
     * are available, so the AI receives explicit product info for help-choose and purchase-advice flows.
     */
    private String appendProductInfoToContent(String content) {
        if (content == null) content = "";
        try {
            List<ProductInfo> products = contentExtractor.extractProducts();
            if (products == null || products.isEmpty()) return content;
            StringBuilder sb = new StringBuilder(content);
            sb.append("\n\nProducts on page (title | URL | price):\n");
            for (ProductInfo p : products) {
                sb.append(p.title()).append(" | ").append(p.url()).append(" | ").append(p.price());
                if (p.addToCartLabel() != null && !p.addToCartLabel().isBlank()) {
                    sb.append(" | Add to cart: ").append(p.addToCartLabel());
                }
                sb.append("\n");
            }
            return sb.toString();
        } catch (Exception e) {
            LOGGER.debug("Could not append product info to content", e);
            return content;
        }
    }

    private String extractLinksFromContent(String content) {
        if (content == null || content.isBlank()) {
            return "No content available";
        }

        // Prefer parsing the "Additional links found on page" section (linkText: url lines)
        int sectionIdx = content.indexOf("Additional links found on page:");
        if (sectionIdx != -1) {
            String linksSection = content.substring(sectionIdx + "Additional links found on page:".length()).trim();
            String[] lines = linksSection.split("\n");
            StringBuilder links = new StringBuilder();
            int count = 0;
            for (String line : lines) {
                line = line.trim();
                if (!line.isBlank()) {
                    links.append(line).append("\n");
                    count++;
                }
            }
            if (links.length() > 0) {
                return links.toString() + "\n(Total: " + count + " links)";
            }
        }

        // Fallback: regex-based URL extraction
        Pattern urlPattern = Pattern.compile("https?://[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+", Pattern.CASE_INSENSITIVE);
        Matcher matcher = urlPattern.matcher(content);

        StringBuilder links = new StringBuilder();
        int count = 0;
        while (matcher.find() && count < 50) {
            links.append(matcher.group()).append("\n");
            count++;
        }

        if (links.length() == 0) {
            return "No links found";
        }
        return links.toString() + "\n(Total: " + count + " links)";
    }

    /** Converts "linkText: url" or bare URL lines into markdown [text](url) for nice display. */
    private String linksListToMarkdown(String links) {
        if (links == null || links.isBlank()) return links;
        StringBuilder out = new StringBuilder();
        for (String line : links.split("\n")) {
            line = line.trim();
            if (line.isBlank()) continue;
            // Keep footer as plain text
            if (line.startsWith("(Total:") && line.endsWith(")")) {
                out.append("\n*").append(line).append("*");
                continue;
            }
            int sep = line.indexOf(": ");
            if (sep > 0) {
                String label = line.substring(0, sep).trim();
                String url = line.substring(sep + 2).trim();
                if (url.startsWith("http://") || url.startsWith("https://")) {
                    out.append("- [").append(escapeMarkdownBrackets(label)).append("](").append(url).append(")\n");
                    continue;
                }
            }
            if (line.startsWith("http://") || line.startsWith("https://")) {
                out.append("- [").append(line).append("](").append(line).append(")\n");
                continue;
            }
            out.append("- ").append(line).append("\n");
        }
        return out.toString().trim();
    }

    private String escapeMarkdownBrackets(String s) {
        if (s == null) return "";
        return s.replace("]", "\\]");
    }

    private void showSuggestedQuestions() {
        String suggestions = "\nTry asking:\n• What are the main points?\n• Who is the author?\n• When was this published?";
        addMessage("AI", suggestions);
    }

    private void showProductSuggestedQuestions() {
        List<String> suggestions = List.of(
                "Sản phẩm nào tốt nhất?",
                "Ngân sách của tôi X — nên chọn cái nào?",
                "Nên mua ngay hay đợi?",
                "Cho tôi link sản phẩm [tên sản phẩm]"
        );
        messages.add(new ChatEntry.SuggestionButtons(suggestions));
        chatListView.scrollTo(messages.size() - 1);
    }

    private void showConfigError() {
        addMessage("AI", "Please configure your LiteLLM API key in:\n" + configManager.getConfigPath());
    }

    private void handleError(Exception e) {
        loadingIndicator.setVisible(false);
        statusLabel.setText("Error");
        addMessage("AI", "Error: " + e.getMessage());
    }

    private void showError(String message) {
        loadingIndicator.setVisible(false);
        statusLabel.setText("Error");
        addMessage("AI", message);
    }

    private void showTemporaryMessage(String text) {
        String original = statusLabel.getText();
        statusLabel.setText(text);
        PauseTransition delay = new PauseTransition(Duration.seconds(2));
        delay.setOnFinished(e -> statusLabel.setText(original));
        delay.playFromStart();
    }

    /**
     * Configure idle detection so that after a short period without interaction,
     * the AI session is reset to its initial state for privacy.
     */
    private void setupIdleResetTimer() {
        idleResetTimer = new PauseTransition(Duration.seconds(IDLE_RESET_SECONDS));
        idleResetTimer.setOnFinished(e -> resetSession(true));
        restartIdleTimer();

        if (root != null) {
            root.addEventFilter(MouseEvent.ANY, e -> restartIdleTimer());
            root.addEventFilter(KeyEvent.ANY, e -> restartIdleTimer());
        }
    }

    private void restartIdleTimer() {
        if (idleResetTimer == null) return;
        idleResetTimer.stop();
        idleResetTimer.playFromStart();
    }

    /**
     * Reset all in-memory AI state (conversation, page content, reading mode) and
     * bring the panel back to a fresh state. When triggered by inactivity, a
     * short status message is shown so the user understands why history is gone.
     */
    private void resetSession(boolean triggeredByIdle) {
        messages.clear();
        autoSummaryShown = false;
        lastReadingModeContent = null;
        currentPageContent = null;
        currentPageTitle = null;
        currentPageUrl = null;

        if (questionInput != null) {
            questionInput.clear();
        }
        if (readingModePanel != null) {
            readingModePanel.setVisible(false);
            readingModePanel.setManaged(false);
        }
        if (chatListView != null) {
            chatListView.setVisible(true);
            chatListView.setManaged(true);
        }
        if (loadingIndicator != null) {
            loadingIndicator.setVisible(false);
            loadingIndicator.setManaged(false);
        }
        if (statusLabel != null) {
            statusLabel.setText(triggeredByIdle
                    ? "Conversation reset after inactivity to protect your privacy."
                    : "Conversation reset.");
        }

        restartIdleTimer();
    }

    @FXML
    public void onResetSession() {
        resetSession(false);
        onClose();
    }

    private static final int MAX_CHAT_ENTRIES = 150;

    private void addMessage(String sender, String text) {
        String formatted = sender + text;
        messages.add(new ChatEntry.TextMessage(formatted));
        trimMessagesIfNeeded();
        chatListView.scrollTo(messages.size() - 1);
    }

    /** Keep chat history bounded so the UI stays responsive even in long sessions. */
    private void trimMessagesIfNeeded() {
        int overshoot = messages.size() - MAX_CHAT_ENTRIES;
        if (overshoot <= 0) {
            return;
        }
        // Remove from the start (oldest first).
        for (int i = 0; i < overshoot; i++) {
            if (!messages.isEmpty()) {
                messages.remove(0);
            }
        }
    }

    /**
     * Appends a markdown list of products to the AI response text. When the AI response mentions
     * specific product URLs, only those products are included; otherwise all products on the page
     * are listed.
     */
    private String appendProductsMarkdownToResponse(String aiResponseBody) {
        if (aiResponseBody == null || aiResponseBody.isBlank()) return aiResponseBody;
        try {
            List<ProductInfo> products = productsForResponse(aiResponseBody);
            if (products.isEmpty()) return aiResponseBody;
            StringBuilder sb = new StringBuilder(aiResponseBody);
            sb.append("\n\n---\nSản phẩm trên trang:\n");
            for (ProductInfo p : products) {
                String title = (p.title() == null || p.title().isBlank()) ? "Sản phẩm" : p.title();
                sb.append("\n- **").append(title).append("**");
                if (p.price() != null && !p.price().isBlank()) {
                    sb.append(" — Giá: ").append(p.price());
                }
                if (p.url() != null && !p.url().isBlank()) {
                    if (p.imageUrl() != null && !p.imageUrl().isBlank()) {
                        sb.append("\n  ![](").append(p.imageUrl()).append(")");
                    }
                    String cta = (p.addToCartLabel() != null && !p.addToCartLabel().isBlank())
                            ? p.addToCartLabel()
                            : "Thêm vào giỏ";
                    sb.append("\n  - [Xem sản phẩm](").append(p.url()).append(")")
                      .append(" · [").append(cta).append("](").append(p.url()).append(")");
                }
            }
            return sb.toString();
        } catch (Exception e) {
            LOGGER.debug("Could not append products markdown to response", e);
            return aiResponseBody;
        }
    }

    /** For UI: compute list of products to show based on the AI response body. */
    private List<ProductInfo> productsForResponse(String aiResponseBody) {
        try {
            List<ProductInfo> pageProducts = contentExtractor != null ? contentExtractor.extractProducts() : List.of();
            if (pageProducts == null || pageProducts.isEmpty() || aiResponseBody == null || aiResponseBody.isBlank()) {
                return List.of();
            }
            List<ProductInfo> products = productsMentionedInResponse(pageProducts, aiResponseBody);
            if (products.isEmpty()) {
                products = pageProducts;
            }
            return products;
        } catch (Exception e) {
            LOGGER.debug("Could not compute products for response", e);
            return List.of();
        }
    }

    /** Returns products whose URL appears in the AI response (markdown or plain). */
    private List<ProductInfo> productsMentionedInResponse(List<ProductInfo> products, String aiResponseBody) {
        if (products == null || aiResponseBody == null || aiResponseBody.isBlank()) return List.of();
        List<ProductInfo> out = new ArrayList<>();
        for (ProductInfo p : products) {
            String url = p.url();
            if (url == null || url.isBlank()) continue;
            if (urlInResponse(url, aiResponseBody)) out.add(p);
        }
        return out;
    }

    private static final Pattern MARKDOWN_LINK_URL = Pattern.compile("\\]\\(\\s*(https?://[^)\\s]+)\\s*\\)", Pattern.CASE_INSENSITIVE);
    private static final Pattern BARE_URL = Pattern.compile("https?://[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+", Pattern.CASE_INSENSITIVE);

    private boolean urlInResponse(String productUrl, String aiResponseBody) {
        String norm = normalizeUrlForMatch(productUrl);
        // Check direct containment (AI often pastes full URL)
        if (norm.length() > 10 && aiResponseBody.contains(norm)) return true;
        if (productUrl.length() > 10 && aiResponseBody.contains(productUrl)) return true;
        // Check extracted URLs from markdown and bare URLs
        Matcher md = MARKDOWN_LINK_URL.matcher(aiResponseBody);
        while (md.find()) {
            String u = md.group(1).trim();
            if (urlsMatch(productUrl, u)) return true;
        }
        Matcher bare = BARE_URL.matcher(aiResponseBody);
        while (bare.find()) {
            if (urlsMatch(productUrl, bare.group())) return true;
        }
        return false;
    }

    private static String normalizeUrlForMatch(String url) {
        if (url == null) return "";
        String s = url.trim();
        if (s.endsWith("/")) s = s.substring(0, s.length() - 1);
        return s;
    }

    private static boolean urlsMatch(String a, String b) {
        if (a == null || b == null) return false;
        String na = normalizeUrlForMatch(a);
        String nb = normalizeUrlForMatch(b);
        return na.equals(nb) || a.contains(nb) || b.contains(na);
    }

    private Runnable onCloseCallback;

    public void setOnCloseCallback(Runnable callback) {
        this.onCloseCallback = callback;
    }
}
