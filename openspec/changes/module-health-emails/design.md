## Context

Phase 1 (module-health-tile) provides:
- Module health status computation (GREEN/YELLOW/RED)
- 24-hour background check via WorkManager
- Local in-app notifications on status **change** (GREEN→YELLOW, YELLOW→RED, any→GREEN)

This phase adds email delivery of the same alerts. Users can opt-in in Settings by enabling the "Email Alerts" setting and signing in with Google, and their module health alerts are sent to their Gmail inbox in their preferred language.

## Goals / Non-Goals

**Goals:**
- Allow users to receive module health alerts via email (opt-in, Settings only)
- Trigger email on the same status changes that trigger local push notifications
- Translate emails to user's app language preference (EN/DE)
- Use user's own Gmail account (zero backend cost)
- Integrate with Phase 1's notification system — both channels fire together on status change

**Non-Goals:**
- Replace local notifications (both coexist)
- Offer email setup during onboarding or first-launch (Settings only)
- Backend email infrastructure or SendGrid integration
- Daily digest or scheduled emails (status-change alerts only)
- SMS or push notifications via third-party services

## Decisions

### 1. Sending mechanism: Gmail SMTP + App Password
**Decision**: Use JavaMail (jakarta.mail) to send email via `smtp.gmail.com:587` (STARTTLS). The user provides their Gmail address and a Gmail App Password in Settings. No OAuth, no GCP Console project, no consent screen review.

**User setup steps**:
1. Enable 2-Step Verification on their Google Account (one-time)
2. Go to Google Account → Security → App Passwords → create password for "EMA Companion"
3. Paste the 16-character App Password into Settings

**Rationale**: No service infrastructure, no GCP project, no OAuth complexity, no token lifecycle to manage. JavaMail is a pure JVM library with no Android-specific dependencies. App Password is a well-established pattern for app-specific credential management.

**Alternatives rejected**:
- Gmail API OAuth: requires GCP project, consent screen review, token refresh management, sensitive `mail.google.com` scope that triggers scary consent dialogs
- ntfy.sh: not email; different delivery paradigm, requires user to install a separate app
- Backend SMTP relay: costs money or has quota limits

**Token management**: None. SMTP authentication uses the static App Password on every send. No expiry, no refresh.

### 2. Settings-only opt-in
**Decision**: Email alerts are an optional setting in the Settings screen. They are never suggested during onboarding or first launch. The user enables "Email Alerts" (toggle), which reveals the sign-in / configuration UI. Without the toggle enabled, no auth UI is shown.

**Rationale**: Email alerts are a convenience layer on top of push notifications. Presenting them before the user has seen the app's core value (the Home screen with real data) is premature friction. Settings is the right place.

### 3. Secure credential storage
**Decision**: Store the Gmail address and App Password in `SettingsRepository` using EncryptedSharedPreferences (AES256-GCM), the same storage used for EMA API credentials. The App Password is never logged or displayed in plain text (masked, last 4 chars visible).

**Rationale**: Consistent with existing credential security pattern. App Password does not expire; no refresh logic needed. Cleared alongside other credentials on factory reset or settings import.

### 4. Email on status change only — aligned with push
**Decision**: Send email (and local push) when and only when status **changes**:
- GREEN → YELLOW: send YELLOW alert
- GREEN → RED: send RED alert
- YELLOW → RED: send RED alert (escalation)
- Any → GREEN: send recovery email
- RED → YELLOW: **no email, no downgrade** — status stays RED until fully GREEN

**Rationale**: Avoids spam; user only notified on meaningful transitions. Aligns email and push so both channels behave identically. RED → YELLOW is not a recovery — some modules are still offline; keeping RED until GREEN prevents confusing "partially recovered" messages.

**Phase 1 impact**: Phase 1 currently sends push on every 12/24-hour check when YELLOW/RED. This must be changed to status-change only as part of this phase, so push and email fire together.

### 5. lastEmailedStatus field
**Decision**: Persist a separate `lastEmailedStatus` (and `lastNotifiedStatus`) field in `ema_module_health` SharedPreferences, distinct from the displayed `status`. This is used to detect whether a status change warrants a new notification/email.

**Reset conditions**: Clear `lastEmailedStatus` and `lastNotifiedStatus` on:
- Factory reset
- Settings import (different system = unknown previous state)
- EMA credential change (System ID, ECU ID)

**Rationale**: Without a separate field, stale status from seeded data or a previous session can suppress the first real alert after reset. Clearing on credential change ensures the first check after connecting a new system fires an alert if the status is non-GREEN.

### 6. Fallback to local notification on email failure
**Decision**: If email send fails (network, token, quota), log the error and ensure local push still fires. Do not retry email until the next status change occurs.

**Rationale**: Email is a convenience supplement. App reliability must not depend on external email delivery.

### 7. Translated email content
**Decision**: Email subject and body templates stored in `strings.xml` (EN) and `values-de/strings.xml` (DE). Language is read from the user's app language preference at send time.

**Rationale**: Reuses existing localization system. User's language choice is a first-class setting; emails match it.

## Risks / Trade-offs

| Risk | Mitigation |
|------|-----------|
| App Password revoked or expired | SMTP send fails; fall back to local notification; Settings shows "Email delivery failed — check App Password" on next open |
| User forgets App Password setup steps | Help link in Settings; one-time setup, not repeated |
| Gmail SMTP rate limit (500/day) | Module alerts fire at most ~once/day in normal operation; irrelevant in practice |
| Translated email templates incomplete | Verify with German speaker; use existing `values-de` strings as source of truth |
| Phase 1 push behavior change (status-change only) | Existing Robolectric notification tests must be updated to match; no functional regression expected |

## Open Questions

1. **Email template design**: Subject/body copy for each transition (examples in `email-content-templates` spec).
