## Context

Push notifications and email alerts are both driven by `ModuleHealthWorker.doWork()`, which runs once daily (ADR-010's dedicated alerting-class `PeriodicWorkRequest`, never gated by tile/widget state). Today:
- Email is boolean-gated: `settings.isEmailConfigured() && settings.getEmailAlertsEnabled()`, and fires only when `newStatus != previousEmailedStatus` (ADR-008, "status-change-only trigger").
- Push is **not gated by any setting at all** — `getNotificationsEnabled()`/`setNotificationsEnabled()` exist, are wired to a UI switch, are persisted, exported, and imported, but `ModuleHealthWorker` never reads the value. Push fires whenever `newStatus != previousNotifiedStatus`.
- `ModuleHealthNotifier.notify()` only ever posts for YELLOW/RED; GREEN/UNKNOWN silently cancels — there is no "all clear" notification today.

Two clarifying decisions were made before this design (see proposal):
1. "Alerts Only" fires on **any** status change, degradation or recovery — it is the existing change-trigger behavior, just gated by a named tier instead of always-on.
2. "All" is symmetric across both channels: push gets a new daily GREEN "all clear" notification, matching the email channel's existing GREEN copy.

## Goals / Non-Goals

**Goals:**
- Independent 3-level control (Off / Alerts Only / All) for push and for email.
- Push notifications become actually controllable — closes the existing gap where the setting was persisted but ignored.
- No loss of existing email alert configuration for users who already set it up (migration, not reset-to-default).

**Non-Goals:**
- No change to how module health *status* itself is computed (3-day window, RED latch) — only to whether/when an alert is dispatched for a given status.
- No change to the Gmail App Password setup/verify/clear flow's mechanics — only to what triggers it being shown.
- No per-status-tier customization (e.g. "email me for RED only, push me for RED+YELLOW") — each channel gets one level covering all tiers uniformly.

## Decisions

### 1. One shared `AlertLevel` enum, two independent settings
`AlertLevel { OFF, ALERTS_ONLY, ALL }` in `core/` (alongside `HomeTile`/`HomeWidget` — used by both `feature/settings` and `feature/home`, satisfying ADR-004's "core only when used by ≥2 features"). `SettingsRepository` stores two independent values, `notificationLevel` and `emailAlertLevel`, both persisted as the enum's `name()` string (same pattern as `ModuleHealthStatus`/`FetchError`). A single enum avoids duplicating three near-identical states twice; two separate keys keep the channels decoupled exactly as ADR-008 §3 already establishes for `lastNotifiedStatus`/`lastEmailedStatus`.

Alternative considered: two distinct enums (`NotificationLevel`, `EmailAlertLevel`) for type safety against passing one setting where the other belongs. Rejected — both call sites (`ModuleHealthWorker`) are adjacent and the risk of mixing them up is low; the shared enum removes real duplication (three options, three string-resource sets would still be separate either way).

### 2. On-device migration is lazy, at read time
`SettingsRepository.getNotificationLevel()`/`getEmailAlertLevel()`: if the new key is absent but the legacy boolean key (`notificationsEnabled` / `emailAlertsEnabled`) is present, translate (`true → ALERTS_ONLY`, `false → OFF`), persist under the new key, and leave the legacy key in place (harmless dead data, avoids a write-on-every-read race). This runs once per install upgrade path — after the first read, the new key wins on all subsequent reads.

This matters most for email: a user who already completed Verify & Save has `emailAlertsEnabled=true` today. Without migration, upgrading would silently reset them to the new default (`OFF`) and stop sending emails with no user-visible signal — a real regression the earlier boolean toggle model doesn't have. Push has the same treatment for consistency even though, since push was never actually gated before, the practical behavior change there is "push becomes governed by the level" rather than "push resumes."

`importFromJson()` gets the equivalent one-directional acceptance: if the JSON has the legacy boolean key and not the new level key, translate on import. `exportToJson()` only ever writes the new level key going forward.

### 3. UI: reuse the Language/Display Mode picker pattern, not a switch
Both `settings_notifications_switch` and `settings_email_alerts_switch` (`MaterialSwitch`) are replaced with a label + current-value row that opens an `AlertDialog.Builder().setItems(...)` 3-option picker on tap — the exact pattern `showLanguageDialog()`/`showDisplayModeDialog()` already use. A switch can't represent three states; rebuilding a segmented-control custom view was considered and rejected as unnecessary UI complexity when a working, tested pattern already exists in this screen for the same "pick one of a few options" shape.

For email specifically: selecting any level other than Off persists that level immediately (mirroring today's `setEmailAlertsEnabled(true)` firing immediately on switch-flip, before credentials exist) and reveals the setup form if `!isEmailConfigured()`. Selecting Off persists immediately and hides the setup form, but — like today — leaves the status row visible (with the level shown alongside the address) if credentials are already saved, so the user can flip back to Alerts Only/All without re-entering the App Password. The Verify & Save / Edit / Clear sub-flow logic is otherwise unchanged; only the boolean checks (`repository.getEmailAlertsEnabled()`) become `repository.getEmailAlertLevel() != AlertLevel.OFF`.

### 4. Worker trigger logic: one pure function, shared by both channels
```kotlin
fun shouldAlert(level: AlertLevel, previousStatus: ModuleHealthStatus?, newStatus: ModuleHealthStatus): Boolean =
    when (level) {
        AlertLevel.OFF -> false
        AlertLevel.ALERTS_ONLY -> newStatus != previousStatus
        AlertLevel.ALL -> true
    }
```
Applied identically for push (`notificationLevel`) and email (`emailAlertLevel`) in `ModuleHealthWorker.doWork()`, each with its own `previousStatus` (`lastNotifiedStatus` / `lastEmailedStatus`). The existing `newStatus != UNKNOWN` guard stays as a precondition before calling this — an UNKNOWN status (fetch never succeeded) never alerts under any level, since there is nothing meaningful to report.

`lastNotifiedStatus`/`lastEmailedStatus` continue to be updated on every successful alert dispatch, including under `ALL` — this keeps the two tiers interchangeable: switching from All back to Alerts Only doesn't immediately re-fire for a status that hasn't actually changed.

### 5. Notifier gains a GREEN path, gated by an explicit flag
`ModuleHealthNotifier.notify(context, state, postOnGreen: Boolean)` — `postOnGreen = (notificationLevel == AlertLevel.ALL)`, passed in by the worker (the notifier itself doesn't read settings, staying consistent with it being a pure presentation object). GREEN now posts an "all clear" notification (new channel copy: `notification_module_health_green_title`/`_text`) when `postOnGreen` is true; otherwise behavior is unchanged (cancel). UNKNOWN always cancels regardless of `postOnGreen` — there's nothing to confirm.

The existing non-stacking behavior (same `NOTIFICATION_ID`, replaces rather than stacks) applies to the GREEN notification too, so a week of consecutive "All" GREEN days produces one notification, not seven.

## Risks / Trade-offs

- **[Risk]** A user who already had email alerts on gets silently reset if migration is missed on some code path → **Mitigation**: migration lives in the repository getter itself (single choke point), not in the UI layer, so every reader (worker, fragment, tests) goes through it.
- **[Risk]** New daily GREEN push notification may read as noisy to a user who only wanted the reassurance in their email digest → **Mitigation**: push and email levels are fully independent; a user can set email to All and leave push at Alerts Only.
- **[Trade-off]** Shared `AlertLevel` enum for both channels means the three option labels must read sensibly for both "notifications" and "email alerts" contexts — acceptable since Off/Alerts Only/All are generic terms, and each dialog uses its own string-resource set so wording can diverge per-context later without an enum change.

## Migration Plan

No backend/infra involved. Ships in a single app version:
1. New `AlertLevel` enum + new `SettingsRepository` keys/getters/setters land alongside the still-present legacy boolean keys (not removed from storage, just no longer written).
2. Lazy read-time migration (Decision 2) handles existing installs transparently on first read after upgrade — no explicit "migration step" in `onCreate`/`Application` needed.
3. UI, worker, and notifier changes ship in the same release (all call sites move to the new API together — this is why `getNotificationsEnabled()`/`setEmailAlertsEnabled()` etc. are deleted rather than deprecated, per the project's no-backwards-compat-shims convention).

Rollback: reverting to a prior app version would resume reading the (now-stale) legacy boolean keys, which were left untouched by the migration — so a rollback loses whatever level a user picked after upgrading (reverts to their pre-upgrade boolean state), but does not corrupt data or crash.

## Open Questions

None — the two behavioral ambiguities (recovery alerts under Alerts Only; daily GREEN push under All) were resolved before this document was written; see proposal.md.
