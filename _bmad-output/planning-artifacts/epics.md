---
stepsCompleted: ["step-01-validate-prerequisites", "step-02-design-epics", "step-03-create-stories", "step-04-final-validation"]
inputDocuments:
  - "_bmad-output/planning-artifacts/prd.md"
  - "_bmad-output/planning-artifacts/architecture.md"
  - "_bmad-output/planning-artifacts/ux-design-specification.md"
---

# ExaminAI - Epic Breakdown

## Overview

This document provides the complete epic and story breakdown for ExaminAI, decomposing the requirements from the PRD, UX Design, and Architecture into implementable stories.

## Requirements Inventory

### Functional Requirements

FR1: Users can log in with a username and password
FR2: The system enforces role-based access — Interns, Mentors, and Admins each have distinct permissions
FR3: Users are directed to their role-appropriate dashboard upon successful login
FR4: Unauthenticated users are redirected to the login page when accessing any protected route
FR5: Admin can perform all Mentor and Intern actions
FR6: Admin can create user accounts with an assigned role (INTERN, MENTOR, ADMIN)
FR7: Admin can deactivate user accounts
FR8: The system populates initial accounts (admin, mentor, intern), a sample course, and sample tasks on first startup
FR9: User passwords are stored as one-way hashed values — never as plaintext
FR10: Mentor and Admin can create, edit, and delete Courses
FR11: Mentor and Admin can create, edit, and delete Tasks within a Course
FR12: Each Task is associated with a Course and an owning Mentor
FR13: Intern can view the list of Tasks available to them
FR14: Intern can submit a Task for review by providing a GitHub repository owner, repository name, and pull request number
FR15: The system accepts a task submission immediately and returns a review identifier without waiting for AI processing to complete
FR16: Intern can view the current status of a submitted review without resubmitting
FR17: The system displays a user-visible error message when a submission cannot be processed (e.g., invalid PR, GitHub API failure) and allows the intern to resubmit
FR18: Intern can resubmit a Task after a rejection or processing failure — each attempt is recorded independently
FR19: Intern can view their submission history for each Task, including all past attempts and their outcomes
FR20: The system retrieves the pull request diff from GitHub using the submitted repository and PR details
FR21: The system submits the PR diff and task description to the configured local LLM and receives structured code review feedback
FR22: The system extracts and persists structured feedback from the LLM response: per-issue line reference, code snippet, issue description, improvement suggestion, and overall verdict
FR23: The system records a failed review state when the GitHub API or LLM call cannot be completed, preserving the original submission record
FR24: Mentor can view a list of reviews awaiting their decision, with filtering by intern, task, and review status
FR25: Mentor can view the AI-generated feedback for a submission, including all flagged issues with code context
FR26: Mentor can approve or reject a submission with optional written remarks
FR27: Mentor can reach a final decision that differs from the AI's suggested verdict
FR28: Admin can perform all Mentor review actions across all tasks and interns
FR29: The system sends an email to the assigned Mentor when an AI review is complete, including intern name, course name, and task name
FR30: The system sends an email to the Intern when a Mentor finalizes their decision, including course name, task name, final status, and mentor remarks
FR31: Intern can view their own progress across all assigned Tasks, including the current status of each
FR32: Mentor can view the task and review list across their assigned interns, filterable by intern name, task, and status
FR33: Admin can view task and review status across all interns and all mentors

### NonFunctional Requirements

NFR1: Server-rendered page load < 1 second (internal network; Thymeleaf SSR should be near-instant)
NFR2: Task submission endpoint (202 response) < 500ms — must feel immediate; intern should not wait for AI to start
NFR3: Status polling endpoint (GET /reviews/{id}/status) < 200ms — DB read only; called every 3 seconds
NFR4: AI review pipeline (background) < 90 seconds end-to-end (submit → mentor email notification)
NFR5: Login and form submit actions < 1 second
NFR6: All user passwords stored using BCrypt strength ≥ 12 — never plaintext or reversible encryption
NFR7: All secrets (GITHUB_TOKEN, DB credentials, SMTP credentials, Ollama URL) injected via environment variables; none hardcoded in source code or committed to version control
NFR8: GitHub token transmitted only as Authorization: Bearer header — never as a query parameter, never logged
NFR9: Every protected endpoint enforces role authorization server-side; Thymeleaf conditional rendering alone is not sufficient
NFR10: HTTP sessions expire after a period of inactivity (configurable via Spring Security; target: 1 hour)
NFR11: No PR diff content, mentor remarks, or review feedback exposed to users outside their authorized role scope
NFR12: GitHub API errors 404/403/429 each result in an ERROR review state with a user-visible message — no application crash
NFR13: Ollama LLM HTTP read timeout is configurable (`examinai.ai.ollama-read-timeout-ms`; default 15 minutes for CPU inference); if exceeded, review recorded as ERROR
NFR14: LLM response parsing: output normalized via `LlmOutputSanitizer` (reasoning tags, markdown fences, prose around JSON) before `BeanOutputConverter`; if parsing still fails, review recorded as ERROR rather than silently dropped
NFR15: SMTP email delivery failures are logged but do not block the review pipeline — review state updated in DB regardless of notification outcome
NFR16: Every task submission persisted as a TaskReview row with status PENDING before any external API call — no submission lost due to downstream failure
NFR17: Liquibase schema migration completes successfully before the application serves HTTP traffic
NFR18: PostgreSQL data persists across container restarts via a named Docker volume (db-data)
NFR19: Ollama model data (qwen2.5-coder:3b, ~2 GB) persists across container restarts via a named Docker volume (ollama-models)
NFR20: Async review thread pool configured with graceful shutdown: in-flight reviews complete before application exits (awaitTerminationSeconds: 120)

### Additional Requirements

- **Starter Template (Epic 1 Story 1):** Architecture specifies Spring Initializr as the starter. First story must run the Spring Initializr curl command and manually add to pom.xml: spring-session-jdbc, spring-ai-bom:1.0.0, spring-ai-ollama-spring-boot-starter, thymeleaf-layout-dialect, thymeleaf-extras-springsecurity6:3.1.2.RELEASE, org.webjars/bootstrap:5.3.x
- Spring Boot 3.4.2 + Java 21 required (hard requirement for Spring AI 1.0.x compatibility)
- Spring AI 1.0.0 via BOM; BeanOutputConverter for structured LLM output
- Package-by-feature code organization under com.examinai (user/, course/, task/, review/, notification/, config/, admin/)
- Spring Session JDBC (PostgreSQL) for HTTP session persistence across container restarts; 002-spring-session.sql changelog required
- Liquibase changelogs in strict order: 001-init-schema.sql → 002-spring-session.sql → 003-indexes.sql → 004-seed-data.sql
- @Async executor bean: core=15, max=30, queue=150, awaitTerminationSeconds=120
- @TransactionalEventListener(phase=AFTER_COMMIT) + @Async for email delivery — SMTP failure must never roll back or block the review pipeline
- Spring Events: AiReviewCompleteEvent (reviewId, mentorId, internName, courseName, taskName) and MentorDecisionEvent (reviewId, internId, courseName, taskName, finalStatus, remarks)
- Write-first pipeline rule: persist PENDING to DB before any external call, then publish async event, then return 202
- State transitions enforced only in ReviewPipelineService: PENDING→LLM_EVALUATED, PENDING→ERROR, LLM_EVALUATED→APPROVED, LLM_EVALUATED→REJECTED
- LLM response processing order: LlmOutputSanitizer (reasoning tags / fences / prose) → JSON object extraction if needed → BeanOutputConverter parse → ERROR on failure
- PRG (Post-Redirect-Get) pattern mandatory on all form handlers — PostMapping always returns "redirect:/..."
- @PreAuthorize on every controller method individually (not class-level only)
- Docker Compose with 3 services (app + postgres + ollama) with healthchecks and condition: service_healthy; named volumes db-data and ollama-models
- Environment profile separation: application.yml (production Docker hostnames) + application-dev.yml (localhost overrides)
- .env file for all secrets; .env.example committed with keys but no values
- Implementation sequence dependency order: Spring Initializr scaffold → Liquibase changelogs → User/Security → Course/Task CRUD → GitHub client → Async pipeline → Polling endpoint → Mentor review UI → Email notifications → Admin views → Docker Compose

### UX Design Requirements

UX-DR1: Implement ReviewStatusBadge Thymeleaf fragment (th:fragment="statusBadge(status)") with 6 states: PENDING (bg-secondary, label "Submitted"), AI Reviewing (text-bg-warning + spinner-sm, aria-live="polite", label "AI Reviewing"), Awaiting Mentor (text-bg-warning + spinner-sm, aria-live="polite"), APPROVED (text-bg-success, aria-live="polite"), REJECTED (text-bg-danger, aria-live="polite"), ERROR (text-bg-danger outline, aria-live="assertive", label "Review Failed")
UX-DR2: Implement AIFeedbackCard Thymeleaf fragment (th:fragment="aiIssueCard(issue)") displaying: line badge + severity label (text-danger/warning/info), code snippet (dark bg, pre/code monospace), issue description text, improvement suggestion text; with role="region" and aria-label="Issue at line N"
UX-DR3: Implement MentorActionPanel Thymeleaf fragment (th:fragment="mentorActionPanel(review)") as sticky sidebar (position: sticky; top: 72px): AI suggested verdict badge → "Your decision is final" text → remarks textarea (rows=4, optional) → Approve button (btn-success full-width) → Reject button (btn-danger full-width) → Back to Queue link; pure HTML form, POST with redirect-after-POST
UX-DR4: Implement InternStatusCard Thymeleaf fragment (th:fragment="internStatusCard(review)") with left border color keyed to status; polling JS anchored to div[data-review-id]; states: AI Reviewing/Awaiting Mentor (spinner, no extra content), APPROVED (green border, congratulatory message), REJECTED (red border, mentor remarks + AI issues + resubmit form visible), ERROR (red border outline, specific error message + resubmit form)
UX-DR5: Implement TaskStatusCard Thymeleaf fragment (th:fragment="taskStatusCard(task, review)") showing one task card with left border color: APPROVED → green (#198754), in review → amber (#ffc107), REJECTED → red (#dc3545), ERROR → red outline, not started → grey (#dee2e6)
UX-DR6: Implement review-polling.js (~30 lines vanilla JS) using setInterval every 3 seconds; fetch GET /reviews/{id}/status; update only the badge span textContent (not full element replacement to preserve screen reader announcements); stop polling on terminal states (APPROVED, REJECTED, ERROR); handle network errors gracefully with silent retry
UX-DR7: Implement custom.css (~50–100 lines) with semantic color tokens: --ai-feedback-bg (#f8f9fa), --mentor-decision-bg (#ffffff); AI content visual style (background: var(--bs-light), left border 3px solid var(--bs-warning), label "AI Suggestion" in secondary text); Mentor content visual style (white bg, left border 3px solid var(--bs-primary), label "Mentor Decision" in primary text); sticky action panel positioning
UX-DR8: Implement status badge Bootstrap palette: PENDING/AI Reviewing → badge bg-secondary; LLM_EVALUATED/Awaiting Mentor → badge bg-warning text-dark; APPROVED → badge bg-success; REJECTED/ERROR → badge bg-danger
UX-DR9: Implement Thymeleaf base layout template (layout/base.html) using Thymeleaf Layout Dialect; load Bootstrap 5 WebJar once; shared navbar with logo, role badge, username, logout; skip link (<a href="#main-content" class="visually-hidden-focusable">); CSRF meta tag in <head>
UX-DR10: Implement mentor review queue page (review-queue.html) as Bootstrap table-hover; columns: Intern Name, Task Name, AI Verdict badge, Status badge; default filter: Status = LLM_EVALUATED; filter controls via form-select onchange triggering GET with query params; cursor: pointer rows; empty state card when queue is empty
UX-DR11: Implement intern task list page (task-list.html) as Bootstrap card-grid (row g-3, col-md-4 per card) using TaskStatusCard fragment; progress bar showing percentage of tasks approved; empty state text-muted paragraph when no tasks assigned
UX-DR12: Implement intern submission form (task-detail.html) with 3 fields (repoOwner, repoName, prNumber) in col-md-4 on desktop, stacked on tablet; all required; labels above inputs; submit button btn-primary; server-side validation only; alert alert-danger above form on error; PRG pattern; submission history list-group below form
UX-DR13: Implement responsive layout adaptations at 768px: mentor review detail from 2-panel to single column (AI issues → action panel below); action panel buttons become full-width; mentor queue table wrapped in table-responsive div; intern task grid from 3-column (col-md-4) to 2-column (col-6)
UX-DR14: Implement accessibility requirements across all templates: semantic HTML (<nav>, <main id="main-content">), heading hierarchy h4→h5→h6 (no skipping), explicit <label for="..."> on every input/select, aria-live="polite" on status badge container (assertive for ERROR), role="region" on code cards, <th scope="col"> on all mentor queue table headers, skip link in base layout
UX-DR15: Implement 2-panel review detail layout (review-detail.html): full-width review header (intern name, task name, AI verdict suggestion "Suggested: REJECT/APPROVE"), col-md-8 AI feedback issues list using AIFeedbackCard fragment, col-md-4 sticky MentorActionPanel; breadcrumb navigation "Review Queue > Intern Name — Task Name"

### FR Coverage Map

FR1: Epic 1 — Form login
FR2: Epic 1 — Role-based access enforcement
FR3: Epic 1 — Role-appropriate dashboard routing on login
FR4: Epic 1 — Unauthenticated redirect to login page
FR5: Epic 1 — Admin inherits all Mentor and Intern capabilities
FR6: Epic 1 — Admin creates user accounts with assigned role
FR7: Epic 1 — Admin deactivates user accounts
FR8: Epic 1 — Seed data (admin, mentor, intern, course, tasks) on startup
FR9: Epic 1 — BCrypt password hashing (never plaintext)
FR10: Epic 2 — Course CRUD (Mentor/Admin)
FR11: Epic 2 — Task CRUD within a Course (Mentor/Admin)
FR12: Epic 2 — Task FK-linked to Course and owning Mentor
FR13: Epic 2 — Intern views list of available tasks
FR14: Epic 3 — Intern submits PR (repoOwner, repoName, prNumber)
FR15: Epic 3 — 202 Accepted + reviewId returned immediately
FR16: Epic 3 — Intern polls review status without resubmitting
FR17: Epic 3 — Error state surfaced with user-visible message and resubmit path
FR18: Epic 3 — Resubmit after rejection/failure; each attempt recorded independently
FR19: Epic 3 — Intern views submission history for each task (all attempts + outcomes)
FR20: Epic 3 — GitHub PR diff fetch via authenticated REST call
FR21: Epic 3 — Ollama LLM invocation → structured code review feedback
FR22: Epic 3 — Persist structured feedback (line, code, issue, improvement, verdict)
FR23: Epic 3 — Record ERROR state on GitHub/LLM failure; preserve original submission
FR24: Epic 4 — Mentor views filtered review queue
FR25: Epic 4 — Mentor views AI feedback with line-level code context
FR26: Epic 4 — Mentor approves/rejects with optional written remarks
FR27: Epic 4 — Mentor overrides AI suggested verdict
FR28: Epic 4 — Admin performs all Mentor review actions across all tasks and interns
FR29: Epic 5 — Email to Mentor when AI review completes (intern name, course, task)
FR30: Epic 5 — Email to Intern when Mentor finalizes decision (status, remarks)
FR31: Epic 2 — Intern views own progress across all tasks (current status per task)
FR32: Epic 4 — Mentor views cross-intern review list filterable by intern, task, status
FR33: Epic 5 — Admin views task/review status across all interns and all mentors

## Epic List

### Epic 1: Project Foundation & Authenticated Access
Users can log into the platform with role-appropriate access. All three roles (Intern, Mentor, Admin) land on their respective dashboards. Role boundaries are enforced server-side. Admins can create and deactivate user accounts. Seed data provides an immediately usable system on first startup.
**FRs covered:** FR1, FR2, FR3, FR4, FR5, FR6, FR7, FR8, FR9
**UX-DRs covered:** UX-DR9 (base layout + Bootstrap), UX-DR7 (custom.css + color tokens), UX-DR8 (badge palette), UX-DR14 (base layout accessibility foundations)

### Epic 2: Course & Task Management
Mentors and admins can create, edit, and delete Courses and Tasks with proper associations. Interns can view their assigned task list with visual progress indicators showing current status across the curriculum.
**FRs covered:** FR10, FR11, FR12, FR13, FR31
**UX-DRs covered:** UX-DR11 (intern task list card-grid), UX-DR5 (TaskStatusCard fragment)

### Epic 3: Intern Submission & AI Review Pipeline
Interns can submit GitHub PRs for any task. The system immediately acknowledges receipt (202 Accepted), asynchronously fetches the PR diff, invokes the local LLM, persists structured feedback, and lets interns track status in real-time via live polling. Errors surface with clear messages and a visible resubmit path. Every attempt is preserved in history.
**FRs covered:** FR14, FR15, FR16, FR17, FR18, FR19, FR20, FR21, FR22, FR23
**UX-DRs covered:** UX-DR1 (ReviewStatusBadge), UX-DR4 (InternStatusCard), UX-DR6 (review-polling.js), UX-DR12 (submission form + PRG), UX-DR13 (responsive intern views)

### Epic 4: Mentor Review & Final Decision
Mentors open a pre-filtered review queue (defaulting to LLM_EVALUATED), scan AI feedback with line-level code context, and make final approve/reject decisions in under 2 minutes. Mentors can override the AI verdict. Admins hold full review authority across all interns. Mentors can filter their queue by intern, task, and status.
**FRs covered:** FR24, FR25, FR26, FR27, FR28, FR32
**UX-DRs covered:** UX-DR2 (AIFeedbackCard), UX-DR3 (MentorActionPanel), UX-DR10 (mentor review queue table), UX-DR15 (2-panel review detail layout), UX-DR13 (responsive mentor views)

### Epic 5: Notifications & Admin Dashboard
Email notifications close the review loop — mentors receive alerts when AI evaluation completes; interns are notified of final decisions with mentor remarks included. Admins gain cross-intern, cross-mentor visibility with filtering to monitor cohort progression and spot stuck interns early.
**FRs covered:** FR29, FR30, FR33
**UX-DRs covered:** UX-DR14 (admin dashboard accessibility)

### Epic 6: Production Deployment
The complete platform is containerized and deployable via Docker Compose with all three services (app + PostgreSQL 16 + Ollama), health checks, persistent named volumes, environment-based secret injection, and a multi-stage Dockerfile. Any developer can run the full stack with a single command. No data is lost across container restarts.
**FRs covered:** *(no new FRs — delivers NFR16–NFR20 and all infrastructure reliability requirements)*

---

## Epic 1: Project Foundation & Authenticated Access

The full technology stack is scaffolded and configured. All three user roles (Intern, Mentor, Admin) can log in and are directed to their role-appropriate dashboards. Role boundaries are enforced server-side. Admins can create and deactivate user accounts. Seed data provides an immediately usable system on first startup.

### Story 1.1: Project Scaffold & Base Configuration

As a developer,
I want the Spring Boot project initialized with all required dependencies, configuration files, and base UI layout,
So that the team has a working foundation to build all features on.

**Acceptance Criteria:**

**Given** the Spring Initializr curl command is run with bootVersion=3.4.2, javaVersion=21, groupId=com.examinai, dependencies=web,thymeleaf,security,data-jpa,postgresql,liquibase,mail,validation,actuator
**When** the project is unzipped
**Then** a valid Maven project structure exists under `com.examinai` with `ExaminAiApplication.java` as the entry point

**Given** the project is generated
**When** `pom.xml` is inspected
**Then** these dependencies are present: `spring-session-jdbc`, `spring-ai-bom:1.0.0` in `<dependencyManagement>`, `spring-ai-ollama-spring-boot-starter`, `thymeleaf-layout-dialect`, `thymeleaf-extras-springsecurity6:3.1.2.RELEASE`, `org.webjars/bootstrap:5.3.x`

**Given** the project is configured
**When** `application.yml` is inspected
**Then** it contains production defaults (Docker service hostnames `db` and `ollama`), `spring.session.store-type: jdbc`, `spring.session.jdbc.initialize-schema: never`, and Ollama base URL pointing to the `ollama` service

**Given** the project is configured
**When** `application-dev.yml` is inspected
**Then** it overrides datasource and Ollama URLs to `localhost` and enables debug logging for `com.examinai`

**Given** the project is configured
**When** `templates/layout/base.html` is inspected
**Then** it uses Thymeleaf Layout Dialect, loads Bootstrap 5 via WebJar, contains a skip link `<a href="#main-content" class="visually-hidden-focusable">Skip to content</a>`, includes a CSRF meta tag, and renders a navbar with role badge, username, and logout link via `sec:authentication`

**Given** the project is configured
**When** `static/css/custom.css` is inspected
**Then** it defines: `--ai-feedback-bg: #f8f9fa`, `--mentor-decision-bg: #ffffff`, AI content style (light bg + `3px solid var(--bs-warning)` left border), Mentor content style (white bg + `3px solid var(--bs-primary)` left border), and sticky action panel positioning (`position: sticky; top: 72px`)

**Given** `.env.example` exists in the project root
**When** it is inspected
**Then** it contains all required keys (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `GITHUB_TOKEN`, `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `OLLAMA_BASE_URL`) with empty values and no secrets

**Given** `WebMvcConfig.java` is implemented
**When** Bootstrap assets are requested at `/webjars/**`
**Then** Spring's `ResourceHandlerRegistry` serves them from the classpath WebJar

---

### Story 1.2: User Authentication & Role-Based Access

As an authenticated user (intern, mentor, or admin),
I want to log in with my username and password and be directed to my role's dashboard,
So that I can immediately access features relevant to my role without unnecessary navigation.

**Acceptance Criteria:**

**Given** a user account exists with role INTERN
**When** they submit valid credentials on `/login`
**Then** they are redirected to `/intern/tasks`
**And** their session is persisted via Spring Session JDBC in the `spring_session` table

**Given** a user account exists with role MENTOR
**When** they submit valid credentials on `/login`
**Then** they are redirected to `/mentor/reviews`

**Given** a user account exists with role ADMIN
**When** they submit valid credentials on `/login`
**Then** they are redirected to `/admin/dashboard`

**Given** an unauthenticated user attempts to access any protected route (e.g. `/intern/tasks`)
**When** the request is received
**Then** they are redirected to `/login` with no error shown

**Given** an intern is authenticated
**When** they attempt to access `/mentor/reviews` directly
**Then** a 403 response is returned — enforced by `@PreAuthorize("hasRole('MENTOR') or hasRole('ADMIN')")` on the controller method, not by Thymeleaf alone

**Given** invalid credentials are submitted on the login form
**When** Spring Security processes the authentication
**Then** the user remains on `/login` with an authentication failure message and no session is created

**Given** Liquibase changelog `001-init-schema.sql` defines the `user_account` table (id BIGSERIAL PK, username VARCHAR UNIQUE NOT NULL, password VARCHAR NOT NULL, email VARCHAR, role VARCHAR NOT NULL, active BOOLEAN NOT NULL, date_created TIMESTAMP NOT NULL)
**When** the application starts
**Then** the table is created before the first HTTP request is served

**Given** Liquibase changelog `002-spring-session.sql` defines `spring_session` and `spring_session_attributes` tables
**When** the application starts
**Then** both tables exist and Spring Session can persist sessions immediately

**Given** a user has been idle for 1 hour (`server.servlet.session.timeout=1h`)
**When** they attempt to make a request
**Then** their session is expired and they are redirected to `/login`

---

### Story 1.3: Admin User Account Management & Seed Data

As an admin,
I want to create user accounts with assigned roles and deactivate existing accounts, with the system pre-loaded with starter data on first startup,
So that I can control platform access and the system is immediately usable without manual setup.

**Acceptance Criteria:**

**Given** the admin is on `/admin/users` and fills out the user creation form (username, email, role, initial password)
**When** the form is submitted
**Then** a new `UserAccount` is saved with the password BCrypt-hashed at strength 12, `active=true`, and the assigned role
**And** the admin is redirected back to `/admin/users` (PRG pattern)

**Given** an existing active user account is listed on `/admin/users`
**When** the admin clicks Deactivate
**Then** `active` is set to `false` and the user is immediately unable to log in (`CustomUserDetailsService` throws `DisabledException` for inactive accounts)

**Given** the application starts for the first time
**When** Liquibase changelog `004-seed-data.sql` is applied
**Then** the following accounts exist with BCrypt-hashed passwords (strength ≥ 12): admin (ADMIN), mentor (MENTOR), intern (INTERN)
**And** at least one Course and 3 Tasks with descriptions and mentor assignment are present

**Given** the admin attempts to create a user with a username that already exists
**When** the form is submitted
**Then** an inline error message is shown ("Username already exists") and no duplicate account is created

**Given** the admin views `/admin/users`
**When** the page loads
**Then** all user accounts are listed with their username, role, and active/inactive status

**Given** `@PreAuthorize("hasRole('ADMIN')")` is on `UserAccountService.createUser()` and `deactivate()`
**When** a non-admin calls these methods
**Then** an `AccessDeniedException` is thrown — server-side enforcement is authoritative

---

## Epic 2: Course & Task Management

Mentors and admins can create, edit, and delete Courses and Tasks with correct associations. Interns can view their assigned task list with visual progress indicators showing their personal status on each task.

### Story 2.1: Course Management

As a mentor or admin,
I want to create, view, edit, and delete Courses,
So that I can organize the training curriculum into structured programs.

**Acceptance Criteria:**

**Given** a mentor or admin is on `/mentor/courses` or `/admin/courses`
**When** the page loads
**Then** all existing courses are listed with course name, technology, and creation date

**Given** a mentor or admin fills out the course creation form (course name, technology) and submits
**When** the form posts
**Then** a new `Course` is saved and they are redirected to the course list (PRG pattern)

**Given** a course exists and the mentor/admin clicks Edit, updates fields, and saves
**When** the form posts
**Then** the course is updated and they are redirected to the course list

**Given** a mentor or admin clicks Delete on a course
**When** the deletion is confirmed
**Then** the course is removed from the system

**Given** a `Course` that has one or more associated `Task` records is deleted
**When** the deletion is confirmed
**Then** all `Task` rows referencing that `Course` via `course_id` FK are also deleted (cascade), and no FK constraint violation is raised

**Given** `@PreAuthorize("hasRole('MENTOR') or hasRole('ADMIN')")` is on `CourseService` CRUD methods
**When** an intern attempts to access `/mentor/courses`
**Then** a 403 is returned

**Given** a Liquibase changelog defines the `course` table (id BIGSERIAL PK, course_name VARCHAR NOT NULL, technology VARCHAR, date_created TIMESTAMP NOT NULL)
**When** the application starts
**Then** the table is created before the first HTTP request is served

---

### Story 2.2: Task Management

As a mentor or admin,
I want to create, view, edit, and delete Tasks within a Course and assign them to an owning mentor,
So that interns have structured assignments with clear descriptions and ownership.

**Acceptance Criteria:**

**Given** a mentor or admin is on `/mentor/tasks` or `/admin/tasks`
**When** the page loads
**Then** all tasks are listed with task name, associated course, owning mentor, and creation date

**Given** a mentor or admin fills out the task creation form (task name, description, course selection, mentor assignment) and submits
**When** the form posts
**Then** a new `Task` is saved with FK references to `Course` (course_id) and `UserAccount` (mentor_id), and they are redirected to the task list (PRG pattern)

**Given** a task exists and the mentor/admin clicks Edit, updates fields, and saves
**When** the form posts
**Then** the task is updated and they are redirected to the task list

**Given** a mentor or admin clicks Delete on a task
**When** the deletion is confirmed
**Then** the task is removed from the system

**Given** a `Task` that has one or more associated `TaskStatus` (or equivalent intern-progress) records is deleted
**When** the deletion is confirmed
**Then** all child rows referencing that `Task` via `task_id` FK are also deleted (cascade), and no FK constraint violation is raised

**Given** the task create/edit form renders
**When** a mentor or admin opens it
**Then** a dropdown lists all available Courses and a second dropdown lists all accounts with role MENTOR

**Given** a Liquibase changelog defines the `task` table (id BIGSERIAL PK, task_name VARCHAR NOT NULL, task_description TEXT, course_id BIGINT NOT NULL FK → course.id, mentor_id BIGINT NOT NULL FK → user_account.id, date_created TIMESTAMP NOT NULL)
**When** the application starts
**Then** the table is created before the first HTTP request

**Given** `@PreAuthorize("hasRole('MENTOR') or hasRole('ADMIN')")` is on `TaskService` CRUD methods
**When** an intern attempts to call task management operations
**Then** a 403 is returned

---

### Story 2.3: Intern Task List & Progress View

As an intern,
I want to view all available tasks as a visual card grid with status indicators showing my personal progress on each task,
So that I always know what needs to be done and can track how far I've come through the curriculum.

**Acceptance Criteria:**

**Given** an intern is authenticated and tasks exist in the system
**When** they navigate to `/intern/tasks`
**Then** tasks are displayed as a Bootstrap card-grid (`row g-3`, `col-md-4` per card) using the `TaskStatusCard` fragment

**Given** the `TaskStatusCard` fragment (`th:fragment="taskStatusCard(task, review)"`) is implemented
**When** a task has no review from the current intern (`review` is null)
**Then** the card shows a grey (`#dee2e6`) left border and label "Not Started"

**Given** the `TaskStatusCard` fragment receives a review with status `APPROVED`
**When** the card renders
**Then** the card shows a green (`#198754`) left border

**Given** the `TaskStatusCard` fragment receives a review with status `PENDING` or `LLM_EVALUATED`
**When** the card renders
**Then** the card shows an amber (`#ffc107`) left border

**Given** the `TaskStatusCard` fragment receives a review with status `REJECTED`
**When** the card renders
**Then** the card shows a red (`#dc3545`) left border

**Given** the `TaskStatusCard` fragment receives a review with status `ERROR`
**When** the card renders
**Then** the card shows a red dashed-border outline variant

**Given** a progress bar is rendered at the top of `/intern/tasks`
**When** the page loads
**Then** it shows the percentage of tasks that have at least one `APPROVED` review, computed for the current intern only

**Given** no tasks exist in the system
**When** an intern navigates to `/intern/tasks`
**Then** a `text-muted` paragraph "No tasks have been assigned yet." is shown in place of the grid

**Given** a Liquibase changelog defines the `task_review` table (id BIGSERIAL PK, task_id BIGINT NOT NULL FK → task.id, intern_id BIGINT NOT NULL FK → user_account.id, mentor_id BIGINT FK → user_account.id, status VARCHAR NOT NULL, llm_result VARCHAR, mentor_result VARCHAR, mentor_remarks TEXT, error_message VARCHAR(500), date_created TIMESTAMP NOT NULL)
**When** the application starts
**Then** the table is created (initially empty — reviews are populated in Epic 3)
**And** indexes `idx_task_review_intern_id` and `idx_task_review_status` exist on the table

**Given** `TaskService.findForIntern()` queries tasks and joins the intern's latest `TaskReview` per task
**When** no reviews exist in the database
**Then** each task is returned with `review = null` and no null pointer exception occurs

---

## Epic 3: Intern Submission & AI Review Pipeline

Interns can submit GitHub PRs for any task. The system immediately acknowledges receipt (202 Accepted), asynchronously fetches the PR diff, invokes the local LLM, persists structured feedback, and lets interns track status in real-time via live polling. Errors surface with clear messages and visible resubmit paths. Every attempt is preserved as an independent record.

### Story 3.1: Task Submission Form & Async AI Review Pipeline

As an intern,
I want to submit a GitHub PR for review and have the system automatically evaluate it using AI,
So that I receive structured code feedback without waiting for a mentor to read the diff manually.

**Acceptance Criteria:**

**Given** an intern is on `/intern/tasks/{taskId}`
**When** the page loads
**Then** a submission form is shown with 3 required fields — Repository Owner, Repository Name, Pull Request Number — each with an explicit `<label for="...">` and a submit button (`btn-primary`)
**And** the form fields are `col-md-4` on desktop and stacked (single column) on tablet (768px)

**Given** the intern fills out the form and submits
**When** the POST reaches `ReviewSubmissionController` at `POST /intern/tasks/{taskId}/submit`
**Then** the following write-first sequence executes in order: (1) `taskReview.setStatus(PENDING)`, (2) `repository.save(taskReview)` flushes to DB, (3) the async event is published, (4) `202 Accepted` + reviewId is returned
**And** the entire sequence completes in under 500ms (DB write only — no external call on the HTTP thread)

**Given** the submission is accepted
**When** `ReviewSubmissionController` returns
**Then** the intern is redirected to `/intern/reviews/{reviewId}` (PRG — `@PostMapping` always returns `"redirect:/..."`, never a view name)

**Given** `AsyncConfig` defines the task executor
**When** the application starts
**Then** the executor is configured with core=15, max=30, queue=150, `awaitTerminationSeconds=120`
**And** `@EnableAsync` is present on `AsyncConfig`

**Given** the async pipeline runs in `ReviewPipelineService.runPipeline(reviewId)`
**When** `GitHubClient.getPrDiff(repoOwner, repoName, prNumber)` is called
**Then** it issues a request to the GitHub REST API with `Authorization: Bearer {GITHUB_TOKEN}` header — the token is never logged and never passed as a query parameter

**Given** the GitHub diff is fetched successfully
**When** `LLMReviewService.review(taskDescription, prDiff)` is called
**Then** processing follows this exact order: (1) `<think>...</think>` tokens stripped via `replaceAll("(?s)<think>.*?</think>", "")`, (2) markdown fences stripped, (3) `BeanOutputConverter` parses the cleaned string into a `ReviewFeedback` record

**Given** the LLM returns valid structured feedback
**When** `ReviewPersistenceService.saveLLMResult()` is called
**Then** `TaskReview.status` is updated to `LLM_EVALUATED`, and one `TaskReviewIssue` row is saved per flagged issue (line, code, issue, improvement)
**And** an `AiReviewCompleteEvent` is published (reviewId, mentorId, internName, courseName, taskName)

**Given** a Liquibase changelog defines the `task_review_issue` table (id BIGSERIAL PK, task_review_id BIGINT NOT NULL FK → task_review.id, line INTEGER, code TEXT, issue TEXT NOT NULL, improvement TEXT)
**When** the application starts
**Then** the table is created

**Given** `@Transactional` annotation placement is inspected across the codebase
**When** all annotated classes are reviewed
**Then** `@Transactional` appears only on `Service` methods — never on `@Controller` or `@Repository` classes

---

### Story 3.2: Review Status Polling & Live Status Updates

As an intern,
I want to see my review status updating in real-time after submission,
So that I always know where my submission stands without manually refreshing the page.

**Acceptance Criteria:**

**Given** a `TaskReview` exists for the given reviewId and the authenticated user owns it
**When** `GET /reviews/{reviewId}/status` is called
**Then** a JSON response is returned within 200ms: `{reviewId, status, displayLabel, errorMessage}`
**And** `displayLabel` maps as: `PENDING` → "Submitted", `LLM_EVALUATED` → "Awaiting Mentor Review", `APPROVED` → "Approved", `REJECTED` → "Rejected", `ERROR` → "Review Failed"
**And** `errorMessage` is `null` for all non-ERROR states

**Given** an authenticated intern calls `GET /reviews/{otherInternReviewId}/status` for a review they do not own
**When** the request is processed
**Then** a 403 is returned — server-side ownership check, not UI-only

**Given** the intern is redirected to `/intern/reviews/{reviewId}`
**When** the page loads
**Then** an `InternStatusCard` (`th:fragment="internStatusCard(review)"`) is rendered with a `div[data-review-id="{reviewId}"]` anchor for the polling JS to attach to

**Given** the `ReviewStatusBadge` fragment (`th:fragment="statusBadge(status)"`) is implemented
**When** status is `PENDING`
**Then** renders as `badge bg-secondary`, label "Submitted", no spinner

**When** status is `LLM_EVALUATED`
**Then** renders as `badge text-bg-warning` with a `spinner-border spinner-border-sm` inline and `aria-live="polite"` on the container, label "Awaiting Mentor Review"

**When** status is `APPROVED`
**Then** renders as `badge text-bg-success`, label "Approved", `aria-live="polite"`

**When** status is `REJECTED`
**Then** renders as `badge text-bg-danger`, label "Rejected", `aria-live="polite"`

**When** status is `ERROR`
**Then** renders as `badge text-bg-danger` outline variant, label "Review Failed", `aria-live="assertive"`

**Given** `review-polling.js` is loaded on the review status page and status is non-terminal
**When** the page loads
**Then** `setInterval` polls `GET /reviews/{id}/status` every 3 seconds
**And** only the badge span's `textContent` is updated — the element itself is never replaced (preserves screen reader live region announcements)
**And** polling stops when status transitions to `APPROVED`, `REJECTED`, or `ERROR`

**Given** a network request inside the polling loop fails
**When** the fetch throws an error
**Then** the error is silently swallowed and the next poll attempt proceeds normally

---

### Story 3.3: Error State Handling, Submission History & Resubmit

As an intern,
I want to see a clear, specific error message when my submission fails and be able to resubmit, while keeping a full history of all past attempts,
So that I can correct mistakes without losing progress and always understand what went wrong.

**Acceptance Criteria:**

**Given** the async pipeline runs and `GitHubClient` receives a 404 response
**When** the exception is caught inside the `@Async` method
**Then** `TaskReview.status` → `ERROR`, `errorMessage` → "GitHub PR not found. Check your PR number and resubmit."
**And** the original PENDING row is preserved and the exception is NOT re-thrown

**Given** a 403 from the GitHub API is caught
**When** the error is handled
**Then** `errorMessage` → "GitHub token has insufficient permissions."

**Given** a 429 from the GitHub API is caught
**When** the error is handled
**Then** `errorMessage` → "GitHub API rate limited. Wait a few minutes and resubmit."

**Given** the Ollama call exceeds 120 seconds
**When** the timeout exception is caught
**Then** `TaskReview.status` → `ERROR`, `errorMessage` → "AI review timed out. Try resubmitting.", exception NOT re-thrown

**Given** `BeanOutputConverter` fails to parse the LLM response after think-strip and fence removal
**When** the parse exception is caught
**Then** `TaskReview.status` → `ERROR`, `errorMessage` → "AI review failed. Try resubmitting.", exception NOT re-thrown

**Given** the polling endpoint returns `ERROR`
**When** the `InternStatusCard` renders in the ERROR state
**Then** the "Review Failed" badge is shown with `aria-live="assertive"`, the specific `errorMessage` is displayed in an `alert alert-danger` block, and a resubmit form (same 3 fields) is visible without scrolling

**Given** the polling endpoint returns `REJECTED`
**When** the `InternStatusCard` renders
**Then** a red-bordered card shows the "Rejected" badge, mentor remarks (if any), and a resubmit form visible without scrolling

**Given** the polling endpoint returns `APPROVED`
**When** the `InternStatusCard` renders
**Then** a green-bordered card shows the "Approved" badge and mentor remarks (if any) — no resubmit form shown

**Given** an intern resubmits after an `ERROR` or `REJECTED` state
**When** the submission form is posted
**Then** a new `TaskReview` row is created with status `PENDING` — the previous attempt row is untouched

**Given** an intern is on `/intern/tasks/{taskId}`
**When** the page loads
**Then** a submission history `list-group` shows all past `TaskReview` attempts for that task by the current intern, ordered newest-first, with: attempt number, status badge, date submitted (`dd MMM yyyy HH:mm`), and mentor remarks if present

**Given** no submissions exist yet for the task
**When** the intern views `/intern/tasks/{taskId}`
**Then** the history section shows "No submissions yet." and only the submission form is visible

---

## Epic 4: Mentor Review & Final Decision

Mentors open a pre-filtered review queue (defaulting to LLM_EVALUATED), scan AI feedback with line-level code context, and make final approve/reject decisions in under 2 minutes. Mentors can override the AI verdict. Admins hold full review authority across all interns.

### Story 4.1: Mentor Review Queue

As a mentor or admin,
I want to see a filtered list of reviews ready for my decision with key information visible per row,
So that I can triage and prioritize my review queue without manual searching.

**Acceptance Criteria:**

**Given** a mentor navigates to `/mentor/reviews`
**When** the page loads
**Then** a `table table-hover` displays reviews with columns: Intern Name, Task Name, AI Verdict badge, Status badge
**And** the default filter is `status=LLM_EVALUATED` — only reviews ready for decision are shown
**And** all column headers use `<th scope="col">` for accessibility

**Given** filter controls exist above the table (Status, Intern Name, Task as `form-select form-select-sm`)
**When** a filter value changes (`onchange`)
**Then** the page reloads via GET with the updated query params and the table reflects the filtered results

**Given** a mentor is authenticated
**When** the `/mentor/reviews` query runs
**Then** only reviews where `task.mentor_id = currentMentor.id` are returned — mentors see only their own assigned reviews

**Given** an admin is authenticated
**When** they access `/mentor/reviews`
**Then** reviews across all mentors and all interns are shown — no mentor-scope restriction applies

**Given** the queue has no reviews matching the current filter
**When** the page renders
**Then** an empty state `card` is shown: "No reviews awaiting your decision. Check back after interns submit." — no empty table is displayed

**Given** a review row exists in the table
**When** the mentor clicks the row
**Then** they navigate to `/mentor/reviews/{reviewId}`

**Given** `@PreAuthorize("hasRole('MENTOR') or hasRole('ADMIN')")` is on `MentorReviewController.getQueue()`
**When** an intern attempts to access `/mentor/reviews`
**Then** a 403 is returned

---

### Story 4.2: Mentor Review Detail — AI Feedback & Decision

As a mentor,
I want to open a review, scan all AI-flagged issues with code context, and make my final approve or reject decision with optional remarks,
So that I can complete a full review cycle in under 2 minutes without leaving the page.

**Acceptance Criteria:**

**Given** a mentor opens `/mentor/reviews/{reviewId}`
**When** the page loads
**Then** a full-width review header shows: intern name, task name, course name, and AI suggested verdict labeled "AI Suggestion: APPROVE" or "AI Suggestion: REJECT" — never "Decision:"
**And** a breadcrumb reads "Review Queue > {InternName} — {TaskName}" with "Review Queue" linking back to `/mentor/reviews`

**Given** the review detail page renders
**When** the AI feedback section loads
**Then** each `TaskReviewIssue` is rendered as an `AIFeedbackCard` (`th:fragment="aiIssueCard(issue)"`) containing: a line badge, a `<pre><code>` code snippet (dark background, monospace) with `role="region"` and `aria-label="Code at line N"`, an issue description, and an improvement suggestion

**Given** the review detail page renders
**When** the `MentorActionPanel` (`th:fragment="mentorActionPanel(review)"`) loads
**Then** it is positioned sticky (`position: sticky; top: 72px`) in the `col-md-4` column and contains: AI Suggestion badge, "Your decision is final" text, a remarks `textarea` (rows=4, not required), an Approve button (`btn-success` full-width), a Reject button (`btn-danger` full-width), and a "← Back to Queue" link (`text-muted small`)

**Given** the mentor clicks Approve (with or without remarks)
**When** `POST /mentor/reviews/{reviewId}/approve` is processed
**Then** `ReviewPersistenceService.saveMentorDecision()` sets `TaskReview.status` → `APPROVED`, stores `mentorRemarks`, publishes `MentorDecisionEvent`, and the mentor is redirected to `/mentor/reviews` (PRG)

**Given** the mentor clicks Reject (with or without remarks)
**When** `POST /mentor/reviews/{reviewId}/reject` is processed
**Then** `ReviewPersistenceService.saveMentorDecision()` sets `TaskReview.status` → `REJECTED`, stores `mentorRemarks`, publishes `MentorDecisionEvent`, and the mentor is redirected to `/mentor/reviews`

**Given** the AI suggested verdict is REJECT and the mentor clicks Approve
**When** the decision is saved
**Then** `TaskReview.status` is `APPROVED` and `mentor_result` is `"APPROVED"` — no validation prevents the override

**Given** state transition logic is inspected
**When** the codebase is reviewed
**Then** `LLM_EVALUATED → APPROVED/REJECTED` transitions occur only inside `ReviewPersistenceService.saveMentorDecision()` — never directly in a controller

**Given** an admin accesses `/mentor/reviews/{reviewId}/approve` or `/reject`
**When** the request is processed
**Then** they can make the decision regardless of which mentor is assigned to the task (`@PreAuthorize("hasRole('MENTOR') or hasRole('ADMIN')")`)

**Given** a mentor tries to approve or reject a review assigned to a different mentor
**When** the request is processed
**Then** a 403 is returned — mentor scope is enforced server-side (admin is exempt)

**Given** the review detail page is viewed at 768px viewport
**When** the layout collapses
**Then** the `col-md-4` action panel appears below the `col-md-8` issues list as a full-width block, Approve and Reject buttons are full-width, and the mentor queue table is wrapped in `table-responsive`

---

## Epic 5: Notifications & Admin Dashboard

Email notifications close the review loop — mentors receive alerts when AI evaluation completes; interns are notified of final decisions with mentor remarks. Admins gain cross-intern, cross-mentor visibility with filtering to monitor cohort progression and spot stuck interns early.

### Story 5.1: Email Notifications

As a mentor or intern,
I want to receive email notifications at key moments in the review lifecycle,
So that I don't have to constantly check the platform and the review loop closes automatically.

**Acceptance Criteria:**

**Given** `ReviewPersistenceService.saveLLMResult()` saves `LLM_EVALUATED` status and publishes `AiReviewCompleteEvent`
**When** the DB transaction commits
**Then** `NotificationService.onAiReviewComplete()` fires via `@TransactionalEventListener(phase = AFTER_COMMIT)` in a separate `@Async` thread — it does NOT run before the commit and does NOT block the pipeline thread

**Given** `NotificationService.onAiReviewComplete()` fires
**When** `JavaMailSender.send()` executes
**Then** an email is sent to the assigned mentor's email address containing: intern name, course name, task name, and a prompt to review the submission

**Given** `ReviewPersistenceService.saveMentorDecision()` saves `APPROVED` or `REJECTED` and publishes `MentorDecisionEvent`
**When** the DB transaction commits
**Then** `NotificationService.onMentorDecision()` fires via `@TransactionalEventListener(phase = AFTER_COMMIT)` in a separate `@Async` thread

**Given** `NotificationService.onMentorDecision()` fires
**When** `JavaMailSender.send()` executes
**Then** an email is sent to the intern's email address containing: course name, task name, final status (APPROVED or REJECTED), and mentor remarks — if no remarks were provided, the email shows "No remarks provided."

**Given** `NotificationService.onAiReviewComplete()` fires and the SMTP call throws any exception
**When** the exception is caught
**Then** it is logged via `log.error(...)` but NOT re-thrown — the `TaskReview` state in the DB is unaffected and the review pipeline does not roll back

**Given** `NotificationService.onMentorDecision()` fires and the SMTP call throws any exception
**When** the exception is caught
**Then** it is logged but NOT re-thrown — the mentor's decision is already persisted and is not reversed

**Given** SMTP configuration is inspected in `application.yml`
**When** the config is reviewed
**Then** `spring.mail.host`, `spring.mail.port`, `spring.mail.username`, `spring.mail.password` are all sourced from environment variables (`MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`) — no credentials are hardcoded in any source file

**Given** `NotificationService` methods are annotated with `@Async`
**When** the application starts
**Then** they use the same executor bean configured in `AsyncConfig` (core=15, max=30, queue=150) — no separate executor is created

---

### Story 5.2: Admin Cross-Intern Dashboard

As an admin,
I want to view all task submissions across all interns and mentors with filtering,
So that I can monitor cohort progression, identify interns with consecutive rejections, and spot blocked pipelines early.

**Acceptance Criteria:**

**Given** an admin navigates to `/admin/dashboard`
**When** the page loads
**Then** a table displays all `TaskReview` records across all interns and mentors with columns: Intern Name, Task Name, Course Name, Status badge, Date Submitted (`dd MMM yyyy HH:mm`), Mentor Remarks (truncated if long)
**And** records are ordered newest-first by `date_created`

**Given** filter controls exist on `/admin/dashboard` (by Intern Name, Task Name, Status as `form-select form-select-sm`)
**When** a filter value changes (`onchange`)
**Then** the page reloads via GET with updated query params and the table reflects the filtered results

**Given** an admin filters by a specific intern
**When** the table renders
**Then** only `TaskReview` rows where `intern_id` matches the selected intern are shown — including all their attempts, not just the latest

**Given** `@PreAuthorize("hasRole('ADMIN')")` is on `AdminController.dashboard()`
**When** a mentor or intern attempts to access `/admin/dashboard`
**Then** a 403 is returned

**Given** no task reviews exist in the system
**When** the admin views `/admin/dashboard`
**Then** a `text-muted` paragraph "No submissions yet." is shown in place of the table

**Given** the admin dashboard page renders
**When** reviewed for accessibility
**Then** all table column headers use `<th scope="col">`, the page has `<main id="main-content">`, and the heading hierarchy follows h4 → h5 without skipping levels

---

## Epic 6: Production Deployment

The complete platform is containerized and deployable via a single `docker compose up --build` command. All three services start in the correct order with health checks. Data persists across restarts. Any developer can run the full stack by providing a `.env` file.

### Story 6.1: Production-Ready Docker Compose Deployment

As a developer or operations engineer,
I want the complete ExaminAI platform deployable via Docker Compose with a single command,
So that anyone can run the full stack locally or in production without manual configuration beyond supplying a `.env` file.

**Acceptance Criteria:**

**Given** a multi-stage `Dockerfile` is implemented
**When** it is inspected
**Then** stage 1 uses a Maven image to build the fat JAR (`mvn package -DskipTests`), and stage 2 copies only the JAR into a slim JRE runtime image (`eclipse-temurin:21-jre`) — no Maven or source code in the final image

**Given** `docker-compose.yml` defines three services: `postgres`, `ollama`, and `app`
**When** the file is inspected
**Then** the `postgres` service uses image `postgres:16`, mounts the `db-data` named volume at `/var/lib/postgresql/data`, and has a healthcheck: `pg_isready -U ${DB_USERNAME}`

**Given** the `ollama` service in `docker-compose.yml`
**When** the file is inspected
**Then** it uses image `ollama/ollama`, mounts the `ollama-models` named volume at `/root/.ollama`, and has a healthcheck: `curl -f http://localhost:11434/api/tags`
**And** its entrypoint is `/bin/sh -c "ollama serve & sleep 5 && ollama pull qwen2.5-coder:3b && wait"` — pre-pulling the model on first start so it is available before the app serves any review requests

**Given** the `app` service in `docker-compose.yml`
**When** the file is inspected
**Then** it declares `depends_on` with `condition: service_healthy` for both `postgres` and `ollama` — the Spring Boot app does not start until both dependencies pass their healthchecks
**And** it has its own healthcheck using `curl -f http://localhost:8080/actuator/health`
**And** all secrets and config are injected via `env_file: .env` — no secrets appear in `docker-compose.yml` itself

**Given** `docker-compose.yml` defines named volumes
**When** the file is inspected
**Then** `db-data` and `ollama-models` are declared under the top-level `volumes:` key
**And** both volumes survive `docker compose down` — data is not destroyed on container stop

**Given** a `.env` file containing valid values for all keys from `.env.example`
**When** `docker compose up --build` is run from the project root
**Then** all three services start successfully, Liquibase migrations complete, seed data is loaded, and the application serves HTTP traffic on port 8080

**Given** the stack is running and the `postgres` container is restarted
**When** postgres restarts and becomes healthy
**Then** all previously created `TaskReview`, `Course`, `Task`, and `UserAccount` records are still present — data is not lost on container restart

**Given** the Ollama container restarts after `qwen2.5-coder:3b` was already pulled
**When** the container starts
**Then** the model is available immediately from the `ollama-models` volume — no re-download occurs

**Given** the Spring Boot app receives a shutdown signal (e.g. `docker compose stop`)
**When** in-flight `@Async` review pipeline tasks are running
**Then** the executor waits up to 120 seconds for them to complete before forcing shutdown (`awaitTerminationSeconds: 120` in `AsyncConfig`) — no in-flight reviews are silently dropped

**Given** Spring Actuator is on the classpath
**When** `GET /actuator/health` is called
**Then** it returns `{"status":"UP"}` — used by the Docker healthcheck to gate dependent service startup

**Given** the production `application.yml` profile is active inside Docker Compose
**When** datasource and Ollama URLs are inspected at runtime
**Then** they reference Docker Compose service hostnames (`db` for postgres, `ollama` for Ollama) — not `localhost`
