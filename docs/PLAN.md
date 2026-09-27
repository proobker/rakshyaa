# Follow-up plan

The Android, Worker, and admin implementations are present. This is a follow-up
plan based on the 2026-09-26 documentation audit, not a claim that the application
is ready for unrestricted emergency use. No application fixes below were made
as part of the Markdown-only audit.

## 1. Make emergency status truthful and durable

- Correct check-in foreground startup, completion/cancellation coordination, and
  timer restoration. Add tests for cold start, completed-before-expiry, cancellation,
  process restart, and permission denial.
- Align missed-check-in text with the actual delivery mechanism. Completion
  requires observable delivery state or explicit user-controlled message preparation.
- Define incident retry/reconciliation behavior for offline create and resolve.
  Verify the local/cloud states converge without duplicating incident IDs.
- Define the intended ride-deviation model and connect the chosen threshold to
  the actual recording/alert path. Verify it against representative routes.

Evidence and relevant source links: [known limitations](KNOWN_LIMITATIONS.md).

## 2. Complete data lifecycle behavior

- Define restore conflict handling so stale remote data cannot silently replace
  newer unsynced local records.
- Implement or explicitly exclude failed-media retries and remote deletion from
  the product contract. Test local success alongside cloud failure.
- Preserve intended custom profile fields across Google re-sign-in and support
  clearing fields through the Android form.
- Enforce or revise notification/location preferences so controls match behavior.
- Define plaintext-cache cleanup and validate account switches, logout, and deletion.
- Keep cross-device recovery out of feature claims unless a real key-recovery
  design and tests are delivered.

Completion requires tests for account isolation, lost-key restore, disabled
backup, partial deletion failure, and the documented user-visible states.

## 3. Prepare a maintainable cloud environment

- Reconcile existing D1 schema with the current migration, including account generations.
- Expand Worker coverage to Google failure paths, profile behavior, admin access,
  account deletion/re-registration, multipart cleanup, and upstream failures.
- Keep the archived `backend/legacy/` code clearly separate from the active
  Worker; any future VPS port needs its own implementation and verification.
- Define operator access, incident pagination if needed, retention, and support contacts.
- Review sample resources, locale handling, and licensing for the intended distribution.

Use [backend deployment](BACKEND_DEPLOYMENT.md) and [security](SECURITY.md)
as the current implementation references.

## 4. Produce release evidence

Run [automated and manual tests](TESTING.md), build the intended signed
artifact, record checksums/configuration, and complete physical-device and
accessibility acceptance. Verify production OAuth, storage, and deletion only
against a disposable test account in the chosen environment.

Update [release readiness](RELEASE_READINESS.md) with actual outcomes and
remaining blockers; follow [release process](RELEASE.md) for distribution.
