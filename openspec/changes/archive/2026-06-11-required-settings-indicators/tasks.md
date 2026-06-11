## 1. SettingRowView — required indicator

- [x] 1.1 Add `isRequired: Boolean` property to `SettingRowView`; when `true` and `value` is empty, set `setting_value` hint to the string "Required"
- [x] 1.2 Clear the hint (set to `null` or `""`) when `isRequired` is `false` or when `value` becomes non-empty
- [x] 1.3 Add Robolectric tests: required+empty shows hint, required+saved clears hint, non-required field never shows hint
- [x] 1.4 Add ATF accessibility check to the new test cases (reuse existing `hasNoAccessibilityErrors` pattern)

## 2. SettingsFragment — mark required fields

- [x] 2.1 Set `isRequired = true` on the five solar-array rows in their respective `wire*()` methods: `wireEmaAppId`, `wireEmaAppSecret`, `wireEmaSystemId`, `wireEmaEcuId`, `wireSystemCapacity`
- [x] 2.2 Verify that after save, the hint disappears automatically (no extra code needed — `value` setter drives it; confirm with manual test on emulator)

## 3. Strings

- [x] 3.1 Add `<string name="setting_row_required_hint">Required</string>` to `values/strings.xml`
- [x] 3.2 Add German translation to `values-de/strings.xml`

## 4. Lint and spec

- [x] 4.1 Run `./gradlew ktlintCheck` — fix any issues
- [x] 4.2 Run `./gradlew testDebugUnitTest` — all tests green
