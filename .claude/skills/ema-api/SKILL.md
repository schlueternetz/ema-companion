---
name: ema-api
description: Load APsystems EMA API context before implementing any API integration. Use whenever working on API client code, stub servers, data models, or request signing. Do NOT use for general Android feature work unrelated to the EMA API.
allowed-tools: Read Glob
---

Load the EMA API documentation before starting any API-related implementation work. This ensures request signing, path prefixes, and response schemas are implemented correctly.

## Step 1 — Read the API manual

Read `docs/ema-api/apsystems-openapi-manual.md`.

Pay particular attention to:

- **Section 2.2 — Request signing:** The HMAC string-to-sign format is `{timestamp}/{nonce}/{appId}/{lastPathSegment}/{method}/{signatureMethod}`. The `lastPathSegment` is the final segment of the URL path after substituting path parameters (e.g. for `/user/api/v2/systems/details/XYZ123` it's `XYZ123`).
- **Section 2.4 — Base URL and path prefixes:** `https://api.apsystemsema.com:9282`. User endpoints use `/user/api/v2/`, Storage endpoints use `/installer/api/v2/`. This is a common source of bugs.
- **Section 3.1.1 — System details:** The `ecu` field uses a `{mainEcu}-{virtualEcu}` format for shared sub-users; the virtual ECU id (after the `-`) is what subsequent device queries expect.
- **Section 3.5.1 — Inverter summary:** Response uses per-channel fields `d1/m1/y1/t1` through `d4/m4/y4/t4`, not a flat `{today, month, year, lifetime}`.
- **Section 3.5.2 — Inverter minutely:** Returns full DC/AC telemetry (`dc_p1`–`dc_p4`, `dc_i1`–`dc_i4`, `dc_v1`–`dc_v4`, `dc_e1`–`dc_e4`, `ac_v1`–`ac_v3`, `ac_t`, `ac_p`, `ac_f`).
- **Section 3.6 — Storage API:** Uses `/installer/api/v2/` prefix, not `/user/api/v2/`.

## Step 2 — Read the OpenAPI spec (optional but recommended for client codegen or stub server work)

Read `docs/ema-api/apsystems-openapi.yaml`.

This has all 15 endpoints with request parameters, response schemas, and inline examples. Useful as a machine-readable reference when generating clients or configuring mock servers.

## Step 3 — Check the Postman collection for real response examples

The file `docs/ema-api/APsystems OpenAPI.postman_collection.json` contains saved real response bodies for most endpoints. Useful for:
- Verifying data types (e.g. are power values strings or numbers?)
- Confirming null fields
- Spot-checking edge cases

## Common pitfalls

| Pitfall | Correct behavior |
|---|---|
| Using `/user/api/v2/` for storage | Storage uses `/installer/api/v2/` |
| Inverter summary: flat `{today, month, year, lifetime}` | Use `d1/m1/y1/t1` through `d4/m4/y4/t4` |
| Signing: using full path as `RequestPath` | Use only the **last segment** of the path |
| Timestamp in seconds | Must be **milliseconds** |
| Using main ECU id for device queries on shared sub-user accounts | Parse `{mainEcu}-{virtualEcu}`, use the virtual ECU id |
| Meter summary: flat `{today, month, year, lifetime}` strings | Each period has `{consumed, exported, imported, produced}` breakdown |
| Batch inverter energy: `{uid: [values]}` | Keys are `"{uid}-{channel}"` strings |
