# Android agent guidance

Read the root [AGENTS.md](../AGENTS.md) first. This file applies to the Android
Gradle project, not the sibling backend or admin applications.

## Before changing code

- Read [local setup](LOCAL_SETUP.md) for build/configuration changes.
- Read [architecture](../docs/ARCHITECTURE.md) and [security](../docs/SECURITY.md)
  before changing sessions, account storage, sync, profile, media, or deletion.
- Check [known limitations](../docs/KNOWN_LIMITATIONS.md) before claiming check-in,
  ride, incident-delivery, or recovery behavior is complete.
- Read [release process](../RELEASE.md) for signing or distribution work.

## Android-specific constraints

- Run Gradle here: `.\gradlew.bat` on Windows or `./gradlew` on POSIX.
  Toolchain versions and SDK levels are authoritative in `build.gradle`,
  `app/build.gradle`, and the Gradle wrapper properties.
- Keep `ENABLE_CLOUD=false` as the configuration default and preserve the
  `local-device` session path. Build properties are compiled into BuildConfig.
- Prefer fully-qualified `com.rakshyaa.rakshyaa.R.*` for new references.
  Existing explicit imports of the application R class remain valid.
- Manifest services: SOSActivationService, LocationTrackingService,
  RideMonitoringService, CheckInService. Use `@AndroidEntryPoint` and field
  injection because Android creates these instances.
- Helpers: VideoEncryptionService, EmergencyContactsService, FakeCallService,
  LegalHelpService, SafePlacesService, GeocodingService. Use singleton constructor
  injection; do not register them as Android services merely because of their names.
- Keep restore orchestration in AuthViewModel, outside AuthRepository, to avoid
  the AppDataSync/ProfileRepository dependency cycle.
- Preserve account-scoped storage and authenticate downloaded ciphertext before
  replacing local data. Device-bound keys do not provide reinstall recovery.
- Use explicit user actions for SMS composition and dialer launch. Service active
  state, local records, or HTTP success do not establish emergency delivery.
- Main/release network security forbids cleartext; emulator exceptions belong
  in the debug resource overlay.
- Account deletion clears installation-wide local data. UI copy must explain
  the effect on other account files in this app installation.

## Verification

Run `.\gradlew.bat test compileDebugKotlin lintDebug lintRelease` for Android changes.
For packaging, add `assembleDebug`; release artifacts require the existing
signing validation described in [RELEASE.md](../RELEASE.md).

Tests use JUnit, Robolectric, Mockito 5, Truth, and coroutines-test.
Preserve the internal SharedPreferences constructor in SecurePreferences so
unit tests do not require Android Keystore. Use current Robolectric service
controller APIs, including `withIntent(...).startCommand(0, 0)` when applicable.
Update coverage when implementations change rather than keeping tests tied to
removed APIs or deleting them.

Instrumentation under `app/src/androidTest/` is separate from JVM tests.
Report Gradle cache/up-to-date results distinctly from freshly executed tests,
and retain lint warnings in release evidence. Do not infer physical-device
reliability from compilation.
