## Why

EMA Companion is a free app maintained by one developer. There is currently no way for users to discover that voluntary support is welcome, and no in-app link to the developer's Buy Me a Coffee page or website.

## What Changes

- Add a fourth bottom navigation destination, "Support," alongside Home, User Guide, and Settings.
- Add a new `SupportFragment` screen with two actions: "Buy Me a Coffee" (opens `https://buymeacoffee.com/schlueternetz`) and "Visit Website" (opens `https://www.schlueternetz.com`).
- Both links open via a plain `Intent.ACTION_VIEW` to the device's default browser/app — no new dependency, no in-app WebView.
- Update the bottom navigation bar to four items: Home, User Guide, Settings, Support (in that order).
- Append a footer with the same two links (Buy Me a Coffee, website) to every module-health email alert (YELLOW, RED, and GREEN templates in `EmailContentBuilder`).

## Capabilities

### New Capabilities
- `support-links`: A Support screen reachable from the bottom navigation bar, offering outbound links to the developer's Buy Me a Coffee page and personal website.
- `email-alert-support-links`: Module-health alert emails include a footer with the Buy Me a Coffee and website links.

### Modified Capabilities
- `main-navigation`: The bottom navigation bar changes from three fixed destinations (Home, User Guide, Settings) to four (Home, User Guide, Settings, Support), with Support added as the rightmost item.

## Impact

- New: `feature/support/SupportFragment.kt` + layout, plus a nav graph destination and bottom nav menu entry.
- Modified: `res/menu/bottom_nav_menu.xml`, `res/navigation/nav_graph.xml`, `MainActivity` (if it references nav destinations directly), `strings.xml` (+ `values-de/strings.xml` for German).
- Modified: `core/email/EmailContentBuilder.kt` (append support-links footer to `buildBody` for all non-UNKNOWN statuses) and its test.
- No EMA API calls, no SharedPreferences/data storage, no ADR-009 budget impact.
- Docs: new `docs/user-guide/support.md` page (or a section on an existing page) via `write-user-guide`.
