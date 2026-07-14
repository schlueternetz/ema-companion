## 1. Resources

- [x] 1.1 Add strings to `values/strings.xml` (English) and `values-de/strings.xml` (German): `nav_support`, `support_bmac_title`/action label, `support_website_title`/action label, and the two URL strings (`support_bmac_url`, `support_website_url` — not user-facing text but kept as string resources for consistency, not hardcoded in Kotlin; these two URL strings are reused by `EmailContentBuilder` in section 4)
- [x] 1.2 Add a `ic_nav_support` vector drawable for the bottom nav icon (match style of existing `ic_nav_home`/`ic_nav_settings`/`ic_nav_user_guide`)

## 2. SupportFragment (AI-TDD: red → green → refactor)

- [x] 2.1 Write a failing Robolectric test (`feature/support/SupportFragmentTest.kt`) asserting: tapping the "Buy Me a Coffee" row fires an `ACTION_VIEW` intent with data `https://buymeacoffee.com/schlueternetz` (assert via `Shadows.shadowOf(activity).nextStartedActivity`)
- [x] 2.2 Extend the test to assert tapping "Visit Website" fires an `ACTION_VIEW` intent with data `https://www.schlueternetz.com`
- [x] 2.3 Implement `feature/support/SupportFragment.kt` + `res/layout/fragment_support.xml` (two clickable rows, no ViewModel/repo — static content per design.md) to make the tests pass
- [x] 2.4 Refactor for clarity/style consistency with `UserGuideFragment`; re-run tests to confirm still green (skipped: implementation was already minimal, nothing to clean up)

## 3. Navigation wiring

- [x] 3.1 Add `supportFragment` destination to `res/navigation/nav_graph.xml`
- [x] 3.2 Add `supportFragment` item (rightmost, after `settingsFragment`) to `res/menu/bottom_nav_menu.xml`
- [x] 3.3 Update `MainActivityTest` (and any other test asserting the fixed set/count of bottom-nav items) to expect four destinations (also updated `MainActivity.applyUnconfiguredNavState` to keep Support enabled when unconfigured, matching User Guide's existing behavior, since it needs no EMA config)
- [x] 3.4 Update `maestro/bottom-nav.yaml`: assert `supportFragment` is visible alongside the other three items, tap it and assert the Support screen content is shown, and fix the stale "Settings is reachable again (always rightmost)" comment/assertion now that Support is rightmost

## 4. Email alert footer (AI-TDD: red → green → refactor)

- [x] 4.1 Add `email_body_support_footer` templated string (`%1$s`/`%2$s` placeholders for the BMAC and website URLs) to `values/strings.xml` and `values-de/strings.xml`
- [x] 4.2 Write a failing test in `EmailContentBuilderTest` asserting `buildBody(...)` output contains both URL strings for YELLOW, RED, and GREEN statuses (UNKNOWN unchanged — still `""`)
- [x] 4.3 Update `EmailContentBuilder.buildBody()` to append the support-links footer (built from `support_bmac_url`/`support_website_url` added in 1.1) after the existing content for YELLOW, RED, and GREEN; make the test pass
- [x] 4.4 Refactor if needed; re-run `EmailContentBuilderTest` to confirm still green (skipped: no cleanup needed)

## 5. Verification

- [x] 5.1 Run `./gradlew testDebugUnitTest` — all tests green
- [x] 5.2 Run `./gradlew ktlintCheck` — passes
- [x] 5.3 Run `/qa` (full pre-flight: unit tests, lint, debug build+install, Maestro flows on emulator) and confirm all flows pass before marking this change complete (3/3 Maestro flows passed: a-home-screen, bottom-nav, email-alerts; unrelated `ema-api-stub` server had to be started first for `a-home-screen`'s local-stub scenario — not caused by this change)

## 6. Documentation

- [x] 6.1 Invoke `write-user-guide` once (after all UI changes above are complete) to document the new Support screen in `docs/user-guide/` and add it to the `user-guide.md` index
