# Play location declaration and recording plan

Prepared 2026-10-09 from the current Android source. These are draft answers,
not evidence that background location permission is necessary or approved.

## App purpose

```text
Rakshyaa is a personal safety app that helps users prepare for emergencies, manage trusted contacts, record location history and rides, set check-in reminders, and capture encrypted video. Its SOS flow prepares a location message and opens the dialer; users send messages and place calls themselves.
```

## Location access: one feature

```text
Location Tracking records the user's location history during an active tracking session, including while the app is minimized or the screen is off. A persistent notification indicates recording. Users can return to view recorded locations on a map and in history, and stop tracking from the Location Tracking screen.
```

This describes the implemented feature but does not establish why it requires
`ACCESS_BACKGROUND_LOCATION`. The service uses a location foreground service.
Android permits such a service to continue location access when the app is
minimized or the screen is off. Prefer removing the unnecessary permission and
its UI gating after reviewing all location flows, then uploading a new build.
Do not claim live contact sharing, automatic emergency delivery, or continuous
operator tracking to justify the permission: those are not implemented here.

## Implementation changes and remaining review concern

- Location Tracking, SOS, and ride recording now show a shared disclosure before
  location consent or a new session, including when Android permission already exists.
- Granting fine location no longer starts tracking. Location Tracking requires
  a separate Start action, which also shows the disclosure.
- Android 11+ users see an explanation before opening app settings for background
  access. Android 10 uses its separate runtime background request. Returning to
  the tracking screen refreshes permissions and history.
- Location Tracking requests notification permission on Android 13+ at session
  start, so users can allow the ongoing tracking notification.
- A foreground service is already used, so disclosure alone does not establish
  eligibility for background permission.

## Recording sequence

Record the actual build submitted to Play, using a clean test installation or
test device without personal data. Do not erase an existing installation just
to reset permission prompts.

1. Open Rakshyaa and select Continue on this device if sign-in is shown.
2. Open Location Tracking. Show the in-app disclosure and its consent action
   before the Android permission prompt.
3. Show the actual location permission flow. If retaining background permission,
   include the all-the-time permission settings flow for the Android version.
4. Start tracking and show the first recorded location.
5. Press Home, show the persistent tracking notification, and demonstrate a
   location change while the app is minimized. Label simulated GPS if using an
   emulator. Allow the actual update interval to elapse; the current service
   requests updates at five-minute intervals with a 100-metre distance filter.
6. Return to Rakshyaa, refresh using Sync Now, and show the new history entry.
7. Stop tracking and show that the ongoing tracking notification disappears.

The final submission video must include disclosure, permission consent, and
the declared feature working. A draft missing these is not submission-ready.
Upload the finished video to YouTube as unlisted and use its accessible URL in
Play Console. No YouTube URL has been created by this task.

## Validation of the code changes

`test compileDebugKotlin lintDebug lintRelease assembleDebug` completed
successfully on 2026-10-09. Debug and release JVM test reports each contain
55 tests with no failures or errors. Gradle executed 58 tasks and reused 44
up-to-date tasks. Both lint variants report 110 warnings and 7 hints, with no
errors. Warnings include unused resources, KTX suggestions, modifier parameters,
and launcher icon guidance; passing lint does not mean a warning-free release.

The updated debug APK was installed on the API 36.1 emulator without clearing
app data. Location permissions were reset for the recording, then granted through
the actual Android UI. The walkthrough verified that permission consent leaves
tracking stopped, and explicit Start displays the disclosure and requests
notification permission before starting the session.

The emulator recorded an initial test point near 37.422, -122.084 at 7:05 PM,
then a changed simulated point near 37.426, -122.080 at 7:10 PM while the app
was minimized. Returning to the app displayed the new history entry. Stopping
tracking removed the ongoing notification. These are emulator observations,
not physical-device reliability results.

The task output includes `rakshyaa-location-walkthrough.mp4` and the uncut
`rakshyaa-location-raw.mp4`. The edited video identifies the debug build,
simulated GPS, sped-up sections, and the omitted idle wait. It is a draft
walkthrough, not proof that background permission is eligible for approval.
The existing release APK/AAB has not been rebuilt or published by this task.

Recorded debug APK SHA-256:
`71d7c172d8298da9e98fa501959e59c7f99bb19c3c63bb515b8d4eb32a4a0e24`.

## Sources

- [Google Play background location guidance](https://support.google.com/googleplay/android-developer/answer/9799150?hl=en-GB)
- [Android location access categories](https://developer.android.com/develop/sensors-and-location/location/permissions)
- [Tracking screen](../rakshyaa/app/src/main/java/com/rakshyaa/rakshyaa/ui/screens/LocationTrackingScreen.kt)
- [Tracking view model](../rakshyaa/app/src/main/java/com/rakshyaa/rakshyaa/viewmodels/LocationTrackingViewModel.kt)
- [Tracking service](../rakshyaa/app/src/main/java/com/rakshyaa/rakshyaa/services/LocationTrackingService.kt)
- [Known limitations](KNOWN_LIMITATIONS.md)
