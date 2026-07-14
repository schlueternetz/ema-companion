# ADR-008: Email Alerts for Module Health

**Status:** Accepted  
**Date:** 2026-06-26

## Context

Module health status changes (GREEN → YELLOW → RED and recovery) are surfaced via push notifications. Users who are away from their device, or whose notification permissions have been revoked, may miss alerts. An out-of-band delivery channel is needed.

Requirements established before implementation:
- Alerts must reach the user even when the app is in the background and the device is silent.
- No server infrastructure should be added — the app must remain self-contained.
- The delivery mechanism must be configurable and disableable per user.
- Credentials for the delivery channel must be stored securely and never appear in logs.

## Decision

### 1. Gmail SMTP with App Password (not OAuth 2.0)

Email is sent directly from the user's own Gmail account via `smtp.gmail.com:587` (STARTTLS) using a Gmail App Password. The user creates the App Password in their Google Account settings; EMA Companion stores it in `EncryptedSharedPreferences` alongside the EMA API credentials.

OAuth 2.0 was rejected — see Alternatives Considered.

### 2. Level-gated trigger: Off / Alerts Only / All

Both delivery channels (push and email) are governed by a shared `AlertLevel` enum (`core/AlertLevel.kt`): `OFF`, `ALERTS_ONLY`, `ALL`. Each channel has its own independent setting — `SettingsRepository.notificationLevel` for push, `emailAlertLevel` for email — so a user can, for example, get a daily email digest while keeping push notifications alert-only.

A shared pure function in `ModuleHealthWorker.kt` decides whether a given channel fires for the current check:

```kotlin
fun shouldAlert(level: AlertLevel, previousStatus: ModuleHealthStatus?, newStatus: ModuleHealthStatus): Boolean =
    when (level) {
        AlertLevel.OFF -> false
        AlertLevel.ALERTS_ONLY -> newStatus != previousStatus
        AlertLevel.ALL -> true
    }
```

- **Off**: the channel never fires, regardless of status.
- **Alerts Only**: fires only when the status differs from the last status that fired on that channel — this is the original status-change-only behavior, unchanged, and covers both degradation and recovery back to GREEN.
- **All**: fires on every 24-hour background check regardless of whether the status changed, so a user gets a daily confirmation that the check is still running even during an extended all-GREEN streak.

For email, `lastEmailedStatus` (persisted in `ema_module_health`) is still updated on every dispatched email — including under `ALL` — so switching back to `ALERTS_ONLY` doesn't immediately re-fire for a status that hasn't actually changed. It is updated only on `EmailResult.Success`; on `AuthFailure` or `NetworkError` it is left unchanged so the next eligible check retries delivery. Push notifications follow the same pattern with `lastNotifiedStatus`.

Under `ALL`, `ModuleHealthNotifier.notify()` also posts a new GREEN "all clear" push notification (via a `postOnGreen: Boolean` parameter) — previously GREEN always silently cancelled any existing notification with nothing posted in its place. Email's existing GREEN subject/body content is reused as-is for the daily confirmation; no new email content was needed.

**Migration**: the two boolean preferences this replaced (`notificationsEnabled`, `emailAlertsEnabled`) are migrated lazily the first time each new level is read after upgrading — `true` becomes `ALERTS_ONLY`, `false` becomes `OFF` — so existing users' settings carry forward instead of silently resetting to the new defaults (`ALERTS_ONLY` for push, `OFF` for email).

### 3. Separate persisted fields for each delivery channel

Push notifications track `lastNotifiedStatus`; email alerts track `lastEmailedStatus`. These are independent fields in `ema_module_health` — they are not merged into a single "last alerted" field.

This keeps the two channels decoupled: a push notification failure does not suppress the next email, and vice versa. Both fields are cleared by `ModuleHealthRepository.resetThrottle()` (called on EMA credential change, import, or factory reset) so the next check sends a fresh alert if needed.

### 4. RED latch: YELLOW does not clear RED

When the persisted status is RED and the freshly computed status is YELLOW, the final stored status remains RED. Only a GREEN result clears RED:

```kotlin
val finalStatus = if (previousStatus == RED && computed == YELLOW) RED else computed
```

A module oscillating between offline and zero-production days should not silently downgrade the alert. The user receives a recovery email only when all modules are producing.

### 5. Settings UI

Email Alerts are configured in a dedicated card on the Settings screen, below API Settings. The card contains:
- A value row (`settings_email_alert_level_row`) showing the current level, matching the existing Language/Display Mode tap-to-open-dialog pattern rather than a switch — selecting Off/Alerts Only/All persists immediately. Selecting a non-Off level with no credentials saved reveals an inline setup form; selecting Off keeps the status row visible if credentials are already saved, so the user can switch back on without re-entering the App Password.
- The setup form: instruction text, **Open Google Account ↗** button (fires `ACTION_VIEW` to `myaccount.google.com/apppasswords`), Gmail address field, App Password field, and a **Verify & Save** button that calls `GmailSmtpEmailSender.testConnection()` before persisting credentials. Saving promotes the level to Alerts Only if it was Off (matching the old "save always re-enables" behavior); an existing All or Alerts Only selection is left as-is.
- When credentials are saved: a status row showing "Email alerts enabled for: address" — tapping it shows a disable confirmation dialog that removes credentials, sets the level to Off, and clears `lastEmailedStatus`.

The push notification level uses the equivalent row (`settings_notification_level_row`) directly under App Settings.

## Alternatives Considered

**OAuth 2.0 for Gmail**: requires registering a Google Cloud project, managing a client ID and secret, handling token refresh, and running a redirect URI callback — all adding server infrastructure or a significant in-app auth flow. Rejected: excessive complexity for a personal solar monitor. App Passwords are supported for accounts with 2-Step Verification and do not require any server-side component.

**A generic SMTP provider (any server)**: would accommodate non-Gmail accounts but requires the user to know their SMTP host, port, and TLS settings. Rejected: the extra configuration burden outweighs the flexibility gain given that Gmail is the common case. Can be revisited by making `smtpHost`, `smtpPort`, and `useTls` user-configurable.

**Shared `lastAlertedStatus` across push and email**: a single field that both channels read and write. Rejected: a push failure would block the next email and vice versa, creating silent gaps. Separate fields let each channel retry independently.

**Sending on every 24-hour check (not just on change)**: would guarantee delivery even if a prior check failed, but would flood the inbox during extended outages. Rejected: status-change-only is the right default; a `lastEmailedStatus` update failure already causes a retry on the next change.

## Consequences

- `ModuleHealthRepository` exposes `getLastEmailedStatus()`, `setLastEmailedStatus()`, and clears `KEY_LAST_EMAILED_STATUS` in `resetThrottle()`.
- `ModuleHealthWorker.doWork()` evaluates `shouldAlert()` independently for push (`notificationLevel`) and email (`emailAlertLevel`), each against its own previous-status field, and sends email independently of the push notification result.
- `GmailSmtpEmailSender` takes injectable `smtpHost`, `smtpPort`, and `useTls` parameters so unit tests can point at a GreenMail in-process server.
- App Password is stored in `EncryptedSharedPreferences` and must never be logged, included in crash reports, or passed to `ApiCallLogRepository`.
- `SettingsFragmentTest.setUp()` must clear `ema_companion_settings` (which holds email credentials) — already satisfied by the existing full clear of that store.
- Disabling email alerts (selecting Off with a saved account, or clearing credentials) removes credentials when cleared and clears `lastEmailedStatus`; the next Verify & Save re-runs the setup flow.
- `notificationLevel` and `emailAlertLevel` (both `AlertLevel`, persisted as their `name()`) replace the old `notificationsEnabled`/`emailAlertsEnabled` booleans; `SettingsRepository` migrates the legacy boolean lazily at read time so existing installs keep their prior effective behavior after upgrading. Email address/App Password/level remain outside `exportToJson()`/`importFromJson()`, matching the pre-existing (not new) exclusion of email settings from export.
