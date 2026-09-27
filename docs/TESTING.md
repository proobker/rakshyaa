# Testing and verification

Run commands from the component directory stated below. In an environment that
requires RTK, prefix shell commands with the configured RTK wrapper.

These checks cover different layers. Compilation, mocked API tests, cached JVM
results, and device acceptance should be reported separately.

## Android

From `rakshyaa/`:

```powershell
.\gradlew.bat test
.\gradlew.bat compileDebugKotlin
.\gradlew.bat lintDebug lintRelease
.\gradlew.bat assembleDebug
```

Use `./gradlew` on POSIX. JVM tests use JUnit, Robolectric, Mockito 5, Truth, and
coroutines-test. Suites cover authentication/local sessions, Google credentials,
login restore orchestration, secure preferences, sync guards, locations, safe
places, fake calls, contacts, legal helpers, SOS state/service behavior, and
emergency intent construction. Test counts are results, not a permanent contract.

Reports are generated under `app/build/reports/tests/`,
`app/build/test-results/`, and `app/build/reports/lint-results-<variant>.*`.
If Gradle reports `UP-TO-DATE`, say that it reused previous results. For a change
requiring fresh execution, invalidate the relevant task or use its rerun option;
avoid unnecessarily rebuilding everything for Markdown edits.

Instrumentation requires a device/emulator:

```powershell
.\gradlew.bat connectedDebugAndroidTest
```

The current instrumentation source includes a Compose SOS-countdown regression
checking that the countdown replaces the activation control and can be cancelled.
This is separate from `test` and does not establish emergency delivery.

## Worker

From `backend/`:

```powershell
npm run typecheck
npm test
npx wrangler deploy --dry-run
```

The Cloudflare Vitest plugin runs the Worker with local D1/test bindings. Setup
applies migrations and mocks external calls using MSW. Current Worker cases cover:

- Public health and authentication required for incident creation.
- Incident creation/replay, owner resolution, and cross-user resolution rejection.
- Invalid incident latitude.
- Authenticated ciphertext upload/download, metadata, and cross-user media isolation.

Google exchange, account deletion/re-registration, all admin-key paths, profile
semantics, multipart failure cleanup, and live provider integration need additional
acceptance or test coverage. The archived `legacy/tests/incidents.cjs` is an Express/Node
regression, not the Worker suite invoked by `npm test`.

A dry-run validates packaging and bindings without publishing. It does not prove
remote D1 schema or provider credentials.

## Admin

From `admin/`:

```powershell
npm run typecheck
npm run build
```

For a TypeScript-only check without updating incremental metadata, use
`npx tsc --noEmit --incremental false`. There is no package test/lint script.

Manual portal checks: correct/wrong key, empty response, null location, long IDs,
failed refresh retaining stale rows, disconnect during an in-flight request,
reload losing the key, keyboard access, narrow layout, and HTTPS enforcement.
Confirm no operator secret is compiled into public environment variables.

## Manual Android acceptance

Use a disposable account and consenting test contacts; emergency calls or sent
messages are not required for the scenarios below.

| Area | Scenarios |
| --- | --- |
| Local session | Fresh launch without network/Google, reopen, sign out/in, retained account files |
| Cloud session | Google success/cancel/no account, expired token, unreachable backend, audience/certificate mismatch |
| SOS | Countdown cancellation, repeated taps, foreground-start failure, activation/deactivation race, service stop |
| External actions | Inspect SMS recipients/location, cancel composition, empty dialer, absent handler apps |
| Location/rides | Denied/coarse/fine/background permissions, GPS unavailable, screen lock, stop/process restart |
| Check-ins | Cold service startup, completion before timer, cancel, repeated schedules, process death/reboot |
| Backup | Disabled uploads, offline save, stale remote copy, lost-key rejection, account switch mid-download |
| Video | Camera/mic denial, capture failure, local playback, failed upload, deletion and remote-media state |
| Profile | Save and clear text, custom photo/re-sign-in, local-mode photo failure, preference behavior |
| Deletion | Cloud failure, success/re-registration, local-only deletion, installation-wide data loss |
| Places/resources | No location, upstream failure, sample fallback labeling, real-world relevance |
| Accessibility | TalkBack, keyboard/focus, large fonts, dark mode, narrow screens, system insets |

Known failures/limitations should be tracked from [KNOWN_LIMITATIONS.md](KNOWN_LIMITATIONS.md),
not hidden by a green compile. Record the device/API level, app/backend revisions,
build mode, and actual outcome. Production OAuth, live Cloudinary, physical-device
foreground behavior, and native alignment/runtime checks remain distinct from
unit tests.

For the latest audit results, see [release readiness](RELEASE_READINESS.md).
