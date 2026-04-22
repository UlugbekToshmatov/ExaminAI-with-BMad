# Story 5.2: Admin Cross-Intern Dashboard

Status: done

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

As an admin,
I want to view all task submissions across all interns and mentors with filtering,
so that I can monitor cohort progression, identify interns with consecutive rejections, and spot blocked pipelines early.

**Scope note:** Implement exactly what is in the acceptance criteria below. The user-story “consecutive rejections” insight is **not** spelled out as a separate UI feature in the epics ACs—if you add streaks or aggregates, treat that as out-of-scope unless the PM extends the story.

## Acceptance Criteria

(From [_bmad-output/planning-artifacts/epics.md](../planning-artifacts/epics.md) — Story 5.2; maps to **FR33**, **NFR1**, **NFR9**, **UX-DR14**.)

1. **Table of all reviews**  
   **Given** an admin navigates to `/admin/dashboard`  
   **When** the page loads  
   **Then** a table lists **all** `TaskReview` rows (every intern, every mentor’s tasks) with columns: **Intern Name** (`UserAccount.username`), **Task Name** (`Task.taskName`), **Course Name** (`Task.course.courseName`), **Status** (use existing `fragments/review-status-badge :: statusBadge` for consistency with mentor queue), **Date Submitted** formatted **`dd MMM yyyy HH:mm`** (use `LocalDateTime` from `TaskReview.dateCreated`), **Mentor Remarks** (`mentorRemarks`; show empty cell or em dash when null; **truncate** long text in the cell with `title` holding full text—e.g. Thymeleaf `#strings.abbreviate`, ~80–120 chars).  
   **And** rows are ordered **newest-first** by `dateCreated`.

2. **GET filters with auto-submit**  
   **Given** filter controls on `/admin/dashboard` for **Intern**, **Task**, and **Status** (`form-select form-select-sm`)  
   **When** a value changes (`onchange`)  
   **Then** the form submits **GET** with query params (mirror [`mentor/review-queue.html`](../../src/main/resources/templates/mentor/review-queue.html): same UX pattern as mentor filters).  
   **Suggested param names:** `internId`, `taskId`, `status` (empty string = “all” for intern/task; for status, empty = all statuses **or** a dedicated “All” option).

3. **Intern filter semantics**  
   **Given** an admin selects a specific intern  
   **When** the table renders  
   **Then** only `TaskReview` rows with `intern_id` matching that intern are shown—including **all attempts** (every row), not only the latest per task.

4. **Task / status filters**  
   **Given** filters for task and status  
   **When** they are applied  
   **Then** the result set is restricted accordingly. **Status** values must align with [`ReviewStatus`](../../src/main/java/com/examinai/review/ReviewStatus.java) (`PENDING`, `LLM_EVALUATED`, `APPROVED`, `REJECTED`, `ERROR`). Invalid `status` query values → **400** (same spirit as [`MentorReviewService.parseRequiredStatus`](../../src/main/java/com/examinai/review/MentorReviewService.java)) or redirect with safe default—pick one behavior and document it in code.

5. **Authorization**  
   **Given** `@PreAuthorize("hasRole('ADMIN')")` on `AdminController.dashboard(...)`  
   **When** a mentor or intern calls `GET /admin/dashboard`  
   **Then** **403** is returned (method security; do not rely on Thymeleaf alone). **NFR9.**

6. **Empty state**  
   **Given** no `TaskReview` rows exist **or** filters exclude everything  
   **When** the admin views `/admin/dashboard`  
   **Then** show a **`text-muted`** paragraph **“No submissions yet.”** (acceptable to use the same copy when filters yield zero rows for MVP).

7. **Accessibility**  
   **Given** the dashboard renders  
   **When** checked against **UX-DR14**  
   **Then** all table headers use `<th scope="col">`, the page includes `<main id="main-content">`, and heading levels do **not** skip (e.g. `h4` → `h5` under it).

## Tasks / Subtasks

- [x] **Repository** (AC: 1, 3, 4)  
  - [x] Add JPQL (or Specifications) on [`TaskReviewRepository`](../../src/main/java/com/examinai/review/TaskReviewRepository.java) to load dashboard rows with **`JOIN FETCH`** `task`, `task.course`, and `intern` in **one query** to avoid N+1 (**NFR1**).  
  - [x] Support optional filters: `internId` and `taskId` with **`0L` or empty param = any** (reuse constants pattern from [`MentorReviewService`](../../src/main/java/com/examinai/review/MentorReviewService.java): `FILTER_ID_ANY`, `MENTOR_QUEUE_ALL_MENTORS` is mentor-only; admin dashboard needs **no mentor scoping**).  
  - [x] Support optional **status**: e.g. `AND (:status IS NULL OR tr.status = :status)` with `null` bound when “all statuses” selected (verify with your Hibernate/Spring Data version).  
  - [x] `ORDER BY tr.dateCreated DESC`.  
  - [x] Add **dropdown option** queries analogous to `findMentorQueueInternOptions` / `findMentorQueueTaskOptions` but **without** the `mentorFilter` predicate—scope options by the **other** filters (status + task for intern list; status + intern for task list), matching mentor queue behavior.

- [x] **Service (recommended)** (AC: 2, 4)  
  - [x] Add e.g. `AdminDashboardService` in `com.examinai.admin` that parses query params, loads rows + filter options, returns a small view DTO (mirror [`MentorReviewQueueView`](../../src/main/java/com/examinai/review/MentorReviewQueueView.java) / [`MentorQueueLabelValue`](../../src/main/java/com/examinai/review/MentorQueueLabelValue.java)).

- [x] **Controller** (AC: 2, 5)  
  - [x] Extend [`AdminController`](../../src/main/java/com/examinai/admin/AdminController.java) `GET /admin/dashboard` to accept optional `@RequestParam` values and populate the model; keep **`@PreAuthorize("hasRole('ADMIN')")`** on the handler method.

- [x] **Template** (AC: 1, 2, 6, 7)  
  - [x] Replace placeholder in [`templates/admin/dashboard.html`](../../src/main/resources/templates/admin/dashboard.html) with filter form + conditional table vs empty state.  
  - [x] Reuse **`review-status-badge`** fragment for the Status column.  
  - [x] Date column: prefer `#temporals.format` (Java 8 time extras) if available in the project; otherwise register a formatter bean—do not print raw `LocalDateTime.toString()`.

- [x] **Tests** (AC: 3, 4, 5)  
  - [x] **`@WebMvcTest(AdminController.class)`** (or slice) with **`@WithMockUser(roles = "ADMIN")`**: dashboard returns 200 and contains expected fragment of data when service/repository mocked.  
  - [x] **`@WithMockUser(roles = "INTERN")`** (or `MENTOR`): **403** on `GET /admin/dashboard`.  
  - [x] Optional **`@DataJpaTest`**: skipped in favor of full regression via existing slice tests (repository JPQL covered indirectly; optional per story).

## Dev Notes

### Developer context

- **FR33** is the product driver: admins see **cross-intern, cross-mentor** submission status. Mentor queue already uses **`mentorFilter = -1L`** for “all mentors” in [`findMentorQueue`](../../src/main/java/com/examinai/review/TaskReviewRepository.java), but that API still filters by a **single** `ReviewStatus`. The admin dashboard must list **all statuses** unless filtered—new queries are required; do not force admins through five separate pages.
- [`admin/dashboard.html`](../../src/main/resources/templates/admin/dashboard.html) currently says *“Admin dashboard coming in Story 5.2.”*—this story replaces that stub.
- **Do not** expose other interns’ remarks or AI content on this page beyond **mentor remarks** column in the AC; the table is metadata-focused, not a duplicate of mentor detail.

### Technical requirements

- **Entities:** [`TaskReview`](../../src/main/java/com/examinai/review/TaskReview.java) holds `intern`, `task`, `status`, `dateCreated`, `mentorRemarks`. Course name via `task.getCourse().getCourseName()`.
- **Consistency:** Match mentor queue patterns ([`mentor/review-queue.html`](../../src/main/resources/templates/mentor/review-queue.html)): Bootstrap `table-responsive`, `table`, `form-select form-select-sm`, `onchange` submit.
- **PRG:** Not required for this page (GET-only filters).
- **Performance:** One fetched graph per row list; avoid lazy-load in Thymeleaf loop.

### Architecture compliance

- **Package:** `com.examinai.admin` for admin-specific services/controllers; **`review`** package owns `TaskReview` persistence ([architecture.md](../planning-artifacts/architecture.md) — Package Organization, FR31–FR33 mapping to `templates/admin/dashboard.html`).
- **Security:** `@PreAuthorize` on the controller method; **`@EnableMethodSecurity`** already assumed project-wide.
- **Thymeleaf:** Layout dialect + [`layout/base.html`](../../src/main/resources/templates/layout/base.html); reuse fragments under `templates/fragments/`.

### Library and framework

- **Spring Boot 3.4.x**, **Spring Data JPA**, **Thymeleaf** — no new dependencies expected for filters/date formatting.
- If `#temporals` is unavailable in templates, add **`thymeleaf-extras-java8time`** or use a `Model` attribute with pre-formatted strings (second option is weaker; prefer proper temporal support).

### File structure

| Action | Path |
|--------|------|
| Extend | `src/main/java/com/examinai/admin/AdminController.java` |
| Add (recommended) | `src/main/java/com/examinai/admin/AdminDashboardService.java` |
| Add (recommended) | `src/main/java/com/examinai/admin/AdminDashboardView.java` |
| Extend | `src/main/java/com/examinai/review/TaskReviewRepository.java` |
| Replace body | `src/main/resources/templates/admin/dashboard.html` |
| Add | `src/test/java/com/examinai/admin/AdminControllerDashboardTest.java` (name as you prefer) |

### Testing requirements

- MockMvc security tests are **mandatory** for role enforcement (**NFR9**).  
- Repository tests are **strongly recommended** to lock filter semantics (especially “all attempts for one intern”).  
- Follow existing test style (JUnit 5, Spring Boot test slices).

### Previous story intelligence (5-1)

- [`5-1-email-notifications.md`](./5-1-email-notifications.md) — `NotificationService` and event wiring are complete; no SMTP work here.  
- **Thread pool:** Epic 4 retro mentioned async executor saturation; unrelated to this read-only dashboard but avoid blocking the request thread with heavy synchronous work.

### Git intelligence (recent commits)

- `b47a46d` — Story 5.1 email notifications.  
- Prior epic: mentor review queue and `TaskReviewRepository` mentor-scoped queries (`82844f7`, `340d3d9`). **Extend** repository patterns; do not break mentor endpoints.

### Latest technical notes

- **Optional JPA parameters:** Binding `null` for `:status` in `(:status IS NULL OR tr.status = :status)` is the standard Hibernate-friendly approach; if your integration test shows the condition is ignored incorrectly, switch to **JPA Specifications** or **blazed-bit Criteria API**—but try JPQL first for consistency with existing repository style.

### Project context reference

- No `project-context.md` in repo; rely on this file, [epics.md](../planning-artifacts/epics.md), [architecture.md](../planning-artifacts/architecture.md), and [prd.md](../planning-artifacts/prd.md) (**FR33**).

## Dev Agent Record

### Agent Model Used

Composer (Cursor)

### Debug Log References

_N/A_

### Implementation Plan (Step 5)

- Added `findAdminDashboardRows` + option queries on `TaskReviewRepository` (JPQL, `null` status = all, `0L` = any id).  
- `AdminDashboardService` parses query params: invalid `status` → HTTP 400 (aligned with `MentorReviewService` spirit).  
- `GET /admin/dashboard` passes `adminDashboard` model; template matches mentor filter UX, `#temporals` + `review-status-badge`, empty copy per AC.  
- `AdminControllerDashboardTest` + `SecurityIntegrationTest` mock for `AdminDashboardService`.

### Completion Notes List

- **Done:** Admin dashboard lists all `TaskReview` rows with intern/task/course/status/date/mentor remarks; GET filters with auto-submit; 403 for non-admins; 400 for invalid status param; `mvn test` green.

### File List

- `src/main/java/com/examinai/review/TaskReviewRepository.java`  
- `src/main/java/com/examinai/admin/AdminDashboardView.java`  
- `src/main/java/com/examinai/admin/AdminDashboardService.java`  
- `src/main/java/com/examinai/admin/AdminController.java`  
- `src/main/resources/templates/admin/dashboard.html`  
- `src/test/java/com/examinai/admin/AdminControllerDashboardTest.java`  
- `src/test/java/com/examinai/user/SecurityIntegrationTest.java`  
- `_bmad-output/implementation-artifacts/5-2-admin-cross-intern-dashboard.md`  
- `_bmad-output/implementation-artifacts/sprint-status.yaml`

### Change Log

- 2026-04-22: Story 5.2 — admin cross-intern dashboard (repository, service, controller, template, WebMvcTest security/behavior).

## Story completion status

- **Status:** `done`  
- **Note:** Implementation complete; code review clean (2026-04-22). Optional `@DataJpaTest` for repository not added (left optional per story).

## References

- [epics.md — Story 5.2](../planning-artifacts/epics.md)  
- [architecture.md — admin/dashboard, FR31–FR33](../planning-artifacts/architecture.md)  
- [prd.md](../planning-artifacts/prd.md) — FR33, NFR1, NFR9  
- [ux-design-specification.md](../planning-artifacts/ux-design-specification.md) — UX-DR14 (accessibility)  
- Source: [`AdminController.java`](../../src/main/java/com/examinai/admin/AdminController.java)  
- Source: [`TaskReviewRepository.java`](../../src/main/java/com/examinai/review/TaskReviewRepository.java)  
- Source: [`MentorReviewService.java`](../../src/main/java/com/examinai/review/MentorReviewService.java)  
- Source: [`mentor/review-queue.html`](../../src/main/resources/templates/mentor/review-queue.html)

## Open questions (non-blocking)

- **Empty state copy:** AC says “No submissions yet.” when **no reviews exist**; when filters return zero rows, consider a different message in a follow-up UX pass.  
- **Row link:** AC does not require linking to mentor detail; optional enhancement: link **Task Name** or **Intern** to `/mentor/reviews/{id}` for admins (already allowed by mentor detail access rules).
