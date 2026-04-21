# Story 3.1: Task Submission Form & Async AI Review Pipeline

Status: done

<!-- Ultimate context engine analysis completed - comprehensive developer guide created -->

## Story

As an intern,
I want to submit a GitHub PR for review and have the system automatically evaluate it using AI,
so that I receive structured code feedback without waiting for a mentor to read the diff manually.

## Acceptance Criteria

1. **Task detail & submission form (SSR)**  
   Given an intern is on `/intern/tasks/{taskId}`  
   When the page loads  
   Then a submission form is shown with three required fields — Repository Owner, Repository Name, Pull Request Number — each with an explicit `<label for="...">` and a submit button (`btn-primary`)  
   And the fields use responsive layout: `col-md-4` on desktop and stack (single column) at tablet (~768px).

2. **Write-first POST handling**  
   Given the intern fills out the form and submits  
   When the POST reaches `ReviewSubmissionController` at `POST /intern/tasks/{taskId}/submit`  
   Then this sequence runs in order on the HTTP thread:  
   (1) Build `TaskReview` with `status = PENDING`, `intern` = current user, `task` = resolved task, `mentor` = `task.mentor`, `dateCreated = LocalDateTime.now()`  
   (2) `repository.save(taskReview)` flushes the row to the database  
   (3) An application event is published to kick off async work (so work runs **after** commit — see Dev Notes)  
   (4) The handler returns **PRG**: `redirect:/intern/reviews/{reviewId}` — never a view name  
   And the handler completes in **under 500ms** (DB write + event publish only — no GitHub/Ollama on the HTTP thread).

3. **HTTP status note (epic vs architecture)**  
   Epics mention `202 Accepted` + `reviewId` while architecture mandates PRG for form POSTs. **Canonical behavior for this codebase:** Thymeleaf form POST → **redirect** (303/302) to `/intern/reviews/{reviewId}` with `reviewId` in the path. That satisfies FR15 (immediate handoff + identifier) and architecture PRG. Do **not** return an HTML view from the POST handler.

4. **Async executor**  
   Given `AsyncConfig` defines the task executor  
   When the application starts  
   Then the executor uses **corePoolSize = 15**, **maxPoolSize = 30**, **queueCapacity = 150**, **awaitTerminationSeconds = 120**  
   And `@EnableAsync` is on `AsyncConfig`.

5. **GitHub diff fetch**  
   Given the async pipeline runs (e.g. `ReviewPipelineService.runPipeline(reviewId)` or equivalent)  
   When `GitHubClient.getPrDiff(repoOwner, repoName, prNumber)` runs  
   Then it calls the GitHub REST API with `Authorization: Bearer {GITHUB_TOKEN}`  
   And the token is **never** logged and **never** sent as a query parameter.

6. **LLM processing order**  
   Given the GitHub diff is fetched successfully  
   When `LLMReviewService.review(taskDescription, prDiff)` runs  
   Then processing order is **exactly**:  
   (1) Strip `<think>...</think>` via `(?s)<think>.*?</think>`  
   (2) Strip markdown fences (e.g. `` ```json `` / `` ``` ``)  
   (3) Parse with **`BeanOutputConverter`** into a `ReviewFeedback` record (or equivalent type).

7. **Persist LLM outcome**  
   Given the LLM returns valid structured feedback  
   When `ReviewPersistenceService.saveLLMResult()` (or equivalent) runs  
   Then `TaskReview.status` becomes `LLM_EVALUATED`, structured issues are persisted as **`TaskReviewIssue`** rows (line, code, issue, improvement), and **`AiReviewCompleteEvent(reviewId, mentorId, internName, courseName, taskName)`** is published **after** the DB updates commit (same event pattern as architecture: publisher in persistence layer; listeners `@TransactionalEventListener(AFTER_COMMIT)` + `@Async` — email body can be no-op / log-only until notification epic if not yet implemented).

8. **Database**  
   Given `task_review_issue` is already defined in `001-init-schema.sql`  
   When the application runs  
   Then **no new Liquibase changelog** is required **unless** you must widen columns (e.g. `task_review.llm_result` is `VARCHAR(20)` in SQL — verify it can hold the stored verdict / summary you plan to keep on `TaskReview`; if not, add a focused migration).

9. **Transactional discipline**  
   Given `@Transactional` usage is reviewed  
   Then `@Transactional` appears **only** on `@Service` methods — **not** on `@Controller` or `@Repository` classes.

10. **Post-submit landing page**  
    Given a successful submit  
    When the browser follows the redirect  
    Then `GET /intern/reviews/{reviewId}` returns a **minimal** intern review page (no 404) — static copy such as “Submitted. AI review in progress.” is acceptable. **Do not** implement `ReviewStatusBadge`, `InternStatusCard`, or `review-polling.js` in this story (Epic 3 Story 3.2).

## Tasks / Subtasks

- [x] **Config & secrets** (AC: 4, 5)  
  - [x] Add `AsyncConfig` under `com.examinai.config` per architecture numbers.  
  - [x] Add `AIConfig` (or equivalent) for `ChatClient` / Ollama with **120s** timeout per PRD.  
  - [x] Wire `GITHUB_TOKEN` in `application.yml` as `${GITHUB_TOKEN:}` (and document in `.env.example` — already listed in deferred work).  
  - [x] Ensure Ollama `base-url` remains overridable (existing `spring.ai.ollama.base-url`).

- [x] **Domain: issues** (AC: 7, 8)  
  - [x] Add `TaskReviewIssue` entity + `TaskReviewIssueRepository` mapping to existing `task_review_issue` table.  
  - [x] Add `ReviewFeedback` record (and nested issue type) compatible with `BeanOutputConverter` + your prompt JSON shape.

- [x] **GitHub client** (AC: 5)  
  - [x] Implement `GitHubClient` (`@HttpExchange`) + `GitHubClientConfig` (`RestClient`, `HttpServiceProxyFactory`) per architecture.  
  - [x] Map GitHub errors to unchecked exceptions or a small result type the pipeline can translate (Story 3.3 owns user-facing `ERROR` messages for 404/403/429; this story should not swallow errors silently).

- [x] **LLM** (AC: 6)  
  - [x] Add `LLMReviewService` using Spring AI + `BeanOutputConverter`.  
  - [x] Add prompt template under `src/main/resources/prompts/` (e.g. `*.st`) including task description + diff.

- [x] **Persistence & events** (AC: 7)  
  - [x] Add `ReviewPersistenceService`: save issues, set `LLM_EVALUATED`, publish `AiReviewCompleteEvent` after successful commit.  
  - [x] Register an async **AFTER_COMMIT** listener (notification service or stub) that **never** rethrows.

- [x] **Pipeline orchestration** (AC: 2, 5–7, 9)  
  - [x] Add `ReviewPipelineService`: transactional **submit** path (PENDING save + publish “start pipeline” event), and **`@Async`** pipeline method that loads review, calls GitHub → LLM → persistence.  
  - [x] Catch **all** exceptions in the async path: set `ERROR`, set `errorMessage`, save, log — **do not rethrow** (exact user-facing strings for GitHub/timeout/parse are refined in Story 3.3; use clear interim messages or shared constants).  
  - [x] Enforce: **no** GitHub/Ollama calls before PENDING is persisted + flushed.

- [x] **Controllers & templates** (AC: 1–3, 10)  
  - [x] `ReviewSubmissionController`: `POST /intern/tasks/{taskId}/submit` with `@PreAuthorize("hasRole('INTERN') or hasRole('ADMIN')")` on the method.  
  - [x] `InternTaskController`: `GET /intern/tasks/{taskId}` with same PreAuthorize, loads task with **`JOIN FETCH`** for `course` (and mentor if needed) to respect `open-in-view: false`.  
  - [x] Add `templates/intern/task-detail.html` (form + CSRF).  
  - [x] Add `GET /intern/reviews/{reviewId}` + `templates/intern/review-status.html` (minimal).  
  - [x] Link each task card from `task-list` to `/intern/tasks/{id}` (update `task-status-card` or list template).

- [x] **Repository additions** (AC: 2, 7)  
  - [x] Extend `TaskReviewRepository` with needed queries (`findById`, ownership helpers, etc.).  
  - [x] Any new query used from Thymeleaf must eagerly fetch what the template touches.

- [x] **Security** (AC: 10)  
  - [x] Review page must verify the `TaskReview.intern` matches the authenticated user (or ADMIN) — **server-side**, not UI-only.

- [x] **Tests**  
  - [x] `ReviewSubmissionControllerTest`: POST persists PENDING and redirects (MockMvc + mocked pipeline/service).  
  - [x] `LLMReviewServiceTest`: think-strip + fence-strip + parse.  
  - [x] `ReviewPipelineServiceTest`: order of calls mocked (save PENDING before GitHub client).  
  - [x] Update `SecurityIntegrationTest` with `@MockBean` for any new beans that break context startup.

## Dev Notes

### Implementation snapshot (brownfield)

- **Spring Boot / Java:** `pom.xml` is authoritative — parent **3.5.13**, Java **21**, Spring AI BOM **1.0.0**, artifact `spring-ai-starter-model-ollama` (already present).  
- **Existing review model:** `TaskReview`, `ReviewStatus`, `TaskReviewRepository` in `com.examinai.review` — extend, do not duplicate.  
- **Task list:** `InternTaskController` + `TaskService.findForInternByUsername` + `TaskWithReview` + `fragments/task-status-card.html` — add navigation into task detail.  
- **`TaskService`:** admin/mentor-only `findById` exists; intern-safe task load for detail likely needs a **new** method (e.g. `findForInternTaskDetail(username, taskId)`) with `@PreAuthorize` on **that** method — avoid widening mentor-only APIs.

### Architecture compliance

- **PRG** on all POST controllers returning redirects; never return a Thymeleaf view name from submit. [Source: `_bmad-output/planning-artifacts/architecture.md` — Process Patterns]  
- **`@PreAuthorize` on each controller method** (class-level alone is insufficient). [Source: architecture.md]  
- **Write-first pipeline** and **async error handling** (catch-all, persist ERROR, no rethrow). [Source: architecture.md]  
- **Packages:** pipeline pieces live under `com.examinai.review` per architecture directory map; config under `com.examinai.config`.  
- **REST / clients:** GitHub via `RestClient` + `@HttpExchange`. [Source: architecture.md — API Patterns]  
- **FR coverage for this slice:** FR14–FR16 partially (submission + identifier + non-blocking); FR17–FR19 largely deferred to Story 3.3; FR20–FR23 core AI path in this story. [Source: `_bmad-output/planning-artifacts/prd.md`]

### UX alignment (this story)

- Three-field submission, immediate confirmation, calm copy on landing page (“Submitted…”) aligns with UX principles for intern clarity. [Source: `_bmad-output/planning-artifacts/ux-design-specification.md` — Core Experience, Experience Principles]  
- **Do not** add live polling UX yet — that is Story 3.2 (`review-polling.js`, `ReviewStatusBadge`, `InternStatusCard`).

### Previous story intelligence (Epic 2 Story 2.3)

- **`open-in-view: false`:** any controller→template path that touches `task.course` (or lazy associations) needs **`JOIN FETCH`** in the query. [Source: `_bmad-output/implementation-artifacts/2-3-intern-task-list-progress-view.md`]  
- **`TaskReview.dateCreated`:** must be set in Java on create (`LocalDateTime.now()`) — DB default exists but JPA insert expects the field populated per prior story notes.  
- **`findForInternByUsername`** is the canonical intern task list API name (epics sometimes say `findForIntern()` — use the implemented name).  
- **Deferred items** affecting this epic: wire `GITHUB_TOKEN`, consider DB constraints on `task_review.status`, `llm_result` width — see `deferred-work.md`.

### Git intelligence (recent commits)

- Recent work pattern: epic 2 stories + BMAD story context commits (`d385ffc` context for 2.3, `db6e229` / `2bba320` implementations). Expect similar layering: domain → service → controller → Thymeleaf → tests.

### Latest technical specifics

- **Spring AI 1.0.0** (BOM) + **Ollama** starter already in project; use `BeanOutputConverter` for structured output.  
- **deepseek-r1** may emit `<think>` blocks — mandatory strip before JSON parse. [Source: architecture.md + PRD NFR]  
- **GitHub API:** Personal access token via `Authorization: Bearer` header only.

### Project context reference

- No `project-context.md` found in repo root — rely on this file + `architecture.md` + `epics.md`.

### Deferred / edge notes

- **`llm_result` / `mentor_result` columns** in PostgreSQL are `VARCHAR(20)` while JPA entities use unconstrained `String` — confirm what you store at `LLM_EVALUATED` (short enum-like verdict vs JSON). Add Liquibase alter if you need longer text. [Source: `001-init-schema.sql`, `deferred-work.md`]  
- **Error copy** for specific GitHub codes and Ollama timeout is specified in Story 3.3 — implement pipeline hooks this story so those branches are easy to fill in.

## Dev Agent Record

### Agent Model Used

Composer (dev-story workflow)

### Debug Log References

### Completion Notes List

- Implemented PRG submit flow with `saveAndFlush` + `ReviewPipelineStartedEvent` handled `@TransactionalEventListener(AFTER_COMMIT)` before `@Async` pipeline work.
- Added Liquibase `005` for GitHub PR fields on `task_review`; `llm_result` kept to a 20-char verdict to match existing column width.
- Full application context test uses Testcontainers PostgreSQL with `@ServiceConnection` and `disabledWithoutDocker = true`; `src/test/resources/application.properties` supplies mail/DB defaults when env vars are unset.
- `UserAccountServiceTest` now sets an admin `SecurityContext` so `deactivate` matches production behavior.

### File List

- pom.xml
- src/main/resources/application.yml
- src/main/resources/db/changelog/db.changelog-master.xml
- src/main/resources/db/changelog/changelogs/005-task-review-github-fields.sql
- src/main/resources/prompts/review-diff.st
- src/main/resources/templates/fragments/task-status-card.html
- src/main/resources/templates/intern/task-detail.html
- src/main/resources/templates/intern/review-status.html
- src/main/java/com/examinai/config/AsyncConfig.java
- src/main/java/com/examinai/config/AIConfig.java
- src/main/java/com/examinai/review/AiReviewCompleteEvent.java
- src/main/java/com/examinai/review/AiReviewNotificationListener.java
- src/main/java/com/examinai/review/GitHubClient.java
- src/main/java/com/examinai/review/GitHubClientConfig.java
- src/main/java/com/examinai/review/InternReviewStatusController.java
- src/main/java/com/examinai/review/LLMReviewService.java
- src/main/java/com/examinai/review/LlmOutputSanitizer.java
- src/main/java/com/examinai/review/ReviewFeedback.java
- src/main/java/com/examinai/review/ReviewPersistenceService.java
- src/main/java/com/examinai/review/ReviewPipelineEventListener.java
- src/main/java/com/examinai/review/ReviewPipelineService.java
- src/main/java/com/examinai/review/ReviewPipelineStartedEvent.java
- src/main/java/com/examinai/review/ReviewSubmissionController.java
- src/main/java/com/examinai/review/ReviewSubmissionDto.java
- src/main/java/com/examinai/review/TaskReview.java
- src/main/java/com/examinai/review/TaskReviewIssue.java
- src/main/java/com/examinai/review/TaskReviewIssueRepository.java
- src/main/java/com/examinai/review/TaskReviewRepository.java
- src/main/java/com/examinai/task/InternTaskController.java
- src/main/java/com/examinai/task/TaskRepository.java
- src/main/java/com/examinai/task/TaskService.java
- src/test/resources/application.properties
- src/test/java/com/examinai/ExaminAiApplicationTests.java
- src/test/java/com/examinai/review/LLMReviewServiceTest.java
- src/test/java/com/examinai/review/ReviewPipelineServiceTest.java
- src/test/java/com/examinai/review/ReviewSubmissionControllerTest.java
- src/test/java/com/examinai/task/TaskServiceTest.java
- src/test/java/com/examinai/user/SecurityIntegrationTest.java
- src/test/java/com/examinai/user/UserAccountServiceTest.java
- _bmad-output/implementation-artifacts/3-1-task-submission-form-async-ai-review-pipeline.md
- _bmad-output/implementation-artifacts/sprint-status.yaml

### Review Findings

- [x] [Review][Decision] Duplicate submission guard — resolved: application-level guard added to `submitPendingReview` via `existsByTask_IdAndIntern_IdAndStatus` check before saving PENDING review [TaskReviewRepository.java, ReviewPipelineService.java]

- [x] [Review][Patch] `ReviewPipelineEventListener` missing `@Async` — added `@Async` to listener; removed `@Async` from `runPipeline` (listener now owns the async dispatch boundary) [ReviewPipelineEventListener.java]

- [x] [Review][Patch] `markPipelineError` silently no-ops when `reviewId` not found — changed to `ifPresentOrElse` with a `log.warn` when row is missing [ReviewPersistenceService.java]

- [x] [Review][Patch] `InternReviewStatusController` leaks review existence via 404/403 distinction — unauthorized access now returns 404 (same as not-found) [InternReviewStatusController.java]

- [x] [Review][Patch] No timeout on `GitHubClient` `RestClient` — added `SimpleClientHttpRequestFactory` with 10s connect / 30s read timeout [GitHubClientConfig.java]

- [x] [Review][Patch] Public `setId()` on `TaskReview` entity — narrowed to package-private; test class in same package retains access [TaskReview.java]

- [x] [Review][Patch] `LLMReviewService.review()` can return null — added null check after `converter.convert`; throws `IllegalStateException` with the cleaned output for diagnosis [LLMReviewService.java]

- [x] [Review][Patch] `normalizeVerdict` silently truncates verdict without logging — added `log.warn` when truncation occurs [ReviewPersistenceService.java]

- [x] [Review][Defer] GitHub token potentially logged via Spring DEBUG HTTP logging — `RestClient` logs Authorization headers at DEBUG level when Spring web debug logging is enabled; deployment/configuration concern not fixable in-code without a masking interceptor [GitHubClientConfig.java:19] — deferred, pre-existing
- [x] [Review][Defer] LLM JSON embedded in prose not handled — `LlmOutputSanitizer` handles fenced JSON and `<think>` blocks but cannot extract JSON buried in plain prose; edge case as model/prompt evolves [LlmOutputSanitizer.java:13] — deferred, pre-existing
- [x] [Review][Defer] Thread pool rejection silently loses pipeline submission — when `AsyncConfig` executor is saturated, `TaskRejectedException` from `@Async` dispatch propagates through `afterCommit()` and leaves `TaskReview` in PENDING permanently; needs rejection policy and/or monitoring hook [AsyncConfig.java:20] — deferred, pre-existing

## Change Log

- 2026-04-21: Story 3.1 implementation — async review pipeline, GitHub diff + Ollama structured review, intern submit/detail/review-status UI, tests and sprint status → review.
- 2026-04-22: Code review complete — 1 decision-needed, 7 patches, 3 deferred, 14 dismissed.
