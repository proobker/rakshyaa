# Release process

Build configuration lives in [rakshyaa/app/build.gradle](../rakshyaa/app/build.gradle).
The source currently declares version name 1.1 and version code 2. This document
describes preparing a new artifact; it does not assert a published release.

## 1. Choose and record the build configuration

Run Android commands from `rakshyaa/`.

- Local mode: leave `ENABLE_CLOUD=false` or omit `backend.properties`.
- Cloud mode: set `ENABLE_CLOUD=true`, a production HTTPS `BACKEND_BASE_URL`,
  and the matching `GOOGLE_WEB_CLIENT_ID` in ignored `backend.properties`.

Record the mode, source revision, version, backend revision, and test environment
for the artifact. Increment `versionCode` for an update to an existing distribution
and choose the intended `versionName`. Update [release notes](RELEASE_NOTES.md)
to match changes actually included.

## 2. Supply signing inputs

Release signing is already wired in Gradle. It reads
`rakshyaa/.release/signing.properties`, with environment variables taking
precedence for each key:

```properties
RAKSHYAA_KEYSTORE=.release/upload-keystore.jks
RAKSHYAA_STORE_PASSWORD=<private-value>
RAKSHYAA_KEY_ALIAS=<private-value>
RAKSHYAA_KEY_PASSWORD=<private-value>
```

Relative keystore paths resolve from the Android project root. Use the certificate
intended for the distribution; do not regenerate it merely to make a build pass.
Keep signing material and passwords in secure backups and out of version control.

`validateReleaseConfiguration` is attached to release packaging/bundle tasks.
It requires all signing inputs and an existing keystore. With cloud enabled it
also checks HTTPS endpoint shape and a plausible Google Web client ID.
Unit tests using release classes do not require signing credentials.

For cloud authentication, register the installed artifact's signing certificate
and package with Google. See [Google release configuration](GOOGLE_SIGN_IN.md).

## 3. Verify code and package

From the Android directory:

```powershell
.\gradlew.bat test compileDebugKotlin lintDebug lintRelease
.\gradlew.bat assembleDebug assembleRelease bundleRelease
```

Use `./gradlew` on POSIX. For a cloud release, also run the backend typecheck,
Worker tests, and dry-run bundle from `backend/`; verify the admin production
build from `admin/`. Exact checks and manual scenarios are in
[testing](TESTING.md).

Expected artifact paths relative to `rakshyaa/`:

| Artifact | Path |
| --- | --- |
| Debug APK | `app/build/outputs/apk/debug/app-debug.apk` |
| Release APK | `app/build/outputs/apk/release/app-release.apk` |
| Release AAB | `app/build/outputs/bundle/release/app-release.aab` |

These are output locations, not promises that a fresh checkout contains binaries.
An AAB cannot be installed directly with ADB.

Using installed Android SDK build tools, verify the exact release APK:

```powershell
apksigner verify --verbose --print-certs app/build/outputs/apk/release/app-release.apk
zipalign -c -P 16 4 app/build/outputs/apk/release/app-release.apk
Get-FileHash app/build/outputs/apk/release/app-release.apk -Algorithm SHA256
Get-FileHash app/build/outputs/bundle/release/app-release.aab -Algorithm SHA256
```

Put build tools on PATH or invoke their full paths. Archive the output alongside
the artifact. ZIP alignment alone does not establish native-library compatibility;
include native alignment inspection and runtime acceptance on the target devices.

## 4. Perform acceptance checks

Use consenting testers and disposable cloud accounts. Verify permission denial,
approximate location, offline startup, screen lock, service/process lifecycle,
SOS cancellation, message composition, recording/playback, logout/login, and
deletion. Do not send emergency calls or messages as an unattended test.

Read [known limitations](KNOWN_LIMITATIONS.md) before defining acceptance
criteria. In particular, check-in contact delivery, planned-route alerts, reliable
incident retries, and cross-installation recovery are not established behavior.

Record physical-device results, accessibility checks, failures, and unresolved
lint findings in [release readiness](RELEASE_READINESS.md). Mark skipped checks
explicitly; a successful build is not end-to-end certification.

## 5. Distribute the reviewed artifact

Back up the signing identity and publish only the artifact whose checksum and
configuration were verified. For a store release, complete that store's current
distribution requirements using the actual app behavior and data flows.
Store publication, provider provisioning, and purchases are separate operations
from creating the APK/AAB.

For cloud rollouts, follow [backend deployment](BACKEND_DEPLOYMENT.md) and verify
the production integration before promoting a client that depends on it.
