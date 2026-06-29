## 0. Fix Phase 1: Push notification fires on status change only

- [x] 0.1 Add `lastNotifiedStatus: String?` field to `ema_module_health` SharedPreferences (alongside existing `status`)
- [x] 0.2 Update `ModuleHealthWorker`: post push notification only when computed status differs from `lastNotifiedStatus`; update `lastNotifiedStatus` after posting
- [x] 0.3 Update `SettingsFragment.showFactoryResetDialog()` to clear `lastNotifiedStatus` from `ema_module_health` prefs
- [x] 0.4 Update `ModuleHealthRepository.resetThrottle()` (or a new `clear()`) to also remove `lastNotifiedStatus`
- [x] 0.5 Update Robolectric tests for `ModuleHealthWorker`: assert notification fires on GREEN→YELLOW, not on repeated YELLOW→YELLOW
- [x] 0.6 Run `./gradlew testDebugUnitTest --rerun` — all existing tests pass

## 1. JavaMail Dependency

- [x] 1.1 Add `com.sun.mail:android-mail:1.6.7` and `com.sun.mail:android-activation:1.6.7` to `gradle/libs.versions.toml` and `app/build.gradle.kts`
- [x] 1.2 Add `com.icegreen:greenmail:2.0.1` as `testImplementation` for SMTP integration tests
- [x] 1.3 Verify build succeeds: `./gradlew assembleDebug`

## 2. SMTP Credential Storage

- [x] 2.1 Add to `SettingsRepository`: `getEmailAddress(): String`, `setEmailAddress(v: String)`, `getEmailAppPassword(): String`, `setEmailAppPassword(v: String)`, `deleteEmailCredentials()`, `isEmailConfigured(): Boolean`
- [x] 2.2 Store both fields in EncryptedSharedPreferences (same backing store as EMA credentials)
- [x] 2.3 Add `emailAlertsEnabled: Boolean` toggle field to `SettingsRepository` (plain SharedPreferences — not sensitive)
- [x] 2.4 Write unit test: verify address and password stored, retrieved, deleted correctly
- [x] 2.5 Write unit test: verify `isEmailConfigured()` returns false when either field is blank

## 3. Email Sender

- [x] 3.1 Create `core/email/EmailSender` interface: `fun send(to: String, subject: String, body: String): EmailResult`; `EmailResult` = `sealed class` with `Success`, `AuthFailure`, `NetworkError`
- [x] 3.2 Create `core/email/GmailSmtpEmailSender` implementing `EmailSender`; constructor takes `from: String, appPassword: String`; sends via `smtp.gmail.com:587` with STARTTLS using JavaMail
- [x] 3.3 Implement `GmailSmtpEmailSender.testConnection(): EmailResult` (sends no email — just opens and closes an authenticated session); used by Settings to verify credentials on save
- [x] 3.4 Write unit tests for `EmailResult` sealed class (exhaustive `when` coverage) — covered by GreenMail tests which exercise all three result types
- [x] 3.5 Write GreenMail integration test: start in-process SMTP server, verify `GmailSmtpEmailSender` delivers a message with correct `To`, `Subject`, and body
- [x] 3.6 Write GreenMail integration test: verify `AuthFailure` returned on wrong credentials
- [x] 3.7 Write GreenMail integration test: verify `NetworkError` returned when server unreachable (wrong port)

## 4. Email Content Builder

- [x] 4.1 Add email template strings to `values/strings.xml` (EN)
- [x] 4.2 Add same strings to `values-de/strings.xml` (DE)
- [x] 4.3 Create `core/email/EmailContentBuilder`
- [x] 4.4 Write unit tests (EN qualifiers)
- [x] 4.5 Write unit tests (DE qualifiers)

## 5. lastEmailedStatus Field

- [x] 5.1 Add `lastEmailedStatus: String?` to `ema_module_health` SharedPreferences
- [x] 5.2 Clear on factory reset (handled by PREFS_HEALTH.clear() in showFactoryResetDialog)
- [x] 5.3 Clear on import/credential change (handled by resetThrottle() via refreshAllDisplayedValues)
- [x] 5.4 Write unit tests (getLastEmailedStatus, setLastEmailedStatus, resetThrottle_clearsLastEmailedStatus)
- [x] 5.5 resetThrottle clears lastEmailedStatus — same path as EMA credential change test

## 6. Settings UI — Email Alerts

- [x] 6.1 Add `email_alerts_toggle_title` = `"Email Alerts"` and DE equivalent to `strings.xml`; add `email_alerts_enabled_for` = `"Email alerts enabled for: %1$s"` and DE equivalent
- [x] 6.2 Add "Email Alerts" toggle (`SwitchPreference` or inline toggle row) to the Settings screen, below API Settings section
- [x] 6.3 Add email setup sub-screen (fragment or full-screen dialog) shown when toggle is enabled and credentials not yet configured; layout contains:
  - Static instruction text (matches spec: 2-minute notice, step-by-step guide, prerequisite note about 2-Step Verification)
  - "Open Google Account ↗" button that fires `Intent(Intent.ACTION_VIEW, Uri.parse("https://myaccount.google.com/apppasswords"))`
  - Gmail address input field
  - App Password input field (masked, last 4 chars visible — same `SettingRowView` pattern as EMA App Secret)
  - "Verify & Save" button
- [x] 6.4 "Verify & Save": calls `GmailSmtpEmailSender.testConnection()` on IO dispatcher; on `Success` saves credentials and navigates back; on failure shows inline error "Connection failed. Check your App Password and try again."
- [x] 6.5 When credentials are configured: toggle row shows "Email alerts enabled for: user@gmail.com"; tapping shows a "Disable Email Alerts" confirmation dialog that calls `deleteEmailCredentials()` and clears `lastEmailedStatus`
- [x] 6.6 Add "Email Alerts" section to `SettingsFragmentTest.setUp()` prefs clear list
- [x] 6.7 Write Robolectric test: toggle off by default; enabling toggle shows setup row
- [x] 6.8 Write Robolectric test: after saving credentials, Settings shows "enabled for: address"
- [x] 6.9 Write Robolectric test: disable flow shows confirmation dialog, deletes credentials on confirm
- [x] 6.10 Write Robolectric test: "Open Google Account" button fires correct `ACTION_VIEW` intent

## 7. Integration with ModuleHealthWorker

- [x] 7.1 Inject `EmailSender` and `EmailContentBuilder` into `ModuleHealthWorker` (via constructor or WorkerFactory); default to `GmailSmtpEmailSender` built from `SettingsRepository` credentials
- [x] 7.2 After computing new status, compare to `lastEmailedStatus`; if changed AND `isEmailConfigured()` AND `emailAlertsEnabled`: call `EmailSender.send()`, update `lastEmailedStatus` on `Success`
- [x] 7.3 On `AuthFailure`: log error, do NOT update `lastEmailedStatus` (retry next change), fall back to local notification
- [x] 7.4 On `NetworkError`: log error, do NOT update `lastEmailedStatus`, fall back to local notification
- [x] 7.5 Local push notification fires independently of email result (already in place from Phase 1 after task 0.x)
- [x] 7.6 Write integration test with `FakeEmailSender`: GREEN→YELLOW triggers one email send with correct subject
- [x] 7.7 Write integration test: YELLOW→YELLOW (same status) sends no email
- [x] 7.8 Write integration test: RED→GREEN sends recovery email
- [x] 7.9 Write integration test: email `AuthFailure` → `lastEmailedStatus` NOT updated → next status change retries
- [x] 7.10 Write integration test: email not configured → no `EmailSender.send()` call

## 8. Documentation

- [x] 8.1 Update `docs/user-guide/settings.md` with Email Alerts section: how to enable, what the setup steps are (condensed), what emails arrive and when, how to disable; invoke `write-user-guide` skill
- [ ] 8.2 Update `docs/notification-methods.md` to include SMTP email as the Phase 2 delivery mechanism
- [x] 8.3 Create ADR documenting: SMTP vs OAuth decision, status-change-only trigger, lastEmailedStatus/lastNotifiedStatus pattern, RED→GREEN-only recovery

## 9. Code Review & Polish

- [x] 9.1 Run `./gradlew ktlintCheck` and fix lint errors
- [x] 9.2 Verify all tests pass: `./gradlew testDebugUnitTest --rerun`
- [x] 9.3 Security review: confirm App Password never appears in Logcat, API log, or crash reports
- [x] 9.4 Verify no blocking calls on main thread: SMTP send and `testConnection()` run only on IO dispatcher

## 10. Real Device Testing

- [ ] 10.1 Test on real Android device: enter Gmail address and App Password, verify "Verify & Save" succeeds
- [ ] 10.2 Test email delivery: trigger status change (seed RED data), verify email arrives in Gmail inbox within 24h check cycle
- [ ] 10.3 Test language: switch app to German, verify next alert email is in German
- [ ] 10.4 Test disable flow: disable Email Alerts, verify no email on next status change
- [ ] 10.5 Test error case: enter wrong App Password, verify inline error shown on save, no credentials stored
