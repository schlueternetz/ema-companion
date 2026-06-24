# ADR-006: Tile Error Display Pattern

**Status:** Accepted  
**Date:** 2026-06-19

## Context

The Home screen shows multiple data tiles (Current Production, Module Health). Each tile fetches live data from the EMA API on every `onResume`. Fetches can fail for four distinct reasons:

- **Network** — no connection or timeout; never reached EMA.
- **Auth** — credentials rejected by EMA (codes 2000–2004, 3000–3004; HTTP 401/403).
- **API** — reached EMA but it returned some other error (bad parameters, server error, code ≠ 0).
- **Configuration** — required settings have not been entered; no request was issued.

The question is how a tile should communicate a fetch failure to the user.

Popups (AlertDialog, Snackbar) interrupt focus and feel like crashes. Silently preserving stale data with no signal is worse — the user cannot know whether a reading is live or hours old.

## Decision

All Home tiles use the **inline status-line pattern** for fetch errors:

1. **Always show the last known data** (or a neutral placeholder such as "— W" or "Checking…" for a first fetch that has never succeeded). Tiles never go blank on error.
2. **Show an inline status line** immediately below the data text when the latest fetch failed. The line is visible without any tap and distinguishes failure mode:
   - *Network* → the network-issue string.
   - *Auth* → the authentication-failed string (directs the user to check credentials in Settings).
   - *API* → the generic data-unavailable string.
3. **No dialog, toast, or popup** for transient fetch errors. Failures are non-disruptive — visible on the tile, but requiring no dismissal.
4. **Clear the status line** automatically the moment a later fetch succeeds.
5. **Persist the error state** in SharedPreferences alongside the tile's data. Fragment recreation (navigating away and back, dark-mode toggle, process restart) must show the same inline status without re-fetching.
6. **`ConfigurationError` is silent.** When required settings have not been entered, the tile shows the neutral placeholder and no error line. Prompting is handled by the Settings screen; the tile has nothing actionable to add.
7. **Each tile manages its own error state independently.** The error field lives in the tile's own state class (`ProductionState.error`, `ModuleHealthState.error`) and is persisted in the tile's own SharedPreferences store.

## Alternatives Considered

**Snackbar / Toast:** Transient, not persisted, and disappears before the user reads it. Provides no memory of which tile failed or why after the user looks away.

**AlertDialog per error:** Disruptive — the user must dismiss it to continue. Feels like a crash. Does not survive screen rotation. Rejected.

**Red card border / icon only (no text):** Accessible only to users who know what the colour/icon means. Screen readers cannot convey the failure mode. Rejected on accessibility grounds.

**Do nothing / silent stale data:** Already the behaviour of the Module Health tile before this ADR. Leaves the user with no signal that the displayed status may be outdated. Rejected.

## Consequences

- Fetch errors are non-disruptive but always visible; the user can see at a glance that a tile's data is stale and why.
- All future Home tiles must follow this pattern. Dialogs for fetch errors are prohibited.
- Each tile's state class must carry a `nullable FetchError?` field; reuse the shared `FetchError` enum (`core/api/FetchError`) rather than inventing per-tile error types.
- Each tile's repository must persist and restore the error so `currentState()` faithfully reconstructs the full rendered state (value + timestamp + error).
- The tile XML must include a status-line `TextView` (initially `gone`) positioned below the primary data text.
