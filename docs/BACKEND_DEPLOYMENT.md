# Worker deployment

The deployment target is [src/worker/index.ts](../backend/src/worker/index.ts), configured
by [wrangler.jsonc](../backend/wrangler.jsonc). The historical Dockerfile does not build
the current Worker package.

Run commands below from `backend/`. Remote migration, secret, and deploy
commands change the selected Cloudflare environment; review the account, database,
and routes before running them.

## Prepare the environment

1. Install dependencies with `npm ci` and authenticate Wrangler with
   `npx wrangler login`.
2. Choose the target Cloudflare account and D1 database. For a new environment,
   create `rakshyaa-db` with `npx wrangler d1 create rakshyaa-db` and put the
   returned ID into the `DB` binding. For an existing environment, confirm the
   configured database rather than creating a duplicate.
3. Review the custom domain route. This checkout names
   `rakshya.rabidahal.com.np`; replace it for another operator/environment.
   The configuration also enables `workers_dev`.
4. Configure a Cloudinary account whose limits support the application's storage
   needs. Uploads use authenticated raw objects, 8 MiB chunks, and a 50 MiB
   application payload cap. Provider quotas/pricing are outside this repository's
   guarantees.
5. Configure a Google OAuth Web client and the intended Android package/signing
   certificate. See [Google release configuration](GOOGLE_SIGN_IN.md).

## Secrets

Set each value interactively; avoid command-line secret literals and logs:

```powershell
npx wrangler secret put GOOGLE_WEB_CLIENT_ID
npx wrangler secret put JWT_SECRET
npx wrangler secret put ADMIN_API_KEY
npx wrangler secret put CLOUDINARY_CLOUD_NAME
npx wrangler secret put CLOUDINARY_API_KEY
npx wrangler secret put CLOUDINARY_API_SECRET
```

Use independent strong values for JWT signing and operator access. The Google Web
client ID must match the Android build. Review non-secret variables in
`wrangler.jsonc`; setting `SUPPORT_EMAIL` alone does not publish a support page.

## Validate, migrate, publish

```powershell
npm run typecheck
npm test
npx wrangler deploy --dry-run
npm run db:migrate:remote
npm run deploy
```

D1 schema is managed through explicit migrations, not Worker startup. The supplied
initial migration includes `session_version`. If an older database already
recorded an earlier form of that migration, applying migrations again will not
repair a changed historical migration; inspect its schema and prepare an
incremental migration before rollout.

The declared custom-domain route is part of Wrangler configuration. Ensure that
the account can manage that domain and verify its resulting DNS/TLS after deploy.
Do not treat a historical DNS failure or a route declaration as current reachability evidence.

## Verify the deployed integration

- Request `GET /health` over HTTPS and confirm the Worker runtime marker.
- Use a disposable test Google account to verify exchange and protected profile access.
- Exercise encrypted data/media upload and download, ownership boundaries, and deletion.
- Create and resolve a test incident, checking the admin endpoint with the operator key.
- Check nearby-place failure behavior as well as a successful response.
- Verify account deletion invalidates old sessions, including after re-registration.
- Inspect failure logs without exposing tokens, secrets, or personal payloads.

Health alone does not verify D1 schema, Google configuration, or Cloudinary access.
Tests use mocks for external storage and cannot certify deployed credentials.

## Client rollout and operations

Set Android `ENABLE_CLOUD=true`, the HTTPS backend origin, and the same Google
Web client ID, then rebuild. Set the admin's `NEXT_PUBLIC_BACKEND_URL` to that
origin before building. The admin key is entered by the operator at runtime.

Account generation checks can require older clients to reauthenticate. Local
unscoped files are not automatically migrated into new account directories.
Device-bound ciphertext has no cross-device key recovery.

Record the deployment revision, database migration state, client configuration,
and rollback candidate. Retain an appropriate D1 recovery strategy; a Worker
rollback does not reverse schema changes or restore deleted Cloudinary objects.
Cloudinary and D1 cleanup are separate operations.

Before publishing cloud features, document the actual operator support contact,
data recipients, retention, and deletion workflow. No scheduled retention job
or externally hosted privacy site is implemented by this Worker. See
[security](SECURITY.md) and [release readiness](RELEASE_READINESS.md).
