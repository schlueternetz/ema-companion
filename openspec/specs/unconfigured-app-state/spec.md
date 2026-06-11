## Requirements

### Requirement: App navigates to Settings when required fields are missing
The app SHALL consider itself unconfigured when any of the following fields are absent or empty: EMA App ID, EMA App Secret, EMA System ID, EMA ECU ID, System Capacity. When unconfigured, the app SHALL open directly to the Settings screen on launch and SHALL disable all bottom navigation items except Settings.

#### Scenario: Unconfigured app opens to Settings on launch
- **WHEN** the app is launched and one or more required fields are not set
- **THEN** the Settings screen SHALL be the active destination
- **AND** all bottom navigation items except Settings SHALL be visually disabled and non-interactive

#### Scenario: Navigation is restored once all required fields are saved
- **WHEN** the user saves the last missing required field
- **THEN** all bottom navigation items SHALL become enabled without requiring an app restart

#### Scenario: Fully configured app opens normally
- **WHEN** the app is launched and all required fields hold valid values
- **THEN** the app SHALL open to its default start destination (not forced to Settings)
- **AND** all bottom navigation items SHALL be enabled
