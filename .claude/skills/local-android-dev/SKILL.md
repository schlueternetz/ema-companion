---
name: local-android-dev
description: Runs the EMA Companion Android app on the local emulator in debug mode, or rebuilds and redeploys it. Use when the user asks to run, start, launch, deploy, rebuild, or update the app on the emulator. Do NOT use for running automated tests — use /ai-tdd instead.
allowed-tools: Bash Read
---

> **Scope**: manual testing and app verification only. For automated test execution (unit, Robolectric, instrumented), use `/ai-tdd`.

## Environment

- **SDK root:** `$HOME/Android/Sdk`
- **Emulator:** `$HOME/Android/Sdk/emulator/emulator`
- **ADB:** `$HOME/Android/Sdk/platform-tools/adb`
- **AVD:** `Lenovo_Tab_11_Plus` (android-33, 1200×2000 portrait, 213 DPI — matches Lenovo Tab 11 Plus). `OnePlus_13_Pro` also exists as a second profile.
- **Debug artifacts dir:** `$HOME/ema-debug` — never inside the project directory.
- **Project root:** `code/ema-companion`
- **APK output:** `app/build/outputs/apk/debug/app-debug.apk`
- **App ID:** `com.schlueternetz.emacompanion`
- **Main activity:** `.MainActivity`

## Task: Run the app (first launch)

1. **Start the emulator** (skip if already running — check `adb devices` first). The emulator process outlives this command; launch it detached rather than foreground, or it blocks the session:
   ```bash
   nohup "$HOME/Android/Sdk/emulator/emulator" -avd Lenovo_Tab_11_Plus -no-snapshot-load -no-audio -no-boot-anim > /tmp/emulator-boot.log 2>&1 &
   disown
   ```

2. **Wait for the emulator to be online.** The adb server frequently isn't running yet on a fresh machine (`start-server` first avoids an "unable to connect to adb daemon" race), then poll `devices`:
   ```bash
   ADB="$HOME/Android/Sdk/platform-tools/adb"
   "$ADB" start-server
   until "$ADB" devices | grep -q "device$"; do sleep 5; done
   echo "Emulator ready."
   ```
   Cold boot (no snapshot) takes roughly 45-60s on top of the above — `adb devices` reporting the device doesn't by itself mean boot has finished; grep `/tmp/emulator-boot.log` for `Boot completed` if timing matters.

3. **Build and install the debug APK:**
   ```bash
   cd code/ema-companion
   ./gradlew installDebug
   ```

4. **Launch the app:**
   ```bash
   "$HOME/Android/Sdk/platform-tools/adb" shell monkey -p com.schlueternetz.emacompanion -c android.intent.category.LAUNCHER 1
   ```

## Task: Rebuild and redeploy (emulator already running)

Use this when the user asks to update, rebuild, or redeploy after a code change.

1. **Build and reinstall** (handles both build and install in one step):
   ```bash
   cd code/ema-companion
   ./gradlew installDebug
   ```

2. **Re-launch the app:**
   ```bash
   "$HOME/Android/Sdk/platform-tools/adb" shell monkey -p com.schlueternetz.emacompanion -c android.intent.category.LAUNCHER 1
   ```

## Gotchas

- **Temp files outside the repo**: all `adb pull` outputs, screenshots, and XML dumps go to `$HOME/ema-debug` — never inside the project directory. Create it first if needed: `mkdir -p ~/ema-debug`.
- `installDebug` combines `assembleDebug` + `adb install` — prefer it over running both separately.
- If the emulator is already running from a previous session, skip the launch step and check `adb devices` first.
- If `installDebug` fails with an `INSTALL_FAILED_UPDATE_INCOMPATIBLE` error, uninstall first: `adb uninstall com.schlueternetz.emacompanion`
- `OnePlus_13_Pro` is a fallback AVD if `Lenovo_Tab_11_Plus` has issues.
- No android-31 system image is installed; android-33 is used instead (app supports both, `minSdk = 31`).
- A stale `KotlinCompileDaemon`/Gradle daemon from a prior build can linger holding an old dependency version in memory after a `libs.versions.toml`/plugin version bump — `./gradlew --stop` before a build that must reflect a just-changed Kotlin/plugin version.
