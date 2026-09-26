# Rakshyaa Android app

A Kotlin/Jetpack Compose app for local safety records and optional cloud services.
Build from this directory; the workspace root is not the Android Gradle project.

## Build configuration

The checked-in configuration is:

| Setting | Value |
| --- | --- |
| Application ID / namespace | `com.rakshyaa.rakshyaa` |
| Version name / code | 1.1 / 2 |
| Minimum / compile / target SDK | 24 / 36 / 36 |
| Android Gradle Plugin / Gradle wrapper | 8.10.1 / 8.11.1 |
| Kotlin / Hilt | 2.0.20 / 2.52 |
| Compose BOM / CameraX | 2024.08.00 / 1.4.2 |
| Java/Kotlin target | 17 |
| Cloud default | Disabled |

Sources: [app/build.gradle](app/build.gradle), [build.gradle](build.gradle), and
[Gradle wrapper properties](gradle/wrapper/gradle-wrapper.properties).
Ignored `backend.properties` may override local build settings.

```powershell
.\gradlew.bat assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Use [LOCAL_SETUP.md](LOCAL_SETUP.md) for prerequisites, local mode, and optional
Google/Worker configuration. On POSIX, replace `.\gradlew.bat` with `./gradlew`.

## Feature map

| Feature | Current implementation |
| --- | --- |
| Login | Local-device session; Google sign-in shown when cloud is enabled |
| Home/navigation | Compose dashboard, bottom tabs, feature routes |
| SOS | Cancellable countdown, foreground runtime state, local incident and optional cloud report |
| Emergency actions | Prepare SMS for up to five valid distinct contacts; open unnumbered dialer |
| Contacts | Encrypted CRUD and primary-contact selection |
| Location | Foreground GPS logging, recent history, one-shot location with stored fallback |
| Rides | Start/end records and GPS points; deviation heuristic has limitations |
| Check-ins | Pending/completed/missed records and service timer; lifecycle/delivery gaps remain |
| Safe places | Saved entries, optional Overpass-backed lookup, bundled sample fallback |
| Legal resources | Bundled text and user-added notes |
| Fake call | Delayed simulated incoming/connected call with ringtone/vibration |
| Video | CameraX capture, encrypted local media, optional best-effort cloud upload |
| Profile | Local or remote text editing, cloud photo upload, settings, sign-out, deletion |

Feature presence does not certify reliable emergency delivery. See
[known limitations](../docs/KNOWN_LIMITATIONS.md), especially check-ins, ride deviation,
sample locations/resources, and cloud media lifecycle.

## Source map

Paths below are relative to `app/src/main/java/com/rakshyaa/rakshyaa/`.

| Path | Responsibility |
| --- | --- |
| `RakshyaaApplication.kt`, `di/` | Hilt application and location-provider binding |
| `ui/MainActivity.kt`, `ui/navigation/` | Session gate, scaffold, feature routes |
| `ui/screens/`, `ui/components/`, `ui/theme/` | Compose screens, emergency intents, theme |
| `viewmodels/` | UI state and orchestration |
| `data/auth/`, `data/network/` | Sessions, Google exchange, account deletion, API client/DTOs |
| `data/local/`, `utils/CryptoManager.kt` | Account-scoped encrypted storage and preferences |
| `data/models/`, `data/repositories/` | Serialized records and feature persistence |
| `data/sync/` | Backup upload, authenticated restore, login orchestration |
| `services/` | Four manifest services and six injected helpers |
| `utils/GeoUtils.kt` | Distance calculations |

The [manifest](app/src/main/AndroidManifest.xml) registers SOS, location tracking,
ride monitoring, and check-in services. Helper classes such as FakeCallService
are not manifest services.

## Persistence and privacy

App files are scoped by account ID. Logout keeps saved files, while account/data
deletion clears all local application data. Backups use device-bound keys and
cannot recover encrypted content after losing the original installation's keys.

Cloud profiles and incidents are readable by the backend; encrypted backups are
a separate channel. Detailed behavior is in [architecture](../docs/ARCHITECTURE.md)
and [security](../docs/SECURITY.md).

## Verification and release

```powershell
.\gradlew.bat test compileDebugKotlin lintDebug lintRelease
```

Run `connectedDebugAndroidTest` with a configured device for instrumentation.
See [testing](../docs/TESTING.md), [release process](../RELEASE.md), and
[Google release configuration](GOOGLE_RELEASE.md). Production signing is already
wired through ignored signing properties or environment variables.

For contributor constraints, read [CLAUDE.md](CLAUDE.md) and the root
[AGENTS.md](../AGENTS.md). The Android license is [MIT](LICENSE).
