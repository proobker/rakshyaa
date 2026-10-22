# Release readiness

## Launch review - 2026-10-09

Source revision `5240d7a`. Status: **not ready for public launch.**

| Check | Result and scope |
| --- | --- |
| Backend `npm run typecheck` | Passed |
| Backend `npm test` | Passed: 5 Worker tests; external storage mocked |
| Backend `npx wrangler deploy --dry-run` | Passed; nothing deployed. `SUPPORT_EMAIL` is empty |
| Admin `npm run typecheck`, `npm run build` | Passed |
| Android `test compileDebugKotlin lintDebug lintRelease` | Passed; Gradle reused up-to-date results: 55 release unit tests, 0 failures; lint 0 errors, 110 warnings |
| Production `GET /health` | HTTP 200 from the configured cloud `BACKEND_BASE_URL` |
| Existing `app-release.apk` | v2 signature verifies; `zipalign -c -P 16 4` passes; built 2026-09-26 |

### Rebuilt artifacts - 2026-10-09

The 2026-09-26 artifacts predated `5240d7a`. They were rebuilt from `5240d7a`
with `assembleDebug assembleRelease bundleRelease`. Version 1.1 / code 2,
`CLOUD_ENABLED=true`, build tools 36.1.0.

| Artifact | SHA-256 |
| --- | --- |
| `app-release.apk` | `b887bfb348572c2420d2a0515abd9661237144722502cc87e63a5d932231a89e` |
| `app-release.aab` | `fd3d052b2779ef8b6ef18b08df685937dc812ebe5ef962eb4edba2e769784385` |

- APK: `apksigner` verifies (v2), signer `CN=Rakshyaa Upload`, certificate SHA-256
  `e85286e4044bc18b53792d515d8b9dbd13c15c89a12b3df27160bcfe2d437ef0`.
- APK: `zipalign -c -P 16 4` passes. The arm64-v8a and x86_64 CameraX native
  libraries have `2**14` (16 KB) LOAD segment alignment.
- AAB: `jarsigner -verify` reports "jar verified".
- The rebuilt APK has not been installed or run on a device.

Blockers:

1. If version code 2 was already distributed, increment `versionCode`
   before shipping these fixes as an update.
2. Acceptance items 2–5 below have no recorded results: physical devices,
   restore and deletion, accessibility, and real OAuth/D1/Cloudinary.
3. `SUPPORT_EMAIL` is empty in `backend/wrangler.jsonc`. A deployed dashboard
   value was not checked. Public privacy, support, and retention materials are
   not in place.
4. Check-in timers do not survive process death or reboot. Release copy must
   not present check-in as a reliable safety alert.

## Implementation update - 2026-09-27

The current source includes bounded safety fixes: scheduled check-ins promote
the service to the foreground before starting their timer, completion and
cancellation persist repository state, and missed-check-in copy now describes
the user action required to contact someone. Cloud incident create/resolve
writes retry three times with short backoff while preserving local success when
the network remains unavailable. The repository, Android, backend, and admin
package declarations now use the root MIT license.

These changes reduce known failure modes but do not close the production gate.
Physical-device recovery, signed artifact verification, real Google OAuth,
Cloudinary and D1 acceptance, accessibility review, and durable reconciliation
for incidents still require evidence before public release.

## Repository publication checks - 2026-09-27

The source was organized into one repository containing Android, backend, admin,
assets, and a central `docs/` hub. The inactive backend is in `backend/legacy/`.
The Android Git history is retained, and generated Kotlin logs/build reports
are removed from the current tracked tree while local outputs are preserved.

| Check | Result and scope |
| --- | --- |
| Backend `npm run typecheck` | Passed |
| Backend `npm test` | Passed: 5 Worker tests; external storage mocked |
| Backend `npx wrangler deploy --dry-run` | Passed; no deployment |
| Admin `npm run typecheck` | Passed |
| Admin `npm run build` | Passed |
| Android `test compileDebugKotlin lintDebug lintRelease` | Passed: 83 actionable tasks, 2 executed and 81 up-to-date |

Android reused compiled, JVM-test, and lint-analysis results; this is not a fresh
physical-device acceptance run. Signing inputs, cloud credentials, local
databases, and generated outputs remain local. No signed APK/AAB or live backend
deployment was produced by this source-publication change. The earlier audit
results and remaining release constraints below are retained with their dates.

## Documentation audit - 2026-09-26

Documentation/source audit: **2026-09-26**. The current checkout provides local
and optional cloud modes, but this review does not certify a public release.
Android version is 1.1 / code 2. Ignored properties determine the actual cloud
configuration of a local artifact.

## Checks performed during this documentation audit

| Check | Result and scope |
| --- | --- |
| Backend `npm run typecheck` | Passed |
| Backend `npm test` | Passed: 5 Worker tests in 1 file |
| Backend `npx wrangler deploy --dry-run` | Passed; nothing deployed |
| Admin `npx tsc --noEmit --incremental false` | Passed |
| Admin `npm run build` | Passed; Next.js production build |
| Android `test compileDebugKotlin lintDebug lintRelease` | Passed |
| Android JVM test reports | Gradle reused up-to-date results: 55 tests per variant, zero failures/errors/skips |
| Android lint | Fresh analysis: zero errors, 110 warnings and 7 hints per variant |

The existing unit results were inspected; the documentation change did not force
all JVM tests to rerun. No signed APK/AAB was rebuilt or revalidated during this
audit. No production deployment, database mutation, Google login, real Cloudinary
operation, emulator interaction, or physical-device acceptance was performed.

The editing scope was Markdown only. Source/configuration contents were compared
with their pre-edit hashes, preserving pre-existing code changes. Build/test
reports and caches are generated verification output.

## Current release constraints

- The default configuration is local mode. A local session needs no hosted backend,
  but maps/geocoding and external apps can still use network services.
- SMS preparation and dialer launch require user confirmation/action. There is no
  automatic call, guaranteed contact delivery, or operator acknowledgement.
- Check-in lifecycle/completion, ride-deviation semantics, incident retries,
  media sync/deletion, and profile preferences have
  [documented implementation gaps](KNOWN_LIMITATIONS.md).
- Device-bound backups do not recover after installation-key loss.
- The backend receives plaintext profile and incident fields in addition to
  opaque encrypted backup payloads.
- Cloud deployment needs the intended D1 schema, provider credentials, reachable
  HTTPS endpoint, and correct Google audience/package/certificate configuration.
- Public privacy/support/retention materials and operator procedures are deployment
  work; they are not supplied merely by a successful Worker build.

## Acceptance still required

1. Address or explicitly scope the source gaps in [PLAN.md](PLAN.md); verify
   check-in and emergency-state behavior with targeted regressions.
2. Test the intended signed APK on physical devices: permissions, offline startup,
   screen lock, process death, background restrictions, GPS loss, recording/playback,
   and user-controlled SMS/dialer flows.
3. Validate same-installation restore, account switching, lost-key rejection, backup
   failure, profile behavior, and deletion with disposable data.
4. Check TalkBack, large fonts, dark mode, narrow screens, and system insets across
   every feature and the operator portal.
5. For cloud mode, run real OAuth, D1, Cloudinary, incident create/resolve, and
   deletion acceptance against the intended environment. Record deployment revisions.
6. Build the distribution artifact, verify signature/native and ZIP alignment,
   record its SHA-256 and cloud mode, and back up signing material.
7. Reconcile licensing and verify bundled place/resource content for the intended
   audience before distribution.

Use [testing](TESTING.md) and [release process](RELEASE.md) for the workflow.
No store submission or publication occurred in this audit.

## Historical evidence

Earlier notes reported signed builds, emulator smoke tests, alignment checks,
and a DNS failure for the chosen custom domain. They are retained as historical
context in the [rebuild milestone](archive/REBUILD_MILESTONE.md).
They do not establish the status of today's deployment, a new artifact, or the
unverified acceptance items above.
