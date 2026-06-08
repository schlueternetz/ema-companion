# ADR-004: Package and Code Organization

**Status:** Accepted  
**Date:** 2026-06-08

## Context

The app will grow significantly: multiple feature screens (production stats, widgets, notifications, alerts), an EMA API client, shared data storage, and UI utilities. Without a clear convention, all classes accumulate in the root package and become impossible to navigate or reason about at 30+ files.

The current package root is `com.schlueternetz.emacompanion`.

## Decision

### Feature-first package structure

All application code is organized by feature first, then by layer within a feature. The root package contains only the single-activity entry point.

```
com.schlueternetz.emacompanion/
├── MainActivity.kt                   # single-activity host; stays at root
├── feature/                          # one sub-package per user-visible feature
│   ├── home/
│   │   └── HomeFragment.kt
│   ├── settings/
│   │   ├── SettingsFragment.kt
│   │   └── SettingsRepository.kt
│   ├── production/                   # (future) production stats screen
│   └── widgets/                      # (future) home-screen widgets
└── core/                             # shared code used by ≥2 features
    ├── api/                          # (future) EMA API client and DTOs
    ├── ui/                           # (future) shared views, adapters, theme helpers
    └── data/                         # (future) shared DB, preferences utilities
```

Test packages mirror source packages:

```
src/test/java/com/schlueternetz/emacompanion/
├── MainActivityTest.kt
├── feature/
│   ├── home/
│   │   └── HomeFragmentTest.kt
│   └── settings/
│       ├── SettingsFragmentTest.kt
│       └── SettingsRepositoryTest.kt
```

### Rules

1. **New feature → new sub-package under `feature/`.** A Fragment, ViewModel, and any repository specific to that feature all live together in `feature/<name>/`. No class should live in the root package except `MainActivity`.

2. **Repository placement follows ownership.** A repository starts in the feature package that owns it (`feature/settings/SettingsRepository`). If a second feature needs it, move it to `core/data/` — not before.

3. **`core/` requires two consumers.** A class moves to `core/` only when it is needed by two or more features. Premature extraction into `core/` creates shared mutable state before there is genuine sharing.

4. **No layer-only sub-packages inside a feature.** `feature/settings/ui/` and `feature/settings/data/` are unnecessary until a feature contains 8+ classes. Prefer a flat feature package.

5. **ViewModels live in the feature package** (when introduced). `feature/settings/SettingsViewModel.kt` — never in a separate `viewmodel/` package.

## Alternatives Considered

### Layer-based (`ui/`, `data/`, `domain/`)

Splits a feature's related files across three unrelated directories. Finding everything for the Settings feature requires looking in `ui/settings/`, `data/settings/`, and `domain/settings/`. Discarded: poor locality for feature-level navigation.

### Flat root package

Simple now (5 files), unmaintainable at 30+. The existing `MainActivity.kt`, `HomeFragment.kt`, `SettingsFragment.kt`, and `SettingsRepository.kt` were all peers with no structure. Discarded: does not scale.

### Separate Gradle modules per feature

Correct for large teams or enforced API boundaries. For the current project size, the build complexity outweighs the benefit. The feature package structure maps directly to Gradle modules if isolation becomes necessary later — no code restructuring required at that point.

## Consequences

- Adding a new feature means adding `feature/<name>/` — no existing code changes.
- All code related to a feature is found in one directory; reviewing or deleting a feature is a single-folder operation.
- The promotion path for shared code is explicit: feature-local → `core/` when shared by ≥2 features.
- Class count per package stays small and readable.
- Consistent with the [Android app architecture guide](https://developer.android.com/topic/architecture) recommendation to organize by feature rather than by layer.
