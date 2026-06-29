## Why

The Settings screen's third section is named "Configuration" — a label that no longer fits as the section grows to include API-specific controls. Introducing an API Request Limit setting (with a consumption progress bar) makes the section clearly API-focused and gives users visibility into their monthly API usage before they hit the limit.

## What Changes

- Rename the "Configuration" settings section header from "Configuration" to "API Settings"
- Add a new setting: **API Request Limit** — a positive integer (1–2,678,400) representing the maximum number of EMA API calls per month, defaulting to 1000; not a required field (it always has a usable default, like Historic Data Days); included in the import/export payload
- Add a progress bar beneath the API Request Limit field showing how many requests have been consumed this month (consumption logic is out of scope; hardcode to 800 for now)

## Capabilities

### New Capabilities
- `api-request-limit`: The API Request Limit setting (optional, default 1000, range 1–2,678,400, unit: requests/month) and the monthly consumption progress bar displayed beneath it

### Modified Capabilities
- `settings`: "Configuration" section renamed to "API Settings"; API Request Limit field and consumption progress bar added to that section
- `settings-import-export`: field count increases from 10 to 11 (adding `apiRequestLimit`); section button references updated from "Configuration" to "API Settings"

## Impact

- `SettingsFragment` — rename section header, add API Request Limit row and progress bar
- `SettingsRepository` — add `getApiRequestLimit()` / `setApiRequestLimit()` with default 1000 (not included in `isConfigured()`); include in `exportToJson()` and `importFromJson()`
- `openspec/specs/settings/spec.md` — delta spec to reflect section rename and new field
- `openspec/specs/settings-import-export/spec.md` — delta spec to reflect 11 settings and section rename
- String resources (`values/strings.xml`, `values-de/strings.xml`) — new label and description strings
