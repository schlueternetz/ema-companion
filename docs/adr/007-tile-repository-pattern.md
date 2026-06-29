# ADR-007: Tile Repository Pattern

**Status:** Accepted  
**Date:** 2026-06-23

## Context

The Home screen is composed of independent data tiles (e.g. current production, module health). Each tile fetches from the EMA API on a throttled schedule and persists its last-known state so it can render immediately on screen re-entry without flicker.

As a second tile was added, a recurring set of integration points emerged that every tile repository must satisfy:

1. **Synchronous current state** — the fragment needs to render immediately from persisted store before any network call.
2. **Throttled refresh** — each tile enforces its own fetch interval via a persisted timestamp.
3. **Throttle reset on settings change** — when connection credentials or base URL change (per-field edit, import, or factory reset), every tile's throttle must be cleared so the next Home visit fetches with the new config.
4. **Factory reset** — every tile's persisted state must be erasable atomically.

Without a documented pattern, each new tile risks missing one of these integration points (the immediate trigger for this ADR was a bug where the module health 24-hour throttle was never reset on settings import, while the production tile was correctly reset).

## Decision

Every Home tile repository must satisfy the following contract:

### 1. Tile source interface
Define a tile-specific source interface in `core/api/<domain>/` with two methods:
- `currentState(): TState` — reads from SharedPreferences only, no network, no suspension; called synchronously in `onViewCreated` for the initial render.
- `suspend fun refresh(): TState` — checks throttle, fetches if due, persists result, returns updated state; called in `onResume`.

### 2. ThrottleResettable
The repository class implements `core/api/ThrottleResettable`:
```kotlin
interface ThrottleResettable {
    fun resetThrottle()
}
```
`resetThrottle()` must remove the persisted throttle timestamp and clear any persisted fetch error, restoring the "never fetched" state so the next `refresh()` call runs immediately.

### 3. Register in SettingsFragment.tileRepositories
`SettingsFragment` maintains:
```kotlin
private lateinit var tileRepositories: List<ThrottleResettable>
```
initialised in `onViewCreated` with every tile repo instance. `invalidateApiThrottle()` iterates this list — no tile-specific code in the method body. Adding a new tile = add it to this list.

### 4. clear() for factory reset
Each tile repository exposes a `clear()` method that wipes all its SharedPreferences stores. `SettingsFragment.showFactoryResetDialog()` calls `clear()` on each repo instance directly (not via a shared interface — see Consequences).

### Current tiles

| Tile | Source interface | Repository | SharedPreferences |
|------|-----------------|------------|-------------------|
| Current production | `ProductionSource` | `ApiUsageRepository` | `ema_api_usage` |
| Module health | `ModuleHealthSource` | `ModuleHealthRepository` | `ema_module_health`, `ema_module_health_daily` |

### Test requirement
`SettingsFragmentTest.setUp()` must clear every SharedPreferences store owned by a tile repo so tests do not leak state across runs.

## Alternatives Considered

**Unified `TileRepository<TState>` interface combining all four concerns**: rejected as premature with only two tiles. Generic bounds across heterogeneous state types add complexity without a demonstrated third case to validate the shape. Revisit when a third tile is added.

**A `Clearable` interface parallel to `ThrottleResettable`**: rejected for the same reason. Factory reset currently involves only two tile repos; a typed list and direct `clear()` calls are readable without the interface. Extract when a third tile makes the pattern unmistakable.

**Screen-level throttle controller**: rejected. Tiles have different intervals (10 min vs 24 h) and different reset triggers (production resets on any credential change; a future tile might reset only on specific field changes). Per-tile ownership of throttle state is simpler and avoids coupling tiles to each other.

## Consequences

- **New tile checklist**: implement the source interface + repository, implement `ThrottleResettable`, add to `tileRepositories` in `SettingsFragment.onViewCreated`, add a `clear()` call to `showFactoryResetDialog()`, add the SharedPreferences store name to `SettingsFragmentTest.setUp()`.
- **Known inconsistency**: `ModuleHealthRepository` does not expose a public `clear()` method — its factory reset clears the two prefs stores directly in `SettingsFragment`. This should be fixed (add `clear()` to the repo and call it from the fragment) when adding a third tile, at which point extracting a `Clearable` interface becomes worthwhile.
- **`SettingsFragment` is the registration point** — it is intentionally the single place that knows all tile repos. If the fragment grows unwieldy, extract a `TileRegistry` helper, but do not scatter registration across feature modules.
