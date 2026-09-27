# Set up and publish the operator portal

The portal source can be public. The deployed portal's operator key must remain
private and is entered into the UI at runtime. Publishing source and granting
operator access are separate decisions.

The app uses the Next.js Pages Router. It fetches the newest 50 active incidents
from `GET /incidents/admin/active`, with the shared key in `x-api-key`.

## Local setup

Install dependencies and create the example config only if no local file exists.
Run from `admin/`:

```powershell
npm ci
Copy-Item .env.example .env.local
```

Set the local backend origin in `.env.local`:

```dotenv
NEXT_PUBLIC_BACKEND_URL=http://localhost:8787
```

Start the Worker in a separate terminal using [backend setup](BACKEND_SELF_HOSTING.md),
then run `npm run dev` from `admin/` and open the local URL it prints. Both the
portal and API must use loopback hostnames for HTTP operator access. If either
is hosted, use an HTTPS API. The code's fallback port is 8080, so explicitly set
8787 for this Wrangler workflow.

Enter your local Worker's `ADMIN_API_KEY` in the portal form. Never put it in
`.env.local` as a `NEXT_PUBLIC_` value or hard-code it into source.

## Production setup

1. Deploy and test the Worker, including D1 migrations and the admin secret.
2. Configure `NEXT_PUBLIC_BACKEND_URL=https://YOUR_WORKER_ORIGIN` in the build
   environment, without a trailing slash. This value is intentionally public.
3. From `admin/`, run:

   ```powershell
   npm ci
   npm run typecheck
   npm run build
   npm start
   ```

4. On a Node-capable host, use `admin/` as the project root (or the repository
   root for a separate admin repository), `npm run build` as the build command,
   and `npm start` as the start command. Configure HTTPS and process supervision
   using that host's deployment facilities. Restrict operator access as required
   for your deployment; `noindex,nofollow` is not access control.
5. Test the hosted URL with the production operator key and a disposable incident.
   Rebuild when the backend origin changes. Store the key privately and share it
   only with authorized operators.

This checkout has no static-export configuration or GitHub Pages deployment
pipeline. Uploading the source to GitHub does not start the dashboard. Keep
deployment credentials in the hosting provider's secret storage, not the repo.

## Operator workflow and acceptance

- Connect loads records. Refresh is manual; there is no polling or push feed.
- Failed refresh retains the previous data with a stale warning.
- Disconnect and page reload discard the in-memory key. Disconnect clears rows.
- Map links open Google Maps. Cloud incident positions are initial coordinates,
  not continuous live tracking.
- A displayed incident does not establish acknowledgement or dispatched help.

Verify successful connect, wrong-key failure (403), network timeout, stale data,
disconnect, missing coordinates, and resolution disappearing after refresh.
There are no test/lint npm scripts in this package; use the typecheck, build, and
[manual checks](TESTING.md).

The component `.gitignore` excludes `.env.local`, dependencies, generated build
output, and TypeScript caches. In the actual publication repository, verify
ignore behavior and staged contents using [GitHub publication](GITHUB_PUBLISHING.md).
