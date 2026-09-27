# Historical backend archive

These files preserve the inactive Express/filesystem SQLite implementation:

- `src/`: earlier Node server, routes, auth, database, and storage helpers.
- `tests/incidents.cjs`: its historical Node regression script.
- `Dockerfile` and `.dockerignore`: its obsolete container recipe.
- `.env.example`: an old filename retained as a placeholder configuration pointer.

This directory is outside the active TypeScript include list. The current
backend package does not provide its dependencies, `build` script, or `start`
script. This is not a supported VPS/Docker deployment or a second backend to run.
The `.env.example` is not evidence that those old files accept the active
Worker's configuration.

Use [the active backend](../README.md), [self-hosting](../../docs/BACKEND_SELF_HOSTING.md),
and [API reference](../../docs/API.md). Preserve these historical files only as
reference when planning a future port; do not count their regression script as
Worker coverage.
