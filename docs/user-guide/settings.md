[User Guide](user-guide.md) › Settings

# Settings

The Settings screen is scrollable and organised into sections.

## Solar Array Settings

These fields connect EMA Companion to your APsystems account and solar array. All five credential fields are required — any field that has not yet been filled in shows **Required** as a hint.

All fields use an inline edit pattern: tap the **Edit** (pencil) icon next to a field, enter a value, then tap **Save** (check) or press Enter. Tap **Cancel** to discard. Only one field can be in edit mode at a time — opening a second field automatically closes the first.

Each field has an **Info** (ⓘ) button. Tap it to see where to find that value in the EMA app. The info button is hidden while a field is in edit mode.

| Field | Format | Notes |
|---|---|---|
| **EMA App ID** | 32 alphanumeric characters | Required; stored in lowercase |
| **EMA App Secret** | 12 alphanumeric characters | Required; displayed masked (only last 4 characters visible); field clears when you enter edit mode |
| **EMA System ID** | 16 alphanumeric characters | Required; stored in uppercase |
| **EMA ECU ID** | 12 digits | Required; numeric keyboard |
| **System Capacity** | Positive number up to 2,000 | Required; displayed with "kW" suffix; up to 2 decimal places |
| **Array Timezone** | IANA timezone ID | Tap to open a searchable list; defaults to your device timezone |

If you enter a value that doesn't meet the format rules, an error message appears below the field and Save does nothing until the input is corrected.

Changing the **Array Timezone** immediately reschedules the background Module Health check to fire at 8 pm in the newly selected timezone.

### Where to find these values in the EMA app

> **Prerequisite:** The App ID and App Secret only appear after OpenAPI access has been enabled. Go to **Settings → OpenAPI Service** in the EMA app and enable it first.

| Field | Where to find it |
|---|---|
| **EMA App ID** | **Settings → OpenAPI Service → Developer Authorization** — the APP ID |
| **EMA App Secret** | **Settings → OpenAPI Service → Developer Authorization** — the APP Secret |
| **EMA System ID** | **Settings → Account Details** — the `sid` value |
| **EMA ECU ID** | **Settings → ECU** — the ECU ID value |
| **System Capacity** | **Home screen** — the Capacity value |

> **Important:** If the EMA API is unused for 6 consecutive months, APsystems may revoke access automatically. If EMA Companion stops retrieving data, open the EMA app and verify that OpenAPI access is still enabled under **Settings → OpenAPI Service**.

## App Settings

| Setting | Details |
|---|---|
| **Language** | Tap to choose: System (device default), English, or German. Each language name is always shown in that language (e.g. "Deutsch" for German) so it's recognizable even if the app is currently set to a language you don't read. Takes effect immediately. |
| **Display Mode** | Tap to choose: System (follows OS dark/light mode), Light, or Dark. Takes effect immediately. |
| **Notifications** | Tap to choose: Off, Alerts Only, or All. Alerts Only by default. Takes effect immediately — see "When alerts are sent" under Email Alerts below. |
| **Historic Data Days** | Days of production history to retain (1–90); defaults to 30. |

## Tiles & Widgets

Choose which Home tiles and home-screen widgets are active. All seven are checked (enabled) by default.

- **Tiles:** Today Production, History Production, Module Health. Unchecking a tile removes its card from Home the next time you view that screen.
- **Widgets:** Today Production, Production Summary, Production History. Android has no way for an app to remove a widget you've already placed on your home screen, so unchecking a widget instead makes it show **"This widget has been disabled in Settings"** in place of its data — both for widgets already placed and any placed after being disabled.

A **Select All / Deselect All** button at the top of the section toggles every checkbox at once: it reads "Deselect All" when everything is checked, and "Select All" as soon as anything is unchecked.

Data is only fetched from the EMA API for a tile or widget that is currently enabled — disabling everything that depends on a given data type (e.g. all daily-energy consumers) stops those API calls entirely.

These settings are included in Import/Export and reset to all-enabled on Factory Reset, like the rest of your settings.

## API Settings

| Control | Description |
|---|---|
| **API Request Limit** | Maximum EMA API calls permitted per month (1–2,678,400; default 1,000). A progress bar below the field shows how many of this month's **successful** reads have been used. The count resets automatically at the start of each calendar month. Tap ↺ to restore the default. |
| **Base URL** | The API endpoint (default `https://api.apsystemsema.com:9282/user/api/v2/`). Must be a valid URL up to 2,048 characters. Tap ↺ to restore the default. |

> **Developer builds only:** a **Use local stub** button appears below Base URL in debug builds, pointing the app at a local test server instead of the real EMA API. It is not present in the released app.

## Email Alerts

Sends an email about your module health status. Emails are sent from your own Gmail account using an App Password — no third-party relay involved. Like Notifications, the **Email Alerts** row is tap-to-choose (Off, Alerts Only, or All) — see "When alerts are sent" below.

**Requirement:** A Gmail account with 2-Step Verification enabled.

### Setting up Email Alerts

1. Tap the **Email Alerts** row and choose **Alerts Only** or **All**. A setup form appears below.
2. Tap **Open Google Account ↗** to open `myaccount.google.com/apppasswords` in your browser.
3. In your Google Account, go to **Security → App Passwords** and create one for "Mail" on "Other device". Copy the 16-character password shown.
4. Back in EMA Companion, enter your Gmail address and paste the App Password. Spaces in the App Password are stripped automatically.
5. Tap **Save**. The app validates the format (a valid email address and a 16-character App Password) and saves immediately — no network connection is required. If the format is invalid, an error message appears below the button.

### Managing Email Alerts

Once credentials are saved, the management section shows your configured email address and an **Edit credentials** button. Tap it to re-open the form with your email pre-filled and a blank password field.

In edit mode, two additional actions appear below the Save button:

| Action | What it does |
|---|---|
| **Send test email** | Sends a real test message to your configured address to verify the credentials work end-to-end. The result (success or error) appears inline. |
| **Clear credentials** | Shows a confirmation dialog. Tap **Clear** to remove your saved Gmail credentials and stop all alert emails. |

Tap **Cancel** to leave edit mode without saving.

Choosing **Off** pauses alerts but keeps the management section visible (if credentials are saved) so you can switch back on without re-entering the App Password. Saving credentials from the setup form always turns alerts on (Alerts Only) if they were Off.

### When alerts are sent

Notifications and Email Alerts each have their own independent level, so you can, for example, get a daily email digest while keeping push notifications set to Alerts Only:

| Level | Behavior |
|---|---|
| **Off** | That channel never sends. |
| **Alerts Only** | Sends only when your module health status actually changes — a degradation (GREEN → YELLOW → RED) or a recovery back to GREEN. This is the original behavior. |
| **All** | Sends once a day on every background check, even if the status is unchanged and still GREEN — a daily confirmation that checks are still running, so a silent failure doesn't go unnoticed. |

Changing your EMA credentials resets the alert history so the next check sends a fresh alert if needed.

## Logs

Records EMA API activity, newest first. Each row shows the time, endpoint, duration (milliseconds), and success or failure. Importing settings also generates a log entry listing which fields were imported — no sensitive values are shown. If no entries exist yet, the section shows "No API calls recorded yet".

The log list is independently scrollable within its own area. Tap any entry to open a detail dialog showing the full request and response. The log keeps the 100 most recent calls; older entries are dropped automatically.

## Configuration

| Control | Description |
|---|---|
| **Import Settings** | Opens the system file picker to select a JSON settings file. Plain JSON is merged immediately; encrypted files prompt for a 4-digit PIN. |
| **Export Settings** | Saves all settings to `ema-companion-settings.json` in a location you choose. Choose no encryption or a 4-digit PIN. |
| **Factory Reset** | Permanently deletes all settings, the API request count, call logs, and module health history. A confirmation dialog appears before anything is deleted. |

For step-by-step import and export instructions, see [Import and Export](import-export.md).
