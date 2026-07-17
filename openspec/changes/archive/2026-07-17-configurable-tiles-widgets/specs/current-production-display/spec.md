## REMOVED Requirements

### Requirement: Home shows current production in a tile
**Reason**: Duplicates the instantaneous production reading already shown in the original APsystems EMA app. EMA Companion is designed to complement that app, not replace or re-implement its screens (README), and the tile consumed EMA API budget (~150 calls/month, ADR-009) for data the user can already see elsewhere.
**Migration**: View current production in the official EMA App. Home now opens directly with the Today Production tile.

### Requirement: Fetch on app open and on Home highlight
**Reason**: Same as above — the Current Production tile and its fetch trigger are removed entirely.
**Migration**: None; there is no replacement fetch. The remaining tiles (Today Production, History Production, Module Health) keep their own independent fetch triggers, unaffected by this removal.

### Requirement: At most one successful call per ten minutes
**Reason**: The 10-minute throttle existed solely to bound the removed current-production fetch.
**Migration**: None; `ProductionRepository` and its throttle state are deleted along with the tile.

### Requirement: Per-tile fetch-error status
**Reason**: The inline status-line pattern (ADR-006) continues to apply to all remaining tiles; only the Current Production tile's instance of it is removed along with the tile itself.
**Migration**: None; the shared `FetchError` enum and inline-status pattern remain in use by Today Production, History Production, and Module Health.
