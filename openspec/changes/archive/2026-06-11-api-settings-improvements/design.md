## Context

The Settings screen has three sections. The third is currently called "Configuration" and holds Base URL, Import, Export, and Factory Reset. Adding an API Request Limit setting (with a monthly consumption progress bar) makes it entirely API-focused, so the rename to "API Settings" improves clarity. The limit and progress bar are new UI elements; the consumption data source is not yet implemented, so the progress bar is seeded with a hardcoded value (800) for this change.

## Goals / Non-Goals

**Goals:**
- Rename the "Configuration" section header to "API Settings" everywhere (UI, specs, user guide)
- Add `apiRequestLimit` to `SettingsRepository` (default 1000, required, part of `isConfigured()`)
- Add `apiRequestLimit` to the import/export JSON payload (field count: 10 → 11)
- Display an `ApiRequestLimitRow` using the existing `SettingRowView` pattern with suffix "req/month" and `isRequired = true`
- Display a `LinearProgressIndicator` beneath the limit row showing consumed/limit ratio (hardcoded consumed = 800)

**Non-Goals:**
- Fetching or persisting real API consumption data
- Alerting the user when the limit is approached or exceeded
- Validating that the limit is not exceeded before making API calls

## Decisions

### Reuse `SettingRowView` for API Request Limit
The existing `SettingRowView` already handles label, value display, edit/save/cancel flow, suffix, required hint, and validation. Using it for the new limit field keeps the UI and test surface consistent with all other editable settings. Alternative (custom layout) rejected — no unique behaviour warrants it.

### `LinearProgressIndicator` for consumption bar
Material Design 3's `LinearProgressIndicator` matches the app's design system and requires no extra dependency. It is placed directly in the settings layout beneath the limit row, visible at all times (not edit-mode-only). Progress value = consumed / limit, clamped to [0, 1].

### Hardcode consumed = 800
The consumption data source is not part of this change. A hardcoded value of 800 makes the progress bar visible and testable without coupling to unbuilt infrastructure. The field used (`hardcodedConsumedRequests = 800`) should be easy to find and replace when real data arrives.

### Include `apiRequestLimit` in `isConfigured()`
Consistent with `systemCapacity` and `historicDataDays`, the limit is a required app setting. The sentinel for "not yet set" is `-1` (same pattern as other numeric required fields). Default in SharedPreferences is `-1`; the UI pre-populates the edit field with `1000` as the suggested default.

### Import/export payload
`SettingsRepository.exportToJson()` and `importFromJson()` already enumerate fields explicitly. Adding `apiRequestLimit` as a new `Int` entry follows the same pattern as `historicDataDays`. No migration is needed — old import files that omit `apiRequestLimit` leave the stored value unchanged (merge semantics).

## Risks / Trade-offs

- **Hardcoded consumption misleads users** → Clearly documented as temporary; the value is a named constant so it is easy to locate and replace.
- **`isConfigured()` change delays first-use for upgrades** → Existing installs will not have `apiRequestLimit` set. They will be directed to Settings on next launch until they save a value. Acceptable: the Settings screen pre-populates the default (1000) making this a one-tap resolution.
- **Section rename touches multiple specs and the user guide** → Low risk — it is a string replacement. Delta specs capture the requirement change; user guide is updated by `write-user-guide` skill.
