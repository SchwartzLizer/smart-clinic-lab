# Smart Clinic Spring Boot Portfolio Implementation Plan

> **Execution requirement:** Follow this plan task by task using test-driven development. Run each stated failing test before adding production code, then run the stated green verification before committing.

**Goal:** Upgrade the existing Coursera Smart Clinic project into a secure, reproducible, recruiter-ready Spring Boot backend portfolio that runs locally with Docker Compose and can be deployed using only Render Free, Aiven Free MySQL, and MongoDB Atlas M0.

**Architecture:** Keep one Spring Boot 3.4.4 modular monolith. HTTP and Thymeleaf/static resources form the web layer, services own authorization-aware business rules, JPA repositories use MySQL, and the prescription repository uses MongoDB. Spring Security creates an authenticated principal from a bearer JWT; controllers accept DTOs only; Flyway owns relational schema changes; the `demo` profile owns idempotent MongoDB seed data.

**Tech stack:** Java 17, Spring Boot 3.4.4, Spring Security, Spring Data JPA, Spring Data MongoDB, Flyway, JJWT 0.12.6, springdoc-openapi 2.8.13, MySQL 8.4, MongoDB 7, Testcontainers, JUnit 5, Mockito, MockMvc, JaCoCo, Maven Wrapper, Docker Compose, ESLint, HTMLHint, Stylelint, GitHub Actions.

**Approved design:** `docs/superpowers/specs/2026-08-28-spring-boot-portfolio-design.md`

**Canonical verification command:** Run `./mvnw clean verify` from `app/` on Bash or `./mvnw.cmd clean verify` from `app/` on PowerShell.

**Commit discipline:** Commit only the files listed in each task. Never stage `.env`, provider credentials, generated JWTs, screenshots containing tokens, or unrelated user changes.

---

## Phase 1 — Reproducible runtime and data

### Task 1: Preserve the isolated baseline context test

**Files:**

- Modify: `app/src/test/java/com/project/back_end/BackEndApplicationTests.java` (already changed and verified locally)

1. Review the existing diff and confirm it contains only `@MockitoBean` repository replacements and exclusions for JDBC, JPA, and Mongo auto-configuration. Do not edit production configuration in this task.
2. Run:

   ```powershell
   Set-Location app
   .\mvnw.cmd -Dtest=BackEndApplicationTests test
   .\mvnw.cmd test
   ```

   Expected: the context test passes; the complete existing suite reports 11 tests with zero failures and zero errors.
3. Commit only the test:

   ```powershell
   git add app/src/test/java/com/project/back_end/BackEndApplicationTests.java
   git commit -m "test: isolate application context from external databases"
   ```

### Task 2: Add portfolio dependencies and environment-backed configuration

**Files:**

- Modify: `app/pom.xml`
- Modify: `app/src/main/resources/application.properties`
- Create: `app/src/main/resources/application-demo.properties`
- Create: `app/src/test/resources/application-test.properties`
- Create: `app/src/test/java/com/project/back_end/config/ConfigurationContractTests.java`

1. Write `ConfigurationContractTests` with `ApplicationContextRunner` assertions proving:
   - `JWT_SECRET` shorter than 32 bytes prevents the token configuration bean from starting.
   - `jwt.expiration` binds to a positive duration.
   - `test` profile configuration disables demo seeding.
2. Run `app\mvnw.cmd -Dtest=ConfigurationContractTests test` and record the expected compilation failure because the typed configuration properties do not exist yet.
3. Add these dependencies to `app/pom.xml`:
   - `spring-boot-starter-security`
   - `spring-boot-starter-actuator`
   - `org.flywaydb:flyway-core`
   - `org.flywaydb:flyway-mysql`
   - `org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.13` (2.9.0 is incompatible with Boot 3.4.4 startup)
   - test-scoped `org.testcontainers:junit-jupiter`, `mysql`, and `mongodb`
4. Add `maven-failsafe-plugin` for `*IT.java` integration tests and `jacoco-maven-plugin` with `prepare-agent`, `report`, and a `verify` check. Start the coverage minimum at `0.00` in this phase so structural migration can proceed; Task 13 raises it to `0.70` after the new behavior tests exist.
5. Replace literal placeholders in `application.properties` with:

   ```properties
   spring.datasource.url=${DB_URL:jdbc:mysql://localhost:3306/smart_clinic}
   spring.datasource.username=${DB_USERNAME:smart_clinic}
   spring.datasource.password=${DB_PASSWORD:smart-clinic-local-only}
   spring.jpa.hibernate.ddl-auto=validate
   spring.jpa.open-in-view=false
   spring.flyway.enabled=true
   spring.data.mongodb.uri=${MONGODB_URI:mongodb://smart_clinic:smart-clinic-local-only@localhost:27017/prescriptions?authSource=admin}
   jwt.secret=${JWT_SECRET:smart-clinic-local-demo-signing-key-change-before-cloud-use}
   jwt.expiration=${JWT_EXPIRATION:PT1H}
   spring.profiles.active=${SPRING_PROFILES_ACTIVE:demo}
   management.endpoints.web.exposure.include=health,info
   management.endpoint.health.probes.enabled=true
   ```

   Treat the committed local defaults as public demo values, never as production credentials.
6. Add `application-demo.properties` with `app.demo-data.enabled=true`. Add `application-test.properties` with `app.demo-data.enabled=false` and test-safe logging.
7. Add typed `@ConfigurationProperties` records/classes under `com.project.back_end.config.properties`:

   ```java
   @ConfigurationProperties("jwt")
   public record JwtProperties(@Size(min = 32) String secret, @DurationMin(seconds = 1) Duration expiration) {}
   ```

   Enable validation and configuration-properties scanning from `BackEndApplication`.
8. Run `app\mvnw.cmd -Dtest=ConfigurationContractTests test`; expected: green.
9. Run `app\mvnw.cmd test`; expected: existing tests remain green.
10. Commit:

   ```powershell
   git add app/pom.xml app/src/main/resources app/src/test/resources app/src/main/java/com/project/back_end/config app/src/main/java/com/project/back_end/BackEndApplication.java app/src/test/java/com/project/back_end/config
   git commit -m "build: add portfolio runtime dependencies and configuration"
   ```

### Task 3: Move MySQL schema and demo data to Flyway

**Files:**

- Create: `app/src/main/resources/db/migration/V1__create_smart_clinic_schema.sql`
- Create: `app/src/main/resources/db/migration/V2__create_stored_procedures.sql`
- Create: `app/src/main/resources/db/migration/V3__seed_demo_accounts.sql`
- Modify: `database/mysql/README.md`
- Create: `app/src/test/java/com/project/back_end/database/MySqlMigrationIT.java`

1. Write `MySqlMigrationIT` using `@Testcontainers` and `MySQLContainer<?>`. Run Flyway directly against `container.getJdbcUrl()` so this focused migration test does not require MongoDB. Assert that:
   - Flyway reports exactly three successful migrations.
   - `admins`, `doctors`, `patients`, and `appointments` exist.
   - every seeded account password starts with `$2a$`, `$2b$`, or `$2y$`.
   - the stored procedures from `database/mysql/03-stored-procedures.sql` exist.
2. Run `app\mvnw.cmd -Dit.test=MySqlMigrationIT verify`; expected: fail because migrations do not exist. If Docker is unavailable, keep the test and note the environment block without marking it green.
3. Convert the existing SQL in order:
   - `01-schema.sql` → `V1__create_smart_clinic_schema.sql`
   - `03-stored-procedures.sql` → `V2__create_stored_procedures.sql`
   - safe rows from `02-seed-data.sql` → `V3__seed_demo_accounts.sql`
4. Replace plaintext demo passwords with pre-generated BCrypt hashes for documented local-only passwords. Do not place a real or reused password in the repository.
5. Make the migration names and columns match the existing JPA mappings. Set Hibernate to `validate`; never restore `ddl-auto=update`.
6. Create `database/mysql/README.md` mapping each course SQL artifact to its Flyway equivalent and explaining that the original files remain assignment evidence, not runtime initialization.
7. Run `app\mvnw.cmd -Dit.test=MySqlMigrationIT verify`; expected: green when Docker is available.
8. Commit:

   ```powershell
   git add app/src/main/resources/db app/src/test/java/com/project/back_end/database database/mysql/README.md
   git commit -m "feat: version MySQL schema and demo data with Flyway"
   ```

### Task 4: Add idempotent Mongo seed and the local Docker stack

**Files:**

- Create: `app/src/main/java/com/project/back_end/config/DemoMongoDataInitializer.java`
- Create: `app/src/test/java/com/project/back_end/config/DemoMongoDataInitializerTests.java`
- Create: `compose.yaml`
- Create: `.env.example`
- Modify: `.gitignore`
- Create: `app/Dockerfile`
- Create: `app/.dockerignore`

1. Write `DemoMongoDataInitializerTests` with a mocked `PrescriptionRepository`. Prove:
   - seeding runs only when `app.demo-data.enabled=true`.
   - `findByAppointmentId` prevents a duplicate seed.
   - an existing document is not overwritten.
2. Run `app\mvnw.cmd -Dtest=DemoMongoDataInitializerTests test`; expected: compilation failure because the initializer does not exist.
3. Implement `DemoMongoDataInitializer` as an `ApplicationRunner` guarded by `@ConditionalOnProperty(name="app.demo-data.enabled", havingValue="true")`. Use stable appointment IDs and insert only when the repository has no prescription for that appointment.
4. Run the initializer test; expected: green.
5. Add `.env.example` containing public local-demo values for `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `MONGODB_URI`, `JWT_SECRET`, `JWT_EXPIRATION`, and `SPRING_PROFILES_ACTIVE`. Add `.env` to `.gitignore`.
6. Create a multi-stage `app/Dockerfile`: Maven/Temurin 17 build stage, Temurin 17 JRE runtime, non-root user, exposed port 8080, and `java -jar /app/app.jar`. Add only Maven source/build inputs to `app/.dockerignore`.
7. Create root `compose.yaml` with:
   - `mysql:8.4` and `mongo:7.0` containers.
   - named volumes for both databases.
   - health checks using `mysqladmin ping` and `mongosh --eval "db.adminCommand('ping')"`.
   - `app` built from `app/Dockerfile`, environment variables from `.env`, and `depends_on` conditions requiring healthy databases.
   - application health check against `http://localhost:8080/actuator/health/readiness`.
8. Run:

   ```powershell
   Copy-Item .env.example .env
   docker compose config
   docker compose up --build --wait
   docker compose ps
   Invoke-RestMethod http://localhost:8080/actuator/health/readiness
   docker compose down
   ```

   Expected: configuration resolves, all three services become healthy, readiness returns `UP`. If Docker is unavailable, report this as unverified and do not claim Compose passed.
9. Commit:

   ```powershell
   git add .env.example .gitignore compose.yaml app/Dockerfile app/.dockerignore app/src/main/java/com/project/back_end/config/DemoMongoDataInitializer.java app/src/test/java/com/project/back_end/config/DemoMongoDataInitializerTests.java
   git commit -m "feat: add reproducible Docker Compose development stack"
   ```

---

## Phase 2 — Spring Security and JWT

### Task 5: Replace repository-coupled tokens with typed JWT principals

**Files:**

- Create: `app/src/main/java/com/project/back_end/security/Role.java`
- Create: `app/src/main/java/com/project/back_end/security/AuthenticatedUser.java`
- Modify: `app/src/main/java/com/project/back_end/services/TokenService.java`
- Create: `app/src/test/java/com/project/back_end/services/TokenServiceTests.java`

1. Write `TokenServiceTests` with a fixed `Clock`. Prove:
   - generated tokens contain subject, `accountId`, `role`, `iat`, and `exp`.
   - `parse` returns `AuthenticatedUser(accountId, subject, role)`.
   - expired, malformed, wrongly signed, or unsupported-role tokens are rejected.
   - secrets shorter than 32 bytes fail construction.
2. Run `app\mvnw.cmd -Dtest=TokenServiceTests test`; expected: fail because the new API is absent.
3. Create:

   ```java
   public enum Role { ADMIN, DOCTOR, PATIENT }

   public record AuthenticatedUser(Long accountId, String subject, Role role) {}
   ```

4. Refactor `TokenService` so it depends only on `JwtProperties` and `Clock`, with this contract:

   ```java
   public String generateToken(Long accountId, String subject, Role role)
   public AuthenticatedUser parse(String token)
   public boolean isValid(String token)
   public Instant expiresAt(String token)
   ```

   Remove all repository lookups and the user-string switch from token validation.
5. Run the token tests; expected: green.
6. Run `app\mvnw.cmd test`; expected: legacy tests still compile or are updated only where the token signature changed.
7. Commit:

   ```powershell
   git add app/src/main/java/com/project/back_end/security app/src/main/java/com/project/back_end/services/TokenService.java app/src/test/java/com/project/back_end/services/TokenServiceTests.java
   git commit -m "feat: issue typed role-aware JWTs"
   ```

### Task 6: Add the stateless Spring Security filter chain

**Files:**

- Create: `app/src/main/java/com/project/back_end/security/JwtAuthenticationFilter.java`
- Create: `app/src/main/java/com/project/back_end/security/RestAuthenticationEntryPoint.java`
- Create: `app/src/main/java/com/project/back_end/security/RestAccessDeniedHandler.java`
- Create: `app/src/main/java/com/project/back_end/config/SecurityConfig.java`
- Create: `app/src/test/java/com/project/back_end/config/SecurityConfigTests.java`

1. Write `SecurityConfigTests` with `MockMvc` and mocked controllers/services. Prove:
   - static resources, frontend pages, OpenAPI, Swagger UI, `/actuator/health/**`, `/actuator/info`, doctor GET, registration, and login are public.
   - protected API calls without a bearer token return RFC 9457 JSON with `401`.
   - an authenticated wrong role returns `403`.
   - a valid token creates a principal whose authority is `ROLE_ADMIN`, `ROLE_DOCTOR`, or `ROLE_PATIENT`.
2. Run the test; expected: protected routes are not secured or new classes are missing.
3. Implement `JwtAuthenticationFilter` as a `OncePerRequestFilter`. Read only `Authorization: Bearer ...`; never accept query/path tokens; never log the header. On valid tokens, create a `UsernamePasswordAuthenticationToken` whose principal is `AuthenticatedUser`.
4. Implement `SecurityConfig` with:
   - CSRF ignored for `/api/**` only.
   - stateless session management.
   - BCrypt `PasswordEncoder`.
   - public matchers from the approved spec.
   - role matchers for doctor administration, doctor workflows, and patient-owned workflows.
   - filter insertion before `UsernamePasswordAuthenticationFilter`.
5. Return `ProblemDetail` JSON from both the authentication entry point and access denied handler.
6. Run `SecurityConfigTests`; expected: green.
7. Commit:

   ```powershell
   git add app/src/main/java/com/project/back_end/security app/src/main/java/com/project/back_end/config/SecurityConfig.java app/src/test/java/com/project/back_end/config/SecurityConfigTests.java
   git commit -m "feat: enforce stateless bearer authentication"
   ```

### Task 7: Add BCrypt-backed login and registration

**Files:**

- Create: `app/src/main/java/com/project/back_end/dto/auth/AdminLoginRequest.java`
- Create: `app/src/main/java/com/project/back_end/dto/auth/UserLoginRequest.java`
- Create: `app/src/main/java/com/project/back_end/dto/auth/AuthResponse.java`
- Create: `app/src/main/java/com/project/back_end/dto/patient/PatientRegistrationRequest.java`
- Create: `app/src/main/java/com/project/back_end/services/AuthService.java`
- Create: `app/src/main/java/com/project/back_end/controllers/AuthController.java`
- Modify: `app/src/main/java/com/project/back_end/services/PatientService.java`
- Modify: `app/src/main/java/com/project/back_end/controllers/PatientController.java`
- Modify: `app/src/main/java/com/project/back_end/repo/AdminRepository.java`
- Modify: `app/src/main/java/com/project/back_end/repo/DoctorRepository.java`
- Modify: `app/src/main/java/com/project/back_end/repo/PatientRepository.java`
- Create: `app/src/test/java/com/project/back_end/services/AuthServiceTests.java`
- Create: `app/src/test/java/com/project/back_end/controllers/AuthControllerTests.java`

1. Write service tests proving all three login types use `PasswordEncoder.matches`, return the correct role/account ID, and return the same invalid-credentials exception for missing accounts and wrong passwords. Prove registration calls `encode` before saving and rejects duplicate email or phone.
2. Write `@WebMvcTest` tests for the three login endpoints and patient registration. Assert typed success bodies, validation errors, `401` invalid credentials, `409` duplicate registration, and absence of `password` in every response.
3. Run both tests; expected: red because the new API does not exist.
4. Make repository single-result lookups return `Optional<T>` to avoid null-based authentication. Add explicit existence methods needed for duplicate registration.
5. Implement validated records:

   ```java
   public record AdminLoginRequest(@NotBlank String username, @NotBlank String password) {}
   public record UserLoginRequest(@Email String email, @NotBlank String password) {}
   public record AuthResponse(String token, String tokenType, Instant expiresAt, Role role, Long accountId) {}
   ```

6. Implement `AuthService.authenticateAdmin`, `authenticateDoctor`, `authenticatePatient`, and patient registration with BCrypt. Never return or log a submitted password.
7. Add `POST /api/auth/admin/login`, `POST /api/auth/doctors/login`, `POST /api/auth/patients/login`, and `POST /api/patients`.
8. Run the focused tests; expected: green. Run `app\mvnw.cmd test`; expected: green after updating assignment tests to the intentional `/api` contract only when their assertions conflict.
9. Commit:

   ```powershell
   git add app/src/main/java/com/project/back_end/dto app/src/main/java/com/project/back_end/services/AuthService.java app/src/main/java/com/project/back_end/services/PatientService.java app/src/main/java/com/project/back_end/controllers/AuthController.java app/src/main/java/com/project/back_end/controllers/PatientController.java app/src/main/java/com/project/back_end/repo app/src/test/java/com/project/back_end/services/AuthServiceTests.java app/src/test/java/com/project/back_end/controllers/AuthControllerTests.java app/src/test/java/com/project/back_end/AssignmentCriteriaTests.java
   git commit -m "feat: add BCrypt login and patient registration"
   ```

---

## Phase 3 — Professional REST API

### Task 8: Establish DTO mapping and RFC 9457 errors

**Files:**

- Create: `app/src/main/java/com/project/back_end/dto/common/PageResponse.java`
- Create: `app/src/main/java/com/project/back_end/dto/doctor/DoctorCreateRequest.java`
- Create: `app/src/main/java/com/project/back_end/dto/doctor/DoctorUpdateRequest.java`
- Create: `app/src/main/java/com/project/back_end/dto/doctor/DoctorResponse.java`
- Create: `app/src/main/java/com/project/back_end/dto/patient/PatientResponse.java`
- Create: `app/src/main/java/com/project/back_end/dto/appointment/AppointmentCreateRequest.java`
- Create: `app/src/main/java/com/project/back_end/dto/appointment/AppointmentUpdateRequest.java`
- Create: `app/src/main/java/com/project/back_end/dto/appointment/AppointmentResponse.java`
- Create: `app/src/main/java/com/project/back_end/dto/prescription/PrescriptionCreateRequest.java`
- Create: `app/src/main/java/com/project/back_end/dto/prescription/PrescriptionResponse.java`
- Create: `app/src/main/java/com/project/back_end/mappers/DoctorMapper.java`
- Create: `app/src/main/java/com/project/back_end/mappers/PatientMapper.java`
- Create: `app/src/main/java/com/project/back_end/mappers/AppointmentMapper.java`
- Create: `app/src/main/java/com/project/back_end/mappers/PrescriptionMapper.java`
- Create: `app/src/main/java/com/project/back_end/exceptions/ResourceNotFoundException.java`
- Create: `app/src/main/java/com/project/back_end/exceptions/ResourceConflictException.java`
- Create: `app/src/main/java/com/project/back_end/exceptions/ForbiddenOperationException.java`
- Create: `app/src/main/java/com/project/back_end/exceptions/InvalidCredentialsException.java`
- Create: `app/src/main/java/com/project/back_end/exceptions/GlobalExceptionHandler.java`
- Delete after replacement and explicit deletion approval: `app/src/main/java/com/project/back_end/controllers/ValidationFailed.java`
- Create: `app/src/test/java/com/project/back_end/mappers/DtoMapperTests.java`
- Create: `app/src/test/java/com/project/back_end/exceptions/GlobalExceptionHandlerTests.java`

1. Write mapper tests that ensure passwords and persistence back-references never appear in response DTOs. Verify nested doctor/patient summaries use IDs and display fields only.
2. Write MockMvc tests proving all four domain exceptions and validation failures return `application/problem+json` with `status`, `title`, `detail`, `instance`, `timestamp`, and validation `errors` when applicable.
3. Run focused tests; expected: compilation failure.
4. Implement immutable record DTOs with Jakarta Validation at request boundaries. Use `PageResponse<T>` fields `content`, `page`, `size`, `totalElements`, and `totalPages`.
5. Implement small explicit mapper components; do not add MapStruct or another dependency.
6. Implement one `@RestControllerAdvice`. After its behavior is covered, request explicit approval before deleting `ValidationFailed`. Map status codes to `400`, `401`, `403`, `404`, and `409` as specified.
7. Run focused tests; expected: green. Run all unit tests.
8. Commit:

   ```powershell
   git add app/src/main/java/com/project/back_end/dto app/src/main/java/com/project/back_end/mappers app/src/main/java/com/project/back_end/exceptions app/src/main/java/com/project/back_end/controllers/ValidationFailed.java app/src/test/java/com/project/back_end/mappers app/src/test/java/com/project/back_end/exceptions
   git commit -m "feat: add typed API DTOs and ProblemDetail errors"
   ```

### Task 9: Implement the doctor directory and admin management API

**Files:**

- Modify: `app/src/main/java/com/project/back_end/controllers/DoctorController.java`
- Modify: `app/src/main/java/com/project/back_end/services/DoctorService.java`
- Modify: `app/src/main/java/com/project/back_end/repo/DoctorRepository.java`
- Create: `app/src/test/java/com/project/back_end/services/DoctorServiceTests.java`
- Create: `app/src/test/java/com/project/back_end/controllers/DoctorControllerTests.java`

1. Write service tests for duplicate email, create/update/delete, missing doctor, normalized name/specialty filters, AM/PM availability filtering, pagination, and password hashing on create/update.
2. Write MVC tests for:
   - public `GET /api/doctors?name=&specialty=&period=&page=&size=&sort=`.
   - `ADMIN`-only create/update/delete.
   - `201`, `200`, `204`, `400`, `403`, `404`, and `409` responses.
   - no password in any doctor response.
3. Run focused tests; expected: red against legacy paths and entity responses.
4. Replace path filters with optional query parameters and a `Pageable`. Keep sorting allow-listed to `name`, `specialty`, and `id` to prevent accidental persistence-field exposure.
5. Refactor `DoctorService` to throw typed exceptions, return DTOs/PageResponse, encode passwords, and use transactions. Remove `ResponseEntity` and `Map<String,Object>` from the service.
6. Replace controller routes with the approved contract: public `GET /api/doctors`, plus admin-only `POST /api/doctors`, `PUT /api/doctors/{doctorId}`, and `DELETE /api/doctors/{doctorId}`. Use `@PreAuthorize("hasRole('ADMIN')")` for mutations.
7. Run focused tests and then `app\mvnw.cmd test`; expected: green.
8. Commit:

   ```powershell
   git add app/src/main/java/com/project/back_end/controllers/DoctorController.java app/src/main/java/com/project/back_end/services/DoctorService.java app/src/main/java/com/project/back_end/repo/DoctorRepository.java app/src/test/java/com/project/back_end/services/DoctorServiceTests.java app/src/test/java/com/project/back_end/controllers/DoctorControllerTests.java
   git commit -m "feat: expose paginated doctor management API"
   ```

### Task 10: Implement patient-owned appointment workflows

**Files:**

- Modify: `app/src/main/java/com/project/back_end/controllers/PatientController.java`
- Modify: `app/src/main/java/com/project/back_end/controllers/AppointmentController.java`
- Delete after replacement and explicit deletion approval: `app/src/main/java/com/project/back_end/controllers/AdminController.java`
- Modify: `app/src/main/java/com/project/back_end/services/PatientService.java`
- Modify: `app/src/main/java/com/project/back_end/services/AppointmentService.java`
- Delete after replacement and explicit deletion approval: `app/src/main/java/com/project/back_end/services/Service.java`
- Delete after replacement and explicit deletion approval: `app/src/main/java/com/project/back_end/DTO/Login.java`
- Delete after replacement and explicit deletion approval: `app/src/main/java/com/project/back_end/DTO/AppointmentDTO.java`
- Modify: `app/src/main/java/com/project/back_end/repo/AppointmentRepository.java`
- Create: `app/src/test/java/com/project/back_end/services/PatientServiceTests.java`
- Create: `app/src/test/java/com/project/back_end/services/AppointmentServiceTests.java`
- Create: `app/src/test/java/com/project/back_end/controllers/PatientControllerTests.java`
- Create: `app/src/test/java/com/project/back_end/controllers/AppointmentControllerTests.java`

1. Write service tests proving:
   - `PATIENT` can read only their own profile and appointments.
   - `PATIENT` cannot update/cancel another patient's appointment.
   - `DOCTOR` sees only appointments assigned to their doctor account.
   - booking rejects missing doctors, unavailable slots, duplicate slots, and past times.
   - status/date/patient-name filters apply only within the authorized scope.
2. Write MVC tests for `GET /api/patients/me` and the four `/api/appointments` methods. Include unauthenticated `401`, wrong-role `403`, wrong-owner `403`, create `201`, update/read `200`, delete `204`, and validation `400`.
3. Run focused tests; expected: red.
4. Read `AuthenticatedUser` through `@AuthenticationPrincipal`; never accept patient ID, doctor ID, role, or token from a request when it can be derived from the principal.
5. Refactor both services to return DTOs and throw typed exceptions. Keep ownership checks inside the transaction before any mutation.
6. Replace legacy token-in-path endpoints with:
   - `GET /api/patients/me`
   - `GET /api/appointments`
   - `POST /api/appointments`
   - `PUT /api/appointments/{appointmentId}`
   - `DELETE /api/appointments/{appointmentId}`
7. Run `rg -n "services\.Service|DTO\.Login|DTO\.AppointmentDTO|new Login|AppointmentDTO" app/src`. After every caller has moved to focused services/DTOs and all covered behavior is green, request explicit approval before deleting `AdminController.java`, `services/Service.java`, `DTO/Login.java`, and `DTO/AppointmentDTO.java`. Delete only files proven unused.
8. Run focused tests, then all tests.
9. Commit:

   ```powershell
   git add app/src/main/java/com/project/back_end/controllers app/src/main/java/com/project/back_end/services app/src/main/java/com/project/back_end/DTO app/src/main/java/com/project/back_end/repo/AppointmentRepository.java app/src/test/java/com/project/back_end/services app/src/test/java/com/project/back_end/controllers
   git commit -m "feat: enforce ownership in patient appointment workflows"
   ```

### Task 11: Implement doctor-authorized prescriptions

**Files:**

- Modify: `app/src/main/java/com/project/back_end/controllers/PrescriptionController.java`
- Modify: `app/src/main/java/com/project/back_end/services/PrescriptionService.java`
- Modify: `app/src/main/java/com/project/back_end/repo/PrescriptionRepository.java`
- Create: `app/src/test/java/com/project/back_end/services/PrescriptionServiceTests.java`
- Create: `app/src/test/java/com/project/back_end/controllers/PrescriptionControllerTests.java`
- Create: `app/src/test/java/com/project/back_end/database/MongoPrescriptionIT.java`

1. Write service tests proving a doctor can read/create a prescription only for their assigned appointment, duplicate prescription creation returns `409`, missing appointment returns `404`, and a successful save changes appointment status to completed.
2. Write MVC tests for `GET /api/prescriptions/{appointmentId}` and `POST /api/prescriptions`, including `401`, `403`, `404`, `409`, `200`, and `201`.
3. Write `MongoPrescriptionIT` with `MongoDBContainer` and `@DynamicPropertySource`; verify save and `findByAppointmentId` against a real Mongo instance.
4. Run focused unit/MVC tests; expected: red.
5. Refactor the service to verify the JPA appointment and doctor ownership before writing MongoDB. Save the prescription first, then update appointment status. Document in the service JavaDoc and README that MySQL and MongoDB do not share an atomic transaction; a retry remains safe because duplicate detection is by appointment ID.
6. Return DTOs only and replace legacy token path routes with the approved routes.
7. Run unit/MVC tests; expected: green. Run `app\mvnw.cmd -Dit.test=MongoPrescriptionIT verify`; expected: green when Docker is available.
8. Commit:

   ```powershell
   git add app/src/main/java/com/project/back_end/controllers/PrescriptionController.java app/src/main/java/com/project/back_end/services/PrescriptionService.java app/src/main/java/com/project/back_end/repo/PrescriptionRepository.java app/src/test/java/com/project/back_end/services/PrescriptionServiceTests.java app/src/test/java/com/project/back_end/controllers/PrescriptionControllerTests.java app/src/test/java/com/project/back_end/database/MongoPrescriptionIT.java
   git commit -m "feat: secure prescription workflows by doctor ownership"
   ```

### Task 12: Publish OpenAPI documentation

**Files:**

- Create: `app/src/main/java/com/project/back_end/config/OpenApiConfig.java`
- Modify: all controllers under `app/src/main/java/com/project/back_end/controllers/`
- Create: `app/src/test/java/com/project/back_end/config/OpenApiContractTests.java`

1. Write a MockMvc integration test that loads `/v3/api-docs` and asserts:
   - all approved endpoints are present.
   - the `bearerAuth` security scheme uses HTTP bearer/JWT.
   - public routes have no security requirement.
   - protected routes declare `bearerAuth`.
   - DTO schemas do not expose a password response field.
2. Run the test; expected: fail because the OpenAPI contract is incomplete.
3. Add `OpenApiConfig` with project title, version, description, and bearer scheme. Add concise operation, response, filter, pagination, and error annotations where generated metadata is insufficient.
4. Run `OpenApiContractTests`; expected: green.
5. Commit:

   ```powershell
   git add app/src/main/java/com/project/back_end/config/OpenApiConfig.java app/src/main/java/com/project/back_end/controllers app/src/test/java/com/project/back_end/config/OpenApiContractTests.java
   git commit -m "docs: publish authenticated OpenAPI contract"
   ```

---

## Phase 4 — Frontend migration, tests, and enforceable CI

### Task 13: Move the frontend to bearer headers and token-free routes

**Files:**

- Modify: `app/src/main/java/com/project/back_end/mvc/DashboardController.java`
- Modify: `app/src/main/resources/static/js/config/config.js`
- Create: `app/src/main/resources/static/js/services/httpClient.js`
- Modify: all files under `app/src/main/resources/static/js/services/`
- Modify: callers under `app/src/main/resources/static/js/`
- Modify: `app/src/main/resources/templates/admin/adminDashboard.html`
- Modify: `app/src/main/resources/templates/doctor/doctorDashboard.html`
- Create: `app/src/test/js/httpClient.test.mjs`

1. Write dependency-free `node:test` cases for the planned HTTP helper. Prove it:
   - adds `Authorization: Bearer <token>` when a token exists.
   - never places a token in a URL.
   - preserves JSON headers.
   - clears `token`, `userRole`, `accountId`, and `doctorId` on `401` and redirects to `/`.
2. Run `node --test app/src/test/js/httpClient.test.mjs`; expected: fail because `httpClient.js` does not exist.
3. Implement `httpClient.js` as the single wrapper for protected `fetch` calls. Keep `config.js` same-origin by setting `API_BASE_URL = "/api"`; do not hard-code localhost or a cloud hostname.
4. Update every service module to the approved token-free endpoint paths. Remove token parameters from exported functions and callers.
5. Change login redirects to `/adminDashboard` and `/doctorDashboard`. Make `DashboardController` return these pages without path tokens; Spring Security handles page access policy.
6. Run:

   ```powershell
   node --test app/src/test/js/httpClient.test.mjs
   rg -n "\{token\}|/\$\{token\}|Dashboard/\$\{.*token|localhost:8080" app/src/main app/src/test
   ```

   Expected: Node tests pass and `rg` returns no matches in runtime code.
7. Run backend tests and manually smoke the landing page, admin dashboard, doctor dashboard, doctor search, patient registration/login, booking, and prescription flow when Compose is available.
8. Commit:

   ```powershell
   git add app/src/main/java/com/project/back_end/mvc/DashboardController.java app/src/main/resources/static/js app/src/main/resources/templates app/src/test/js
   git commit -m "feat: migrate frontend to bearer-authenticated API"
   ```

### Task 14: Expand integration coverage and enforce the 70% gate

**Files:**

- Create: `app/src/test/java/com/project/back_end/api/SmartClinicApiIT.java`
- Modify: `app/src/test/java/com/project/back_end/AssignmentCriteriaTests.java`
- Modify: `app/src/test/java/com/project/back_end/BackEndApplicationTests.java`
- Modify: `app/src/test/java/com/project/back_end/models/ModelValidationTests.java`
- Modify: `app/pom.xml`

1. Create `SmartClinicApiIT` using shared MySQL and Mongo Testcontainers plus `TestRestTemplate`. Execute this journey:
   - login as seeded admin.
   - create a doctor.
   - register and login a patient.
   - list/filter doctors.
   - book and update the patient's appointment.
   - login as the doctor and read only assigned appointments.
   - create and retrieve a prescription.
   - verify a second patient cannot read/update the first patient's appointment.
2. Run `app\mvnw.cmd -Dit.test=SmartClinicApiIT verify`; expected: red until fixture and endpoint integration is complete.
3. Update existing course tests to assert the intentional new endpoint/DTO/security architecture while retaining checks for all original domain models, repositories, services, controllers, static pages, and SQL evidence.
4. Add focused tests until application line coverage reaches at least 70%. Exclude only configuration wiring, the main application class, DTO-generated accessors, and exceptions without branches; do not exclude services or controllers.
5. Raise the JaCoCo line minimum from `0.00` to `0.70`.
6. Run `app\mvnw.cmd clean verify`; expected: all unit, MVC, and Testcontainers tests pass and the coverage gate passes. If Docker is unavailable, unit/MVC results may be reported separately, but `verify` is not considered complete.
7. Commit:

   ```powershell
   git add app/src/test app/pom.xml
   git commit -m "test: cover secure Smart Clinic API end to end"
   ```

### Task 15: Make every CI check blocking and reproducible

**Files:**

- Create: `app/config/checkstyle/checkstyle.xml`
- Create: `package.json`
- Create: `package-lock.json`
- Create: `.eslintrc.cjs`
- Create: `.stylelintrc.json`
- Create: `.htmlhintrc`
- Modify: `.github/workflows/compile-backend.yml`
- Modify: `.github/workflows/lint-backend.yml`
- Modify: `.github/workflows/lint-frontend.yml`
- Modify: `.github/workflows/lint-docker.yml`
- Create: `.github/workflows/build-image.yml`

1. Add Maven Checkstyle with a repository-owned configuration and bind it to `verify`. Start from the current code style, then fix violations rather than suppressing whole packages.
2. Add a root `package.json` with exact development versions `eslint@8.57.1`, `htmlhint@1.6.3`, `stylelint@16.12.0`, and `stylelint-config-standard@36.0.1`. Add scripts `lint:js`, `lint:html`, `lint:css`, `test:js`, and aggregate `verify:frontend`. Generate and commit `package-lock.json` with `npm install --package-lock-only`. Use Node 20 in CI.
3. Run `npm ci` and `npm run verify:frontend`; expected: red on current violations. Fix only lint/syntax issues, not visual behavior.
4. Replace the backend compile workflow with `./mvnw --batch-mode clean verify`. Run Checkstyle through Maven. Remove every `|| true` and global npm install from the lint workflows.
5. Keep Hadolint blocking. Add a Docker image build workflow that runs only after backend and frontend verification jobs succeed.
6. Run locally:

   ```powershell
   Set-Location app
   .\mvnw.cmd clean verify
   Set-Location ..
   npm ci
   npm run verify:frontend
   docker build -f app/Dockerfile app
   rg -n "\|\| true" .github
   ```

   Expected: Maven, frontend verification, and image build pass; `rg` returns no matches. Report Docker as unverified if it is unavailable.
7. Commit:

   ```powershell
   git add app/config app/pom.xml package.json package-lock.json .eslintrc.cjs .stylelintrc.json .htmlhintrc .github app/src/main app/src/test
   git commit -m "ci: enforce backend frontend and container quality gates"
   ```

---

## Phase 5 — Observability, portfolio documentation, and free deployment

### Task 16: Add readiness, liveness, and correlation IDs

**Files:**

- Create: `app/src/main/java/com/project/back_end/observability/CorrelationIdFilter.java`
- Modify: `app/src/main/resources/application.properties`
- Create: `app/src/test/java/com/project/back_end/observability/CorrelationIdFilterTests.java`
- Create: `app/src/test/java/com/project/back_end/observability/HealthEndpointTests.java`

1. Write MockMvc tests proving `X-Correlation-ID` is echoed when supplied, generated when absent, attached to the response, and available through MDC during request processing. Prove bearer tokens never appear in captured logs.
2. Write health tests proving only `health` and `info` are exposed publicly, liveness excludes external databases, readiness includes `db` and `mongo`, and public responses do not expose credentials or connection strings.
3. Run focused tests; expected: red.
4. Implement `CorrelationIdFilter` with a UUID fallback and `try/finally` MDC cleanup. Configure the logging pattern to include the correlation ID but never request headers.
5. Configure:

   ```properties
   management.endpoints.web.exposure.include=health,info
   management.endpoint.health.probes.enabled=true
   management.endpoint.health.show-details=never
   management.endpoint.health.group.liveness.include=livenessState,ping
   management.endpoint.health.group.readiness.include=readinessState,db,mongo
   ```

6. Run focused tests and the full Maven suite; expected: green.
7. Commit:

   ```powershell
   git add app/src/main/java/com/project/back_end/observability app/src/main/resources/application.properties app/src/test/java/com/project/back_end/observability
   git commit -m "feat: expose safe health probes and correlation IDs"
   ```

### Task 17: Build the recruiter-facing README and deployment manifest

**Files:**

- Modify: `README.md`
- Create: `render.yaml`
- Create: `docs/architecture.md`
- Create: `docs/api-examples.md`
- Create: `docs/deployment/free-tier-runbook.md`
- Create: `docs/screenshots/README.md`

1. Replace the template README with these verified sections: product problem, roles, backend capabilities, stack, architecture Mermaid diagram, ER diagram, local startup for PowerShell/Bash, public local-demo accounts, demo workflow, Swagger URL, health URLs, test/lint commands, screenshots, API examples, security decisions, cross-database prescription trade-off, deployment architecture, and free-tier limits.
2. Add `docs/architecture.md` with module responsibilities and request/data-flow diagrams. Add `docs/api-examples.md` with copy-ready `curl` examples that use an environment variable for the bearer token and never put it in the URL.
3. Add `render.yaml` for a Docker-based free web service with `/actuator/health/readiness`, `JAVA_TOOL_OPTIONS=-XX:MaxRAMPercentage=70.0`, and environment-variable declarations using `sync: false` for `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `MONGODB_URI`, and `JWT_SECRET`. Do not embed provider values.
4. Add `docs/deployment/free-tier-runbook.md` with exact user-controlled steps for Aiven Free MySQL, MongoDB Atlas M0, and Render Free. Include TLS JDBC parameters, Atlas database user/network controls, secret generation, migration verification, sleep/cold-start/storage limitations, and rollback by Render deploy history.
5. Add `docs/screenshots/README.md` naming the required evidence files: `landing-login.png`, `admin-dashboard.png`, `doctor-search.png`, `appointment-flow.png`, `doctor-dashboard.png`, `swagger-ui.png`, and `github-actions.png`. Do not fabricate screenshots before the pages/actions exist.
6. Validate docs:

   ```powershell
   rg -n "<mysql_|<mongodb_|JWT_SECRET=.*[^=]$|Bearer eyJ|token.*(/|\\)" README.md docs render.yaml .env.example
   docker compose config
   ```

   Expected: no legacy placeholders, embedded JWTs, tokenized URLs, or provider secrets. `render.yaml` and Compose references are internally consistent.
7. Commit:

   ```powershell
   git add README.md render.yaml docs .env.example compose.yaml
   git commit -m "docs: present architecture API and free deployment runbook"
   ```

### Task 18: Final local, CI, and live-demo verification

**Files:**

- Add only verified images: `docs/screenshots/*.png`
- Modify documentation only if verification reveals an inaccurate command, URL, or claim

1. From a clean clone or clean worktree, run:

   ```powershell
   Set-Location app
   .\mvnw.cmd clean verify
   Set-Location ..
   npm ci
   npm run verify:frontend
   Copy-Item .env.example .env
   docker compose up --build --wait
   docker compose ps
   Invoke-RestMethod http://localhost:8080/actuator/health/readiness
   Invoke-RestMethod http://localhost:8080/v3/api-docs
   ```

2. Run the documented API journey with fresh local-demo data. Confirm role boundaries, ownership failures, `ProblemDetail` bodies, pagination, Flyway history, Mongo prescription persistence, and no bearer token in URLs/logs.
3. Inspect tracked content and history for secrets:

   ```powershell
   git grep -n -I -E "(Bearer eyJ|mongodb(\+srv)?://[^$]|jdbc:mysql://[^$]|BEGIN (RSA|OPENSSH|EC) PRIVATE KEY)"
   git status --short
   ```

   Expected: only documented local public values or environment-variable examples; `.env` remains untracked/ignored.
4. Push the implementation branch and wait for all GitHub Actions checks. Do not claim CI passes until the live checks are green.
5. The user creates/authorizes the Aiven, Atlas, and Render resources and enters secrets in provider dashboards. Verify remotely:
   - Render readiness is `UP`.
   - Swagger UI loads.
   - admin → doctor → patient → appointment → prescription journey succeeds.
   - Aiven shows Flyway-created tables.
   - Atlas shows the prescription document.
6. Capture the seven approved screenshots with all tokens, account passwords, provider credentials, and connection strings excluded or redacted. Add the images and update README links.
7. Run the full local verification again after documentation changes, then commit:

   ```powershell
   git add README.md docs/screenshots docs
   git commit -m "docs: add verified Smart Clinic portfolio evidence"
   ```
8. Push the evidence commit and verify CI once more. The project is complete only when every gate in the approved design's **Verification Gate** is backed by current output or a live URL.

---

## Execution order and stop conditions

1. Execute tasks in order; each later phase assumes the earlier API and security contracts are stable.
2. Stop and investigate any unexpected failure before changing production code. Do not weaken a test, coverage gate, lint rule, or security matcher merely to make a command green.
3. Docker-dependent tasks may continue with unit/MVC work on a host without Docker, but they remain explicitly unverified until rerun on a Docker-capable host or GitHub Actions runner.
4. Provider signup, acceptance of terms, secret entry, network allow-list changes, and deployment activation require the user's direct action or explicit approval.
5. Preserve the original Coursera SQL/evidence and unrelated repository changes throughout the upgrade.
