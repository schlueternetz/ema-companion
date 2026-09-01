---
name: qa
description: Runs the full local pre-flight QA pass before a commit — unit/Robolectric tests, ktlint, debug build+install, and Maestro E2E flows on the emulator. Use when the user asks to QA, run a pre-flight/pre-commit check, verify before committing, or run the end-to-end flows. Not for a single test (use /ai-tdd) or just launching the app (use /local-android-dev).
allowed-tools: Bash Read
---

A deliberate, on-demand quality gate for committing straight to `main`. Run it at meaningful checkpoints (end of a feature, before a risky commit) — not on every edit. It runs the cheap checks first and only reaches the emulator-dependent E2E step at the end, so failures surface fast.

Report a single pass/fail summary at the end. **Stop and report** at the first failing layer — don't push on to the emulator if unit tests are red (an E2E run on broken code wastes time).

**A failing layer blocks the commit. Full stop.** Do not report a red result as a known/pre-existing issue and continue anyway, and do not push code while any layer is red — this has caused real CI breaks reaching `main`. Either fix the failure in this same pass, or stop and ask the user whether to explicitly defer it, and get their answer before anything is committed or pushed. "I found it and mentioned it" is not a substitute for "it's green or the user signed off."

## Step 1 — Fast checks (no emulator)

```bash
cd code/ema-companion
./gradlew testDebugUnitTest ktlintCheck
```

- `testDebugUnitTest` runs all unit + Robolectric tests.
- `ktlintCheck` runs `ktlintMainSourceSetCheck`/`ktlintTestSourceSetCheck` (and the kts-script check) — it does cover `.kt` main and test sources, not just build scripts. (A prior version of this note claimed `.kt` sources weren't wired under AGP 9.2.1; verified against ktlint 14.2.0 / AGP 9.3.0 that both source sets now run as real, non-skipped tasks — treat a green `ktlintCheck` as full coverage unless you observe otherwise.)
- If a Kotlin/Gradle plugin version was just bumped, stop any running Gradle/Kotlin daemons first (`./gradlew --stop`) — a stale `KotlinCompileDaemon` can keep compiling against the old version and produce misleading failures.

If either fails, stop and report — do not proceed to the build/E2E steps.

## Step 2 — Build & install the debug APK

Requires a running emulator. Reuse `/local-android-dev` for full emulator/install details (AVD `Lenovo_Tab_11_Plus`).

```bash
ADB="$HOME/Android/Sdk/platform-tools/adb"
"$ADB" start-server
"$ADB" devices
```

- **No emulator online** (no `emulator-...device` line): skip Steps 2–3, mark E2E **SKIPPED (no emulator)** in the summary, and tell the user how to start one (`/local-android-dev`). The fast checks still stand on their own.
- **Emulator online:** build and install:

```bash
cd code/ema-companion
./gradlew installDebug
```

## Step 3 — Maestro E2E flows

The flows also need `code/ema-api-stub` running and freshly reset — start it and confirm it's up before invoking Maestro:

```bash
cd code/ema-api-stub
nohup ./gradlew run > /tmp/ema-api-stub.log 2>&1 &
disown
# poll until ready, then reset immediately before the run (see Gotchas)
for i in $(seq 1 30); do
  curl -fsS -X POST "http://localhost:8080/__stub__/reset" >/dev/null 2>&1 && break
  sleep 2
done
```

Maestro CLI, if not already installed:
```bash
curl -fsSL "https://get.maestro.mobile.dev" | bash
export PATH="$PATH:$HOME/.maestro/bin"
```

Run the flows:
```bash
maestro test code/ema-companion/maestro/a-home-screen.yaml code/ema-companion/maestro/bottom-nav.yaml code/ema-companion/maestro/email-alerts.yaml
```

- Flow order is explicit (matching `.github/workflows/ci.yml`), not a bare directory path — `maestro test <dir>` iterates filesystem order, which is arbitrary on Linux/ext4. `a-home-screen.yaml` must run first (see its header comment) to avoid the accessibility driver degrading after other flows' `clearState`+relaunch cycles.
- If the `maestro` command is not found, mark E2E **SKIPPED (Maestro CLI not installed)** and point to https://maestro.mobile.dev/getting-started/installing-maestro — do not fail the whole QA pass on a missing CLI.
- Maestro runs black-box against the installed APK; flows live in `code/ema-companion/maestro/` (ADR-002).
- A run longer than ~2 minutes is normal for the full 3-flow suite — don't cut it short on a naive timeout; background it and poll/monitor for completion instead.

## Step 4 — Report

Output a compact summary, one line per layer:

```
## QA Pre-flight

- Unit + Robolectric: PASS (N tests)
- ktlint (.kt + .kts): PASS
- Debug build + install: PASS
- Maestro E2E: PASS (M flows)  |  SKIPPED (no emulator)

Verdict: READY TO COMMIT  |  BLOCKED — <first failing layer>
```

## Gotchas

- Run the fast lane first; the emulator steps are the slowest and most fragile — never run them ahead of green unit tests.
- `installDebug` re-runs the `copyUserGuideAssets` task, so the latest `docs/user-guide/` content is bundled before E2E.
- `INSTALL_FAILED_UPDATE_INCOMPATIBLE` → `adb uninstall com.schlueternetz.emacompanion` then retry (see `/local-android-dev`).
- **Reset the stub immediately before running Maestro, every time** — any manual `curl`/debug request against it in between (even just a health check done minutes earlier) silently advances its per-ECU interaction cursor and the next real run fails looking like a regression. If you've been poking the stub manually while debugging, reset again right before the real run.
- **One Maestro failure with a genuinely rendered error screen (not a system dialog) is still worth one clean retry before treating it as a real regression** — especially on a loaded dev machine running a Gradle/Kotlin daemon, an IDE, and the emulator side by side. `./gradlew --stop` to clear compile daemons, reset the stub, and rerun. Confirmed pattern from prior incidents: driver-under-load can corrupt an `inputText` field the validator then silently rejects, producing a real (not flaky-looking) fetch-error render with no other clue. Always check the actual screenshot at `~/.maestro/tests/<timestamp>/<flow>/screenshots/` before deciding — an ANR/system dialog covering a healthy app looks different from a real app-rendered error, and both need different follow-up.
- This skill is read-only on the codebase — it verifies, it does not fix. If a layer fails, hand control back with the failure; reproduce at the lowest test layer per ADR-002 before touching code. The one exception is a failure this skill's own retry guidance already resolves (stale daemon, unreset stub) — that's re-running the check, not fixing code.
