## ADDED Requirements

### Requirement: Post daily notification while status is yellow or red
The system SHALL post a local notification on every check where status is YELLOW or RED. Notifications are not posted on GREEN days. There is no suppression for repeated non-green status — the user should be reminded daily until the array is fixed.

#### Scenario: Notify on yellow status
- **WHEN** module health check completes with status YELLOW
- **THEN** system posts a notification with priority INFO, title "Module Alert", and body showing count of affected inverters and days offline

#### Scenario: Notify on red status
- **WHEN** module health check completes with status RED
- **THEN** system posts a notification with priority HIGH, title "Module Critical", and body showing count and days offline

#### Scenario: Repeated non-green days re-alert
- **WHEN** status is YELLOW or RED on consecutive daily checks
- **THEN** a notification is posted on each check, not just the first occurrence
- **AND** the previous notification is replaced (not stacked) so only one module-health notification appears at a time

#### Scenario: No notification on green
- **WHEN** check completes with status GREEN
- **THEN** no notification is posted
- **AND** any existing module-health notification is cleared

#### Scenario: No notification on error
- **WHEN** check fails (API error, network error)
- **THEN** no notification is posted and the previous notification state is unchanged

### Requirement: Notification channel for module alerts
The system SHALL create and configure a notification channel for module health alerts (API 26+).

#### Scenario: Create notification channel on first launch
- **WHEN** app first runs on Android API 26 or higher
- **THEN** system creates a notification channel named "Module Alerts" with importance HIGH
- **AND** a single channel is used for both YELLOW and RED (importance HIGH covers both)

#### Scenario: Notification channel not created on API <26
- **WHEN** app runs on Android API <26
- **THEN** system does not attempt to create a notification channel (falls back to default notification behavior)

### Requirement: POST_NOTIFICATIONS permission gate
The system SHALL only post notifications if `POST_NOTIFICATIONS` permission is granted (Android 13+ / API 33+).

#### Scenario: Permission granted
- **WHEN** user has granted `POST_NOTIFICATIONS`
- **THEN** notifications are posted normally

#### Scenario: Permission denied
- **WHEN** user has denied `POST_NOTIFICATIONS` on API 33+
- **THEN** background check still runs and status is persisted, but no notification is posted
