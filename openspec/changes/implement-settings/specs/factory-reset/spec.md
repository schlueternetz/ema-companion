## ADDED Requirements

### Requirement: Factory Reset clears all settings and local data
The app SHALL provide a Factory Reset action in the "Configuration" section. Tapping Factory Reset SHALL display a confirmation dialog warning the user that the action is permanent and will delete all settings and locally stored production data. On confirmation, all 9 settings SHALL be reset to their defaults and any locally stored production data SHALL be deleted. On cancellation, no changes SHALL be made.

#### Scenario: Factory Reset requires confirmation
- **WHEN** the user taps the Factory Reset button
- **THEN** a confirmation dialog SHALL appear describing the consequences before any data is deleted

#### Scenario: Confirming Factory Reset clears all settings
- **WHEN** the user confirms the Factory Reset action
- **THEN** all stored settings SHALL be cleared (reset to defaults)
- **AND** all locally stored production data SHALL be deleted
- **AND** the Settings screen SHALL refresh to show all fields at their default (empty) state

#### Scenario: Cancelling Factory Reset makes no changes
- **WHEN** the user dismisses or cancels the confirmation dialog
- **THEN** no settings or data SHALL be modified
