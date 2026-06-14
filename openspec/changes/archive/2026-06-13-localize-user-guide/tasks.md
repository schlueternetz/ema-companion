## 1. Skill: German generation phase

- [x] 1.1 Add `Bash` to `allowed-tools` in `.claude/skills/write-user-guide/SKILL.md` (for `npx mmdc`); expect the `skill-check` reminder hook to fire on the edit
- [x] 1.2 Add a German-generation section to the skill: after writing the English guide, translate `docs/user-guide/user-guide.md` → `app/src/main/assets/user-guide/user-guide-de.md` (German is in-app only, NOT written to `docs/`). Use real in-app German UI labels from `values-de/strings.xml`
- [x] 1.3 In the skill, translate the **quoted labels only** of `docs/user-guide/system-context.mmd` into a transient `system-context-de.mmd` (never translate mermaid ids); document the temp-path + discard step
- [x] 1.4 In the skill, render via mmdc: transient `system-context-de.mmd` → `app/src/main/assets/user-guide/system-context-de.png`, and re-render `docs/user-guide/system-context.mmd` → `docs/user-guide/system-context.png` (EN stays in docs). Use `npx -p @mermaid-js/mermaid-cli mmdc` (mmdc is not assumed on PATH)
- [x] 1.5 Have the skill write a "GENERATED — source of truth is English, do not hand-edit, rerun the skill" HTML-comment header at the top of each generated German markdown file
- [x] 1.6 Update the skill's Rules/Gotchas: German is generated not authored, in-app only (asset folder, not `docs/`); `*-de.mmd` is transient and not committed; only `user-guide-de.md` + `system-context-de.png` are committed

## 2. Provenance & gitignore

- [x] 2.1 Add a note to `AGENTS.md` (User guide section): German is in-app only, generated from the English sources by `write-user-guide` into `app/src/main/assets/user-guide/`; edit English only and rerun the skill; never hand-edit a `*-de.*` file
- [x] 2.2 Rewrite the asset-folder rule in `code/ema-companion/.gitignore`: ignore `/app/src/main/assets/user-guide/*` (build copy target for English `docs/` files) **except** `!*-de.md` / `!*-de.png` (committed German originals). The transient `*-de.mmd` is covered by the `/*` ignore

## 3. Fragment: locale-aware entry asset

- [x] 3.1 In `UserGuideFragment`, resolve the loaded `assetPath` to its `-de.md` sibling when the current locale is German **and** that asset exists (`AssetManager.list`), else the requested file (EN fallback). Uses `ConfigurationCompat.getLocales(resources.configuration)` (reliable runtime signal; avoids the `getApplicationLocales()` Robolectric pitfall). Generalized from "entry-only" to "any loaded `.md` by suffix" — strictly a superset, EN-fallback, and makes it testable with debug fixtures (see 6.1/6.2)
- [x] 3.2 Inter-file link/image **resolution** unchanged — localization is applied only at load time, not at navigation time; existing link-navigation test still green under the default locale

## 4. Generate initial German artifacts

- [x] 4.1 Generated `app/src/main/assets/user-guide/user-guide-de.md` and `system-context-de.png` (and re-rendered `docs/user-guide/system-context.png` — identical bytes, no churn). English guide line updated ("guide follows your app language"). mmdc via `npx -p @mermaid-js/mermaid-cli` (v11.15.0)
- [x] 4.2 Spot-checked the German diagram PNG visually: translated labels (*Systemkontext*, *Solaranlage*, *Verwendet*, *Ruft Daten ab*, *Produktion/Module/Daten (Berichte)/Einstellungen*), proper nouns preserved (EMA App/Companion/API), structure intact

## 5. Optional: guarded render task

- [~] 5.1 (Optional) **Deferred — not needed.** A guarded `preBuild` render task adds little here: the skill already re-renders the EN `.png` from its `.mmd`, and the German `.mmd` is transient (not committed), so there is no committed `.mmd` for the task to track on the German side. Revisit only if EN diagram drift becomes a problem in practice

## 6. Tests

- [x] 6.1 Robolectric test `userGuideFragment_germanLocaleLoadsLocalizedAsset` (`@Config(qualifiers = "de")`): with `index-de.md` present, the fragment loads it; includes ATF checks. Added `src/debug/assets/feature/userguide/index-de.md` fixture
- [x] 6.2 Robolectric test `userGuideFragment_germanLocaleFallsBackToEnglishWhenNoLocalizedAsset` (`@Config(qualifiers = "de")`): with no `linked-page-de.md`, the fragment falls back to the English `linked-page.md`; includes ATF checks
- [x] 6.3 Ran `./gradlew testDebugUnitTest ktlintCheck` — BUILD SUCCESSFUL (all tests green, ktlint clean)

## 7. Verification

- [x] 7.1 Manually confirmed on the `Lenovo_Tab_11_Plus` AVD (German locale): app runs in German (nav: Startseite/Benutzerhandbuch/Einstellungen), User Guide shows the German guide — verified working by the user
- [x] 7.2 Confirmed `git add -n` stages only `app/src/main/assets/user-guide/user-guide-de.md` + `system-context-de.png` (German, in-app); English asset copies stay ignored; no `*-de.mmd` staged; `docs/` is English-only

## 8. Documentation

- [x] 8.1 `AGENTS.md` and the `write-user-guide` skill both reflect the English-source-of-truth, German-in-app-only workflow (covered by 1.6 / 2.1). Skill also passed `skill-check` (version pinned, Bash constraint noted)
