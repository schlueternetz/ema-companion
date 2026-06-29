# Notification Methods

This document compares the notification delivery methods available for EMA Companion module health alerts.

## Methods

### 1. Local Notifications (current MVP)

**How it works**: Android `NotificationManager` posts a notification from `ModuleHealthWorker` after each 24-hour background check.

| Attribute | Detail |
|-----------|--------|
| Setup cost | Zero — no accounts, no backend, no API keys |
| Latency | ~0–30 min after the 8pm check (WorkManager flex window) |
| Reliability | High on foreground devices; doze/battery-optimization can delay up to several hours on aggressive OEM skins |
| Cost | Free |
| Requires backend | No |
| User action | Grant `POST_NOTIFICATIONS` on Android 13+ |

**Best for**: MVP and self-hosted use. Works offline. No dependency on external services.

---

### 2. Gmail API Emails (user-authenticated, phase 2)

**How it works**: The app uses the Gmail REST API with the user's own Google account (OAuth 2.0) to send emails to themselves. No backend required.

| Attribute | Detail |
|-----------|--------|
| Setup cost | Low — user logs in with Google in-app; developer registers an OAuth client in Google Cloud Console (free) |
| Latency | Near-instant (seconds) after the check |
| Reliability | Very high — Gmail is highly available; token refresh is handled by Android AccountManager |
| Cost | Free (Gmail API has a generous free quota far above one email/day) |
| Requires backend | No |
| User action | Log in with Google; grant Gmail send scope |

**Best for**: Users who want email alerts without any backend infrastructure. Phase 2 priority.

**Limitation**: Only sends from the user's own Gmail address to themselves.

---

### 3. Firebase Cloud Messaging (recommended for server-push)

**How it works**: A small backend service calls the FCM API when a check detects a non-green status. The app registers a device token; the backend stores it and sends push notifications.

| Attribute | Detail |
|-----------|--------|
| Setup cost | Medium — Firebase project, backend service (e.g., Cloud Run), token registration endpoint |
| Latency | ~1–5 seconds end-to-end |
| Reliability | Very high — FCM is Google's production push infrastructure |
| Cost | FCM is free; backend hosting ~$0–5/month on Cloud Run free tier for this volume |
| Requires backend | Yes |
| User action | None after initial setup |

**Best for**: Multi-device delivery, future sharing with family members, or when reliable push is required without depending on WorkManager timing.

---

### 4. Email via SendGrid / AWS SES

**How it works**: A backend service sends transactional email via an email delivery API.

| Attribute | Detail |
|-----------|--------|
| Setup cost | Medium — account, API key, sender verification, backend service |
| Latency | Seconds to minutes (SMTP queue + delivery) |
| Reliability | High — delivery rates >99% with proper SPF/DKIM setup |
| Cost | SendGrid free tier: 100 emails/day. AWS SES: ~$0.10/1000 emails. Backend hosting adds cost. |
| Requires backend | Yes |
| User action | Enter email address in settings |

**Best for**: Users who prefer email over push. Can send to any address (family, monitoring alias).

---

### 5. Webhook (IFTTT / Applet / Zapier)

**How it works**: The app (or a minimal backend) fires an HTTP POST to a webhook URL. IFTTT/Zapier routes the event to any destination (SMS, email, Slack, etc.).

| Attribute | Detail |
|-----------|--------|
| Setup cost | Low — user creates a free IFTTT/Zapier applet and pastes a webhook URL into settings |
| Latency | 1–15 minutes (IFTTT polling delay on free tier; Zapier near-instant on paid) |
| Reliability | Medium — depends on IFTTT/Zapier uptime and the destination service |
| Cost | Free on IFTTT/Zapier basic; paid plans for multiple Zaps |
| Requires backend | No (if called directly from app) or minimal |
| User action | Create applet, paste webhook URL |

**Best for**: Power users who want to route alerts into custom workflows without writing code.

---

### 6. Telegram / Discord Webhooks

**How it works**: A POST to a Telegram Bot API or Discord webhook URL sends a message to a channel/chat.

| Attribute | Detail |
|-----------|--------|
| Setup cost | Very low — create a bot (Telegram: BotFather; Discord: server integration), copy webhook URL |
| Latency | Near-instant (1–3 seconds) |
| Reliability | High — both platforms have very high uptime |
| Cost | Free |
| Requires backend | No — the app calls the webhook URL directly |
| User action | Create bot/webhook, paste URL into settings |

**Best for**: Users already using Telegram or Discord, or who want instant free push to a group/family channel.

---

## Recommendation

| Phase | Method |
|-------|--------|
| MVP (current) | Local notifications |
| Phase 2 | Gmail API — zero backend, user-authenticated |
| Future | FCM with backend — for reliable multi-device push |

Telegram/Discord webhooks are the best low-effort option for users who want instant delivery to an existing chat platform without any backend.
