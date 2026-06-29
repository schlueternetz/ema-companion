## 1. String Resources

- [x] 1.1 Add `settings_section_api` string ("API Settings") to `values/strings.xml` and `values-de/strings.xml`
- [x] 1.2 Add `setting_api_request_limit_label` ("API Request Limit") and `setting_api_request_limit_suffix` ("req/month") strings to both locale files
- [x] 1.3 Add `setting_api_request_limit_description` ("Max EMA API calls per month") string to both locale files

## 2. SettingsRepository

- [x] 2.1 Add `API_REQUEST_LIMIT_KEY = "apiRequestLimit"` constant and `getApiRequestLimit(): Int` (default 1000) / `setApiRequestLimit(value: Int)` to `SettingsRepository`
- [x] 2.2 Leave `apiRequestLimit` out of `isConfigured()` — it always has a usable default (1000), so it is not a required field
- [x] 2.3 Include `apiRequestLimit` in `exportToJson()` and `importFromJson()`
- [x] 2.4 Write unit tests: `getApiRequestLimit_returnsDefault_whenNotSet`, `setApiRequestLimit_persists`, `exportToJson_includesApiRequestLimit`, `importFromJson_setsApiRequestLimit`

## 3. Settings Layout

- [x] 3.1 Rename the "Configuration" section header view's string reference to `@string/settings_section_api` in the Settings layout XML
- [x] 3.2 Add a `SettingRowView` for API Request Limit (id: `settingApiRequestLimit`) below the section header and above Base URL, with `keyboardType="number"`; set `suffix = "req/month"` in `wireApiRequestLimit()` so it renders outside the text input via `TextInputLayout.suffixText` (same pattern as System Capacity / kW)
- [x] 3.3 Add a `LinearProgressIndicator` (id: `apiRequestProgressBar`) immediately below `settingApiRequestLimit`

## 4. SettingsFragment Wiring

- [x] 4.1 Add `wireApiRequestLimit()` method: set `suffix`, show default 1000 when unset, validate 1–2,678,400, call `settingsRepository.setApiRequestLimit()` on save, provide a reset-to-default action (disabled while editing)
- [x] 4.2 Set `apiRequestProgressBar` progress in `onViewCreated`: `consumed = 800`, `progress = consumed.toFloat() / limit.toFloat()` (or 0f when limit is sentinel), clamped to [0, 1]
- [x] 4.3 Refresh progress bar after API Request Limit is saved (re-compute and set progress)
- [x] 4.4 Write Robolectric tests: `apiRequestLimit_showsDefault_whenUnset`, `apiRequestLimit_savesValue`, `apiRequestLimit_rejectsOutOfRange`, `progressBar_showsConsumedRatio`

## 5. MainActivityTest & configureSettings Helper

- [x] 5.1 Add `apiRequestLimit` (e.g. 1000) to the `configureSettings()` helper in `MainActivityTest` so that existing configured-state tests continue to pass
