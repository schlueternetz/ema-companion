## Context

Phase 1 (module-health-tile) provides:
- Module health status computation (GREEN/YELLOW/RED)
- 12-hour background check via WorkManager
- Local in-app notifications on status change

This phase adds email delivery of the same alerts. Users can opt-in via Google Sign-In (at signup or in Settings), and their module health alerts are sent to their Gmail inbox in their preferred language.

## Goals / Non-Goals

**Goals:**
- Allow users to receive module health alerts via email (opt-in feature)
- Translate emails to user's app language preference (EN/DE)
- Use user's own Gmail account (zero backend cost)
- Integrate seamlessly with Phase 1's notification system

**Non-Goals:**
- Replace local notifications (both coexist)
- Require email signup/verification (piggyback on Google Account)
- Backend email infrastructure or SendGrid integration
- Daily digest or scheduled emails (immediate alerts only)
- SMS or push notifications

## Decisions

### 1. Google Sign-In with Gmail Scope
**Decision**: Use Google Play Services OAuth 2.0 to authenticate users, requesting Gmail API "send" scope.

**Rationale**: Eliminates signup friction (one tap, already has Google Account). No password stored, no external email service required.

**Alternatives Considered**:
- Firebase Authentication: adds Firebase dependency, overkill for one OAuth flow
- Manual Google OAuth: more boilerplate, same end result

### 2. Optional at Signup or in Settings
**Decision**: Offer Google Sign-In as optional flow either during app onboarding (suggested) or always available in Settings > Email Alerts.

**Rationale**: Gives users choice; some want email, others don't. Reduces friction at signup (not mandatory), but easy to enable later.

**Alternatives Considered**:
- Mandatory at signup: too aggressive; local notifications are fine for non-email users
- Settings-only: some users might want it during onboarding

### 3. Secure Token Storage
**Decision**: Store OAuth access token in `SettingsRepository` using EncryptedSharedPreferences (AES256-GCM), same as existing API credentials.

**Rationale**: Consistent with app's existing security pattern. Token never logged or exposed. Survives app restart.

**Alternatives Considered**:
- In-memory only: lost on app close, poor UX
- Plain SharedPreferences: security risk

### 4. Email on Status Change (Not Throttled)
**Decision**: Send email when status changes (GREEN→YELLOW, YELLOW→RED, RED→YELLOW, etc.), NOT on every 12-hour check if status is unchanged.

**Rationale**: Avoids spam; user only notified when something new happens. Local notifications already throttled at 12h check level; emails add on top for users who opt in.

**Alternatives Considered**:
- Send on every check: spam
- Throttle emails separately: complicates logic, unclear to user

### 5. Translated Email Content
**Decision**: Email subject and body templates stored in strings.xml (EN and values-de/strings.xml), read at send time using user's app language preference.

**Rationale**: Reuses existing localization system. User's language choice is a first-class setting in the app; emails should match it.

**Alternatives Considered**:
- Hard-coded English: ignores German users
- Detect system locale: error-prone; app language can differ from device language

### 6. Fallback to Local Notification on Failure
**Decision**: If Gmail API call fails (network, token expired, rate limit), log the error and post a local notification instead. Do not crash.

**Rationale**: Email is a convenience, not essential. App remains reliable; user still gets notified locally.

**Alternatives Considered**:
- Retry with backoff: complex; 12h window is long anyway
- Silent failure: user unaware of alert

### 7. Token Refresh on Expiry
**Decision**: Before sending email, check token expiry; if expired, attempt silent refresh. If refresh fails, treat as failed send (fallback to local notification).

**Rationale**: OAuth tokens expire; silent refresh minimizes user friction. If refresh fails, something is wrong; local notification is safe fallback.

**Alternatives Considered**:
- Always request fresh token: more API calls, slower
- No refresh: token eventually stale, emails fail silently

## Risks / Trade-offs

| Risk | Mitigation |
|------|-----------|
| Gmail API rate limits (100 emails/day per user) | Acceptable for module alerts (max 2-3 per day in normal scenarios). Document if user exceeds limits. |
| Google account revoked or disconnected | Graceful: app detects in next check, falls back to local notifications, suggests re-signing-in. |
| Token expires and refresh fails (e.g., offline) | Fall back to local notification, retry refresh on next check when online. |
| User's Gmail quota full | Email fails silently (handled by Gmail API); local notification sent as fallback. |
| Translated email templates incomplete or incorrect | Verify with German speaker; use app's existing `values-de` strings as source of truth. |
| Wrong language selected (user changed locale mid-email) | Language is read at send time; emails match current preference. Previous emails may differ. Acceptable. |
| Google Play Services not available on device | Gmail Sign-In fails gracefully; local notifications remain functional. Document requirement. |

## Migration Plan

1. **Phase 1 complete**: module-health-tile shipped, local notifications working
2. **Phase 2 development**: Add Google Sign-In, token storage, Gmail API integration
3. **Phase 2 testing**: Test OAuth flow, token refresh, email sending on emulator with real Google Account
4. **Phase 2 rollout**: Feature flag or opt-in via Settings to control rollout; can disable if Gmail API issues arise

Rollback: Remove "Email Alerts" section from Settings; disable Gmail API calls; app reverts to local notifications only.

## Open Questions

1. **Email address collection**: Should we display user's Gmail address in Settings for confirmation? (Yes, recommended UX)
2. **Signup flow**: Should signup screen suggest email opt-in, or save for onboarding tutorial later? (Defer to PM)
3. **Email template design**: What should subject/body say? Examples: 
   - YELLOW: "⚠️ Module Alert: 1 module offline for 25 hours"
   - RED: "🚨 Module Critical: 1 module offline for 80 hours—action needed"
   - RECOVERY: "✅ All modules online—system recovered"
