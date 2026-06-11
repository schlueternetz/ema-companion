## Context

The app uses a View-based XML UI (not Compose). The existing `SettingsRepository` stores one field (`language`) via `EncryptedSharedPreferences`. `SettingsFragment` renders a single clickable row. Both need significant extension to support 9 new fields, inline editing, import/export, and factory reset. `MainActivity` is also touched for dark mode cold-start apply and the unconfigured-app navigation gate.

## Goals / Non-Goals

**Goals:**
- Add all 9 settings (including language) to `SettingsRepository` with typed getters/setters and constants
- Render each new field as an inline-editable row in `SettingsFragment` (display value → tap Edit → text field with Save/Cancel)
- Validate each field in the UI before allowing save; show inline error text on invalid input
- Import all 9 settings from a user-selected JSON file via the Storage Access Framework
- Export all 9 settings to a user-chosen JSON file via the Storage Access Framework
- Factory Reset: confirmation dialog, then clear all settings and any locally stored production data

**Non-Goals:**
- Triggering API calls or data refreshes when credentials change (that is a future concern)
- Any server-side or remote config mechanism
- Notification scheduling or delivery logic (only the on/off preference is stored here)

## Decisions

### 1. Extend `SettingsRepository` rather than split it

All 9 settings belong to one `EncryptedSharedPreferences` file. A single repository is simpler and sufficient — there is no reason to split at this scale. If a `core/` data layer is introduced later, it can absorb this class at that point.

**Alternative considered:** separate repositories per domain (credentials, system config, preferences). Rejected — over-engineering for the current feature count.

### 2. Validation lives in the UI layer only

Regex and range checks are enforced in `SettingsFragment` before calling `repository.set*()`. The repository stores and retrieves values without re-validating. This matches the single-entry-point model: settings are only written via the Settings screen.

**Alternative considered:** validate in the repository and throw. Rejected — the repository has no access to string resources for error messages, and double-validation adds noise without benefit.

### 3. Inline edit rows implemented as a reusable custom View

The old app used a Compose `EditableSetting` composable. The new app uses XML layouts. Rather than copy-pasting a large block of XML and Fragment code for each of 8 fields, a `SettingRowView` custom View encapsulates the label, value display, text field, edit/save/cancel icons, and error text. The fragment inflates one `SettingRowView` per field and configures it via properties (`label`, `validator`, `onSave`, `isMasked`).

**Alternative considered:** a plain `RecyclerView` with adapter. Rejected — the list is small and static; a RecyclerView adds complexity without benefit.

### 4. Import/Export via Storage Access Framework (SAF)

`ActivityResultContracts.GetContent` (import) and `ActivityResultContracts.CreateDocument` (export) give the user full control over file location without requiring `READ_EXTERNAL_STORAGE` permission. This matches Android 12+ best practice.

JSON serialization uses `org.json.JSONObject` (already on the classpath via the Android SDK — no new dependency).

Import is a merge, not a replace: fields present in the JSON overwrite the stored value; fields absent are left unchanged. Export always writes all 10 fields. The default suggested filename for export is `ema-companion-settings.json`.

**Alternative considered:** `READ/WRITE_EXTERNAL_STORAGE`. Rejected — deprecated, requires permission, worse UX.

### 5. Test settings injection via `importFromJson` + resource fixtures

Tests configure `SettingsRepository` by calling `importFromJson(json)` directly — the same merge-based import used at runtime. This avoids SAF file pickers and device-specific paths entirely.

Fixture JSON files live in `src/test/resources/settings/` and contain only the fields the test needs to override (e.g. `{"baseUrl": "http://localhost:8080"}`). A `SettingsTestLoader` helper in `src/test/` reads a named resource file and calls `importFromJson`, returning the configured repository ready for use in test setup.

Because import is a merge, a fixture only needs to specify the fields it cares about — other settings keep their defaults. This means a stub-server fixture only needs `baseUrl`; it does not need to supply credentials or any other field.

**Fixture files must not contain real credentials.** The pre-commit hook already blocks API keys in staged diffs; fixture files should use placeholder values (e.g. `"emaAppId": "test000000000000000000000000000000"`).

**Alternative considered:** A separate `SettingsRepository` constructor overload that accepts a pre-populated map. Rejected — `importFromJson` already does this and keeping one write path simplifies testing.

### 6. Export encryption: AES-256-GCM with PIN-derived key, no salt

The encryption key is `SHA-256(PIN.toByteArray())` — derived entirely from the 4-digit PIN with no random salt, no PBKDF2, and no device-specific state. A random 12-byte IV is generated per export and prepended to the ciphertext before base64 encoding. File format: `base64(IV || ciphertext)`.

This design is intentionally weak against brute force (10,000 PINs × one SHA-256 + AES-GCM op ≈ milliseconds on a desktop) but satisfies the stated goals: the file is not plain text, and decryption requires only the PIN — no keyfiles, no device setup, no pairing.

AES-GCM's authentication tag provides the "wrong PIN" signal without a separate checksum: a bad PIN produces an `AEADBadTagException`, which the app catches and surfaces as an error message.

Auto-detection on import: the app attempts `JSONObject(fileContent)` first; if that succeeds the file is unencrypted; otherwise it is treated as encrypted and the PIN dialog is shown.

**Alternative considered:** PBKDF2 key derivation (salt stored in file). Rejected — the user explicitly does not require brute-force resistance, and the added complexity (salt handling, iteration count choice) is not justified.

**Alternative considered:** A fixed magic-byte header to distinguish encrypted from plain files. Rejected — trying JSON parse first is simpler and requires no format versioning.

### 6. Factory Reset scope

Factory Reset clears the entire `EncryptedSharedPreferences` file (all 9 settings) and any locally stored production data tables. The exact set of data tables to clear will be a no-op placeholder in this change and extended when data storage is implemented.

Confirmation dialog is required; the action is irreversible.

## Risks / Trade-offs

- **Encrypted prefs not readable in Robolectric** → the existing fallback to plain `SharedPreferences` is retained and all tests run against plain prefs (via the constructor overload); no test-environment regression.
- **SAF file picker not available in unit tests** → import/export logic is extracted into a pure function `importFromJson(json: String): Map<String, Any>` and `exportToJson(settings: Map<String, Any>): String` that can be unit-tested without a file picker.
- **`SettingRowView` adds UI complexity** → mitigated by keeping it a single-responsibility class; Robolectric tests cover each field's validation and save path.
- **PIN-encrypted export is not brute-force resistant** → 10,000 combinations, no key stretching. Accepted — credentials are read-only API access to a solar array; the encryption goal is "not plain text" rather than "resistant to a motivated attacker with the file."
- **Unencrypted export option remains** → required for the local stub-server testing workflow where typing a PIN each run is impractical. Users should be aware the plain export contains credentials in clear text.
