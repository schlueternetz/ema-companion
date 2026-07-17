## Purpose

Lets the user control when module-health status changes are emailed to them, independently of push notifications.

## Requirements

### Requirement: Email alert level can be selected
The app SHALL provide a selectable email alert level for module health emails — Off, Alerts Only, or All — presented as a value row in the Email Alerts section that opens a selection dialog. The default value SHALL be Off. The preference SHALL be persisted and survive app restarts.

#### Scenario: Email alert level row reflects stored value on screen open
- **WHEN** the user navigates to the Settings screen
- **THEN** the email alert level row SHALL display the currently stored level (Off, Alerts Only, or All)

#### Scenario: Selecting a level other than Off persists immediately
- **WHEN** the user opens the email alert level dialog and selects Alerts Only or All
- **THEN** the new value SHALL be stored immediately without requiring a separate Save action

#### Scenario: Selecting a level other than Off without saved credentials reveals setup
- **WHEN** the user selects Alerts Only or All and no email address/App Password is currently saved
- **THEN** the email setup form SHALL be shown so the user can enter and verify credentials

#### Scenario: Selecting Off persists immediately and hides setup
- **WHEN** the user selects Off
- **THEN** the new value SHALL be stored immediately and the email setup form SHALL be hidden

#### Scenario: Selecting Off with saved credentials keeps them visible
- **WHEN** the user selects Off and email credentials are already saved
- **THEN** the status row showing the saved address SHALL remain visible so the user can switch back to a non-Off level or clear credentials

#### Scenario: Legacy boolean preference is migrated to a level
- **WHEN** the app reads the email alert preference for the first time after upgrading from a version that stored a boolean `emailAlertsEnabled` value, and no level has been stored yet
- **THEN** a stored `true` SHALL be migrated to Alerts Only and a stored `false` SHALL be migrated to Off, and the migrated level SHALL be persisted under the new preference

### Requirement: Email alert level gates email dispatch
The module health background check SHALL use the persisted email alert level, together with whether email credentials are configured, to decide whether to send an email for the computed status:
- **Off**: never send an email.
- **Alerts Only**: send an email only when the computed status differs from the previously emailed status.
- **All**: send an email on every check, regardless of whether the status changed.

An email SHALL NOT be sent while the computed status is UNKNOWN, or while email credentials are not configured, regardless of level.

#### Scenario: Off level suppresses an email on status change
- **WHEN** the email alert level is Off and the computed status differs from the previously emailed status
- **THEN** no email SHALL be sent

#### Scenario: Alerts Only sends on degradation
- **WHEN** the email alert level is Alerts Only and the computed status degrades (e.g. GREEN to YELLOW)
- **THEN** an email SHALL be sent for the new status

#### Scenario: Alerts Only sends on recovery
- **WHEN** the email alert level is Alerts Only and the computed status recovers to GREEN from YELLOW or RED
- **THEN** a recovery email SHALL be sent

#### Scenario: Alerts Only does not resend on an unchanged status
- **WHEN** the email alert level is Alerts Only and the computed status equals the previously emailed status
- **THEN** no email SHALL be sent

#### Scenario: All sends on every check regardless of change
- **WHEN** the email alert level is All and the computed status equals the previously emailed status
- **THEN** an email SHALL still be sent for the current check

#### Scenario: Unconfigured email never sends regardless of level
- **WHEN** the email alert level is Alerts Only or All but no email address/App Password is saved
- **THEN** no email SHALL be sent

#### Scenario: UNKNOWN status never sends an email
- **WHEN** the computed status is UNKNOWN, regardless of the email alert level
- **THEN** no email SHALL be sent

### Requirement: All level sends a daily GREEN confirmation email
When the email alert level is All, email is configured, and the computed status is GREEN, the app SHALL send a confirmation email using the existing GREEN subject/body content, even if the previous check also resulted in GREEN.

#### Scenario: Daily All-level check with GREEN status sends a confirmation email
- **WHEN** the email alert level is All, email is configured, and the computed status is GREEN
- **THEN** an email confirming all modules are producing SHALL be sent
