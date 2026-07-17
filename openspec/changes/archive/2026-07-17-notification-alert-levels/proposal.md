## Why

Push notifications and email alerts currently only fire on a module health status change, and neither can be silenced independently in a granular way — email alerts are effectively boolean (configured or not), and the "Notifications Enabled" toggle is persisted but never actually checked by `ModuleHealthWorker`, so push notifications cannot currently be turned off at all. Some users want a daily "still watching, still green" confirmation so a silent failure of the health check itself doesn't go unnoticed; others want to be alerted only when something actually degrades. A single on/off switch can't express both.

## What Changes

- Replace the `notificationsEnabled` boolean setting with a 3-level `NotificationLevel` (Off / Alerts Only / All), selected via a tap-to-open dialog (same UX pattern as Language/Display Mode), not a switch.
- Replace the `emailAlertsEnabled` boolean setting with a 3-level `EmailAlertLevel` (Off / Alerts Only / All), selected the same way. The existing Gmail App Password setup/verify flow is unchanged — it now triggers the first time the level is moved off "Off" without saved credentials.
- **BREAKING**: `SettingsRepository.getNotificationsEnabled()`/`setNotificationsEnabled()` and `getEmailAlertsEnabled()`/`setEmailAlertsEnabled()` are removed in favor of level-based getters/setters. Existing exported settings JSON with the old boolean keys is migrated on import (`true` → Alerts Only, `false` → Off) rather than rejected.
- `ModuleHealthWorker.doWork()` gains real gating for push notifications (previously the enabled flag was persisted but silently ignored):
  - **Off**: never notify / never email.
  - **Alerts Only**: fire only on a status change (`newStatus != lastStatus`), same trigger as today — covering both degradation and recovery back to GREEN.
  - **All**: fire on every check regardless of whether status changed, once per day per the existing 24h worker cadence.
- `ModuleHealthNotifier` gains a GREEN push notification (new channel copy) for the "All" level's daily check — today it silently cancels on GREEN with no notification posted at all.
- Email content for the daily GREEN "All" case reuses the existing green subject/body (no content change needed for email).

## Capabilities

### New Capabilities
- `email-alert-preference`: user-configurable 3-level email alert setting (Off / Alerts Only / All) governing when module health emails are sent. This is the first OpenSpec spec for the email alerts feature (previously implemented per ADR-008 without a spec).

### Modified Capabilities
- `notification-preference`: the existing boolean "Notifications Enabled" toggle requirement is replaced with a 3-level selection requirement, and the level now actually gates whether `ModuleHealthWorker` posts a push notification.

## Impact

- `SettingsRepository`: new `NotificationLevel`/`EmailAlertLevel` enums (likely in `core/`, mirroring `HomeTile`/`HomeWidget`), new persisted keys, removal of the two boolean keys, import/export + migration logic.
- `SettingsFragment` + `fragment_settings.xml`: replace `settings_notifications_switch` and `settings_email_alerts_switch` with value rows + selection dialogs (pattern reused from `showLanguageDialog`/`showDisplayModeDialog`). Email setup/verify/clear sub-flow logic is retargeted to key off the new level instead of a boolean.
- `ModuleHealthWorker`: notification and email trigger conditions become level-aware instead of unconditional-on-change (email) / entirely unwired (push).
- `ModuleHealthNotifier`: new GREEN notification path + new string resources (title/text, EN + DE).
- Tests: `SettingsRepositoryTest`, `SettingsFragmentTest`, `ModuleHealthWorkerTest`, `ModuleHealthNotifierTest` all reference the old boolean API and need updating.
- `docs/adr/008-email-alerts.md`: the "status-change-only trigger" decision is superseded by the level-based trigger; ADR needs an update via `write-adr`.
- `docs/user-guide/settings.md`: describes the current toggles; needs an update via `write-user-guide`.
