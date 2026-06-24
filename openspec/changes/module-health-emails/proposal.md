## Why

Users who've opted into email alerts via Google Sign-In need to receive translated module health alerts in their inbox. This feature builds on the app's 12-hour module health checks (from Phase 1) to deliver email notifications in the user's preferred language, enabling alerts even when the app is not active.

## What Changes

- **Google Sign-In integration at signup** (or optional in Settings): users can authenticate with their personal Google Account to enable email notifications
- **Module health emails sent automatically**: when status changes to yellow (some offline) or red (critical offline), email is sent to user's Gmail inbox
- **Emails are translated**: subject and body use user's app language preference (English or German)
- **Graceful fallback**: if Gmail API fails or token expires, system logs error and falls back to in-app local notifications
- **Token management**: OAuth tokens stored securely in EncryptedSharedPreferences, refreshed on expiry, revoked on sign-out
- **Zero backend cost**: each user sends from their own Gmail account using Gmail API (no server infrastructure required)

## Capabilities

### New Capabilities
- `gmail-authentication`: Google Sign-In OAuth 2.0 flow, token storage, refresh, and revocation
- `module-health-emails`: Sending translated module health alert emails via Gmail API on status changes
- `email-content-templates`: Email subject/body templates with module details, translated to user's language

### Modified Capabilities
- `settings-ui`: Settings screen adds "Email Alerts" section with Sign-In/Sign-Out button
- `module-health-notifications`: Already working in Phase 1; Phase 2 adds email sending as supplement (not replacement) to local notifications

## Impact

- **Frontend**: Settings UI additions for Google Sign-In button, sign-out button, email settings section
- **Data**: OAuth token storage in `SettingsRepository`; email preferences (user's Google account email, token)
- **API**: Gmail API client and authenticated request handling
- **Background work**: `ModuleHealthWorker` (from Phase 1) triggers email send on status change if user is signed in
- **Dependencies**: Google Play Services (Google Auth, Gmail API)
- **Manifest**: Permissions for Google Account access (handled by Play Services); no new manifest permissions
- **Localization**: Email templates in strings.xml (EN, DE), reusing app's language preference
- **Testing**: OAuth mock tests, email sending tests, token refresh/expiry tests, Robolectric Settings UI tests

## Dependencies & Sequencing

**Depends on**: module-health-tile (Phase 1 must be complete)
- Phase 1 provides: WorkManager background task, ModuleHealthWorker, status computation, module health repository
- Phase 2 adds: email sending triggered from ModuleHealthWorker on status change
- **Non-blocking**: Phase 1 can ship without Phase 2; email is optional feature
