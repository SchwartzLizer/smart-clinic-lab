# Screenshot evidence

These are real, sanitized captures from isolated local Smart Clinic demo and repository GitHub Actions. They show synthetic demo data only; no real patient data, bearer token, password, provider credential, connection string, account profile content, or live-cloud UI is included.

| File | Observed evidence |
| --- | --- |
| [landing-login.png](landing-login.png) | Smart Clinic landing page and role-entry/login path in local demo. |
| [admin-dashboard.png](admin-dashboard.png) | Authenticated Admin dashboard and doctor-directory management view. |
| [doctor-search.png](doctor-search.png) | Patient-facing doctor search/filter view. |
| [appointment-flow.png](appointment-flow.png) | Same-page patient appointment booking result using synthetic demo data. |
| [doctor-dashboard.png](doctor-dashboard.png) | Doctor Portal scheduled-appointment table with a synthetic scheduled visit. |
| [swagger-ui.png](swagger-ui.png) | Local Swagger UI/OpenAPI contract rendering. |
| [github-actions.png](github-actions.png) | Successful post-merge GitHub Actions checks for application image/backend/frontend, frontend lint, Java verification/lint, and Dockerfile lint. |

## Capture and redaction policy

- Capture only pages or CI runs that completed successfully.
- Use synthetic, disposable local-demo data; never capture real patient or personal data.
- Remove or avoid bearer tokens, passwords, provider credentials, connection strings, non-demo email addresses, and account/avatar/profile content.
- Treat this set as local runtime and repository-CI evidence. It does not show live cloud deployment, provider console, production performance, SLA, compliance, or commercial use.

See project context and reproduction steps in root [README](../../README.md).
