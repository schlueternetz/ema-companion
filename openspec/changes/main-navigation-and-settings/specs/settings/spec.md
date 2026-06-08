## ADDED Requirements

### Requirement: Settings screen accessible from bottom navigation
The app SHALL provide a Settings screen reachable via the bottom navigation bar. The Settings screen SHALL display all user-configurable app preferences.

#### Scenario: Settings screen displays available preferences
- **WHEN** the user navigates to the Settings screen
- **THEN** the Settings screen SHALL display at least the language preference option

### Requirement: Settings persisted with encryption
All user-configurable settings SHALL be persisted using `EncryptedSharedPreferences` so that values survive app restarts. Settings SHALL be readable and writable only within the app process.

#### Scenario: Setting survives app restart
- **WHEN** the user changes a setting and the app is closed and reopened
- **THEN** the setting SHALL retain the value the user selected

#### Scenario: Unreadable store falls back to defaults
- **WHEN** the encrypted store cannot be read (e.g. Keystore key lost)
- **THEN** the app SHALL fall back to default values for all settings and SHALL NOT crash
