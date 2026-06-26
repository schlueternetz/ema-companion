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

### 2. Status-change-only trigger

An email is sent **only when the module health status changes**, not on every 24-hour background check. The last status that triggered an email is persisted as `lastEmailedStatus` in `ema_module_health` SharedPreferences. A new email is sent when:

```
newStatus != UNKNOWN && newStatus != lastEmailedStatus
```

`lastEmailedStatus` is updated only on `EmailResult.Success`. On `AuthFailure` or `NetworkError` it is left unchanged so the next status change retries delivery.

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
- A toggle (`MaterialSwitch`). When turned on with no credentials saved, an inline setup form appears.
- The setup form: instruction text, **Open Google Account ↗** button (fires `ACTION_VIEW` to `myaccount.google.com/apppasswords`), Gmail address field, App Password field, and a **Verify & Save** button that calls `GmailSmtpEmailSender.testConnection()` before persisting credentials.
- When credentials are saved: a status row showing "Email alerts enabled for: address" — tapping it shows a disable confirmation dialog that removes credentials and clears `lastEmailedStatus`.

## Alternatives Considered

**OAuth 2.0 for Gmail**: requires registering a Google Cloud project, managing a client ID and secret, handling token refresh, and running a redirect URI callback — all adding server infrastructure or a significant in-app auth flow. Rejected: excessive complexity for a personal solar monitor. App Passwords are supported for accounts with 2-Step Verification and do not require any server-side component.

**A generic SMTP provider (any server)**: would accommodate non-Gmail accounts but requires the user to know their SMTP host, port, and TLS settings. Rejected: the extra configuration burden outweighs the flexibility gain given that Gmail is the common case. Can be revisited by making `smtpHost`, `smtpPort`, and `useTls` user-configurable.

**Shared `lastAlertedStatus` across push and email**: a single field that both channels read and write. Rejected: a push failure would block the next email and vice versa, creating silent gaps. Separate fields let each channel retry independently.

**Sending on every 24-hour check (not just on change)**: would guarantee delivery even if a prior check failed, but would flood the inbox during extended outages. Rejected: status-change-only is the right default; a `lastEmailedStatus` update failure already causes a retry on the next change.

## Consequences

- `ModuleHealthRepository` exposes `getLastEmailedStatus()`, `setLastEmailedStatus()`, and clears `KEY_LAST_EMAILED_STATUS` in `resetThrottle()`.
- `ModuleHealthWorker.doWork()` sends email after computing status, independently of push notification result.
- `GmailSmtpEmailSender` takes injectable `smtpHost`, `smtpPort`, and `useTls` parameters so unit tests can point at a GreenMail in-process server.
- App Password is stored in `EncryptedSharedPreferences` and must never be logged, included in crash reports, or passed to `ApiCallLogRepository`.
- `SettingsFragmentTest.setUp()` must clear `ema_companion_settings` (which holds email credentials) — already satisfied by the existing full clear of that store.
- Disabling email alerts removes credentials and clears `lastEmailedStatus`; the next enable re-runs the Verify & Save flow.
