# Free-tier deployment runbook

This runbook uses only free plans. It does not create provider accounts, accept provider terms, or handle secrets for you.

## 1. Prepare the application

1. Fork or push this repository to GitHub.
2. Generate a random JWT secret of at least 32 UTF-8 bytes (for example, a password-manager generated value).
3. Keep provider credentials in your password manager and paste them only into provider dashboards. Never commit `.env`.

## 2. Aiven Free MySQL

1. Create a free MySQL service and database.
2. Copy the provided host, port, database, username, and password into Render environment variables.
3. Build `DB_URL` as the Aiven TLS JDBC URL supplied by Aiven, retaining its required `sslMode=REQUIRED`/certificate parameters. Do not copy a connection string into Git.
4. After the first deploy, verify Flyway created the schema history and the `admin`, `doctor`, `patient`, and `appointment` tables.

## 3. MongoDB Atlas M0

1. Create an M0 cluster and a least-privilege database user for this application.
2. Restrict Network Access to the deployed application's permitted egress where your provider plan supports a stable rule. For a temporary smoke test, use the narrowest documented alternative and remove it afterward.
3. Copy the Atlas SRV URI into Render as `MONGODB_URI`; keep credentials URL-encoded and out of repository files.
4. After the first prescription workflow, verify one synthetic document exists in the `prescriptions` collection.

## 4. Render Free web service

1. In Render, create a Blueprint from `render.yaml` or create a Docker Web Service manually.
2. Set `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `MONGODB_URI`, and `JWT_SECRET` as secret environment values. Keep `SPRING_PROFILES_ACTIVE=default` so production does not seed demo data.
3. Keep the health check at `/actuator/health/readiness` and deploy from the portfolio branch.
4. Confirm the service is healthy, open Swagger UI, and run the documented admin → doctor → patient → appointment → prescription journey with synthetic data.

## Free-tier limits and rollback

Render Free can sleep and cold-start. Aiven Free and Atlas M0 have small storage, connection, throughput, and availability limits; they are suitable for a demo, not production workloads. Monitor logs for the correlation ID and do not enable request-header logging.

For rollback, use Render's deploy history to redeploy the last known-good image. If a migration needs correction, add a new Flyway migration; do not edit an applied migration in place. Rebuild disposable local volumes with `docker compose down --volumes` only when local data may be discarded.
