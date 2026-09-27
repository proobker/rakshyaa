# Admin agent guidance

Read the root [AGENTS.md](../AGENTS.md) and [portal README](README.md) before changes.

- This app uses the Pages Router: `pages/index.tsx` and `pages/_app.tsx`.
- Preserve the in-memory operator-key flow. The backend origin may be public build
  configuration; `ADMIN_API_KEY` must not be placed in a `NEXT_PUBLIC_` variable.
- Keep manual-refresh, stale-data, and disconnect behavior explicit. An incident
  row is not proof of dispatch or acknowledgement.
- Preserve HTTPS checks for hosted operator access.
- Run `npm run typecheck` and `npm run build` from this directory.
  No package test or lint script exists.
- Use [manual portal checks](../docs/TESTING.md) for UI changes.
- Keep the generated Next.js guidance below intact; use its local documentation
  pointer when changing framework APIs.

<!-- BEGIN:nextjs-agent-rules -->

# This is NOT the Next.js you know

This version has breaking changes — APIs, conventions, and file structure may all differ from your training data. Read the relevant guide in `node_modules/next/dist/docs/` (resolved from this file's directory; in monorepos the `next` package may not be visible from the repo root) before writing any code. Heed deprecation notices.

This block is written and re-added by `next dev` — verify at `node_modules/next/dist/server/lib/generate-agent-files.js`. Removing it from a diff only re-creates the uncommitted change; committing it with your work keeps the tree clean.

<!-- END:nextjs-agent-rules -->
