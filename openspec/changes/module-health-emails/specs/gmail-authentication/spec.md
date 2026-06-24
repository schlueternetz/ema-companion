## ADDED Requirements

### Requirement: Google Sign-In OAuth 2.0 authentication
The system SHALL provide a Google Sign-In flow that allows users to authenticate with their personal Google Account and grant permission to send emails.

#### Scenario: User initiates Google Sign-In
- **WHEN** user taps "Enable Email Alerts" button in Settings (Email Alerts section)
- **THEN** system launches Google Sign-In activity
- **AND** user selects their Google Account
- **AND** user is shown consent screen requesting "Send email on your behalf" permission

#### Scenario: Sign-In success and token storage
- **WHEN** Google Sign-In completes successfully
- **THEN** OAuth 2.0 access token is stored securely in EncryptedSharedPreferences
- **AND** user's email address (from Google Account) is retrieved and stored in SettingsRepository
- **AND** Settings UI displays "Email alerts enabled for: user@gmail.com"
- **AND** token persists across app restarts

#### Scenario: Sign-In cancellation
- **WHEN** user cancels or dismisses the Sign-In activity
- **THEN** no token is stored
- **AND** Settings displays "Email alerts: Tap to sign in"
- **AND** local notifications remain functional

#### Scenario: Sign-In failure
- **WHEN** Google Sign-In fails (network error, account unavailable, consent denied)
- **THEN** error message is displayed: "Sign-in failed. Email alerts not enabled. Local notifications will continue."
- **AND** email notifications remain disabled
- **AND** local notifications continue to work as fallback

### Requirement: User sign-out and token revocation
The system SHALL allow users to disable email alerts and revoke the OAuth token.

#### Scenario: User disables email alerts
- **WHEN** user taps "Disable Email Alerts" in Settings
- **THEN** system displays confirmation: "Email alerts will be disabled. Continue?"
- **AND** upon confirmation, OAuth token is revoked and deleted
- **AND** user email is cleared
- **AND** Settings displays "Email alerts: Tap to sign in"

#### Scenario: Token revocation confirms no further access
- **WHEN** token is revoked
- **THEN** Gmail API will reject any subsequent requests with the old token
- **AND** user's data is not accessible via the app

### Requirement: Token refresh on expiry
The system SHALL automatically refresh expired OAuth tokens before sending emails.

#### Scenario: Token refresh before email send
- **WHEN** email is about to be sent and stored token is expired (>1 hour old)
- **THEN** system attempts silent token refresh using Google APIs
- **AND** new token is stored, replacing the old one
- **AND** email send proceeds with refreshed token

#### Scenario: Token refresh failure
- **WHEN** token refresh fails (network error, user revoked access, refresh token expired)
- **THEN** email send is abandoned
- **AND** system falls back to local notification as alert
- **AND** error is logged for debugging
- **AND** app does not crash

#### Scenario: Token valid, no refresh needed
- **WHEN** stored token is fresh (<1 hour old)
- **THEN** system uses existing token immediately for email send (no refresh call)

### Requirement: Secure token handling
The system SHALL protect OAuth tokens from unauthorized access or exposure.

#### Scenario: Token stored encrypted
- **WHEN** Google Sign-In completes
- **THEN** access token is stored in EncryptedSharedPreferences (AES256-GCM encryption)
- **AND** token is never logged, displayed, or exposed in plain text

#### Scenario: Token cleared on app uninstall
- **WHEN** app is uninstalled
- **THEN** token is deleted from device storage
- **AND** user must sign in again if app is reinstalled
