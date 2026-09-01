# Phase 3 release-readiness gate

This workflow prepares a reviewable release candidate and verifies a manually deployed demo. It never deploys, publishes an image, changes a provider, reads secrets, or gives GitHub a provider token.

## What `Phase 3 Release Readiness` does

`workflow_dispatch` is the only trigger. It runs only when started from the repository default `main` branch.

1. Builds `app/Dockerfile` exactly once for `linux/amd64`, tagged with the full source commit SHA.
2. Saves that built image to `smart-clinic-linux-amd64.tar.gz`, records Docker `imageId` and archive SHA-256, and writes a `release-manifest.json`.
3. Uploads only the manifest, manifest hash, and image archive as a GitHub Actions artifact retained for seven days.
4. Waits at a protected GitHub Environment named `demo`, then runs an unauthenticated smoke check against an already deployed HTTPS root URL.

The manifest records `schemaVersion`, `sourceCommit`, `imageId`, `platform`, `archiveSha256`, `workflowRunId`, `workflowRunAttempt`, and `expectedAppVersion`. Its no-secret contents also appear in the GitHub Actions job summary for review without downloading the image archive. It does not claim that an uploaded GitHub artifact has a cryptographic artifact digest.

## Required GitHub Environment setup

Create a GitHub Environment called `demo` before using the workflow:

1. Add at least one required reviewer.
2. Restrict deployment branches to `main`.
3. Keep Environment secrets and variables empty. The workflow does not require them.
4. For a single-owner repository, a required reviewer may still be the same person. Treat this as a deliberate approval checkpoint, not independent peer review.

Repository Environment configuration is a GitHub setting; this repository file cannot create or enforce it.

## Human release and rollback sequence

1. Dispatch `Phase 3 Release Readiness` from `main`. Enter only exact public HTTPS root URL. Do not include credentials, query string, fragment, path, `localhost`, or private/reserved literal IP address. Workflow binds revision/version directly to candidate outputs and fixes environment to `cloud`.
2. Download and retain the candidate artifact. Record `sourceCommit`, `imageId`, archive SHA-256, and current Render deploy ID/source SHA before changing Render.
3. In Render, manually deploy the exact `sourceCommit`. Set `APP_REVISION` to that commit, `APP_VERSION` to the manifest `expectedAppVersion`, and `APP_ENVIRONMENT=cloud` through managed environment values.
4. Approve the `demo` environment only after the manual Render deployment completes. The smoke gate checks liveness, readiness, and safe `/actuator/info` metadata.
5. If smoke fails, use Render deploy history to manually roll back to the recorded previous successful deploy. Re-run readiness and `/actuator/info` after rollback. Do not edit an already-applied Flyway migration.

The GitHub candidate artifact and a Render-rebuilt binary are not provably identical: Render builds its own Docker image from the selected source commit. The artifact proves CI build inputs and Docker image metadata only. A same-binary deployment would require a separately approved registry or provider-image deployment design.

## Smoke checker contract

`scripts/smoke-demo.mjs` uses Node.js 20 with no third-party dependencies. It sends unauthenticated requests with no cookie, authorization, or provider headers; follows no redirects; logs no response body, header, credential, resolved address, raw URL, or environment dump; resolves hostname at connection time and rejects private, loopback, link-local, multicast, reserved, or mixed DNS answers; and stops JSON stream before reading more than 16 KiB.

It requires HTTP 200 and JSON from:

- `/actuator/health/liveness` with `status=UP`
- `/actuator/health/readiness` with `status=UP`
- `/actuator/info` with exact `app.revision`, `app.version`, `app.environment=cloud`, `app.data=synthetic`, and `app.sla=none`

It applies a per-request timeout of smaller of ten seconds or remaining budget, and six-minute global deadline checked before each request, while streaming, and before/after retry sleep. It retries transport failures, HTTP 408/425/429/500/502/503/504, non-`UP` readiness, and temporarily stale revision. Redirects, authentication failures, invalid or oversized JSON, unsafe DNS answers, unexpected sensitive fields, and other metadata mismatches fail immediately.
