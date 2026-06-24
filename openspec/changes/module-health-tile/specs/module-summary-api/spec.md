## ADDED Requirements

### Requirement: Fetch only missing daily inverter energy data
The system SHALL persist each day's batch energy result and only fetch dates not already in the cache, minimising API calls. The evaluation window is determined by the 8pm array-timezone cutoff (see `array-timezone-setting` spec).

#### Scenario: Normal check (yesterday and older already cached)
- **WHEN** module health check is triggered and throttle is not active
- **AND** yesterday's and day-before-yesterday's data are already in the cache
- **THEN** system fetches only today's batch energy
- **AND** result is stored in the cache keyed by today's date
- **AND** status is computed from the 3-day cache (today + 2 persisted days)
- **AND** exactly 1 API call is made

#### Scenario: Catch-up after device downtime
- **WHEN** module health check is triggered
- **AND** one or more dates in the 3-day window are absent from the cache (e.g. device was off)
- **THEN** system fetches each missing date's batch energy, in order from oldest to newest
- **AND** each result is stored in the cache before the next call
- **AND** at most 3 API calls are made (only the last 3 days matter; older dates are not fetched)

#### Scenario: Today is always re-fetched
- **WHEN** module health check is triggered
- **THEN** today's batch energy is always fetched fresh (data accumulates during the day)
- **AND** any previously cached value for today's date is overwritten

#### Scenario: Past days are never re-fetched
- **WHEN** a date earlier than today is already present in the cache
- **THEN** system uses the cached value without making an API call for that date

#### Scenario: API call respects throttle
- **WHEN** last check was within `AppConfig.MODULE_HEALTH_CHECK_INTERVAL` (24h)
- **THEN** system makes no API calls
- **AND** persisted status is displayed

#### Scenario: API error on a date
- **WHEN** a batch energy call fails for any date (network error, auth error, malformed response)
- **THEN** that date is NOT written to the cache
- **AND** system logs the error and does NOT update the health status
- **AND** previous persisted status remains displayed
- **AND** failed calls are NOT counted toward API quota

### Requirement: Per-day cache storage
The system SHALL persist daily batch energy results in `ema_module_health_daily` SharedPreferences, keyed by date string (`yyyy-MM-dd`).

**Serialization format**: each entry is a JSON object string mapping inverter UID to total kWh (summed across channels, as a float). Example key `daily_2025-07-24`, value `{"902000001234":1.24,"902000001235":0.00}`. An inverter with value `0.0` did not produce that day. An inverter absent from the response on a given day is also treated as 0 kWh for that day during status computation (see module-health-status spec). The distinction between "reported 0.0" and "absent" is not preserved — both are handled the same way by taking the union of UIDs across the evaluation window.

#### Scenario: Cache entry written after successful fetch
- **WHEN** a batch energy call for a given date completes successfully (response code 0)
- **THEN** the parsed uid→kWh map (JSON string) for that date is written to `ema_module_health_daily` under key `daily_{yyyy-MM-dd}`
- **AND** the result is available for subsequent checks without an API call

#### Scenario: Cache pruned of old entries
- **WHEN** a health check completes
- **THEN** any cache entries older than 3 days are removed from `ema_module_health_daily`

### Requirement: API quota and usage tracking
The system SHALL track API calls and conform to EMA's monthly quota limits.

#### Scenario: Track successful calls against quota
- **WHEN** a successful batch energy API call completes (response code 0)
- **THEN** the call is logged with timestamp in `ema_api_log` SharedPreferences
- **AND** monthly count is incremented once per call

#### Scenario: Failed calls do not count against quota
- **WHEN** an API call fails
- **THEN** the call is logged but NOT counted toward monthly quota

### Requirement: Inverter data model
The system SHALL derive offline duration from the number of consecutive cached days with 0 kWh, not from a timestamp field.

#### Scenario: Parse batch energy response
- **WHEN** a batch energy response is received
- **THEN** each entry `"{uid}-{channel}-{kWh}"` is parsed
- **AND** entries are grouped by `uid`, summing kWh across channels
- **AND** an inverter is considered to have produced on that day if the total kWh sum > 0

#### Scenario: Handle empty response
- **WHEN** batch energy response contains no entries
- **THEN** result is stored as an empty map (no inverters) for that date
- **AND** status is computed as GREEN (no offline inverters identified)
