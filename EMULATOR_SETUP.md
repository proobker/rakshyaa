# Emulator setup and startup recovery

An Android Virtual Device must be created on the development machine. The
repository does not contain an installed AVD. For Google sign-in, use a Google
Play system image and configure the device account as described in
[local setup](LOCAL_SETUP.md). Local-device login does not require Google sign-in.

## Start an existing AVD

The helper [scripts/start-emulator.ps1](scripts/start-emulator.ps1) starts SDK ADB,
checks for an already-running/offline emulator, then launches a visible cold boot
with software graphics:

```powershell
.\scripts\start-emulator.ps1 -Avd Your_Avd_Name
```

Its default name is `Medium_Phone_API_36.1`; use your actual AVD name. SDK lookup
checks `ANDROID_SDK_ROOT`, then `ANDROID_HOME`, then
`%LOCALAPPDATA%\Android\Sdk`.

The script uses `-no-snapshot -gpu swiftshader_indirect`. It does not wipe AVD
data or terminate unrelated ADB sessions. Close an existing emulator first.

## Verify boot before installation

Using platform-tools on PATH:

```powershell
adb devices -l
adb -s emulator-5554 shell getprop sys.boot_completed
```

Replace the serial with the one listed by ADB. Expect status `device` and boot
property `1`. Then install from the Android project directory:

```powershell
adb -s emulator-5554 install -r app/build/outputs/apk/debug/app-debug.apk
```

Use the full SDK path if ADB is not on PATH. If the daemon cannot start, check
the owner of TCP port 5037 and competing SDK/ADB installations. For graphics or
snapshot issues, try the provided cold-boot helper before considering data loss.

## Historical recovery note

An earlier September 2026 run reported recovery of a black/offline emulator by
starting ADB first and using cold boot/software graphics. That machine reportedly
retained a backup of its AVD configuration. Those local AVD edits and the backup
are not repository setup prerequisites, and the report does not prove the state
of the emulator currently installed on another machine.

Clearing an AVD or app data can remove the encryption keys needed for local
records and cloud ciphertext. Preserve the installation while diagnosing startup.
