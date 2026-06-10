## 1. Repository — New Settings Fields

- [ ] 1.1 Add constants and getters/setters to `SettingsRepository` for EMA App ID (String, lowercase, default "")
- [ ] 1.2 Add constants and getters/setters for EMA App Secret (String, lowercase, default "")
- [ ] 1.3 Add constants and getters/setters for EMA System ID (String, uppercase, default "")
- [ ] 1.4 Add constants and getters/setters for EMA ECU ID (String, default "")
- [ ] 1.5 Add constants and getters/setters for System Capacity (Float, default -1f as "unset" sentinel)
- [ ] 1.6 Add constants and getters/setters for Historic Data Days (Int, default -1 as "unset" sentinel)
- [ ] 1.7 Add constants and getters/setters for Notifications Enabled (Boolean, default true)
- [ ] 1.8 Add constants and getters/setters for Base URL (String, default `https://api.apsystemsema.com:9282/user/api/v2/`)
- [ ] 1.9 Add constants and getters/setters for Display Mode (String enum: "system"/"light"/"dark", default "system")
- [ ] 1.10 Add `clearAll()` method that removes every key from the shared preferences store
- [ ] 1.11 Write unit tests in `SettingsRepositoryTest` covering get/set round-trips and defaults for all 9 new fields, plus `clearAll()`

## 2. Repository — Import/Export Logic

- [ ] 2.1 Add `exportToJson(): String` to `SettingsRepository` — serializes all 10 settings to a JSON string using the documented key names
- [ ] 2.2 Add `importFromJson(json: String)` to `SettingsRepository` — merges recognized keys from the JSON string into stored settings; ignores unrecognized keys; throws `IllegalArgumentException` on malformed JSON
- [ ] 2.3 Write unit tests for `exportToJson` (all 10 keys present, correct types) and `importFromJson` (merge semantics, missing keys unchanged, malformed JSON throws)
- [ ] 2.4 Create `src/test/resources/settings/stub-server.json` containing all 10 settings with valid placeholder values so `isConfigured()` returns true; `baseUrl` points to `http://localhost:8080`; credential placeholders must satisfy their validation rules (e.g. `emaAppId`: 32 alphanumeric chars, `emaAppSecret`: 12, `emaSystemId`: 16, `emaEcuId`: 12 digits) but must not be real credentials
- [ ] 2.5 Create `src/test/java/.../settings/SettingsTestLoader.kt` — a test helper with a single function `loadFixture(repository: SettingsRepository, resourcePath: String)` that reads the named file from `src/test/resources/` and calls `repository.importFromJson()`

## 3. Crypto Utility

- [ ] 3.1 Create `feature/settings/SettingsCrypto.kt` with `encrypt(json: String, pin: String): String` — derives key via `SHA-256(pin)`, generates a random 12-byte IV, encrypts with AES-256-GCM, returns `base64(IV + ciphertext)`
- [ ] 3.2 Add `decrypt(data: String, pin: String): String` to `SettingsCrypto` — decodes base64, splits IV and ciphertext, decrypts with AES-256-GCM; throws `AEADBadTagException` on wrong PIN
- [ ] 3.3 Write unit tests for `SettingsCrypto`: round-trip (encrypt then decrypt returns original JSON), wrong PIN throws, different exports of the same JSON produce different ciphertext (random IV)

## 4. Custom View — SettingRowView

- [ ] 4.1 Create `res/layout/view_setting_row.xml` — label, value TextView, OutlinedEditText (initially hidden), error TextView, Edit/Save/Cancel icon buttons
- [ ] 4.2 Create `feature/settings/SettingRowView.kt` — custom View wrapping the layout; exposes `label`, `value`, `isMasked`, `validator: (String) -> Boolean`, `errorMessage`, `onSave: (String) -> Unit`, `keyboardType` properties
- [ ] 4.3 Implement edit/read-only state toggle: tapping Edit shows the text field and Save/Cancel buttons; Save calls `onSave` only when valid; Cancel restores previous display value
- [ ] 4.4 Implement masking: in read-only mode, when `isMasked = true`, display bullet characters for all but the last 4 characters
- [ ] 4.5 Implement `clearOnEdit`: when `isMasked = true` the text field is cleared when entering edit mode
- [ ] 4.6 Write Robolectric tests for `SettingRowView` covering: read-only display, edit mode toggle, validation error display, save callback, cancel restores value, masking, clear-on-edit behaviour; include ATF accessibility checks

## 5. Settings Screen — Layout and Sections

- [ ] 5.1 Update `fragment_settings.xml` to use a `ScrollView` containing three `MaterialCardView` sections: "Solar Array Settings", "App Settings", "Configuration"
- [ ] 5.2 Add `SettingRowView` instances to "Solar Array Settings": EMA App ID, EMA App Secret, EMA System ID, EMA ECU ID, System Capacity (with kW suffix)
- [ ] 5.3 Add Language row (existing click-to-dialog pattern), Display Mode row (same click-to-dialog pattern, three options: System/Light/Dark), a `SwitchMaterial` for Notifications Enabled, and a `SettingRowView` for Historic Data Days (with days suffix) to "App Settings"
- [ ] 5.4 Add a `SettingRowView` for Base URL to "Configuration" with a "Reset to default" icon button that writes `https://api.apsystemsema.com:9282/user/api/v2/` to the repository and refreshes the displayed value
- [ ] 5.5 Add Import, Export, and Factory Reset buttons to "Configuration" section
- [ ] 5.6 Add all new string resources (labels, error messages, section headers, button text, dialog text) to `strings.xml` and `strings-de.xml`

## 6. Settings Screen — Fragment Logic

- [ ] 6.1 Update `SettingsFragment.onViewCreated` to wire each `SettingRowView` to its `SettingsRepository` getter (initial value) and setter (`onSave`)
- [ ] 6.2 Wire Display Mode row: read initial value; on dialog selection call `AppCompatDelegate.setDefaultNightMode()` and persist via repository (same pattern as Language)
- [ ] 6.3 Wire Notifications toggle: read initial value from repository; persist on toggle change
- [ ] 6.4 Wire Import button: launch `ActivityResultContracts.GetContent("application/json")`; on result read file content; attempt `JSONObject` parse — if valid call `repository.importFromJson()` directly; if not valid show PIN dialog and call `SettingsCrypto.decrypt()` before importing; show error snackbar on wrong PIN or unreadable file
- [ ] 6.5 Wire Export button: show dialog with two options ("No encryption" / "Encrypt with PIN"); if no encryption launch `ActivityResultContracts.CreateDocument` and write plain JSON; if PIN chosen show 4-digit PIN entry dialog then launch file picker and write `SettingsCrypto.encrypt(json, pin)`; suggested filename `ema-companion-settings.json` in both cases
- [ ] 6.6 Wire Factory Reset button: show confirmation `AlertDialog`; on confirm call `repository.clearAll()` and any data-layer clear (no-op placeholder if data layer not yet implemented); refresh all field displays
- [ ] 6.7 Apply persisted display mode in `MainActivity.onCreate()` before `setContentView()` by reading from `SettingsRepository` and calling `AppCompatDelegate.setDefaultNightMode()`
- [ ] 6.8 Write Robolectric tests for `SettingsFragment` covering: all fields display stored values on load, Notifications toggle persists, Factory Reset with cancel makes no changes; include ATF accessibility checks

## 7. Unconfigured App State

- [ ] 7.1 Add `isConfigured(): Boolean` to `SettingsRepository` — returns true only when EMA App ID, App Secret, System ID, ECU ID, and System Capacity all hold non-empty/non-sentinel values
- [ ] 7.2 In `MainActivity.onCreate()`, after applying dark mode: if `!repository.isConfigured()`, navigate to the Settings destination and disable all other `BottomNavigationView` menu items
- [ ] 7.3 In `SettingsFragment`, after each successful field save, call `repository.isConfigured()` and re-enable bottom navigation items if the app is now fully configured
- [ ] 7.4 Write unit tests for `isConfigured()` covering: all fields present (true), each required field missing individually (false)
- [ ] 7.5 Write Robolectric test for `MainActivity` covering: unconfigured state shows Settings and disables other nav items; configured state leaves nav items enabled

## 8. Lint and Localization

- [ ] 8.1 Run `./gradlew ktlintCheck` and fix any violations
- [ ] 8.2 Verify no hardcoded strings remain in layouts or Kotlin source (`./gradlew lint`)
- [ ] 8.3 Verify German translations in `strings-de.xml` are complete for all new string resources
