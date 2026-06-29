## ADDED Requirements

### Requirement: Display mode is user-selectable
The app SHALL provide a display mode preference with three options: System (default), Light, and Dark. The selected mode SHALL be applied immediately without requiring an app restart. The preference SHALL be grouped under the "App Settings" section of the Settings screen.

#### Scenario: Display mode dialog shows three options
- **WHEN** the user taps the Display Mode row
- **THEN** a dialog SHALL appear with exactly three options: System, Light, Dark

#### Scenario: Selecting Light forces light mode
- **WHEN** the user selects "Light"
- **THEN** the app SHALL switch to light mode immediately via `AppCompatDelegate.setDefaultNightMode(MODE_NIGHT_NO)`
- **AND** the setting SHALL be persisted

#### Scenario: Selecting Dark forces dark mode
- **WHEN** the user selects "Dark"
- **THEN** the app SHALL switch to dark mode immediately via `AppCompatDelegate.setDefaultNightMode(MODE_NIGHT_YES)`
- **AND** the setting SHALL be persisted

#### Scenario: Selecting System defers to OS setting
- **WHEN** the user selects "System"
- **THEN** the app SHALL follow the device's system dark mode setting via `AppCompatDelegate.setDefaultNightMode(MODE_NIGHT_FOLLOW_SYSTEM)`
- **AND** the setting SHALL be persisted

### Requirement: Display mode preference is applied on cold start
The app SHALL apply the persisted display mode preference during startup, before the UI is rendered, so the correct mode is active from the first frame.

#### Scenario: Persisted dark mode is active on relaunch
- **WHEN** the user has selected "Dark" and relaunches the app
- **THEN** the app SHALL start in dark mode without any flash of the wrong mode
