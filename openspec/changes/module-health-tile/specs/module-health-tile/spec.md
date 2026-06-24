## ADDED Requirements

### Requirement: Display module health status tile on Home screen
The system SHALL display a module health status tile on the Home screen showing the current health status with a visual indicator and a "last checked" subtitle on all states.

#### Scenario: Green status display
- **WHEN** module health status is GREEN
- **THEN** tile displays a green checkmark, label "All modules producing", and subtitle "Checked [date] at [time]"

#### Scenario: Yellow status display
- **WHEN** module health status is YELLOW
- **THEN** tile displays a yellow warning icon, label "Some modules not producing", and subtitle "Checked [date] at [time]"

#### Scenario: Red status display
- **WHEN** module health status is RED
- **THEN** tile displays a red alert icon, label "Module alert", and subtitle "Checked [date] at [time]"

#### Scenario: Error state display
- **WHEN** module health status is ERROR or data unavailable
- **THEN** tile displays a gray icon, label "Module status unavailable", and subtitle "Last checked [date] at [time]"

#### Scenario: No prior check
- **WHEN** no module health check has ever been run (fresh install)
- **THEN** tile displays the gray icon with label "Checking..." until first check completes

### Requirement: Show module details on tile tap (yellow/red only)
The system SHALL display a modal showing offline inverter details when the user taps the tile while status is YELLOW or RED. Tapping a GREEN tile does nothing.

#### Scenario: Detail modal shows offline inverters (yellow or red)
- **WHEN** user taps the tile while status is YELLOW or RED
- **THEN** modal displays a list of offline inverters with format: "Inverter <uid> — no production for X days"
- **AND** list is sorted by offline duration (longest first)

#### Scenario: No interaction on green status
- **WHEN** user taps the tile while status is GREEN
- **THEN** nothing happens (no ripple, no modal, no message)

### Requirement: Tile updates when Home screen appears
The system SHALL refresh the module health display when the Home screen becomes visible, respecting the 24-hour throttle.

#### Scenario: Initial load on Home open
- **WHEN** user opens Home fragment
- **THEN** tile immediately shows the persisted status (if available)
- **AND** if last check was >24 hours ago, a background fetch is triggered

#### Scenario: Recent check, no fetch
- **WHEN** user opens Home fragment and last check was <24 hours ago
- **THEN** tile displays persisted status without an API call
