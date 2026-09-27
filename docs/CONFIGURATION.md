# Configuration reference

Values below describe the active source. Examples contain placeholders only.
See [hosting](BACKEND_SELF_HOSTING.md) for setup order and
[signing](SIGNED_ANDROID_APP.md) for Android credentials.

## Where configuration lives

| Location | Purpose | Public? |
| --- | --- | --- |
| `backend/wrangler.jsonc` | Worker runtime, D1 binding, routes, non-secret variables | Yes; replace environment identifiers for a fork |
| `backend/.dev.vars.example` | Local Worker template | Yes, placeholders only |
| `backend/.dev.vars` | Local Worker secrets | No |
| Wrangler secrets | Deployed Worker secrets | No; set through Wrangler/provider tooling |
| `rakshyaa/backend.properties.example` | Android build configuration template | Yes |
| `rakshyaa/backend.properties` | Actual Android cloud configuration | Keep local; compiled client identifiers are public |
| `rakshyaa/local.properties` | Machine-specific Android SDK path | No |
| `rakshyaa/.release/signing.properties` | Release signing inputs | No |
| `admin/.env.example` | Public backend-origin template | Yes |
| `admin/.env.local` | Actual portal build configuration | Keep local |

The archived `backend/legacy/.env.example` is only a historical filename pointer.
Use `.dev.vars.example` for the Worker. Do not copy credentials from a different
environment just to satisfy an example.

## Worker bindings and variables

| Name | Meaning / requirement |
| --- | --- |
| `DB` | Required D1 binding; queries use `env.DB` |
| `GOOGLE_WEB_CLIENT_ID` | Expected Google ID-token audience; must equal Android's Web client ID |
| `JWT_SECRET` | Independent strong session-signing secret; rotation invalidates existing sessions |
| `ADMIN_API_KEY` | Independent strong shared operator secret; sent as `x-api-key` |
| `CLOUDINARY_CLOUD_NAME` | Cloudinary product environment cloud name |
| `CLOUDINARY_API_KEY` | Server-side Cloudinary API credential |
| `CLOUDINARY_API_SECRET` | Server-side Cloudinary signing secret |
| `JWT_EXPIRY_SECONDS` | Session duration; current configuration is `2592000` (30 days) |
| `OVERPASS_URL` | Place-query endpoint; current value is `https://overpass-api.de/api/interpreter` |
| `SUPPORT_EMAIL` | Declared placeholder; no current route consumes it |

The first six string values are supplied through `.dev.vars` locally and the
secret commands in the hosting guide for deployment. Non-secret vars are in
Wrangler configuration. Never rely on local files to configure a remote Worker.
Avoid empty JWT/admin secrets: configuration must be completed before serving
authenticated routes.

## Android build-time properties

| Property | Default if absent | Effect |
| --- | --- | --- |
| `ENABLE_CLOUD` | `false` | Becomes `BuildConfig.CLOUD_ENABLED` |
| `BACKEND_BASE_URL` | `https://placeholder.invalid` | API origin; release cloud builds require real HTTPS |
| `GOOGLE_WEB_CLIENT_ID` | `placeholder-web-client-id` | Credential Manager server client ID; use the Web client |

For emulator development use `http://10.0.2.2:8787` in a debug build. For USB
debugging, `adb reverse tcp:8787 tcp:8787` allows a configured
`http://localhost:8787`. Debug network-security resources allow these hosts;
release resources forbid cleartext. Rebuild after every property change.

Signing reads `RAKSHYAA_KEYSTORE`, `RAKSHYAA_STORE_PASSWORD`,
`RAKSHYAA_KEY_ALIAS`, and `RAKSHYAA_KEY_PASSWORD` from environment variables
first, then `.release/signing.properties`. Relative key paths resolve from
`rakshyaa/`. Do not embed Worker or Cloudinary secrets in an APK.

## Portal configuration

Set `NEXT_PUBLIC_BACKEND_URL` to the Worker **origin**, without a trailing slash
or endpoint suffix. Use `http://localhost:8787` for loopback development and
HTTPS for hosting. The code's fallback is `http://localhost:8080`, so explicitly
configure the current Worker port. Rebuild production after changing the value.

`NEXT_PUBLIC_` values reach browser JavaScript. The portal has no environment
variable for the operator credential: the operator types it into the UI, which
keeps it in memory until disconnect/reload. Never add a public admin-key variable.

## Environment isolation

The checked-in Wrangler config defines one target, with no named staging or
production environment. Do not assume `--env production` is ready. A separate
environment needs its own reviewed binding, secrets, routes, and migration
target. Keep test accounts and data separate from live incidents and backups.

Before remote writes, verify the Cloudflare account, Worker name, database ID,
and URL. After deployment, record non-secret values with the release evidence;
store credentials in the appropriate secret manager.
