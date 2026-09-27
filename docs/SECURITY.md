# Security and data handling

This is an implementation reference, not a published privacy policy or a security
certification. Source reviewed: 2026-09-26.

## What is encrypted

| Data | Storage / visibility |
| --- | --- |
| Local serialized records and profile cache | AES-256-GCM via `CryptoManager`, Base64-encoded IV plus ciphertext/tag |
| Local authentication preferences | Android EncryptedSharedPreferences and MasterKey |
| Saved video and uploaded avatar bytes | AES-256-GCM via VideoEncryptionService, binary IV plus ciphertext/tag |
| Cloud backup payloads | Opaque ciphertext stored in authenticated Cloudinary raw objects |
| D1 profiles | Server-readable subject, email, name, picture reference, phone, bio |
| D1 incidents | Server-readable owner, status, coordinates, timestamps |
| Backup metadata | Server-readable keys, kinds, sizes, checksums, timestamps, object references |
| Nearby-place requests | Coordinates visible to the Worker and Overpass |

The server has no app decryption keys. That does **not** mean every request or
record is encrypted from the server's perspective. HTTPS protects transport;
the receiving service still reads profile and incident JSON.

Recording and playback use plaintext files in private cache before encryption
or after decryption. The code does not establish a complete cache-cleanup lifecycle
for every success, failure, and sign-out path. Do not describe all local files
as permanently encrypted.

## Keys and account separation

The Keystore aliases `rakshyaa_master` and `rakshyaa_video_encryption_key` belong
to the app installation. Key generation does not require StrongBox or establish
hardware backing on every device.

Private datastore paths are separated by a hash of the account ID, including
the local-device account. This is filesystem separation, not a distinct encryption
key per signed-in account. Legacy unscoped files are not automatically migrated.

Sign-out clears credentials and stops manifest services; it does not erase
saved account files. Reopening the same account on the same installation can
reuse those files and keys. Clearing app data, uninstalling, or losing keys can
make backups permanently unreadable. There is no key export, escrow, recovery
phrase, or cross-device restore protocol. Android system backup is disabled.

## Cloud authentication

The Worker validates Google ID tokens with RS256, the configured Web-client
audience, and Google issuer values. It issues HS256 sessions with an expiry and
account-generation value. Protected routes verify the session and current D1
generation on each request. There is no refresh endpoint or individual-session
revocation API; account deletion invalidates all that account's tokens.

The admin API uses a separate shared `ADMIN_API_KEY` in the request header.
The portal retains it in memory only. Anyone holding that key can query active
incidents; the source does not provide per-operator roles or an acknowledgement log.
CORS middleware currently uses its defaults; CORS is not an authorization boundary.

## Backup behavior

Uploads require `application/octet-stream`, valid object keys, and at most
50 MiB including the encrypted representation. The Worker does not prove the
supplied bytes are encrypted. Android is responsible for encryption.

Cloudinary objects are uploaded as `authenticated`; the Worker fetches them
through signed requests and returns private, non-cacheable download responses.
The stored checksum is SHA-256 of ciphertext. Data restore also authenticates
the ciphertext using AES-GCM before replacing a working local file.

Disabling backup prevents normal uploads. Current restore reads, plaintext
profile requests, incident reporting, and nearby-place requests are separate
paths. It is not a global “no network” or “delete existing backups” control.

## Deletion and retention

`DELETE /user/account` requires the session and
`x-confirm-delete: delete-my-account`. The Worker deletes the account's
Cloudinary prefix first, then deletes its D1 user and cascaded records.
An external cleanup failure prevents the D1 deletion; this is not an atomic
transaction across providers.

Android account deletion then requests `clearApplicationUserData()`, removing
private data for **all accounts on that installation**, including the local
session. If cloud deletion fails, local clearing is not attempted. Deleting in
local mode clears local application data without deleting a cloud account.

The backend also supports deletion of individual data and media blobs. Android's
current video removal deletes its local encrypted file and metadata but does not
call the media-delete API. Existing remote media can remain until separately
deleted or the account is removed.

There is no scheduled retention/purge job in the Worker. Provider logs, caches,
and backups have their own operational retention and are not proven erased by
the application deletion response. Configure and document those policies before
publishing a privacy or retention promise.

## Configuration and transport

- Keep JWT, admin, Cloudinary, and signing secrets out of commits and Markdown.
- Android backend properties are build-time settings; rebuild after changing them.
- Main/release network security forbids cleartext. Debug resources allow the
  configured emulator host `10.0.2.2` and `localhost`.
- Local mode does not make maps, geocoding, Google Maps links, or external SMS
  applications offline. Treat those as separate data recipients.
- Release validation checks HTTPS and a plausible Google Web client ID only
  when cloud is enabled; it does not establish backend reachability.

For remaining implementation gaps, see [known limitations](KNOWN_LIMITATIONS.md).
For deployment configuration, see [backend deployment](BACKEND_DEPLOYMENT.md).
