# GitHub publication and repository layout

Android, backend, admin, assets, and documentation share one public repository:
[proobker/rakshyaa](https://github.com/proobker/rakshyaa).
The repository root is the outer workspace, with one `.git` directory. The
Android project's previous Git history is preserved; no force push or history
replacement is needed for this organization.

```text
rakshyaa/                 repository root
  .git/
  README.md
  AGENTS.md
  docs/                  all guides, API reference, and release documentation
    archive/             historical notes
  rakshyaa/              Android Gradle project
  backend/               active Cloudflare Worker package
    src/worker/
    migrations/
    tests/
    legacy/              inactive Express/Docker source
  admin/                 Next.js operator portal
  assets/                branding artwork
```

Component READMEs and agent instructions remain beside their source. There is
one `docs/` hub; do not recreate a `doc/` directory or nested component docs hubs.

Open [rakshyaa.code-workspace](../rakshyaa.code-workspace) in VS Code to use a
single repository root. If an editor was open while the Git root moved, reload
that workspace to discard the stale Android-only repository entry. Do not
initialize a second `.git` directory inside the Android project.

## Clone and work on the project

```powershell
git clone https://github.com/proobker/rakshyaa.git
Set-Location rakshyaa
git remote -v
git status --short --branch
```

From this root, enter `backend/` or `admin/` for npm commands and `rakshyaa/`
for Gradle. See [setup and configuration](README.md). Existing clones created
before the monorepo change will place Android under `rakshyaa/` after updating.
Preserve local configuration and signing files before relocating them into the
new Android directory; do not commit them. Open that inner directory in Android
Studio. Existing backend/admin local configuration remains with each component.

## Publish further changes

Fetch the current branch and review local work before updating. Use a feature
branch and a pull request for normal contributions. Maintainers with permission
can push reviewed commits to the intended branch. Never force-push to work around
a rejected update; fetch and reconcile concurrent changes.

From the repository root, review the intended paths and ignore behavior before
staging them. For example, for a backend/admin/documentation change:

```powershell
git check-ignore -v backend/.dev.vars backend/.env admin/.env.local rakshyaa/.release/signing.properties rakshyaa/backend.properties
git add --dry-run backend admin docs
git add backend admin docs
git diff --cached --stat
git diff --cached
git diff --cached --check
```

Stage other intended changes explicitly. Review the staged contents for secrets,
generated files, personal data, and accidental nested repositories. Ignore rules
do not remove files already tracked. After the relevant checks pass:

```powershell
git commit -m "Describe the reviewed change"
git push
```

The checkout branch must have the intended upstream. Confirm with
`git status --short --branch` and `git remote -v` before pushing. Follow
[GitHub's source publication guidance](https://docs.github.com/en/migrations/importing-source-code/using-the-command-line-to-import-source-code/adding-locally-hosted-code-to-github)
when publishing a separate fork or repository. Backend `private: true` in
`package.json` prevents npm publication, not public GitHub source hosting.

## What belongs in the public repository?

| Publish | Keep private or generated locally |
| --- | --- |
| Source, migrations, tests, scripts, dependency lockfiles | `node_modules/`, build/cache output, local databases |
| Placeholder `.dev.vars.example` and `.env.example` | `.dev.vars`, `.env`, `.env.local`, environment variants |
| Android build scripts and backend properties example | `backend.properties`, `local.properties`, signing inputs |
| Documentation and reviewed artwork | Production logs, tokens, personal records/media |
| Verified release APK and checksum as release assets | Keystores, signing passwords, database exports |

The admin source is safe to publish only with its real operator key kept out of
the code. `NEXT_PUBLIC_BACKEND_URL` is public build configuration; do not add
`ADMIN_API_KEY` or another secret to a `NEXT_PUBLIC_` variable. Keep JWT and
Cloudinary credentials in local private files/provider secret storage.

The D1 database ID, backend origin, and OAuth client IDs are identifiers, not
login credentials. Fork operators must replace environment-specific values.
The repository root and all three component manifests use MIT. Dependency
licenses remain governed by their own notices.

## Source publication versus hosting

A public repository does not start the backend or grant operator access.
Use [backend hosting](BACKEND_SELF_HOSTING.md) and [admin setup](ADMIN_SETUP.md)
for deployment, and [signed builds](SIGNED_ANDROID_APP.md) for app distribution.
[GitHub Pages](https://docs.github.com/en/pages/getting-started-with-github-pages/what-is-github-pages)
serves static content; it does not run this Worker or a `next start` server.

After publication, verify the pushed commit and its component directories on
GitHub, and test a fresh source checkout. If a credential was published, rotate
it and address repository history; deleting the current file alone is insufficient.
