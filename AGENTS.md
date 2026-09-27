# Agent instructions

## Scope and sources

This workspace contains three components: Android in `rakshyaa/`, the active
Cloudflare Worker in `backend/src/worker/`, and the Next.js operator portal in
`admin/`.

The outer workspace is the Git root. Keep shared guides under `docs/` and link
them from [the documentation index](docs/README.md); component READMEs and agent
instructions remain beside their source.

- For Android edits, read [Android guidance](rakshyaa/CLAUDE.md).
- For backend edits, read [backend setup](backend/README.md) and [API contracts](docs/API.md).
- For admin edits, read [admin guidance](admin/AGENTS.md).
- For authentication, encryption, sync, deletion, or privacy changes, read
  [architecture](docs/ARCHITECTURE.md) and [security](docs/SECURITY.md).
- For release work, read [release process](docs/RELEASE.md) and [known limitations](docs/KNOWN_LIMITATIONS.md).

## Guardrails

1. Run Gradle inside `rakshyaa/` and npm commands inside their component directory.
2. Use the `DB` D1 binding for the Worker. The Express, filesystem SQLite, Docker,
   and Node regression files are archived in `backend/legacy/`; current npm scripts
   do not support them.
3. Keep the local session usable without Google or backend credentials.
   `backend.properties` maps `ENABLE_CLOUD` to `BuildConfig.CLOUD_ENABLED`, default false.
4. There is no active Supabase integration. Do not introduce Supabase SDKs,
   `SupabaseProvider`, or `hiltService` into the current architecture.
5. Prefer fully-qualified `com.rakshyaa.rakshyaa.R.*` for new resource references.
   Existing explicit imports of that app R class also resolve; non-transitive R
   does not make an explicit import invalid.
6. Preserve the Hilt boundary: `AuthViewModel` triggers `AppDataSync.restoreAll()`.
   Adding `AppDataSync` to `AuthRepository` creates the dependency cycle
   `AuthRepository -> AppDataSync -> ProfileRepository -> AuthRepository`.
7. Preserve user-edited `users.phone` and `users.bio` on Google re-sign-in.
   D1 uses explicit migrations in `backend/migrations/`, not startup auto-migration.
8. Manifest services use `@AndroidEntryPoint` with field injection; helper classes
   use constructor injection. See Android guidance for the current service map.
9. Keep secrets and signing material out of Markdown and commits. Consult
   root/component ignore rules before adding local config. Verify private files,
   including `admin/.env.local`, are ignored and absent from the staged index.
10. Keep existing tests aligned with current APIs. Update tests when behavior
    changes rather than deleting coverage. Do not freeze a test count in instructions.
11. Describe actual emergency behavior: SMS composition and dialer actions require
    user action; incident upload is best-effort; encrypted backup is not cross-device recovery.

## Completion checks

Run relevant typechecks and lints before declaring completion:

- Android: `.\gradlew.bat test compileDebugKotlin lintDebug lintRelease`.
- Backend: `npm run typecheck`, `npm test`, `npx wrangler deploy --dry-run`.
- Admin: `npm run typecheck`, `npm run build`.

Use `./gradlew` on POSIX. Follow an active shell-wrapper policy such as RTK when
provided by the environment. Report failures and environment blockers accurately.
For documentation changes, validate relative links, command/script names, and
claims against source. Respect an explicitly requested Markdown-only scope.
