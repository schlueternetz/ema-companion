# EMA API Stub

A standalone Kotlin (Ktor) HTTP server that mocks the APsystems EMA API for local
development and automated integration tests. It is a deterministic test double — it
does **not** replace the real API and is never shipped inside the Companion app.

It works as a **record/replay** mock: behaviour is driven by external **scenario files,
one JSON file per ECU id**. Each file is an *ordered* list of interactions (an expected
request + the response to return). The stub matches incoming calls against a per-ECU
cursor in strict order and fails loudly when a call does not match what the scenario
expects.

## Build & run

The stub is its own Gradle project under [`code/ema-api-stub/`](../../code/ema-api-stub/)
(separate from the Android app build).

```bash
cd code/ema-api-stub
./gradlew run            # start the server (Ctrl+C to stop)
./gradlew test          # unit + integration tests
./gradlew ktlintCheck   # lint
```

### Configuration

| Setting | Env var | CLI arg | Default |
|---|---|---|---|
| Listening port | `STUB_PORT` | 1st arg | `8080` |
| Scenario directory | `STUB_SCENARIO_DIR` | 2nd arg | bundled `src/main/resources/scenarios/` |

```bash
STUB_PORT=8089 STUB_SCENARIO_DIR=/path/to/scenarios ./gradlew run
# or
./gradlew run --args="8089 /path/to/scenarios"
```

When no scenario directory is given, the stub loads the bundled default set from the
classpath (this works under `./gradlew run`; a packaged fat-jar is out of scope).

The base URL to point a client at is then `http://localhost:<port>` — e.g.
`http://localhost:8080`. EMA user endpoints live under `/user/api/v2/`.

## Scenario file schema

One file per ECU id. The ECU id in the file is authoritative (the filename is just for
humans). Example — the bundled
[`203000001234.json`](../../code/ema-api-stub/src/main/resources/scenarios/203000001234.json):

```json
{
  "ecuId": "203000001234",
  "description": "Good Data — healthy 10kW array currently producing at 80% (8000 W)",
  "interactions": [
    {
      "request": {
        "method": "GET",
        "path": "/user/api/v2/systems/{sid}/devices/ecu/energy/{eid}",
        "pathParams": { "eid": "203000001234" },
        "query": { "energy_level": "minutely" }
      },
      "response": {
        "body": { "code": 0, "data": { "time": ["…"], "power": ["…", 8000], "energy": ["…"] } }
      }
    }
  ]
}
```

| Field | Meaning |
|---|---|
| `ecuId` | The ECU id this scenario serves (EMA-format opaque string, e.g. `203000001234`). |
| `description` | Free-text label for humans. |
| `interactions[]` | Ordered list of expected calls and their responses. |
| `interactions[].request.method` | HTTP method to match (default `GET`). |
| `interactions[].request.path` | Path **template** to match (e.g. `/user/api/v2/systems/{sid}/devices/ecu/energy/{eid}`). |
| `interactions[].request.pathParams` | Path params to assert. Only listed params are checked; others are wildcards. |
| `interactions[].request.query` | Query params to assert. Only listed params are checked; others are wildcards. |
| `interactions[].response.status` | HTTP status to return (default `200`). |
| `interactions[].response.body` | The verbatim JSON body to return (e.g. an EMA `{ "code": 0, "data": … }`). |

### Matching rules

- **Strict sequential.** The Nth call for an ECU id must match the Nth interaction's
  request matcher. On a match the response is returned and the cursor advances.
- **Wildcards.** Path/query params not named in the matcher are not checked — so pinning
  `eid` and `energy_level` while leaving `sid` unspecified accepts any system id.
- **Repeated endpoints.** Listing the same endpoint in two interactions scripts a
  multi-call flow: the two responses are returned in order.
- **Scripted EMA errors are normal responses.** To simulate a fault, author an EMA error
  body (e.g. `{ "code": 1001 }`) as an interaction response — it is served on match like
  any other, and is **not** treated as an unexpected call.

### Unexpected calls

A request that does not match the next interaction — wrong path/params, an ECU id with no
scenario file, or a call after the last interaction — returns **HTTP 409** with a
diagnostic body and does **not** advance the cursor:

```json
{
  "error": "unexpected_call",
  "reason": "Request does not match interaction #0 for ECU id '203000001234'",
  "expected": { "method": "GET", "path": "…", "pathParams": {…}, "query": {…} },
  "actual":   { "method": "GET", "path": "…", "pathParams": {…}, "query": {…} }
}
```

A path the stub does not recognize at all returns **HTTP 404**.

### Control endpoints

Cursor state is mutable, so tests reset it between scenarios:

| Request | Effect |
|---|---|
| `POST /__stub__/reset` | Reset every ECU's cursor to its first interaction. |
| `POST /__stub__/reset/{eid}` | Reset only that ECU's cursor. |

## ECU-id scenarios

The ECU id selects the scenario. Add a new ECU id by dropping another JSON file in the
scenario directory — no rebuild needed. The bundled set currently ships:

| ECU id | Scenario | Implemented endpoints |
|---|---|---|
| `203000001234` | Good Data (healthy 10 kW array at 80%) | ECU energy in period, in order: `energy_level=minutely` → current production **8000 W**, `energy_level=hourly`, `energy_level=daily` |
| `203000005678` | Module Health — all inverters producing (GREEN) | Batch inverter energy, `energy_level=energy` |
| `203000009012` | Module Health — one inverter offline today only (YELLOW) | Batch inverter energy, `energy_level=energy` |
| `203000003456` | Module Health — one inverter offline 3 days (RED) | Batch inverter energy, `energy_level=energy` (3 interactions, replayed for day-before/yesterday/today) |

Future scenarios (e.g. a faulty module or an offline ECU) are added as additional
per-ECU files that script the relevant responses (including EMA error bodies).

Dedicated no-data (`code:1001`) and two-calendar-month `daily` fixtures for `hourly`/`daily`
are not part of the bundled set above — they're written directly into scoped test
directories (not `loadDefault()`) by the tests that need them, so they stay decoupled from
the bundled scenario files.

## Using the stub from the Companion app / integration tests

- Point the client's base URL at the stub (the Companion app has a configurable base URL;
  integration tests per [ADR-002](../adr/002-testing-strategy.md) hit a local mock with a
  configurable base URL). Use `http://10.0.2.2:<port>` from an Android emulator to reach
  the host.
- In debug builds only, Settings has a one-tap **Use local stub** action that points the
  Base URL at `http://10.0.2.2:{STUB_PORT}/user/api/v2/`. `STUB_PORT` is a Gradle property
  (`-PSTUB_PORT=` or `local.properties`) read by `code/ema-companion/app/build.gradle.kts`
  into `BuildConfig.STUB_PORT` — the same name the stub server itself reads, defaulting to
  `8080` on both sides.
- The Companion app's `code/ema-companion/settings.gradle.kts` uses `includeBuild("../ema-api-stub")`
  so its `app` module can depend on this project directly (`testImplementation`) and embed
  the real `MatchingEngine`/`ScenarioLoader` in JVM/Robolectric tests — a real `embeddedServer`
  bound to an ephemeral port (`port = 0`), not a real deployed process, so `OkHttpEmaApiClient`
  can hit it over an actual (but in-process, no external port to manage) socket.
- For embedded Ktor tests, construct a fresh `MatchingEngine(ScenarioLoader.loadDefault())`
  (or `loadFromDirectory` against a scoped directory — the pattern this project's own tests
  and the Companion app's embedded-stub tests use) per test (no shared cursor state). For a
  long-lived server, call `POST /__stub__/reset` before each scenario.
- `ScenarioLoader.loadDefault()` requires an exploded resources directory on the classpath —
  it cannot list a packaged jar's contents. Code consuming this project as a packaged jar
  (as the Companion app does via the composite build) should read bundled scenario files as
  a classpath resource (`getResourceAsStream`) instead, not `loadDefault()`.

## Caveats

- **No HMAC verification.** The stub ignores the `X-CA-*` signing headers — it is a data
  test double, not an auth gate. Do not use it to test request signing.
- Only the endpoints listed above are implemented; everything else is an unexpected call
  until a scenario provides it.
