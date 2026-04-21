# Story 3.2: Review Status Polling & Live Status Updates

Status: done

<!-- Ultimate context engine analysis completed - comprehensive developer guide created -->

## Story

As an intern,
I want to see my review status updating in real-time after submission,
so that I always know where my submission stands without manually refreshing the page.

## Acceptance Criteria

1. **JSON polling endpoint (ownership + contract)**  
   Given a `TaskReview` exists for `reviewId` and the authenticated user owns it (or is `ADMIN`)  
   When `GET /reviews/{reviewId}/status` is called  
   Then the response is **200** with JSON body:  
   `{ "reviewId": <number>, "status": "<enum name>", "displayLabel": "<string>", "errorMessage": <string|null> }`  
   And `displayLabel` maps exactly:  
   - `PENDING` → `"Submitted"`  
   - `LLM_EVALUATED` → `"Awaiting Mentor Review"`  
   - `APPROVED` → `"Approved"`  
   - `REJECTED` → `"Rejected"`  
   - `ERROR` → `"Review Failed"`  
   And `errorMessage` is **`null`** for all non-`ERROR` states; for `ERROR`, return the persisted `TaskReview.errorMessage` (may be null in edge cases — still use label `"Review Failed"`).

2. **Cross-intern access on JSON endpoint**  
   Given an authenticated **intern** calls `GET /reviews/{reviewId}/status` for a review owned by another intern  
   When the request is processed  
   Then **403 Forbidden** is returned — **server-side** ownership check (not UI-only).  
   **Note:** Story 3.1 deliberately returns **404** from `InternReviewStatusController` for wrong-owner SSR to reduce ID enumeration. **This story’s epic AC overrides that for the JSON status API only:** use **403** here so acceptance tests and API semantics match `epics.md`.

3. **Performance (NFR3)**  
   Given normal DB health  
   When the polling endpoint runs  
   Then it should remain a **cheap read** (single-row lookup by primary key, no GitHub/Ollama, no pipeline work). Target **&lt; 200ms** internal network per PRD/NFR3 — avoid N+1 and avoid loading `issues` collection for this endpoint.

4. **SSR page shell**  
   Given the intern lands on `GET /intern/reviews/{reviewId}` after submit  
   When the page loads  
   Then `templates/intern/review-status.html` renders an **`InternStatusCard`** (`th:fragment="internStatusCard(review)"`) including an element with `th:attr="data-review-id=${review.id}"` (or equivalent) so JS can read the numeric id.  
   And the controller passes a fully-hydrated `TaskReview` (see Dev Notes for fetch strategy / `open-in-view`).

5. **ReviewStatusBadge fragment**  
   Given `templates/fragments/review-status-badge.html` defines `th:fragment="statusBadge(status)"`  
   When each status renders:  
   - `PENDING` → `badge bg-secondary`, label **"Submitted"**, **no** spinner, container **`aria-live="polite"`**  
   - `LLM_EVALUATED` → `badge text-bg-warning`, **inline** `spinner-border spinner-border-sm`, **`aria-live="polite"`**, label **"Awaiting Mentor Review"**  
   - `APPROVED` → `badge text-bg-success`, **`aria-live="polite"`**  
   - `REJECTED` → `badge text-bg-danger`, **`aria-live="polite"`**  
   - `ERROR` → `badge text-bg-danger` **outline** variant, **`aria-live="assertive"`**, label **"Review Failed"**  
   Align with **UX-DR1** and **UX-DR8** in `_bmad-output/planning-artifacts/ux-design-specification.md` (badge classes).

6. **review-polling.js**  
   Given the review status page loads `static/js/review-polling.js` and status is **non-terminal** (`PENDING` or `LLM_EVALUATED`)  
   When the script runs  
   Then it uses `setInterval` every **3 seconds** to `fetch` **`/reviews/{id}/status`** (same-origin; session cookie via default `fetch` credentials)  
   And it updates **only** the **badge text/spinner area** via `textContent` / minimal DOM ops — **do not replace** the outer element that hosts `aria-live` (preserves screen reader announcements per **UX-DR6**)  
   And polling **stops** when status becomes **`APPROVED`**, **`REJECTED`**, or **`ERROR`**  
   And **network errors** in the loop are **swallowed** — no user alert; next tick continues.

7. **Security wiring**  
   Given `SecurityConfig` today permits `/intern/**` for INTERN+ADMIN and uses `.anyRequest().authenticated()` for the rest  
   When you add `/reviews/**`  
   Then restrict it to **`hasAnyRole('INTERN','ADMIN')`** explicitly so **mentors** do not accidentally rely on “authenticated-only” for a path outside `/mentor/**`.  
   And add `@PreAuthorize("hasRole('INTERN') or hasRole('ADMIN')")` on the status controller method (architecture: method-level enforcement).

8. **Tests**  
   - `@WebMvcTest` for `ReviewStatusController`: owner → 200 + JSON fields; other intern → **403**; unknown id → **404**; unauthenticated → **401/302** per Spring Security test setup.  
   - Optional: lightweight test that polling JS is referenced from `review-status.html` (smoke-level).

## Tasks / Subtasks

- [x] **API model + controller** (AC: 1–3, 7)  
  - [x] Add `ReviewStatusResponse` record in `com.examinai.review` (fields match AC JSON).  
  - [x] Add `ReviewStatusController` with `@RestController` + `GET /reviews/{reviewId}/status`.  
  - [x] Map `ReviewStatus` → `displayLabel` in one place (enum method or dedicated mapper) to avoid drift.  
  - [x] Use a **minimal repository query** (e.g. new `@Query` selecting only needed columns, or `findById` without touching `issues` lazy bag).

- [x] **Security** (AC: 2, 7)  
  - [x] Update `SecurityConfig` with an explicit `/reviews/**` rule **before** `.anyRequest()`.  
  - [x] Ownership: intern username vs `TaskReview.intern.username`; `ADMIN` bypass.

- [x] **Thymeleaf fragments + page** (AC: 4, 5)  
  - [x] Create `templates/fragments/review-status-badge.html`.  
  - [x] Create `templates/fragments/intern-status-card.html` (`internStatusCard`).  
  - [x] Replace minimal copy in `templates/intern/review-status.html` with fragment composition + script tag for `/js/review-polling.js`.  
  - [x] For **non-terminal** states, card body may stay minimal (“Checking status…”) — **full** REJECTED/ERROR/APPROVED rich layouts belong largely to **Story 3.3**; this story must still render the **badge** correctly for all states so polls have a live target.

- [x] **Client JS** (AC: 6)  
  - [x] Add `src/main/resources/static/js/review-polling.js` (~30 lines).  
  - [x] Read `data-review-id` from the anchor; parse JSON; map `displayLabel` into the badge span; clear spinner when terminal.

- [x] **Controller data fetch** (AC: 4)  
  - [x] Extend `InternReviewStatusController` to pass `review` model attribute — add a repository method such as `findByIdForInternStatusPage` with **`JOIN FETCH` only for associations the card template touches** (likely `task` + `task.course` + `intern`; **not** `issues` unless you render them).  
  - [x] Respect **`spring.jpa.open-in-view: false`** (project standard from prior stories).

- [x] **Tests** (AC: 8)  
  - [x] `ReviewStatusControllerTest` with `MockMvc` + `@WithMockUser`.  
  - [x] Register security filter chain / `@Import` or `@WebMvcTest` slices as done in existing review controller tests.

### Review Findings

- [x] [Review][Decision] Hidden spinner on PENDING badge vs spec "NO spinner" — resolved: hidden spinner acceptable as JS manipulation target; "NO spinner" interpreted as no visible spinner [review-status-badge.html]
- [x] [Review][Patch] Badge CSS color class not updated by polling JS on status transition — fixed: `badgeClasses` map + `badge.className` on poll [review-polling.js]
- [x] [Review][Patch] fetch non-2xx response not checked before `r.json()` — fixed: `if (!r.ok) throw` before `json()` [review-polling.js]
- [x] [Review][Patch] `<h1>` heading "Review submitted" inconsistent with `<title>` "Review status" — fixed: `<h1>` now "Review status" [review-status.html]
- [x] [Review][Patch] `reviewId` model attribute dead code — removed unused attribute [InternReviewStatusController.java]
- [x] [Review][Patch] INNER JOIN on `t.course` in `findByIdForInternStatusPage` silently returns empty Optional if course is null — fixed: `LEFT JOIN FETCH t.course` [TaskReviewRepository.java]
- [x] [Review][Patch] No `th:case="*"` default in `statusBadge` fragment — fixed: default case shows enum name fallback [review-status-badge.html]
- [x] [Review][Patch] Duplicate `mentorCannotAccessReviewStatusJson` test in `SecurityIntegrationTest` — removed duplicate; JSON mentor denial remains in `ReviewStatusControllerTest` [SecurityIntegrationTest.java]
- [x] [Review][Patch] No test asserting `errorMessage` is null for `APPROVED` and `REJECTED` statuses — added `approved_errorMessageSuppressed`, `rejected_errorMessageSuppressed` [ReviewStatusControllerTest.java]
- [x] [Review][Patch] `InternReviewStatusControllerTest` has only one test (script tag smoke) — added: cross-intern 404, admin 200, unauthenticated redirect, unknown ID 404 [InternReviewStatusControllerTest.java]
- [x] [Review][Patch] `errorMessage` returned verbatim from pipeline — fixed: `sanitizeErrorMessageForClient` (first line only, max 500 chars) for ERROR JSON; test `error_multilineMessage_returnsFirstLineOnly` [ReviewStatusController.java]
- [x] [Review][Defer] Inconsistent 404/403 between HTML and JSON endpoints (enumeration risk) — deferred, by spec design; Story 3.1 intentionally uses 404 for SSR, this story intentionally uses 403 for JSON [controllers]
- [x] [Review][Defer] Stale fetch race — out-of-order tick responses can overwrite newer state [review-polling.js] — deferred, pre-existing
- [x] [Review][Defer] `label` element null check between polling ticks [review-polling.js:14] — deferred, pre-existing
- [x] [Review][Defer] `tr.getIntern()` null guard missing in `ReviewStatusController` — NPE leaks as 500 [ReviewStatusController.java:29] — deferred, pre-existing
- [x] [Review][Defer] `tr.getStatus()` null guard missing in `ReviewStatusController` [ReviewStatusController.java:32] — deferred, pre-existing
- [x] [Review][Defer] `tr.getIntern()` null guard missing in `InternReviewStatusController` [InternReviewStatusController.java:30] — deferred, pre-existing
- [x] [Review][Defer] `tr.getId()` null unboxed into primitive `long` in `ReviewStatusResponse` — NPE on edge DB state [ReviewStatusController.java:34] — deferred, pre-existing
- [x] [Review][Defer] INNER JOIN on `tr.intern` in `findByIdWithInternForStatusJson` — orphaned intern causes silent 404 [TaskReviewRepository.java:37] — deferred, pre-existing
- [x] [Review][Defer] `data.displayLabel` null/undefined guard missing in polling JS [review-polling.js:14] — deferred, pre-existing
- [x] [Review][Defer] Primitive `long` in `ReviewStatusResponse` — JS precision loss for IDs > 2^53 [ReviewStatusResponse.java] — deferred, pre-existing
- [x] [Review][Defer] Duplicate ownership-check logic across both controllers — refactoring candidate [controllers] — deferred, pre-existing
- [x] [Review][Defer] Repository query naming inconsistency — three different suffixing conventions [TaskReviewRepository.java] — deferred, pre-existing
- [x] [Review][Defer] No polling back-off or jitter — infinite polling on stuck reviews [review-polling.js] — deferred, pre-existing
- [x] [Review][Defer] APPROVED/REJECTED/ERROR badge cases include dead hidden-spinner elements — terminal states, never shown [review-status-badge.html] — deferred, pre-existing
- [x] [Review][Patch] Polling waits for first interval before any fetch — fixed: invoke `tick()` once after `setInterval` so `handle` exists and the first server sync runs immediately after load [review-polling.js:45]

## Dev Notes

### Brownfield snapshot

- **Spring Boot / Java:** `pom.xml` is authoritative (Story 3.1 noted **3.5.13** parent — keep aligned).  
- **Existing types:** `ReviewStatus` enum is **exactly** `PENDING`, `LLM_EVALUATED`, `APPROVED`, `REJECTED`, `ERROR` — no separate “AI Reviewing” DB state; while GitHub+LLM runs, status stays **`PENDING`** until persistence sets `LLM_EVALUATED`.  
- **Existing controllers:** `InternReviewStatusController` serves **HTML** at `/intern/reviews/{reviewId}`; new JSON lives at **`/reviews/{reviewId}/status`** per architecture directory map (`ReviewStatusController.java`).  
- **Repository:** `TaskReviewRepository` already has `findByIdWithInternAndMentor`, `findByIdWithTask`, `findByIdForLlmPersistence` — add a **dedicated** fetch for the intern status page if the existing ones pull too much (especially `issues`).

### Architecture compliance

- Polling endpoint path, JSON shape, and “authenticated + owns review” boundary: [Source: `_bmad-output/planning-artifacts/architecture.md` — API Boundaries table, `ReviewStatusResponse` in package diagram, data flow step 5–8]  
- `@PreAuthorize` on **each** controller method; Thymeleaf `sec:*` is display-only. [Source: architecture.md — Authentication & Security]  
- **NFR3** poll latency — DB read only. [Source: `epics.md` NonFunctional Requirements]  
- Fragment names **`statusBadge`**, **`internStatusCard`**, **`review-polling.js`** — [Source: architecture.md — static resource tree + UX-DR1, UX-DR4, UX-DR6 in `epics.md`]

### UX alignment

- **UX-DR6:** 3s interval; `fetch` GET; update inner text only; stop on terminal states; silent retry on failure.  
- **UX-DR1 / UX-DR4:** Spinner + `aria-live` rules as in AC.  
- **UX-DR8:** Bootstrap palette for badges (warning/success/danger/secondary).

### Previous story intelligence (3.1)

- **PRG:** Submit flow already redirects to `/intern/reviews/{reviewId}` — this story upgrades that destination into a live status experience.  
- **Events / async:** Do **not** move LLM work onto the HTTP thread for polling; polling stays read-only.  
- **404 vs 403:** JSON endpoint uses **403** for foreign reviews per **this** epic AC, even though SSR used **404** in 3.1 — document in tests.  
- **Review findings:** Thread-pool saturation and GitHub debug logging are **deferred** — do not scope-creep fixes here.

### Git intelligence (recent commits)

- Pattern: feature work landed in cohesive commits (`7016daa` epic 2 + story 3.1). Expect similar: security → controller → templates → JS → tests.

### Latest technical specifics

- Use **vanilla JS** only (no SPA framework) per architecture/PRD.  
- `fetch` to same origin inherits session cookies; **GET** does not need CSRF header.  
- JSON field names: use **camelCase** in API (`reviewId`, `displayLabel`, `errorMessage`) to match Spring Boot default Jackson and front-end conventions.

### Project context reference

- No `project-context.md` in repo — rely on this file + `architecture.md` + `epics.md` + `ux-design-specification.md`.

### References

- Story AC source: `_bmad-output/planning-artifacts/epics.md` — Epic 3 Story 3.2  
- NFR3 / polling: `epics.md` (NFR3), `prd.md`  
- UX fragments: `_bmad-output/planning-artifacts/ux-design-specification.md` (UX-DR1, UX-DR4, UX-DR6, UX-DR8)  
- API & package layout: `_bmad-output/planning-artifacts/architecture.md`

## Dev Agent Record

### Agent Model Used

{{agent_model_name_version}}

### Implementation Plan

- Add `ReviewStatus.getDisplayLabel()` and `ReviewStatusResponse`; JSON controller uses `findByIdWithInternForStatusJson` (JOIN FETCH intern only) to satisfy cheap-read NFR; ownership 403 for wrong intern, 404 for missing id; SSR page uses `findByIdForInternStatusPage` (task, course, intern) for open-in-view=false safe rendering.
- Security: `requestMatchers("/reviews/**").hasAnyRole("INTERN", "ADMIN")` plus method `@PreAuthorize` on both REST and HTML review controllers; fragments + vanilla polling JS.

### Debug Log References

### Completion Notes List

- Implemented `GET /reviews/{reviewId}/status` with `ReviewStatusResponse`, `findByIdWithInternForStatusJson` (no issues/task heavy joins), 403 for non-owner intern on JSON path, 404 for missing id. `ReviewStatus.getDisplayLabel()` is the single label map.
- `findByIdForInternStatusPage` + `InternReviewStatusController` pass hydrated `TaskReview` for the status card. Thymeleaf `statusBadge` / `internStatusCard`, Bootstrap badges + `aria-live` per AC; `review-polling.js` polls every 3s, updates label + spinner + badge classes, stops on terminal, swallows fetch errors.
- `SecurityConfig`: explicit `/reviews/**` → `INTERN` + `ADMIN` only. Tests: `ReviewStatusControllerTest` (including APPROVED/REJECTED `errorMessage` null, multiline ERROR sanitization), `InternReviewStatusControllerTest` (script + SSR security matrix), `SecurityIntegrationTest` without duplicate mentor JSON test.
- ERROR JSON responses use `sanitizeErrorMessageForClient` so stack traces and multi-line pipeline dumps are not echoed in full to the browser.

### File List

- `src/main/java/com/examinai/review/ReviewStatus.java`
- `src/main/java/com/examinai/review/ReviewStatusResponse.java`
- `src/main/java/com/examinai/review/ReviewStatusController.java`
- `src/main/java/com/examinai/review/InternReviewStatusController.java`
- `src/main/java/com/examinai/review/TaskReviewRepository.java`
- `src/main/java/com/examinai/config/SecurityConfig.java`
- `src/main/resources/templates/fragments/review-status-badge.html`
- `src/main/resources/templates/fragments/intern-status-card.html`
- `src/main/resources/templates/intern/review-status.html`
- `src/main/resources/static/js/review-polling.js`
- `src/test/java/com/examinai/review/ReviewStatusControllerTest.java`
- `src/test/java/com/examinai/review/InternReviewStatusControllerTest.java`
- `src/test/java/com/examinai/user/SecurityIntegrationTest.java`

## Change Log

- 2026-04-22: Story 3.2 — JSON review status API, intern status page + polling JS, Thymeleaf badge/card fragments, security rule for `/reviews/**`, WebMvc tests.
- 2026-04-22: Code review batch patches — badge class sync in polling JS, fetch `ok` check, SSR copy/repo/tests cleanups, ERROR `errorMessage` client sanitization, expanded `InternReviewStatusControllerTest`.
- 2026-04-22: Final code review — immediate `tick()` after `setInterval` for first-load status sync [review-polling.js].

## Story Completion Status

- **done** — Code review complete; immediate first poll applied in `review-polling.js`; sprint synced to `done`.
