## ADDED Requirements

### Requirement: Gmail SMTP credential setup
The system SHALL allow users to configure Gmail SMTP credentials (address + App Password) to enable email alerts.

#### Scenario: User enables Email Alerts in Settings
- **WHEN** user toggles "Email Alerts" on in Settings
- **THEN** the email configuration row expands (sign-in button or current account shown)

#### Scenario: User opens email setup
- **WHEN** user taps the email configuration row with Email Alerts enabled
- **THEN** Settings shows a setup screen (or full-screen dialog) with instructions displayed BEFORE any input fields:

  ```
  Setting up email alerts

  EMA Companion will send alerts from your Gmail account using an App Password —
  a one-time setup that takes about 2 minutes.

  You will need to leave this app briefly to create the App Password.

  How to set up:
  1. Make sure 2-Step Verification is enabled on your Google Account
  2. Tap "Open Google Account" below — your browser will open
  3. Go to Security → 2-Step Verification → App passwords
  4. Create an App Password — name it "EMA Companion"
  5. Copy the 16-character password shown
  6. Return here and paste it below

  [ Open Google Account ↗ ]   ← opens https://myaccount.google.com/apppasswords in browser

  Gmail address    [________________]
  App Password     [________________]  (masked, last 4 chars visible)

  [ Verify & Save ]
  ```

- **AND** the "Open Google Account" button is shown prominently before the input fields
- **AND** the instructions remain visible while the user fills in the fields (not collapsed)

#### Scenario: Credentials saved and verified
- **WHEN** user taps "Verify & Save"
- **THEN** system attempts a test SMTP connection to `smtp.gmail.com:587` using the provided credentials
- **AND** on success: stores credentials encrypted, returns to Settings showing "Email alerts enabled for: user@gmail.com"
- **AND** on failure: displays inline error "Connection failed. Check your App Password and try again." — fields remain filled for correction
- **AND** credentials are stored encrypted in EncryptedSharedPreferences (AES256-GCM)

#### Scenario: Credentials cleared
- **WHEN** user taps "Disable Email Alerts" toggle
- **THEN** system displays confirmation: "Email alerts will be disabled. Continue?"
- **AND** upon confirmation, Gmail address and App Password are deleted from storage
- **AND** Settings displays the email setup row in unconfigured state

#### Scenario: Credentials never exposed
- **WHEN** credentials are stored or displayed
- **THEN** App Password is never logged, never shown in full (masked to last 4 chars)
- **AND** Gmail address is shown as confirmation only (not editable inline without re-entering App Password)

### Requirement: Secure credential storage
The system SHALL protect Gmail credentials from unauthorized access or exposure.

#### Scenario: Credentials stored encrypted
- **WHEN** user saves Gmail address and App Password
- **THEN** both are stored in EncryptedSharedPreferences (AES256-GCM)
- **AND** cleared automatically on factory reset or settings import

#### Scenario: Credentials cleared on app uninstall
- **WHEN** app is uninstalled
- **THEN** credentials are deleted from device storage
- **AND** user must re-enter credentials if app is reinstalled

### Requirement: lastEmailedStatus and lastNotifiedStatus tracking
The system SHALL maintain separate persisted fields for last-alerted status to detect meaningful changes.

#### Scenario: Fields initialised
- **WHEN** app is first installed or after factory reset
- **THEN** `lastEmailedStatus` and `lastNotifiedStatus` are absent (treated as UNKNOWN)
- **AND** the first check that finds a non-GREEN status triggers both a push notification and an email (if configured)

#### Scenario: Fields cleared on credential change or import
- **WHEN** EMA System ID, ECU ID, or any connection credential is changed
- **OR** settings are imported from a file
- **OR** factory reset is performed
- **THEN** `lastEmailedStatus` and `lastNotifiedStatus` are cleared
- **AND** the next background check re-evaluates from scratch
