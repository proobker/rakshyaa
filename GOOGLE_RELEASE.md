# Google sign-in release

The local build configuration enables Google sign-in and targets `https://rakshya.rabidahal.com.np`. Both a signed APK and AAB are built. Local mode remains available as a fallback.

## Backend blocker

During verification, this hostname failed DNS resolution (`getaddrinfo failed`). A Google sign-in button alone cannot complete authentication: the app exchanges the Google ID token with `POST /auth/google` on this backend. Set up DNS and HTTPS hosting for the existing `backend/` service, then verify `/health` before distributing this build as functional cloud sign-in. No hosting or paid service was purchased.

The backend must use the same GOOGLE_WEB_CLIENT_ID as the ignored Android backend.properties. Never use the Android OAuth client ID as the web/server client ID. The backend deployment instructions are in ../backend/DEPLOYMENT.md.

## Google Cloud configuration

Register an Android OAuth client for package `com.rakshyaa.rakshyaa` and the signing certificate used by the installed app. The directly distributed release APK certificate SHA-1 is:

`76:FE:3D:F8:19:A6:BE:C1:C9:C3:99:03:AD:D8:01:B7:8A:F5:73:C5`

If publishing through Play App Signing, also register the Play app-signing certificate from Play Console; it may differ from the upload certificate. End-to-end Google authentication remains unverified until DNS, backend configuration and OAuth registration are complete.

The APK is installed with ADB. The AAB is a store-upload artifact and cannot be installed directly with `adb install`.
