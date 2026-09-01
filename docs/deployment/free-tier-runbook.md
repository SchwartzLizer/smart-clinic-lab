# Free-tier deployment runbook

This runbook uses only free plans. It does not create provider accounts, accept provider terms, or handle secrets for you.

## 1. Prepare the application

1. Fork or push this repository to GitHub.
2. Generate a random JWT secret of at least 32 UTF-8 bytes (for example, a password-manager generated value).
3. Keep provider credentials in your password manager and paste them only into provider dashboards. Never commit `.env`.

## 2. Aiven Free MySQL

1. Create a free MySQL service and database.
2. Create two database identities: a least-privilege runtime identity for the application and a separate migration identity for Flyway. Do not reuse their usernames.
3. Build `DB_URL` and `FLYWAY_DB_URL` as Aiven TLS JDBC URLs supplied by Aiven, retaining required `sslMode=REQUIRED`/certificate parameters. Do not copy connection strings into Git.
4. Add `DB_USERNAME`/`DB_PASSWORD` for the runtime identity and `FLYWAY_DB_USERNAME`/`FLYWAY_DB_PASSWORD` for the migration identity in Render.
5. After a deploy you perform, verify Flyway created the schema history and the `admin`, `doctor`, `patient`, and `appointment` tables. The cloud migration track intentionally creates no demo rows.

## 3. MongoDB Atlas M0

1. Create an M0 cluster and a least-privilege database user for this application.
2. Restrict Network Access to the deployed application's permitted egress where your provider plan supports a stable rule. For a temporary smoke test, use the narrowest documented alternative and remove it afterward.
3. Copy the Atlas SRV URI into Render as `MONGODB_URI`; keep credentials URL-encoded and out of repository files.
4. After a prescription workflow you perform, verify the `prescriptions` collection contains its expected document and the `appointmentId` unique index.

## 4. Render Free web service

1. In Render, create a Blueprint from `render.yaml` or create a Docker Web Service manually.
2. Set `SPRING_PROFILES_ACTIVE=cloud`. Supply `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `FLYWAY_DB_URL`, `FLYWAY_DB_USERNAME`, `FLYWAY_DB_PASSWORD`, `MONGODB_URI`, `JWT_SECRET`, `CORS_ALLOWED_ORIGINS`, `APP_VERSION`, and `APP_REVISION` through Render's managed environment values; keep credentials and the JWT secret secret. Keep runtime and Flyway usernames distinct; use exact public HTTPS origins for `CORS_ALLOWED_ORIGINS`, never `*` or localhost. `APP_ENVIRONMENT=cloud`, `JWT_EXPIRATION`, and `JAVA_TOOL_OPTIONS` are declared in the blueprint.
3. Keep the health check at `/actuator/health/readiness`. The blueprint has automatic deploys disabled, so select and initiate any deployment yourself.
4. Cloud disables `/v3/api-docs` and Swagger UI. The `cloud` profile also disables demo data. Run bootstrap with exactly one application instance; multi-instance coordination is deferred to backlog B03. For a first authenticated portfolio journey, set every `APP_BOOTSTRAP_*` value in Render's managed environment, set `APP_BOOTSTRAP_ENABLED=true`, and deploy one selected source commit. Bootstrap accepts an empty database or an exact safe rerun only; it never repairs existing accounts. After the startup log reports only `admin=created|existing` and `doctor=created|existing`, verify both identities, set `APP_BOOTSTRAP_ENABLED=false`, and wait for a healthy restart. Only then remove both bootstrap password values and verify the next restart remains healthy. Do not put identifiers or passwords in Git, screenshots, tickets, or command history.
5. Before a portfolio demonstration, follow [release-readiness.md](release-readiness.md): retain the release-candidate manifest and previous Render deploy reference, deploy an exact source commit manually, then approve the protected GitHub `demo` smoke gate. This workflow does not deploy or access provider credentials.

This repository run does not create provider accounts, enter secrets, deploy the service, or bootstrap cloud identities. It therefore makes no claim about a live URL or provider-side validation.

## Free-tier limits and rollback

Render Free can sleep and cold-start. Aiven Free and Atlas M0 have small storage, connection, throughput, and availability limits; they are suitable for a demo, not production workloads. Monitor logs for the correlation ID and do not enable request-header logging.

For rollback, first record the previous successful Render deploy ID and source SHA. If the protected demo smoke check fails, use Render's deploy history to redeploy that recorded successful deploy, then re-check readiness and `/actuator/info`. If a migration needs correction, add a new Flyway migration; do not edit an applied migration in place. Rebuild disposable local volumes with `docker compose down --volumes` only when local data may be discarded.
