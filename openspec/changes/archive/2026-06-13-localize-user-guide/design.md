## Context

The user guide lives in `docs/user-guide/` (English) and is rendered in-app by `UserGuideFragment` via Markwon, reading from `assets/user-guide/` — a gitignored folder populated at build time by the `copyUserGuideAssets` Gradle `Copy` task (committed sources in `docs/`, derived assets in `assets/`). The same markdown also renders on GitHub. The diagram is a committed PNG rendered from a committed mermaid source (`system-context.mmd`); there is currently no automated render step. The app supports English and German.

This design was reached through extended exploration; several alternatives were considered and rejected (see below). The guiding insight: the pain is **drift**, not translation effort, and the guide is small (one ~122-line file, one diagram) for a handful of expected international users — so the cheapest mechanism that avoids drift wins.

## Goals / Non-Goals

**Goals:**
- German users see the guide (prose + diagram) in German, generated from the English source.
- English remains the single source of truth and the only GitHub-rendered version.
- Builds are reproducible without the skill in the build pipeline (CI must not need an LLM or mmdc).
- The EN diagram never drifts from its `.mmd` source.

**Non-Goals:**
- Human review / hand-fixing of the German (explicitly out for now — see "Future" for the upgrade path).
- Runtime / on-device translation (ML Kit, Cloud Translation API).
- Per-string or per-section translation memory / anti-clobber machinery.
- A native-view re-render of the guide.
- Localising guide content beyond German.
- Translating the diagram inside the rendered guide at runtime.

## Decisions

### Translate at build/dev time via the skill, not at runtime
Runtime machine translation (ML Kit on-device, or Cloud Translation API) is rejected: it is non-deterministic (breaks Robolectric/ATF tests), needs a model download or network + an embeddable API key, and mangles markdown syntax. The skill (an LLM in a dev session) produces German once, deterministically reviewable in git.

### English is the only source; German is generated, in-app only, and committed
`docs/user-guide/` holds **English only** (`user-guide.md`, `system-context.mmd`, `system-context.png`) — this is the single source of truth and the only version rendered on GitHub. The generated German artifacts (`user-guide-de.md`, `system-context-de.png`) live **in the app asset folder** `app/src/main/assets/user-guide/`, never in `docs/`, so German is in-app only and never published on GitHub. They are committed there for **build reproducibility** (the build consumes committed files and never depends on the skill running) via a `.gitignore` negation: the asset folder is ignored (it is the build copy target for the English `docs/` files) **except** the `*-de.md` / `*-de.png` originals. German is treated like generated code / a lockfile: committed, but regenerated from English, never hand-edited.

> **Why not commit German in `docs/`?** It would render on GitHub and live in the documentation tree. German is wanted in-app only. Committing it in the app asset folder (the place it is actually consumed) keeps it out of `docs/`/GitHub while still being reproducible.

### The diagram is never translated as an image — only its `.mmd` source
A PNG is a raster; translating it is impossible. The chain is `EN.mmd → (translate quoted labels) → DE.mmd → mmdc → DE.png`. Only quoted strings in the mermaid source are translated; ids (`Person(owner, ...)`) are left untouched — a leaked id breaks rendering, which fails loudly.

### `system-context-de.mmd` is transient, not committed
The German `.mmd` carries no information not derivable from `EN.mmd` + the translation step; every refresh starts from `EN.mmd`. It is generated to a temp path, fed to mmdc, and discarded (or gitignored). Only `DE.png` (the shipped artifact) is committed. Committed `.mmd` exists **only** where it is an editable source — i.e. English.

### The skill re-renders `EN.png` too
Since the skill runs mmdc anyway, it re-renders `system-context.png` from `system-context.mmd`, guaranteeing the committed EN PNG matches its source. (Expect a one-time byte churn on the existing EN PNG the first time it is rendered through mmdc.)

### `UserGuideFragment` selects the localized asset by locale, with EN fallback
At load time the fragment swaps the asset it reads for its `-de.md` sibling when the current locale is German **and** that sibling exists; otherwise it loads the requested file. Locale is read via `ConfigurationCompat.getLocales(resources.configuration)` (the applied per-context locale — reliable at runtime, and avoids the `getApplicationLocales()` Robolectric pitfall noted in lessons-learned). Fallback means a build/locale lacking a translation still shows English rather than erroring. Link/image **resolution** (navigation targets) is unchanged — localization is applied only at the read step, so existing link-navigation behaviour is preserved. (Generalized from "entry-only" to "any loaded `.md` by suffix": a strict superset, EN-fallback, and what makes it testable with the `src/debug/assets/feature/userguide/` fixtures.)

### Provenance is recorded so German is never mistaken for hand-maintained
Two cheap layers: (1) a generated-by header comment at the top of each German file — invisible in-app (no `HtmlPlugin` registered) and on GitHub (HTML comments are hidden); (2) a short note in `AGENTS.md`. Both say: source of truth is English; do not hand-edit; rerun the skill.

### Render trigger: skill phase, optional guarded preBuild task — no render hook
mmdc rendering is folded into the skill (it already does the translation, runs on docs updates, and gains `Bash`). A standalone `PostToolUse` render hook is rejected: it only fires on the agent's Edit/Write tool calls, silently missing manual IDE edits, so the committed PNG could drift from a hand-edited `.mmd`. The robust catch-all is an **optional** `preBuild` Gradle task that re-renders a `.png` when its `.mmd` is newer and **skips silently if mmdc is absent** — keeping render local-only while never breaking a CI build (which uses the committed PNG).

## Rejected Alternatives

| Alternative | Why rejected |
|---|---|
| Runtime translation (ML Kit / Cloud Translation API) | Non-deterministic (breaks tests), needs model/network, API key not safely embeddable, mangles markdown. |
| Replace `strings.xml` with machine translation | Strictly worse than Android's free, instant, offline resource system; breaks tests, a11y, UI timing; only 2 locales — no scaling problem to solve. |
| Gitignore German (generated only, uncommitted) | Skill is not in the build pipeline, so DE would be absent from any build not preceded by a skill run (incl. CI). Committing fixes reproducibility. |
| Commit German in `docs/` (alongside English) | Would render German on GitHub and put it in the documentation tree. German is wanted **in-app only** → commit it in the app asset folder (`app/src/main/assets/user-guide/`) via a `.gitignore` negation instead. |
| Commit German and hand-fix it | Wanted initially for quality, then dropped — few international users; ships raw MT for now, upgrade later if quality bites. |
| Native-view generated from markdown + string resources | Requires building a markdown→Android-views compiler (≈ reimplementing Markwon, incl. tables/links/asset images) for marginal gain; per-segment granularity not needed at this scale. |
| Section-hash anti-clobber manifest + lock flag | Solves a problem (overwriting human fixes) that does not exist while German is generated-not-fixed; deferred. |
| `PostToolUse` hook that runs mmdc on `.mmd` writes | Only catches the agent's tool edits, not manual IDE edits → PNG can drift; npx+Chromium startup cost per write. |

## Future / Upgrade Path

If the German quality becomes a problem and/or international users grow:
1. Start hand-editing the committed German files and stop blindly regenerating them — *committed-generated → committed-maintained, zero architecture change.*
2. Add an anti-clobber mechanism so the skill regenerates a German **section only when its English source changed** (per-section source-hash in a sidecar manifest, plus a `locked` flag for frozen sections). The rule: *regenerate a section ⇔ its English changed; everything else is untouched.*

## Risks / Trade-offs

- **Unreviewed machine translation ships.** Accepted given few international users; mitigated by graceful EN fallback and the documented upgrade path.
- **German present only in builds made after a skill run** — committing the German artifacts removes this risk for normal builds; a CI/clean build still reflects whatever German is committed.
- **mmdc toolchain is local-only.** First `npx mmdc` run downloads Chromium (~300 MB); subsequent runs cost a few seconds. Acceptable for an occasional docs-update step; never required in CI.
- **EN PNG byte churn** the first time it is rendered through mmdc. One-time.
- **LLM could translate a mermaid id instead of a label**, breaking the diagram. Mitigated: translate quoted strings only; a bad render fails loudly.
