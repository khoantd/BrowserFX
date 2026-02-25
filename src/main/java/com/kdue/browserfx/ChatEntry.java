package com.kdue.browserfx;

import java.util.List;

/**
 * A single entry in the AI Companion chat: either a text message,
 * a set of product cards, or a group of suggestion buttons.
 */
public sealed interface ChatEntry permits ChatEntry.TextMessage, ChatEntry.ProductCards, ChatEntry.SuggestionButtons {

    String getDisplayText();

    record TextMessage(String displayText) implements ChatEntry {
        @Override
        public String getDisplayText() {
            return displayText;
        }
    }

    record ProductCards(List<ProductInfo> products) implements ChatEntry {
        @Override
        public String getDisplayText() {
            return "";
        }
    }

    /**
     * A compact group of follow-up suggestion buttons (quick actions) shown as part of the chat.
     */
    record SuggestionButtons(List<String> suggestions) implements ChatEntry {
        @Override
        public String getDisplayText() {
            return "";
        }
    }
}
