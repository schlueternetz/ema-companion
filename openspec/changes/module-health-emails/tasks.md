## 1. Google Sign-In & OAuth Setup

- [ ] 1.1 Add Google Play Services SDK dependencies to gradle/libs.versions.toml (Google Auth, Gmail API)
- [ ] 1.2 Create Google OAuth 2.0 Client ID for Android app in Google Cloud Console
- [ ] 1.3 Configure OAuth consent screen in Google Cloud Console with app name, scopes (Gmail send)
- [ ] 1.4 Create `GoogleSignInManager` class to handle OAuth 2.0 authentication flow
- [ ] 1.5 Implement Google Sign-In launcher activity / fragment (Settings > Email Alerts)
- [ ] 1.6 Write unit test: mock Google Sign-In, verify token returned
- [ ] 1.7 Write Robolectric test: verify Sign-In UI displays, button tappable

## 2. OAuth Token Storage & Security

- [ ] 2.1 Add OAuth token storage methods to `SettingsRepository`: `encryptedGetGoogleToken()`, `setGoogleToken()`, `deleteGoogleToken()`, `getGoogleUserEmail()`
- [ ] 2.2 Implement token storage in EncryptedSharedPreferences (AES256-GCM)
- [ ] 2.3 Add token expiry tracking (store expiry timestamp alongside token)
- [ ] 2.4 Implement token refresh logic: check expiry before use, silently refresh if needed
- [ ] 2.5 Write unit test: verify token stored encrypted, retrieved correctly
- [ ] 2.6 Write unit test: verify token refresh on expiry, fallback on failure

## 3. Gmail Email Sending

- [ ] 3.1 Create `GmailEmailSender` class to send emails via Gmail API
- [ ] 3.2 Implement email composition: MIME format for subject, body, sender, recipient
- [ ] 3.3 Implement Gmail API call (send message on behalf of authenticated user)
- [ ] 3.4 Implement error handling: network errors, rate limits, auth failures, fallback to local notification
- [ ] 3.5 Add retry logic: on transient failures, queue for next check (12h)
- [ ] 3.6 Write unit test: mock Gmail API, verify email sent with correct content
- [ ] 3.7 Write unit test: verify error handling on API failure, fallback to local notification

## 4. Email Content Templates (Localization)

- [ ] 4.1 Add email template strings to strings.xml (EN) and values-de/strings.xml (DE):
  - Yellow alert subject and body
  - Red alert subject and body
  - Recovery subject and body
  - Partial recovery (RED→YELLOW) subject and body
- [ ] 4.2 Create `EmailContentBuilder` class to generate personalized email body with module details
- [ ] 4.3 Include module ID, offline duration, system context in email body
- [ ] 4.4 Implement sorting: list offline modules by duration (longest first)
- [ ] 4.5 Write unit test: verify templates render correctly in both languages
- [ ] 4.6 Write unit test: verify module details formatted correctly in email

## 5. Settings UI for Email Alerts

- [ ] 5.1 Add "Email Alerts" section to Settings screen (SettingsFragment)
- [ ] 5.2 Implement "Sign In with Google" button (launches GoogleSignInManager)
- [ ] 5.3 Implement display of signed-in email address: "Email alerts enabled for: user@gmail.com"
- [ ] 5.4 Implement "Disable Email Alerts" button with confirmation dialog
- [ ] 5.5 Add error message display for sign-in failures
- [ ] 5.6 Add loading indicator during sign-in process
- [ ] 5.7 Write Robolectric test: verify sign-in UI state, email address display, disable button
- [ ] 5.8 Write Robolectric test: verify token persists across app restart

## 6. Integration with ModuleHealthWorker

- [ ] 6.1 Modify `ModuleHealthWorker` to trigger email send on status change (if user is signed in)
- [ ] 6.2 Add state-change detection: compare new status to persisted status from Phase 1
- [ ] 6.3 Call `GmailEmailSender` only on actual status changes (GREEN→YELLOW, YELLOW→RED, etc.)
- [ ] 6.4 Ensure local notification sent regardless of email success/failure
- [ ] 6.5 Add logging: email sent, errors, token refresh, fallback to local
- [ ] 6.6 Write integration test: mock Gmail API, verify email sent on status change
- [ ] 6.7 Write integration test: verify no email on same status, email on recovery

## 7. Testing & Validation

- [ ] 7.1 Test OAuth flow on emulator with real Google Account
- [ ] 7.2 Test email sending via Gmail API on emulator (verify in Gmail Sent folder)
- [ ] 7.3 Test token expiry handling: wait for expiry (or mock), verify refresh and retry
- [ ] 7.4 Test error scenarios: network offline, Gmail quota exceeded, revoked token
- [ ] 7.5 Test language switching: change app language mid-session, verify next email is in new language
- [ ] 7.6 Test German email templates: verify subject/body in German, special characters render
- [ ] 7.7 Test Settings UI: sign-in, email display, disable, token persistence across restart
- [ ] 7.8 Test concurrent scenarios: email send fails, local notification sent, user disables email alerts

## 8. Documentation & Localization

- [ ] 8.1 Add email template strings to values-de/strings.xml (German translations)
- [ ] 8.2 Create `docs/gmail-setup-guide.md`: instructions for Google Cloud Console OAuth setup
- [ ] 8.3 Update user guide (docs/user-guide/user-guide.md) with Email Alerts section
  - How to sign in with Google
  - What emails they'll receive and when
  - How to disable email alerts
- [ ] 8.4 Update notification methods documentation to include Gmail in Phase 2
- [ ] 8.5 Create ADR documenting design decisions (token storage, email on status change, fallback behavior)

## 9. Code Review & Polish

- [ ] 9.1 Run `./gradlew ktlintCheck` and fix lint errors
- [ ] 9.2 Verify all Robolectric tests pass: `./gradlew testDebugUnitTest --rerun`
- [ ] 9.3 Verify integration tests pass (mock Gmail API)
- [ ] 9.4 Code review: token security, error handling, test coverage
- [ ] 9.5 Performance review: no blocking operations on main thread (OAuth, Gmail API calls async)
- [ ] 9.6 Security review: no tokens logged, no sensitive data in Logcat

## 10. Real Device Testing

- [ ] 10.1 Test on real Android device (API 31+): Google Sign-In flow, token storage
- [ ] 10.2 Test email sending: modify status, verify email arrives in user's Gmail inbox
- [ ] 10.3 Test background notification: trigger 12h check, verify email + local notification
- [ ] 10.4 Test edge cases: user signs out mid-email, app force-stopped, device offline
- [ ] 10.5 Test Settings UI: Settings screen responsive, no ANR, text renders
- [ ] 10.6 Verify no crashes in Logcat, no permission denials
