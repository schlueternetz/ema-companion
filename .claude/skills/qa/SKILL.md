---
name: qa
description: Runs the full local pre-flight QA pass before a commit — unit/Robolectric tests, ktlint, debug build+install, and Maestro E2E flows on the emulator. Use when the user asks to QA, run a pre-flight/pre-commit check, verify before committing, or run the end-to-end flows. Not for a single test (use /ai-tdd) or just launching the app (use /local-android-dev).
allowed-tools: PowerShell Read
---

A deliberate, on-demand quality gate for committing straight to `main`. Run it at meaningful checkpoints (end of a feature, before a risky commit) — not on every edit. It runs the cheap checks first and only reaches the emulator-dependent E2E step at the end, so failures surface fast.

Report a single pass/fail summary at the end. **Stop and report** at the first failing layer — don't push on to the emulator if unit tests are red (an E2E run on broken code wastes time).

## Step 1 — Fast checks (no emulator)

```powershell
Set-Location "code\ema-companion"
.\gradlew.bat testDebugUnitTest ktlintCheck
```

- `testDebugUnitTest` runs all unit + Robolectric tests.
- `ktlintCheck` currently lints only `.kts` scripts — the ktlint 12.1.1 plugin does not wire the Android `.kt` source sets under AGP 9.2.1 (known repo-wide gap). Treat a green `ktlintCheck` as **necessary but not sufficient** for `.kt` style; note this in the summary rather than implying full lint coverage.

If either fails, stop and report — do not proceed to the build/E2E steps.

## Step 2 — Build & install the debug APK

Requires a running emulator. Reuse `/local-android-dev` for emulator/install details (AVD `Lenovo_Tab_11_Plus`, ADB at `%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe`).

```powershell
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
$devices = & $adb devices
```

- **No emulator online** (`$devices` has no `emulator-...\tdevice` line): skip Steps 2–3, mark E2E **SKIPPED (no emulator)** in the summary, and tell the user how to start one (`/local-android-dev`). The fast checks still stand on their own.
- **Emulator online:** build and install:

```powershell
Set-Location "code\ema-companion"
.\gradlew.bat installDebug
```

## Step 3 — Maestro E2E flows

```powershell
maestro test code\ema-companion\maestro\a-home-screen.yaml code\ema-companion\maestro\bottom-nav.yaml code\ema-companion\maestro\email-alerts.yaml
```

- Flow order is explicit (matching `.github/workflows/ci.yml`), not a bare directory path — `maestro test <dir>` iterates filesystem order, which is alphabetical on Windows/NTFS but arbitrary on Linux/ext4 (GitHub's runners). `a-home-screen.yaml` must run first (see its header comment) to avoid the accessibility driver degrading after other flows' `clearState`+relaunch cycles.
- If the `maestro` command is not found, mark E2E **SKIPPED (Maestro CLI not installed)** and point to https://maestro.mobile.dev/getting-started/installing-maestro — do not fail the whole QA pass on a missing CLI.
- Maestro runs black-box against the installed APK; flows live in `code\ema-companion\maestro\` (ADR-002).

## Step 4 — Report

Output a compact summary, one line per layer:

```
## QA Pre-flight

- Unit + Robolectric: PASS (N tests)
- ktlint (.kts only — .kt not wired): PASS
- Debug build + install: PASS
- Maestro E2E: PASS (M flows)  |  SKIPPED (no emulator)

Verdict: READY TO COMMIT  |  BLOCKED — <first failing layer>
```

## Gotchas

- Run the fast lane first; the emulator steps are the slowest and most fragile — never run them ahead of green unit tests.
- `installDebug` re-runs the `copyUserGuideAssets` task, so the latest `docs/user-guide/` content is bundled before E2E.
- `INSTALL_FAILED_UPDATE_INCOMPATIBLE` → `& $adb uninstall com.schlueternetz.emacompanion` then retry (see `/local-android-dev`).
- This skill is read-only on the codebase — it verifies, it does not fix. If a layer fails, hand control back with the failure; reproduce at the lowest test layer per ADR-002 before touching code.
