## Context

The EMA Companion app integrates with the APsystems EMA API (`https://api.apsystemsema.com:9282`, HMAC-signed, metered, live-data-dependent). ADR-002 already calls for integration tests that "hit the local mock API service (real HTTP, configurable base URL)" — this change builds that service. It is the first of several stub-development tasks; later tasks add more scenarios and endpoints.

Decisions already made with the user:
- **Runtime:** Kotlin + Ktor, keeping one language/toolchain with the Android app.
- **Behaviour is data-driven, one scenario file per ECU id:** each ECU id has its own JSON file (loaded from a configurable directory) holding an ordered list of interactions. Nothing is hardcoded. Format is JSON, parsed with kotlinx.serialization (also used for response serialization — zero extra deps).
- **Record/replay with strict sequential matching:** the stub keeps a per-ECU-id cursor; the Nth call for an ECU must match the Nth interaction's request matcher, else it's a loud "unexpected call" error. Order of interactions = order responses are returned, so the same endpoint can recur for multi-call flows.
- **Reset control endpoint** for per-ECU cursor state; embedded tests load fresh.
- **First endpoint:** ECU Energy in Period at `energy_level=minutely`, whose `power` array is in watts — the natural carrier for an instantaneous "current production" value.
- **First value:** current production = 8000 W (80% of a 10 kW array), as the last element of `power`, authored in the shipped "Good Data" scenario file's first interaction.
- **ECU id format:** the EMA API types `eid` as an opaque string. Real ids are ~12-digit numeric (e.g. `203000001234`), with an optional shared sub-user form `{mainEcu}-{virtualEcu}`. The stub treats `eid` as an opaque string and scenario files use real-format ids; `1` is not used.

## Goals / Non-Goals

**Goals:**
- A standalone, locally runnable Ktor server in `code/ema-api-stub/` with its own Gradle build.
- A record/replay engine: load one ordered scenario file per ECU id; on each request resolve the ECU id, match the request against that ECU's next interaction (strict sequential), serve its response, and advance the cursor; on mismatch return a loud diagnostic error.
- A reset control endpoint (`POST /__stub__/reset`, `/__stub__/reset/{eid}`) for per-ECU cursor state.
- A shipped default scenario file defining a "Good Data" ECU (real-format id) whose first interaction is the ECU minutely endpoint reporting 8000 W current production.
- Docs in `docs/ema-api-stub/` for running it, the scenario-file schema, and the matching/ordering/reset model.
- Unit/integration tests (AI-TDD, ktlint-clean) covering the matching engine, sequencing, unexpected-call diagnostics, reset, and the shipped Good Data scenario (last power = 8000, equal-length arrays).

**Non-Goals:**
- HMAC signature verification — the stub does not validate `X-CA-*` headers (it is a local test double, not a security gate).
- Storage (`/installer/api/v2/`) endpoints, meter/inverter/system endpoints, and non-minutely energy levels — served only if/when authored in a scenario file; no handler routing for them in this change.
- Header/body request matching, regex/JSONPath matchers, response delays/templating — matchers cover method, path template, path params, and query params only.
- A scenario authoring UI/validator, or generating realistic time-series curves programmatically — curves are authored in scenario files.
- Changing any Companion app code.

## Decisions

**D1 — Ktor server, standalone Gradle module under `code/ema-api-stub/`.**
A separate Gradle project (not a module of the Android build) keeps the JVM/server toolchain isolated from the Android plugin and lets it run via a plain `java -jar` / `./gradlew run`. Rationale: the Android build pulls in AGP and device constraints irrelevant to a server; coupling them complicates both. Alternative considered: a submodule of the existing Gradle build — rejected to avoid AGP/JVM cross-contamination and slow Android builds.

**D2 — One ordered scenario file per ECU id, loaded from a directory.**
Behaviour lives in per-ECU JSON files in a configurable directory (a default set is bundled). Each file declares its `ecuId` and an ordered `interactions` array; each interaction is `{ request: <matcher>, response: <EMA body> }`. At startup the engine loads every file and indexes scenarios by `ecuId`. Rationale: the user expects many scenarios and multi-call flows; one file per ECU keeps each scenario self-contained, diffable, and addable without touching others or rebuilding. Alternative considered: a single combined config — rejected as unwieldy at the expected scenario count.

**D3 — Strict sequential matching with a per-ECU cursor.**
The engine keeps a cursor per ECU id (starting at 0). For each request it resolves the ECU id from the `eid` path param, takes the interaction at that ECU's cursor, and verifies the request against the interaction's matcher. The matcher asserts `method`, `path` (template), and any specified `pathParams` / `query` entries; **unspecified params are wildcards**. On match it serves `response.body` (default HTTP 200, overridable via `response.status`) and advances the cursor. Because order is authoritative, the same endpoint may recur to script a multi-call flow (e.g. production changing over time). Rationale: gives both ordered replay and caller verification with one mechanism, matching the user's intent. Response bodies are held as a generic JSON tree and re-emitted verbatim so any EMA schema is expressible without code changes. Alternative considered: match-any-pending — rejected for weaker order verification.

**D4 — Loud unexpected-call diagnostics; EMA errors are just scripted responses.**
A request that does not match the next interaction (wrong path/params, unknown ECU id, or cursor past the end) returns a distinct **HTTP 4xx diagnostic** body (`{ "error": "unexpected_call", "expected": {...}, "actual": {...} }`) — not an EMA-shaped body — so test-author mistakes fail loudly. Genuine EMA error responses (e.g. a faulty-module scenario returning `code: 1001`) are authored as ordinary interaction responses. Unknown *routes* the engine doesn't recognize at all return HTTP 404. Rationale: separates "the harness called wrong" (diagnostic) from "the API legitimately returned an error" (scripted), which the stateless map design conflated.

**D5 — Reset control endpoint for state isolation.**
`POST /__stub__/reset` resets all cursors; `POST /__stub__/reset/{eid}` resets one ECU. Embedded Ktor `testApplication` tests instead construct a fresh engine per test. Rationale: cursors are mutable state; integration tests against a long-lived server need a cheap reset between scenarios without restarting the process. The `/__stub__/` prefix is namespaced away from the EMA API surface.

**D6 — Deliverable pinned by a scenario test, not the engine.**
Because the engine serves scenario files verbatim, the "current production = 8000 W" / equal-length-arrays guarantees are asserted by a test that loads the *shipped* Good Data scenario and checks its first (`ecu.energy.minutely`) interaction (last `power` == 8000; `time`/`power`/`energy` equal length). Rationale: keeps the engine generic while pinning the deliverable; the test fails loudly if the bundled scenario is edited to violate the spec.

**D7 — No HMAC verification.** Accept any/no signing headers. Rationale: the stub's purpose is deterministic data for the Companion and tests, not auth testing; verifying signatures would force test clients to implement signing against a fake secret with zero added value. Documented explicitly so it isn't mistaken for a security test surface.

## Risks / Trade-offs

- **Scenario drift from the real API schema (no compile-time check, served verbatim)** → Mitigation: author bodies from the manual/OpenAPI shapes; the scenario test (D6) asserts documented field names/units/invariants for the shipped Good Data scenario; keep the `ema-api` skill docs as source of truth.
- **Strict sequential matching is brittle for non-deterministic call order (e.g. parallel requests)** → Mitigation: documented as a deliberate trade-off for strong verification; scenarios are authored to a known call order, and a dedicated ECU id per scenario isolates flows. Revisit a relaxed mode only if a real caller needs it.
- **Mutable cursor state leaks between tests** → Mitigation: reset endpoint (D5) + fresh embedded engine per unit test; integration tests reset before each scenario.
- **Malformed/missing scenario files at startup** → Mitigation: fail fast with a clear error on parse/load failure; document the directory and ship a known-good default set.
- **Port conflicts on developer machines / CI** → Mitigation: configurable port (env var or arg) with a documented default; tests bind to an ephemeral port.
- **Two toolchains in one repo (Android + server Gradle)** → Mitigation: isolated `code/ema-api-stub/` build, documented build/run commands; no shared Gradle config with the app.

## Open Questions

- How will integration tests launch the stub (embedded via Ktor `testApplication` vs. spawning the jar)? Resolved per-test layer during implementation; both are supported (reset endpoint for a live server, fresh engine for embedded).
- Scenario-directory override mechanism (CLI arg vs. env var vs. both) — finalize in implementation; spec only requires it be configurable with a bundled default.
- Whether the `eid` is taken from the filename or a required in-file `ecuId` field (leaning: in-file `ecuId` is authoritative, filename is for humans) — finalize in implementation.
