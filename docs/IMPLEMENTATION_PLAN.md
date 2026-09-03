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

## Phase 2: Location Tracking & Emergency Contacts (PLANNED)

### Features to Implement
1. **LocationTrackingScreen**
   - Start/stop background location tracking
   - Foreground service integration (`LocationTrackingService`)
   - Live location sharing with contacts
   - Map preview with current location

2. **EmergencyContactsScreen**
   - CRUD for emergency contacts (name, phone, relationship)
   - Call/SMS quick actions
   - Integration with `EmergencyContactsService` (helper service)
   - Contact picker from device contacts

3. **Navigation Updates**
   - Add location tracking toggle to Home screen
   - Deep links for SOS activation from notifications

---

## Phase 3: Ride Monitoring & Check-ins (PLANNED)

### Features to Implement
1. **RideMonitoringScreen**
   - Ride start/end with route tracking
   - Deviation detection using `RideMonitoringService`
   - Emergency trigger on deviation
   - Share ride link with contacts

2. **CheckInScreen**
   - Scheduled check-in timer
   - Auto-SOS on missed check-in
   - Integration with `CheckInService` (manifest service)
   - Customizable intervals

---

## Phase 4: Advanced Features (PLANNED)

### Features to Implement
1. **VideoCaptureScreen** - Encrypted video recording (AES-256-GCM + Keystore)
2. **SafePlacesScreen** - Nearby hospitals/police stations, user-added places
3. **LegalHelpScreen** - Legal resources, emergency law contacts
4. **FakeCallScreen** - Simulated incoming call with customizable caller
5. **ProfileScreen** - User settings, backup management, app preferences

---

## Architecture Notes

### Service Architecture (Per AGENTS.md)
- **Manifest Services** (4): `@AndroidEntryPoint` + `@Inject lateinit var`
  - `SOSActivationService`, `LocationTrackingService`, `RideMonitoringService`, `CheckInService`
- **Helper Services** (5): `@Singleton` + constructor injection
  - `VideoEncryptionService`, `EmergencyContactsService`, `FakeCallService`, `LegalHelpService`, `SafePlacesService`

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

1. **Immediate**: Test APK on emulator, verify SOS flow end-to-end
2. **Phase 2**: Implement LocationTrackingScreen with foreground service
3. **Phase 2**: Implement EmergencyContactsScreen with contact picker
4. **Integration**: Connect SOS activation to backend incident API
5. **Polish**: Add user-provided color scheme and navigation assets