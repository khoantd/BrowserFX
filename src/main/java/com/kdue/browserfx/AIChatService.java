package com.kdue.browserfx;

import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class AIChatService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AIChatService.class);
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    /** Appended to system prompt so the assistant responds in Vietnamese by default. */
    private static final String DEFAULT_LANGUAGE_INSTRUCTION = " Always respond in Vietnamese (Tiếng Việt).";

    public enum PromptType {
        QUESTION("You are a helpful assistant that answers questions about web page content. Provide concise and accurate answers based only on the provided page content. The content may include \"Products on page (title | URL | price):\" and an \"Additional links found on page\" section with lines of the form \"linkText: url\". When the user asks for the link or URL of a specific product (or the cheapest product, etc.), use those sections and return the full URL for that product—never return only \"/\" or a relative path. Do not return the current page URL or a collection/category URL unless the user explicitly asked for that. When the user asks for any other link or URL, use the section to find and return the exact full URL."),
        SUMMARIZE("You are a helpful assistant that summarizes web page content. Provide a clear, concise summary capturing the main points."),
        EXTRACT_LINKS("You are a helpful assistant that extracts URLs from web page content. List all valid URLs found, one per line."),
        EXTRACT_CONTACT("You are a helpful assistant that extracts contact information from web page content. Find and list all emails, phone numbers, and addresses."),
        TRANSLATE("You are a helpful assistant that translates content to the specified language. Preserve the original meaning."),
        READING_MODE("You are a helpful assistant that extracts clean, readable text from web page content. Remove all navigation, ads, and clutter. Return only the main article or content in a clean format."),
        HELP_CHOOSE_PRODUCT("You are a helpful shopping assistant. Based on the web page content (which may include a \"Products on page (title | URL | price):\" section and an \"Additional links found on page\" section with \"linkText: url\" lines), help the user choose the right product. Use product titles, URLs, and prices from these sections. Compare options clearly, consider their stated budget or priorities if given, and recommend the best fit with brief pros and cons. When mentioning a product, include its full URL from the page content so the user can click through (never return only a path like \"/\" or \"/product/1\"). Be concise and practical."),
        PURCHASE_ADVICE("You are a helpful, honest shopping advisor. Based on the page content (including \"Products on page (title | URL | price):\" and \"Additional links found on page\" with \"linkText: url\"), give smart purchase advice: highlight real value, genuine limited-time or stock cues if present, and how well items match the user's needs. Use the product list and full URLs from the content when referring to specific items (never return only \"/\" or a relative path). Gently nudge toward a confident decision when it makes sense, but never be pushy or invent fake urgency. If something is not a good fit, say so. Keep the tone helpful and trustworthy.");

        private final String systemPrompt;

        PromptType(String systemPrompt) {
            this.systemPrompt = systemPrompt;
        }

        public String getSystemPrompt() {
            return systemPrompt;
        }
    }

    private final ConfigManager configManager;
    private final OkHttpClient client;
    private String currentModel;

    public AIChatService(ConfigManager configManager) {
        this.configManager = configManager;
        this.currentModel = configManager.getLitellmModel();
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    public void setModel(String model) {
        this.currentModel = model;
    }

    public String getModel() {
        return currentModel;
    }

    public void sendMessage(String pageContent, String userQuestion, Callback callback) {
        sendMessage(pageContent, userQuestion, PromptType.QUESTION, callback);
    }

    public void sendMessage(String pageContent, String userQuestion, PromptType promptType, Callback callback) {
        String endpoint = configManager.getLitellmEndpoint();
        String apiKey = configManager.getLitellmApiKey();

        if (apiKey == null || apiKey.isBlank()) {
            callback.onFailure(null, new IOException("API key not configured"));
            return;
        }

        String url = buildUrl(endpoint);

        String systemPrompt = promptType.getSystemPrompt() + DEFAULT_LANGUAGE_INSTRUCTION;
        String userContent = buildUserContent(pageContent, userQuestion, promptType);

        sendRequest(url, apiKey, currentModel, systemPrompt, userContent, callback);
    }

    public String sendMessageSync(String pageContent, String userQuestion) throws IOException {
        return sendMessageSync(pageContent, userQuestion, PromptType.QUESTION);
    }

    public String sendMessageSync(String pageContent, String userQuestion, PromptType promptType) throws IOException {
        String endpoint = configManager.getLitellmEndpoint();
        String apiKey = configManager.getLitellmApiKey();

        if (apiKey == null || apiKey.isBlank()) {
            throw new IOException("API key not configured");
        }

        String url = buildUrl(endpoint);
        String systemPrompt = promptType.getSystemPrompt() + DEFAULT_LANGUAGE_INSTRUCTION;
        String userContent = buildUserContent(pageContent, userQuestion, promptType);

        try {
            Response response = sendRequestSync(url, apiKey, currentModel, systemPrompt, userContent);
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected response: " + response);
            }
            String responseBody = response.body() != null ? response.body().string() : "";
            response.close();
            return parseAIResponse(responseBody);
        } catch (IOException e) {
            throw e;
        }
    }

    private String buildUrl(String endpoint) {
        String url = endpoint + "/chat/completions";
        if (!url.startsWith("http")) {
            url = "https://" + url;
            if (!url.contains("/chat/completions")) {
                url = url + "/chat/completions";
            }
        }
        return url;
    }

    private String buildUserContent(String pageContent, String userQuestion, PromptType promptType) {
        if (pageContent == null || pageContent.isBlank()) {
            return userQuestion + "\n\n(No page content available)";
        }

        return switch (promptType) {
            case SUMMARIZE -> "Summarize this web page content:\n\n" + pageContent;
            case EXTRACT_LINKS -> "Extract all URLs from this web page content. List each URL on a new line:\n\n" + pageContent;
            case EXTRACT_CONTACT -> "Extract all contact information (emails, phones, addresses) from this web page content:\n\n" + pageContent;
            case TRANSLATE -> userQuestion + "\n\nContent to translate:\n" + pageContent;
            case READING_MODE -> "Extract the main readable content from this web page. Remove all navigation, ads, sidebars, and clutter. Return clean text:\n\n" + pageContent;
            case HELP_CHOOSE_PRODUCT -> ("Help the user choose the right product from this page. " + (userQuestion != null && !userQuestion.isBlank() ? "User's priorities or context: " + userQuestion + "\n\n" : "") + "Page content:\n\n") + pageContent;
            case PURCHASE_ADVICE -> ("Give smart purchase advice based on this page. " + (userQuestion != null && !userQuestion.isBlank() ? "User's question or context: " + userQuestion + "\n\n" : "") + "Page content:\n\n") + pageContent;
            default -> "Web page content:\n" + pageContent + "\n\nQuestion: " + userQuestion;
        };
    }

    private void sendRequest(String url, String apiKey, String model, String systemPrompt, String userContent, Callback callback) {
        String jsonBody = String.format("""
                {
                    "model": "%s",
                    "messages": [
                        {"role": "system", "content": "%s"},
                        {"role": "user", "content": "%s"}
                    ],
                    "temperature": 0.7
                }
                """,
                escapeJson(model),
                escapeJson(systemPrompt),
                escapeJson(userContent)
        );

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(jsonBody, JSON))
                .build();

        client.newCall(request).enqueue(callback);
    }

    private Response sendRequestSync(String url, String apiKey, String model, String systemPrompt, String userContent) throws IOException {
        String jsonBody = String.format("""
                {
                    "model": "%s",
                    "messages": [
                        {"role": "system", "content": "%s"},
                        {"role": "user", "content": "%s"}
                    ],
                    "temperature": 0.7
                }
                """,
                escapeJson(model),
                escapeJson(systemPrompt),
                escapeJson(userContent)
        );

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(jsonBody, JSON))
                .build();

        return client.newCall(request).execute();
    }

    /**
     * Extracts the assistant message content from a chat completion JSON response.
     * Looks for choices[0].message.content and parses the string value so that
     * escaped quotes (e.g. in URLs or quoted product names) do not truncate the result.
     */
    public String parseAIResponse(String jsonResponse) {
        try {
            // Target the content inside the assistant message, not any other "content" key
            int messageStart = jsonResponse.indexOf("\"message\"");
            if (messageStart == -1) {
                return "Error: Could not parse AI response";
            }
            int contentKeyStart = jsonResponse.indexOf("\"content\"", messageStart);
            if (contentKeyStart == -1) {
                return "Error: Could not parse AI response";
            }
            int colonIndex = jsonResponse.indexOf(":", contentKeyStart);
            int afterColon = colonIndex + 1;
            while (afterColon < jsonResponse.length() && Character.isWhitespace(jsonResponse.charAt(afterColon))) {
                afterColon++;
            }
            if (afterColon < jsonResponse.length() && jsonResponse.charAt(afterColon) == '[') {
                // content is array, e.g. [{"type":"text","text":"...actual response..."}]
                int textKey = jsonResponse.indexOf("\"text\"", afterColon);
                if (textKey != -1) {
                    int textColon = jsonResponse.indexOf(":", textKey);
                    int textQuoteStart = jsonResponse.indexOf("\"", textColon);
                    if (textQuoteStart != -1) {
                        int textQuoteEnd = findStringEnd(jsonResponse, textQuoteStart + 1);
                        if (textQuoteEnd != -1) {
                            String raw = jsonResponse.substring(textQuoteStart + 1, textQuoteEnd);
                            return unescapeJsonString(raw);
                        }
                    }
                }
                return "Error: Could not parse AI response";
            }
            // content is a string
            int quoteStart = jsonResponse.indexOf("\"", colonIndex);
            if (quoteStart == -1 || quoteStart + 1 >= jsonResponse.length()) {
                return "Error: Could not parse AI response";
            }
            int quoteEnd = findStringEnd(jsonResponse, quoteStart + 1);
            if (quoteEnd == -1) {
                return "Error: Could not parse AI response";
            }
            String raw = jsonResponse.substring(quoteStart + 1, quoteEnd);
            return unescapeJsonString(raw);
        } catch (Exception e) {
            LOGGER.error("Failed to parse AI response", e);
            return "Error: Could not parse AI response";
        }
    }

    /** Finds the index of the closing double-quote of a JSON string, respecting \\ and \". */
    private static int findStringEnd(String json, int start) {
        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '\\' && i + 1 < json.length()) {
                i++; // skip next character (escaped)
                continue;
            }
            if (c == '"') {
                return i;
            }
        }
        return -1;
    }

    /** Unescapes a JSON string value (e.g. \\n -> newline, \\" -> ", \\\\ -> \). */
    private static String unescapeJsonString(String s) {
        if (s == null || s.isEmpty()) return s;
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) {
                char next = s.charAt(i + 1);
                switch (next) {
                    case '"' -> sb.append('"');
                    case '\\' -> sb.append('\\');
                    case 'n' -> sb.append('\n');
                    case 'r' -> sb.append('\r');
                    case 't' -> sb.append('\t');
                    default -> { sb.append('\\'); sb.append(next); }
                }
                i++;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
