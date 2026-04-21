# Story 3.3: Error State Handling, Submission History & Resubmit

Status: done

<!-- Ultimate context engine analysis completed - comprehensive developer guide created -->

## Story

As an intern,
I want to see a clear, specific error message when my submission fails and be able to resubmit, while keeping a full history of all past attempts,
so that I can correct mistakes without losing progress and always understand what went wrong.

## Acceptance Criteria

1. **GitHub 404 in async pipeline**  
   Given the async pipeline runs and `GitHubClient` receives a **404** response  
   When the failure is handled inside the async execution path (not on the HTTP submit thread)  
   Then `TaskReview.status` → `ERROR` and `errorMessage` → exactly `GitHub PR not found. Check your PR number and resubmit.`  
   And the existing `TaskReview` row is updated in place (no new row), and the exception is **not** re-thrown out of the async worker.

2. **GitHub 403**  
   Given a **403** from the GitHub API is handled in the same async path  
   Then `errorMessage` → exactly `GitHub token has insufficient permissions.`

3. **GitHub 429**  
   Given a **429** from the GitHub API is handled  
   Then `errorMessage` → exactly `GitHub API rate limited. Wait a few minutes and resubmit.`

4. **Ollama / LLM timeout (NFR13)**  
   Given the Ollama chat call exceeds the configured **120 second** maximum  
   When the timeout is caught in the async path  
   Then `TaskReview.status` → `ERROR`, `errorMessage` → exactly `AI review timed out. Try resubmitting.`, exception **not** re-thrown.

5. **LLM parse failure (after sanitize)**  
   Given `BeanOutputConverter` fails to parse the LLM response after think-strip and fence removal (`LLMReviewService.review`)  
   When the parse failure is caught  
   Then `TaskReview.status` → `ERROR`, `errorMessage` → exactly `AI review failed. Try resubmitting.`, exception **not** re-thrown.

6. **InternStatusCard — ERROR**  
   Given the polling endpoint returns `ERROR` for the current review  
   When `templates/fragments/intern-status-card.html` (`internStatusCard`) renders  
   Then the "Review Failed" badge matches Story 3.2 / **UX-DR1** (`aria-live="assertive"`), the persisted `errorMessage` appears in a Bootstrap **`alert alert-danger`** block (server-rendered; safe text — no raw HTML from DB), and a **resubmit** form with the same three fields as `task-detail.html` (repo owner, repo name, PR number) is visible **above the fold** on a typical laptop viewport (no scrolling required — use compact layout / card body ordering).

7. **InternStatusCard — REJECTED**  
   Given status `REJECTED`  
   When the card renders  
   Then a **red-bordered** card shows the Rejected badge, **mentor remarks** when `mentorRemarks` is non-null/non-empty, and the same resubmit form, visible without scrolling.

8. **InternStatusCard — APPROVED**  
   Given status `APPROVED`  
   When the card renders  
   Then a **green-bordered** card shows the Approved badge and mentor remarks (if any) — **no** resubmit form.

9. **Resubmit creates new row**  
   Given the intern resubmits from a review that is in `ERROR` or `REJECTED` (via task detail page **or** embedded form on the review status page)  
   When `POST /intern/tasks/{taskId}/submit` succeeds  
   Then a **new** `TaskReview` row is created with status `PENDING` and the prior attempt row is **unchanged**.

10. **Submission history on task detail**  
    Given the intern opens `GET /intern/tasks/{taskId}`  
    When the page loads  
    Then a **`list-group`** lists **all** `TaskReview` rows for that **task** and **current intern**, **newest first**, each row showing: **attempt number** (consistent numbering — e.g. newest = highest index), **status** via existing `statusBadge` fragment or equivalent styling, **`date_created`** formatted as **`dd MMM yyyy HH:mm`** (English locale), and **mentor remarks** when present.

11. **Empty history**  
    Given no `TaskReview` rows exist for that task/intern  
    When the intern views the task detail page  
    Then the history section shows **`No submissions yet.`** and the main submission form remains as today.

## Tasks / Subtasks

- [x] **Pipeline error classification** (AC: 1–5, NFR12–14)  
  - [x] In `ReviewPipelineService` async path: map GitHub `RestClient` failures by HTTP status (404 / 403 / 429) to the **exact** user strings in AC; persist via `ReviewPersistenceService` without generic `"Review pipeline failed: ..."` overwriting these messages.  
  - [x] Map LLM timeout and parse failures to the exact strings in AC 4–5.  
  - [x] Ensure classified paths **return without rethrowing** so the outer `catch` does not replace messages with a generic wrapper.  
  - [x] Enforce **120s** Ollama request timeout per **NFR13** / `epics.md` (Spring Boot **3.5.13**, Spring AI **1.0.0** — verify autoconfig; if no property, document the supported way to cap chat duration, e.g. HTTP client read timeout on the Ollama client used by Spring AI).

- [x] **Resubmit + duplicate guard** (AC: 9)  
  - [x] Confirm `submitPendingReview` only blocks when an attempt with status **`PENDING`** already exists for the same intern+task (`existsByTask_IdAndIntern_IdAndStatus(..., PENDING)` is already the right shape).  
  - [x] Do **not** add logic that prevents a new row after `ERROR` / `REJECTED`.

- [x] **InternStatusCard UX** (AC: 6–8, UX-DR4, UX-DR12 patterns)  
  - [x] Extend `intern-status-card.html`: left border / outline per UX spec (APPROVED green, REJECTED red, ERROR danger outline).  
  - [x] ERROR: `alert alert-danger` + resubmit form; REJECTED: remarks + resubmit; APPROVED: remarks only.  
  - [x] Resubmit form: `th:action` to `@{/intern/tasks/{id}/submit(id=${review.task.id})}`, CSRF hidden field, `th:object` for `ReviewSubmissionDto`, same field names/labels as `task-detail.html`.  
  - [x] For **REJECTED**, UX-DR4 mentions **AI issues** on the card — include `issues` only if they can be loaded without violating **NFR3** on the SSR path: use a **dedicated** `TaskReviewRepository` query with `JOIN FETCH` only for `issues` when rendering this page, or a lazy-safe batch fetch; do **not** enable `open-in-view`.

- [x] **Task detail history** (AC: 10–11)  
  - [x] Add repository method e.g. `findAllByTask_IdAndIntern_IdOrderByDateCreatedDesc`.  
  - [x] `InternTaskController` / `TaskService`: load history into model (`submissionHistory` or similar).  
  - [x] `task-detail.html`: `list-group` section below the form; fragment or `th:each` row; empty state copy exact: `No submissions yet.`

- [x] **Tests**  
  - [x] **Unit/integration** tests for pipeline classification: mock `GitHubClient` to throw status-specific exceptions; assert `markPipelineError` / persisted `errorMessage` strings **equal** AC literals.  
  - [x] **WebMvc** or controller test: task detail exposes history attribute and renders list-group when multiple reviews exist.  
  - [x] Optional: Thymeleaf smoke asserting resubmit form `action` contains correct `taskId`.

### Review Findings

- [x] [Review][Patch] Restore polling anchor `data-review-id` / `data-initial-status` on InternStatusCard — `static/js/review-polling.js` attaches to `[data-review-id][data-initial-status]`; the updated `intern-status-card.html` card root no longer exposes these attributes, so polling never starts for `PENDING` / `LLM_EVALUATED` (regression vs Story 3.2 AC and `epics.md` UX-DR4 polling anchor). **Fixed:** restored `th:attr` on card root in `intern-status-card.html`.

- [x] [Review][Patch] Align `TaskServiceTest.findSubmissionHistoryForInternTask_newestFirst` repository stub with a real intern id — the mock uses `isNull()` for the intern parameter because `UserAccount` has no `id` set; set `intern.setId(1L)` (or similar) and stub with `eq(1L)` so the test matches production queries. **Fixed:** `ReflectionTestUtils.setField(intern, "id", 1L)` and `eq(1L)` in stub.

## Dev Notes

### Brownfield snapshot (current code)

- **Pipeline:** `ReviewPipelineService.runPipeline` → `executePipeline` → `gitHubClient.getPrDiff` → `llmReviewService.review` → `reviewPersistenceService.saveLlmSuccess`. Outer `catch` logs and calls `markPipelineError(reviewId, "Review pipeline failed: " + msg)` — **too generic** for Story 3.3; inner handling must set precise messages first and **exit** without falling through.  
- **Persistence:** `ReviewPersistenceService.markPipelineError` truncates to **500** chars (`error_message` column) — AC strings fit.  
- **GitHub:** `GitHubClient` is a `@HttpExchange` interface; failures surface as Spring **`RestClient`** exceptions (e.g. `RestClientResponseException` with `getStatusCode()`). Map **404 / 403 / 429** explicitly.  
- **LLM:** `LLMReviewService.review` uses `BeanOutputConverter`; parse failures throw (e.g. `IllegalStateException` for null feedback) — map to AC 5 string.  
- **Submit:** `ReviewSubmissionController` PRG redirect to `/intern/reviews/{reviewId}`. Validation errors re-render `intern/task-detail` with `task` — ensure history is loaded on **both** success path prep and validation error path if you centralize loading in the controller.  
- **Task detail:** `InternTaskController` currently adds only `task` + empty `ReviewSubmissionDto`; **no** history. Template `intern/task-detail.html` has form only.  
- **Status card:** `intern-status-card.html` only shows badge + “Checking status…” for non-terminal states; **3.3** adds terminal layouts + resubmit.  
- **Polling:** `review-polling.js` + `ReviewStatusController` unchanged for JSON contract; terminal **ERROR** must still drive badge + any client-side label updates; **rich ERROR content** is **SSR** in the card (alert + form), not required from JSON for first paint (SSR shows immediately; poll keeps badge in sync).

### Architecture compliance

- **State transitions:** Only services mutate review state; controllers stay thin. [Source: `_bmad-output/planning-artifacts/architecture.md` — mutation ownership table]  
- **PRG:** All submission `POST` handlers return `redirect:`. [Source: `architecture.md`, `epics.md` Additional Requirements]  
- **`@PreAuthorize`:** Method-level on controllers. [Source: `architecture.md` — Authentication & Security]  
- **`open-in-view: false`:** Every new `JOIN FETCH` must be deliberate. [Source: `application.yml`, prior stories]  
- **NFR3:** Do not load heavy graphs for `GET /reviews/{id}/status` (unchanged). SSR pages may fetch more than the JSON endpoint.

### UX alignment

- **UX-DR4:** InternStatusCard borders and terminal content (ERROR alert + resubmit, REJECTED remarks + resubmit, APPROVED congratulations + remarks).  
- **UX-DR12:** Submission history `list-group` below form on task detail.  
- **Feedback patterns (UX spec):** Critical pipeline errors → `alert alert-danger` + assertive live region on badge.

### Previous story intelligence (3.2)

- **403 vs 404:** JSON status API uses **403** for non-owner intern; SSR review page may still use **404** for foreign reviews — do not “fix” that in 3.3 unless you explicitly reconcile (out of scope unless AC changes).  
- **`ReviewStatusController`:** Sanitizes `errorMessage` for JSON; **SSR alert** should show **persisted** message with normal Thymeleaf text escaping (`th:text`). If JSON sanitization and DB text differ slightly, that is OK — users read SSR first.  
- **Deferred items from 3.2** (null guards, stale poll race, etc.): fix only if you touch the same lines or a bug blocks AC.

### Git intelligence (recent commits)

- Recent pattern: story-sized vertical slices (`b07e2e1` Story 3.2; `7016daa` Epic 2 + Story 3.1) — pipeline → persistence → Thymeleaf → tests.

### Latest technical specifics

- **Spring Boot** `3.5.13`, **Java 21**, **Spring AI BOM** `1.0.0` (`pom.xml`).  
- **GitHub API:** Bearer token only; never log token.  
- **NFR13:** Cap LLM wait at **120 seconds** — confirm how Spring AI Ollama starter exposes timeouts; use project-consistent configuration (YAML or bean override).

### Project context reference

- No `project-context.md` in repo — this file + `architecture.md` + `epics.md` + `ux-design-specification.md` are authoritative.

### References

- Story AC source: `_bmad-output/planning-artifacts/epics.md` — Epic 3, Story 3.3  
- FR16–FR19, NFR12–NFR14: `_bmad-output/planning-artifacts/epics.md` — Requirements Inventory  
- UX-DR4, UX-DR12, feedback patterns: `_bmad-output/planning-artifacts/ux-design-specification.md`  
- API / data flow: `_bmad-output/planning-artifacts/architecture.md`  
- Prior implementation: `3-2-review-status-polling-live-status-updates.md`, `3-1-task-submission-form-async-ai-review-pipeline.md`

## Dev Agent Record

### Agent Model Used

Composer (Cursor)

### Debug Log References

### Completion Notes List

- Implemented `ReviewPipelineService` classification for GitHub 404/403/429 (`RestClientResponseException`), LLM timeout (`SocketTimeoutException` / `TimeoutException` in cause chain, `ResourceAccessException` with "timed out"), and LLM parse failures (`JsonProcessingException` in cause chain, `IllegalStateException` for null parsed feedback). Classified paths call `markPipelineError` and return without rethrowing; unclassified errors still use the generic outer catch.
- Documented NFR13 in `application.yml`; 120s Ollama read timeout remains in `AIConfig`’s `OllamaApi` `RestClient` factory.
- Extended `findByIdForInternStatusPage` with `LEFT JOIN FETCH tr.issues` for REJECTED SSR; added `findAllByTask_IdAndIntern_IdOrderByDateCreatedDesc` and `TaskService.findSubmissionHistoryForInternTask`; wired `submissionHistory` on task detail and submit validation error path.
- Rebuilt `intern-status-card.html` with border styling, ERROR alert, REJECTED remarks + AI issues list, APPROVED remarks, compact resubmit form + `ReviewSubmissionDto`; `InternReviewStatusController` supplies `submission`; `review-status.html` passes fragment args.
- Added `InternTaskControllerTest`, expanded `ReviewPipelineServiceTest`, `TaskServiceTest`, `ReviewSubmissionControllerTest`.

### File List

- `src/main/java/com/examinai/review/ReviewPipelineService.java`
- `src/main/java/com/examinai/review/TaskReviewRepository.java`
- `src/main/java/com/examinai/task/TaskService.java`
- `src/main/java/com/examinai/task/InternTaskController.java`
- `src/main/java/com/examinai/review/ReviewSubmissionController.java`
- `src/main/java/com/examinai/review/InternReviewStatusController.java`
- `src/main/resources/application.yml`
- `src/main/resources/templates/fragments/intern-status-card.html`
- `src/main/resources/templates/intern/review-status.html`
- `src/main/resources/templates/intern/task-detail.html`
- `src/test/java/com/examinai/review/ReviewPipelineServiceTest.java`
- `src/test/java/com/examinai/task/TaskServiceTest.java`
- `src/test/java/com/examinai/review/ReviewSubmissionControllerTest.java`
- `src/test/java/com/examinai/task/InternTaskControllerTest.java`

## Story Completion Status

- **done** — Implementation and code-review patches applied; `mvn test` passed.

## Change Log

- 2026-04-22: Story 3.3 — pipeline error messages, status card terminal UX + resubmit, task submission history, tests; sprint status → review.
- 2026-04-22: Code review — restored InternStatusCard polling `data-*` attributes; fixed `TaskServiceTest` intern id stub; sprint status → done.
