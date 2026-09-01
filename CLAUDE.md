# CLAUDE.md

Guidance for AI coding agents (Claude Code, opencode, etc.) working in this repository.

## Project Overview

**Rakshyaa** is a women's safety Android application. The backend is a self-hosted
Node.js + TypeScript (Express) service. The app uses **Google sign-in (Credential
Manager)** whose ID token is verified by our own backend, which then issues its own
session JWT. All sensitive app data is **encrypted on-device** (Android Keystore) and
optionally backed up to the backend as **opaque encrypted blobs** — the server never
sees plaintext.

The architecture deliberately does **not** use Supabase. If you see references to
Supabase, `io.github.jan.supabase`, `io.github.jmnarloch`, or the `SupabaseProvider`
class, they are stale and should be removed.

## Repository Layout

```
rakshyaa/     # Native Android app (Kotlin, Jetpack Compose, Hilt)
backend/      # Node.js + TypeScript + Express + SQLite (own backend)
admin/        # Existing Next.js admin portal (out of scope for APK; reads via API key)
```

## Backend (Node.js + TypeScript + Express)

Location: `backend/`

- Framework: Express 4 + TypeScript (NodeNext modules).
- **SQLite** via the built-in `node:sqlite` module (`DatabaseSync`). The package
  `better-sqlite3` is intentionally NOT used (native build fails on Windows without
  Visual Studio tooling).
- `google-auth-library` verifies Google ID tokens (`verifyIdToken` with audience =
  the configured web client id).
- `jsonwebtoken` signs/verifies session JWTs.
- Schema (`backend/src/db.ts`): `users`, `blobs` (encrypted-blob metadata),
  `incidents`.
- Encrypted media files stored under `backend/data/media/<userId>/`.

### Commands (run inside `backend/`)
| Action | Command |
| --- | --- |
| Install deps | `npm install` |
| Run (dev, watch) | `npm run dev` |
| Typecheck | `npm run typecheck` (`tsc --noEmit`) |
| Build | `npm run build` |
| Run (built) | `npm start` |

Environment: copy `.env.example` → `.env`. Requires `GOOGLE_WEB_CLIENT_ID`,
`JWT_SECRET`, `ADMIN_API_KEY`. `DATA_DIR` and `DB_PATH` have sensible defaults.

### API surface
- `GET /health`
- `POST /auth/google` — body `{ idToken }` → `{ token, user }`
- Auth-protected (`Authorization: Bearer <jwt>`):
  - `GET /backup/me`
  - `GET /backup` — list encrypted blob metadata
  - `PUT /backup/data/:key`, `GET /backup/data/:key`, `DELETE /backup/data/:key`
  - `PUT /backup/media/:id`, `GET /backup/media/:id`
  - `POST /incidents`, `POST /incidents/:id/resolve`
- API-key protected (`x-api-key: <ADMIN_API_KEY>`):
  - `GET /incidents/admin/active`

## Android App (`rakshyaa/`)

- Kotlin 2.0.20, AGP 8.5.2, Jetpack Compose (BOM 2024.09.00), Hilt 2.52.
- `compileSdk` 34, `minSdk` 24.
- `android.nonTransitiveRClass=true` → resource references must be fully-qualified
  (`com.rakshyaa.rakshyaa.R.string.x`) or explicitly imported. Do not write `R.xxx`.
- Build config fields: `BACKEND_BASE_URL`, `GOOGLE_WEB_CLIENT_ID` come from
  `app/build.gradle` buildConfigFields (and a `local.properties`-style properties file).

### Commands (run inside `rakshyaa/`)
| Action | Command |
| --- | --- |
| Build debug APK | `./gradlew assembleDebug` |
| Output | `app/build/outputs/apk/debug/app-debug.apk` |
| Tests | `./gradlew test` |
| Lint | `./gradlew lint` |

### Key packages
- `data/auth/` — Google sign-in (Credential Manager) + backend token exchange.
- `data/network/` — OkHttp REST client wired to the backend.
- `data/local/` — `SecurePreferences` (EncryptedSharedPreferences) and encrypted
  file/datastore helpers.
- `utils/CryptoManager` — AES-GCM keyed from Android Keystore.
- `data/repositories/` — per-feature repositories (contacts, rides, check-ins, ...).
- `services/` — foreground services (SOS, location, ride monitoring, check-in, fake call).
- `viewmodels/`, `ui/` — Compose viewmodels, screens, navigation, theme.

### Google OAuth notes
- The app uses Credential Manager + `GetGoogleIdOption` with a **server client id**
  (web client id) to obtain a Google ID token, which is sent to the backend
  (`POST /auth/google`). The backend verifies it and returns a session JWT.
- For this to work on a device, the Android package name (`com.rakshyaa.rakshyaa`)
  **and its SHA-1 fingerprint** must be registered in Google Cloud Console against the
  OAuth client. The `GOOGLE_WEB_CLIENT_ID` and `BACKEND_BASE_URL` build-config fields
  must point at valid values.

## Security model
- All sensitive data (emergency contacts, incident logs, videos) is encrypted on the
  device with AES-GCM, keys held in Android Keystore.
- Backend backup endpoints store only opaque encrypted blobs; the server does not
  have the keys and treats payloads as bytes.
- Backend verifies the Google ID token itself; clients never trust a token without
  backend confirmation.

## General rules for agents
- Do not introduce Supabase dependencies.
- Do not reintroduce `better-sqlite3`; use `node:sqlite`.
- Follow existing code conventions (mimic nearby files).
- After editing Android code, run `./gradlew compileDebugKotlin` or `assembleDebug`.
- After editing backend code, run `npm run typecheck`.
