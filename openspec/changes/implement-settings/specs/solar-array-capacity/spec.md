## ADDED Requirements

### Requirement: System Capacity is configurable
The app SHALL provide an editable field for the solar array's System Capacity in kW. The field SHALL accept a positive decimal number with at most 2 decimal places and a maximum value of 999.99. The field SHALL present a decimal keyboard and display a " kW" suffix. The field SHALL be grouped under the "Solar Array Settings" section.

#### Scenario: Valid capacity is accepted and saved
- **WHEN** the user enters a decimal number with up to 2 decimal places (e.g. "4.56") and taps Save
- **THEN** the value SHALL be stored as a Float and displayed with the kW suffix

#### Scenario: Invalid capacity is rejected
- **WHEN** the user enters a value that is empty, non-numeric, or has more than 2 decimal places
- **THEN** the Save action SHALL be disabled and an inline error message SHALL be displayed
