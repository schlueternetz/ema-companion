## ADDED Requirements

### Requirement: Local HTTP server exposing the EMA API surface

The stub SHALL run as a standalone, locally launchable HTTP server that serves APsystems EMA API endpoints under the documented path prefixes (`/user/api/v2/` and, in future, `/installer/api/v2/`). The listening port SHALL be configurable.

#### Scenario: Server starts on the configured port

- **WHEN** the stub is launched with a configured port
- **THEN** it accepts HTTP requests on that port and dispatches them to the matching engine

#### Scenario: Unrecognized route returns not-found

- **WHEN** a request targets a path that is not a recognized EMA API endpoint or stub control endpoint
- **THEN** the stub responds with HTTP 404

### Requirement: Per-ECU scenario files drive behaviour

The stub's behaviour SHALL be driven by external JSON scenario files — one file per ECU id — loaded at startup from a configurable directory, rather than hardcoded. Each file SHALL declare its ECU id and an ordered `interactions` array, where each interaction pairs a request matcher with a response. The `eid` SHALL be treated as an opaque EMA-format string (e.g. `203000001234`). A default scenario set SHALL be bundled with the stub.

#### Scenario: Scenario files are loaded at startup

- **WHEN** the stub starts with a valid scenario directory
- **THEN** it loads each per-ECU scenario file and indexes the ordered interactions by ECU id

#### Scenario: Invalid or missing scenario files fail fast

- **WHEN** the stub starts and a scenario file is missing or cannot be parsed
- **THEN** the stub fails to start with a clear error rather than serving fabricated or empty data

### Requirement: Strict sequential matching with a per-ECU cursor

The stub SHALL maintain a per-ECU-id cursor, starting at the first interaction. For each request it SHALL resolve the ECU id from the path, take that ECU's interaction at the current cursor position, and verify the request against the interaction's matcher. The matcher SHALL assert the HTTP method, the path template, and every specified path parameter and query parameter; parameters not specified in the matcher SHALL be treated as wildcards. On a successful match the stub SHALL return the interaction's response and advance the cursor.

#### Scenario: Matching call returns its response and advances

- **WHEN** a request matches the current interaction's matcher for its ECU id
- **THEN** the stub returns that interaction's response body (HTTP 200 by default, or the interaction's specified status)
- **AND** the cursor for that ECU id advances to the next interaction

#### Scenario: Repeated endpoint returns responses in order

- **WHEN** a scenario lists the same endpoint in two interactions and a client calls it twice in order
- **THEN** the stub returns the first interaction's response, then the second interaction's response

#### Scenario: Unspecified parameters act as wildcards

- **WHEN** an interaction matcher specifies `eid` and `energy_level` but not `sid`, and a request matches method, path, `eid`, and `energy_level` with any `sid`
- **THEN** the stub treats it as a match

### Requirement: Unexpected calls fail loudly

When a request does not match the current interaction (wrong path or parameters, an ECU id with no scenario file, or a cursor advanced past the last interaction), the stub SHALL return a diagnostic HTTP 4xx response that is distinct from an EMA response body and SHALL NOT advance the cursor. The diagnostic SHALL convey what was expected versus what was received. Authored EMA error responses (e.g. `code` `1001`) SHALL be expressible as ordinary interaction responses and SHALL NOT be treated as unexpected calls.

#### Scenario: Request not matching the next interaction is rejected

- **WHEN** a request does not match the current interaction's matcher for its ECU id
- **THEN** the stub responds with an HTTP 4xx diagnostic indicating the mismatch and does not advance the cursor

#### Scenario: Request for an ECU id with no scenario is rejected

- **WHEN** a request targets an ECU id that has no loaded scenario file
- **THEN** the stub responds with an HTTP 4xx diagnostic and does not return EMA success data

#### Scenario: Call after the last interaction is rejected

- **WHEN** all interactions for an ECU id have been consumed and another request arrives for it
- **THEN** the stub responds with an HTTP 4xx diagnostic

#### Scenario: Scripted EMA error is served normally

- **WHEN** the current interaction's response is an authored EMA error body (non-zero `code`) and the request matches its matcher
- **THEN** the stub returns that EMA error body and advances the cursor, treating it as a normal match

### Requirement: Cursor reset control endpoint

The stub SHALL expose a control endpoint to reset cursor state: `POST /__stub__/reset` resets all ECU cursors, and `POST /__stub__/reset/{eid}` resets the cursor for one ECU id. Control endpoints SHALL be namespaced away from the EMA API surface.

#### Scenario: Reset all cursors

- **WHEN** a client sends `POST /__stub__/reset`
- **THEN** every ECU's cursor returns to its first interaction

#### Scenario: Reset a single ECU cursor

- **WHEN** a client sends `POST /__stub__/reset/{eid}`
- **THEN** only that ECU's cursor returns to its first interaction

### Requirement: Good Data scenario — ECU minutely current production

The bundled default scenario set SHALL include a "Good Data" ECU (a real-format ECU id) whose first interaction matches `GET /user/api/v2/systems/{sid}/devices/ecu/energy/{eid}` with `energy_level=minutely` and returns the documented minutely body (`today`, `time`, `power`, `energy` parallel arrays). The most recent `power` value SHALL be **8000** (current production = 80% of a 10 kW array), in watts, and the `time`, `power`, and `energy` arrays SHALL be of equal length.

#### Scenario: Minutely request for the Good Data ECU returns current production of 8000 W

- **WHEN** a client requests the ECU minutely energy endpoint with `energy_level=minutely`, any `sid`, and the Good Data ECU id, as the first call for that ECU
- **THEN** the stub responds with `code` `0` and a `data` object whose `time`, `power`, and `energy` arrays have equal length
- **AND** the last element of `power` equals `8000`

### Requirement: Documented usage

The stub SHALL be documented in `docs/` covering how to run it, the configurable port and scenario directory, the scenario-file schema (ordered interactions, request matchers, responses), the matching/ordering and reset model, the list of implemented endpoints, and how to point the Companion app and integration tests at its base URL.

#### Scenario: Documentation enables a developer to run the stub

- **WHEN** a developer follows the stub documentation
- **THEN** they can start the server, author or edit per-ECU scenario files, understand matching/ordering and reset, and configure a client to use the stub's base URL
