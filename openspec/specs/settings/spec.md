## Requirements

### Requirement: Settings screen displays all user-configurable preferences grouped by section
The app SHALL provide a Settings screen reachable via the bottom navigation bar. The Settings screen SHALL display all user-configurable app preferences organized into three labeled sections:

**Solar Array Settings** — EMA App ID, EMA App Secret, EMA System ID, EMA ECU ID, System Capacity

**App Settings** — Language, Display Mode, Notifications Enabled, Historic Data Days

**API Settings** — API Request Limit (with monthly consumption progress bar), Base URL, Import Settings button, Export Settings button, Factory Reset button

#### Scenario: Settings screen displays all sections and fields
- **WHEN** the user navigates to the Settings screen
- **THEN** the screen SHALL display three labeled sections: "Solar Array Settings", "App Settings", and "API Settings"
- **AND** "App Settings" SHALL contain: Language, Display Mode, Notifications Enabled, Historic Data Days
- **AND** "Solar Array Settings" SHALL contain: EMA App ID, EMA App Secret, EMA System ID, EMA ECU ID, System Capacity
- **AND** "API Settings" SHALL contain: API Request Limit (with consumption progress bar), Base URL (with reset-to-default action), Import Settings, Export Settings, Factory Reset

### Requirement: Settings persisted with encryption
All user-configurable settings SHALL be persisted using `EncryptedSharedPreferences` so that values survive app restarts. Settings SHALL be readable and writable only within the app process.

#### Scenario: Setting survives app restart
- **WHEN** the user changes a setting and the app is closed and reopened
- **THEN** the setting SHALL retain the value the user selected

#### Scenario: Unreadable store falls back to defaults
- **WHEN** the encrypted store cannot be read (e.g. Keystore key lost)
- **THEN** the app SHALL fall back to default values for all settings and SHALL NOT crash
