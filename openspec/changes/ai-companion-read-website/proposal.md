# AI Companion Feature - Proposal

## Summary

Add an AI Companion feature to BrowserFX that allows users to:
1. Extract content from the currently loaded web page
2. Ask questions about the page content
3. Receive AI-powered responses

## User Experience

1. User browses to any website
2. User clicks "AI" button in toolbar
3. AI Companion panel slides open from the right
4. Panel shows extracted page content summary
5. User types a question in the input field
6. AI responds based on the page content

## Technical Implementation

### Components

| Component | Responsibility |
|-----------|---------------|
| `AICompanionPanel.fxml` | Panel UI layout |
| `AICompanionController.java` | Handle UI events, manage chat |
| `PageContentExtractor.java` | Extract text via JavaScript |
| `AIChatService.java` | Call OpenAI API |
| `ConfigManager.java` | Load/store API key |

### Dependencies

- Add `com.squareup.okhttp3:okhttp` for API calls
- Existing: JavaFX WebView, SLF4J

### API Contract

```
POST <LiteLLM_ENDPOINT>/chat/completions
Headers: Authorization: Bearer <API_KEY>
Body: {
  "model": "<MODEL_NAME>",
  "messages": [
    {"role": "system", "content": "You are a helpful assistant that answers questions about web page content."},
    {"role": "user", "content": "Context:\n<extracted_page_content>\n\nQuestion: <user_question>"}
  ]
}
```

**Configuration** (in `config.properties`):
```properties
litellm.endpoint=https://your-litellm-proxy.com
litellm.api_key=your-api-key
litellm.model=gpt-4o-mini
```

## Acceptance Criteria

- [ ] AI button appears in toolbar
- [ ] Ctrl+Shift+A toggles AI Companion panel
- [ ] Clicking AI button toggles side panel
- [ ] Page content extracted silently (not displayed) when panel opens
- [ ] User can type and send questions
- [ ] AI responds with relevant answers
- [ ] Loading indicator shown during API calls
- [ ] Error handling for API failures
- [ ] LiteLLM endpoint configurable via config file

## Timeline

- Design: Complete
- Implementation: ~2-3 hours
- Testing: ~1 hour

## Questions for User

- [x] AI Model: LiteLLM proxy (answered)
- [x] Content Display: Hidden entirely (answered)
- [x] Keyboard Shortcut: Ctrl+Shift+A (answered)
