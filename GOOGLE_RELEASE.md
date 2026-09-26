# Google sign-in in release builds

Google sign-in is optional and controlled at build time. The checked-in
[example](backend.properties.example) defaults to local mode; ignored properties
can enable cloud in a specific APK/AAB.

## Release configuration

Set `ENABLE_CLOUD=true`, a reachable production HTTPS `BACKEND_BASE_URL`, and a
Google OAuth Web client ID in ignored `backend.properties`. The Worker must use
the same `GOOGLE_WEB_CLIENT_ID`. Follow [backend deployment](../backend/DEPLOYMENT.md)
before enabling cloud for users.

The build validates URL/client-ID shape, not DNS, TLS, Google project access,
or a successful token exchange.

## Signing identity

Register the installed app's package `com.rakshyaa.rakshyaa` and signing
certificate with the OAuth Android client configuration. Derive fingerprints
from `.\gradlew.bat signingReport` or the actual signed APK/certificate.

For direct APK distribution, use the APK signing certificate. For Play-distributed
builds, use the applicable app-signing certificate; it can differ from the upload
certificate. Do not reuse a historical developer SHA-1 without checking the artifact.

Signing input names and artifact paths are documented in [release process](../RELEASE.md).
An AAB is a store bundle and is not directly installable with `adb install`.

## Acceptance evidence

For the exact signed artifact and backend revision, record:

1. Backend `/health` reachability over HTTPS.
2. Matching Web-client audience and Android package/certificate registration.
3. Google account selection, cancellation, failed-network behavior, and successful
   `POST /auth/google` exchange.
4. Profile fetch/edit, logout/login, same-installation backup restore, and account
   deletion using a disposable test account.
5. Availability of **Continue on this device** when cloud access is unavailable.

The earlier release notes reported DNS failure for the configured custom domain.
That is historical evidence only; this documentation audit did not test current
DNS or authenticate against production. Consult [release readiness](../RELEASE_READINESS.md)
for checks actually performed during the audit.
