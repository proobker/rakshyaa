# Local Setup Instructions (Current Stack)

## Prerequisites

- **Android Studio** (AGP 8.5.2, Android SDK 34) or CLI tools
- **JDK 21** (or Java 17+)
- **Node.js 22+**
- **Google Cloud Console** project with:
  - OAuth **Web** client (copy Client ID)
  - OAuth **Android** client: package `com.rakshyaa.rakshyaa` + SHA-1 `0F:2E:8A:D0:82:3D:7D:A5:C8:BF:15:0E:5A:2B:BA:FB:9F:E5:AE:01`
  - Consent screen: **External**, status **Testing**, add your Google account as **Test user**

## Backend Setup

```bash
cd backend
npm install
cp .env.example .env
```

Edit `.env` with real values:
```env
PORT=8080
GOOGLE_WEB_CLIENT_ID=765590596814-68hll5uflj7b4h9u8r9vlgrgiqvg4amu.apps.googleusercontent.com
JWT_SECRET=<64-char hex, e.g. openssl rand -hex 32>
ADMIN_API_KEY=<random long string>
DATA_DIR=./data
DB_PATH=./data/rakshyaa.db
```

Start backend:
```bash
npm run dev          # http://localhost:8080
# verify: curl http://localhost:8080/health
```

## Android Setup

### 1. Configure build properties

File: `rakshyaa/backend.properties` (root of rakshyaa/):
```properties
BACKEND_BASE_URL=http://10.0.2.2:8080
GOOGLE_WEB_CLIENT_ID=765590596814-68hll5uflj7b4h9u8r9vlgrgiqvg4amu.apps.googleusercontent.com
```

These are baked into `BuildConfig` and used by the app.

### 2. Emulator (recommended)

The project includes an AVD `Medium_Phone_API_36.1` (google_apis_playstore, API 36).

```bash
$env:LOCALAPPDATA\Android\Sdk\emulator\emulator.exe -avd Medium_Phone_API_36.1
```

On first boot:
- Sign in the **same Google account** you added as a Test user on the OAuth consent screen
- Ensure Play Services is up to date

### 3. Build & Install

```bash
cd rakshyaa
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### 4. Run

- Launch **Rakshyaa** on the emulator
- Tap **Sign in with Google** → choose the Test-user account
- Backend `POST /auth/google` returns a session JWT → **Home** screen

## Troubleshooting

| Symptom | Cause | Fix |
|---------|-------|-----|
| `CLEARTEXT communication to 10.0.2.2 not permitted` | Network security policy blocks HTTP | App includes `res/xml/network_security_config.xml` allowing `10.0.2.2` and `localhost` — rebuild after any change |
| `developer console isn't setup properly` (code 28444) | No Android OAuth client registered for package+SHA-1 | Create Android-type client in Cloud Console with package + SHA-1; wait ~5 min for propagation |
| `getCredentialAsync no provider dependencies found` | Play Services out of date / no Google account | Use google_apis_playstore AVD; sign in a Google account; update Play Services in the emulator |
| `Access blocked: not in testers` | Emulator account not a Test user | Add the Google account as a Test user on the OAuth consent screen |
| Backend `verifyIdToken` fails | `GOOGLE_WEB_CLIENT_ID` mismatch | Ensure **same** Web client ID in `rakshyaa/backend.properties` AND `backend/.env` |

## Build Commands

```bash
cd rakshyaa
./gradlew compileDebugKotlin   # fast type-check
./gradlew assembleDebug        # build APK
./gradlew installDebug         # build + install on connected device
./gradlew test                 # unit tests
./gradlew lint                 # lint
```

## Security Notes

- **No Supabase** — the old `SUPABASE_URL`/`SUPABASE_ANON_KEY` setup is removed
- `GOOGLE_WEB_CLIENT_ID` must be the **Web** client ID everywhere (not the Android client ID)
- `backend.properties` and `.env` are gitignored; never commit real secrets
- Debug APK is signed with the debug keystore (SHA-1 `0F:2E:8A:D0:82:3D:7D:A5:C8:BF:15:0E:5A:2B:BA:FB:9F:E5:AE:01`)
- Release builds need a separate keystore + its SHA-1 registered as another Android OAuth client