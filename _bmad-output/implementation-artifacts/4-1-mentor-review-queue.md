# Story 4.1: Mentor Review Queue

Status: done

<!-- Ultimate context engine analysis completed - comprehensive developer guide created -->

## Story

As a mentor or admin,
I want to see a filtered list of reviews ready for my decision with key information visible per row,
so that I can triage and prioritize my review queue without manual searching.

## Acceptance Criteria

1. **Queue table (default filter)**  
   **Given** a mentor navigates to `/mentor/reviews`  
   **When** the page loads  
   **Then** a `table table-hover` displays reviews with columns: **Intern Name**, **Task Name**, **AI Verdict** badge, **Status** badge  
   **And** the default filter is **`status=LLM_EVALUATED`** — only reviews ready for decision are shown  
   **And** all column headers use `<th scope="col">` for accessibility (UX-DR14).

2. **GET filters (`onchange`)**  
   **Given** filter controls exist above the table (Status, Intern Name, Task as `form-select form-select-sm`)  
   **When** a filter value changes (`onchange`)  
   **Then** the page reloads via **GET** with the updated query params and the table reflects the filtered results.

3. **Mentor scope (server-side)**  
   **Given** a mentor is authenticated  
   **When** the `/mentor/reviews` query runs  
   **Then** only reviews where **`task.mentor_id` matches the current mentor’s `UserAccount.id`** are returned — mentors see only their own assigned reviews.

4. **Admin scope**  
   **Given** an admin is authenticated  
   **When** they access `/mentor/reviews`  
   **Then** reviews across **all mentors and all interns** are shown — **no** mentor-scope restriction applies.

5. **Empty state**  
   **Given** the queue has no reviews matching the current filter  
   **When** the page renders  
   **Then** an empty state **`card`** is shown with this **exact** copy: `No reviews awaiting your decision. Check back after interns submit.` — **no** empty table is displayed.

6. **Row navigation**  
   **Given** a review row exists in the table  
   **When** the mentor clicks the row  
   **Then** they navigate to **`/mentor/reviews/{reviewId}`** (GET).

7. **Authorization**  
   **Given** `@PreAuthorize("hasRole('MENTOR') or hasRole('ADMIN')")` is on the queue handler (e.g. `MentorReviewController` method for the queue)  
   **When** an intern attempts to access `/mentor/reviews`  
   **Then** a **403** is returned.

**Cross-story note:** Story **4.2** implements the full review **detail** UI. This story still requires that **`GET /mentor/reviews/{reviewId}`** does not 404 when the user follows a queue row — implement a **minimal placeholder** detail page and controller mapping if 4.2 is not implemented in the same change set, or implement the thin route + redirect forward as your team prefers. Do **not** leave a dead link.

## Tasks / Subtasks

- [x] **Repository + query layer** (AC: 1, 3, 4)  
  - [x] Add read-only repository/service method(s) to list `TaskReview` rows with **`JOIN FETCH`** / batch strategy appropriate for the table (intern name, task name, `llm_result`, `status`) — avoid N+1; respect **open-in-view** policy (prefer explicit fetch in query).  
  - [x] Apply **default** filter `status = LLM_EVALUATED` when the `status` query param is absent.  
  - [x] Apply **optional** filters from GET params for status, intern, and task (match epics: filter by selected intern and task when provided).  
  - [x] **Mentor:** restrict with `WHERE task.mentor.id = :currentMentorId` (resolve mentor id from `Authentication` username → `UserAccount`).  
  - [x] **Admin:** omit mentor restriction.

- [x] **`MentorReviewController` + DTO/model** (AC: 1–2, 6–7)  
  - [x] Extend `MentorReviewController` (`GET /mentor/reviews`) to accept query params and pass resolved filter + review list + dropdown option lists into the model.  
  - [x] Keep `@PreAuthorize` on the queue method per architecture.  
  - [x] Ensure filter `<select>` elements use **`onchange`** to submit/navigate to the same path with updated query string (no POST for filters).

- [x] **Thymeleaf `review-queue.html`** (AC: 1, 2, 5)  
  - [x] Replace placeholder [`src/main/resources/templates/mentor/review-queue.html`](../../src/main/resources/templates/mentor/review-queue.html) with `table table-hover`, `table-responsive` wrapper (UX-DR13), and filter row.  
  - [x] **AI Verdict:** display from `TaskReview.llmResult` (values align with LLM output, e.g. `APPROVE` / `REJECT`) as Bootstrap badges — consistent with product language (“AI Suggestion” labeling can stay minimal in the queue; full styling in 4.2).  
  - [x] **Status:** reuse [`fragments/review-status-badge.html`](../../src/main/resources/templates/fragments/review-status-badge.html) fragment `statusBadge(status)` where applicable.  
  - [x] Empty state: **card** only; hide table when the list is empty.  
  - [x] **Accessibility:** `<main id="main-content">`, semantic structure, labels on filters (`<label for=...>`), heading hierarchy per UX-DR14.  
  - [x] **Row click:** `cursor: pointer` on rows; navigate via GET to `/mentor/reviews/{id}` (inline `th:onclick` with URL, or `<a>` stretched across row — avoid breaking accessibility; keyboard users need focusable target — prefer explicit link or `tabindex` + key handler if using click on `<tr>`).

- [x] **Detail route stub (dependency of AC 6)**  
  - [x] Add `GET /mentor/reviews/{reviewId}` returning a minimal template or forward until Story 4.2 if required so queue links are not 404.

- [x] **Tests**  
  - [x] **WebMvc** / slice tests: mentor sees only scoped rows; admin sees all; intern gets 403 on `/mentor/reviews`.  
  - [x] **Repository** or **service** test: default `LLM_EVALUATED` filter; optional param filters.  
  - [x] Optional: Thymeleaf assertion that empty state copy matches exactly.

### Review Findings

- [x] [Review][Decision] Invalid query params — **Resolved (D1:2):** non-blank invalid `status` / `internId` / `taskId` now yield **400** via `MentorReviewService` (`ResponseStatusException`).

- [x] [Review][Decision] Intern/Task filter dropdown scope — **Resolved (D2:3):** intern options use mentor scope + **status** + **task** filter; task options use mentor scope + **status** + **intern** filter (cross-filtering so each list respects the other two dimensions).

- [x] [Review][Patch] Row navigation — real `<a>` links per cell (first cell focusable; others `tabindex="-1"` / `aria-hidden`) in [`review-queue.html`](../../src/main/resources/templates/mentor/review-queue.html).

- [x] [Review][Patch] Long `llmResult` — `#strings.abbreviate(…, 24)`, `text-truncate`, `max-width`, and `title` tooltip on verdict badges in [`review-queue.html`](../../src/main/resources/templates/mentor/review-queue.html).

- [x] [Review][Patch] Detail stub HTTP contract — `reviewDetail_notFound_returns404` / `reviewDetail_forbidden_returns403` in [`MentorReviewControllerTest.java`](../../src/test/java/com/examinai/review/MentorReviewControllerTest.java).

- [x] [Review][Defer] `SecurityIntegrationTest` still only stubs an empty mentor queue — no integration assertion that real data scoping matches AC 3/4; consistent with existing shallow slice. [`SecurityIntegrationTest.java`] — deferred, pre-existing

- [x] [Review][Defer] No `@DataJpaTest` for `TaskReviewRepository.findMentorQueue` JPQL — story listed repository tests as optional. [`TaskReviewRepository.java`] — deferred, pre-existing gap

## Dev Notes

### Brownfield snapshot (do not reinvent)

- **Controller stub exists:** [`MentorReviewController`](../../src/main/java/com/examinai/review/MentorReviewController.java) currently returns static view `mentor/review-queue` with **no** model — replace with real data.  
- **Template stub:** [`review-queue.html`](../../src/main/resources/templates/mentor/review-queue.html) — placeholder copy only.  
- **Ownership pattern:** [`TaskService.assertOwnership`](../../src/main/java/com/examinai/task/TaskService.java) — admin bypasses mentor restriction; mentor matches `task.getMentor().getUsername()`. For **queue**, filter by **`task.mentor`** (same ownership concept; epics specify `task.mentor_id`).  
- **Entities:** [`TaskReview`](../../src/main/java/com/examinai/review/TaskReview.java) has `intern`, `task`, `mentor`, `status`, `llmResult`; [`Task`](../../src/main/java/com/examinai/task/Task.java) has **`mentor`** FK — **scope on `task.mentor`, not `TaskReview.mentor`**, unless product confirms both always align (prefer epics: `task.mentor_id`).  
- **Repository:** [`TaskReviewRepository`](../../src/main/java/com/examinai/review/TaskReviewRepository.java) — add dedicated query method(s) for the mentor queue; existing methods are intern-centric.  
- **Status enum:** [`ReviewStatus`](../../src/main/java/com/examinai/review/ReviewStatus.java) — `getDisplayLabel()` for badges via fragment.

### Architecture compliance

- **Server-side MPA, Thymeleaf, PRG for POST** — this story is **GET-only** for the queue; no change to PRG rules. [Source: `_bmad-output/planning-artifacts/architecture.md` — API & Communication Patterns]  
- **`@PreAuthorize` / `@EnableMethodSecurity`** — queue endpoint must remain authoritative; Thymeleaf `sec:*` is display-only. [Source: `architecture.md` — Authentication & Security]  
- **Package-by-feature:** keep mentor review code under `com.examinai.review`. [Source: `architecture.md` — Code Organization]  
- **No PR diff or cross-role data leaks** — queue only shows rows the user is allowed to see per AC 3–4 (NFR11). [Source: `epics.md` / `architecture.md`]

### Technical requirements (guardrails)

| Topic | Requirement |
|--------|-------------|
| Default query | Without `?status=`, assume **LLM_EVALUATED** |
| Mentor resolution | Load `UserAccount` by `Authentication#getName()`; use **id** for join |
| Admin check | `auth.getAuthorities()` contains `ROLE_ADMIN` → skip mentor filter |
| AI verdict UI | Map `llmResult` string to visible badge text (e.g. APPROVE / REJECT) |
| Filters | Persist selected values in `<select>` via query params on reload |

### Library / stack

- **Spring Boot 3.5.13**, **Java 21**, **Spring Data JPA** — use repository `@Query` or **Specification** if team already uses specifications (project currently uses explicit `@Query`; stay consistent).  
- **Thymeleaf** + **Thymeleaf Layout Dialect** — match existing layouts [`layout/base`](../../src/main/resources/templates/layout/base.html).  
- **Bootstrap** (WebJar) — `table-hover`, `form-select form-select-sm`, `card` for empty state.

### File / module touch map

| Area | Files likely touched |
|------|----------------------|
| Controller | `MentorReviewController.java` — queue + optional detail stub |
| Service | New or extended `*Review*Service` for queue query + filter resolution (keep controllers thin) |
| Repository | `TaskReviewRepository.java` — queue finder(s) |
| Templates | `templates/mentor/review-queue.html`; optional `mentor/review-detail-placeholder.html` |
| Tests | `src/test/java/.../review/` mirror existing patterns (`@WebMvcTest`, `@DataJpaTest`) |

### Testing requirements

- Prefer **`@WebMvcTest(MentorReviewController.class)`** with **`@MockBean`** collaborators for auth matrix (mentor vs admin vs intern).  
- Use **`@WithMockUser(roles = ...)`** or security test utilities already in the project.  
- Repository tests with **`@DataJpaTest`** + Testcontainers or existing DB test style in repo.

### Previous story intelligence (Epic 3 → Epic 4)

- **Story 3.3** [`3-3-error-state-handling-submission-history-resubmit.md`](./3-3-error-state-handling-submission-history-resubmit.md): polling anchor `data-review-id` / `data-initial-status` on `InternStatusCard` — **not** required for mentor queue, but do not regress intern templates.  
- **State machine:** Reviews reach **`LLM_EVALUATED`** via `ReviewPersistenceService.saveLLMResult()` / pipeline — queue default aligns with mentor “ready to decide.”  
- **Thin controllers / fat services** for mutations; this story is **read-heavy** but should still use a **service** for query + scoping logic.

### Git intelligence (recent commits)

- Recent work finished Epic 3 (`Finished implementation of epic 3`, story 3.2, etc.) — patterns: `TaskReviewRepository` expansions, intern-facing controllers, Thymeleaf fragments for status.

### Latest tech / versions (pin at implementation time)

- **Spring Boot 3.5.13** (parent POM in [`pom.xml`](../../pom.xml)) — use current Spring Security 6.x method security semantics (`@PreAuthorize`).  
- **Spring AI 1.0.0 BOM** — irrelevant to queue listing; no new AI dependency for this story.

### Project context reference

- No `project-context.md` in repo — rely on this file + [`_bmad-output/planning-artifacts/architecture.md`](../planning-artifacts/architecture.md) + [`epics.md`](../planning-artifacts/epics.md).

### UX references (epics inventory)

- **UX-DR10:** Mentor review queue — Bootstrap `table-hover`, columns, default filter `LLM_EVALUATED`, `form-select` filters, `cursor: pointer` rows, empty state card.  
- **UX-DR13:** `table-responsive` for mentor queue at narrow widths.  
- **UX-DR14:** Table headers `scope="col"`, skip link / semantic layout in base template.

## Dev Agent Record

### Agent Model Used

Composer (Cursor agent)

### Debug Log References

### Completion Notes List

- Implemented mentor queue end-to-end: `TaskReviewRepository.findMentorQueue` + option queries, `MentorReviewService` for default `LLM_EVALUATED`, mentor vs admin scope (`task.mentor`), `MentorReviewQueueView` / `MentorQueueLabelValue`, controller + placeholder detail with access check on detail.
- Fixed `SecurityIntegrationTest` by adding `@MockBean MentorReviewService` (controller dependency).
- Added `MentorReviewControllerTest` (403 intern, queue model, query params, empty-state copy, detail view) and `MentorReviewServiceTest` (mentor/admin filters, param parsing, detail 403/admin bypass).
- Table column headings aligned to AC wording (Intern Name, Task Name, AI Verdict).

### File List

- `src/main/java/com/examinai/review/MentorReviewController.java`
- `src/main/java/com/examinai/review/MentorReviewService.java`
- `src/main/java/com/examinai/review/MentorReviewQueueView.java`
- `src/main/java/com/examinai/review/MentorQueueLabelValue.java`
- `src/main/java/com/examinai/review/TaskReviewRepository.java`
- `src/main/resources/templates/mentor/review-queue.html`
- `src/main/resources/templates/mentor/review-detail-placeholder.html`
- `src/test/java/com/examinai/review/MentorReviewControllerTest.java`
- `src/test/java/com/examinai/review/MentorReviewServiceTest.java`
- `src/test/java/com/examinai/user/SecurityIntegrationTest.java`
- `_bmad-output/implementation-artifacts/4-1-mentor-review-queue.md`
- `_bmad-output/implementation-artifacts/sprint-status.yaml`

### Change Log

- 2026-04-22: Story 4.1 implementation completed; tests green; status → review.
- 2026-04-22: Post-review: 400 on invalid filter params; cross-filtered intern/task options; row links + verdict truncation; detail MockMvc 404/403; status → done.

---

**Story completion status:** Code review addressed; story **done**.

### Clarifications / open questions (non-blocking)

- Exact **query parameter names** (`status`, `internId`, `taskId` vs namespaced) — stay consistent in one PR and document in controller Javadoc.  
- Whether **intern/task filter** dropdowns list **all** mentors’ interns/tasks for admin — expected: only interns/tasks that appear in **current result set** or global lists; choose the option that matches product expectation (minimal: derive distinct values from the same base query without extra UX).
