# Build and distribute a signed Android app

The existing [Gradle configuration](../rakshyaa/app/build.gradle) signs release
artifacts using four `RAKSHYAA_*` values. No Gradle edits are needed to supply
your signing identity. Use JDK 17, SDK platform 36, Android SDK build tools,
platform-tools, and the checked-in Gradle wrapper.

Run every command below from the Android project directory (`rakshyaa/` inside
the workspace). On macOS/Linux replace `.\gradlew.bat` with `./gradlew` and use
the corresponding shell commands for file creation/checksums.

## 1. Choose local or cloud mode

For a local release, omit `backend.properties` or set `ENABLE_CLOUD=false` in
an existing file. The app remains usable through **Continue on this device**.

For a cloud release, complete [backend setup](BACKEND_SELF_HOSTING.md), then
create ignored `backend.properties` from its example, if absent, and configure:

```properties
ENABLE_CLOUD=true
BACKEND_BASE_URL=https://YOUR_WORKER_ORIGIN
GOOGLE_WEB_CLIENT_ID=YOUR_WEB_CLIENT_ID.apps.googleusercontent.com
```

These values are compiled into the app. Changing the file after packaging does
not change the APK. Cloud release validation rejects HTTP/local/placeholder
URLs and a missing or implausible Web client ID, but does not test connectivity.

## 2. Create a key only for a new signing identity

If the app is already distributed, use its existing signing identity. Generating
a different key can prevent updates to installed APKs. Keep an encrypted backup
of the keystore and store its passwords separately.

For a genuinely new release key, first confirm the target file does not exist:

```powershell
New-Item -ItemType Directory -Path .release -Force
Test-Path .release/upload-keystore.jks
```

Continue with key generation only when the result is `False`. JDK `keytool`
prompts for passwords and certificate details:

```powershell
keytool -genkeypair -v -keystore .release/upload-keystore.jks -storetype JKS -alias rakshyaa-upload -keyalg RSA -keysize 2048 -validity 10000
```

Do not put passwords into the command. The filename does not determine whether
this is an upload key or the app's installed signing key. For direct APK
distribution it signs the APK users install; Play App Signing can use a separate
app-signing key. See [Android signing documentation](https://developer.android.com/studio/publish/app-signing).

## 3. Supply signing properties

Create `rakshyaa/.release/signing.properties` locally with these keys, replacing
the example values with your own:

```properties
RAKSHYAA_KEYSTORE=.release/upload-keystore.jks
RAKSHYAA_STORE_PASSWORD=YOUR_PRIVATE_STORE_PASSWORD
RAKSHYAA_KEY_ALIAS=rakshyaa-upload
RAKSHYAA_KEY_PASSWORD=YOUR_PRIVATE_KEY_PASSWORD
```

The relative path resolves from the Android project root, not `app/`. Prefer
forward slashes in absolute Windows property paths, e.g. `C:/secure/key.jks`;
Java properties interpret backslashes as escapes. Environment variables with
these names override the file values individually. A stale environment value
can therefore override an otherwise correct file.

The Android `.gitignore` excludes `.release/`. From whichever Git repository
will actually be published, verify that the signing properties and keystore
are ignored and untracked before staging source. Never upload this directory
to GitHub Releases or commit it with the source.

## 4. Register Google signing certificates for cloud builds

```powershell
.\gradlew.bat signingReport
keytool -list -v -keystore .release/upload-keystore.jks -alias rakshyaa-upload
```

Register package `com.rakshyaa.rakshyaa` and the appropriate certificate SHA-1
in a Google OAuth Android client. Use the OAuth **Web** client ID as the Worker
audience and Android `GOOGLE_WEB_CLIENT_ID`. For Play installs, register the
Play app-signing certificate, which can differ from your upload certificate.
Keep debug and release identities distinct. See
[Google release configuration](GOOGLE_SIGN_IN.md).

## 5. Check, version, and build

The source reviewed here has `versionName "1.1"` and `versionCode 2`. For an
update, choose a higher version code and record the release name in
`app/build.gradle` before packaging. Preserve the application ID for updates.

```powershell
.\gradlew.bat test compileDebugKotlin lintDebug lintRelease
.\gradlew.bat validateReleaseConfiguration
.\gradlew.bat assembleRelease bundleRelease
```

Stop and investigate any failure. `validateReleaseConfiguration` checks signing
input presence and the keystore path; packaging also checks the actual key and
password. Release minification is currently disabled.

| Output | Path relative to the Android directory | Use |
| --- | --- | --- |
| Signed APK | `app/build/outputs/apk/release/app-release.apk` | Direct installation or GitHub Release asset |
| Signed AAB | `app/build/outputs/bundle/release/app-release.aab` | Store upload; not directly installable with ADB |

Android Studio users can open this same Gradle project and select
**Build > Generate Signed Bundle / APK**, use the same keystore, and select
the release variant. Keep the properties configured because this project's
custom release validation still runs. Note Studio's selected output directory.

## 6. Verify the exact artifact

Use the SDK build tools on PATH, or invoke their full paths:

```powershell
apksigner verify --verbose --print-certs app/build/outputs/apk/release/app-release.apk
zipalign -c -P 16 4 app/build/outputs/apk/release/app-release.apk
Get-FileHash app/build/outputs/apk/release/app-release.apk -Algorithm SHA256
Get-FileHash app/build/outputs/bundle/release/app-release.aab -Algorithm SHA256
adb install -r app/build/outputs/apk/release/app-release.apk
```

An update requires a compatible signing certificate. If a debug APK is already
installed under the same package, test the release on a separate device/emulator
instead of casually uninstalling the working app. Uninstalling or clearing data
can destroy the only keys able to decrypt existing backups.

Confirm the APK certificate matches the intended identity and retain the
checksums. Check native-library alignment and actual target-device behavior as
described in [release process](RELEASE.md); ZIP alignment alone is insufficient.

## 7. Publish the reviewed app

Complete the [manual acceptance tests](TESTING.md) and record skipped
checks and [known limitations](KNOWN_LIMITATIONS.md). Use consenting
testers; SMS composition and dialer actions require user action, incident upload
is best-effort, and backups do not offer recovery on a new installation.

For GitHub, create a Release from the intended source tag and upload the verified
APK plus its SHA-256 and release notes. State cloud/local mode, backend origin
if applicable, supported Android API level (minimum 24), and tested devices.
Keep signing material, local configs, and personal data out of release assets.
For Play, upload the AAB and complete the current store requirements using the
app's actual data handling. Preserve the key and source revision for updates.
