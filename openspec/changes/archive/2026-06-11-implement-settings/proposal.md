## Why

The Settings screen currently stores only the language preference, but the app needs to connect to the EMA API — which requires credentials and system identifiers the user must supply. Without these settings the app cannot fetch any data. An import/export mechanism is also needed so configuration can be loaded from a JSON file, which supports both end-user convenience and local stub-server testing.

## What Changes

- Add 9 new user-configurable settings to the Settings screen: EMA App ID, EMA App Secret, EMA System ID, EMA ECU ID, System Capacity, Historic Data Days, Base URL, Notifications Enabled, and Display Mode (System/Light/Dark)
- Each credential/ID field has strict validation (fixed length, character set) enforced in the UI before saving
- EMA App Secret is masked in the UI (last 4 characters visible); field is cleared when the user enters edit mode
- Display Mode applies immediately app-wide via `AppCompatDelegate` and is re-applied on cold start
- Add Import Settings — user picks a JSON file; all 10 settings (including language and display mode) are loaded from it
- Add Export Settings — app writes all 10 settings to a user-chosen JSON file (`ema-companion-settings.json` as default filename); export optionally encrypts the file with a 4-digit PIN (AES-256-GCM, key derived from PIN); import auto-detects encrypted files and prompts for the PIN
- Add Factory Reset — destructive action (confirmation required) that clears all settings and any locally stored production data
- When required settings (EMA credentials + System Capacity) are not fully configured, the app opens directly to Settings and all other bottom navigation items are disabled until configuration is complete

## Capabilities

### New Capabilities
- `ema-api-credentials`: Store, validate, and display the four EMA API credential fields (App ID, App Secret, System ID, ECU ID) with field-level validation rules and masking for the secret — grouped under Solar Array Settings
- `solar-array-capacity`: Store and validate System Capacity (Float, ≤ 2 d.p., 0–999.99 kW) — grouped under Solar Array Settings
- `data-history-preference`: Store and validate Historic Data Days (Int, 1–90) — grouped under App Settings
- `notification-preference`: Notifications Enabled toggle (Boolean, default true) — grouped under App Settings
- `base-url-configuration`: Base URL field (valid URL, defaults to production EMA API) with reset-to-default action — grouped under Configuration
- `settings-import-export`: Import all 10 settings from a user-selected JSON file (auto-detects encrypted files and prompts for PIN); export all 10 settings to a user-chosen JSON file with optional AES-256-GCM PIN encryption (default filename: `ema-companion-settings.json`)
- `factory-reset`: Destructive action with confirmation dialog that resets all settings to defaults and clears locally stored production data
- `dark-mode`: User-selectable display mode (System / Light / Dark) applied immediately app-wide via `AppCompatDelegate`; persisted and re-applied on cold start
- `unconfigured-app-state`: When EMA credentials and System Capacity are not all provided, the app opens to Settings and disables all other navigation until configuration is complete

### Modified Capabilities
- `settings`: Settings screen now displays all 10 configurable fields grouped into three sections (Solar Array Settings, App Settings, Configuration) plus Import, Export, and Factory Reset actions

## Impact

- `feature/settings/SettingsRepository` — extended with getters/setters for 9 new fields; import/export and `isConfigured()` logic added
- `feature/settings/SettingsCrypto` (new file) — AES-256-GCM encrypt/decrypt using a PIN-derived key; no external dependencies (`javax.crypto`)
- `feature/settings/SettingsFragment` and `fragment_settings.xml` — new rows for each field, inline edit/save/cancel pattern, masking for secret, section grouping
- `MainActivity` — reads display mode preference before `setContentView()` (dark mode cold start); checks configuration completeness and disables navigation items when unconfigured
- `strings.xml` / `strings-de.xml` — labels, error messages, tooltips, confirmation dialog text for all new fields and actions
- No new Gradle dependencies required (file I/O via `ContentResolver`; AES-256-GCM via `javax.crypto` which is part of the Android SDK)
