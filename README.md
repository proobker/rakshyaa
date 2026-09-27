# Rakshyaa

Rakshyaa is a native Android women's-safety app with optional cloud services and
an operator dashboard. The default build supports **Continue on this device**
without Google sign-in or a backend. Cloud mode adds Google authentication,
encrypted backups, profile synchronization, incident reporting, and nearby-place lookup.

## Components

| Directory | Implementation | Entry point |
| --- | --- | --- |
| [Android](rakshyaa/README.md) | Kotlin, Jetpack Compose, Hilt, Android Keystore | `rakshyaa/app/src/main/java/com/rakshyaa/rakshyaa/ui/MainActivity.kt` |
| [Backend](backend/README.md) | Cloudflare Workers, Hono, D1, Cloudinary, jose | `backend/src/worker/index.ts` |
| [Admin](admin/README.md) | Next.js Pages Router, React, TypeScript | `admin/pages/index.tsx` |

The inactive Express/filesystem implementation is archived under
[backend/legacy](backend/legacy/README.md). Current package scripts build and run the Worker.
There is no active Supabase integration.

## Repository layout

This repository contains all three components and preserves the Android Git
history. Open [rakshyaa.code-workspace](rakshyaa.code-workspace) in VS Code for
one Source Control root. Open the inner `rakshyaa/` directory in Android Studio.

```text
rakshyaa/        Android Gradle project
backend/        Cloudflare Worker, migrations, tests, and legacy archive
admin/          Next.js operator portal
docs/           Single documentation hub, including release and setup guides
assets/         Branding artwork
```

## Start with the Android app

Use JDK 17 and an Android SDK with platform 36. Open `rakshyaa/` as the Gradle
project in Android Studio, or run from that directory:

```powershell
.\gradlew.bat assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

On macOS/Linux, use `./gradlew` instead of `.\gradlew.bat`. Set the SDK location
through Android Studio or ignored `local.properties`. No backend properties are
needed for the default local build. Choose **Continue on this device** at launch.

For cloud mode, follow [local setup](docs/ANDROID_SETUP.md). It requires
`ENABLE_CLOUD=true`, a reachable backend, and matching Google Web client IDs.
Wrangler development normally uses port 8787; the guides set it explicitly.
Ignored local configuration can override the repository defaults.

## What the app does

- SOS countdown and foreground service, with separate SMS preparation and dialer actions.
- Encrypted contact lists, location history, ride records, check-in records, and videos.
- Fake-call simulation, saved places, and bundled legal-resource text.
- Profile editing, local session management, and installation-wide local data deletion.
- Optional encrypted backup and cloud incident reporting.
- Manual operator review of the latest 50 active cloud incidents.

SMS and calls require user action in the external app. Backend incident creation
does not establish that an operator saw an alert or dispatched help. Check-ins,
ride deviation, and sample place/resource data have
[known implementation limitations](docs/KNOWN_LIMITATIONS.md).

## Data boundaries

Local datastores and saved media use device-bound encryption. Backup payloads
remain ciphertext on the server, but profiles, incident coordinates/timestamps,
and backup metadata are readable by the backend. Nearby lookup sends coordinates
to the Worker and Overpass. Map and geocoding features may use network services
even in local mode.

Losing the installation's encryption keys makes its backup ciphertext unreadable.
Reinstall and cross-device recovery are not implemented. See
[security and data handling](docs/SECURITY.md).

## Documentation and validation

For publishing the backend and portal on GitHub, hosting your own backend, and
creating a signed Android release, start with the [documentation hub](docs/README.md).
It also includes the [technology inventory](docs/TECHNOLOGY_STACK.md), configuration,
operations, and troubleshooting guides.

The [documentation index](docs/README.md) links setup, architecture, API, testing,
deployment, release, and known-limitations guides. Run commands from the stated
component directory:

| Component | Checks |
| --- | --- |
| Android: `rakshyaa/` | `.\gradlew.bat test compileDebugKotlin lintDebug lintRelease` |
| Backend: `backend/` | `npm run typecheck`, `npm test`, `npx wrangler deploy --dry-run` |
| Admin: `admin/` | `npm run typecheck`, `npm run build` |

See [release readiness](docs/RELEASE_READINESS.md) for verification scope; current source
presence is not evidence that a production deployment or every device flow works.

## License

The Android project includes an [MIT license](rakshyaa/LICENSE). The backend
package declares MIT; the admin package declares ISC. These declarations should
be reconciled before assigning a single license to the complete distribution.
