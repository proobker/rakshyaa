# Backend operations and maintenance

Operate the active Worker using [deployment](BACKEND_SELF_HOSTING.md),
[API contracts](API.md), and [security](SECURITY.md). All
Wrangler/npm commands in this guide run from `backend/`.

## Deployment record

For each rollout, record the source commit, lockfile revision, Worker name and
deployment version, account/environment, database name and migration state,
public origin, client versions, timestamp, and operator responsible. Record
secret **names**, never their values. Record checks that were skipped or failed.

Run `npm run typecheck`, `npm test`, and `npx wrangler deploy --dry-run` before
promotion. Review migrations separately before applying them remotely. Verify
Google authentication, storage, and incident flows against the deployed service;
mocked tests and a successful bundle cannot validate credentials.

## Health and monitoring

`GET /health` checks that the route is reachable and returns the Worker runtime
marker. It does not query D1, sign in to Google, or access Cloudinary. Monitor
availability separately from authenticated integration checks.

Review Cloudflare request/error metrics and logs, D1 usage, and Cloudinary storage
and delivery usage. The checked-in observability configuration enables logs
and traces; review retention and access with the operator. Inspect errors such
as `request failed`, `stale Cloudinary part cleanup failed`, and
`Overpass request failed`, while excluding credentials and personal payloads
from shared diagnostic reports. Configure provider alerts/budgets appropriate to
your traffic; this repository does not provision monitoring or retention jobs.

## Database and object recovery

Plan recovery for **both** D1 metadata and Cloudinary objects. A restored metadata
row cannot recreate a deleted object, and a ciphertext object is unusable by
Android without the original installation's key.

Before database changes, capture a recovery point using the provider's supported
backup/recovery facilities. An explicit SQL export can supplement that process:

```powershell
npx wrangler d1 export rakshyaa-db --remote --output C:/secure-backups/rakshyaa-before-change.sql
```

Create the private destination directory first and replace the path as needed.
Exports contain personal data; keep them outside the source repository with
restricted access and retention. Confirm export syntax with the installed CLI
and refer to [D1 commands](https://developers.cloudflare.com/workers/wrangler/commands/d1/).

Test recovery into an isolated environment before relying on it. Document how
Cloudinary objects are retained/recovered under your account settings. Do not
import a dump into production as an unreviewed troubleshooting step. Restoring
old data can also reintroduce deleted accounts or old account-generation values;
review privacy and session invalidation as part of a recovery procedure.

## Schema changes and rollback

The current schema lives in `migrations/0001_initial.sql`. Once a migration has
been applied, add a new migration for subsequent changes. An existing database
that recorded an older initial migration may still lack `session_version`;
check schema rather than assuming the filename proves parity.

A Worker code rollback does not roll back D1 data, schema, or Cloudinary objects.
Choose a code version compatible with the resulting database, or plan a reviewed
forward repair. Cloudinary cleanup and D1 writes are separate operations, so
partial failures can leave storage work to investigate.

## Credential rotation

| Credential | Operational effect |
| --- | --- |
| `ADMIN_API_KEY` | Operators must reconnect with the new key; there is no dual-key/per-operator mechanism |
| `JWT_SECRET` | Existing backend sessions stop verifying; users must sign in again |
| Cloudinary credentials | Upload, download and cleanup depend on the replacement having access to the same objects |
| Google Web client ID | Android and Worker must match; requires coordinated client rebuild/rollout |
| Android signing key | Distribution identity; do not replace it as routine backend maintenance |

Use interactive `npx wrangler secret put NAME` for the intended environment.
These updates may deploy immediately. Treat them as releases with appropriate
testing and operator communication. Do not log the new values.

## Deletion and support

The account-delete endpoint requires a valid session and confirmation header,
cleans up Cloudinary first, then deletes the D1 account and cascaded rows.
External cleanup failure can prevent completion. Android then clears local app
data for **all accounts on that installation**. Provider logs/backups have
separate retention policies; the API response is not proof of erasure from them.

There is no scheduled purge, durable incident delivery queue, or operator
acknowledgement workflow. Publish support/retention information consistent with
these limits. `SUPPORT_EMAIL` alone does not create a support page or email flow.
