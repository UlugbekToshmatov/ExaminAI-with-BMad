---
stepsCompleted: ["step-01-init", "step-02-discovery", "step-02b-vision", "step-02c-executive-summary", "step-03-success", "step-04-journeys", "step-05-domain", "step-06-innovation", "step-07-project-type", "step-08-scoping", "step-09-functional", "step-10-nonfunctional", "step-11-polish"]
inputDocuments: ["docs/requirements.md", "_bmad-output/planning-artifacts/research/technical-ai-mentorship-platform-research-2026-04-19.md"]
workflowType: 'prd'
briefCount: 0
researchCount: 1
brainstormingCount: 0
projectDocsCount: 1
classification:
  projectType: web_app
  domain: edtech
  complexity: medium
  projectContext: brownfield
---

# Product Requirements Document — ExaminAI

**Author:** Ulugbek
**Date:** 2026-04-19

## Executive Summary

ExaminAI is a web-based code review platform that eliminates the mentor bottleneck in intern training programs. Interns submit GitHub pull requests for assigned tasks; a locally-hosted AI (`qwen2.5-coder:3b` via Ollama) produces structured, line-level feedback asynchronously—typically minutes on CPU for modest diffs, bounded by a configurable Ollama HTTP read timeout (default 15 minutes); mentors review the AI draft and make the final approve/reject decision. Interns get structured feedback far sooner than manual queue times; mentors spend about 2 minutes per review instead of 20.

**Target users:**
- **Interns** — software development trainees submitting code for structured tasks
- **Mentors** — senior developers who assign tasks and make final review decisions
- **Admins** — program managers with full platform access

Mentor availability is the single biggest constraint on intern progression through a training curriculum. ExaminAI removes the slow part of code review (reading diffs, identifying issues, writing comments) from the mentor's plate entirely.

### What Makes This Special

The AI never replaces the mentor's judgment — it replaces their reading time. Mentors receive a structured AI draft (flagged issues, code references, suggested verdict) and make the final call. Human accountability is preserved; the bottleneck is eliminated.

The platform runs on a **locally-hosted LLM** (Ollama + qwen2.5-coder:3b) — zero per-review API cost, all code stays on-premises, no scaling costs as the intern cohort grows.

The async review pipeline (submit → 202 Accepted → poll → notification) ensures interns are never blocked waiting for a UI response during 10–60 second LLM inference.

## Project Classification

- **Project Type:** Web Application (server-side MPA — Spring MVC + Thymeleaf, role-based views per user type)
- **Domain:** EdTech / Developer Mentorship
- **Complexity:** Medium (no regulatory compliance; complexity from AI integration, async pipeline, and multi-role access control)
- **Project Context:** Brownfield — detailed requirements (`docs/requirements.md`) and validated technical research exist; this PRD formalizes and extends them

## Success Criteria

### User Success

- **Intern:** A submitted task reaches `APPROVED` status — this is the completion event. Every other interaction (submission form, AI feedback display, status polling, notifications) exists to support this outcome.
- **Mentor:** A complete review cycle — read AI draft, verify issues, approve or reject with remarks — takes ≤ 2 minutes per submission. Longer indicates AI feedback quality or UI failure.
- **Both:** No submission is lost, stuck in `PENDING` indefinitely, or fails silently. The system surfaces errors visibly.

### Business Success

- Mentors process ≥ 10 reviews per day (vs ~3 in a manual review workflow)
- Intern task completion rate ≥ 80% within deadlines
- Average time from intern submission to mentor final decision: < 30 minutes

### Technical Success

- AI review stage latency: bounded by configurable Ollama HTTP read timeout (default 15 minutes); `qwen2.5-coder:3b` targets much faster turnaround than larger reasoning models on CPU
- LLM JSON parse success rate: > 95%
- GitHub API error rate: < 1% (track 403/404/429 responses)
- Application startup time (Liquibase migration + Ollama healthcheck): < 45 seconds
- Submission loss rate: 0% — every attempt persisted as a separate `TaskReview` row

### Measurable Outcomes

| Metric | Target |
|---|---|
| Time to mentor notification after AI completes | Bounded by Ollama timeout (default 15m); typically minutes with 3B coder on CPU |
| Mentor time per review | ≤ 2 minutes |
| Mentor daily review capacity | ≥ 10 reviews/day |
| Intern task completion rate | ≥ 80% on time |
| LLM parse success rate | > 95% |
| Submission loss rate | 0% |

## Product Scope & Development Phases

### MVP Strategy

**Approach:** Problem-solving MVP — prove that AI-assisted code review with human-in-the-loop mentor authority reduces review time to ≤ 2 minutes and delivers structured AI feedback asynchronously without blocking the intern UI. No feature is in scope unless it directly enables this loop.

**Resource requirements:** 1–2 Java developers with Spring Boot experience. Spring AI integration (Ollama, BeanOutputConverter, async pipeline) is the steepest learning curve — budget 1 week for this component. Docker Compose + Ollama administration requires basic DevOps familiarity. Solo developer build priority: (1) async pipeline + DB → (2) mentor review UI → (3) intern submission UI → (4) email notifications → (5) admin CRUD.

### Phase 1 — MVP

**Core journeys enabled:**
- Intern submits task → AI reviews → mentor approves/rejects → intern notified (Journey 1)
- Failed submission surfaces error state; intern resubmits (Journey 2)
- Mentor processes queued reviews sequentially from filtered list (Journey 3)
- Admin creates courses, tasks, and user accounts (Journey 4)

**Must-have capabilities:**
- Spring Security form login with 3 roles (INTERN, MENTOR, ADMIN)
- Course CRUD and Task CRUD (mentor/admin)
- Intern task submission form (repo owner, repo name, PR number)
- Async review pipeline: GitHub diff fetch → Ollama LLM → structured JSON → DB persist
- `LlmOutputSanitizer` + `BeanOutputConverter` for structured JSON from `qwen2.5-coder:3b`
- Status polling endpoint (`GET /reviews/{reviewId}/status`) + JS polling every 3 seconds
- Error state (`ERROR`) surfaced to intern UI when GitHub/LLM call fails
- Mentor review UI: AI feedback display (line, code, issue, improvement) + approve/reject with remarks
- Intern task list (own tasks + review history)
- Mentor task list with filters (intern, task, status)
- Email notifications via Spring Mail: mentor on AI complete, intern on final decision
- Seed data on startup (admin, mentor, intern, course, tasks — BCrypt hashed passwords)
- Docker Compose: app + PostgreSQL 16 + Ollama with healthchecks and named volumes
- `.env` file for all secrets and configuration

### Phase 2 — Growth (Post-MVP)

- Task deadlines: deadline field on `Task`, overdue indicators on intern dashboard
- User management UI: create/edit/deactivate accounts (replace seed-only approach)
- Analytics: mentor workload, intern progression rate, average review latency
- GitHub PR comment posting: write AI + mentor feedback back to the PR
- Configurable AI prompts per course/technology
- Re-submission attempt history UI (visible to both intern and mentor)

### Phase 3 — Expansion (Vision)

- Multi-LLM support: swap `qwen2.5-coder:3b` for other Ollama models or cloud APIs per course
- Peer review: intern-to-intern review before mentor sees submission
- CI/CD webhook: auto-trigger submission on GitHub PR open event
- Cohort/group management with program-level reporting dashboard
- Multi-tenant SaaS version with organization isolation

### Project Risk Mitigation

**Technical risks:**

| Risk | Severity | Mitigation |
|---|---|---|
| Spring AI + LLM JSON not matching `ReviewFeedback` after sanitization | High | `LlmOutputSanitizer` + extract JSON object fallback; unit tests with fenced / prose-wrapped samples |
| Ollama container OOM on low-RAM host | High | 16 GB RAM minimum; 4-bit quantized model as fallback |
| GitHub API 404 / 403 on bad PR input | Medium | Validate API response before assembling prompt; surface `ERROR` state immediately |
| Async thread pool exhaustion | Low | Configure core=15, max=30, queue=150 |

**Adoption risk:** Mentors may default to reviewing manually. The email notification with AI draft is the primary adoption hook — it must arrive before they open GitHub. AI feedback display must be fast to read and act on.

## User Journeys

### Journey 1: Intern — Happy Path — "Dilnoza Gets Her First Approval"

**Persona:** Dilnoza, a Java intern two weeks into her program. Motivated, but nervous about code quality. Hates waiting — uncertainty kills her momentum.

**Opening scene:** Dilnoza finishes Task #3 (implement a REST endpoint with error handling). She pushes to GitHub, opens a PR, and goes to ExaminAI. She selects the task from her list, pastes in the repo owner, repo name, and PR number, and hits Submit.

**Rising action:** The page immediately confirms receipt — "Submitted. AI review in progress." Forty seconds later, her browser tab updates — the AI has flagged two issues: a missing null check on line 34, and an inconsistent naming convention in the service layer.

**Climax:** Mentor Bobur gets a notification email with Dilnoza's name, course, task name, and the AI's structured feedback. He reads the two flagged issues — they're correct — adds a remark ("Fix the null check, naming is minor but worth addressing"), and clicks **Reject**. Dilnoza gets an email: Task #3 Rejected — here's why.

**Resolution:** Dilnoza fixes both issues, resubmits. The AI finds no major issues. Bobur approves in 90 seconds. Dilnoza gets the email she's been working toward: **Task #3 — APPROVED.**

**Capabilities revealed:** Task list view for interns, submission form (repo owner/name/PR number), real-time status polling, AI feedback display, email notification with mentor remarks, per-task submission history.

---

### Journey 2: Intern — Edge Case — "Azizbek's Broken Submission"

**Persona:** Azizbek, a Python intern who moves fast and doesn't double-check. He pastes the wrong PR number.

**Opening scene:** Azizbek submits Task #5. The system accepts immediately (202 Accepted). The GitHub API returns 404 — the PR doesn't exist.

**The problem moment:** If the system fails silently, Azizbek stares at a spinner for 10 minutes. His mentor gets no notification. The task is invisible.

**Resolution (what the system must do):** Review status moves to `ERROR` with a message: "GitHub PR not found. Please check your PR number and resubmit." Azizbek resubmits with the correct PR number. The failed attempt is preserved as a separate row.

**Capabilities revealed:** Error state on `TaskReview` for GitHub/LLM failures, user-visible error message on status polling, resubmit after failure without losing history.

---

### Journey 3: Mentor — Daily Review Sprint — "Bobur's 8-Minute Morning"

**Persona:** Bobur, a senior Java developer mentoring 8 interns. His time is scarce. He resents tasks that take longer than they should.

**Opening scene:** Bobur opens his inbox at 9 AM — 4 notification emails, 4 AI-evaluated reviews waiting. Previously, each took 15–20 minutes.

**Rising action:** His task list is pre-filtered to `LLM_EVALUATED`. Review #1: 3 flagged issues with line numbers, code snippets, and improvement suggestions. All correct. He clicks **Approve**, types "Good work overall." 90 seconds. Review #2: the AI suggested REJECT; Bobur disagrees and overrides to **Approve** with a note. Still under 2 minutes.

**Climax:** Four reviews done. Total time: 8 minutes.

**Resolution:** All 4 interns get email notifications. Two approved, two rejected with specific feedback.

**Capabilities revealed:** Mentor task list filtered by status, AI feedback display with line-level context, approve/reject with remarks, AI verdict override, sequential review workflow, outbound email notification on decision.

---

### Journey 4: Admin — Program Setup — "Ulugbek Launches a New Cohort"

**Persona:** Ulugbek, the program administrator. He sets up courses and tasks, creates accounts, and monitors cohort progression.

**Opening scene:** A new Java Backend cohort starts Monday. Ulugbek logs in as Admin.

**Rising action:** He creates Course "Java Backend Fundamentals," adds 8 Tasks with descriptions, assigns Bobur as mentor. He creates 5 intern accounts and 1 mentor account with roles and temporary passwords.

**Climax:** Two weeks in, Ulugbek filters the cross-intern task list and notices one intern has 3 consecutive rejections with no resubmission — he flags it to Bobur.

**Capabilities revealed:** Course CRUD, Task CRUD with mentor assignment, user account creation and role assignment, cross-intern task list with filters (intern, group, task), admin inherits all mentor + intern capabilities.

---

### Journey Requirements Summary

| Capability | Driven By |
|---|---|
| Task list with status per intern | Journey 1, 3, 4 |
| Submission form (repo owner, repo name, PR number) | Journey 1, 2 |
| Real-time status polling (3-second interval) | Journey 1, 2 |
| AI feedback display (line, code, issue, improvement) | Journey 1, 3 |
| Error state display on failed review | Journey 2 |
| Resubmit after failure or rejection | Journey 2 |
| Approve/reject with remarks + AI override | Journey 3 |
| Email notifications (mentor on AI complete, intern on decision) | Journey 1, 3 |
| Mentor task list filtered by status/intern/task | Journey 3, 4 |
| Course and Task CRUD with mentor assignment | Journey 4 |
| User account creation and role management | Journey 4 |
| Admin cross-intern visibility with filters | Journey 4 |

## Domain-Specific Requirements

### Data Privacy & Code Confidentiality

- Intern code (GitHub PR diffs) is processed exclusively by the locally-hosted LLM (Ollama + qwen2.5-coder:3b). No PR content is sent to external APIs or cloud services.
- GitHub PR data is fetched per review via a single authenticated API call. Data is not cached beyond the review cycle.
- Interns submit from personal GitHub repositories. A fine-grained PAT (`GITHUB_TOKEN`) with repository read scope is sufficient — no organization-level permissions required.

### Credential & Secret Security

- `GITHUB_TOKEN`, database credentials (`DB_*`), SMTP credentials (`MAIL_*`), and Ollama URL are supplied exclusively via environment variables (`.env` file, Docker Compose env injection).
- No credentials are hardcoded, logged, or exposed in HTTP responses or Thymeleaf-rendered HTML.
- GitHub token is transmitted only as `Authorization: Bearer {GITHUB_TOKEN}` — never as a query parameter.

### Role Boundary Enforcement

- Interns can only view their own submissions and results — no cross-intern visibility.
- Mentors see tasks and reviews within their assigned scope; Admin sees all.
- Role enforcement is applied at Spring Security filter chain level and method level (`@PreAuthorize`) — UI-only hiding is insufficient.

### Submission Audit Trail

- Every submission attempt is persisted as a distinct `TaskReview` row regardless of outcome (pending, error, rejected, approved).
- Approval history is the authoritative record for intern progress. No record is overwritten or deleted on resubmission.
- Long-term data retention is acceptable — no expiry, archival, or deletion requirements for this version.

### Security Risk Mitigations

| Risk | Mitigation |
|---|---|
| GitHub token exposed in logs | Never log request headers; exclude token from application logs |
| Intern views another intern's review | Server-side `@PreAuthorize` check on every review endpoint |
| LLM receives malformed PR data | Validate GitHub API response before assembling prompt; surface error state if empty |
| Submission lost on LLM/GitHub failure | Persist `TaskReview` at submission time (status `PENDING`); update to `ERROR` on failure |

## Innovation & Novel Patterns

### Detected Innovation Areas

**Human-in-the-loop code review pipeline:** ExaminAI separates AI capability from AI authority. The LLM performs the first-pass analysis (reading diffs, identifying issues, writing structured feedback); the mentor holds the final approve/reject decision. This is distinct from fully-automated review tools and from unaided human review — AI eliminates reading time without eliminating human judgment.

**Zero-cost, on-premises LLM inference:** qwen2.5-coder:3b runs locally via Ollama (Docker container) — unlimited reviews with no per-call API cost and no external data exposure. Viable for any intern cohort size without scaling costs.

**Async review UX in a synchronous-feeling product:** The 202 Accepted + client-side polling pattern delivers a non-blocking experience for a 10–60 second process — a UX challenge most internal tools handle poorly.

### Validation Approach

- Mentor ≤ 2-minute review cycle is the primary validation signal — longer indicates AI feedback quality or UI layout failure.
- LLM JSON parse success rate > 95% validates that the `qwen2.5-coder:3b` + `LlmOutputSanitizer` pipeline is production-reliable.

### Innovation Risk Mitigations

| Risk | Mitigation |
|---|---|
| qwen2.5-coder:3b produces low-quality feedback | Mentors can always override; poor AI output doesn't block the workflow |
| Local LLM too slow on CPU-only hardware | Default model `qwen2.5-coder:3b` targets CPU; increase `examinai.ai.ollama-read-timeout-ms` if needed; optional GPU or larger Ollama model for quality |
| Async polling confuses interns | Clear UI state labels: Submitted / AI Reviewing / Awaiting Mentor / Approved / Rejected |

## Web Application Requirements

### Project-Type Overview

ExaminAI is a server-side Multi-Page Application (MPA) built with Spring MVC + Thymeleaf. Each role (Intern, Mentor, Admin) has a distinct view set rendered server-side. Navigation triggers full page loads via HTTP GET/POST. JavaScript is used only for client-side polling and minor UI interactions.

### Technical Architecture Considerations

- **Rendering:** Server-side Thymeleaf templates. Role-specific template sets under `/intern/`, `/mentor/`, `/admin/` URL namespaces.
- **State management:** Session-based (Spring Security `SecurityContext`). No client-side state store.
- **Polling mechanism:** Vanilla JavaScript `setInterval` polling `GET /reviews/{reviewId}/status` every 3 seconds, stopping on terminal state (`APPROVED`, `REJECTED`, `ERROR`).
- **Form handling:** Standard HTML forms with POST submission and redirect-after-POST to prevent duplicate submissions.

### Browser Matrix

| Browser | Support Level |
|---|---|
| Chrome (last 2 versions) | Full |
| Edge (last 2 versions) | Full |
| Firefox (last 2 versions) | Full |
| Safari (last 2 versions) | Full |
| Internet Explorer | Not supported |

### Responsive Design

- Desktop-first layout (1024px+ primary target)
- Minimum usable at 768px for tablet access
- No mobile-first optimization required for MVP

### SEO Strategy

None required — all routes are behind Spring Security authentication. No public-facing pages.

### Accessibility Level

Semantic HTML with proper heading hierarchy and form labels. No WCAG 2.1 AA compliance required for this internal tool version.

### Implementation Considerations

- Thymeleaf `sec:authorize` attributes for conditional UI rendering by role (requires `thymeleaf-extras-springsecurity6`)
- `@PreAuthorize` on all controllers/services as the authoritative access check
- Redirect-after-POST on all form submissions to prevent double-submission on refresh
- Polling JavaScript handles network errors gracefully: retry silently, surface error only after repeated failure

Performance requirements are specified in the Non-Functional Requirements section.

## Functional Requirements

### Authentication & Access Control

- **FR1:** Users can log in with a username and password
- **FR2:** The system enforces role-based access — Interns, Mentors, and Admins each have distinct permissions
- **FR3:** Users are directed to their role-appropriate dashboard upon successful login
- **FR4:** Unauthenticated users are redirected to the login page when accessing any protected route
- **FR5:** Admin can perform all Mentor and Intern actions

### User Account Management

- **FR6:** Admin can create user accounts with an assigned role (INTERN, MENTOR, ADMIN)
- **FR7:** Admin can deactivate user accounts
- **FR8:** The system populates initial accounts (admin, mentor, intern), a sample course, and sample tasks on first startup
- **FR9:** User passwords are stored as one-way hashed values — never as plaintext

### Course & Task Management

- **FR10:** Mentor and Admin can create, edit, and delete Courses
- **FR11:** Mentor and Admin can create, edit, and delete Tasks within a Course
- **FR12:** Each Task is associated with a Course and an owning Mentor
- **FR13:** Intern can view the list of Tasks available to them

### Task Submission

- **FR14:** Intern can submit a Task for review by providing a GitHub repository owner, repository name, and pull request number
- **FR15:** The system accepts a task submission immediately and returns a review identifier without waiting for AI processing to complete
- **FR16:** Intern can view the current status of a submitted review without resubmitting
- **FR17:** The system displays a user-visible error message when a submission cannot be processed (e.g., invalid PR, GitHub API failure) and allows the intern to resubmit
- **FR18:** Intern can resubmit a Task after a rejection or processing failure — each attempt is recorded independently
- **FR19:** Intern can view their submission history for each Task, including all past attempts and their outcomes

### AI Review Processing

- **FR20:** The system retrieves the pull request diff from GitHub using the submitted repository and PR details
- **FR21:** The system submits the PR diff and task description to the configured local LLM and receives structured code review feedback
- **FR22:** The system extracts and persists structured feedback from the LLM response: per-issue line reference, code snippet, issue description, improvement suggestion, and overall verdict
- **FR23:** The system records a failed review state when the GitHub API or LLM call cannot be completed, preserving the original submission record

### Mentor Review & Decision

- **FR24:** Mentor can view a list of reviews awaiting their decision, with filtering by intern, task, and review status
- **FR25:** Mentor can view the AI-generated feedback for a submission, including all flagged issues with code context
- **FR26:** Mentor can approve or reject a submission with optional written remarks
- **FR27:** Mentor can reach a final decision that differs from the AI's suggested verdict
- **FR28:** Admin can perform all Mentor review actions across all tasks and interns

### Notifications

- **FR29:** The system sends an email to the assigned Mentor when an AI review is complete, including intern name, course name, and task name
- **FR30:** The system sends an email to the Intern when a Mentor finalizes their decision, including course name, task name, final status, and mentor remarks

### Progress Tracking & Visibility

- **FR31:** Intern can view their own progress across all assigned Tasks, including the current status of each
- **FR32:** Mentor can view the task and review list across their assigned interns, filterable by intern name, task, and status
- **FR33:** Admin can view task and review status across all interns and all mentors

## Non-Functional Requirements

### Performance

| Operation | Requirement | Rationale |
|---|---|---|
| Server-rendered page load | < 1 second | Internal network; Thymeleaf SSR should be near-instant |
| Task submission endpoint (202 response) | < 500ms | Must feel immediate — intern should not wait for AI to start |
| Status polling endpoint (`GET /reviews/{id}/status`) | < 200ms | DB read only; called every 3 seconds |
| AI review pipeline (background) | Within Ollama read timeout (default 15m); typically faster with 3B coder | Submit → mentor email notification |
| Login and form submit actions | < 1 second | Standard synchronous operations |

### Security

- All user passwords stored using BCrypt strength ≥ 12 — never plaintext or reversible encryption
- All secrets (`GITHUB_TOKEN`, DB credentials, SMTP credentials, Ollama URL) injected via environment variables; none hardcoded in source code or committed to version control
- GitHub token transmitted only as `Authorization: Bearer` header — never as a query parameter, never logged
- Every protected endpoint enforces role authorization server-side; Thymeleaf conditional rendering alone is not sufficient
- HTTP sessions expire after a period of inactivity (configurable via Spring Security)
- No PR diff content, mentor remarks, or review feedback exposed to users outside their authorized role scope

### Integration

- **GitHub API:** Errors 404 (invalid PR), 403 (token insufficient), 429 (rate limited) each result in an `ERROR` review state with a user-visible message — no application crash
- **Ollama LLM:** HTTP read timeout configurable (`examinai.ai.ollama-read-timeout-ms`; default 15 minutes); if exceeded, review recorded as `ERROR`
- **LLM response parsing:** reasoning tags, markdown fences, and prose around JSON normalized (`LlmOutputSanitizer`) before parsing; if parsing still fails, review recorded as `ERROR` rather than silently dropped
- **SMTP email:** Delivery failures are logged but do not block the review pipeline — review state is updated in DB regardless of notification outcome

### Reliability

- Every task submission persisted as a `TaskReview` row with status `PENDING` before any external API call — no submission lost due to downstream failure
- Liquibase schema migration completes successfully before the application serves HTTP traffic
- PostgreSQL data persists across container restarts via a named Docker volume
- Ollama model data (`qwen2.5-coder:3b`, ~2 GB) persists across container restarts via a named Docker volume
- Async review thread pool configured with graceful shutdown: in-flight reviews complete before application exits (`awaitTerminationSeconds: 120`)
