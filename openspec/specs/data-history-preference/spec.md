## Requirements

### Requirement: Historic Data Days is configurable
The app SHALL provide an editable field for the number of days of production history to retain. The field SHALL accept an integer between 1 and 90 inclusive. The field SHALL present a numeric keyboard and display a " days" suffix. The field SHALL be grouped under the "App Settings" section.

#### Scenario: Valid day count is accepted and saved
- **WHEN** the user enters an integer between 1 and 90 and taps Save
- **THEN** the value SHALL be stored as an Int and displayed with the days suffix

#### Scenario: Out-of-range day count is rejected
- **WHEN** the user enters a value less than 1 or greater than 90
- **THEN** the Save action SHALL be disabled and an inline error message SHALL be displayed
