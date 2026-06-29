## 1. String Resources

- [x] 1.1 Add English hint strings for all five Solar Array Settings fields to `res/values/strings.xml` (App ID, App Secret, System ID, ECU ID, System Capacity)
- [x] 1.2 Add German translations for the same five hint strings to `res/values-de/strings.xml`

## 2. Layout

- [x] 2.1 Add an `ImageButton` with `@drawable/ic_info` to `res/layout/view_setting_row.xml`, positioned between the value `TextView` and the edit `ImageButton`, with `android:visibility="gone"` as default

## 3. SettingRowView — Tests First (AI-TDD)

- [x] 3.1 Write a failing Robolectric test: info button is `GONE` when `hintText` is null
- [x] 3.2 Write a failing Robolectric test: info button is `VISIBLE` when `hintText` is set
- [x] 3.3 Write a failing Robolectric test: tapping info button shows an `AlertDialog` with correct title and body text
- [x] 3.4 Write a failing Robolectric test: info button is `GONE` during edit mode (after entering edit)
- [x] 3.5 Write a failing Robolectric test: info button is `VISIBLE` again after cancelling edit

## 4. SettingRowView — Implementation

- [x] 4.1 Add `infoButton: ImageButton` field and wire it from the inflated layout in `SettingRowView.kt`
- [x] 4.2 Add `hintText: String?` property; setter sets button visibility and registers a click listener that shows an `AlertDialog` titled with `label` and body from `hintText`
- [x] 4.3 Update `enterEditMode()` to hide the info button alongside the edit button
- [x] 4.4 Update `exitEditMode()` to restore info button visibility based on `hintText`
- [x] 4.5 Verify all tests from step 3 now pass

## 5. SettingsFragment Wiring

- [x] 5.1 Set `hintText` on `setting_ema_app_id` using the App ID hint string
- [x] 5.2 Set `hintText` on `setting_ema_app_secret` using the App Secret hint string
- [x] 5.3 Set `hintText` on `setting_ema_system_id` using the System ID hint string
- [x] 5.4 Set `hintText` on `setting_ema_ecu_id` using the ECU ID hint string
- [x] 5.5 Set `hintText` on `setting_system_capacity` using the System Capacity hint string

## 6. Quality Gates

- [x] 6.1 Run `./gradlew ktlintCheck` and fix any style violations
- [x] 6.2 Run `./gradlew testDebugUnitTest` and confirm all tests pass
- [x] 6.3 Confirm Robolectric ATF accessibility checks pass for the updated `SettingRowView` (content description on info button)

## 7. User Guide

- [x] 7.1 Run the `write-user-guide` skill to update `docs/user-guide/user-guide.md` with the following content under Solar Array Settings:
  - A **prerequisite note**: OpenAPI access must be enabled in the EMA app (Settings → OpenAPI Service) before the Developer Authorization settings (App ID / App Secret) appear.
  - A **"Where to find these values"** subsection listing the EMA app navigation path for each field (per the Field Hint Content table in `proposal.md`).
  - A **warning**: If the EMA API is not used for 6 consecutive months, APsystems may revoke API access; if the Companion app stops working, verify OpenAPI access is still active in the EMA app.
