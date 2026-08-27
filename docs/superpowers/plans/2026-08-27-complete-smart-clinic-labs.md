# Smart Clinic Remaining Labs Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace template placeholders with a compilable Smart Clinic backend and add Docker/CI artifacts required by assignment Q5-Q12.

**Architecture:** Keep existing Spring Boot MVC/REST structure. Controllers validate roles and delegate to services; services own business rules; JPA and Mongo repositories own persistence.

**Tech Stack:** Java 17, Spring Boot 3.4, Spring MVC, Spring Data JPA, Spring Data MongoDB, JWT, Maven, Docker, GitHub Actions

**Spec:** IBM Skills Network lab files `lab-instructions (1).md` through `lab-instructions (6).md`

## Global Constraints

- Preserve existing frontend and model work.
- Implement only behaviors named by the six labs and Q5-Q12.
- Use constructor injection and structured `ResponseEntity` responses.
- Trigger CI on push and pull request.

---

### Task 1: DTOs and repositories

**Files:** `app/src/main/java/com/project/back_end/DTO/*.java`, `repo/*.java`

- [ ] Replace placeholder comments with DTO fields/getters and Spring Data repository interfaces.
- [ ] Compile to verify query method/property names.

### Task 2: Services and MVC

**Files:** `app/src/main/java/com/project/back_end/services/*.java`, `mvc/DashboardController.java`

- [ ] Implement token, login, availability, appointment, patient, and prescription flows.
- [ ] Add focused tests for assignment-required methods and annotations.

### Task 3: REST controllers

**Files:** `app/src/main/java/com/project/back_end/controllers/*.java`

- [ ] Implement role validation and structured responses for all lab endpoints.
- [ ] Verify Q5 and Q7 endpoint signatures through tests.

### Task 4: Docker and CI

**Files:** `app/Dockerfile`, `.github/workflows/*.yml`

- [ ] Add multi-stage Java 17 Dockerfile.
- [ ] Add frontend, backend, Maven compile, and Docker lint workflows.

### Task 5: Evidence and delivery

**Files:** `ASSIGNMENT-ANSWERS.md`

- [ ] Record public links and verified Q19-Q23 outputs.
- [ ] Run Maven tests/compile, inspect Git diff, commit, and push main.
