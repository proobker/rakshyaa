# Rakshyaa - Women's Safety Android Application

A comprehensive women's safety Android app. Sign-in uses **Google (Credential Manager)**,
with the ID token verified by our **own Node.js backend** (no Supabase). All sensitive data
is **encrypted on-device** (Android Keystore, AES-256-GCM) and optionally backed up to the
backend as **opaque encrypted blobs** — the server never sees plaintext.

**Current Version**: 1.1

## Features

- **Authentication**: Google sign-in via Credential Manager; backend verifies the ID
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

See [`backend/README`](#) and `backend/src` for the API surface and schema.

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

### Google sign-in configuration (required, on your side)
Rakshyaa obtains a Google ID token and sends it to the backend for verification. For the
sign-in to return a token on a real device:

1. In **Google Cloud Console**, create an OAuth 2.0 **Web** client and copy its **Client ID**
   (used as the "server client id").
2. Register the Android package name `com.rakshyaa.rakshyaa` **and its SHA-1 fingerprint**
   against that client (for Credential Manager on Android).
3. Set `GOOGLE_WEB_CLIENT_ID` in `backend/.env` (server-side verification audience) and in
   the Android build config.
4. Point `BACKEND_BASE_URL` at your running backend (e.g. `http://10.0.2.2:8080` from an
   emulator).

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

## Running End-to-End

1. Start the backend: `cd backend && npm run dev`.
2. Set `BACKEND_BASE_URL` and a valid `GOOGLE_WEB_CLIENT_ID` in the Android build config.
3. Build & install the app: `cd rakshyaa && ./gradlew installDebug`.
4. Sign in with Google, set up a profile, and use the safety features.

## License

MIT — see the LICENSE file for details.
