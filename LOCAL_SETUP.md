# Android local setup

Run Gradle from `rakshyaa/`. Commands below use PowerShell; POSIX users can use
`./gradlew` and `cp` in place of the Windows wrapper and copy command.

## Prerequisites

- JDK 17, Android SDK platform 36, SDK build tools, and platform-tools/ADB.
- Android Studio or equivalent command-line SDK setup.
- Android device/API 24+ or an emulator.
- For optional cloud work: Node.js/npm, backend dependencies, and Google OAuth configuration.

Set the SDK path using Android Studio or ignored `local.properties`. Use the
checked-in Gradle wrapper rather than a system Gradle installation.

## Default local mode

Without a `backend.properties` file, cloud features default off. If the file
already exists, inspect its cloud flag before assuming a build is local.

To create local configuration when no file exists:

```powershell
Copy-Item backend.properties.example backend.properties
```

It contains `ENABLE_CLOUD=false`. Build and install:

```powershell
.\gradlew.bat assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Choose **Continue on this device**. Contacts, records, settings, and encrypted
videos use local storage. Maps/geocoding and external apps may still require
connectivity. Profile photo upload and live backend place discovery require cloud.

## Optional Worker development

From the workspace's `backend/` directory:

```powershell
npm ci
Copy-Item .dev.vars.example .dev.vars
npm run db:migrate:local
npm run dev -- --port 8787
```

Fill `.dev.vars` using [backend setup](../backend/README.md); preserve an existing
file. Check `http://localhost:8787/health`. This guide deliberately pins the
port; the legacy Express port 8080 is not the active default.

Set the Android project's ignored `backend.properties`:

```properties
ENABLE_CLOUD=true
BACKEND_BASE_URL=http://10.0.2.2:8787
GOOGLE_WEB_CLIENT_ID=your-web-client-id.apps.googleusercontent.com
```

The emulator's `10.0.2.2` reaches the development host. On a USB-connected physical
device, one option for a debug build is `adb reverse tcp:8787 tcp:8787` with
`BACKEND_BASE_URL=http://localhost:8787`. Rebuild whenever properties change.

Debug cleartext exceptions are in
[app/src/debug/res/xml/network_security_config.xml](app/src/debug/res/xml/network_security_config.xml).
The [main configuration](app/src/main/res/xml/network_security_config.xml) forbids
cleartext, so release builds need HTTPS when cloud is enabled.

## Google configuration

1. Configure an OAuth **Web** client. Its client ID must match
   `GOOGLE_WEB_CLIENT_ID` in Android and the Worker bindings.
2. Configure an OAuth **Android** client for package `com.rakshyaa.rakshyaa`
   and the signing certificate used by the installed APK.
3. Read the certificate from your build instead of copying a developer-specific
   fingerprint from historical documentation:

   ```powershell
   .\gradlew.bat signingReport
   ```

4. Configure consent-screen access for the account you will test. If the project
   is in testing mode, include that account among permitted test users.
5. Use a device with Google Play services and a Google account. An emulator with
   a Google Play system image is suitable; an AVD is a local machine resource,
   not included by this repository.
6. Rebuild/install, choose Google sign-in, and confirm the backend token exchange
   completes. Merely seeing the Google button does not verify the cloud setup.

The Android OAuth client maps package/certificate identity. Its ID is not the
server client ID passed to `GetSignInWithGoogleOption`.

## Troubleshooting

| Symptom | Check |
| --- | --- |
| Only local login is offered | `ENABLE_CLOUD` in the properties used to build this APK |
| API connection fails | Wrangler is running, actual port matches, emulator uses host alias |
| Cleartext rejected | Debug variant and debug XML exceptions; use HTTPS for release |
| Google console/setup error | Installed APK certificate, package, Web-client audience, consent access |
| No credential available | Device Google account and Play services |
| API returns 401 | Session expiry/account deletion; sign in again and verify Worker secrets |
| Cloud backup unreadable | Original installation keys may be missing; preserve working local data |
| No live places | Cloud session, backend and Overpass connectivity; fallback entries are sample data |
| Emulator offline/black screen | Follow [emulator startup](EMULATOR_SETUP.md) |

Do not clear app data or uninstall as a casual sign-in workaround: it can destroy
the only usable decryption keys.

## Checks

```powershell
.\gradlew.bat test compileDebugKotlin lintDebug lintRelease
```

See [testing](../docs/TESTING.md) for scope and reports. Production configuration
is covered by [release process](../RELEASE.md).
