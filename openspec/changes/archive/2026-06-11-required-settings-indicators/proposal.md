## Why

Required settings fields (EMA App ID, App Secret, System ID, ECU ID, System Capacity) show no visual indication that they are mandatory when empty, so users have no way to tell what they still need to fill out before the app becomes functional. The navigation lock already enforces this contract — this change makes it visible.

## What Changes

- Required fields that are still empty display a "Required" indicator (hint text on the value label or the input field) while the app is in the unconfigured state
- Once a required field is saved with a valid value, its "Required" indicator clears
- Once all required fields are filled, the navigation unlocks as before (no change to existing unlock behaviour)

## Capabilities

### New Capabilities
<!-- none -->

### Modified Capabilities
- `unconfigured-app-state`: Add requirement that required-but-empty fields are visually marked as required while the app is unconfigured

## Impact

- `SettingRowView` — new optional `isRequired` property; drives a "Required" hint when value is empty
- `SettingsFragment` — marks the five required fields as required; clears the indicator on successful save
- `unconfigured-app-state` spec — new scenario covering the visual indicator
