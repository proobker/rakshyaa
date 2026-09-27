# Rakshyaa operator portal

This Next.js Pages Router app displays active incidents from the Worker.
It uses React state for the fetched records and an in-memory operator API key.

The source can be public on GitHub. See [publication](../docs/GITHUB_PUBLISHING.md)
and the [portal hosting walkthrough](../docs/ADMIN_SETUP.md).

## Setup

From `admin/`:

```powershell
npm ci
Copy-Item .env.example .env.local
npm run dev
```

Set the public backend URL in `.env.local` before starting or building:

```dotenv
NEXT_PUBLIC_BACKEND_URL=http://localhost:8787
```

Use the HTTPS Worker origin for a hosted portal. The API client defaults to
`http://localhost:8080`, which differs from Wrangler's usual development port;
set the URL explicitly. HTTP operator requests are accepted only when both the
portal and backend use loopback hostnames. A hosted portal requires an HTTPS backend.

The backend URL is public build configuration. **Do not put the operator key in
a `NEXT_PUBLIC_` variable.** The root and component ignore rules exclude local
environment files while keeping `.env.example`. Verify the actual publication
repository's staged files before committing; ignore rules do not remove files
already tracked. Keep this file free of secrets.

## Use

Enter the backend's `ADMIN_API_KEY` in the password field. The app sends it as
`x-api-key` to `GET /incidents/admin/active`; no Google session is used.

- Connection loads the newest 50 active incidents, ordered by activation time.
- Refresh is manual; there is no polling, push delivery, or operator acknowledgement.
- A failed refresh retains the last response and displays a stale-data warning.
- Disconnect clears the key and records. Reload also loses the in-memory key.
- Location links open Google Maps. Missing coordinates display as unavailable.
- The shared key provides backend API access; it is not per-operator identity or
  a portal access-control system.

Host the portal over HTTPS and arrange operator access restrictions for the
deployment. The `noindex,nofollow` meta tag does not restrict access.

## Maintenance

| Path | Role |
| --- | --- |
| [pages/index.tsx](pages/index.tsx) | Login form, manual refresh, stale/error state, incident table |
| [lib/apiClient.ts](lib/apiClient.ts) | Backend URL, transport checks, 15-second timeout, response validation |
| [pages/_app.tsx](pages/_app.tsx) | Shared app entry and global stylesheet |
| [AGENTS.md](AGENTS.md) | Agent guidance and installed Next.js documentation pointer |

```powershell
npm run typecheck
npm run build
npm start
```

There is no lint or test script in this package. Check the installed Next.js docs
before framework changes, and use the manual portal scenarios in
[testing](../docs/TESTING.md). See [API contracts](../docs/API.md) for the
incident response and [limitations](../docs/KNOWN_LIMITATIONS.md) for integration gaps.
