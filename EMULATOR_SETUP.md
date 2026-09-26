# Emulator startup recovery

The Medium Phone API 36.1 emulator was missing from ADB after the reported black/offline startup. Starting SDK ADB first and cold-booting with software graphics recovered it. A second boot using saved AVD settings also returned `device` and `sys.boot_completed=1`; the app opened past its splash screen with no AndroidRuntime crash logged.

Local AVD settings in `~/.android/avd/Medium_Phone.avd/config.ini` now use:

```ini
fastboot.forceColdBoot=yes
fastboot.forceFastBoot=no
hw.gpu.mode=swiftshader_indirect
```

The original configuration is retained as `config.ini.before-rakshyaa-repair`. No wipe-data command was used, and installed data was preserved. The recovery implicates the emulator startup/snapshot/graphics path; it does not prove which component originally failed. An initial daemon-start message is not itself an app crash.

From the Android project folder, a repeatable fallback is:

```powershell
.\scripts\start-emulator.ps1
```

This starts the SDK's ADB daemon before opening a visible emulator, bypasses snapshots and uses software graphics. Close an existing emulator before running it. It does not kill unrelated ADB sessions or delete AVD data.

Confirm startup with the SDK's `platform-tools/adb.exe devices -l` and `adb.exe -s emulator-5554 shell getprop sys.boot_completed`. Expect `device` and `1`. Wait for Android to finish booting before installing an APK. If ADB cannot start, inspect the owner of port 5037 instead of repeatedly wiping the AVD.

The supplied `assets/favicon520x520.png` (actual dimensions 512 x 512) is reused unchanged for app branding and launcher artwork. Adaptive icons add safe padding around the original artwork.
