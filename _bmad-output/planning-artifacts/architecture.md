---
stepsCompleted: [1, 2, 3, 4, 5, 6, 7, 8]
status: 'complete'
completedAt: '2026-04-20'
lastStep: 8
inputDocuments:
  - '_bmad-output/planning-artifacts/prd.md'
  - '_bmad-output/planning-artifacts/ux-design-specification.md'
  - '_bmad-output/planning-artifacts/research/technical-ai-mentorship-platform-research-2026-04-19.md'
  - 'docs/requirements.md'
workflowType: 'architecture'
project_name: 'examin-ai-with-bmad'
user_name: 'Ulugbek'
date: '2026-04-20'
---

# Architecture Decision Document

_This document builds collaboratively through step-by-step discovery. Sections are appended as we work through each architectural decision together._

## Project Context Analysis

### Requirements Overview

**Functional Requirements:**

ExaminAI has 33 FRs across 7 capability groups:

- **Authentication & Access Control (FR1–FR5):** Spring Security form login; 3 roles (INTERN/MENTOR/ADMIN); role-based dashboard routing on login; Admin inherits all lower roles
- **User Account Management (FR6–FR9):** Admin-only user creation/deactivation; BCrypt-hashed passwords (strength ≥12); seed data on startup
- **Course & Task Management (FR10–FR13):** Mentor/Admin CRUD for Courses and Tasks; each Task FK-linked to a Course and owning Mentor
- **Task Submission (FR14–FR19):** 3-field submission form (repoOwner, repoName, prNumber); 202 Accepted + reviewId returned immediately; 3-second client polling; ERROR state surfaces on GitHub/LLM failure with resubmit path; every attempt saved as a new `TaskReview` row
- **AI Review Processing (FR20–FR23):** GitHub PR diff fetch via authenticated REST call; Ollama LLM invocation with structured JSON response; `LlmOutputSanitizer` + `BeanOutputConverter`; ERROR state persisted on failure
- **Mentor Review & Decision (FR24–FR28):** Filtered queue view (`LLM_EVALUATED` default); AI feedback display with line/code/issue/improvement; approve or reject with optional remarks; mentor can override AI verdict
- **Notifications (FR29–FR30):** Email to mentor when AI review completes; email to intern when mentor finalizes; SMTP failures logged but do not block review pipeline

**Non-Functional Requirements:**

| Area | Key Requirement | Architectural Impact |
|---|---|---|
| Performance | Submit endpoint < 500ms; status poll < 200ms; AI pipeline bounded by configurable Ollama HTTP read timeout (default 15m on CPU) | Async thread pool mandatory; DB read-only poll path must be fast |
| Security | BCrypt ≥12; no secrets in code; server-side role checks; session expiry | `@EnableMethodSecurity`; `@PreAuthorize` on all controllers/services |
| Integration resilience | GitHub 404/403/429 → ERROR state; Ollama HTTP read timeout (configurable; default 15m) → ERROR state; SMTP failure → log only | All external calls must catch and surface structured errors |
| Reliability | Every submission persisted as PENDING before external calls; Liquibase runs before HTTP traffic; data survives container restart | Write-first pipeline; Docker volumes for DB and Ollama model |

**Scale & Complexity:**

- Primary domain: EdTech / Full-stack web application (server-side MPA)
- Complexity level: Medium
- Estimated architectural components: ~12 (SecurityConfig, UserService, CourseService, TaskService, ReviewSubmissionController, ReviewPipelineService, GitHubClient, LLMReviewService, ReviewPersistenceService, NotificationService, polling endpoint, Thymeleaf template sets × 3 roles)

### Technical Constraints & Dependencies

- **Spring Boot 3.4.2+** — hard requirement for Spring AI 1.0.x compatibility (3.2.x incompatible)
- **Spring AI 1.0.0 via BOM** — `spring-ai-ollama-spring-boot-starter`; BeanOutputConverter for structured output
- **Ollama qwen2.5-coder:3b** — code-focused 3B model; `LlmOutputSanitizer` handles reasoning tags / fences / prose around JSON; configurable HTTP read timeout (default 15m for CPU); pre-pull in Docker entrypoint
- **PostgreSQL 16 + Liquibase 4.31.1+** — schema migration completes before HTTP traffic; named Docker volume
- **GitHub PAT (`GITHUB_TOKEN`)** — env var only; Bearer header only; never logged; fine-grained token with repo read scope
- **No external cloud dependencies** — all inference is local; no third-party SaaS required for operation
- **Docker Compose 3-service deployment** — app + postgres + ollama with healthchecks and `condition: service_healthy`

### Cross-Cutting Concerns Identified

1. **Async pipeline & state machine** — PENDING → LLM_EVALUATED → APPROVED/REJECTED/ERROR; state transitions drive all UI, notification, and data behaviors across every component
2. **Multi-role security enforcement** — `@PreAuthorize` is the authoritative check on every endpoint; Thymeleaf `sec:authorize` is display-only; interns cannot see cross-intern data
3. **External API resilience** — GitHub and Ollama failures must transition `TaskReview` to ERROR (not lost), surface a user-visible message, and allow resubmission without losing history
4. **Email delivery isolation** — SMTP must be `@Async` + `@TransactionalEventListener(AFTER_COMMIT)` so notification failure never blocks or rolls back the review pipeline
5. **Thymeleaf fragment component system** — ReviewStatusBadge, AIFeedbackCard, MentorActionPanel, InternStatusCard, TaskStatusCard are reusable fragments that enforce consistent status display across all roles
6. **Redirect-after-POST** — all form submissions must follow PRG pattern to prevent duplicate submissions on browser refresh

## Starter Template Evaluation

### Primary Technology Domain

Java full-stack server-side MPA (Spring Boot + Thymeleaf), based on project requirements analysis.

### Starter Options Considered

| Option | Verdict | Reason |
|---|---|---|
| Spring Initializr (start.spring.io) | ✅ Selected | Official; handles Spring AI BOM; generates correct pom.xml structure |
| Manual Maven scaffold | ❌ Rejected | Error-prone for BOM-based Spring AI + multi-dependency setup |
| Spring Boot CLI | ❌ Rejected | Fewer options; less suitable for complex multi-dependency projects |

### Selected Starter: Spring Initializr

**Rationale for Selection:**
Spring Initializr is the authoritative scaffold for Spring Boot projects. It correctly generates the parent POM, dependency management blocks, and starter imports needed for Spring AI's BOM-based dependency management. All versions are pre-validated by the ExaminAI technical research document (April 2026).

**Initialization Command:**

```bash
curl -G https://start.spring.io/starter.zip \
  -d type=maven-project \
  -d language=java \
  -d bootVersion=3.4.2 \
  -d baseDir=examin-ai \
  -d groupId=com.examinai \
  -d artifactId=examin-ai \
  -d javaVersion=21 \
  -d dependencies=web,thymeleaf,security,data-jpa,postgresql,liquibase,mail,validation,actuator \
  -o examin-ai.zip
```

> Spring AI and Thymeleaf Security extras must be added manually to `pom.xml` after initialization (Spring Initializr does not include `spring-ai-bom` or `thymeleaf-extras-springsecurity6` as selectable dependencies).

**Architectural Decisions Provided by Starter:**

**Language & Runtime:**
Java 21 (LTS); Spring Boot 3.4.2 parent POM manages all transitive dependency versions

**Build Tooling:**
Maven with Spring Boot parent (`spring-boot-starter-parent:3.4.2`); dependency management via BOM import for Spring AI 1.0.0

**Key Dependencies to Add Post-Init to `pom.xml`:**
- `spring-ai-bom:1.0.0` (BOM import in `<dependencyManagement>`)
- `spring-ai-ollama-spring-boot-starter` (from Spring AI BOM)
- `org.webjars/bootstrap:5.3.x` (Bootstrap via WebJar)
- `nz.net.ultraq.thymeleaf/thymeleaf-layout-dialect`
- `org.thymeleaf.extras/thymeleaf-extras-springsecurity6:3.1.2.RELEASE`

**Testing Framework:**
Spring Boot Test + JUnit 5 (auto-configured by starter); WireMock for GitHub API stubs; Testcontainers for PostgreSQL integration tests

**Code Organization:**
Package-by-feature hybrid under `com.examinai`: `task/`, `review/`, `notification/`, `user/`, `config/`

**Development Experience:**
Spring Boot DevTools (hot reload); `.env` file for all secrets; `application.yml` structured config; Docker Compose for full local stack

**Note:** Project initialization using this command should be the first implementation story.

## Core Architectural Decisions

### Decision Priority Analysis

**Critical Decisions (Block Implementation):**
- DB-backed session persistence via Spring Session
- Async review pipeline with dedicated thread pool
- Role-based security enforcement at method level (`@PreAuthorize`)
- Write-first submission pipeline (PENDING persisted before external calls)

**Important Decisions (Shape Architecture):**
- Package-by-feature code organization
- Thymeleaf fragment component system
- Environment profile separation (default vs. dev override)
- `@TransactionalEventListener(AFTER_COMMIT)` for email isolation

**Deferred Decisions (Post-MVP):**
- JSON structured logging (plain text Logback is sufficient for MVP)
- CI/CD pipeline (manual Docker Compose deploy for MVP)
- Multi-tenant isolation (Phase 3 SaaS vision)
- Centralized log aggregation (ELK/Grafana stack)

### Data Architecture

| Decision | Choice | Rationale |
|---|---|---|
| ORM | Spring Data JPA (`JpaRepository<T, ID>`) | Auto-configured by starter; handles all CRUD and custom queries |
| DB migrations | Liquibase 4.31.1+ changelogs in `db/changelog/` | Runs before HTTP traffic; ensures schema is ready before app starts |
| Connection pool | HikariCP default (max 10 connections) | Sufficient for single-node intern cohort; configurable via `spring.datasource.hikari` |
| Session storage | Spring Session + PostgreSQL (`spring-session-jdbc`) | Sessions survive container restarts; single node — no Redis needed |
| Session timeout | 1 hour inactivity | Balances security with usability for intern/mentor workflow |
| Application cache | None (MVP) | Data volumes are low; DB read latency is acceptable for poll endpoint |
| Task completion tracking | `TaskReview.status = APPROVED` per intern (not `Task.dateDone`) | Enables per-intern independent tracking across multiple attempts |

**Changelog structure:**
```
src/main/resources/db/changelog/
├── db.changelog-master.xml
└── changelogs/
    ├── 001-init-schema.sql       (UserAccount, Course, Task, TaskReview, TaskReviewIssue)
    ├── 002-spring-session.sql    (Spring Session JDBC tables)
    ├── 003-indexes.sql           (intern_id, status on TaskReview)
    └── 004-seed-data.sql         (admin, mentor, intern, course, tasks — BCrypt hashed)
```

### Authentication & Security

| Decision | Choice | Rationale |
|---|---|---|
| Authentication | Spring Security form login | Standard; integrates with Thymeleaf and session management |
| Password hashing | BCryptPasswordEncoder strength 12 | Resists brute force; ~100ms/hash acceptable for login |
| Session backend | Spring Session JDBC (PostgreSQL) | Survives container restarts; no extra infra beyond existing DB |
| Session timeout | 1 hour (`server.servlet.session.timeout=1h`) | Interns/mentors in daily focused sessions — 1hr is appropriate |
| Remember-me | Not implemented (MVP) | Internal tool; short sessions are acceptable |
| CSRF | Enabled (Spring Security default for form submissions) | Required for all POST forms; Thymeleaf auto-injects CSRF token |
| Authorization | `@EnableMethodSecurity` + `@PreAuthorize` on all controllers/services | Server-side enforcement; Thymeleaf `sec:authorize` is display-only |
| Role model | `ROLE_INTERN`, `ROLE_MENTOR`, `ROLE_ADMIN` | Admin inherits all; URL namespaces `/intern/**`, `/mentor/**`, `/admin/**` |
| Secret management | `.env` + Docker Compose env injection | No secrets in source code or version control |
| GitHub token | `Authorization: Bearer {GITHUB_TOKEN}` header only — never logged | Fine-grained PAT with repo read scope |

### API & Communication Patterns

| Decision | Choice | Rationale |
|---|---|---|
| Web pattern | Server-side MPA (Spring MVC + Thymeleaf full-page loads) | Matches team skills; no SPA complexity for MVP |
| Async submission | `POST /intern/reviews` → `202 Accepted` + `{reviewId}` | Non-blocking; LLM takes 10–60s — HTTP thread must not block |
| Status polling | `GET /reviews/{reviewId}/status` → JSON `{status, message}` | DB read only; < 200ms; polled every 3s from vanilla JS |
| Form submissions | POST + redirect-after-POST (PRG pattern) | Prevents duplicate submissions on browser refresh |
| Form validation | `@Valid` + `BindingResult` → Thymeleaf error rendering | Server-side only; no client-side JS validation needed |
| Unhandled errors | `@ControllerAdvice` → custom error page with return-to-dashboard link | Graceful fallback for unexpected exceptions |
| GitHub client | `RestClient` + `@HttpExchange` interface (`GitHubClient`) | Modern Spring 6.1+ fluent API; clean abstraction for testing |
| Async thread pool | `@Async` with dedicated executor: core=15, max=30, queue=150, awaitTermination=120s | Handles 15 concurrent LLM reviews; queue absorbs bursts |
| Email delivery | `JavaMailSender` + `@Async` + `@TransactionalEventListener(AFTER_COMMIT)` | Fires only after DB commit; SMTP failure never blocks review pipeline |
| API error format | No JSON error body — all errors map to named review `ERROR` state or Thymeleaf error page | Consistent with SSR-first approach |

### Frontend Architecture

| Decision | Choice | Rationale |
|---|---|---|
| Rendering | Thymeleaf 3.1.x SSR — full-page loads for all navigation | Java-developer-friendly; zero JS framework complexity |
| Layout system | Thymeleaf Layout Dialect — shared base template loads Bootstrap once | DRY layout; all role-specific pages extend a single base |
| Design system | Bootstrap 5 via `org.webjars/bootstrap:5.3.x` WebJar | No npm/build pipeline; served by Spring ResourceHandlerRegistry |
| Custom CSS | ~50–100 lines in `static/css/custom.css` | Status badges, sticky action bar, AI vs. mentor visual distinction |
| JavaScript | Vanilla JS only — `static/js/review-polling.js` (~30 lines) | No framework; `setInterval` polling + DOM update on terminal state |
| Component system | Thymeleaf fragments in `templates/fragments/` | ReviewStatusBadge, AIFeedbackCard, MentorActionPanel, InternStatusCard, TaskStatusCard |
| Prompt templates | `.st` (StringTemplate) files in `src/main/resources/prompts/` | Externalized from code; referenced via `application.yml` |
| URL namespacing | `/intern/**`, `/mentor/**`, `/admin/**` | Role-appropriate routing; Spring Security filter chains per namespace |

### Infrastructure & Deployment

| Decision | Choice | Rationale |
|---|---|---|
| Deployment | Docker Compose (3 services: app, postgres, ollama) | Self-contained local deployment; no cloud account required |
| Healthchecks | `pg_isready` on postgres; `curl /api/tags` on ollama; `condition: service_healthy` on app | Prevents Spring Boot starting before DB/Ollama are ready |
| Data persistence | Named Docker volumes: `db-data`, `ollama-models` | Survives container restarts; Ollama model (~2 GB for `qwen2.5-coder:3b`) persists |
| Configuration | `.env` file → Docker Compose `env_file` → Spring `application.yml` | Single source for all secrets; never committed to VCS |
| Environment profiles | `application.yml` (production defaults) + `application-dev.yml` (local dev overrides, no Docker hostnames) | Enables running app directly (IDE) vs. inside Docker Compose |
| Logging | SLF4J + Logback defaults (Spring Boot built-in, plain text) | Sufficient for MVP; `docker logs` readable without tooling |
| Monitoring | Spring Actuator `/actuator/health` endpoint | Used by Docker healthcheck for `app` service |
| Ollama model init | `entrypoint: /bin/sh -c "ollama serve & sleep 5 && ollama pull qwen2.5-coder:3b && wait"` | Pre-pulls model on first start; named volume prevents re-download |
| Scaling | Single-node (MVP) | Internal cohort size doesn't require horizontal scaling |

### Decision Impact Analysis

**Implementation Sequence (dependencies drive order):**
1. UserAccount + Spring Security + Spring Session JDBC (all other features depend on auth)
2. Liquibase changelogs (schema first: schema → Spring Session tables → indexes → seed data)
3. Course + Task CRUD with mentor assignment
4. GitHub client (`GitHubClient` + `@HttpExchange`)
5. Async review pipeline (submit endpoint → `@Async` executor → LLM service → DB persist)
6. Polling endpoint + vanilla JS polling
7. Mentor review UI + approve/reject
8. Email notifications (`@TransactionalEventListener`)
9. Admin views + Docker Compose with healthchecks

**Cross-Component Dependencies:**
- Spring Session JDBC requires `002-spring-session.sql` changelog to exist before app starts
- `@Async` executor must be configured before `ReviewPipelineService` is invoked
- `@TransactionalEventListener(AFTER_COMMIT)` requires `@EnableAsync` + transaction manager wiring
- Thymeleaf `sec:authorize` requires `thymeleaf-extras-springsecurity6` on classpath
- Bootstrap WebJar requires `ResourceHandlerRegistry` mapping in `WebMvcConfig`

## Implementation Patterns & Consistency Rules

### Critical Conflict Points Identified

8 areas where AI agents could make incompatible choices without explicit rules:
1. Database naming convention (snake_case vs camelCase)
2. URL endpoint structure and plurality
3. Transaction boundary placement
4. Async pipeline error handling
5. Spring event naming and payload structure
6. Thymeleaf template and fragment organization
7. JSON response format for the polling endpoint
8. Enum persistence strategy

---

### Naming Patterns

**Database Naming Conventions:**

| Element | Convention | Example |
|---|---|---|
| Table names | `snake_case`, singular | `user_account`, `task_review`, `task_review_issue` |
| Column names | `snake_case` | `intern_id`, `mentor_remarks`, `date_created` |
| Foreign keys | `{referenced_table}_id` | `task_id`, `intern_id`, `mentor_id` |
| Indexes | `idx_{table}_{column(s)}` | `idx_task_review_intern_id`, `idx_task_review_status` |
| Primary keys | `id` (always `BIGSERIAL`) | `id` |

**Java Code Naming Conventions:**

| Element | Convention | Example |
|---|---|---|
| Entities | `PascalCase`, noun | `UserAccount`, `TaskReview`, `TaskReviewIssue` |
| Entity fields | `camelCase` + `@Column(name="snake_case")` | `internId` → `@Column(name="intern_id")` |
| Enums | `PascalCase` + `@Enumerated(EnumType.STRING)` | `ReviewStatus.LLM_EVALUATED` |
| Controllers | `PascalCase` + `Controller` | `InternSubmissionController`, `MentorReviewController` |
| Services | `PascalCase` + `Service` | `ReviewPipelineService`, `NotificationService` |
| Repositories | `PascalCase` + `Repository` | `TaskReviewRepository`, `UserAccountRepository` |
| Spring Events | `PascalCase` + `Event` | `AiReviewCompleteEvent`, `MentorDecisionEvent` |
| Config classes | `PascalCase` + `Config` | `SecurityConfig`, `AsyncConfig`, `WebMvcConfig` |
| Packages | `lowercase`, feature-named | `com.examinai.review`, `com.examinai.notification` |

**URL Naming Conventions:**

| Element | Convention | Example |
|---|---|---|
| Role namespaces | `/role/` prefix | `/intern/`, `/mentor/`, `/admin/` |
| Resource paths | `kebab-case`, plural nouns | `/intern/task-reviews`, `/mentor/reviews` |
| Detail pages | `/{id}` suffix | `/mentor/reviews/{reviewId}` |
| Actions | verb after resource | `/mentor/reviews/{reviewId}/approve` |
| Polling endpoint | `/reviews/{reviewId}/status` | `GET /reviews/{reviewId}/status` |
| Form actions | POST to same path as GET (PRG) | `POST /intern/tasks/{taskId}/submit` |

**Thymeleaf & Static Asset Naming:**

| Element | Convention | Example |
|---|---|---|
| Template files | `kebab-case.html` | `task-list.html`, `review-detail.html` |
| Template directories | role name | `templates/intern/`, `templates/mentor/` |
| Fragment files | `kebab-case.html` in `fragments/` | `templates/fragments/review-status-badge.html` |
| Fragment names | `camelCase` | `th:fragment="statusBadge(status)"` |
| JS files | `kebab-case.js` | `review-polling.js` |
| CSS files | `kebab-case.css` | `custom.css` |

---

### Structure Patterns

**Package Organization (package-by-feature):**

```
com.examinai/
├── user/
│   ├── UserAccount.java
│   ├── UserAccountRepository.java
│   ├── UserAccountService.java
│   └── CustomUserDetailsService.java
├── course/
│   ├── Course.java
│   ├── CourseRepository.java
│   ├── CourseService.java
│   └── CourseController.java
├── task/
│   ├── Task.java
│   ├── TaskRepository.java
│   ├── TaskService.java
│   └── TaskController.java
├── review/
│   ├── TaskReview.java
│   ├── TaskReviewIssue.java
│   ├── TaskReviewRepository.java
│   ├── ReviewStatus.java
│   ├── ReviewSubmissionController.java
│   ├── ReviewStatusController.java
│   ├── MentorReviewController.java
│   ├── ReviewPipelineService.java
│   ├── GitHubClient.java
│   ├── LLMReviewService.java
│   └── ReviewPersistenceService.java
├── notification/
│   ├── AiReviewCompleteEvent.java
│   ├── MentorDecisionEvent.java
│   └── NotificationService.java
└── config/
    ├── SecurityConfig.java
    ├── AsyncConfig.java
    ├── WebMvcConfig.java
    └── AIConfig.java
```

**Template Organization:**

```
src/main/resources/templates/
├── layout/
│   └── base.html
├── fragments/
│   ├── review-status-badge.html
│   ├── ai-feedback-card.html
│   ├── mentor-action-panel.html
│   ├── intern-status-card.html
│   └── task-status-card.html
├── intern/
│   ├── task-list.html
│   ├── task-detail.html
│   └── review-status.html
├── mentor/
│   ├── review-queue.html
│   └── review-detail.html
├── admin/
│   ├── dashboard.html
│   ├── course-list.html
│   ├── task-list.html
│   └── user-list.html
├── auth/
│   └── login.html
└── error/
    └── error.html
```

**Test Organization:**

```
src/test/java/com/examinai/
├── review/
│   ├── ReviewPipelineServiceTest.java
│   ├── LLMReviewServiceTest.java
│   └── ReviewSubmissionControllerTest.java
├── user/
│   └── SecurityIntegrationTest.java
└── integration/
    └── ReviewPipelineIntegrationTest.java
```

---

### Format Patterns

**Polling Endpoint JSON Response:**

```json
{
  "reviewId": 42,
  "status": "LLM_EVALUATED",
  "displayLabel": "Awaiting Mentor Review",
  "errorMessage": null
}

// ERROR state:
{
  "reviewId": 42,
  "status": "ERROR",
  "displayLabel": "Review Failed",
  "errorMessage": "GitHub PR not found. Check your PR number and resubmit."
}
```

- `status` is always the raw `ReviewStatus` enum string
- `displayLabel` is the human-readable UI label that drives badge text
- `errorMessage` is `null` for non-ERROR states, populated only for ERROR
- No other JSON endpoints exist — everything else is Thymeleaf SSR

**Enum Persistence:**
- ALL enums use `@Enumerated(EnumType.STRING)` — never `ORDINAL`
- `ReviewStatus` values: `PENDING`, `LLM_EVALUATED`, `APPROVED`, `REJECTED`, `ERROR`

**Date/Time Handling:**
- Store as `LocalDateTime` (PostgreSQL `TIMESTAMP WITHOUT TIME ZONE`)
- Field name is always `dateCreated` — not `createdAt`, `created_date`, or `timestamp`
- Render in Thymeleaf: `#temporals.format(date, 'dd MMM yyyy HH:mm')`
- Never serialize dates as epoch timestamps in JSON

---

### Communication Patterns

**Spring Event Structure:**

```java
public record AiReviewCompleteEvent(Long reviewId, Long mentorId, String internName,
                                     String courseName, String taskName) {}

public record MentorDecisionEvent(Long reviewId, Long internId, String courseName,
                                   String taskName, ReviewStatus finalStatus, String remarks) {}
```

**Event Listener Rules:**
- ALL notification listeners: `@TransactionalEventListener(phase = AFTER_COMMIT)` + `@Async`
- SMTP failures: caught and logged — never re-thrown (pipeline must not roll back)
- Events published by `ReviewPersistenceService` after DB save completes

**Review State Transitions (enforced in `ReviewPipelineService` only):**

```
PENDING        → LLM_EVALUATED   (successful Ollama call + DB save)
PENDING        → ERROR           (GitHub API failure or LLM timeout)
LLM_EVALUATED  → APPROVED        (mentor decision)
LLM_EVALUATED  → REJECTED        (mentor decision)
```

Status must never be set directly from a controller — only through designated service methods.

---

### Process Patterns

**Transaction Boundary Rule:**
- `@Transactional` on Service methods only — never on Controllers or Repositories
- `ReviewPipelineService.submitReview()` is `@Transactional` for the initial PENDING persist
- The `@Async` background method runs in a separate transaction

**Write-First Pipeline Rule (mandatory order):**
```
1. taskReview.setStatus(PENDING)
2. repository.save(taskReview)        ← flush to DB before external calls
3. eventPublisher.publishEvent(...)   ← triggers @Async background work
4. return reviewId                    ← return 202 to client
```

**Async Pipeline Error Handling:**
```java
try {
    // GitHub call or LLM call
} catch (Exception e) {
    review.setStatus(ReviewStatus.ERROR);
    review.setErrorMessage(userFacingMessage(e));
    reviewRepository.save(review);
    log.error("Review {} failed: {}", reviewId, e.getMessage());
    // DO NOT re-throw
}
```

**PRG Rule (mandatory for all form handlers):**
```java
@PostMapping("/intern/tasks/{taskId}/submit")
public String submitReview(...) {
    Long reviewId = reviewService.submit(...);
    return "redirect:/intern/reviews/" + reviewId;  // always redirect, never return view
}
```

**`@PreAuthorize` Placement:**
- On every controller method — class-level annotation alone is not sufficient
- Interns: `@PreAuthorize("hasRole('INTERN') or hasRole('ADMIN')")`
- Mentors: `@PreAuthorize("hasRole('MENTOR') or hasRole('ADMIN')")`
- Admin-only: `@PreAuthorize("hasRole('ADMIN')")`

**LLM Response Processing (mandatory order in `LLMReviewService`):**
```java
// 1. Strip <think> tokens
String cleaned = raw.replaceAll("(?s)<think>.*?</think>", "").trim();
// 2. Strip markdown fences
cleaned = cleaned.replaceAll("```json\\s*|```\\s*", "").trim();
// 3. Parse
ReviewFeedback feedback = converter.convert(cleaned);
```

---

### Enforcement Guidelines

**All AI Agents MUST:**
- Use `snake_case` for all DB identifiers with `@Column(name=...)` mappings on entity fields
- Use `@Enumerated(EnumType.STRING)` on every enum field
- Follow PRG: every `@PostMapping` returns `"redirect:/..."`, never a view name
- Persist `PENDING` status before any external API call
- Catch all exceptions in async pipeline — update to ERROR, never re-throw
- Place `@Transactional` on service methods only
- Apply `@PreAuthorize` on every controller method individually
- Run `LlmOutputSanitizer` (reasoning tags, markdown fences, embedded JSON) before calling `BeanOutputConverter`

**Anti-Patterns to Reject:**
- `@Transactional` on a `@Controller` or `@Repository`
- POST handler returning a view name (always redirect)
- Calling GitHub or Ollama before saving `PENDING` to DB
- `@Enumerated(EnumType.ORDINAL)` on any enum
- Business logic in Thymeleaf templates
- Any secret hardcoded in Java source or templates
- Re-throwing exceptions from the `@Async` review pipeline

## Project Structure & Boundaries

### Requirements to Structure Mapping

| FR Category | Files / Directories |
|---|---|
| Authentication & Access Control (FR1–FR5) | `config/SecurityConfig.java`, `user/CustomUserDetailsService.java`, `templates/auth/login.html` |
| User Account Management (FR6–FR9) | `user/*`, `admin/AdminController.java`, `templates/admin/user-*.html`, `db/changelogs/004-seed-data.sql` |
| Course & Task Management (FR10–FR13) | `course/*`, `task/*`, `templates/mentor/`, `templates/admin/course-*.html`, `templates/admin/task-*.html` |
| Task Submission (FR14–FR19) | `review/ReviewSubmissionController.java`, `review/ReviewStatusController.java`, `templates/intern/task-detail.html`, `templates/intern/review-status.html`, `static/js/review-polling.js` |
| AI Review Processing (FR20–FR23) | `review/ReviewPipelineService.java`, `review/GitHubClient.java`, `review/LLMReviewService.java`, `review/ReviewPersistenceService.java`, `prompts/*.st` |
| Mentor Review & Decision (FR24–FR28) | `review/MentorReviewController.java`, `templates/mentor/review-queue.html`, `templates/mentor/review-detail.html`, `templates/fragments/mentor-action-panel.html` |
| Notifications (FR29–FR30) | `notification/*` |
| Progress Tracking (FR31–FR33) | `templates/intern/task-list.html`, `templates/mentor/review-queue.html`, `templates/admin/dashboard.html` |

---

### Complete Project Directory Structure

```
examin-ai/
├── .env                                      (secrets — never committed)
├── .env.example                              (committed — all keys, no values)
├── .gitignore
├── docker-compose.yml
├── Dockerfile
├── pom.xml
│
└── src/
    ├── main/
    │   ├── java/com/examinai/
    │   │   ├── ExaminAiApplication.java
    │   │   │
    │   │   ├── config/
    │   │   │   ├── SecurityConfig.java           (filter chains, BCrypt bean, Spring Session, /login, role namespaces)
    │   │   │   ├── AsyncConfig.java              (@EnableAsync, executor: core=15, max=30, queue=150, awaitTermination=120s)
    │   │   │   ├── WebMvcConfig.java             (WebJar resource handlers, static asset mappings)
    │   │   │   └── AIConfig.java                 (`OllamaApi` RestClient; configurable read timeout, default 15m)
    │   │   │
    │   │   ├── user/
    │   │   │   ├── UserAccount.java              (@Entity — id, username, password, email, role, active, dateCreated)
    │   │   │   ├── Role.java                     (enum: INTERN, MENTOR, ADMIN)
    │   │   │   ├── UserAccountRepository.java    (findByUsername, findAllByRole)
    │   │   │   ├── UserAccountService.java       (createUser, deactivate, findAll — @PreAuthorize ADMIN)
    │   │   │   └── CustomUserDetailsService.java (UserDetailsService impl — loads from user_account table)
    │   │   │
    │   │   ├── course/
    │   │   │   ├── Course.java                   (@Entity — id, courseName, technology, dateCreated)
    │   │   │   ├── CourseRepository.java
    │   │   │   ├── CourseService.java            (CRUD — @PreAuthorize MENTOR or ADMIN)
    │   │   │   └── CourseController.java         (/mentor/courses/**, /admin/courses/**)
    │   │   │
    │   │   ├── task/
    │   │   │   ├── Task.java                     (@Entity — id, taskName, taskDescription, courseId, mentorId, dateCreated)
    │   │   │   ├── TaskRepository.java           (findByCourseId, findByMentorId)
    │   │   │   ├── TaskService.java              (CRUD — @PreAuthorize MENTOR or ADMIN; findForIntern)
    │   │   │   └── TaskController.java           (/mentor/tasks/**, /intern/tasks/**)
    │   │   │
    │   │   ├── review/
    │   │   │   ├── TaskReview.java               (@Entity — id, taskId, internId, mentorId, status, llmResult,
    │   │   │   │                                  mentorResult, mentorRemarks, errorMessage, dateCreated)
    │   │   │   ├── TaskReviewIssue.java          (@Entity — id, taskReviewId, line, code, issue, improvement)
    │   │   │   ├── ReviewStatus.java             (enum: PENDING, LLM_EVALUATED, APPROVED, REJECTED, ERROR)
    │   │   │   ├── ReviewFeedback.java           (record for BeanOutputConverter — issues list + result)
    │   │   │   ├── ReviewStatusResponse.java     (record for polling JSON — reviewId, status, displayLabel, errorMessage)
    │   │   │   ├── TaskReviewRepository.java     (findByInternId, findByMentorIdAndStatus, findByTaskIdAndInternId)
    │   │   │   ├── TaskReviewIssueRepository.java
    │   │   │   ├── ReviewSubmissionController.java   (POST /intern/tasks/{taskId}/submit → 202 + reviewId)
    │   │   │   ├── ReviewStatusController.java       (GET /reviews/{reviewId}/status → JSON)
    │   │   │   ├── MentorReviewController.java       (GET /mentor/reviews, GET/POST /mentor/reviews/{id}/approve|reject)
    │   │   │   ├── ReviewPipelineService.java        (@Transactional submit; @Async runPipeline)
    │   │   │   ├── ReviewPersistenceService.java     (save TaskReview + issues atomically; publish events)
    │   │   │   ├── LLMReviewService.java             (ChatModel call, LlmOutputSanitizer, BeanOutputConverter)
    │   │   │   ├── GitHubClient.java                 (@HttpExchange interface — getPrDiff)
    │   │   │   └── GitHubClientConfig.java           (RestClient bean + HttpServiceProxyFactory)
    │   │   │
    │   │   ├── notification/
    │   │   │   ├── AiReviewCompleteEvent.java    (record: reviewId, mentorId, internName, courseName, taskName)
    │   │   │   ├── MentorDecisionEvent.java      (record: reviewId, internId, courseName, taskName, status, remarks)
    │   │   │   └── NotificationService.java      (@Async + @TransactionalEventListener(AFTER_COMMIT) — JavaMailSender)
    │   │   │
    │   │   └── admin/
    │   │       └── AdminController.java          (/admin/dashboard — cross-intern visibility, inherits all roles)
    │   │
    │   └── resources/
    │       ├── application.yml                   (production defaults — Docker service hostnames, Spring Session JDBC)
    │       ├── application-dev.yml               (dev overrides — localhost, debug logging)
    │       │
    │       ├── prompts/
    │       │   ├── system-prompt.st              (mentor persona, {technology} variable, review instructions)
    │       │   └── review-prompt.st              ({taskDescription}, {prDiff}, JSON response schema)
    │       │
    │       ├── db/changelog/
    │       │   ├── db.changelog-master.xml
    │       │   └── changelogs/
    │       │       ├── 001-init-schema.sql       (user_account, course, task, task_review, task_review_issue)
    │       │       ├── 002-spring-session.sql    (spring_session + spring_session_attributes tables)
    │       │       ├── 003-indexes.sql           (idx_task_review_intern_id, idx_task_review_status, idx_task_review_mentor_id)
    │       │       └── 004-seed-data.sql         (admin/mentor/intern users BCrypt-hashed, course, 3 tasks)
    │       │
    │       ├── templates/
    │       │   ├── layout/
    │       │   │   └── base.html                 (navbar: logo + role badge + username + logout; Bootstrap; CSRF meta)
    │       │   ├── fragments/
    │       │   │   ├── review-status-badge.html  (th:fragment="statusBadge(status)" — badge + spinner + aria-live)
    │       │   │   ├── ai-feedback-card.html     (th:fragment="aiIssueCard(issue)" — line, code pre, issue, improvement)
    │       │   │   ├── mentor-action-panel.html  (th:fragment="mentorActionPanel(review)" — sticky sidebar, approve/reject)
    │       │   │   ├── intern-status-card.html   (th:fragment="internStatusCard(review)" — polling anchor, error/result)
    │       │   │   └── task-status-card.html     (th:fragment="taskStatusCard(task, review)" — left-border color)
    │       │   ├── intern/
    │       │   │   ├── task-list.html            (FR13, FR31 — card grid, TaskStatusCard per task)
    │       │   │   ├── task-detail.html          (FR14, FR18, FR19 — submission form + history list)
    │       │   │   └── review-status.html        (FR16, FR17 — InternStatusCard + polling JS init)
    │       │   ├── mentor/
    │       │   │   ├── review-queue.html         (FR24, FR32 — table-hover, LLM_EVALUATED default filter)
    │       │   │   └── review-detail.html        (FR25, FR26, FR27 — col-md-8 issues + col-md-4 sticky panel)
    │       │   ├── admin/
    │       │   │   ├── dashboard.html            (FR33 — cross-intern table, all filters)
    │       │   │   ├── course-list.html          (FR10)
    │       │   │   ├── course-form.html          (FR10 — create/edit)
    │       │   │   ├── task-list.html            (FR11)
    │       │   │   ├── task-form.html            (FR11 — create/edit, mentor assignment)
    │       │   │   ├── user-list.html            (FR6, FR7)
    │       │   │   └── user-form.html            (FR6 — role selection)
    │       │   ├── auth/
    │       │   │   └── login.html                (FR1 — Spring Security form login page)
    │       │   └── error/
    │       │       └── error.html                (global — return-to-dashboard link)
    │       │
    │       └── static/
    │           ├── css/
    │           │   └── custom.css                (--ai-feedback-bg, --mentor-decision-bg, sticky panel, border tokens)
    │           └── js/
    │               └── review-polling.js         (setInterval 3s, fetch /reviews/{id}/status,
    │                                              update ReviewStatusBadge DOM, stop on terminal state)
    │
    └── test/
        └── java/com/examinai/
            ├── review/
            │   ├── LLMReviewServiceTest.java         (unit: sanitizer + BeanOutputConverter parsing)
            │   ├── ReviewPipelineServiceTest.java     (unit: mock GitHubClient + ChatClient, PENDING-first rule)
            │   └── ReviewSubmissionControllerTest.java(@WebMvcTest: 202 response, CSRF, role access)
            ├── user/
            │   └── SecurityIntegrationTest.java      (@SpringBootTest: role routing, unauthorized redirects)
            └── integration/
                └── ReviewPipelineIntegrationTest.java (Testcontainers PG + WireMock GitHub + live Ollama optional)
```

---

### Supplement — stacks, intern access, submission gating, navigation (2026-04-26)

**Source of truth:** `docs/requirements.md` → *Platform updates — stacks, submissions, admin, and UI (2026-04-26)* and the PRD (`prd.md`) → FR34–FR39.

**Domain / data:** `Stack` entity; `Course` has required FK to `Stack`; `UserAccount` has many-to-many stacks for interns (`user_account_stack`). Liquibase changelog `006-stacks-and-course-stack.sql`.

**Application layer:** `InternTaskAccessService` enforces stack match for intern-facing task reads and submissions; `InternReviewSubmissionEligibility` centralizes when a new `TaskReview` may be started (blocks `APPROVED` / `PENDING` / `LLM_EVALUATED`); `ReviewSubmissionBlockedException` is handled by `ReviewSubmissionExceptionHandler` (redirect + flash). `TaskService.loadInternTaskPage` batches task + history + eligibility for the intern task detail view.

**Admin:** `AdminStackController` + `StackService` provide stack CRUD; course and user forms include stack selection as already wired in the app.

**UI:** `layout/base.html` marks the active nav link from `navRequestUri` / `navContextPath` (`NavViewAdvice`); styles in `custom.css` for `.nav-link.active`.

---

### Architectural Boundaries

**API Boundaries:**

| Boundary | Path | Auth | Returns |
|---|---|---|---|
| Public | `GET/POST /login`, `GET /actuator/health` | None | HTML / JSON |
| Intern | `/intern/**` | `ROLE_INTERN` or `ROLE_ADMIN` | HTML (Thymeleaf SSR) |
| Mentor | `/mentor/**` | `ROLE_MENTOR` or `ROLE_ADMIN` | HTML (Thymeleaf SSR) |
| Admin | `/admin/**` | `ROLE_ADMIN` | HTML (Thymeleaf SSR) |
| Shared JSON | `GET /reviews/{reviewId}/status` | Authenticated + owns review | JSON |

**Service Boundaries:**

| Service | Owns | Must Not |
|---|---|---|
| `ReviewPipelineService` | State transitions; async orchestration | Directly send email or render views |
| `ReviewPersistenceService` | DB writes for TaskReview + issues; event publishing | Call GitHub or LLM |
| `LLMReviewService` | ChatModel calls; LlmOutputSanitizer; BeanOutputConverter | Persist anything to DB |
| `GitHubClient` | GitHub API calls | Know about reviews or LLM |
| `NotificationService` | Email delivery; event listeners | Block on SMTP; call any repository |

**Data Boundaries:**

| Component | Reads | Writes |
|---|---|---|
| `ReviewSubmissionController` | Task, UserAccount | Nothing (delegates to service) |
| `ReviewPipelineService` | TaskReview (by id) | TaskReview.status (PENDING only) |
| `ReviewPersistenceService` | — | TaskReview, TaskReviewIssue |
| `MentorReviewController` | TaskReview, TaskReviewIssue | Nothing (delegates to service) |
| `ReviewStatusController` | TaskReview.status (read-only) | Nothing |

---

### Integration Points

**Internal Communication (Spring Events):**

```
ReviewPersistenceService
    → publishes AiReviewCompleteEvent (after LLM_EVALUATED save)
    → publishes MentorDecisionEvent   (after APPROVED/REJECTED save)

NotificationService
    ← @TransactionalEventListener(AFTER_COMMIT) AiReviewCompleteEvent → mentor email
    ← @TransactionalEventListener(AFTER_COMMIT) MentorDecisionEvent  → intern email
```

**External Integrations:**

| System | Client | Auth | Failure Handling |
|---|---|---|---|
| GitHub REST API | `GitHubClient` (@HttpExchange + RestClient) | `Authorization: Bearer {GITHUB_TOKEN}` | 404/403/429 → ERROR state |
| Ollama | Spring AI `OllamaChatModel` / `OllamaApi` | None (internal Docker network) | HTTP read timeout exceeded → ERROR state |
| SMTP | `JavaMailSender` | `MAIL_USERNAME` / `MAIL_PASSWORD` from env | Log + continue (never blocks pipeline) |

**Data Flow — Happy Path:**

```
[1] Intern POST /intern/tasks/{taskId}/submit
[2] ReviewPipelineService → persist PENDING → publish async event → return 202 + reviewId
[3] @Async thread: GitHubClient.getPrDiff → LLMReviewService.review (think-strip + parse)
        → ReviewPersistenceService.saveLLMResult → LLM_EVALUATED + issues saved
        → publish AiReviewCompleteEvent
[4] NotificationService AFTER_COMMIT → mentor email
[5] Client polls GET /reviews/{reviewId}/status every 3s
[6] Mentor POST /mentor/reviews/{reviewId}/approve|reject
        → ReviewPersistenceService.saveMentorDecision → APPROVED/REJECTED
        → publish MentorDecisionEvent
[7] NotificationService AFTER_COMMIT → intern email
[8] Client polls → terminal state → JS stops
```

---

### Development Workflow Integration

**Running locally (IDE):** Active profile `application-dev.yml` — localhost hostnames, local PostgreSQL + Ollama, Spring DevTools hot reload.

**Running via Docker Compose:** `docker compose up --build` — default `application.yml` profile, Docker service hostnames (`db`, `ollama`), app waits for healthchecks.

**Configuration Files:**

| File | Purpose | Committed? |
|---|---|---|
| `.env` | All runtime secrets and config values | No |
| `.env.example` | Template with all keys, empty values | Yes |
| `application.yml` | Production defaults (Docker hostnames) | Yes |
| `application-dev.yml` | Local dev overrides (localhost) | Yes |
| `docker-compose.yml` | 3-service stack with healthchecks + volumes | Yes |
| `Dockerfile` | Multi-stage build: Maven → JRE runtime | Yes |

## Architecture Validation Results

### Coherence Validation ✅

**Decision Compatibility:**
All technology versions are mutually compatible: Spring Boot 3.4.2 + Spring AI 1.0.0 (BOM) + Java 21 + Spring Security 6.x + Liquibase 4.31.1+ + Thymeleaf 3.1.x + PostgreSQL 16 + Bootstrap 5 (WebJar). No version conflicts detected. `RestClient` + `@HttpExchange` available in Spring 6.1+ (included in Boot 3.4.2). `@TransactionalEventListener` supported by `spring-tx` (auto-included via `spring-boot-starter-data-jpa`).

**Pattern Consistency:**
All implementation patterns align with the chosen stack. Package-by-feature structure matches Spring Boot conventions. PRG pattern is consistent with Thymeleaf SSR. `@Async` + `@TransactionalEventListener(AFTER_COMMIT)` correctly isolates email from the review pipeline. `BeanOutputConverter` is the correct Spring AI 1.0.0 structured output mechanism.

**Structure Alignment:**
Project structure directly reflects the package-by-feature decision. Template directories mirror role URL namespaces. Changelog numbering ensures correct migration order. Test structure mirrors main package structure.

---

### Requirements Coverage Validation ✅

**Functional Requirements (39/39 covered — FR34–FR39 added 2026-04-26; see PRD + `docs/requirements.md`):**

| FR Range | Category | Coverage |
|---|---|---|
| FR1–FR5 | Auth & Access Control | `SecurityConfig`, `CustomUserDetailsService`, `login.html` |
| FR6–FR9 | User Account Management | `UserAccountService`, `admin/user-*.html`, `004-seed-data.sql` |
| FR10–FR13 | Course & Task Management | `CourseController`, `TaskController`, mentor/admin templates |
| FR14–FR19 | Task Submission | `ReviewSubmissionController`, `ReviewStatusController`, `review-polling.js` |
| FR20–FR23 | AI Review Processing | `GitHubClient`, `LLMReviewService`, `ReviewPersistenceService` |
| FR24–FR28 | Mentor Review & Decision | `MentorReviewController`, `review-queue.html`, `review-detail.html` |
| FR29–FR30 | Notifications | `NotificationService`, `AiReviewCompleteEvent`, `MentorDecisionEvent` |
| FR31–FR33 | Progress Tracking | `task-list.html`, `review-queue.html`, `admin/dashboard.html` |
| FR34–FR39 | Stacks, access, submission control, nav | `Stack` / `StackService`, `AdminStackController`, `InternTaskAccessService`, `InternReviewSubmissionEligibility`, `ReviewSubmissionExceptionHandler`, `NavViewAdvice`, `006-stacks-*.sql`, `layout/base.html` |

**Non-Functional Requirements:**

| NFR | Coverage | Mechanism |
|---|---|---|
| Submit < 500ms | ✅ | 202 Accepted before any external call |
| Poll < 200ms | ✅ | DB read-only `ReviewStatusController` |
| AI pipeline completes within Ollama timeout | ✅ | Dedicated @Async thread pool; configurable Ollama read timeout (default 15m) |
| BCrypt ≥ 12 | ✅ | `BCryptPasswordEncoder(12)` in `SecurityConfig` |
| Server-side auth | ✅ | `@PreAuthorize` on every controller method |
| No secret in code | ✅ | `.env` + Docker Compose env injection only |
| Zero submission loss | ✅ | PENDING persisted before external calls |
| Graceful shutdown | ✅ | `awaitTerminationSeconds: 120` in `AsyncConfig` |
| Data survives restart | ✅ | Named Docker volumes: `db-data`, `ollama-models` |

---

### Gap Analysis Results

**Critical Gap — Resolved:**

`spring-session-jdbc` dependency missing from initialization spec. Must be added to `pom.xml`:
```xml
<dependency>
    <groupId>org.springframework.session</groupId>
    <artifactId>spring-session-jdbc</artifactId>
</dependency>
```
And `application.yml` must include:
```yaml
spring:
  session:
    store-type: jdbc
    jdbc:
      initialize-schema: never  # Liquibase handles this via 002-spring-session.sql
```

**Minor Gap — Resolved:**

`errorMessage` field on `TaskReview` is architecturally required for surfacing user-visible error messages (e.g., "GitHub PR not found") via the polling endpoint. Add to `001-init-schema.sql`:
```sql
error_message VARCHAR(500)   -- populated on ERROR status, null otherwise
```

---

### Architecture Completeness Checklist

**✅ Requirements Analysis**
- [x] Project context thoroughly analyzed (33 FRs, 7 categories)
- [x] Scale and complexity assessed (Medium — no regulatory compliance)
- [x] Technical constraints identified (Spring AI BOM, LLM output sanitization, async pipeline)
- [x] Cross-cutting concerns mapped (6 concerns documented)

**✅ Architectural Decisions**
- [x] Critical decisions documented with verified versions
- [x] Technology stack fully specified (Java 21, Spring Boot 3.4.2, etc.)
- [x] Integration patterns defined (GitHub, Ollama, SMTP, Spring Session)
- [x] Performance considerations addressed (async thread pool, write-first, poll endpoint)

**✅ Implementation Patterns**
- [x] Naming conventions established (DB, Java, URL, templates)
- [x] Structure patterns defined (package-by-feature, template organization)
- [x] Communication patterns specified (events, state machine, transitions)
- [x] Process patterns documented (PRG, write-first, error handling, LLM processing)

**✅ Project Structure**
- [x] Complete directory structure defined (all files named)
- [x] Component boundaries established (service boundary table)
- [x] Integration points mapped (data flow diagram)
- [x] Requirements to structure mapping complete (FR → file table)

---

### Architecture Readiness Assessment

**Overall Status: READY FOR IMPLEMENTATION**

**Confidence Level: High** — All decisions are specific, all versions are validated, all 33 FRs are mapped to concrete files, and all potential AI agent conflict points are explicitly ruled.

**Key Strengths:**
- Write-first pipeline makes zero submission loss architecturally guaranteed
- Spring Session JDBC gives session persistence with zero extra infrastructure
- `@TransactionalEventListener(AFTER_COMMIT)` makes email a true fire-and-forget — SMTP outages cannot roll back reviews
- `LlmOutputSanitizer` + BeanOutputConverter is specified precisely enough that any agent will implement it identically
- PRG rule eliminates an entire class of duplicate submission bugs

**Areas for Future Enhancement (Post-MVP):**
- JSON structured logging for centralized log aggregation
- `review_audit_log` table for state transition history
- GitHub PR comment posting (write AI feedback back to the PR)
- Configurable LLM model per course (multi-model support)

---

### Implementation Handoff

**First Implementation Story:** Run Spring Initializr command (step 3), then manually add to `pom.xml`: `spring-session-jdbc`, `spring-ai-bom`, `spring-ai-ollama-spring-boot-starter`, `thymeleaf-layout-dialect`, `thymeleaf-extras-springsecurity6`, `webjars/bootstrap`.

**AI Agent Implementation Order:**
1. `pom.xml` + Spring Initializr scaffold
2. Liquibase changelogs 001–004
3. `user/` package + `SecurityConfig` + `CustomUserDetailsService`
4. `course/` + `task/` packages + Thymeleaf templates
5. `review/` package — submission + pipeline + persistence + status polling
6. `notification/` package
7. `admin/` controller + templates
8. Docker Compose + Dockerfile + `.env.example`
