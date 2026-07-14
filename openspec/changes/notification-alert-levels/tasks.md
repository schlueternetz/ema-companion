## 1. Core enum & repository (AI-TDD)

- [ ] 1.1 Add `AlertLevel { OFF, ALERTS_ONLY, ALL }` enum in `core/` (mirrors `HomeTile`/`HomeWidget` placement)
- [ ] 1.2 Write failing `SettingsRepositoryTest` cases for `getNotificationLevel()`/`setNotificationLevel()` (default Alerts Only, persists, round-trips)
- [ ] 1.3 Write failing `SettingsRepositoryTest` cases for `getEmailAlertLevel()`/`setEmailAlertLevel()` (default Off, persists, round-trips)
- [ ] 1.4 Write failing `SettingsRepositoryTest` cases for legacy boolean migration on read (`notificationsEnabled=true/false` → Alerts Only/Off; `emailAlertsEnabled=true/false` → Alerts Only/Off; new key wins once written)
- [ ] 1.5 Implement `getNotificationLevel()`/`setNotificationLevel()` and `getEmailAlertLevel()`/`setEmailAlertLevel()` with the lazy read-time migration from the legacy boolean keys; remove `getNotificationsEnabled()`/`setNotificationsEnabled()`/`getEmailAlertsEnabled()`/`setEmailAlertsEnabled()`
- [ ] 1.6 Update `exportToJson()` to write the new level keys; update `importFromJson()` to read the new level keys, with one-directional acceptance of the legacy boolean keys when the level key is absent
- [ ] 1.7 Update `SettingsRepositoryTest`'s existing export/import tests that reference `notificationsEnabled`/`emailAlertsEnabled` for the new key names
- [ ] 1.8 Run `SettingsRepositoryTest`, confirm green

## 2. Worker gating logic (AI-TDD)

- [ ] 2.1 Write failing unit tests for a `shouldAlert(level, previousStatus, newStatus)` pure function: Off always false; Alerts Only true only on change; All always true
- [ ] 2.2 Implement `shouldAlert()` (shared by both push and email paths in `ModuleHealthWorker`)
- [ ] 2.3 Write failing `ModuleHealthWorkerTest` cases for push notification gating across all three `notificationLevel` values, including the previously-missing Off case and the new All-fires-unconditionally case
- [ ] 2.4 Wire `settings.getNotificationLevel()` + `shouldAlert()` into the push dispatch branch of `doWork()`
- [ ] 2.5 Write failing `ModuleHealthWorkerTest` cases for email gating across all three `emailAlertLevel` values (replacing the existing boolean-based `getEmailAlertsEnabled()` tests), including unconfigured-email-never-sends
- [ ] 2.6 Wire `settings.getEmailAlertLevel()` + `shouldAlert()` into the email dispatch branch of `doWork()`
- [ ] 2.7 Run `ModuleHealthWorkerTest`, confirm green

## 3. Notifier GREEN path (AI-TDD)

- [ ] 3.1 Write failing `ModuleHealthNotifierTest` cases: GREEN + `postOnGreen=true` posts a confirmation notification; GREEN + `postOnGreen=false` still cancels (unchanged); UNKNOWN always cancels regardless of `postOnGreen`; consecutive GREEN posts replace rather than stack (same `NOTIFICATION_ID`)
- [ ] 3.2 Add `postOnGreen: Boolean` parameter to `ModuleHealthNotifier.notify()` and implement the GREEN posting branch
- [ ] 3.3 Add `notification_module_health_green_title`/`notification_module_health_green_text` string resources (EN + DE, `values/strings.xml` and `values-de/strings.xml`)
- [ ] 3.4 Update `ModuleHealthWorker.doWork()` to pass `postOnGreen = (notificationLevel == AlertLevel.ALL)`
- [ ] 3.5 Run `ModuleHealthNotifierTest`, confirm green

## 4. Settings UI

- [ ] 4.1 Add string resources (EN + DE): notification level dialog title + 3 option labels; email alert level dialog title + 3 option labels (mirror `language_option_*`/`display_mode_option_*` naming)
- [ ] 4.2 Update `fragment_settings.xml`: replace `settings_notifications_switch` (`MaterialSwitch`) with a label + value row matching the Language/Display Mode row style
- [ ] 4.3 Update `fragment_settings.xml`: replace `settings_email_alerts_switch` with an equivalent value row above the existing setup/status rows
- [ ] 4.4 Write failing `SettingsFragmentTest` cases for the notification level row: reflects stored value on open, dialog shows 3 options, selecting an option persists immediately
- [ ] 4.5 Replace `wireNotifications()` with a value-row + `AlertDialog.Builder().setItems(...)` picker (pattern from `showLanguageDialog()`/`showDisplayModeDialog()`), reading/writing `notificationLevel`
- [ ] 4.6 Write failing `SettingsFragmentTest` cases for the email alert level row: reflects stored value on open; selecting non-Off without saved credentials reveals setup form; selecting non-Off with saved credentials shows status row; selecting Off hides setup but keeps status row visible when configured
- [ ] 4.7 Replace `wireEmailAlerts()`'s switch listener with the level-dialog picker, retargeting `isChecked`/`setEmailAlertsEnabled` call sites (`verifyAndSaveEmailCredentials()`, `showClearCredentialsDialog()`, `updateEmailAlertsDisplay()`) to `emailAlertLevel`
- [ ] 4.8 Update `refreshAllDisplayedValues()` to refresh both new level rows instead of `notificationsSwitch.isChecked = ...`
- [ ] 4.9 Update `SettingsFragmentTest`'s existing switch-based assertions (checked/unchecked) and the factory-reset/import prefs assertions (`notificationsEnabled` → new key) for the new UI
- [ ] 4.10 Run `ktlintCheck`, fix any violations

## 5. Docs

- [ ] 5.1 Invoke the `write-adr` skill to update ADR-008: the "status-change-only trigger" decision is superseded by the level-based trigger; document `AlertLevel`, the shared `shouldAlert()` rule, and the GREEN "All" notification/email addition
- [ ] 5.2 Invoke the `write-user-guide` skill to update `docs/user-guide/settings.md` for the new notification level and email alert level pickers

## 6. Verification

- [ ] 6.1 Update `maestro/email-alerts.yaml` to drive the new email alert level dialog instead of tapping `settings_email_alerts_switch`
- [ ] 6.2 Run `./gradlew testDebugUnitTest`, confirm all green
- [ ] 6.3 Run `./gradlew ktlintCheck`, confirm clean
- [ ] 6.4 Run `/qa` (unit/Robolectric + ktlint + debug build/install + full Maestro suite on the emulator) and confirm all flows pass before marking this change done
