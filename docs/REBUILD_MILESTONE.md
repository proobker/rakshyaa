# Historical rebuild milestone

This is historical context for the September 2026 rebuild. It is not a current
setup guide, deployment status, or certification of every feature.

## Earlier rebuild

Earlier project notes recorded replacement of stale Supabase-oriented code with
Kotlin/Compose/Hilt repositories, device-bound encrypted storage, Google Credential
Manager sign-in, and a Node/Express/SQLite backend. They reported emulator
authentication and APK/test success in that development environment.

That implementation introduced the four manifest services and six injected
helpers, the backend-token exchange, account feature repositories, and restored
feature screens. Restore-on-login orchestration was placed in AuthViewModel to
avoid the AuthRepository/AppDataSync/ProfileRepository dependency cycle.

## Subsequent source changes

The reviewed checkout now has:

- Local-device login and optional cloud mode.
- Hono on Cloudflare Workers, D1 migrations, jose, and authenticated Cloudinary
  ciphertext chunks as the active backend.
- Account-scoped local files, account-generation sessions, and confirmed deletion.
- API 36 targeting, current signing validation, and debug-only cleartext exceptions.
- Explicit SMS composition/dialer actions rather than automatic emergency calls.
- SOS countdown cancellation coverage and an in-memory-key operator dashboard.

## Historical verification limits

The previous release report described signed APK/AAB generation, signature and
alignment checks, local-login/emulator smoke tests, and a custom-domain DNS failure
during Google release testing. It also referenced local signing material and
`release-qa/` evidence outside the maintained documentation set.

Those reports are not rerun by editing documentation, and their old test/lint
totals are not current acceptance evidence. Developer-specific OAuth IDs,
fingerprints, AVD settings, and artifact hashes must be derived from the intended
environment/artifact rather than copied from a historical note.

Use [release readiness](../../RELEASE_READINESS.md) for the audit's actual check
results, [local setup](../LOCAL_SETUP.md) for reproducible instructions, and
[known limitations](../../docs/KNOWN_LIMITATIONS.md) for source gaps.
