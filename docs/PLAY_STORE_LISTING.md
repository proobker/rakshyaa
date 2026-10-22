# Google Play listing draft

Draft for Android 1.1 / version code 2 (`com.rakshyaa.rakshyaa`), cloud-enabled
build, prepared 2026-10-09. Wording is checked against source and
[known limitations](KNOWN_LIMITATIONS.md). This is not a published listing.
Re-check it whenever emergency, location, or data behavior changes.

## Store text

### App name (30 characters max)

```text
Rakshyaa: Personal Safety
```

### Short description (80 characters max)

```text
SOS countdown, emergency contacts, check-ins and location history in one app.
```

### Full description (4000 characters max)

```text
Rakshyaa is a personal safety companion that keeps your emergency tools one tap away. You can start using it straight away on your device, with no account required.

SOS WHEN YOU NEED IT
• Large SOS button with a short countdown you can cancel if pressed by mistake.
• Prepares a text message with your location for your emergency contacts and opens your dialer. You review and send the message or place the call yourself.
• If you are signed in, an SOS alert with your starting location is also sent to the Rakshyaa operator dashboard when you have a connection.

EMERGENCY CONTACTS
• Keep a list of trusted people to reach quickly in an emergency.

LOCATION HISTORY AND RIDES
• Record your location while tracking is switched on and view it on a map.
• Record a ride so you have a log of the route you took.

CHECK-INS
• Set a check-in timer before you walk home or meet someone new.
• If you do not check in on time, Rakshyaa shows a notification reminding you to contact someone.

RECORD EVIDENCE
• Record video with your camera. Recordings are encrypted on your device.

FAKE CALL
• Trigger a realistic incoming call to help you leave an uncomfortable situation.

NEARBY HELP
• Find nearby hospitals, clinics, police stations and fire stations on the map, using OpenStreetMap data.
• Save your own places and keep notes on legal information.

PRIVACY BY DEFAULT
• Your contacts, location history, check-ins, rides, notes and videos are stored encrypted on your device.
• Google sign-in is optional. Signing in turns on encrypted backup and SOS alerts to the operator dashboard.
• Backups are encrypted with keys that stay on this installation. They cannot be restored on a new phone or after reinstalling the app.
• You can delete your account and data from inside the app.

IMPORTANT
Rakshyaa is not an emergency service. It does not call or text anyone automatically, and it does not contact the police or emergency services for you. Check-in reminders depend on the app keeping running; if your phone restarts or Android closes the app, a pending check-in may not fire. If you are in danger, call your local emergency number.
```

### What's new (500 characters max)

For the first Play release:

```text
First release of Rakshyaa.
• SOS countdown that prepares a message with your location and opens the dialer
• Emergency contacts, location history and ride logs
• Check-in timers with a reminder if you miss one
• Encrypted video recording and fake call
• Nearby hospitals, police and fire stations
• Optional Google sign-in with encrypted backup
```

For an update to an existing Play release (requires version code 3 or higher):

```text
• Check-ins now keep running in the background and save completed or cancelled status correctly.
• The missed check-in notice now tells you to contact someone yourself, instead of saying they were notified.
• SOS alerts to the operator dashboard retry automatically if the connection drops.
```

## Data safety form draft

Based on the cloud-enabled build and [security](SECURITY.md). Confirm each
answer against the deployed backend before submitting.

| Question | Draft answer | Source |
| --- | --- | --- |
| Data collected | Yes, only for signed-in users | Local mode sends nothing to the Rakshyaa backend |
| Precise location | Collected. Used for app functionality: SOS incident coordinates and nearby-place lookup | D1 incidents, `/places/nearby` |
| Name, email address, user IDs | Collected. Used for account management | Google sign-in, D1 `users` |
| Phone number | Collected if the user enters one. Used for account management | D1 `users.phone` |
| Photos | Collected if the user sets a profile photo; encrypted before upload | Avatar upload |
| Videos, contacts, other app data | Uploaded only as ciphertext the server cannot decrypt. Confirm with Play's guidance whether this counts as collected | Encrypted backup |
| Data shared with third parties | No. Cloudinary, Cloudflare, Google sign-in and Overpass act as service providers | Worker integrations |
| Encrypted in transit | Yes, HTTPS only in release | Network security config |
| Users can request deletion | Yes, in the app. Play also requires a web URL for deletion requests | `DELETE /user/account` |
| Collection optional | Yes, the app works without signing in | Local session |

## Permission declarations

| Permission | Play requirement | Notes |
| --- | --- | --- |
| `ACCESS_BACKGROUND_LOCATION` | Location permissions declaration, video demo, and an in-app prominent disclosure | See blocker 1 below |
| `FOREGROUND_SERVICE_LOCATION` | Foreground service declaration for SOS, tracking, and ride services | Describe each user-started task |
| `FOREGROUND_SERVICE_SPECIAL_USE` | Special-use declaration | `CheckInService`: scheduled safety check-ins with a grace-period timer |
| `CAMERA`, `RECORD_AUDIO` | Runtime permission only | Evidence video recording |

## Listing blockers

1. **Background location.** Tracking requires `ACCESS_BACKGROUND_LOCATION`, but
   the tracking, ride, and SOS services already run as `location` foreground
   services. Play rejects background location when a foreground service covers
   the use case. Remove the permission and request, or prepare the declaration
   and in-app disclosure.
2. **Privacy policy URL.** Play requires one, and none exists. It must describe
   the data flows in the table above.
3. **Account deletion URL.** Play requires a web page or form, in addition to
   in-app deletion.
4. **OpenStreetMap attribution.** The app shows OSM map tiles (osmdroid) and
   Overpass place data, but has no visible "© OpenStreetMap contributors" credit.
   OSM's licence requires it. Also review osmdroid's default tile server against
   the OSM tile usage policy for production traffic.
5. **Support email.** `SUPPORT_EMAIL` is empty in `backend/wrangler.jsonc`, and
   Play requires a contact email for the listing.
6. **Sample places.** Fallback safe places use generic names and Kathmandu-area
   coordinates. Remove or label them before they appear in screenshots or to users.
7. **Testing track.** Personal developer accounts created after November 2023
   must run a closed test with at least 12 testers for 14 days before
   production access.

Graphic assets still needed: 512×512 icon, 1024×500 feature graphic, and at
least two phone screenshots. Source artwork is in `assets/`.
