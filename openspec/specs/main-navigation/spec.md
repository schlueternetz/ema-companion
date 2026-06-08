## Requirements

### Requirement: Bottom navigation bar with Home and Settings destinations
The app SHALL display a persistent bottom navigation bar containing exactly two destinations: Home and Settings. The bottom navigation bar SHALL be visible on both destination screens.

#### Scenario: Home is the start destination
- **WHEN** the app is launched
- **THEN** the Home screen SHALL be displayed and the Home item SHALL be selected in the bottom navigation bar

#### Scenario: Navigating to Settings
- **WHEN** the user taps the Settings item in the bottom navigation bar
- **THEN** the Settings screen SHALL be displayed and the Settings item SHALL be selected

#### Scenario: Navigating back to Home
- **WHEN** the user is on the Settings screen and taps the Home item
- **THEN** the Home screen SHALL be displayed and the Home item SHALL be selected

#### Scenario: Back press on start destination exits the app
- **WHEN** the user is on the Home screen (start destination) and presses the system Back button
- **THEN** the app SHALL exit (the activity finishes)
