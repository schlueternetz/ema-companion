## MODIFIED Requirements

### Requirement: Notification level can be selected
The app SHALL provide a selectable notification level for module health push notifications — Off, Alerts Only, or All — presented as a value row under "App Settings" that opens a selection dialog (the same pattern used for Language and Display Mode), replacing the previous on/off switch. The default value SHALL be Alerts Only. The preference SHALL be persisted and survive app restarts.

#### Scenario: Notification level row reflects stored value on screen open
- **WHEN** the user navigates to the Settings screen
- **THEN** the notification level row SHALL display the currently stored level (Off, Alerts Only, or All)

#### Scenario: Selecting a notification level persists the new value
- **WHEN** the user opens the notification level dialog and selects a level
- **THEN** the new value SHALL be stored immediately without requiring a separate Save action

#### Scenario: Legacy boolean preference is migrated to a level
- **WHEN** the app reads the notification preference for the first time after upgrading from a version that stored a boolean `notificationsEnabled` value, and no level has been stored yet
- **THEN** a stored `true` SHALL be migrated to Alerts Only and a stored `false` SHALL be migrated to Off, and the migrated level SHALL be persisted under the new preference

## ADDED Requirements

### Requirement: Notification level gates push notification dispatch
The module health background check SHALL use the persisted notification level to decide whether to dispatch a push notification for the computed status:
- **Off**: never dispatch a notification.
- **Alerts Only**: dispatch a notification only when the computed status differs from the previously notified status.
- **All**: dispatch a notification on every check, regardless of whether the status changed.

A notification SHALL NOT be dispatched while the computed status is UNKNOWN, regardless of level.

#### Scenario: Off level suppresses a notification on status change
- **WHEN** the notification level is Off and the computed status differs from the previously notified status
- **THEN** no push notification SHALL be dispatched

#### Scenario: Alerts Only notifies on degradation
- **WHEN** the notification level is Alerts Only and the computed status degrades (e.g. GREEN to YELLOW)
- **THEN** a push notification SHALL be dispatched for the new status

#### Scenario: Alerts Only notifies on recovery
- **WHEN** the notification level is Alerts Only and the computed status recovers to GREEN from YELLOW or RED
- **THEN** a push notification SHALL be dispatched confirming recovery

#### Scenario: Alerts Only does not re-notify on an unchanged status
- **WHEN** the notification level is Alerts Only and the computed status equals the previously notified status
- **THEN** no push notification SHALL be dispatched

#### Scenario: All notifies on every check regardless of change
- **WHEN** the notification level is All and the computed status equals the previously notified status
- **THEN** a push notification SHALL still be dispatched for the current check

#### Scenario: UNKNOWN status never notifies
- **WHEN** the computed status is UNKNOWN, regardless of the notification level
- **THEN** no push notification SHALL be dispatched

### Requirement: All level posts a GREEN confirmation notification
When the notification level is All and the computed status is GREEN, the app SHALL post a notification confirming the array is healthy, using the same non-stacking replacement behavior as YELLOW/RED notifications.

#### Scenario: Daily All-level check with GREEN status posts a confirmation
- **WHEN** the notification level is All and the computed status is GREEN
- **THEN** a notification confirming all modules are producing SHALL be posted

#### Scenario: Consecutive GREEN days under All replace rather than stack
- **WHEN** the notification level is All and the computed status is GREEN on consecutive daily checks
- **THEN** each day's notification SHALL replace the previous day's rather than creating an additional notification
