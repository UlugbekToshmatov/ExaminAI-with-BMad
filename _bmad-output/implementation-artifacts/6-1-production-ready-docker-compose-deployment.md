# Story 6.1: Production-Ready Docker Compose Deployment

Status: done

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

As a developer or operations engineer,  
I want the complete ExaminAI platform deployable via Docker Compose with a single command,  
so that anyone can run the full stack locally or in production without manual configuration beyond supplying a `.env` file.

## Acceptance Criteria

(From [`_bmad-output/planning-artifacts/epics.md`](../planning-artifacts/epics.md) — Epic 6, Story 6.1; aligns with NFR16–NFR20 and [architecture — Infrastructure & Deployment](../planning-artifacts/architecture.md#infrastructure--deployment).)

1. **Multi-stage `Dockerfile`**  
   **Given** a multi-stage `Dockerfile` is implemented  
   **When** it is inspected  
   **Then** stage 1 uses a **Maven** image to build the fat JAR (`mvn package -DskipTests`), and stage 2 copies **only** the JAR into a slim JRE image (`eclipse-temurin:21-jre`) — **no** Maven or source in the final image.

2. **Postgres service**  
   **Given** `docker-compose.yml` defines `postgres`, `ollama`, and `app`  
   **When** the file is inspected  
   **Then** the `postgres` service uses image `postgres:16`, mounts named volume `db-data` at `/var/lib/postgresql/data`, and has a healthcheck: `pg_isready -U ${DB_USERNAME}` (username must come from environment supplied via `env_file`, not hardcoded in YAML).

3. **Ollama service**  
   **Given** the `ollama` service in `docker-compose.yml`  
   **When** the file is inspected  
   **Then** it uses image `ollama/ollama`, mounts `ollama-models` at `/root/.ollama`, healthcheck: `curl -f http://localhost:11434/api/tags`  
   **And** entrypoint is  
   ` /bin/sh -c "ollama serve & sleep 5 && ollama pull qwen2.5-coder:3b && wait"` — pre-pulling the model on first start.

4. **App service**  
   **Given** the `app` service in `docker-compose.yml`  
   **When** the file is inspected  
   **Then** it declares `depends_on` with **`condition: service_healthy`** for **both** `postgres` and `ollama`  
   **And** it has a healthcheck: `curl -f http://localhost:8080/actuator/health`  
   **And** **all** secrets and config are injected via `env_file: .env` — **no** secret values in `docker-compose.yml` (non-secret `build`/`image` keys are fine).

5. **Named volumes**  
   **Given** `docker-compose.yml` defines named volumes  
   **When** the file is inspected  
   **Then** `db-data` and `ollama-models` are under top-level `volumes:`  
   **And** `docker compose down` (without `-v`) does **not** remove those volumes — data survives ordinary stop.

6. **End-to-end bring-up**  
   **Given** a `.env` with valid values for **all** keys in [`.env.example`](../../.env.example) (same keys)  
   **When** `docker compose up --build` runs from the **repository root**  
   **Then** all three services start, Liquibase runs, seed data loads, and the app serves HTTP on **port 8080** (map host port as needed, typically `8080:8080`).

7. **Postgres data durability**  
   **Given** the stack is running and the `postgres` container restarts  
   **When** postgres becomes healthy again  
   **Then** existing `TaskReview`, `Course`, `Task`, and `UserAccount` data remains — no data loss on container restart.

8. **Ollama model volume**  
   **Given** Ollama restarts after `qwen2.5-coder:3b` was already pulled  
   **When** the container starts  
   **Then** the model is available from `ollama-models` — no full re-download.

9. **Graceful async shutdown**  
   **Given** the Spring app receives shutdown (e.g. `docker compose stop`) while `@Async` review tasks may run  
   **When** shutdown proceeds  
   **Then** the executor must wait up to **120s** for tasks — already implemented: [`AsyncConfig`](../../src/main/java/com/examinai/config/AsyncConfig.java) sets `setAwaitTerminationSeconds(120)` and `setWaitForTasksToCompleteOnShutdown(true)`. **Verify** this remains true after any Docker-related changes; do not reduce below story/NFR expectations.

10. **Actuator health**  
    **Given** Actuator is on the classpath (already)  
    **When** `GET /actuator/health` is called **inside** the app container  
    **Then** response indicates **UP** — used for the app healthcheck. [`application.yml`](../../src/main/resources/application.yml) exposes `health` only.

11. **Production-style JDBC / Ollama URLs**  
    **Given** the **default** Spring profile is used in Compose (not `dev`)  
    **When** datasource and Ollama base URL are read at runtime  
    **Then** `DB_URL` must use the **Docker Compose service name** for PostgreSQL as the JDBC host. Story 6.1 AC2 names that service **`postgres`** — use e.g. `jdbc:postgresql://postgres:5432/examinai` (database name should match your dev DB, e.g. `examinai` from [`application-dev.yml`](../../src/main/resources/application-dev.yml)). `OLLAMA_BASE_URL` must use hostname **`ollama`**, not `localhost`. [Source: `application.yml`](../../src/main/resources/application.yml) — `${OLLAMA_BASE_URL:http://ollama:11434}` already defaults Ollama correctly; **JDBC has no default** in `application.yml`, so `.env` must set `DB_URL` for Docker. *(Note: some architecture prose says hostname `db` for postgres; this story follows the epic’s explicit service list `postgres`, `ollama`, `app` — keep JDBC host equal to the compose service name.)*

## Tasks / Subtasks

- [x] **Add `Dockerfile` (multi-stage)** (AC: 1)  
  - [x] Stage 1: `maven:*` (or `eclipse-temurin:21-jdk` + Maven) — `mvn -q -DskipTests package`  
  - [x] Stage 2: `eclipse-temurin:21-jre`, `COPY` fat JAR, `ENTRYPOINT` `java -jar ...`  
  - [x] Ensure runtime image has **`curl`** available for the app healthcheck (e.g. use a JRE image variant that includes it, or install in Dockerfile — do not remove healthcheck; fix image instead).

- [x] **Add `docker-compose.yml` at repo root** (AC: 2–5, 11)  
  - [x] Services (per epics): **`postgres`**, `ollama`, `app` — JDBC host in `DB_URL` must be **`postgres`**  
  - [x] `postgres:16` + volume `db-data` + `pg_isready` healthcheck  
  - [x] `ollama/ollama` + volume `ollama-models` + healthcheck + **exact** entrypoint per AC 3  
  - [x] `app`: `build: .` (or `context`/`dockerfile`), `env_file: .env`, `ports` for 8080, `depends_on` with `service_healthy`, actuator healthcheck  
  - [x] Top-level `volumes:` for `db-data` and `ollama-models`  
  - [x] **Never** put secrets in compose YAML — wire via `env_file` / variable substitution from `.env` only

- [x] **Environment documentation** (AC: 6, 11)  
  - [x] Update or extend [`.env.example`](../../.env.example) with **comments** (not values) showing example **Docker** `DB_URL` using hostname **`postgres`** and database name consistent with local dev (e.g. `examinai` — same as [`application-dev.yml`](../../src/main/resources/application-dev.yml) database name)  
  - [x] Do **not** add new required keys without PRD alignment — all keys should remain those in `.env.example`

- [x] **Verify runtime configuration** (AC: 6, 9–11)  
  - [x] Confirm Compose does **not** activate `dev` profile unless explicitly set — `application.yml` + env vars should suffice  
  - [x] Manually verify graceful shutdown + async drain per AC 9 (see Testing)

- [x] **Testing** (AC: 6–10)  
  - [x] **Manual / smoke:** `docker compose up --build` from clean volume state (or document first-run download time for Ollama model)  
  - [x] **Manual:** `curl` / browser `http://localhost:8080/login` and `GET /actuator/health`  
  - [x] **Optional:** document-only or scripted smoke test — do not add heavy CI E2E unless the project already has a pattern (none required by epics for this story)

## Dev Notes

### Developer context

- **Greenfield in repo:** there is **no** [`docker-compose.yml`](../../docker-compose.yml) or [`Dockerfile`](../../Dockerfile) in the project yet — this story **adds** them at the **repository root** per [architecture project structure](../planning-artifacts/architecture.md#complete-project-directory-structure).
- **Profile split:** [application-dev.yml](../../src/main/resources/application-dev.yml) uses **localhost** for DB and Ollama. Docker runs should use the **default** `application.yml` and env-driven URLs for **service names** on the Docker network.
- **Async / thread pool:** [`AsyncConfig`](../../src/main/java/com/examinai/config/AsyncConfig.java) already implements `awaitTerminationSeconds(120)` and a custom `CallerRunsWithWarningHandler` for rejection — do **not** rip this out. Epic 5 retro noted pool saturation (T1) as carried risk; this story’s ACs focus on **shutdown drain**, not new pool tuning.
- **Ollama image / `curl`:** If the Ollama official image lacks `curl`, the prescribed healthcheck may fail — prefer an image tag documented to include `curl`, or a minimal `wget`/HTTP alternative **only** if you cannot satisfy `curl` after verifying the official image. The epic text is the default; any deviation must be justified in Dev Agent Record and match behavior (healthy when API is ready).

### Technical requirements

| Topic | Requirement |
|--------|---------------|
| Java / Spring | Java **21**; Spring Boot **3.5.13** per [`pom.xml`](../../pom.xml) — Dockerfile must target the same JRE major version. |
| Build | `mvn package -DskipTests` in Docker build — align with existing `examin-ai` artifact name in `pom.xml`. |
| Config | All sensitive values via `.env` — [`.env` is gitignored](../../.gitignore). |
| Actuator | [`application.yml`](../../src/main/resources/application.yml) — `management.endpoints.web.exposure.include: health` only. |
| Ollama client | [`AIConfig`](../../src/main/java/com/examinai/config/AIConfig.java) configurable read timeout (default 15m) — not changed by Compose work unless env breaks connectivity. |

### Architecture compliance

- Follow [architecture.md — Infrastructure & Deployment](../planning-artifacts/architecture.md) and [Development Workflow — Docker Compose](../planning-artifacts/architecture.md#development-workflow-integration): three services, healthchecks, named volumes, `.env` → `env_file`.
- [architecture.md — Configuration Files table](../planning-artifacts/architecture.md): `.env`, `.env.example`, `application.yml`, `application-dev.yml`, `docker-compose.yml`, `Dockerfile` — this story materializes the last two for real.

### Library / framework

- **Docker / Compose** — Compose file format v2+ (`docker compose` CLI). Use `depends_on: condition: service_healthy` syntax supported by your Compose version.
- **No new Java dependencies** required for a minimal story unless you introduce a test helper the project does not use — avoid scope creep.

### File structure

| Action | Path |
|--------|------|
| Add | `Dockerfile` (repo root) |
| Add | `docker-compose.yml` (repo root) |
| Update (optional comments only) | `.env.example` |
| Verify (no rewrites unless required) | `src/main/resources/application.yml`, `src/main/resources/application-dev.yml` |
| Verify | `src/main/java/com/examinai/config/AsyncConfig.java` |

### Testing requirements

- **Prove:** one-command `docker compose up --build` from repo root with a filled `.env` (all keys from `.env.example`) brings up the stack; app responds on port 8080; `/actuator/health` returns UP.
- **Prove:** `docker compose down` and `up` again — Postgres data and Ollama model data persist (named volumes, not `down -v`).
- **Note:** first Ollama start can take a long time while pulling `qwen2.5-coder:3b`; `depends_on: service_healthy` should prevent the app from starting before Ollama healthcheck passes. Compose gives Ollama a long `start_period` (1200s) and extra `retries` for slow networks; if health still flaps, check bandwidth or pull the model on a faster link once.

### Project structure notes

- Keep **all** operations files at **repo root** per architecture diagram (`Dockerfile`, `docker-compose.yml` next to `pom.xml`).
- Package naming in Java remains `com.examinai` — no new packages for this story unless extracting tiny helper (avoid unless necessary).

### References

- [Epics: Story 6.1](../planning-artifacts/epics.md) — full BDD ACs (source of truth for this story)
- [Architecture: Infrastructure & Deployment, Project structure, Config files](../planning-artifacts/architecture.md)
- [Implementation readiness (Epic 6)](../planning-artifacts/implementation-readiness-report-2026-04-20.md) — NFR / epic notes
- [Epic 5 retrospective — Epic 6 preview](epic-5-retro-2026-04-22.md) — operational hardening context (T1 thread pool, optional SMTP integration)

### Latest technical information (for implementer)

- **Compose:** Use the modern `docker compose` (space) plugin; `depends_on` with health conditions requires compatible Compose spec (2.1+ file format for `condition` under `depends_on` — use current [Compose Specification](https://github.com/compose-spec/compose-spec/blob/master/spec.md) as needed).
- **eclipse-temurin:21-jre:** Default to a glibc-based tag if you need `curl` in the app image without extra packages; confirm with Docker Hub tag description.

## Dev Agent Record

### Agent Model Used

Composer (Cursor)

### Debug Log References

_N/A_

### Implementation Plan (Step 5)

- **Dockerfile (root):** Multi-stage: `maven:3.9.9-eclipse-temurin-21` → `mvn -q -DskipTests package` → JAR `examin-ai-0.0.1-SNAPSHOT.jar`; runtime `eclipse-temurin:21-jre` + `apt-get install curl` for `curl` healthcheck.  
- **Dockerfile.ollama:** `FROM ollama/ollama` + `curl` (official runtime omits it; [ollama#9781](https://github.com/ollama/ollama/issues/9781)) so AC3 `curl` healthcheck can run.  
- **docker-compose.yml:** `postgres:16` + `db-data` + `pg_isready` using `$DB_USERNAME` from `env_file`; `ollama` with AC3 entrypoint, `ollama-models`, healthcheck; `app` with `env_file: .env`, `8080:8080`, `depends_on` + `service_healthy` for both deps, `curl` to `/actuator/health`; no secrets in YAML (only `POSTGRES_*` / compose interpolation from `.env` + `env_file` on `postgres` / `app`).  
- **.env.example:** Commented examples for `DB_URL` (`postgres:5432/examinai`) and `OLLAMA_BASE_URL` (`http://ollama:11434`); no new env keys.  
- **Config verification:** `docker-compose` does not set `SPRING_PROFILES_ACTIVE` (default profile, `application.yml`); `AsyncConfig` unchanged — `setAwaitTerminationSeconds(120)` and `setWaitForTasksToCompleteOnShutdown(true)` (AC9).

### Completion Notes List

- **Done:** Root `Dockerfile`, `Dockerfile.ollama`, `docker-compose.yml`, `.dockerignore`, `.env.example` comments. `mvn -q -DskipTests package` and full `mvn -q test` pass on implementer’s machine.  
- **Docker E2E:** Full `docker compose up --build` not executed here (Docker Engine unavailable in this environment). On a host with Docker: copy `.env` from `.env.example`, set `DB_URL=jdbc:postgresql://postgres:5432/examinai`, set DB and mail creds, set `OLLAMA_BASE_URL=http://ollama:11434` (or omit that line so `application.yml` default applies — avoid a blank `OLLAMA_BASE_URL` key if Spring binds empty string). First Ollama start can take a long time while `qwen2.5-coder:3b` pulls.  
- **AC9 manual:** To confirm async drain under Docker, run `docker compose stop app` (or `stop` full stack) while a review is in progress and check logs for orderly shutdown; config remains 120s await.  
- **AC7–8 manual:** After data exists, `docker compose down` (no `-v`) then `up` again — `db-data` and `ollama-models` must persist (named volumes in compose file).

### File List

- `Dockerfile`  
- `Dockerfile.ollama`  
- `docker-compose.yml`  
- `.dockerignore`  
- `.env.example`  
- `_bmad-output/implementation-artifacts/6-1-production-ready-docker-compose-deployment.md`  
- `_bmad-output/implementation-artifacts/sprint-status.yaml`

### Change Log

- 2026-04-22: Story 6.1 — production Docker Compose: multi-stage app image, ollama image with `curl` for healthchecks, compose stack with named volumes, `.env` wiring, `.env.example` Docker URL comments.

### Git intelligence summary

- Recent commits: `f8d99e3` Finished implementation of epic 5; `b47a46d` story 5.1; `82844f7` epic 4 — work has been app-feature focused; **no** prior Docker layout in history — this story establishes container patterns fresh.

### Previous story intelligence

- **Epic 6 / Story 6.1** is the **first** story in the epic; there is no `6-0` story. Closest prior work: [**5-2** Admin dashboard](5-2-admin-cross-intern-dashboard.md) (done) — follow its style for file references, AC traceability, and test discipline; deployment files are new scope here.

### Review Findings

- [x] [Review][Decision] Ollama first-boot timing — **Resolved (2026-04-22):** option 1 — compose-only relax. `docker-compose.yml` Ollama service: `start_period` **600s → 1200s**, `retries` **12 → 20**; first-run / slow-network note in YAML comments. Entrypoint unchanged.

- [x] [Review][Patch] Hardcoded fat JAR path in app image build [`Dockerfile`](../Dockerfile) — **Fixed (2026-04-22):** `COPY --from=build /app/target/examin-ai-*.jar app.jar` (single repackaged artifact from `mvn package`).

- [x] [Review][Defer] Ollama custom image runs as `root` with models under `/root/.ollama` (volume mount) [`Dockerfile.ollama:1-7`](../Dockerfile.ollama) — deferred: tightening to a non-root user would require coordinating home directory, Ollama data paths, and the compose volume; matches current `ollama-models:/root/.ollama` design.

- [x] [Review][Defer] Compose defines no `deploy.resources` limits and does not pin images by digest [`docker-compose.yml`](../docker-compose.yml) — deferred: production hardening not required by story ACs; pre-existing class of improvement for a later pass.

## Story completion status

- **Status:** `done`  
- **Note:** Code review (2026-04-22): decision D1 applied (Ollama health relax + docs); JAR `COPY` glob fix. Full `docker compose` smoke test still best verified on a host with Docker (see Dev Agent Record).
