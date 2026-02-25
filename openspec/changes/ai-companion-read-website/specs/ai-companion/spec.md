## Requirements

### Requirement: AI Companion button in toolbar
The browser SHALL display an "AI" button in the toolbar to toggle the AI Companion panel.

#### Scenario: AI button visible in toolbar
- **WHEN** the browser application starts
- **THEN** an AI button (🤖 icon) is visible in the toolbar

#### Scenario: Clicking AI button opens panel
- **WHEN** the user clicks the AI button
- **THEN** the AI Companion panel slides in from the right side of the window

#### Scenario: Keyboard shortcut toggles panel
- **WHEN** the user presses Ctrl+Shift+A (Windows/Linux) or Cmd+Shift+A (macOS)
- **THEN** the AI Companion panel toggles (opens if closed, closes if open)

#### Scenario: Clicking AI button while panel open closes it
- **WHEN** the AI Companion panel is visible and the user clicks the AI button
- **THEN** the panel slides out and hides

---

### Requirement: Page content extraction
The AI Companion SHALL extract readable text content from the currently loaded web page.

#### Scenario: Extract content on panel open
- **WHEN** the AI Companion panel opens
- **THEN** the system extracts text content from the current page silently (content is not displayed, used only as AI context)

#### Scenario: Content extraction in progress
- **WHEN** content is being extracted
- **THEN** a brief "Reading page..." indicator is shown

#### Scenario: No content when page not loaded
- **WHEN** the panel opens with no page loaded
- **THEN** a message "No page loaded" is displayed

---

### Requirement: AI question answering
The AI Companion SHALL allow users to ask questions about the page content and receive AI-generated responses.

#### Scenario: Send question to AI
- **WHEN** the user types a question and presses Enter or clicks Send
- **THEN** the question is sent to the AI API with the page context

#### Scenario: Display AI response
- **WHEN** the AI API returns a response
- **THEN** the response is displayed in the chat area

#### Scenario: Show loading indicator
- **WHEN** a question is sent to the AI
- **THEN** a loading spinner is displayed while waiting for response

#### Scenario: Handle API error
- **WHEN** the AI API returns an error
- **THEN** an error message is displayed in the chat area

---

### Requirement: API key configuration
The AI Companion SHALL allow users to configure their own AI API key.

#### Scenario: First launch without API key
- **WHEN** the app launches without a configured API key
- **THEN** a prompt or message instructs the user to configure their API key

#### Scenario: API key in config file
- **WHEN** the user provides an API key in the config file
- **THEN** the key is used for AI API calls

---

## ADDED Requirements

### Requirement: Panel dimensions
The AI Companion panel SHALL have a fixed width of 350 pixels and span the full height of the window.

#### Scenario: Panel width
- **WHEN** the panel is visible
- **THEN** it occupies exactly 350 pixels of width on the right side

#### Scenario: Panel height
- **WHEN** the panel is visible
- **THEN** it spans from the toolbar to the bottom of the window

---

### Requirement: Chat message display
The AI Companion SHALL display a scrollable list of messages, alternating between user questions and AI responses.

#### Scenario: New message added
- **WHEN** a user sends a question or receives an AI response
- **THEN** the message appears at the bottom of the chat area

#### Scenario: Chat scroll
- **WHEN** the chat area contains many messages
- **THEN** the user can scroll to view previous messages
