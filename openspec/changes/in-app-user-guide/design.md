## Context

The app currently has two bottom-nav destinations (Home, Settings). The project's user guide lives in `docs/user-guide/` and is not accessible from within the app. The folder will grow into multiple linked Markdown files and may include images. When unconfigured, all non-Settings nav items are disabled by iterating the menu — a pattern that must be extended to exempt the new User Guide item.

## Goals / Non-Goals

**Goals:**
- Add a third bottom-nav item that renders the user guide, starting at `user-guide.md`.
- Support relative links between Markdown files within the guide folder.
- Render images stored alongside the Markdown files.
- Keep the nav item always enabled (never disabled, even when unconfigured).
- Sync the entire `docs/user-guide/` folder automatically via a Gradle copy task.

**Non-Goals:**
- Localisation of user guide content (English-only, deliberate).
- Online/remote fetch of the guide.
- Search within the guide.
- Dark-mode-specific Markdown styling beyond Markwon defaults.

## Decisions

### Markdown rendering: Markwon with image plugin + custom link resolver

Use [Markwon](https://noties.io/Markwon/) with two additional modules:
- `markwon-image` — renders inline images from asset URIs.
- Custom `LinkResolver` — intercepts taps on `.md` links and navigates to a new `UserGuideFragment` instance via the Navigation Component.

| Option | Pros | Cons |
|---|---|---|
| **Markwon + image plugin + link resolver** | Native rendering, dark mode via system theme, no WebView sandbox | Requires a custom asset image loader and link resolver |
| WebView + JS Markdown renderer (marked.js bundled) | Zero Android-library deps, relative links and images work natively | WebView overhead, poor system-theme integration, JS engine in a doc viewer is overengineered |
| WebView + pre-built HTML (Gradle conversion step) | Relative links and images work natively | Build-time MD→HTML conversion adds toolchain complexity |

Markwon keeps rendering in a native `TextView` and integrates with Material text styles. The additional complexity (image loader, link resolver) is well-contained inside `UserGuideFragment`.

### Inter-file navigation: `UserGuideFragment` with `assetPath` argument

`UserGuideFragment` accepts an `assetPath` argument that is a **full path relative to the `assets/` root** (e.g. `user-guide/user-guide.md` in production, `feature/userguide/index.md` in tests). The current folder for resolving relative links and images is derived from `assetPath`'s parent — no hardcoded base path in the fragment. The custom `LinkResolver`:
- For links ending in `.md`: resolves the path relative to the current file's folder using `java.net.URI`, then calls `navController.navigate()` to a new `UserGuideFragment` destination with the resolved path as argument.
- For external `http(s)://` links: opens the system browser via `Intent.ACTION_VIEW`.

The Navigation Component's back stack handles all back-navigation automatically — no custom stack management needed.

### Image loading: custom `AsyncDrawableLoader` for assets

Markwon's default image plugin loads from network or data URIs; it does not read from `assets/` out of the box. A small custom `AsyncDrawableLoader` implementation intercepts relative image paths, resolves them against the current file's parent folder (derived from `assetPath`), and loads them via `AssetManager`. This keeps the image paths in the Markdown files simple (e.g. `![screenshot](screenshots/home.png)`) and works for any base folder, including test fixtures.

### Asset sync: Gradle `Copy` task (entire folder) wired to `preBuild`

A `tasks.register<Copy>` block in `app/build.gradle.kts` copies the entire `docs/user-guide/` directory tree into `src/main/assets/user-guide/` before every build. The folder under `src/main/assets/user-guide/` is **gitignored** (derived artifact). The canonical source remains `docs/user-guide/`.

### Unconfigured-state exemption: allowlist over blocklist

`MainActivity.disableNonSettingsNavItems` currently disables everything that isn't `settingsFragment`. Change the logic to disable everything not in an explicit _always-enabled_ set: `{settingsFragment, userGuideFragment}`. Easier to extend than a blocklist and makes intent explicit.

### Feature package: `feature/userguide/`

Consistent with ADR-004 (feature-first packaging). `UserGuideFragment` and its custom Markwon helpers live here.

### Test fixtures: `src/test/assets/feature/userguide/`

Robolectric with `isIncludeAndroidResources = true` merges `src/test/assets/` into the test context alongside `src/main/assets/`, but test assets are never included in the production APK. Fixtures go in a feature-specific folder that mirrors the source package structure and cannot collide with production assets:

```
src/test/assets/feature/userguide/
  index.md          # references linked-page.md and test-image.png
  linked-page.md    # a second page to verify back-navigation
  test-image.png    # a tiny valid PNG to verify image loading
```

This also clarifies the `assetPath` contract: the argument is a path relative to the `assets/` root (e.g. `feature/userguide/index.md` in tests, `user-guide/user-guide.md` in production). The fragment derives the current folder from `assetPath`'s parent, so image and link resolution work correctly for any base folder with no hardcoded `user-guide/` prefix in the fragment.

## Risks / Trade-offs

- **Asset image loader complexity** — The custom `AsyncDrawableLoader` adds ~50 lines but is self-contained. If the Markwon image module's API changes, only this class needs updating. → Pin Markwon version in `libs.versions.toml`.
- **Real-doc tests depend on copy task** — Tests that use the real `user-guide.md` (e.g. smoke test that something loads) require `preBuild` to have run first. → Use test fixtures for behaviour tests; reserve real-doc tests for a single existence check that gives a clear error if the copy hasn't run.
- **Relative path resolution for deep pages** — If `user-guide.md` links to `topics/setup.md` and that file links back to `../user-guide.md`, the resolver must normalise paths correctly. → Use `java.net.URI` relative resolution, which handles `../` correctly.
- **Large image assets** — The APK size grows with each image added to the guide. → Acceptable for a documentation folder; flag if size becomes a concern.
