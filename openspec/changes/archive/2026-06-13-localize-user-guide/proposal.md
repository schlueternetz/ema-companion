## Why

The in-app user guide is English-only, but the app supports English and German (ADR-003). German users get the whole guide — prose and the C4 system-context diagram — in English. Hand-maintaining a parallel German guide rots (the app's own `strings.xml` already drifts: 72 EN vs 70 DE keys), so translation should be generated from the English source, not authored twice.

## What Changes

- English stays the **single source of truth** in `docs/user-guide/` and the only version rendered on GitHub.
- The `write-user-guide` skill gains a **German generation phase** (run during a docs update, not during the app build): translate `user-guide.md` → `user-guide-de.md`, translate the quoted labels in `system-context.mmd` → a transient `system-context-de.mmd`, and render `system-context-de.png` via mmdc. It also re-renders `system-context.png` from the EN `.mmd` so the EN diagram never drifts from its source.
- German is **in-app only** — the generated `user-guide-de.md` and `system-context-de.png` are written into the app asset folder `app/src/main/assets/user-guide/` (NOT `docs/`, so German never appears on GitHub). They are **committed** there via a `.gitignore` negation (so builds are reproducible and the skill is never required in the build pipeline), but flagged as generated and **not hand-edited**.
- `UserGuideFragment` selects the `-de` asset when the app locale is German, with **graceful fallback to English** when a German asset is absent.
- No new runtime translation, no API keys, no CI/GitHub dependency on translation. mmdc rendering is local-only.

## Capabilities

### Modified Capabilities
- `user-guide`: The entry guide is rendered in the app's current locale (German when set), falling back to English. German guide assets are generated from the English sources.

## Impact

- `write-user-guide` skill: `Bash` added to `allowed-tools`; new translation + mmdc render phases; writes a "generated, do not edit" header into each German file; writes German into the app asset folder, not `docs/`.
- New committed files (in `app/src/main/assets/user-guide/`): `user-guide-de.md`, `system-context-de.png`. The intermediate `system-context-de.mmd` is transient (gitignored / discarded after render). German is **in-app only** — never in `docs/`, never on GitHub.
- `.gitignore` (`code/ema-companion/.gitignore`): the asset `user-guide/` folder is ignored except the committed `*-de.md` / `*-de.png` (negation), so the English files copied from `docs/` stay ignored while German is tracked.
- `AGENTS.md`: short note recording that German is in-app-only, generated from the English sources (edit English, rerun the skill).
- `UserGuideFragment`: locale-aware entry asset selection with EN fallback.
- `copyUserGuideAssets` Gradle task: unchanged — it copies the English `docs/user-guide/` folder into `assets/user-guide/`; the committed German files coexist there (the copy never deletes them).
- Optional: a guarded `preBuild` render task that refreshes a `.png` whose `.mmd` is newer, skipping silently when mmdc is unavailable (keeps committed PNGs fresh on machines that have mmdc without breaking CI).
- No localisation of the in-app **navigation label** changes here (`nav_user_guide` already exists); this change is about guide **content**.
