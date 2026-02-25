## ADDED Requirements

### Requirement: Back navigation
The browser SHALL navigate to the previous page in the WebEngine history when the Back button is activated. The Back button SHALL be disabled when there is no previous history entry.

#### Scenario: Back navigates to previous page
- **WHEN** the user has visited at least two pages and clicks the Back button
- **THEN** the browser loads the previous page in the WebEngine history

#### Scenario: Back button disabled with no history
- **WHEN** the browser has loaded only one page (no previous history entry exists)
- **THEN** the Back button SHALL be disabled and non-interactive

---

### Requirement: Forward navigation
The browser SHALL navigate to the next page in the WebEngine history when the Forward button is activated. The Forward button SHALL be disabled when there is no next history entry.

#### Scenario: Forward navigates to next page
- **WHEN** the user has gone back at least once and clicks the Forward button
- **THEN** the browser loads the next page in the WebEngine history

#### Scenario: Forward button disabled at end of history
- **WHEN** the user is at the most recent page in history
- **THEN** the Forward button SHALL be disabled and non-interactive

---

### Requirement: Reload current page
The browser SHALL reload the currently loaded page when the Reload button is activated.

#### Scenario: Reload refreshes current page
- **WHEN** the user clicks the Reload button
- **THEN** the WebEngine reloads the current URL from the network

---

### Requirement: Home navigation
The browser SHALL navigate to the configured home page when the Home button is activated.

#### Scenario: Home loads the home page
- **WHEN** the user clicks the Home button
- **THEN** the browser loads `https://www.google.com`

---

### Requirement: Navigation button state reflects history
The browser SHALL continuously update the enabled/disabled state of Back and Forward buttons as the WebEngine history changes.

#### Scenario: Buttons update after each page load
- **WHEN** a new page finishes loading
- **THEN** Back is enabled if a previous history entry exists, disabled otherwise
- **THEN** Forward is enabled if a next history entry exists, disabled otherwise
