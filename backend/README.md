# Rakshyaa backend

The active backend is a Cloudflare Worker using Hono and TypeScript. D1 stores
users, incident records, and backup metadata; Cloudinary stores authenticated
raw ciphertext chunks. The entry point is [src/worker/index.ts](src/worker/index.ts).

For a first deployment under your own accounts, follow the
[self-hosting walkthrough](../docs/BACKEND_SELF_HOSTING.md). Backend source can be
public; see [GitHub publication](../docs/GITHUB_PUBLISHING.md) for repository layout
and private-file exclusions.

## Local development

Run from `backend/` with Node.js/npm and the package dependencies installed:

```powershell
npm ci
Copy-Item .dev.vars.example .dev.vars
npm run db:migrate:local
npm run dev -- --port 8787
```

Fill the copied file before exercising authentication or storage. Avoid replacing
an existing local configuration. On POSIX use `cp` for the copy command.
Open `http://localhost:8787/health`; its response reports
`runtime: "cloudflare-workers"`. This checks the route, not Google or Cloudinary connectivity.

Local D1 is managed by Wrangler. Cloudinary, Google token verification, and Overpass
remain external integrations when invoked; local mode does not emulate them.
Automated Worker tests supply their own test bindings and mocked Cloudinary requests.

## Configuration

| Binding / variable | Purpose |
| --- | --- |
| `DB` | D1 binding configured in [wrangler.jsonc](wrangler.jsonc) |
| `GOOGLE_WEB_CLIENT_ID` | Google token audience; same Web client ID as Android |
| `JWT_SECRET` | HS256 session signing secret |
| `ADMIN_API_KEY` | Shared operator credential sent in `x-api-key` |
| `CLOUDINARY_CLOUD_NAME` | Storage account cloud name |
| `CLOUDINARY_API_KEY` / `CLOUDINARY_API_SECRET` | Storage API credentials |
| `JWT_EXPIRY_SECONDS` | Session lifetime, configured as 2592000 seconds |
| `OVERPASS_URL` | Nearby-place upstream URL |
| `SUPPORT_EMAIL` | Declared configuration placeholder; no active route consumes it |

Use `.dev.vars` for local Worker secrets and Wrangler secrets for deployment.
The archived `legacy/.env.example` is not the active Worker configuration template.
Never copy secret values into issue reports or documentation.

## Commands

| Command | Behavior |
| --- | --- |
| `npm run dev` | Start Wrangler development |
| `npm run typecheck` | Typecheck active Worker, TypeScript tests, and test config |
| `npm test` | Run Vitest with the Cloudflare test plugin |
| `npx wrangler deploy --dry-run` | Validate/bundle without publishing |
| `npm run cf-typegen` | Regenerate binding types; modifies a source declaration file |
| `npm run db:migrate:local` | Apply D1 migrations locally |
| `npm run db:migrate:remote` | Apply D1 migrations to the configured remote database |
| `npm run deploy` | Publish the configured Worker and routes |

Deployment instructions are in [the deployment checklist](../docs/BACKEND_DEPLOYMENT.md). The configured
domain and D1 identifier belong to a particular environment; review them before
any remote command. A configured hostname alone does not prove a live deployment.

## Source map

| Path | Responsibility |
| --- | --- |
| [src/worker/index.ts](src/worker/index.ts) | Routes, auth middleware, request validation, streamed downloads |
| [src/worker/auth.ts](src/worker/auth.ts) | Google JWT verification, session issuance and account-generation validation |
| [src/worker/db.ts](src/worker/db.ts) | D1 queries, profile updates, blob/part batches |
| [src/worker/cloudinary.ts](src/worker/cloudinary.ts) | Chunked uploads, signed downloads, object/account cleanup |
| [src/worker/places.ts](src/worker/places.ts) | Overpass query and distance-sorted POIs |
| [migrations/0001_initial.sql](migrations/0001_initial.sql) | Initial relational schema |
| [tests/worker.test.ts](tests/worker.test.ts) | Worker API regression tests |

Uploads are bounded to 50 MiB and split into 8 MiB chunks. The Worker buffers the
incoming upload before splitting it; downloads are streamed sequentially. These
are application constants, not a statement of provider plan limits.

See [API reference](../docs/API.md) for exact request/response contracts and
[security](../docs/SECURITY.md) for what is encrypted and what the server can read.

## Historical files

The [legacy archive](legacy/README.md) contains the earlier Express/filesystem
source, Docker recipe, configuration pointer, and Node regression script.
The current TypeScript include list excludes that application. Its dependencies,
`npm run build`, and `npm start` are not supplied by the current package.
The Dockerfile still calls the removed build script and is not a working Worker
deployment recipe. Do not run the legacy regression and report it as Worker coverage.
