# Worker API reference

Source: [src/worker/index.ts](../backend/src/worker/index.ts), reviewed 2026-09-26.
Paths are relative to the Worker origin. JSON errors generally use
`{ "error": "message" }`; blob downloads return raw bytes.

## Authentication and timestamps

Public routes require no credentials. User routes require
`Authorization: Bearer <session JWT>`; missing/invalid/expired or
deleted-account sessions return 401. Operator access uses
`x-api-key: <ADMIN_API_KEY>`; a missing or wrong key returns 403.
Putting the key in a query string is not supported.

API record timestamps are Unix **milliseconds**. JWT issued/expiry timestamps
and `JWT_EXPIRY_SECONDS` use seconds. Session tokens are issued only by the Google
exchange; there is no refresh endpoint.

## Public routes

| Method and path | Request | Success |
| --- | --- | --- |
| `GET /` | None | `{ ok: true, service: "rakshyaa-backend", message: "Rakshyaa API is running", health: "/health" }` |
| `GET /health` | None | `{ ok: true, service: "rakshyaa-backend", runtime: "cloudflare-workers", ts }` |
| `POST /auth/google` | JSON `{ idToken: string }` | `{ token, user: { sub, email, name, picture } }` |

Missing/empty/non-string `idToken` returns 400; failed Google exchange returns
401. Google JWT validation checks the configured audience and Google issuers.
Phone and bio survive Google sign-in; name, email, and picture are refreshed
from Google. Token exchange does not return the complete persisted profile.

## Profile and account

| Method and path | Request | Success |
| --- | --- | --- |
| `GET /user/profile` | User session | `{ user: { sub, email, name, picture, phone, bio } }` |
| `PUT /user/profile` | JSON `{ name?, phone?, bio?, picture? }` | Same `{ user }` wrapper |
| `GET /backup/me` | User session | Same profile wrapper; **no blob list** |
| `DELETE /user/account` | User session plus confirmation header below | `{ ok: true }` |

Profile updates accept string values; omitted, null, or non-string fields are
left unchanged. Empty strings are accepted by the Worker. These fields are
server-readable. `picture` can be a URL or an Android media reference.

Account deletion requires:

```http
x-confirm-delete: delete-my-account
```

Missing confirmation returns 400. Cloudinary account objects are removed before
the D1 user and cascaded rows. An upstream cleanup failure returns an error and
does not complete the relational deletion.

## Encrypted backups

| Method and path | Behavior |
| --- | --- |
| `GET /backup` | `{ blobs: [{ key, kind, size, checksum, updatedAt }] }`, newest first |
| `PUT /backup/data/:key` | Save/replace opaque data bytes |
| `GET /backup/data/:key` | Read opaque data bytes |
| `DELETE /backup/data/:key` | Delete data and its external parts |
| `PUT /backup/media/:id` | Save/replace opaque media bytes |
| `GET /backup/media/:id` | Read opaque media bytes |
| `DELETE /backup/media/:id` | Delete media and its external parts |

All operations are scoped to the authenticated user. Media keys appear in the
list/database with a `media:` prefix; path IDs omit that prefix.

PUT requirements and results:

- Content type must be `application/octet-stream` (parameters are tolerated).
- Keys/IDs must match `^[A-Za-z0-9_-][A-Za-z0-9._-]{0,127}$`.
- Maximum request payload is 50 MiB; limits are checked while reading as well as
  against the declared content length.
- Success returns `{ ok: true, key, id, size }`. For data, `id` is also the raw key.
- Wrong content type returns 415; invalid key 400; excessive size 413.

The Worker computes a ciphertext SHA-256 checksum, uploads 8 MiB authenticated
raw chunks, and records their order in D1. It accepts opaque bytes, including
zero-length payloads, without verifying the client's encryption scheme.

GET returns `application/octet-stream`, stored `Content-Length`, and
`Cache-Control: private, no-store`. Missing/other-user blobs return 404.
Failure fetching the first external part returns 502; a later failure terminates
the stream after the response has started.

DELETE returns `{ ok: true }` even when the key is absent. External deletion is
attempted before deleting metadata; cleanup errors can interrupt the operation.

## Incidents

### POST /incidents

Send JSON with optional fields:

```json
{
  "id": "6b610651-7bf6-42f6-bb88-1d6105e1953b",
  "status": "active",
  "latitude": 27.7,
  "longitude": 85.3,
  "activatedAt": 1790380800000
}
```

- Omitted ID generates a UUID. Supplied IDs must match the route's UUID-shaped pattern.
- Coordinates may be omitted/null; supplied values must be finite numbers within
  latitude +/-90 and longitude +/-180.
- `activatedAt` may be omitted/null; supplied values must be non-negative safe
  integers. Default is server time.
- A supplied non-null status must be `active`.
- New incident: 201 with `{ ok: true, id }`.
- Replay of the same user's ID: 200 with the same shape, without updating details
  or reopening a resolved record.
- ID owned by another user: 409. Invalid input: 400.

### POST /incidents/:id/resolve

The session owner can mark their incident resolved. Success returns
`{ ok: true, id }`; a missing or other-user record returns 404.
No operator key can substitute for the owner session.

### GET /incidents/admin/active

Requires only the operator key. Returns:

```json
{
  "incidents": [
    {
      "id": "...",
      "user_id": "...",
      "status": "active",
      "latitude": 27.7,
      "longitude": 85.3,
      "activated_at": 1790380800000,
      "created_at": 1790380800000
    }
  ]
}
```

At most 50 active rows, ordered by `activated_at DESC`. No pagination,
acknowledgement, or live subscription exists.

## Nearby places

`GET /places/nearby?lat=27.7&lon=85.3` requires a user session and returns:

```json
{
  "places": [
    {
      "id": "node-123",
      "name": "Example hospital",
      "address": "Example address",
      "latitude": 27.71,
      "longitude": 85.31,
      "type": "hospital",
      "distanceMeters": 1485
    }
  ]
}
```

The example is illustrative, not a real directory entry. Types are `hospital`,
`clinic`, `police`, and `fire`; doctors map to clinic. The Worker queries
nodes/ways within 50 km, deduplicates, sorts by distance, and returns at most 25.
The Overpass timeout is 25 seconds. There is no radius request parameter.

Missing/non-finite converted coordinates return 400; upstream failures return
502. The current route does not range-check coordinates as incident creation
does. Android applies its own display radius.
