# Troubleshooting

Start with the exact command, component directory, expected result, and redacted
error. Record the app/backend version and whether the problem is local or
deployed. Keep tokens, operator keys, signing passwords, and personal records
out of shared reports.

## GitHub publication

| Symptom | Check / next step |
| --- | --- |
| Backend/admin absent after pushing | Update to the monorepo revision and confirm the pushed branch includes `backend/` and `admin/` |
| `not a git repository` at workspace root | Clone the complete repository; its outer directory should contain `.git` |
| Android appears as a link instead of files | Look for mode `160000` in staged entries; the maintained repository uses normal Android files, not a nested repository |
| A secret file appears staged | Stop publication; remove it from the index, fix ignore rules, then inspect the staged diff again |
| Secret was already public | Rotate/revoke it and address history; a new ignore rule is not enough |
| `private: true` in backend package | This blocks npm publication, not public GitHub source hosting |

## Backend

| Symptom | Check / next step |
| --- | --- |
| Unsupported Node engine | Use Node compatible with the lockfile; reviewed Wrangler requires >=22 |
| `npm start` / backend `npm run build` missing | Use `npm run dev` or `npm run deploy`; Docker/Express files are historical |
| D1 not found / binding missing | Verify account, database ID, and binding name `DB` |
| Missing table / `session_version` column | Apply migrations to the intended local/remote database; an already-applied historical migration needs a new incremental migration |
| Local works, deployment fails auth/storage | Local `.dev.vars` does not populate deployed secrets; configure the intended Worker |
| `/health` works but features fail | Health verifies the route only; test D1, Google audience and Cloudinary credentials independently |
| Google exchange returns 401 | Match Web client IDs, correct ID token, issuer/expiry, device clock and Google setup; inspect backend errors without logging tokens |
| Protected route returns 401 | Session expired, JWT secret rotated, or account generation changed; sign in again |
| Admin endpoint returns 403 | Confirm the entered key matches that Worker's `ADMIN_API_KEY`; bearer auth is not a substitute |
| Upload returns 415 / 413 | Use `application/octet-stream`; encrypted payload must fit the 50 MiB limit |
| Backup returns 404 / 502 | Check account/key ownership and recorded Cloudinary parts/access; preserve working local data |
| Nearby places fail | Check Worker/Overpass connectivity; bundled fallback entries are sample data |
| Deployment targets another owner's domain | Replace Wrangler routes and database ID before remote commands |

## Android signing and cloud access

| Symptom | Check / next step |
| --- | --- |
| Release requires a signing property | Supply all four `RAKSHYAA_*` inputs; check environment overrides |
| Keystore file does not exist | Relative path is from `rakshyaa/`; use forward slashes in Java property paths |
| Incorrect keystore password / alias | Verify with interactive `keytool -list`; do not create a replacement key to hide the error |
| HTTPS configuration validation fails | Set a production HTTPS origin when `ENABLE_CLOUD=true`; local signed builds can keep cloud disabled |
| Google sign-in fails only in release | Register the actual installed signing certificate and package; Play app-signing and upload keys may differ |
| Only local login appears | Check the build's `ENABLE_CLOUD` value and rebuild |
| Emulator cannot reach localhost | Use `10.0.2.2:8787`; physical USB debugging can use ADB reverse and localhost |
| Cleartext request rejected | Use allowed debug hosts or deploy HTTPS for release |
| APK cannot update installed app | Compare signing identity/version; test on a separate device rather than uninstalling valuable local data |
| AAB will not install with ADB | Use the signed APK for direct installation; AAB is a store input |
| Backup fails after reinstall | Installation-bound keys may be lost; cross-installation recovery is not implemented |

## Portal and incident behavior

| Symptom | Check / next step |
| --- | --- |
| Requests still go to port 8080 | Explicitly set `NEXT_PUBLIC_BACKEND_URL`, restart dev or rebuild production |
| Hosted UI refuses HTTP backend | Use HTTPS; HTTP is limited to loopback portal/backend combinations |
| Rows appear old | Refresh is manual; a failure leaves stale rows with a warning |
| Key disappears after reload | Expected: it is held in memory only |
| SOS exists locally but no admin row | Cloud incident upload is best-effort and has no durable retry queue |
| Location does not move in admin | Cloud record contains initial coordinates, not a live feed |
| More than 50 incidents not shown | Current endpoint returns the latest 50 active incidents without pagination |

Use [testing](TESTING.md) to reproduce integration behavior and
[known limitations](KNOWN_LIMITATIONS.md) before treating a result as a
new regression. Do not clear app data as a generic troubleshooting step: it can
erase the only usable encryption keys.
