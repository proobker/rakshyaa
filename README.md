# Rakshyaa - Women's Safety Android Application

A comprehensive women's safety Android app. Sign-in uses **Google (Credential Manager)**,
with the ID token verified by our **own Node.js backend** (no Supabase). All sensitive data
is **encrypted on-device** (Android Keystore, AES-256-GCM) and optionally backed up to the
backend as **opaque encrypted blobs** — the server never sees plaintext.

**Current Version**: 1.1

## Features (verified: Auth + Home screen; other screens pending restore)

- **Authentication (verified)**: Google sign-in via Credential Manager; backend verifies the ID
  token and issues a session JWT.
- **Location Tracking**: Foreground service for continuous GPS tracking.
- **SOS Emergency System**:
  - SOS triggering with a short countdown to prevent false alarms
  - Emergency calling integration
  - Incident reporting with location sharing to the backend
- **Encrypted Video Capture**:
  - Client-side encryption using AES-256-GCM (key in Android Keystore)
  - Front/rear camera capture with CameraX
  - Optional backup of the encrypted blob to the backend
- **Ride Monitoring**:
  - GPS logging with route deviation detection (Haversine formula)
  - Alerts when leaving safe zones
- **Safe Places Discovery**:
  - Nearby hospitals / police / fire stations
  - User-submitted safe places
- **Check-ins System**:
  - Scheduled safety check-ins with grace periods
  - Geofence validation and escalation procedures
- **Emergency Contacts Management**:
  - Encrypted storage of contact information
  - Escalation procedures for missed check-ins
- **Fake Call Feature**: Realistic incoming call simulation for escape scenarios.
- **Legal Help Section**: Offline access to legal resources and support information.

## Repository Layout

```
rakshyaa/     # Native Android app (Kotlin, Jetpack Compose, Hilt)
backend/      # Own Node.js + TypeScript + Express + SQLite backend
admin/        # Existing Next.js admin portal (reads backend via API key)
```

## Backend

The backend lives in `backend/`. It verifies Google ID tokens, issues session JWTs,
and stores encrypted-blob backups in SQLite (`node:sqlite`) + disk files.

Setup:

```bash
cd backend
npm install
cp .env.example .env   # fill in GOOGLE_WEB_CLIENT_ID, JWT_SECRET, ADMIN_API_KEY
npm run dev            # http://localhost:8080  (GET /health to check)
```

See [`backend/README.md`](../backend/README.md) and `backend/src` for the API surface and schema.

## Development Setup (Android)

### Prerequisites
- Android Studio (or equivalent) and an Android SDK with platform 34 / build-tools.
- Java 17+ (JDK 21 works).
- Node.js 22+ (for backend and admin).

### Build the app

```bash
cd rakshyaa
./gradlew assembleDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk`

Other useful commands:

```bash
./gradlew test              # unit tests
./gradlew lint              # lint
./gradlew installDebug      # install on a connected device/emulator
```

### Google sign-in configuration (required)

Rakshyaa obtains a Google ID token and sends it to the backend for verification. The sign-in flow
is now **verified end-to-end** on the `Medium_Phone_API_36.1` (google_apis_playstore, API 36)
emulator with a Google Test-user account.

1. In **Google Cloud Console**, create an OAuth 2.0 **Web** client and copy its **Client ID**
   (used as the "server client id").
2. Create a second OAuth client → **Android** → package `com.rakshyaa.rakshyaa` + SHA-1
   `0F:2E:8A:D0:82:3D:7D:A5:C8:BF:15:0E:5A:2B:BA:FB:9F:E5:AE:01`.
3. On the **OAuth consent screen**, set status **Testing** and add your Google account as a
   **Test user**.
4. Wire the **same Web client ID** in **both** places:
   - `backend/.env` → `GOOGLE_WEB_CLIENT_ID=765590596814-68hll5uflj7b4h9u8r9vlgrgiqvg4amu.apps.googleusercontent.com`
   - `rakshyaa/backend.properties` (root of rakshyaa/) → `GOOGLE_WEB_CLIENT_ID=...` (same value)
5. Point `BACKEND_BASE_URL` at your running backend
   (e.g. `http://10.0.2.2:8080` from the emulator; set in `rakshyaa/backend.properties`).
6. **Dev-only cleartext**: the app includes `res/xml/network_security_config.xml` allowing
   `http://10.0.2.2` and `http://localhost` for the emulator.

The app currently builds with the Web client ID baked into `BuildConfig.GOOGLE_WEB_CLIENT_ID`
and the backend verifies the same audience. The Android client (type "Android") exists only in
the console to map package+SHA-1; its client ID is **not** used in code.

## Architecture

1. **Android App (Client)** — Google sign-in, foreground services for location/SOS/ride
   monitoring/check-ins, CameraX video capture, encrypted local storage via Android Keystore,
   and encrypted-blob backup to the backend.
2. **Backend (self-hosted)** — verifies Google ID tokens, issues session JWTs, and stores
   opaque encrypted backup blobs (SQLite metadata + disk files). It never holds or reads
   plaintext.
3. **Admin portal** — web interface that reads backend incident data via an API key.

## Security Model

- All sensitive data (contacts, incident logs, videos) is encrypted on-device with
  AES-256-GCM; keys live only in the Android Keystore.
- The backend receives only **encrypted** blobs — it has no decryption keys.
- The Google ID token is always verified server-side; the client does not trust tokens alone.
- All network communication should be over HTTPS in production.

## Running End-to-End (verified)

1. Start the backend: `cd backend && npm run dev`.
2. Set `BACKEND_BASE_URL=http://10.0.2.2:8080` and the Web client ID in
   `rakshyaa/backend.properties`.
3. Set the same Web client ID + `JWT_SECRET` in `backend/.env`.
3. Build & install: `cd rakshyaa && ./gradlew assembleDebug && adb install -r app-debug.apk`.
4. On the emulator (`Medium_Phone_API_36.1`, google_apis_playstore), sign in the same Google
   account added as a Test user on the OAuth consent screen.
5. Launch the app → **Sign in with Google** → pick the Test-user account → backend returns a
   session JWT → **Home** screen appears.

The APK builds with `compileSdk 34`, `minSdk 24`, `targetSdk 34`, version `1.1`.
Output: `app/build/outputs/apk/debug/app-debug.apk`.

## License

MIT — see the LICENSE file for details.
