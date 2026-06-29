## ADDED Requirements

### Requirement: EMA App ID is configurable
The app SHALL provide an editable field for the EMA App ID. The field SHALL enforce that the value is exactly 32 alphanumeric characters. The stored value SHALL be normalized to lowercase. The field SHALL be grouped under a "Solar Array Settings" section.

#### Scenario: Valid App ID is accepted and saved
- **WHEN** the user enters a 32-character alphanumeric string and taps Save
- **THEN** the value SHALL be stored (lowercase) and displayed in the field

#### Scenario: Invalid App ID is rejected
- **WHEN** the user enters a string that is not exactly 32 alphanumeric characters
- **THEN** the Save action SHALL be disabled and an inline error message SHALL be displayed

### Requirement: EMA App Secret is configurable
The app SHALL provide an editable field for the EMA App Secret. The field SHALL enforce that the value is exactly 12 alphanumeric characters. The stored value SHALL be normalized to lowercase. While in read-only mode the field SHALL mask all but the last 4 characters with bullet characters. When the user enters edit mode the field SHALL be cleared. The field SHALL be grouped under the "Solar Array Settings" section.

#### Scenario: Secret is masked in read-only mode
- **WHEN** the user views the Settings screen and an App Secret is stored
- **THEN** the displayed value SHALL show bullet characters for all but the last 4 characters

#### Scenario: Secret field clears on edit
- **WHEN** the user taps Edit on the App Secret field
- **THEN** the text field SHALL be empty, ready for a new value to be entered

#### Scenario: Valid App Secret is accepted and saved
- **WHEN** the user enters a 12-character alphanumeric string and taps Save
- **THEN** the value SHALL be stored (lowercase) and displayed in masked form

#### Scenario: Invalid App Secret is rejected
- **WHEN** the user enters a string that is not exactly 12 alphanumeric characters
- **THEN** the Save action SHALL be disabled and an inline error message SHALL be displayed

### Requirement: EMA System ID is configurable
The app SHALL provide an editable field for the EMA System ID. The field SHALL enforce that the value is exactly 16 alphanumeric characters. The stored value SHALL be normalized to uppercase. The field SHALL be grouped under the "Solar Array Settings" section.

#### Scenario: Valid System ID is accepted and saved
- **WHEN** the user enters a 16-character alphanumeric string and taps Save
- **THEN** the value SHALL be stored (uppercase) and displayed in the field

#### Scenario: Invalid System ID is rejected
- **WHEN** the user enters a string that is not exactly 16 alphanumeric characters
- **THEN** the Save action SHALL be disabled and an inline error message SHALL be displayed

### Requirement: EMA ECU ID is configurable
The app SHALL provide an editable field for the EMA ECU ID. The field SHALL enforce that the value is exactly 12 decimal digits. The field SHALL present a numeric keyboard. The field SHALL be grouped under the "Solar Array Settings" section.

#### Scenario: Valid ECU ID is accepted and saved
- **WHEN** the user enters exactly 12 digits and taps Save
- **THEN** the value SHALL be stored and displayed in the field

#### Scenario: Invalid ECU ID is rejected
- **WHEN** the user enters a value that is not exactly 12 digits
- **THEN** the Save action SHALL be disabled and an inline error message SHALL be displayed
