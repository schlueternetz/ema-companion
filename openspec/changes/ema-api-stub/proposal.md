## Why

Developing and testing the EMA Companion app against the real APsystems EMA API is slow, costs metered API calls, and depends on live solar-array conditions we cannot control (you cannot make the sun produce exactly 80% on demand). We need a local, deterministic stub that returns controllable EMA API responses so feature work and automated integration tests run offline against known data.

## What Changes

- Add a new standalone Kotlin (Ktor) application in `code/ema-api-stub/` that serves a subset of the APsystems EMA API over local HTTP.
- Behaviour is **data-driven from external scenario files — one JSON file per ECU id** — loaded at startup from a configurable directory. No responses are hardcoded in the app.
- Each scenario file is an **ordered list of interactions** (`request` matcher + `response`). The stub keeps a per-ECU-id cursor and serves responses in the order defined; the same endpoint can appear multiple times to script a multi-call flow.
- **Strict sequential verification**: the Nth call for an ECU id must match the Nth interaction's `request` matcher (method, path, specified path/query params; unspecified params act as wildcards). A mismatch returns a loud **HTTP 4xx "unexpected call" diagnostic** (expected-vs-actual), distinct from EMA bodies. Genuine EMA error responses are authored as ordinary scripted responses.
- Expose a **reset control endpoint** (`POST /__stub__/reset`, and `/__stub__/reset/{eid}`) so integration tests can reset per-ECU cursors; embedded Ktor `testApplication` tests load fresh.
- Ship a default scenario file for a **"Good Data"** ECU (real-format id, e.g. `203000001234`) whose first interaction is **ECU Energy in Period (minutely)** — `GET /user/api/v2/systems/{sid}/devices/ecu/energy/{eid}?energy_level=minutely` — returning a `power` array whose latest value represents **current production of 8000 W (80% of a 10 kW array)**.
- Add documentation in `docs/` covering how to run the stub, the scenario-file schema, the matching/ordering and reset model, and how to point the Companion app / integration tests at it.

## Capabilities

### New Capabilities
- `ema-api-stub`: A configurable, data-driven record/replay mock of the APsystems EMA API. Loads one ordered scenario file per ECU id, serves responses in sequence with strict per-ECU matching, fails loudly on unexpected calls, supports cursor reset, and ships a "Good Data" ECU whose ECU minutely endpoint reports current production = 80% of 10 kW.

### Modified Capabilities
<!-- None. This adds a standalone test-support app; it does not change Companion app requirements. -->

## Impact

- **New app**: `code/ema-api-stub/` (Kotlin + Ktor, its own Gradle build), plus a default per-ECU scenario file.
- **New docs**: `docs/ema-api-stub/` describing usage, the scenario-file schema, and the matching/reset model.
- **No change** to the Companion app source. The stub is consumed via its configurable base URL (per ADR-002 integration tests and the app's existing base-url configuration).
- **Dependencies**: adds Ktor server and a Kotlin test/JSON toolchain scoped to the new stub module only.
