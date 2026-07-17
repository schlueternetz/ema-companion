## Purpose

Lets the user independently enable or disable each Home tile and each home-screen widget, so unused features stop rendering, consuming EMA API budget, and cluttering the UI.

## Requirements

### Requirement: Settings expose an enable/disable checkbox for each Home tile and each widget
Settings SHALL provide a checkbox for each of the three Home tiles (Today Production, History Production, Module Health) and each of the three widgets (Today Production, Production Summary, Production History), in a "Tiles & Widgets" section. Each checkbox SHALL reflect and control that tile's or widget's enabled state independently of the others.

#### Scenario: All six checkboxes are shown
- **WHEN** the user navigates to the Settings screen
- **THEN** the "Tiles & Widgets" section SHALL show one checkbox per tile (3) and per widget (3), each labeled with that tile's or widget's name

#### Scenario: Unchecking a tile persists independently
- **WHEN** the user unchecks the Today Production tile checkbox
- **THEN** only Today Production's enabled flag SHALL change; the other five checkboxes SHALL remain in their prior state

### Requirement: All tiles and widgets default to enabled
On first launch, and for any tile/widget flag not yet present in storage, every tile and every widget SHALL be treated as enabled (checked).

#### Scenario: Fresh install shows everything enabled
- **WHEN** the app is launched for the first time (no settings previously saved)
- **THEN** all three tile checkboxes and all three widget checkboxes SHALL be checked
- **AND** all three tiles SHALL be visible on Home

### Requirement: Select All / Deselect All control
The "Tiles & Widgets" section SHALL provide a single control that checks all six checkboxes when at least one is unchecked, and unchecks all six when all are currently checked.

#### Scenario: Select All checks every box
- **WHEN** at least one of the six checkboxes is unchecked and the user activates the all-toggle control
- **THEN** all six checkboxes SHALL become checked and all corresponding flags SHALL be persisted as enabled

#### Scenario: Deselect All unchecks every box
- **WHEN** all six checkboxes are checked and the user activates the all-toggle control
- **THEN** all six checkboxes SHALL become unchecked and all corresponding flags SHALL be persisted as disabled

### Requirement: A disabled tile is hidden from Home
When a Home tile is disabled, its entire card SHALL be removed from the Home screen layout (not merely emptied of data). The remaining enabled tiles SHALL still render normally.

#### Scenario: Disabled tile card is not shown
- **WHEN** the Module Health tile is disabled and the user views Home
- **THEN** the Module Health card SHALL NOT appear on the Home screen

#### Scenario: Re-enabling a tile restores it
- **WHEN** a previously disabled tile is re-enabled in Settings and the user returns to Home
- **THEN** the tile's card SHALL reappear, showing its last-known cached value immediately

#### Scenario: Re-enabling a tile whose throttle has already elapsed fetches immediately
- **WHEN** a tile is re-enabled and its data source's throttle window had already elapsed while it was disabled (e.g. disabled longer than the throttle interval, or was already overdue when disabled)
- **THEN** the next refresh SHALL issue a fresh request immediately, with no additional wait imposed by having been disabled

#### Scenario: Re-enabling a tile whose throttle has not elapsed keeps the cached value
- **WHEN** a tile is re-enabled and its data source's throttle window has not yet elapsed since its last successful fetch
- **THEN** the next refresh SHALL NOT issue a new request, and the cached value SHALL remain displayed until the window elapses

#### Scenario: Tile visibility updates without restarting the app
- **WHEN** the user changes a tile's enabled state in Settings and then navigates to Home via the bottom navigation bar
- **THEN** Home SHALL reflect the new visibility without requiring the app to be restarted

### Requirement: A disabled widget shows a disabled message instead of data
Because the app cannot remove an already-placed home-screen widget instance, a disabled widget (whether already placed or newly placed after being disabled) SHALL render a message stating it has been disabled in Settings, in place of its normal data content. It SHALL NOT attempt to fetch or display stale/live production data while disabled.

#### Scenario: Existing placed widget shows disabled message
- **WHEN** a widget that is already placed on the home screen is disabled in Settings
- **THEN** its next redraw SHALL show a "disabled in Settings" message instead of production data

#### Scenario: Newly placed widget while disabled
- **WHEN** the user places a widget on the home screen while that widget type is disabled in Settings
- **THEN** the newly placed widget SHALL immediately show the "disabled in Settings" message

#### Scenario: Re-enabling a widget restores its data
- **WHEN** a disabled widget is re-enabled in Settings
- **THEN** its next redraw SHALL show its normal production data content instead of the disabled message
- **AND** if the widget's data source's throttle window had already elapsed while it was disabled, that redraw SHALL reflect a freshly fetched value rather than waiting for an additional throttle window

### Requirement: An EMA API data source is fetched only if it has an enabled consumer
Each of the three EMA API data sources (hourly energy, daily energy, module health) SHALL be fetched only when at least one enabled tile or widget consumes it. A data source with zero enabled consumers SHALL NOT be fetched, regardless of its own throttle state. Because the Today Production tile's best-day cards are computed from daily energy data, the Today Production tile counts as a consumer of daily energy in addition to hourly energy.

#### Scenario: Disabling the only consumer of a data source stops its fetches
- **WHEN** Module Health is the only enabled consumer of module-health data and the user disables it
- **THEN** subsequent Home visits and widget refresh cycles SHALL NOT issue a module-health request

#### Scenario: A data source with a remaining enabled consumer keeps fetching
- **WHEN** History Production tile is disabled but the Production Summary widget (which also uses daily energy) remains enabled
- **THEN** daily energy fetches SHALL continue on their normal throttle

#### Scenario: Today Production tile alone keeps daily energy fetching
- **WHEN** History Production tile and both daily-consuming widgets are disabled, but Today Production tile remains enabled
- **THEN** daily energy fetches SHALL continue, because Today Production's best-day cards depend on it

#### Scenario: Disabling everything stops all fetches
- **WHEN** all three tiles and all three widgets are disabled
- **THEN** no EMA API requests SHALL be issued by Home or the widget refresh worker

### Requirement: Tile/widget enabled flags are included in Settings import and export
Every tile's and widget's enabled flag SHALL be included in the Settings export JSON and SHALL be restored on import, using the same merge semantics as existing settings (fields present in the imported JSON overwrite the stored value; absent fields are left unchanged).

#### Scenario: Export includes tile/widget flags
- **WHEN** the user exports Settings
- **THEN** the exported JSON SHALL include a key for each of the 3 tile and 3 widget enabled flags

#### Scenario: Import restores tile/widget flags
- **WHEN** the user imports a Settings file that disables some tiles/widgets
- **THEN** Home and the widgets SHALL reflect the imported enabled/disabled state after the import completes

### Requirement: Factory Reset restores all tiles and widgets to enabled
Confirming Factory Reset SHALL reset every tile's and widget's enabled flag to enabled (checked), in addition to the existing settings reset to their defaults.

#### Scenario: Factory Reset re-enables everything
- **WHEN** the user confirms Factory Reset after having disabled some tiles/widgets
- **THEN** all three tile checkboxes and all three widget checkboxes SHALL be checked afterward
- **AND** all three tiles SHALL be visible on Home
