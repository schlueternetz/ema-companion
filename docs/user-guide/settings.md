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
| **Language** | Tap to choose: System (device default), English, or German. Takes effect immediately. |
| **Display Mode** | Tap to choose: System (follows OS dark/light mode), Light, or Dark. Takes effect immediately. |
| **Notifications Enabled** | Toggle; on by default. Takes effect immediately — no Save step required. |
| **Historic Data Days** | Days of production history to retain (1–90); defaults to 30. |

## API Settings

| Control | Description |
|---|---|
| **API Request Limit** | Maximum EMA API calls permitted per month (1–2,678,400; default 1,000). A progress bar below the field shows how many of this month's **successful** reads have been used. The count resets automatically at the start of each calendar month. Tap ↺ to restore the default. |
| **Base URL** | The API endpoint (default `https://api.apsystemsema.com:9282/user/api/v2/`). Must be a valid URL up to 2,048 characters. Tap ↺ to restore the default. |

## Email Alerts

Sends an email when your module health status changes (GREEN → YELLOW or RED, and back to GREEN). Emails are sent from your own Gmail account using an App Password — no third-party relay involved.

**Requirement:** A Gmail account with 2-Step Verification enabled.

### Enabling Email Alerts

1. Toggle **Email Alerts** on. A setup form appears below the toggle.
2. Tap **Open Google Account ↗** to open `myaccount.google.com/apppasswords` in your browser.
3. In your Google Account, go to **Security → App Passwords** and create one for "Mail" on "Other device". Copy the 16-character password shown.
4. Back in EMA Companion, enter your Gmail address and paste the App Password.
5. Tap **Verify & Save**. The app connects to Gmail to confirm the credentials. On success the setup form is replaced by a status line showing the address in use. If the connection fails, an error message appears — check that the App Password was copied correctly.

### Disabling Email Alerts

Tap the **"Email alerts enabled for: …"** row. A confirmation dialog appears — tap **Disable** to remove the credentials and stop sending emails.

### When emails are sent

An email is sent once per status change, not on every background check. If a module has been offline for two days (YELLOW) and a third day passes with no production (RED), a second email is sent. When all modules recover, a recovery email is sent. Changing your EMA credentials resets the email history so the next check sends a fresh alert if needed.

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
