## Why

Users who open the app for the first time — or after a reset — have no in-context help; they must locate the project README or GitHub docs separately. Embedding the existing `docs/user-guide/user-guide.md` directly in the app surfaces help where it's needed without maintaining a second copy.

## What Changes

- Add a **User Guide** destination to the bottom navigation bar (third item alongside Home and Settings).
- Render the `docs/user-guide/` folder as formatted Markdown inside the app, starting at `user-guide.md`; relative links between files and embedded images must work.
- Copy the entire `docs/user-guide/` folder into app assets at build time so the in-app copy stays in sync with the repo docs automatically.
- The User Guide navigation item is **always enabled**, even when the app is unconfigured (unlike Home, which is disabled until configuration is complete).

## Capabilities

### New Capabilities
- `user-guide`: In-app screen that renders the project user guide from a Markdown asset, always accessible regardless of configuration state.

### Modified Capabilities
- `main-navigation`: Third bottom-nav destination (User Guide) added alongside Home and Settings.
- `unconfigured-app-state`: User Guide navigation item must remain enabled while the app is unconfigured (only Home continues to be disabled).

## Impact

- New `feature/userguide/` package with a Fragment and Markdown rendering.
- `nav_graph.xml` and `bottom_nav_menu.xml` gain a third destination.
- `MainActivity` unconfigured-state logic updated to exempt the User Guide item from being disabled.
- Gradle copy task added to sync entire `docs/user-guide/` folder → `app/src/main/assets/user-guide/` on every build.
- Markdown rendering via `noties/Markwon` with its image plugin; inter-file link navigation via a custom link resolver backed by the Navigation Component back stack.
- Documentation is English-only — deliberate, no localisation needed for guide content.
