## MODIFIED Requirements

### Requirement: System Capacity is configurable
The app SHALL provide an editable field for the solar array's System Capacity in kW. The field SHALL accept a positive decimal number with at most 2 decimal places and a maximum value of 2,000. The field SHALL present a decimal keyboard and display a " kW" suffix. The field SHALL be grouped under the "Solar Array Settings" section.

When set, the System Capacity SHALL be used as the Y-axis maximum for all production charts (today's hourly line chart and history bar chart). When not set (empty or zero), each chart SHALL scale its Y-axis to the data maximum.

#### Scenario: Valid capacity is accepted and saved
- **WHEN** the user enters a decimal number with up to 2 decimal places (e.g. "4.56") and taps Save
- **THEN** the value SHALL be stored as a Float and displayed with the kW suffix

#### Scenario: Invalid capacity is rejected
- **WHEN** the user enters a value that is empty, non-numeric, or has more than 2 decimal places
- **THEN** the Save action SHALL be disabled and an inline error message SHALL be displayed

#### Scenario: Capacity used as chart Y-axis maximum
- **WHEN** System Capacity is set to a positive value and a production chart is displayed
- **THEN** the chart's Y-axis maximum SHALL equal the configured System Capacity in kW

#### Scenario: Chart scales to data when capacity not set
- **WHEN** System Capacity is not configured (zero or empty)
- **THEN** the chart's Y-axis SHALL scale automatically to the maximum data value
