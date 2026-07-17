## Context

The bottom nav (`res/menu/bottom_nav_menu.xml` + `res/navigation/nav_graph.xml`) currently has three fixed fragment destinations: Home, User Guide, Settings (per ADR-004 feature-first packaging, each fragment lives in its own `feature/<name>/` package). None of them link outside the app today. This change adds a fourth destination, `SupportFragment`, that is pure UI with two outbound links — no data, no repository, no ADR-009 API budget impact.

## Goals / Non-Goals

**Goals:**
- Add a "Support" bottom nav destination with two tappable rows: "Buy Me a Coffee" and "Visit Website".
- Open both links externally via `Intent.ACTION_VIEW`, matching the simplicity of the rest of the app (no new Gradle dependency).
- Keep the screen static and stateless — no persistence, no ViewModel needed.

**Non-Goals:**
- No in-app WebView or Chrome Custom Tabs (rejected — adds `androidx.browser` dependency for a screen that's inherently "leave the app").
- No BMAC widget/SDK embedding — BMAC has no native Android SDK; only a hosted web page exists.
- No tracking/analytics of link taps.

## Decisions

**Fourth bottom nav tab vs. entry inside Settings.** Chose a dedicated tab because the user asked for something "always accessible like Settings" — a Settings sub-item would be one tap deeper and less discoverable. Four tabs is still comfortably within Material bottom-nav guidance (up to 5) and fits the reference tablet (Lenovo Tab P11 Plus, ADR-003).

**Plain `ACTION_VIEW` intent vs. Chrome Custom Tabs.** Chose plain intent per user's explicit preference: zero new dependencies, simplest implementation, consistent with how the rest of the app has no browser integration today. Custom Tabs would give a themed in-app-feeling transition but isn't worth the added dependency for two static outbound links.

**Fragment shape.** Modeled directly on `UserGuideFragment` (static content fragment with no ViewModel/repo) rather than introducing a new pattern — two buttons in a layout, each with an `OnClickListener` firing `startActivity(Intent(ACTION_VIEW, Uri.parse(url)))`. URLs are hardcoded string resources (not user-configurable, unlike EMA API base URL), since they're the developer's own fixed links.

**Testing.** Robolectric test drives clicks and asserts the fired intent's action/data (via `Shadows.shadowOf(activity).nextStartedActivity`), same pattern already used for other outbound-intent assertions in this codebase (`ModuleHealthRepository` email tests use an analogous shadow-intent check). No MockWebServer/network involved.

**Email footer placement.** `EmailContentBuilder.buildBody()` already appends a `email_body_cta` line for YELLOW/RED and has a dedicated `email_body_green` string; append a footer built from a new templated string `email_body_support_footer` (`%1$s`/`%2$s` placeholders) filled with the *same* `support_bmac_url`/`support_website_url` string resources added for the Support screen (task 1.1) — one URL string per link, reused via `context.getString(...)` in both `SupportFragment` and `EmailContentBuilder`, never duplicated as a second literal. Joined after the existing content the same way the CTA line is (`"$existing\n\n$footer"`). UNKNOWN stays untouched (its body is `""` and no email is ever sent for it, per ADR-008).

## Risks / Trade-offs

- [Hardcoded URLs become stale if the developer changes accounts] → Low risk; they're plain string resources, trivially updated in a future change, no migration needed.
- [Four bottom-nav items reduces per-icon tap-target width] → Still comfortably above the 48dp minimum (ADR-003) on the reference tablet; verify visually during QA.
- [Google Play policy on donation links] → Not a concern: this links out to an external, browser-hosted donation page and does not implement in-app billing or solicit payment for app features, which is the pattern Play policy restricts.

## Migration Plan

Additive UI change, no data migration. Roll out as a normal app update; no rollback concerns beyond reverting the commit if issues arise.

## Open Questions

None — user confirmed both design decisions (dedicated tab, plain browser intent).
