# Rakshyaa Rebuild Milestone — Implementation Summary (v1.1)

## Overview
This document summarizes the complete rewrite of the Rakshyaa Android app and backend, completed September 2026. All Supabase references removed; self-hosted backend + Google Credential Manager auth now verified end-to-end.

---

## What Was Rewritten

### Android App (`rakshyaa/`)
| Area | Changes |
|------|---------|
| **Auth** | Google sign-in via Credential Manager (`GetGoogleIdOption` + web client ID). `AuthViewModel`, `GoogleAuthClient`, `AuthRepository` all rewritten. LoginScreen + HomeScreen working. |
| **Services (9 total)** | 4 manifest `@AndroidEntryPoint` foreground services (SOS, Location, Ride, CheckIn) + 5 helper `@Singleton` (VideoEncryption, EmergencyContacts, FakeCall, LegalHelp, SafePlaces). All wired to real repositories. |
| **Repositories** | LocationRepository (encrypted local log), VideoRepository (AES-256-GCM + SyncManager), plus contacts, rides, check-ins, incidents, legal, safe places — all using real APIs. |
| **Models** | `LocationRecord`, `VideoRecord` added. |
| **UI** | `LoginScreen` (Google-only), `HomeScreen` (dashboard + sign-out), `MainActivity` (switches Login/Home on auth state). Removed broken: `SignupScreen`, `ProfileSetupScreen`, `SOSScreen`, `LocationPermissionsHelper`, `VideoCaptureUtil`. |
| **Utils** | `GeoUtils` fixed (no nested companion object; pre-API-33 `Location` ctor). `CryptoManager` / `SecurePreferences` / `EncryptedLocalStore` intact. |
| **Config** | `backend.properties` → `BuildConfig.BACKEND_BASE_URL` + `GOOGLE_WEB_CLIENT_ID`. `network_security_config.xml` for cleartext dev (`10.0.2.2`). |
| **Deps** | Added `androidx.hilt:hilt-navigation-compose:1.2.0` for `hiltViewModel()`. |

### Backend (`backend/`)
| Area | Changes |
|------|---------|
| **Framework** | Express 4 + TypeScript (NodeNext), `node:sqlite` (`DatabaseSync`), no `better-sqlite3`. |
| **Auth** | `google-auth-library` verifies Google ID token (audience = Web client ID); issues session JWT (`jsonwebtoken`). |
| **Endpoints** | `GET /health`, `POST /auth/google`, auth-protected backup/media/incidents, API-key `/incidents/admin/active`. |
| **Storage** | SQLite tables: `users`, `blobs`, `media`, `incidents`. Files on disk under `backend/data/media/<userId>/`. |
| **Security** | Server never decrypts blobs; only verifies ID tokens and issues JWTs. |

---

## Verified End-to-End Flow (Sep 2026)

1. **Emulator**: `Medium_Phone_API_36.1` (google_apis_playstore, API 36).
2. **Google account**: Test user added on OAuth consent screen.
3. **OAuth clients**:
   - Web: `765590596814-68hll5uflj7b4h9u8r9vlgrgiqvg4amu.apps.googleusercontent.com`
   - Android: package `com.rakshyaa.rakshyaa` + SHA-1 `0F:2E:8A:D0:82:3D:7D:A5:C8:BF:15:0E:5A:2B:BA:FB:9F:E5:AE:01`
4. **Config**:
   - `rakshyaa/backend.properties`: `BACKEND_BASE_URL=http://10.0.2.2:8080` + Web client ID
   - `backend/.env`: same Web client ID + `JWT_SECRET`
5. **Result**: App launches → **Sign in with Google** → account chooser → ID token → backend `POST /auth/google` → session JWT → **Home** screen.

---

## Service Architecture (Final)

| Type | Services | DI Pattern |
|------|----------|------------|
| **Manifest-registered** (4) | SOSActivationService, LocationTrackingService, RideMonitoringService, CheckInService | `@AndroidEntryPoint` + `@Inject lateinit var` field injection |
| **Helper** (5) | VideoEncryptionService, EmergencyContactsService, FakeCallService, LegalHelpService, SafePlacesService | Plain `@Singleton` + `javax.inject` constructor injection |

**Rule**: Do NOT add new manifest services without `@AndroidEntryPoint` + field injection. Do NOT use `hiltService` or `SupabaseProvider` (stale).

---

## Key Fixes During Build Iteration

| Issue | Fix |
|-------|-----|
| `removeRange` not on List | `logs.takeLast(500)` |
| `scope.cancel()` unresolved | Added `import kotlinx.coroutines.cancel` |
| `emergencyNumbers` unresolved | Switched to hardcoded `tel:112` in `SOSActivationService.makeEmergencyCall()` |
| `AudioAttributes` overload on `setSound` | Simplified to `.setSound(alarmUri)` |
| `GCMParameterSpec` missing in decrypt | Added import; wrapped IV in `GCMParameterSpec(128, iv)` in both `VideoEncryptionService` decrypt paths |
| `GeoUtils` nested companion object | Moved `EARTH_RADIUS_M` to object body |
| `Location("", lat, lon, 0f)` pre-API-33 | Replaced with `Location("")` + `setLatitude`/`setLongitude` |
| `GOOGLE_WEB_CLIENT_ID` mismatch | Fixed: Web client ID in both `backend.properties` and `.env` (was Android client ID in app) |
| Cleartext HTTP blocked | Added `network_security_config.xml` allowing `10.0.2.2` + `localhost` |
| `developer console isn't setup properly` (28444) | Created Android OAuth client in console; waited for propagation |
| `getCredentialAsync no provider dependencies found` | Used google_apis_playstore AVD + signed-in Google account |

---

## Current App State (v1.1)

**Verified working**:
- Google sign-in → backend token exchange → Home screen
- All 9 services compile, register in manifest
- All repositories compile with real APIs
- APK builds: `app/build/outputs/apk/debug/app-debug.apk`

**Not yet wired into UI** (next incremental steps):
- SOS screen → `SOSActivationService` + `SOSViewModel`
- Location tracking screen → `LocationTrackingService`
- Ride monitoring screen → `RideMonitoringService` + `RideRepository`
- Check-ins screen → `CheckInService` + `CheckInRepository`
- Emergency contacts screen → `EmergencyContactsService`
- Video capture/upload → `VideoEncryptionService` + `VideoRepository` + CameraX
- Safe places screen → `SafePlacesService`
- Legal help screen → `LegalHelpService`
- Fake call screen → `FakeCallService`
- Profile/settings screen

Each will be restored incrementally from the existing wired repositories/services.

---

## Commands Reference

```bash
# Backend
cd backend && npm run typecheck && npm run dev

# Android
cd rakshyaa
./gradlew compileDebugKotlin
./gradlew assembleDebug          # -> app/build/outputs/apk/debug/app-debug.apk
adb install -r app-debug.apk
```