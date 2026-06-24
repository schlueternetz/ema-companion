## ADDED Requirements

### Requirement: Determine module health status from daily energy data
The system SHALL compute a module health status (green/yellow/red) based on consecutive days with 0 kWh per inverter. Because the job is scheduled at 8pm array-local time, today's production window is always complete when the check runs — no time-of-day cutoff is needed in the computation.

The **expected inverter set** is the union of all UIDs seen across all days in the evaluation window (today, yesterday, day-before-yesterday). An inverter absent from a day's response is treated identically to one that reported 0 kWh — both mean no production on that day. This handles the case where a broken inverter stops reporting entirely rather than reporting zero.

Status rules:
- **GREEN**: All inverters in the expected set have produced (>0 kWh) on every evaluated day
- **YELLOW**: At least one inverter has 0 kWh (or was absent) for 1–2 consecutive days
- **RED**: At least one inverter has 0 kWh (or was absent) for 3 consecutive days

#### Scenario: All inverters producing (green)
- **WHEN** all inverters have >0 kWh on each day in the evaluation window
- **THEN** system reports status as GREEN

#### Scenario: One inverter offline 1–2 days (yellow)
- **WHEN** at least one inverter in the expected set has 0 kWh or was absent for 1 or 2 consecutive evaluated days
- **THEN** system reports status as YELLOW with list of affected inverters

#### Scenario: One inverter offline 3 days (red)
- **WHEN** at least one inverter in the expected set has 0 kWh or was absent for 3 consecutive complete days
- **THEN** system reports status as RED with list of affected inverters

#### Scenario: Inverter disappears from response
- **WHEN** an inverter UID is present in yesterday's cached response but absent from today's response
- **THEN** it is treated as 0 kWh for today
- **AND** its `offlineDays` count increments normally
- **AND** it appears in the offline list in the detail modal

#### Scenario: No inverter data available
- **WHEN** evaluation window is empty or all API calls failed
- **THEN** system reports status as ERROR with appropriate error message

### Requirement: Display offline duration as days, not hours
The system SHALL express offline duration as whole days ("no production for X days"), since only day-granularity data is available from the API. No hours or sub-day precision shall be shown.

#### Scenario: Offline duration in detail modal
- **WHEN** detail modal displays an offline inverter
- **THEN** duration is shown as "no production for 1 day", "no production for 2 days", etc.
- **AND** no hours or minutes are displayed

### Requirement: Persist module health state
The system SHALL persist the last computed health status and offline inverter details in local storage so they survive app restarts.

#### Scenario: State persisted and retrieved
- **WHEN** module health status is computed
- **THEN** status, offline inverter list, and check timestamp are stored in `ema_module_health` SharedPreferences
- **AND WHEN** app is restarted before next check
- **THEN** the previously stored state is immediately displayed

### Requirement: Track last check timestamp
The system SHALL store the timestamp of the last successful module health check to enable throttling and display.

#### Scenario: Check timestamp updated after fetch
- **WHEN** all batch energy calls for a health check complete successfully
- **THEN** `lastCheckEpochMs` is updated to the current time in `ema_module_health` SharedPreferences
