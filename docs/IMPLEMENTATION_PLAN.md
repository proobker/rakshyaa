# Android implementation map

This replaces the earlier phase checklist, which called incomplete behaviors
fully verified. It records source ownership as of 2026-09-26; release acceptance
is tracked separately in [release readiness](../../RELEASE_READINESS.md).

| Area | Main source ownership | Remaining qualification |
| --- | --- | --- |
| Navigation and dashboard | MainActivity, NavGraph, BottomNavBar, HomeScreen | Exercise all routes with accessibility/font changes |
| Authentication | GoogleAuthClient, AuthRepository, AuthViewModel | Local and cloud modes; real OAuth acceptance is separate |
| SOS | SOSViewModel, SOSButton, SOSActivationService, SosRuntime | Service active is not emergency delivery |
| Contacts and message preparation | EmergencyContactsRepository/Service, EmergencyActionsViewModel | External app requires user confirmation |
| Location | LocationRepository, LocationTrackingService/ViewModel | Permissions, GPS availability, lifecycle checks |
| Rides | RideRepository, RideMonitoringService/ViewModel | Current deviation heuristic is not a planned-route system |
| Check-ins | CheckInRepository, CheckInService/ViewModel | Timer, foreground promotion, completion/cancellation gaps |
| Video | VideoCaptureScreen/ViewModel, VideoRepository, VideoEncryptionService | Best-effort media backup and incomplete remote removal |
| Places/legal | SafePlacesRepository, LegalHelpRepository, helpers and screens | Bundled sample data needs verification |
| Profile | ProfileRepository/ViewModel, ProfileScreen | Custom-field preservation and preference enforcement gaps |
| Backup | SyncManager, AppDataSync, EncryptedLocalStore | Original keys required; no merge/retry protocol |

The active work order and completion criteria are in [the root plan](../../PLAN.md).
See [Android overview](../README.md) for current tooling and
[architecture](../../docs/ARCHITECTURE.md) for persistence and service boundaries.

Previous claims of automatic SOS on missed check-in, notified contacts, live
operator location, and guaranteed reinstall restore are not supported by the
current source. Use [known limitations](../../docs/KNOWN_LIMITATIONS.md) when
writing feature copy or acceptance tests.
