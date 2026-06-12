# EMA Companion User Guide

## Overview

EMA Companion is an Android app for APsystems solar array owners. It is designed to work alongside the existing EMA app — not replace it — and will add features such as detailed production statistics and graphs, home screen widgets, and alert notifications.

![EMA Companion system context: the owner uses both the EMA app and EMA Companion on their Android device; both retrieve data from the EMA API, which in turn reads from the solar array.](system-context.png)

## Getting Started

Install EMA Companion on your Android phone or tablet (Android 12 or later required). Open it from your home screen or app drawer by tapping the **EMA Companion** icon.

**First launch:** The app opens directly to the **Settings** screen and the Home navigation item is disabled. You must enter your EMA API credentials and system capacity before the rest of the app becomes accessible.

Once the required fields are saved, the bottom navigation bar unlocks and you can freely switch between screens.

## Navigation

A bottom navigation bar runs across the bottom of every screen with three destinations:

- **Home** — the main dashboard (currently a placeholder)
- **User Guide** — this guide, viewable inside the app
- **Settings** — app preferences and configuration (always the rightmost item)

When the app is not fully configured, only **Settings** and **User Guide** are reachable. The **Home** item (and any future screens) are re-enabled automatically once configuration is complete.

## Screens

### Home

> **Early development:** The Home screen is currently a placeholder displaying "Hello World!" while production features are being built.

Once development is complete, this screen will be the central dashboard for your solar production data.

### User Guide

The **User Guide** screen shows this guide inside the app, so help is always at hand — even before you have configured your credentials. It is available from the moment you first open the app.

The guide is formatted text with headings, tables, and images, and you can scroll through it. Where a page links to another section, tap the link to open that page; use your device's **Back** gesture or button to return to the previous page. Links to external websites open in your browser. The guide is available in English.

### Settings

The Settings screen is scrollable and organised into three sections.

#### Solar Array Settings

These fields connect EMA Companion to your APsystems account and solar array. All five fields are required — any field that has not yet been filled in shows **Required** as a hint. All fields use an inline edit pattern: tap the **Edit** (pencil) icon next to a field, enter a value, then tap the **Save** (check) icon or press Enter on the keyboard. Tap **Cancel** to discard the change. Only one field can be in edit mode at a time — opening a second field automatically closes the first.

Each field has an **Info** (ⓘ) button. Tap it to see a description of exactly where to find that value in the EMA app. The info button is hidden while a field is in edit mode.

| Field | Format | Notes |
|---|---|---|
| **EMA App ID** | 32 alphanumeric characters | Required; stored in lowercase |
| **EMA App Secret** | 12 alphanumeric characters | Required; displayed masked (only last 4 characters visible); field clears when you enter edit mode |
| **EMA System ID** | 16 alphanumeric characters | Required; stored in uppercase |
| **EMA ECU ID** | 12 digits | Required; numeric keyboard |
| **System Capacity** | Positive number up to 2,000 | Required; displayed with a "kW" suffix outside the input; decimal keyboard; up to 2 decimal places |

If you enter a value that doesn't meet the format rules, an error message appears below the field and the Save button does nothing until the input is corrected.

##### Where to find these values in the EMA app

> **Prerequisite:** The App ID and App Secret only appear in the EMA app after OpenAPI access has been enabled. Go to **Settings → OpenAPI Service** in the EMA app and enable it before looking for the Developer Authorization settings.

| Field | Where to find it |
|---|---|
| **EMA App ID** | **Settings → OpenAPI Service → Developer Authorization** — the APP ID shown there |
| **EMA App Secret** | **Settings → OpenAPI Service → Developer Authorization** — the APP Secret shown there |
| **EMA System ID** | **Settings → Account Details** — the `sid` value |
| **EMA ECU ID** | **Settings → ECU** — the ECU ID value |
| **System Capacity** | **Home screen** — the Capacity value displayed there |

> **Important:** If the EMA API is not used for 6 consecutive months, APsystems may automatically revoke API access. If EMA Companion stops retrieving data, open the EMA app and verify that OpenAPI access is still enabled under **Settings → OpenAPI Service**.

#### App Settings

| Setting | Type | Details |
|---|---|---|
| **Language** | Dialog | Tap to choose: System (device default), English, or German. Takes effect immediately. |
| **Display Mode** | Dialog | Tap to choose: System (follows OS dark/light mode), Light, or Dark. Takes effect immediately and is applied on every app start. |
| **Notifications Enabled** | Toggle | Enables or disables app notifications. On by default. Takes effect immediately — no Save step required. |
| **Historic Data Days** | Editable row | Number of days of production history to retain (1–90). Defaults to 30. Displayed with a "days" suffix; numeric keyboard. |

#### API Settings

| Control | Description |
|---|---|
| **API Request Limit** | Maximum number of EMA API calls permitted per month (1–2,678,400). Defaults to 1,000. Displayed with a "req/month" suffix outside the input; numeric keyboard. Tap the reset icon (↺) to restore the default. The reset icon is disabled while the field is in edit mode. A progress bar below the field shows how many of this month's requests have been consumed, with a label showing the exact count (e.g. "800 / 1000 requests this month"). |
| **Base URL** | The API endpoint used to reach the EMA service. Defaults to `https://api.apsystemsema.com:9282/user/api/v2/`. Must be a valid URL up to 2,048 characters. Tap the reset icon (↺) beside the field to restore the default without typing. |

#### Configuration

These actions apply to **all** settings on the page, not just the API settings.

| Control | Description |
|---|---|
| **Import Settings** | Opens the system file picker to select a JSON settings file. If the file is plain JSON all recognised fields are merged into the current settings. If the file is encrypted you are prompted for the 4-digit PIN that was set during export. |
| **Export Settings** | Saves all settings to a file named `ema-companion-settings.json` in a location you choose. A dialog first asks whether to export without encryption or to encrypt with a 4-digit PIN. |
| **Factory Reset** | Permanently deletes all settings and any locally stored data. A confirmation dialog appears before anything is deleted. Tap **Reset** to confirm or **Cancel** to abort. |

## Import and Export

Settings can be transferred between devices or backed up using the Import and Export buttons in the **Configuration** section.

**Exporting:**
1. Tap **Export Settings**.
2. Choose **No encryption** for a plain JSON file, or **Encrypt with PIN** and enter a 4-digit PIN.
3. The system file picker opens — choose a folder and confirm. The file is saved as `ema-companion-settings.json`.

**Importing:**
1. Tap **Import Settings** and select a previously exported file.
2. If the file is plain JSON, settings are merged immediately.
3. If the file is encrypted, you are prompted for the PIN. Entering the wrong PIN shows an error and leaves all settings unchanged.
4. Only the fields present in the file are updated; any fields not in the file keep their current values.

## What's Coming

Features planned for future releases:

- Production statistics and graphs on the Home screen
- Home screen widgets showing live output
- Notifications for solar array alerts
