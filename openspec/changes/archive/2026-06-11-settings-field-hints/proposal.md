## Why

New users need to look up several cryptic values (App ID, App Secret, System ID, ECU ID, System Capacity) from specific locations in the EMA app before they can configure EMA Companion — there is currently no in-app guidance on where to find them.

## What Changes

- Each Solar Array Settings field gains an optional **info icon** that opens a dialog explaining exactly where in the EMA app the user can find that field's value.
- The user guide is updated to document the same navigation paths for each field, plus two important EMA app notes:
  - **OpenAPI prerequisite**: The App ID and App Secret settings only appear in the EMA app after OpenAPI access has been explicitly enabled (Settings → OpenAPI Service).
  - **6-month inactivity warning**: If the EMA API is not called for 6 consecutive months, APsystems may revoke API access. If the Companion app stops working, verify in the EMA app that OpenAPI access is still enabled.

## Field Hint Content

The following hint text should be shown for each Solar Array Settings field:

| Field | Where to find it in the EMA app |
|---|---|
| **EMA App ID** | The APP ID generated in **Settings → OpenAPI Service → Developer Authorization** |
| **EMA App Secret** | The APP Secret generated in **Settings → OpenAPI Service → Developer Authorization** |
| **EMA System ID** | The `sid` value found in **Settings → Account Details** |
| **EMA ECU ID** | The ECU ID value found in **Settings → ECU** |
| **System Capacity** | The Capacity value shown on the **Home** screen |

## Capabilities

### New Capabilities

- `settings-field-hints`: An info/help button on each Solar Array Settings field that, when tapped, shows a dialog describing where in the EMA app to find that field's value.

### Modified Capabilities

*(none — existing field validation and save behaviour is unchanged)*

## Impact

- `feature/settings/SettingRowView.kt` — add optional hint text support and info icon button
- `feature/settings/SettingsFragment.kt` — wire hint strings to each field
- `res/layout/view_setting_row.xml` — add info icon `ImageButton` to the row layout
- `res/values/strings.xml` / `res/values-de/strings.xml` — add hint strings for each field (EN + DE)
- `docs/user-guide/user-guide.md` — add "Where to find these values" guidance under Solar Array Settings
