## Purpose

Isolates all EMA API access — request signing, typed results, and endpoint calls — behind a single client component so UI code never talks to the network directly.

## Requirements

### Requirement: Separated EMA API client
The app SHALL access the EMA API exclusively through a dedicated client component in `core/` that is independent of any UI (Fragment/Activity) code. UI code SHALL NOT construct HTTP requests, build signatures, or parse EMA response bodies directly. The client SHALL read its base URL and credentials (App Id, App Secret, System Id, ECU Id) from the persisted settings.

#### Scenario: UI does not perform HTTP directly
- **WHEN** the Home screen needs the current production value
- **THEN** it SHALL obtain it through the EMA API client component, not by issuing HTTP calls itself

#### Scenario: Client is unusable without configuration
- **WHEN** the app is not fully configured (`isConfigured()` is false)
- **THEN** the client SHALL NOT issue a request and SHALL report a configuration error rather than calling the API

### Requirement: HMAC request signing
Every EMA API request SHALL include the headers `X-CA-AppId`, `X-CA-Timestamp` (Unix time in **milliseconds**), `X-CA-Nonce` (32-character UUID without dashes), `X-CA-Signature-Method` (`HmacSHA256`), and `X-CA-Signature`. The signature SHALL be the Base64 of an HMAC-SHA256 over the string `{timestamp}/{nonce}/{appId}/{lastPathSegment}/{method}/{signatureMethod}`, keyed by the App Secret, where `lastPathSegment` is the final path segment after substituting all path parameters.

#### Scenario: Signature uses the last path segment
- **WHEN** the client requests `GET /user/api/v2/systems/{sid}/devices/ecu/energy/{eid}` with `{eid}` = `203000001234`
- **THEN** the signed string SHALL use `203000001234` as the path segment (not the full path)

#### Scenario: Timestamp is in milliseconds
- **WHEN** the client builds the `X-CA-Timestamp` header
- **THEN** it SHALL be the current Unix time in milliseconds

### Requirement: Typed request results
The client SHALL return a typed result distinguishing at least: success (with parsed data), a network/unreachable failure, and an API/business error (non-zero EMA `code` or HTTP error). The client SHALL NOT throw raw exceptions across its boundary for these expected outcomes.

#### Scenario: Successful read
- **WHEN** the API returns `code: 0` with a valid body
- **THEN** the client SHALL return a success result carrying the parsed value

#### Scenario: Network unreachable
- **WHEN** the request fails to reach the server (connection refused, timeout, no network)
- **THEN** the client SHALL return a network-failure result (not a success and not an API error)

#### Scenario: API business error
- **WHEN** the API responds with a non-zero `code` or an HTTP error status
- **THEN** the client SHALL return an API-error result carrying the code/status

### Requirement: Current production read
The client SHALL expose an operation that returns the current production power. It SHALL call the ECU energy endpoint at minutely level (`GET /user/api/v2/systems/{sid}/devices/ecu/energy/{eid}?energy_level=minutely&date_range={today}`) for the configured System Id and ECU Id, and SHALL derive the current production as the **last** element of the response `data.power` array, expressed in watts (W).

#### Scenario: Current production is the latest power sample
- **WHEN** the response `data.power` is `[1500, 3200, 5000, 6500, 7400, 8000]`
- **THEN** the current production SHALL be reported as `8000 W`

#### Scenario: No samples available
- **WHEN** the response contains an empty `data.power` array
- **THEN** the client SHALL return an API-error / no-data result rather than a success
