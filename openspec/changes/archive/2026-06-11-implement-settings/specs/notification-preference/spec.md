## ADDED Requirements

### Requirement: Notifications can be toggled on or off
The app SHALL provide a toggle (switch) to enable or disable notifications. The default value SHALL be true (enabled). The preference SHALL be persisted and survive app restarts. The toggle SHALL be grouped under the "App Settings" section.

#### Scenario: Notifications toggle reflects stored value on screen open
- **WHEN** the user navigates to the Settings screen
- **THEN** the notifications toggle SHALL reflect the currently stored value

#### Scenario: Toggling notifications persists the new value
- **WHEN** the user flips the notifications toggle
- **THEN** the new value SHALL be stored immediately without requiring a separate Save action
