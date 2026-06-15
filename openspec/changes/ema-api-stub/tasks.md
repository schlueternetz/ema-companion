## 1. Project scaffold

- [x] 1.1 Create `code/ema-api-stub/` as a standalone Gradle (Kotlin JVM) project with its own wrapper, `settings.gradle.kts`, and `build.gradle.kts`
- [x] 1.2 Add dependencies: Ktor server (CIO engine), Ktor content-negotiation + kotlinx.serialization JSON, and Ktor test host + JUnit for tests
- [x] 1.3 Add ktlint to the stub build and confirm `./gradlew ktlintCheck` runs against the module's `.kt` files
- [x] 1.4 Add a `main()` entry point that reads the port and scenario-directory path from arg/env (documented defaults), with `./gradlew run`

## 2. Scenario model & loading (AI-TDD)

- [x] 2.1 Write failing test: parse a per-ECU scenario file into a model (`ecuId`, ordered `interactions` of `{ request matcher, response }`) with response body as a generic JSON tree
- [x] 2.2 Implement the kotlinx.serialization scenario model + request-matcher model (method, path, pathParams, query, optional response status) to pass 2.1
- [x] 2.3 Write failing test + implement: load all files in the scenario directory, index by `ecuId`; missing/malformed file fails fast with a clear error

## 3. Matching engine with per-ECU cursor (AI-TDD)

- [x] 3.1 Write failing test: request matches the current interaction's matcher (method + path template + specified pathParams/query; unspecified params are wildcards) → returns its response, cursor advances
- [x] 3.2 Implement the matcher + per-ECU cursor advance to pass 3.1
- [x] 3.3 Write failing test + implement: same endpoint listed twice returns the two responses in order
- [x] 3.4 Write failing test + implement: a scripted EMA error body (non-zero `code`) is served as a normal match and advances the cursor

## 4. Unexpected-call diagnostics (AI-TDD)

- [x] 4.1 Write failing test: request not matching the current interaction → HTTP 4xx diagnostic (expected vs actual), cursor does NOT advance
- [x] 4.2 Write failing test: request for an ECU id with no scenario file → HTTP 4xx diagnostic
- [x] 4.3 Write failing test: call after the last interaction is consumed → HTTP 4xx diagnostic
- [x] 4.4 Implement the diagnostic error path to pass 4.1–4.3

## 5. HTTP wiring & reset endpoint (AI-TDD with Ktor testApplication)

- [x] 5.1 Write failing route test: `GET /user/api/v2/systems/{sid}/devices/ecu/energy/{goodDataEid}?energy_level=minutely` as first call → `code 0`, last `power` is 8000
- [x] 5.2 Implement the EMA route(s) + content negotiation, dispatching to the matching engine, to pass 5.1
- [x] 5.3 Write failing test + implement: unrecognized route → HTTP 404
- [x] 5.4 Write failing test + implement: `POST /__stub__/reset` resets all cursors; `POST /__stub__/reset/{eid}` resets one

## 6. Shipped Good Data scenario (AI-TDD)

- [x] 6.1 Write failing scenario test: load the bundled default scenario set, read the Good Data ECU's first interaction, assert it matches `ecu/energy?energy_level=minutely`, `time`/`power`/`energy` equal length, and `power.last() == 8000`
- [x] 6.2 Author the default Good Data scenario file with a real-format ECU id (e.g. `203000001234`) and the minutely interaction (current = 8000 W = 80% of 10 kW) to pass 6.1

## 7. Documentation

- [x] 7.1 Create `docs/ema-api-stub/` describing how to build/run the stub, the configurable port and scenario directory, and the base URL to use
- [x] 7.2 Document the scenario-file schema (ECU id, ordered interactions, request matchers, responses), strict-sequential matching, wildcard params, the reset endpoints, and how to author faulty/offline scenarios
- [x] 7.3 Document how to point the Companion app (base-url configuration) and integration tests at the stub (including cursor reset between scenarios) and the no-HMAC-verification caveat

## 8. Verification

- [x] 8.1 Run the full stub test suite green (`./gradlew test`)
- [x] 8.2 Run `./gradlew ktlintCheck` clean on the stub module
- [x] 8.3 Manually start the stub and `curl` the Good Data minutely call (confirm `power` ends at 8000), an unexpected call (confirm 4xx diagnostic), and `POST /__stub__/reset`
