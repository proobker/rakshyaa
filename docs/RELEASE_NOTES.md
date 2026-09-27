# Release notes

## Repository organization - 2026-09-27

- Android, the active Cloudflare Worker, and the Next.js portal share the public
  repository and its existing Git history.
- Guides, API documentation, and release records are consolidated under `docs/`.
- The inactive Express/Docker implementation is isolated in `backend/legacy/`.
- Supplied branding artwork is organized under `assets/`.
- A portable VS Code workspace opens one Source Control root.
- Private configuration, local databases, signing files, logs, and build output
  are excluded from publication. Previously tracked generated reports/logs are
  removed from the current tree.

This publishes and organizes source; it does not deploy services or distribute
a new signed app. Validation is recorded in [release readiness](RELEASE_READINESS.md).

## Android 1.1 / version code 2 - current source

These notes describe the checkout reviewed on 2026-09-26. They do not assert that
this version is published or every feature has passed device acceptance.
Backend package version 2.0.0 and admin package version 1.0.0 are independent.

### Application

- Default local-device sessions without a Google account or hosted backend.
- Optional Google sign-in through Credential Manager and the Worker token exchange.
- Compose dashboard, bottom navigation, light/dark theme, and prominent SOS entry.
- Cancellable SOS countdown, foreground runtime state, and explicit SMS/dialer actions.
- Account-scoped encrypted contacts, location logs, rides, check-ins, saved places,
  legal notes, profile cache, and video records.
- CameraX recording and encrypted local media, with optional cloud upload.
- Fake-call simulation, profile editing, sign-out, and account/data deletion.
- Build targets Android API 36 with AGP 8.10.1, Gradle 8.11.1, and CameraX 1.4.2.
- Release signing validation and HTTPS-only main/release network configuration.

### Cloud and operator portal

- Active Hono Worker backend replaces the old Express deployment path.
- D1 holds users, incidents, and backup metadata; Cloudinary holds authenticated
  raw ciphertext chunks.
- Google verification and account-generation-bound session JWTs use jose.
- User-scoped data/media backup, profile updates, and confirmed account deletion.
- Incident UUID replay and ownership checks; manual operator view of active incidents.
- Overpass-backed nearby-place lookup.
- Next.js portal keeps its shared API key in memory and identifies stale responses.

### Limits to communicate

SOS does not automatically call or text contacts. Cloud incident upload is
best-effort and does not confirm operator response. Check-in scheduling/contact
notification and ride deviation have implementation gaps. Sample places/legal
resources are not a verified location-aware directory.

Encrypted backups depend on the original installation's keys. Profile and
incident fields are server-readable. Media sync/deletion and profile preference
behavior have remaining gaps. Details: [known limitations](KNOWN_LIMITATIONS.md).

### Documentation reconciliation - 2026-09-26

Setup now reflects local mode, SDK 36, Wrangler development, D1 migrations, actual
release signing inputs, and the current API response wrappers. Architecture,
security, testing, and limitations references replace obsolete blanket “all
features verified” and “server never sees plaintext” claims. Only Markdown files
were edited for this reconciliation.

Verification details are in [release readiness](RELEASE_READINESS.md).

## Historical context

Earlier September rebuild notes describe an Express/SQLite backend and Google-only
onboarding. Those details are superseded. The
[rebuild milestone](archive/REBUILD_MILESTONE.md) retains the historical
context without treating old commands or test counts as current instructions.
