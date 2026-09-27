# Host your own Rakshyaa backend

This guide deploys the active [Hono Worker](../backend/src/worker/index.ts) to
your own Cloudflare account, with D1 for database records and Cloudinary for
encrypted backup bytes. Google supplies identity and Overpass supplies nearby
places. You control the accounts and configuration; the providers operate the
infrastructure.

## 1. Prepare the tools and accounts

- Install Git and Node.js 22 or newer supported by the locked dependencies. The
  installed Wrangler reviewed here requires Node >=22. Use `npm ci` to preserve
  the lockfile instead of upgrading dependencies during setup.
- Create/access a Cloudflare account with permission to deploy Workers and D1.
- Create/access a Cloudinary product environment and obtain its cloud name,
  API key, and API secret from its dashboard.
- Create/access a Google Cloud project for OAuth. Use separate test and
  production resources if you need isolated environments.
- Review the providers' current quotas and billing before opening public access.
  This repository does not guarantee a free deployment at any traffic level.

From the workspace root:

```powershell
Set-Location backend
npm ci
npx wrangler --version
npx wrangler login
npx wrangler whoami
```

Confirm the intended account before any remote command. The following steps are
for a **new deployment**. For an existing deployment, reuse its database and
signing secrets; inspect migration state before changing anything.

## 2. Configure the Worker and database

Create a database if this account does not already have the intended one:

```powershell
npx wrangler d1 create rakshyaa-db
```

Edit [backend/wrangler.jsonc](../backend/wrangler.jsonc):

| Setting | Action |
| --- | --- |
| `name` | Choose a Worker name unique to this account, e.g. `rakshyaa-backend` |
| `main` | Keep `src/worker/index.ts` |
| `d1_databases[0].binding` | Keep **`DB`**, which the code uses |
| `d1_databases[0].database_name` | Keep `rakshyaa-db` for the provided npm migration scripts |
| `d1_databases[0].database_id` | Replace with the ID returned for your database |
| `routes` | For the first deployment, replace the existing owner's route with `[]` |
| `workers_dev` | Keep `true` to obtain a Workers development-domain URL |
| `compatibility_date`, `compatibility_flags` | Preserve the checked-in values for initial setup |

If you choose a different database name, update both `db:migrate:*` scripts in
`package.json` or use the explicit Wrangler migration commands with that name.
The bundled database ID and custom domain identify someone else's environment;
they are not reusable deployment placeholders.

For a custom domain later, replace `routes` with your own hostname and
`custom_domain: true`, ensure the account manages its zone, redeploy, and verify
HTTPS. Start client configuration only after you know the working origin.

## 3. Configure Google and Cloudinary

In Google Cloud, configure the consent screen/audience and create an OAuth
**Web application** client. Use its client ID as `GOOGLE_WEB_CLIENT_ID` in both
the Worker and Android. The backend verifies Google ID tokens; it does not
consume a Google client secret or implement a browser redirect callback.

Create an OAuth **Android** client in the same project with package
`com.rakshyaa.rakshyaa` and the SHA-1 of the certificate signing the installed
app. Add test users when required by the consent configuration. Debug, direct
release, and Play signing certificates may require separate Android clients.
See [signed app setup](SIGNED_ANDROID_APP.md) and the existing
[Google release guide](GOOGLE_SIGN_IN.md).

Google's current setup prerequisites are described in its
[Credential Manager sign-in documentation](https://developer.android.com/identity/sign-in/credential-manager-siwg).

For Cloudinary, use the credentials of your own product environment. The Worker
makes signed server-side uploads of authenticated raw objects; an unsigned
browser upload preset is not part of this implementation. Application uploads
are limited to 50 MiB and split into 8 MiB chunks. Google, Cloudinary, and
Overpass remain external services even when Wrangler runs locally.

## 4. Set local and deployed configuration

For local testing, copy the example only when `.dev.vars` does not exist:

```powershell
Copy-Item .dev.vars.example .dev.vars
```

Fill all six variables in that private file. Generate independent random JWT
and admin secrets with a password manager (at least 32 random characters each).
Do not reuse the example strings. Local `.dev.vars` is not uploaded as deployed
secrets. Configure production values interactively:

```powershell
npx wrangler secret put GOOGLE_WEB_CLIENT_ID
npx wrangler secret put JWT_SECRET
npx wrangler secret put ADMIN_API_KEY
npx wrangler secret put CLOUDINARY_CLOUD_NAME
npx wrangler secret put CLOUDINARY_API_KEY
npx wrangler secret put CLOUDINARY_API_SECRET
```

Confirm the target name/account if prompted to create a new Worker. Secret
updates can deploy a new version immediately; coordinate changes on an existing
service. Keep values out of command arguments and logs. See
[Cloudflare secret handling](https://developers.cloudflare.com/workers/configuration/secrets/).

Review non-secret `vars`: the current session lifetime is 2592000 seconds
(30 days); `OVERPASS_URL` supplies place lookup; `SUPPORT_EMAIL` currently has
no route consuming it. Full meanings are in [configuration](CONFIGURATION.md).

## 5. Validate locally, then migrate and deploy

Run from `backend/`:

```powershell
npm run db:migrate:local
npm run typecheck
npm test
npx wrangler deploy --dry-run
npm run dev -- --port 8787
```

In another PowerShell window:

```powershell
Invoke-RestMethod http://localhost:8787/health
```

Expect `ok: true` and `runtime: cloudflare-workers`. Stop the dev server with
Ctrl+C when finished. Tests mock external storage, and health does not query
D1 or verify provider credentials.

Before production migration, verify the account and database again. For an
existing database, capture recovery evidence and inspect its current schema:
an old `0001_initial.sql` already recorded as applied will not rerun merely
because its contents changed. In particular, verify `users.session_version`.
Resolve any mismatch with a reviewed incremental migration.

```powershell
npx wrangler d1 migrations list rakshyaa-db --remote
npm run db:migrate:remote
npm run deploy
```

Use the HTTPS URL printed by deployment; do not guess the account subdomain.
Verify `https://YOUR_WORKER_ORIGIN/health`. Wrangler manages explicit SQL
migrations; the Worker does not create tables at startup. Refer to the
[D1 command reference](https://developers.cloudflare.com/workers/wrangler/commands/d1/).

## 6. Connect clients and test the complete flow

In ignored `rakshyaa/backend.properties`, set:

```properties
ENABLE_CLOUD=true
BACKEND_BASE_URL=https://YOUR_WORKER_ORIGIN
GOOGLE_WEB_CLIENT_ID=YOUR_WEB_CLIENT_ID.apps.googleusercontent.com
```

Replace the placeholders, then rebuild Android. In `admin/.env.local`, set
`NEXT_PUBLIC_BACKEND_URL` to the same HTTPS origin and rebuild the portal.
Operators enter the shared admin key at runtime; never embed it in Android or
a `NEXT_PUBLIC_` variable.

Using disposable test data, verify Google sign-in, profile editing, encrypted
upload/download on the original installation, incident creation and resolution,
operator manual refresh, and account deletion. Verify that one account cannot
read another account's backups. Follow [testing](TESTING.md) for negative
cases and [operations](OPERATIONS.md) for rollout records.

## If you mean a private VPS or Docker host

That deployment is not implemented by the current package. The retained
Dockerfile uses removed Node build scripts. `npm start` and `npm run build` do
not exist in the backend package.

A VPS port would need an HTTP runtime adapter, replacement of the D1 binding
with a supported database interface, migration/test changes, background cleanup
handling, HTTPS/process management, and a backup strategy. Removing Cloudinary
also requires an object-storage adapter preserving authenticated access and
cleanup. These are engineering tasks, not configuration switches. Do not expose
Wrangler's development server as the production solution.
