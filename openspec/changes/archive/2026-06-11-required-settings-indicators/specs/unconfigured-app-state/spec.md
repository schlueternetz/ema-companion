## ADDED Requirements

### Requirement: Required-but-empty fields display a "Required" indicator
While the app is unconfigured, each required field that has no saved value SHALL display a "Required" hint in its value area, making it visible to the user without entering edit mode. The required fields are: EMA App ID, EMA App Secret, EMA System ID, EMA ECU ID, and System Capacity.

#### Scenario: Empty required field shows Required hint
- **WHEN** the app is unconfigured and the user views the Settings screen
- **THEN** each required field that has no saved value SHALL display "Required" as a hint in the value area
- **AND** fields that already have a saved value SHALL NOT display the hint

#### Scenario: Required hint clears after field is saved
- **WHEN** the user saves a valid value for a required field
- **THEN** the "Required" hint for that field SHALL no longer be visible
- **AND** the field SHALL display the saved value instead

#### Scenario: Required hint does not appear for optional fields
- **WHEN** the user views the Settings screen in any state
- **THEN** fields that are not required (Historic Data Days, Base URL, etc.) SHALL NOT display a "Required" hint
