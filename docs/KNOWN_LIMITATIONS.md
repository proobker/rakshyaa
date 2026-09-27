# Known implementation limitations

These findings come from source inspection on 2026-09-26. They were documented
without changing application code. They are not all reproduced device failures.
Use [testing](TESTING.md) to verify behavior before treating a feature as release-ready.

## Emergency and location flows

| Area | Source-backed limitation | Consequence |
| --- | --- | --- |
| Check-in startup | CheckInViewModel starts a foreground service with `ACTION_SCHEDULE_CHECK_IN`; that service branch starts a timer without calling the foreground-start branch. | Cold scheduling needs a device regression for foreground-service promotion. |
| Check-in completion | The view model writes completed status directly; it does not notify the service's `isCheckedIn` flag. | The still-running timer can later mark the same record missed. |
| Check-in cancellation | Cancelling clears service state but leaves the repository record pending. | UI history can retain a cancelled item as pending. |
| Check-in delivery | The missed-check-in branch posts a local notification if a contact exists; no message is sent to that contact. | Its “contact has been notified” text is not evidence of delivery. |
| Check-in durability | Timer state is held in memory, with no persisted alarm or restart reconstruction. | Process death/reboot does not establish reliable scheduled execution. |
| Incident delivery | IncidentRepository wraps cloud create/resolve calls in best-effort `runCatching`; no durable retry queue exists. | Local success can coexist with a missing or still-active cloud incident. |
| Incident location | The cloud record receives initial coordinates only; SOS location polling writes the local history. | The operator location is not a live tracking feed. |
| Ride deviation | RideRepository compares the newest point to earlier recorded points using a fixed 300 m threshold. The service has a separate configurable helper, unused by its GPS callback. | The slider/helper is not a proven planned-route alert system. |
| Location lifecycle | Location/ride cleanup removes callbacks in explicit stop paths; `onDestroy()` only cancels coroutine scope. | Process/service lifecycle and denied/approximate permissions need physical-device tests. |

Source:
[CheckInViewModel](../rakshyaa/app/src/main/java/com/rakshyaa/rakshyaa/viewmodels/CheckInViewModel.kt),
[CheckInService](../rakshyaa/app/src/main/java/com/rakshyaa/rakshyaa/services/CheckInService.kt),
[IncidentRepository](../rakshyaa/app/src/main/java/com/rakshyaa/rakshyaa/data/repositories/IncidentRepository.kt),
[SOSActivationService](../rakshyaa/app/src/main/java/com/rakshyaa/rakshyaa/services/SOSActivationService.kt),
[RideRepository](../rakshyaa/app/src/main/java/com/rakshyaa/rakshyaa/data/repositories/RideRepository.kt),
[RideMonitoringService](../rakshyaa/app/src/main/java/com/rakshyaa/rakshyaa/services/RideMonitoringService.kt),
[LocationTrackingService](../rakshyaa/app/src/main/java/com/rakshyaa/rakshyaa/services/LocationTrackingService.kt).

## Backup, profile, and settings

- Backups depend on the original installation's keys. No reinstall or cross-device
  recovery is implemented.
- Restore authenticates ciphertext but has no timestamp merge/conflict policy.
  A readable remote copy can overwrite newer unsynced local content.
- Upload preference does not gate restore reads. Manual sync uploads top-level
  datastore files, not a retry queue for failed video uploads.
- Video upload failures are swallowed after local storage; local recording success
  does not prove media reached Cloudinary. Video removal does not delete remote media.
- Google re-sign-in preserves phone/bio but overwrites server name and picture
  with Google values. A custom name/photo can therefore be replaced.
- Android converts empty editable profile text to null; the Worker updates only
  string fields. Clearing a cloud phone/bio through the Android form is not
  equivalent to explicitly sending an empty string to the API.
- Profile photo selection requires cloud backup; there is no implemented local-only
  avatar storage path.
- Notification and location-sharing switches persist preferences, but the
  foreground services do not read those preferences. Do not present them as
  enforced service/privacy controls.
- Plaintext recording/decryption cache files lack a comprehensive cleanup lifecycle.

Source:
[SyncManager](../rakshyaa/app/src/main/java/com/rakshyaa/rakshyaa/data/sync/SyncManager.kt),
[VideoRepository](../rakshyaa/app/src/main/java/com/rakshyaa/rakshyaa/data/repositories/VideoRepository.kt),
[ProfileRepository](../rakshyaa/app/src/main/java/com/rakshyaa/rakshyaa/data/repositories/ProfileRepository.kt),
[ProfileViewModel](../rakshyaa/app/src/main/java/com/rakshyaa/rakshyaa/viewmodels/ProfileViewModel.kt),
[Worker database functions](../backend/src/worker/db.ts).

## Places, resources, backend, and tooling

- Bundled fallback places use generic names and fixed Kathmandu-area coordinates.
  They are sample data, not a verified local safety directory.
- Bundled legal text and phone numbers have no locale selection or maintained
  verification metadata. This review did not validate their real-world applicability.
- Overpass searches 50 km but returns at most 25 places. Client-side radius filtering
  does not imply complete coverage. Nearby lookup has no cache or rate limiter in
  the Worker source.
- The operator API returns only 50 active incidents and provides no pagination.
  The browser refresh is manual and has no delivery acknowledgement.
- Uploads are buffered in Worker memory before chunking. Cloudinary and D1 writes
  are separate; cleanup is best-effort on some failure/replacement paths.
- The initial D1 migration contains the current schema. There is no incremental
  migration in this checkout for a database that already applied an older version
  of that same migration without `session_version`. Compare existing schema before rollout.
- The inactive Express source, Dockerfile, and Node API regression are isolated
  in `backend/legacy/`; current package scripts/dependencies do not support them.
- The missing `admin/.env.local` exclusion identified in the original audit was
  addressed on 2026-09-27 with root and component ignore rules. Verify staged
  files in the actual publication repository; already-tracked files stay tracked.
- Android license text is MIT, backend package declares MIT, and admin declares ISC.

These are follow-up implementation/documentation decisions, not changes performed
during this Markdown-only audit. [PLAN.md](PLAN.md) orders the remaining work.
