---
name: local-android-dev
description: Runs the EMA Companion Android app on the local emulator in debug mode, or rebuilds and redeploys it. Use when the user asks to run, start, launch, deploy, rebuild, or update the app on the emulator.
allowed-tools: PowerShell Read
---

> **Scope**: manual testing and app verification only. For automated test execution (unit, Robolectric, instrumented), use `/ai-tdd`.

## Environment

- **SDK root:** `%LOCALAPPDATA%\Android\Sdk`
- **Emulator:** `%LOCALAPPDATA%\Android\Sdk\emulator\emulator.exe`
- **ADB:** `%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe`
- **AVD:** `Lenovo_Tab_11_Plus` (android-33, 1200×2000 portrait, 213 DPI — matches Lenovo Tab 11 Plus)
- **Project root:** `code\ema-companion`
- **APK output:** `app\build\outputs\apk\debug\app-debug.apk`
- **App ID:** `com.schlueternetz.emacompanion`
- **Main activity:** `.MainActivity`

## Task: Run the app (first launch)

1. **Start the emulator** (skip if already running):
   ```powershell
   $emulator = "$env:LOCALAPPDATA\Android\Sdk\emulator\emulator.exe"
   Start-Process -FilePath $emulator -ArgumentList "-avd Lenovo_Tab_11_Plus -no-snapshot-load" -WindowStyle Normal
   ```

2. **Wait for the emulator to be online:**
   ```powershell
   $adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
   do { Start-Sleep 5; $out = & $adb devices } until ($out -match "emulator.*device$")
   Write-Host "Emulator ready."
   ```

3. **Build and install the debug APK:**
   ```powershell
   Set-Location "code\ema-companion"
   .\gradlew.bat installDebug
   ```

4. **Launch the app:**
   ```powershell
   $adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
   & $adb shell monkey -p com.schlueternetz.emacompanion -c android.intent.category.LAUNCHER 1
   ```

## Task: Rebuild and redeploy (emulator already running)

Use this when the user asks to update, rebuild, or redeploy after a code change.

1. **Build and reinstall** (handles both build and install in one step):
   ```powershell
   Set-Location "code\ema-companion"
   .\gradlew.bat installDebug
   ```

2. **Re-launch the app:**
   ```powershell
   $adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
   & $adb shell monkey -p com.schlueternetz.emacompanion -c android.intent.category.LAUNCHER 1
   ```

## Gotchas

- `installDebug` combines `assembleDebug` + `adb install` — prefer it over running both separately.
- If the emulator is already running from a previous session, skip step 1 and check `& $adb devices` first.
- If `installDebug` fails with a `INSTALL_FAILED_UPDATE_INCOMPATIBLE` error, uninstall first: `& $adb uninstall com.schlueternetz.emacompanion`
- The `Medium_Tablet` AVD (android-33) also works as a fallback if `Lenovo_Tab_11_Plus` has issues.
- No android-31 system image is installed; android-33 is used instead (app supports both, `minSdk = 31`).
