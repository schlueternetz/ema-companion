## ADDED Requirements

### Requirement: Developer config for per-endpoint poll intervals
The system SHALL define each API endpoint's poll interval as a compile-time constant in `AppConfig`, so a developer can tune frequency in one place without touching multiple files.

#### Scenario: Constants available at build time
- **WHEN** any component needs a poll interval
- **THEN** it reads from `AppConfig` (e.g. `com.example.emacompanion.core.AppConfig`)
- **AND** no runtime settings lookup, no SharedPreferences read, no user UI is involved

#### Scenario: Module health interval drives both worker and throttle
- **WHEN** `AppConfig.MODULE_HEALTH_CHECK_INTERVAL` is changed
- **THEN** both the WorkManager `PeriodicWorkRequest` interval AND the repository throttle guard (`now - lastCheckEpochMs < interval`) use the new value automatically
- **AND** no other file needs to be updated

#### Scenario: Production interval drives existing throttle
- **WHEN** `AppConfig.PRODUCTION_FETCH_INTERVAL` is changed
- **THEN** the production repository throttle guard uses the new value
- **AND** no other file needs to be updated

### Requirement: Default values
- `PRODUCTION_FETCH_INTERVAL`: 10 minutes
- `MODULE_HEALTH_CHECK_INTERVAL`: 24 hours
