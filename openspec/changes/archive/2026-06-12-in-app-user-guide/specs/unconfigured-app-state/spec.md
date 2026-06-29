## MODIFIED Requirements

### Requirement: App navigates to Settings when required fields are missing
The app SHALL consider itself unconfigured when any of the following fields are absent or empty: EMA App ID, EMA App Secret, EMA System ID, EMA ECU ID, System Capacity. (Historic Data Days and API Request Limit are NOT required — they always hold a usable default and do not affect this check.) When unconfigured, the app SHALL open directly to the Settings screen on launch and SHALL disable all bottom navigation items except Settings and User Guide. The app SHALL achieve this by making Settings the navigation start destination while unconfigured, so the back stack contains only Settings and bottom-navigation to other destinations works correctly once they are enabled.

#### Scenario: Unconfigured app opens to Settings on launch
- **WHEN** the app is launched and one or more required fields are not set
- **THEN** the Settings screen SHALL be the active destination
- **AND** all bottom navigation items except Settings and User Guide SHALL be visually disabled and non-interactive

#### Scenario: User Guide is accessible when app is unconfigured
- **WHEN** the app is launched and one or more required fields are not set
- **THEN** the User Guide navigation item SHALL be enabled and tappable
- **AND** tapping the User Guide item SHALL navigate to the User Guide screen

#### Scenario: Navigation is restored once all required fields are saved
- **WHEN** the user saves the last missing required field
- **THEN** all bottom navigation items SHALL become enabled without requiring an app restart

#### Scenario: Navigation is restored and usable after import completes configuration
- **WHEN** the app started unconfigured and the user imports a settings file that fills in all required fields
- **THEN** all bottom navigation items SHALL become enabled without requiring an app restart
- **AND** tapping the Home navigation item SHALL navigate to the Home screen

#### Scenario: Fully configured app opens normally
- **WHEN** the app is launched and all required fields hold valid values
- **THEN** the app SHALL open to its default start destination (not forced to Settings)
- **AND** all bottom navigation items SHALL be enabled

