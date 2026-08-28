# Smart Clinic Spring Boot Portfolio Design

## Objective

Transform the existing Coursera Smart Clinic capstone into a recruiter-ready Spring Boot portfolio project. The finished repository must demonstrate secure authentication, clean REST API design, relational and document database integration, automated tests, enforceable CI, one-command local startup, observable runtime behavior, and a live demo that uses free infrastructure only.

The project remains a modular monolith. It will not add microservices, messaging, payments, chat, or other unrelated product features.

## Success Criteria

1. A new developer can clone the repository, copy `.env.example` to `.env`, and start the complete local stack with `docker compose up --build`.
2. REST authentication uses Spring Security, BCrypt password hashes, stateless JWT authorization, and role checks for `ADMIN`, `DOCTOR`, and `PATIENT`.
3. Tokens are accepted only through the `Authorization: Bearer <token>` header; tokens never appear in URL paths.
4. Controllers exchange DTOs rather than persistence entities and return consistent RFC 9457 `ProblemDetail` errors.
5. MySQL schema changes and demo relational data are versioned with Flyway; MongoDB demo data is seeded idempotently under the `demo` profile.
6. Unit, MVC, and Testcontainers integration tests pass with `./mvnw verify`.
7. Java, JavaScript, HTML, CSS, Docker, and Maven verification failures block CI.
8. Swagger UI, Actuator health information, architecture documentation, screenshots, and runnable API examples are available from the repository.
9. A free Render web service connects to free Aiven MySQL and MongoDB Atlas M0 without secrets committed to Git.

## Constraints

- Keep Spring Boot `3.4.4` and Java `17` during this upgrade. A framework-major upgrade is outside scope.
- Preserve the existing Smart Clinic domain: admins, doctors, patients, appointments, availability, and prescriptions.
- Preserve the existing Thymeleaf/static frontend as the visual demo, but update it to the new API and security contract.
- Use only free deployment tiers: Render Free web service, Aiven Free MySQL, and MongoDB Atlas M0.
- Do not require a credit card for the selected managed database services.
- Never commit real credentials, generated JWT secrets, provider tokens, or production connection strings.
- Keep the existing Coursera assignment evidence and SQL stored procedures accessible in repository history and documentation.

## Architecture

The application stays as one Spring Boot process with three internal layers:

1. Web layer: Thymeleaf/static pages, REST controllers, DTO validation, Spring Security filter chain, and `ProblemDetail` exception mapping.
2. Application layer: services containing authorization-aware business rules and transaction boundaries.
3. Data layer: Spring Data JPA repositories backed by MySQL and a Spring Data MongoDB repository for prescriptions.

The same Docker image runs locally and on Render. Local Docker Compose supplies MySQL and MongoDB containers. Render uses externally managed Aiven MySQL and MongoDB Atlas connections through environment variables.

## Phase 1: Reproducible Local Environment

### Files and configuration

- Add root `compose.yaml` containing `app`, `mysql`, and `mongodb` services.
- Add root `.env.example` documenting every required variable without usable secrets.
- Keep `.env` ignored by Git.
- Convert `application.properties` to environment-backed values:
  - `DB_URL`
  - `DB_USERNAME`
  - `DB_PASSWORD`
  - `MONGODB_URI`
  - `JWT_SECRET`
  - `JWT_EXPIRATION`
  - `SPRING_PROFILES_ACTIVE`
- Add health checks for MySQL, MongoDB, and the application.
- Make the application container wait on healthy database services through Compose dependency conditions.

### Database initialization

- Add Flyway and convert the MySQL schema, stored procedures, and safe demo data into ordered migrations.
- Store BCrypt hashes, never plaintext passwords, in demo migrations.
- Add an idempotent MongoDB demo initializer enabled only by the `demo` profile.
- Retain a documented mapping from the original assignment SQL files to the Flyway migrations.

### Local command

The supported local path is:

```bash
copy .env.example .env
docker compose up --build
```

The README will include PowerShell and Bash forms of the environment-file copy step.

## Phase 2: Spring Security and JWT

### Dependencies

- Add `spring-boot-starter-security`.
- Continue using JJWT already present in the project.

### Authentication design

- Create a stateless `SecurityFilterChain` with CSRF disabled for the JSON API and session creation set to stateless.
- Add a `PasswordEncoder` using BCrypt.
- Add a `OncePerRequestFilter` that validates bearer tokens and creates an authenticated principal containing account ID and role.
- Generate JWTs containing subject, account ID, role, issued-at time, and expiration.
- Require a runtime secret of at least 32 bytes.
- Return authentication results through a typed `AuthResponse` containing token, token type, expiration, role, and account ID.

### Authorization rules

- Public:
  - frontend pages and static assets
  - Swagger UI and OpenAPI JSON
  - `/actuator/health`
  - patient registration
  - admin, doctor, and patient login
  - read-only doctor directory and filters
- `ADMIN`:
  - create, update, and delete doctors
- `DOCTOR`:
  - view assigned appointments
  - read patient details required for an appointment
  - create and view prescriptions
- `PATIENT`:
  - view own profile
  - book, update, view, and cancel own appointments

Ownership checks remain in services so a valid role cannot access another user's records.

### Frontend migration

- Update all `fetch` calls to send `Authorization: Bearer <token>`.
- Remove token segments from URLs and MVC routes.
- Keep browser storage limited to the demo token and non-sensitive role/account identifiers.
- Clear stored authentication data on `401` and redirect to the landing page.

## Phase 3: Professional REST API

### Endpoint contract

Use `/api` as the common prefix:

- `POST /api/auth/admin/login`
- `POST /api/auth/doctors/login`
- `POST /api/auth/patients/login`
- `POST /api/patients`
- `GET /api/patients/me`
- `GET /api/doctors`
- `POST /api/doctors`
- `PUT /api/doctors/{doctorId}`
- `DELETE /api/doctors/{doctorId}`
- `GET /api/appointments`
- `POST /api/appointments`
- `PUT /api/appointments/{appointmentId}`
- `DELETE /api/appointments/{appointmentId}`
- `GET /api/prescriptions/{appointmentId}`
- `POST /api/prescriptions`

Doctor filtering uses query parameters such as `name`, `specialty`, `period`, `page`, `size`, and `sort`. Appointment filtering uses `date`, `patientName`, and `status` where authorized.

### DTOs and mapping

- Add dedicated request and response DTOs for authentication, doctors, patients, appointments, prescriptions, and pagination.
- Validate requests at the controller boundary with Jakarta Validation.
- Map entities to DTOs through small explicit mapper classes; do not add a mapping framework.
- Never serialize passwords, repository internals, or persistence relationships directly.

### Errors

- Add domain exceptions for not found, conflict, forbidden ownership, and invalid credentials.
- Add one `@RestControllerAdvice` that produces `ProblemDetail` with status, title, detail, instance, timestamp, and field-validation errors.
- Use stable HTTP semantics: `201` create, `200` read/update, `204` delete, `400` validation, `401` unauthenticated, `403` unauthorized, `404` missing, and `409` conflict.

### Documentation

- Add `springdoc-openapi-starter-webmvc-ui` `2.8.13`, the compatible Spring Boot 3.4.x line. During implementation, `2.9.0` was rejected because it fails application context startup with Spring Boot 3.4.4's `PathPatternParser`.
- Document bearer authentication, DTO schemas, filters, pagination, and error responses.

## Phase 4: Tests and Enforceable CI

### Test strategy

- Unit tests: service business rules, token parsing, password encoding, ownership checks, and DTO mappers.
- MVC slice tests: authentication, authorization, validation, status codes, and `ProblemDetail` bodies.
- Integration tests: MySQL and MongoDB through Testcontainers with Flyway migrations applied.
- Browser-facing smoke tests: landing page and dashboard resource availability.
- Preserve and update the existing assignment-criteria tests.

### Dependencies and build

- Add Testcontainers JUnit Jupiter, MySQL, and MongoDB test modules.
- Add JaCoCo and enforce at least 70% line coverage for application classes, excluding generated configuration and DTO accessors.
- `./mvnw verify` becomes the canonical backend verification command.

### CI workflows

- Backend workflow runs `./mvnw --batch-mode verify` on Java 17.
- Remove every `|| true` from lint workflows.
- Add a checked-in frontend `package.json` with pinned HTMLHint, Stylelint, and ESLint development dependencies.
- Run frontend lint with `npm ci` and fail on violations.
- Run Checkstyle through Maven with a repository-owned configuration and fail on violations.
- Keep Hadolint as a blocking Dockerfile check.
- Build the Docker image in CI after tests and lint pass.

## Phase 5: Observability, Documentation, and Free Deployment

### Runtime visibility

- Add Spring Boot Actuator.
- Expose only `health` and `info` publicly.
- Configure liveness and readiness health groups.
- Include database health in readiness without exposing credentials or connection details.
- Use structured application logs with request correlation IDs; do not log passwords or bearer tokens.

### Repository presentation

Replace the template README with a portfolio-focused document containing:

- product problem and supported roles
- key backend capabilities
- technology stack
- architecture Mermaid diagram
- database ER diagram
- local startup instructions
- demo workflow and safe demo accounts
- Swagger and Actuator URLs
- test and CI commands
- screenshots
- API request/response examples
- security decisions and trade-offs
- deployment architecture and free-tier limitations

Screenshots will show landing/login, role dashboards, doctor search, appointment flow, Swagger UI, and passing GitHub Actions.

### Free cloud deployment

- Render Free hosts the Docker image with 512 MB RAM.
- Set `JAVA_TOOL_OPTIONS=-XX:MaxRAMPercentage=70.0` for the free memory limit.
- Configure Render health checks against `/actuator/health/readiness`.
- Aiven Free MySQL supplies the relational database with TLS enabled.
- MongoDB Atlas M0 supplies the prescriptions database with network access restricted to the deployed application where the provider permits stable egress rules.
- All provider credentials are entered through provider dashboards and Render environment variables.
- Free-tier sleep, cold-start, storage, and availability limitations are disclosed in the README.

Provider account creation, accepting provider terms, and entering secret values remain user-controlled actions. Codex may guide and verify them but will not accept terms or expose secrets in chat.

## Migration and Compatibility

- Existing frontend behavior is migrated endpoint by endpoint before old token-in-path routes are removed.
- Existing database records require a one-time password reset or a controlled demo-data rebuild because plaintext passwords cannot be converted into BCrypt hashes without knowing the original passwords.
- Demo deployments use a clean database initialized by Flyway and the demo Mongo initializer.
- The old assignment endpoint list remains documented in `ASSIGNMENT-ANSWERS.md`; it is not retained as an insecure runtime compatibility layer.

## Verification Gate

The portfolio upgrade is complete only when all conditions hold:

1. `./mvnw clean verify` passes from a clean checkout.
2. Frontend lint and JavaScript syntax checks pass without ignored failures.
3. `docker compose up --build` reaches healthy state for all three services.
4. Automated API tests prove all public and role-protected routes.
5. Tokens are absent from request URLs, logs, repository files, and screenshots.
6. Git history for the upgrade contains no new secrets.
7. GitHub Actions pass on the final commit.
8. Render, Aiven MySQL, and MongoDB Atlas run the demo successfully on free tiers.
9. README links, screenshots, Swagger UI, and health endpoint are verified from the public repository and live deployment.

## Out of Scope

- Microservices or service discovery
- Kafka, RabbitMQ, or asynchronous messaging
- Payment processing
- Real patient data or healthcare compliance claims
- Mobile applications
- Kubernetes
- Paid infrastructure
- Spring Boot 4 or Java 21 migration
