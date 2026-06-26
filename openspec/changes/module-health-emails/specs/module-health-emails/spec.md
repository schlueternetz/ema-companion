## ADDED Requirements

### Requirement: Send module health emails on status change
The system SHALL send an email to the user's Gmail inbox when module health status changes, if user is signed in.

#### Scenario: Email sent on status change to yellow
- **WHEN** module health status changes to YELLOW (from GREEN or unset) AND user is signed in
- **THEN** system sends an email to the user's Gmail address via Gmail API with:
  - Subject: Localized (e.g., "⚠️ Module Alert" in English or German)
  - Body: Lists offline modules, time offline, link to app
- **AND** email is sent from user's own Gmail account (visible in their "Sent" folder)

#### Scenario: Email sent on status change to red
- **WHEN** module health status changes to RED (from any state) AND user is signed in
- **THEN** system sends an email with higher urgency:
  - Subject: Localized (e.g., "🚨 Module Critical")
  - Body: Lists critical modules with durations, urgency notice
- **AND** email is sent immediately

#### Scenario: Email sent on recovery to green
- **WHEN** module health status returns to GREEN (from YELLOW or RED) AND user is signed in
- **THEN** system sends a recovery email:
  - Subject: Localized (e.g., "✅ All Modules Online")
  - Body: Confirmation that all modules are producing

#### Scenario: No email on same status during check
- **WHEN** 24-hour check runs and status remains YELLOW or RED (no change)
- **THEN** NO email is sent
- **AND** NO local notification is sent (both channels fire on status change only)

#### Scenario: No email if user not signed in
- **WHEN** status changes and user has not enabled email alerts
- **THEN** local notification is sent (Phase 1)
- **AND** NO email is sent

### Requirement: Handle email send errors gracefully
The system SHALL gracefully handle Gmail API failures without crashing the app.

#### Scenario: Network error sending email
- **WHEN** Gmail API call fails due to network error
- **THEN** system logs error with timestamp and details
- **AND** falls back to local notification
- **AND** will retry email on next status check (if status changed)
- **AND** app continues running normally

#### Scenario: Gmail API rate limit or quota exceeded
- **WHEN** Gmail API returns 429 (rate limit) or quota error
- **THEN** system logs error
- **AND** falls back to local notification
- **AND** retries on next status change

#### Scenario: Invalid or revoked token
- **WHEN** Gmail API returns 401 (unauthorized/token invalid)
- **THEN** system attempts token refresh
- **AND** if refresh succeeds, retries email send
- **AND** if refresh fails, logs error and falls back to local notification

### Requirement: Email sent only on meaningful status changes
The system SHALL not spam user with emails on every check.

#### Scenario: Status change transitions that trigger email
- **WHEN** status: GREEN→YELLOW — send YELLOW alert email
- **WHEN** status: GREEN→RED — send RED alert email
- **WHEN** status: YELLOW→RED — send RED alert email (escalation)
- **WHEN** status: YELLOW→GREEN — send recovery email
- **WHEN** status: RED→GREEN — send recovery email
- **THEN** email is sent for each of the above transitions
- **AND** no email if status remains the same

#### Scenario: RED stays RED — no partial recovery email
- **WHEN** some offline modules recover but at least one remains offline (RED→YELLOW transition)
- **THEN** the displayed status remains RED (not downgraded to YELLOW)
- **AND** no email is sent
- **RATIONALE**: a module is still offline; this is not a recovery. Alert only clears when all modules are producing (→GREEN).
