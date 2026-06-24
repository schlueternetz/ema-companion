## ADDED Requirements

### Requirement: Array timezone setting
The system SHALL store a user-configured timezone representing the physical location of the solar array. This timezone is used to determine when 8pm local time is, so the daily background job can be scheduled to run after solar production hours are complete.

#### Scenario: Default timezone on first install
- **WHEN** app is installed and no array timezone has been configured
- **THEN** the array timezone defaults to the device's system timezone

#### Scenario: User changes array timezone
- **WHEN** user navigates to Settings and changes the "Array Timezone" field
- **THEN** the selected timezone is stored in encrypted SharedPreferences
- **AND** the WorkManager job is rescheduled so the next run aligns to 8pm in the new timezone

#### Scenario: Timezone selection covers all IANA timezones
- **WHEN** user opens the timezone selector
- **THEN** all standard IANA timezone IDs are available (e.g. "Europe/Berlin", "America/New_York")
- **AND** the list is searchable or grouped by region
- **AND** the currently selected timezone is shown as the default selection

### Requirement: Schedule daily job at 8pm array local time
The system SHALL schedule the `ModuleHealthWorker` to run at 8pm in the configured array timezone, ensuring the job always runs after solar production hours are complete.

#### Scenario: Initial scheduling aligns to next 8pm
- **WHEN** `ModuleHealthWorker` is first scheduled (app install or after timezone change)
- **THEN** `initialDelay` is calculated as the milliseconds until the next 8pm in the array timezone
- **AND** the job repeats every 24 hours from that first run
- **AND** `ExistingPeriodicWorkPolicy.KEEP` prevents app restarts from resetting the timer

#### Scenario: Job runs after solar hours
- **WHEN** `ModuleHealthWorker` runs
- **THEN** it is approximately 8pm in the array timezone
- **AND** today's solar production window is complete
- **AND** today's batch energy data can be evaluated without false zero readings
