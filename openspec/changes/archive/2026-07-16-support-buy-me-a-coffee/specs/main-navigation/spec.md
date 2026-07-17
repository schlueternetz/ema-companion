## MODIFIED Requirements

### Requirement: Bottom navigation bar with Home, Settings, User Guide, and Support destinations
The app SHALL display a persistent bottom navigation bar containing exactly four destinations in this fixed order: Home (leftmost), User Guide, Settings, Support (rightmost). The bottom navigation bar SHALL be visible on all destination screens.

#### Scenario: Home is the start destination
- **WHEN** the app is launched
- **THEN** the Home screen SHALL be displayed and the Home item SHALL be selected in the bottom navigation bar

#### Scenario: Navigating to Settings
- **WHEN** the user taps the Settings item in the bottom navigation bar
- **THEN** the Settings screen SHALL be displayed and the Settings item SHALL be selected

#### Scenario: Navigating back to Home
- **WHEN** the user is on the Settings screen and taps the Home item
- **THEN** the Home screen SHALL be displayed and the Home item SHALL be selected

#### Scenario: Navigating to User Guide
- **WHEN** the user taps the User Guide item in the bottom navigation bar
- **THEN** the User Guide screen SHALL be displayed and the User Guide item SHALL be selected

#### Scenario: Navigating to Support
- **WHEN** the user taps the Support item in the bottom navigation bar
- **THEN** the Support screen SHALL be displayed and the Support item SHALL be selected

#### Scenario: Back press on start destination exits the app
- **WHEN** the user is on the Home screen (start destination) and presses the system Back button
- **THEN** the app SHALL exit (the activity finishes)
