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
admin/        # Next.js admin portal (reads backend incidents via API key)
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
  - `GET /user/profile` — current user's profile
  - `PUT /user/profile` — update profile (`{ name?, phone?, bio?, picture? }`)
  - `GET /backup/me` — current user's profile + blob metadata
  - `GET /backup` — list encrypted blob metadata
  - `PUT /backup/data/:key`, `GET /backup/data/:key`, `DELETE /backup/data/:key`
  - `PUT /backup/media/:id`, `GET /backup/media/:id`
  - `POST /incidents`, `POST /incidents/:id/resolve`
  - `GET /places/nearby` — proxy Overpass/OpenStreetMap for nearby safe places
- API-key protected (`x-api-key: <ADMIN_API_KEY>`):
  - `GET /incidents/admin/active`

## Android App (`rakshyaa/`)

- Kotlin 2.0.20, AGP 8.5.2, Jetpack Compose (BOM 2024.08.00), Hilt 2.52.
- `compileSdk` 34, `minSdk` 24.
- `android.nonTransitiveRClass=true` → resource references must be fully-qualified
  (`com.rakshyaa.rakshyaa.R.string.x`) or explicitly imported. Do not write `R.xxx`.
- Build config fields: `BACKEND_BASE_URL`, `GOOGLE_WEB_CLIENT_ID` come from
  `rakshyaa/backend.properties` (root project file, read by `app/build.gradle`).

### Commands (run inside `rakshyaa/`)
| Action | Command |
| --- | --- |
| Build debug APK | `./gradlew assembleDebug` |
| Output | `app/build/outputs/apk/debug/app-debug.apk` |
| Tests | `./gradlew test` (47 unit tests — required before declaring work complete) |
| Lint | `./gradlew lint` |
| Fast type-check | `./gradlew compileDebugKotlin` |

### Unit tests (rewritten Sep 2026)
- Stack: **Robolectric** (4.13) + **Mockito inline** (5.x — required to mock Kotlin `final`
  classes; 4.x cannot) + **Truth** + `kotlinx-coroutines-test` (UnconfinedTestDispatcher + `setMain`).
- Existing suites: `FakeCallServiceUnitTest`, `AuthViewModelTest`, `LegalHelpServiceUnitTest`,
  `EmergencyContactsServiceUnitTest`, `LocationRepositoryUnitTest`, `SOSActivationServiceUnitTest`,
  `SecurePreferencesUnitTest`.
- **Do not delete tests for a removed feature without replacing them with current-API tests** —
  the suite must always compile and pass (`./gradlew test`).
- `SecurePreferences` keeps an `internal constructor(prefs: SharedPreferences)` test seam so
  Robolectric never touches the Android Keystore ("AndroidKeyStore not found" under Robolectric).
- `SOSActivationServiceUnitTest`: use `withIntent(...).startCommand(0, 0)` (not `startCommand(intent,0,0)`)
  and `org.robolectric.Shadows.shadowOf(...)`, not `as ShadowNotificationManager`.

### Key packages
- `data/auth/` — Google sign-in (Credential Manager) + backend token exchange.
- `data/network/` — OkHttp REST client wired to the backend.
- `data/local/` — `SecurePreferences` (EncryptedSharedPreferences) and encrypted
  file/datastore helpers.
- `utils/CryptoManager` — AES-GCM keyed from Android Keystore.
- `data/repositories/` — per-feature repositories (contacts, rides, check-ins, ...).
- `data/sync/AppDataSync.kt` — **restore-on-login**: pulls all remote encrypted blobs +
  profile from the backend when a session starts.
- `services/` — foreground services (SOS, location, ride monitoring, check-in, fake call).
- `viewmodels/`, `ui/` — Compose viewmodels, screens, navigation, theme.

### Restore-on-login (applies to auth changes)
- `AppDataSync.restoreAll()` is **triggered from `AuthViewModel`** (init: collect
  `isLoggedIn` with `distinctUntilChanged`, call `restoreAll()` on transition to logged-in).
- **`AuthRepository` must NOT depend on `AppDataSync`** — that creates a Hilt cycle
  `AuthRepository → AppDataSync → ProfileRepository → AuthRepository`. Any new wiring that
  needs restore-on-login must go through `AuthViewModel`, not `AuthRepository`.

### Service architecture (decided)
- **Manifest-registered services** (4): `@AndroidEntryPoint` + field injection (`@Inject lateinit var`)
  — SOSActivationService, LocationTrackingService, RideMonitoringService, CheckInService.
- **Helper services** (6): plain `@Singleton` with constructor injection via `javax.inject.*`
  — VideoEncryptionService, EmergencyContactsService, FakeCallService, LegalHelpService, SafePlacesService, GeocodingService.
- Do NOT add new manifest services without the `@AndroidEntryPoint` pattern.
- Do NOT use `hiltService` or `SupabaseProvider` — both are stale.

### Google OAuth notes
- The app uses Credential Manager + `GetGoogleIdOption` with a **server client id**
  (web client id) to obtain a Google ID token, which is sent to the backend
  (`POST /auth/google`). The backend verifies it and returns a session JWT.
- For this to work on a device, the Android package name (`com.rakshyaa.rakshyaa`)
  **and its SHA-1 fingerprint** must be registered in Google Cloud Console against the
  OAuth client. The `GOOGLE_WEB_CLIENT_ID` and `BACKEND_BASE_URL` build-config fields
  must point at valid values.
- **SHA-1 for debug builds**: `0F:2E:8A:D0:82:3D:7D:A5:C8:BF:15:0E:5A:2B:BA:FB:9F:E5:AE:01`.
- Android OAuth client (type "Android") exists in Cloud Console only for package+SHA-1 mapping.
- Backend `.env` must contain the **same** `GOOGLE_WEB_CLIENT_ID` (Web client ID).

### Backend profile contracts (user-edited data)
- `GET /user/profile` and `PUT /user/profile` (`{ name?, phone?, bio?, picture? }`) are
  auth-protected routes in `backend/src/routes/user.ts`.
- The `users` table has `phone` and `bio` columns (auto-migrated); `users.picture` is the
  profile photo ref. **Google (re)sign-in never overwrites `phone`/`bio`** — user-edited
  data survives logout/login.
- Picture refs are `media:<id>` (encrypted upload); Coil loads them via an image loader that
  handles the `media:` scheme.

### Network security
- Dev builds allow cleartext `http://10.0.2.2` and `http://localhost` via
  `rakshyaa/app/src/main/res/xml/network_security_config.xml` (referenced in manifest).
