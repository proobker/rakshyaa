# Rakshyaa Android App - Implementation Plan & Progress

## Phase 1: Navigation + SOS Screen (COMPLETED)

### Objective
Implement Instagram-style bottom navigation and SOS activation screen with countdown animation.

### Completed Work

#### 1. Navigation Infrastructure
- **NavGraph.kt** (`ui/navigation/NavGraph.kt`)
  - Sealed interface `Screen` with 5 main tabs: Home, SOS, Tracking, Contacts, More
  - Sub-routes for Phase 2 features: ride, checkin, video, safeplaces, legal, fakecall, profile
  - `RakshyaaNavHost` composable with full navigation graph
  - Fixed Hilt import: `androidx.hilt.navigation.compose.hiltViewModel`
  - Fixed NavHost import: `androidx.navigation.compose.NavHost`

- **BottomNavBar.kt** (`ui/components/BottomNavBar.kt`)
  - Instagram-style animated bottom navigation using Material3 `NavigationBar`/`NavigationBarItem`
  - Animated icon scaling (spring animation) and alpha transitions
  - Selected/unselected states with proper Material3 colors
  - Removed unused `ReceiveChannel` import
  - Fixed Material3 API: `containerColor` instead of `backgroundColor`

- **MainActivity.kt**
  - Integrated `RakshyaaNavHost` with `BottomNavBar` inside `Scaffold`
  - Auth state handling with `AuthViewModel`
  - Fixed `State.map` → `derivedStateOf` for current route tracking
  - Added missing `Modifier.padding(PaddingValues)` import

#### 2. SOS Screen (`ui/screens/SOSScreen.kt`)
- **Features**:
  - Central shield icon with active/inactive state colors
  - Status text showing SOS state (active/activating/inactive)
  - Large circular SOS button with pulse animation when active
  - 3-second countdown overlay with cancel-on-tap
  - Three info cards: Emergency Called, Location Sharing, Admin Notified
  - Emergency info card at bottom
  - Deactivate confirmation dialog

- **Dependencies on existing services**:
  - Uses `SOSViewModel` (Hilt-injected) with `uiState` flow
  - Integrates with `SOSButton` component

#### 3. SOS Button Component (`ui/components/SOSButton.kt`)
- **Features**:
  - Circular button (140dp) with shield/close icon
  - Color animation (primary → error) on activation
  - Pulse scale animation (spring, infinite repeat) when SOS active
  - Countdown overlay with large numeric display
  - Disabled state during activation countdown
  - Tap-to-cancel during countdown

- **Fixed imports**:
  - `androidx.compose.runtime.collectAsState`
  - `androidx.compose.ui.graphics.graphicsLayer`
  - `androidx.compose.foundation.background`
  - `androidx.compose.ui.draw.alpha`
  - Animation APIs from `androidx.compose.animation.core`

#### 4. Home Screen Updates (`ui/screens/HomeScreen.kt`)
- **Features**:
  - 9-feature grid (2 columns) with Instagram-style cards
  - Primary SOS card highlighted
  - Navigation to all feature screens
  - Sign out button
  - Added missing imports: `background`, `TextAlign`

#### 5. Placeholder Screens Created
All placeholder screens accept `onNavigate` callback for future sub-navigation:
- `LocationTrackingScreen.kt`
- `EmergencyContactsScreen.kt`
- `RideMonitoringScreen.kt`
- `CheckInScreen.kt`
- `VideoCaptureScreen.kt`
- `SafePlacesScreen.kt`
- `LegalHelpScreen.kt`
- `FakeCallScreen.kt`
- `ProfileScreen.kt`

### Build Configuration Fixes

#### build.gradle (`rakshyaa/app/build.gradle`)
- Added Compose animation dependencies:
  - `androidx.compose.animation:animation:1.6.8`
  - `androidx.compose.animation:animation-core:1.6.8`
- Updated Compose BOM to stable `2024.08.00` (resolves to Compose 1.6.8)
- Fixed animation import paths (`androidx.compose.animation.core` for specs)

### Technical Issues Resolved

| Issue | Solution |
|-------|----------|
| `Unresolved reference 'composable'` in NavGraph | Changed `androidx.navigation.NavHost` → `androidx.navigation.compose.NavHost` |
| `Unresolved reference 'animateFloatAsState'` | Added `animation:1.6.8` and `animation-core:1.6.8` dependencies; fixed import to `androidx.compose.animation.core.animateFloatAsState` |
| `Easing.Default` / `FastOutSlowInEasing` not found | Removed explicit easing (uses `tween` default) |
| `State.map` not found | Replaced with `derivedStateOf` |
| `Modifier.weight` internal access error | Moved `.weight(1f)` to call site in `Row` scope; removed problematic import |
| `MutableState` delegate missing `setValue` | Added `import androidx.compose.runtime.setValue` |
| `BottomNavigation` not in Material3 | Replaced with `NavigationBar`/`NavigationBarItem` |
| `backgroundColor` not in Material3 BottomNavigation | Changed to `containerColor` |

### Build Verification
- ✅ `./gradlew compileDebugKotlin` - SUCCESS
- ✅ `./gradlew assembleDebug` - SUCCESS
- APK output: `app/build/outputs/apk/debug/app-debug.apk`

---

## Phase 2: Location Tracking & Emergency Contacts (COMPLETED — Sep 2026)

### Features Implemented
1. **LocationTrackingScreen** (`ui/screens/LocationTrackingScreen.kt`)
   - Start/stop background location tracking with **two-step permission flow** (fine → background on API 23+/28+)
   - Foreground service integration (`LocationTrackingService`) with **permission-before-`startForeground`** guard
   - Live location state + permission/error banners
2. **EmergencyContactsScreen** (`ui/screens/EmergencyContactsScreen.kt`)
   - CRUD for emergency contacts (name, phone, relationship)
   - Call/SMS quick actions
   - Integration with `EmergencyContactsService` (helper service)
3. **Navigation Updates**
   - Location tracking & emergency contacts reachable from Home grid + bottom nav

---

## Phase 3: Ride Monitoring & Check-ins (COMPLETED — Sep 2026)

### Features Implemented
1. **RideMonitoringScreen** (`ui/screens/RideMonitoringScreen.kt`)
   - Ride start/end with route tracking; **in-screen location-permission request** before start
   - Deviation detection using `RideMonitoringService`
   - Emergency/alerts on route deviation
2. **CheckInScreen** (`ui/screens/CheckInScreen.kt`)
   - Scheduled check-in timer with grace periods
   - Auto-SOS on missed check-in
   - Integration with `CheckInService` (manifest service, `specialUse` FGS type on Android 14+)
   - Customizable intervals

---

## Phase 4: Advanced Features (COMPLETED — Sep 2026)

### Features Implemented
1. **VideoCaptureScreen** — encrypted video recording (AES-256-GCM + Keystore) with camera-style start/stop control bar, mm:ss overlay, FRONT/REAR + ON/OFF chips (CameraX `Recorder`; output via `outputResults.outputUri`)
2. **SafePlacesScreen** — nearby hospitals/police stations + user-added places
3. **LegalHelpScreen** — legal resources, emergency law contacts
4. **FakeCallScreen** — simulated incoming call with **live delay slider (5–60s)** + phase machine (IDLE/COUNTDOWN/INCOMING/CONNECTED)
5. **ProfileScreen** — Google avatar or photo-picker photo, phone/bio editing (`GET/PUT /user/profile`), sign-out (moved from Home), links to `https://rakshyaapp.github.io`

---

## Architecture Notes

### Service Architecture (Per AGENTS.md)
- **Manifest Services** (4): `@AndroidEntryPoint` + `@Inject lateinit var`
  - `SOSActivationService`, `LocationTrackingService`, `RideMonitoringService`, `CheckInService`
- **Helper Services** (6): `@Singleton` + constructor injection
  - `VideoEncryptionService`, `EmergencyContactsService`, `FakeCallService`, `LegalHelpService`, `SafePlacesService`, `GeocodingService`

### Key Constraints
- `android.nonTransitiveRClass=true` → Use `com.rakshyaa.rakshyaa.R.*` fully qualified
- No Supabase references (stale)
- Backend: `http://10.0.2.2:8080` (emulator) via `BACKEND_BASE_URL` BuildConfig
- Google OAuth: `GOOGLE_WEB_CLIENT_ID` from `backend.properties`

### Testing
- Emulator: `Medium_Phone_API_36.1` (Google APIs Play Store, API 36)
- Run: `./gradlew assembleDebug` → install APK
- Backend: `cd ../backend && npm run dev`

---

## Next Steps

1. **Immediate (done)**: Test APK on emulator, verify each feature end-to-end — all features now restored and verified (Sep 2026)
2. **Restore-on-login**: `AppDataSync` pulls blobs + profile on session start — done
3. **Release hardening**: production signing (register release SHA-1 as a second Android OAuth client), Play Store submission
4. **Polish**: dark-mode fixes, string consistency across screens, notification deep links