# Documentation

This is the single documentation hub for the Android app, Cloudflare Worker,
and operator portal in [proobker/rakshyaa](https://github.com/proobker/rakshyaa).
The root [README](../README.md) introduces the project; component READMEs provide
short entry points beside their source.

## Start here

1. [Android setup](ANDROID_SETUP.md): build the default local app without backend credentials.
2. [Backend self-hosting](BACKEND_SELF_HOSTING.md): provision your own Cloudflare/D1 and Cloudinary environment.
3. [Operator portal setup](ADMIN_SETUP.md): configure and host the admin UI.
4. [Signed Android app](SIGNED_ANDROID_APP.md): signing keys, APK/AAB builds, verification, and distribution.
5. [GitHub publication](GITHUB_PUBLISHING.md): clone, contribute, and publish source from the combined repository.

## Setup and operation

| Guide | Purpose |
| --- | --- |
| [Configuration](CONFIGURATION.md) | Local files, deployed secrets, and build-time client settings |
| [Google sign-in](GOOGLE_SIGN_IN.md) | OAuth audience, package, and signing-certificate configuration |
| [Emulator setup](EMULATOR_SETUP.md) | Start an AVD and diagnose startup problems |
| [Backend deployment checklist](BACKEND_DEPLOYMENT.md) | Review and roll out an existing environment |
| [Operations](OPERATIONS.md) | Monitoring, recovery, migrations, and credential rotation |
| [Troubleshooting](TROUBLESHOOTING.md) | Symptoms and component-specific checks |

## Implementation reference

| Guide | Purpose |
| --- | --- |
| [Technology inventory](TECHNOLOGY_STACK.md) | Languages, libraries, services, toolchains, and versions |
| [Architecture](ARCHITECTURE.md) | Sessions, persistence, sync, and component boundaries |
| [API reference](API.md) | Worker routes, authentication, payloads, and limits |
| [Security and data](SECURITY.md) | Encryption scope, server-readable data, and deletion |
| [Android implementation map](ANDROID_IMPLEMENTATION.md) | Feature ownership and remaining qualifications |
| [Known limitations](KNOWN_LIMITATIONS.md) | Source-backed implementation gaps |

## Testing and releases

| Guide | Purpose |
| --- | --- |
| [Testing](TESTING.md) | Automated commands and manual acceptance scenarios |
| [Release process](RELEASE.md) | Release decisions, checks, and evidence |
| [Release readiness](RELEASE_READINESS.md) | Actual validation results and unverified acceptance work |
| [Release notes](RELEASE_NOTES.md) | Changes represented by the source |
| [Follow-up plan](PLAN.md) | Remaining work and completion criteria |
| [Historical rebuild milestone](archive/REBUILD_MILESTONE.md) | Historical context, not current setup instructions |

Keep new cross-component guides in this directory and link them here. Archive
historical narratives under `docs/archive/`; keep runnable component scripts with
their component. The inactive backend is explicitly archived in
[backend/legacy](../backend/legacy/README.md), and branding assets are described in
[assets](../assets/README.md).

The supported backend runs under your own Cloudflare and Cloudinary accounts;
a fully independent VPS port requires code changes. GitHub hosts source and
release assets, not the running API. A signed local app needs no cloud credentials.
Encrypted backups do not provide cross-device key recovery.

Commands use PowerShell unless stated otherwise; use `./gradlew` on POSIX. Run
npm and Gradle from the specified component directory. Follow an active RTK
wrapper policy when one applies to your environment. For code changes, read
the root [agent instructions](../AGENTS.md) and the relevant component guidance.
