# Home

The Home screen is a dashboard of tiles. Each tile fetches live data when you open the app or return to this screen.

## Current Production tile

Shows the latest power reading from your solar array (e.g. "8000 W") and the time it was last updated. A fresh value is fetched when you open the app and whenever you return to Home. Successful readings are limited to once every 10 minutes; between refreshes, the last retrieved value stays on the tile. Until the first successful reading, a neutral placeholder ("— W") is shown.

If a refresh fails, the tile shows a short status line beneath the value and keeps the last known value:

- **Network issue — couldn't update** — the app could not reach the EMA service (e.g. no connection)
- **Authentication failed — check your API credentials** — your credentials were rejected; check them in Settings
- **Couldn't update production data** — another error, such as an invalid System/ECU ID or a server problem

The status clears automatically when a later fetch succeeds. A failed fetch does **not** count against your monthly request limit and is retried the next time you open or return to Home. Fixing your credentials or connection settings in Settings triggers an immediate retry on return to Home.

## Module Health tile

Shows whether your individual solar modules have been producing over the last three days. The tile follows the same layout as the Current Production tile: a title line, a content line showing a status icon alongside the status text, and a footer line showing when the last check ran.

| Status | Icon | Meaning |
|---|---|---|
| **All modules producing** (green) | ✓ checkmark | Every module reported energy on each of the past three days |
| **Module offline** (yellow) | ⚠ warning | One or more modules had zero production for 1–2 consecutive days |
| **Module offline — action needed** (red) | ⚠ warning | One or more modules had zero production for 3 consecutive days |
| **Checking…** (gray) | ✓ checkmark | The first background check has not run yet |

A "Checked [date] at [time]" line appears below the status once a check has completed.

If a check fails, the tile shows a short status line beneath the checked timestamp:

- **Network issue — couldn't check** — the app could not reach the EMA service
- **Authentication failed — check your API credentials** — your credentials were rejected; check them in Settings
- **Couldn't check module status** — another error, such as an invalid ECU ID or a server problem

**Viewing details:** Tap the tile when it shows yellow or red to open an **Offline Modules** dialog. The dialog lists each affected module by its ID and how many consecutive days it has had no production. The tile is not tappable when all modules are green or the status is not yet known.

**How the check works:** A background job runs automatically once per day at 8 pm in your array's timezone (configured in Settings). It retrieves the last three days of per-module energy from the EMA API and classifies each module individually. Yesterday's and the day-before's data are cached; only today's data is re-fetched. The tile shows the result of the most recent completed check immediately when you open Home; the background job refreshes it silently at 8 pm without requiring the app to be open.

**Notifications:** When the Module Health check finds a yellow or red status, EMA Companion sends a push notification so you know even when the app is closed. The notification is replaced (not stacked) on each daily check until the status returns to green. On Android 13 and later, the app asks for notification permission the first time it starts — granting it is recommended so you receive these alerts.
