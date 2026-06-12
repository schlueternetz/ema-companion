# ADR-005: In-App Markdown Rendering

**Status:** Accepted  
**Date:** 2026-06-11

## Context

The app bundles its user guide as Markdown assets (copied from `docs/user-guide/` at build time). This guide must be rendered inside the app with formatting, support inter-file navigation via relative links, and display images stored alongside the Markdown files. The library choice has long-term consequences: it affects dark mode behaviour, Material Design integration, testability, and whether a browser engine is needed.

## Decision

Use [Markwon](https://noties.io/Markwon/) (`io.noties.markwon:markwon-core` + `markwon-image`) with a custom `AsyncDrawableLoader` for asset-based images and a custom `LinkResolver` for inter-file navigation.

Implementation rules that follow from this decision:

- **`assetPath` is always a full path relative to the `assets/` root** (e.g. `user-guide/user-guide.md` in production, `feature/userguide/index.md` in tests). There is no hardcoded base path in the fragment.
- **The current folder for resolving relative links and images is derived from `assetPath`'s parent directory**, using `java.net.URI` for correct `../` handling.
- **The custom `AsyncDrawableLoader`** resolves relative image paths against the derived folder and loads them via `AssetManager`.
- **The custom `LinkResolver`** navigates `.md` links by calling `navController.navigate()` to a new `UserGuideFragment` destination with the resolved `assetPath` as an argument. External `http(s)://` links are opened via `Intent.ACTION_VIEW`.
- **`UserGuideFragment`** lives in `feature/userguide/` per ADR-004. Test fixtures go in `src/test/assets/feature/userguide/` so they are never included in the production APK.

## Alternatives Considered

### WebView + JS Markdown renderer (marked.js bundled as asset)

Relative links and images work natively via `file:///android_asset/` base URLs. Rejected: `WebView` is heavyweight, does not integrate with the system dark/light theme or Material text styles, and embedding a JS engine for a static documentation viewer is overengineered.

### WebView + pre-built HTML (Gradle MD→HTML conversion step)

Avoids the JS engine but introduces a build-time conversion tool (pandoc or Node). Rejected: same `WebView` theming drawbacks, plus added toolchain complexity and a new build dependency.

### Custom Markdown parser

No external dependencies. Rejected: significant implementation effort, ongoing maintenance burden, and no rendering quality benefit over Markwon.

## Consequences

- Markwon is pinned to a specific version in `gradle/libs.versions.toml`; Dependabot surfaces updates automatically (ADR-001).
- All future in-app documentation rendering follows the same pattern: bundle as assets via a Gradle `Copy` task wired to `preBuild`, render with Markwon, navigate via the Navigation Component.
- The custom `AsyncDrawableLoader` (~50 lines) is the only non-trivial bespoke code. If Markwon's image module API changes, only that class needs updating.
- Markwon's last public release was 4.6.2 (2021). The library is stable and widely used but in low-activity maintenance mode. This is an accepted risk; if the library becomes unmaintained the migration path is to a `WebView`-based renderer or Compose Markdown.
- `UserGuideFragment` Robolectric tests must use test fixtures in `src/test/assets/feature/userguide/` — not the production `user-guide/` assets — so tests are independent of the Gradle copy task having run.
