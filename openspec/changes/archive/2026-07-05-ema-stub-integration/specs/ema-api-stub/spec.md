## ADDED Requirements

### Requirement: Good Data scenario — ECU hourly and daily energy
The bundled Good Data ECU's scenario (`203000001234.json`) SHALL include interactions for `GET /user/api/v2/systems/{sid}/devices/ecu/energy/{eid}` with `energy_level=hourly` and with `energy_level=daily`, in addition to its existing `minutely` interaction. The `hourly` interaction SHALL return a `data` array of at most 24 entries (nullable/omitted entries representing hours with no data) representing energy for hours 0-23 of the requested date. The `daily` interaction SHALL return a `data` array with one entry per day of the requested month, consistent with the values in the `minutely`/Good Data persona (10 kW array).

#### Scenario: Hourly request for the Good Data ECU returns per-hour energy
- **WHEN** a client requests the ECU energy endpoint with `energy_level=hourly`, any `sid`, and the Good Data ECU id
- **THEN** the stub responds with `code` `0` and a `data` array of at most 24 entries indexed by hour

#### Scenario: Daily request for the Good Data ECU returns per-day energy
- **WHEN** a client requests the ECU energy endpoint with `energy_level=daily`, any `sid`, and the Good Data ECU id
- **THEN** the stub responds with `code` `0` and a `data` array with one entry per day of the requested month

### Requirement: No-data and multi-month scenario fixtures for hourly/daily
Dedicated scenario fixtures (separate from the bundled default set) SHALL be available exercising: an `hourly` or `daily` request that returns EMA's no-data response (`code` `1001`), and a `daily` request sequence spanning two consecutive calendar months.

#### Scenario: No-data response is servable
- **WHEN** a client requests `energy_level=hourly` or `energy_level=daily` against the no-data fixture
- **THEN** the stub responds with `code` `1001`

#### Scenario: Multi-month daily sequence is servable
- **WHEN** a client requests `energy_level=daily` twice in sequence, once per calendar month, against the multi-month fixture
- **THEN** the stub returns each month's response in the order requested
