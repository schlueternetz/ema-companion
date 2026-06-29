# Companion app test configs

Importable Companion settings configs (the JSON shape produced by
`SettingsRepository.exportToJson` / consumed by `importFromJson`) for tests that drive the
app against the [EMA API stub](../../../../../../../docs/ema-api-stub/README.md).

Each config pairs with a stub ECU scenario: `emaEcuId` and `systemCapacity` match the
scenario the stub serves, and `baseUrl` points at the running stub.

| Config | Stub ECU id | System size | Scenario |
|---|---|---|---|
| `good-data.json` | `203000001234` | 10 kW | Good Data — healthy array at 80% (8000 W) |

## Notes

- These are in `src/debug/assets/` so Robolectric can read them (the debug-variant asset
  merge); `src/test/assets/` is **not** on the Robolectric asset path.
- `baseUrl` uses `http://localhost:8080/...` for host-JVM/Robolectric runs. From an Android
  **emulator** use `http://10.0.2.2:8080/...` to reach the host, and start the stub on
  port 8080.
- The app's base URL includes the `/user/api/v2/` suffix (matching `BASE_URL_DEFAULT`); the
  stub serves EMA user endpoints under that prefix.
- `emaAppId` / `emaAppSecret` are dummy values — the stub does not verify HMAC signing, but
  the app requires them non-empty for `isConfigured()`.
- `emaSystemId` is arbitrary: the stub wildcards the `{sid}` path param.
