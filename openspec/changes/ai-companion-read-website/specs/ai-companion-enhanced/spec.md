# AI Companion Enhanced Features - Design

## Context

BrowserFX AI Companion already supports asking questions about web page content. This enhancement adds more utility features for end users on vendor machines.

---

## Goals

Provide a richer AI Companion experience with:
1. Quick actions for common tasks
2. Auto-summarization
3. Smart suggestions
4. Additional utility features (extract links, data, translate, etc.)

---

## Decisions

### 1. Quick Actions Panel

**Decision**: Add a row of quick action buttons below the header.

**Buttons**:
- 📝 Summarize - Summarize page content
- ❓ What is this? - Page overview
- 🔗 Extract Links - List all URLs
- 📧 Find Contact - Extract emails/phones
- 📋 Copy Page Text - Copy readable text

**Rationale**: One-click actions for common tasks, no typing required.

---

### 2. Auto-Summary on Open

**Decision**: When AI panel opens, automatically generate and display a summary.

**Flow**:
1. Panel opens → Extract page content → Send to AI with "summarize" prompt
2. Display summary in chat as AI message (with "Summary" label)
3. User can ask follow-up questions

**Rationale**: Immediate value without user input.

---

### 3. Suggested Questions

**Decision**: After auto-summary, show 3 suggested questions as clickable chips.

**Suggestions** (context-aware):
- "What are the main points?"
- "Who is the author?"
- "When was this published?"
- Custom based on page content

**Rationale**: Helps users discover useful questions.

---

### 4. Copy Response Button

**Decision**: Add copy button next to each AI response.

**Implementation**: Click to copy → Show "Copied!" tooltip → Revert after 2s

---

### 5. Clear Chat

**Decision**: Add trash icon in header to clear all messages.

**Confirmation**: Show alert before clearing.

---

### 6. Model Switcher

**Decision**: Add dropdown in header to select LLM model.

**Stored**: Save preference in config.

**Models** (configurable):
- gpt-4o-mini (default)
- gpt-4o
- claude-3-haiku
- etc.

---

### 7. Reading Mode

**Decision**: Add "Reading Mode" quick action that shows clean, distraction-free text.

**Implementation**: 
1. Extract clean text via JavaScript
2. Display in a modal/overlay
3. Font size controls (+/-)
4. Close button

---

### 8. Link Extractor

**Decision**: Quick action to extract and display all links from page.

**Output**: List of URLs with titles, displayed in chat area.

---

### 9. Page Translation

**Decision**: Add language selector and translate page content.

**Implementation**: 
- Dropdown: English, Spanish, French, German, Vietnamese, Chinese, Japanese
- Use AI to translate extracted content

---

### 10. Extract Data

**Decision**: Smart extraction of structured data.

**Types**:
- Emails
- Phone numbers
- Prices/costs
- Dates
- Addresses

---

## UI Layout

```
┌─────────────────────────────────┐
│ AI Companion        [Model▼] 🗑 │  ← Header
├─────────────────────────────────┤
│ [📝 Summarize] [🔗 Links] [📧]  │  ← Quick Actions
│ [📋 Copy]    [📖 Read] [🌐 │
├────────────────────────────────]    ─┤
│                                 │
│  Summary:                       │  ← Auto-summary
│  This page discusses...         │
│                                 │
│  [What is this?] [Main points] │  ← Suggestions
│                                 │
│ ─────────────────────────────── │
│                                 │
│  You: What about X?             │
│                                 │
│  AI: Based on the page...       │
│                                 │
├─────────────────────────────────┤
│ [________________] [Send]       │  ← Input
└─────────────────────────────────┘
```

---

## Acceptance Criteria

- [ ] Quick action buttons visible and functional
- [ ] Auto-summary generated when panel opens
- [ ] Suggested questions shown after summary
- [ ] Copy button works on AI responses
- [ ] Clear chat with confirmation
- [ ] Model switcher changes API model
- [ ] Reading mode shows clean text
- [ ] Link extractor lists all URLs
- [ ] Translation works for selected languages
- [ ] Data extraction (emails, phones) works

---

## Implementation Plan

1. Update AICompanionPanel.fxml with quick actions + header controls
2. Update AICompanionController with new features
3. Update AIChatService to support different prompt types
4. Add new service methods for extraction
