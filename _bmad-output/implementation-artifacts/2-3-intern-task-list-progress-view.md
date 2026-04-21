# Story 2.3: Intern Task List & Progress View

Status: done

## Story

As an intern,
I want to view all available tasks as a visual card grid with status indicators showing my personal progress on each task,
So that I always know what needs to be done and can track how far I've come through the curriculum.

## Acceptance Criteria

**AC1 — Card-grid layout:**
Given an intern is authenticated and tasks exist in the system
When they navigate to `/intern/tasks`
Then tasks are displayed as a Bootstrap card-grid (`row g-3`, `col-md-4` per card) using the `TaskStatusCard` fragment

**AC2 — Not-started card:**
Given the `TaskStatusCard` fragment (`th:fragment="taskStatusCard(task, review)"`) is implemented
When a task has no review from the current intern (`review` is null)
Then the card shows a grey (`#dee2e6`) left border and label "Not Started"

**AC3 — Approved card:**
Given the `TaskStatusCard` fragment receives a review with status `APPROVED`
When the card renders
Then the card shows a green (`#198754`) left border

**AC4 — In-review card:**
Given the `TaskStatusCard` fragment receives a review with status `PENDING` or `LLM_EVALUATED`
When the card renders
Then the card shows an amber (`#ffc107`) left border

**AC5 — Rejected card:**
Given the `TaskStatusCard` fragment receives a review with status `REJECTED`
When the card renders
Then the card shows a red (`#dc3545`) left border

**AC6 — Error card:**
Given the `TaskStatusCard` fragment receives a review with status `ERROR`
When the card renders
Then the card shows a red dashed-border outline variant

**AC7 — Progress bar:**
Given a progress bar is rendered at the top of `/intern/tasks`
When the page loads
Then it shows the percentage of tasks that have at least one `APPROVED` review, computed for the current intern only

**AC8 — Empty state:**
Given no tasks exist in the system
When an intern navigates to `/intern/tasks`
Then a `text-muted` paragraph "No tasks have been assigned yet." is shown in place of the grid

**AC9 — task_review table already migrated:**
Given Liquibase changelogs `001-init-schema.sql` and `003-indexes.sql` already define the `task_review` table and its indexes
When the application starts
Then the table and indexes already exist — NO new changelog is needed for this story

**AC10 — findForInternByUsername null safety:**
Given `TaskService.findForInternByUsername(username)` queries tasks and joins the intern's latest `TaskReview` per task
When no reviews exist in the database
Then each task is returned with `review = null` in `TaskWithReview` and no NullPointerException occurs

## Tasks / Subtasks

- [x] Task 1: Create `ReviewStatus` enum (AC: 3, 4, 5, 6)
  - [x] Create `src/main/java/com/examinai/review/ReviewStatus.java`
  - [x] Values: `PENDING`, `LLM_EVALUATED`, `APPROVED`, `REJECTED`, `ERROR`
  - [x] No annotations needed — stored as String in DB via `@Enumerated(EnumType.STRING)` on entity field

- [x] Task 2: Create `TaskReview` entity (AC: 9, 10)
  - [x] Create `src/main/java/com/examinai/review/TaskReview.java`
  - [x] Annotate: `@Entity`, `@Table(name = "task_review")`
  - [x] Fields:
    - `id` (Long, `@Id @GeneratedValue(strategy = IDENTITY)`)
    - `task` (`@ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "task_id", nullable = false)` → `Task`)
    - `intern` (`@ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "intern_id", nullable = false)` → `UserAccount`)
    - `mentor` (`@ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "mentor_id")` → `UserAccount`, nullable)
    - `status` (`@Enumerated(EnumType.STRING) @Column(name = "status", nullable = false)` → `ReviewStatus`)
    - `llmResult` (`@Column(name = "llm_result")` → `String`)
    - `mentorResult` (`@Column(name = "mentor_result")` → `String`)
    - `mentorRemarks` (`@Column(name = "mentor_remarks", columnDefinition = "TEXT")` → `String`)
    - `errorMessage` (`@Column(name = "error_message", length = 500)` → `String`)
    - `dateCreated` (`@Column(name = "date_created", nullable = false)` → `LocalDateTime`)
  - [x] No `@PrePersist` — `date_created` defaults to `now()` in DB; Epic 3 sets it explicitly
  - [x] No-arg constructor + getters only (no setters on `id`/`dateCreated`); full setters on all mutable fields
  - [x] No JPA cascade — DB `ON DELETE CASCADE` on `task_review.task_id` is sufficient

- [x] Task 3: Create `TaskReviewRepository` (AC: 10)
  - [x] Create `src/main/java/com/examinai/review/TaskReviewRepository.java`
  - [x] Extends `JpaRepository<TaskReview, Long>`
  - [x] Add custom JPQL query:
    ```java
    @Query("SELECT tr FROM TaskReview tr WHERE tr.intern.id = :internId ORDER BY tr.dateCreated DESC")
    List<TaskReview> findAllByInternIdOrderByDateCreatedDesc(@Param("internId") Long internId);
    ```

- [x] Task 4: Create `TaskWithReview` DTO (AC: 1, 7, 10)
  - [x] Create `src/main/java/com/examinai/task/TaskWithReview.java`
  - [x] Implement as a Java **record** (not a class — no Thymeleaf `th:object` binding needed, read-only)
    ```java
    package com.examinai.task;
    import com.examinai.review.TaskReview;
    public record TaskWithReview(Task task, TaskReview review) {}
    ```
  - [x] `review` may be `null` (not-started tasks)

- [x] Task 5: Update `TaskRepository` with JOIN FETCH query (AC: 10)
  - [x] Open `src/main/java/com/examinai/task/TaskRepository.java`
  - [x] Add new method alongside existing `findAllByOrderByTaskNameAsc()`:
    ```java
    @Query("SELECT t FROM Task t JOIN FETCH t.course ORDER BY t.taskName ASC")
    List<Task> findAllWithCourseOrderByTaskNameAsc();
    ```
  - [x] Keep `findAllByOrderByTaskNameAsc()` for `TaskService.findAll()` — do NOT remove it
  - [x] `open-in-view: false` is set → LAZY loading in templates will throw `LazyInitializationException`; JOIN FETCH is mandatory for `task.course.courseName` template access

- [x] Task 6: Add `findForInternByUsername()` to `TaskService` (AC: 7, 10)
  - [x] Open `src/main/java/com/examinai/task/TaskService.java`
  - [x] Inject `TaskReviewRepository` via constructor (add parameter, update constructor body)
  - [x] Add new method with `@Transactional(readOnly = true)` and `@PreAuthorize`
  - [x] Add required imports: `com.examinai.review.TaskReview`, `com.examinai.review.TaskReviewRepository`, `java.util.Map`
  - [x] `@PreAuthorize` on this new method individually (mandatory pattern)
  - [x] Do NOT add `@Transactional` to the class level or to the existing `findAll`/`findById` signatures

- [x] Task 7: Flesh out `InternTaskController` (AC: 1, 7, 8, 10)
  - [x] Open `src/main/java/com/examinai/task/InternTaskController.java`
  - [x] Add `TaskService` constructor injection (remove the no-arg constructor style if needed)
  - [x] Replace stub `taskList()` method body with model attributes and progress calculation
  - [x] Keep `@RequestMapping("/intern")` at class level — don't change it
  - [x] Add imports: `org.springframework.ui.Model`, `org.springframework.security.core.Authentication`, `com.examinai.task.TaskWithReview`, `java.util.List`

- [x] Task 8: Create `templates/fragments/task-status-card.html` (AC: 2–6)
  - [x] Create `src/main/resources/templates/fragments/task-status-card.html`
  - [x] Fragment signature: `th:fragment="taskStatusCard(task, review)"`
  - [x] Left border logic via `th:style` for all statuses (null→grey, APPROVED→green, REJECTED→red, ERROR→dashed-red, PENDING/LLM_EVALUATED→amber)
  - [x] Fragment lives in `templates/fragments/` per architecture — do NOT put it in `templates/intern/`

- [x] Task 9: Replace stub `templates/intern/task-list.html` (AC: 1, 7, 8)
  - [x] Open `src/main/resources/templates/intern/task-list.html`
  - [x] Replace stub with card-grid layout, progress bar, and empty state
  - [x] Uses `twr.task()` and `twr.review()` — record accessor syntax
  - [x] Fragment include: `th:replace="~{fragments/task-status-card :: taskStatusCard(...)}"`

- [x] Task 10: Update `SecurityIntegrationTest` for new `TaskService` dependency (AC: 1)
  - [x] Open `src/test/java/com/examinai/user/SecurityIntegrationTest.java`
  - [x] Add `@MockBean TaskService taskService;` field alongside existing `@MockBean` fields
  - [x] Add import: `import com.examinai.task.TaskService;`
  - [x] Add `@BeforeEach` setup with `when(taskService.findForInternByUsername(any())).thenReturn(emptyList())`
  - [x] All 10 security tests pass

- [x] Task 11: Add `findForInternByUsername` test to `TaskServiceTest` (AC: 10)
  - [x] Open `src/test/java/com/examinai/task/TaskServiceTest.java`
  - [x] Add `@Mock TaskReviewRepository taskReviewRepository;` alongside existing mocks
  - [x] Add `findForInternByUsername_withNoReviews_returnsTasksWithNullReview` test
  - [x] Fixed pre-existing test bugs: added `Role.MENTOR` to create/update tests, added admin SecurityContext for delete/update ownership checks
  - [x] All 5 TaskServiceTest tests pass

## Dev Notes

### Implementation snapshot

- **Canonical code**: `ReviewStatus`, `TaskReview`, `TaskReviewRepository` in `src/main/java/com/examinai/review/`; `TaskWithReview`, `TaskService.findForInternByUsername`, `InternTaskController` in `src/main/java/com/examinai/task/`; templates under `src/main/resources/templates/fragments/` and `templates/intern/`.
- **Spring Boot**: use **`pom.xml`** as source of truth (planning docs may still list 3.4.x).
- **Epic doc vs code**: `epics.md` calls the operation `TaskService.findForIntern()`; the implemented API is **`findForInternByUsername(String)`** only — do not add a second `findForIntern()` unless you refactor all call sites.
- **`task_review`**: defined in `001-init-schema.sql` (and indexes in `003-indexes.sql`); no new Liquibase file for this story.
- **Git at validation**: `d385ffc` — *Context for story 2.3 of epic 2 created*
- **Tracking**: story **Status** and `sprint-status.yaml` use **`review`** after this validation pass.

### Cross-story deferrals

`_bmad-output/implementation-artifacts/deferred-work.md` lists follow-ups from earlier stories (e.g. `task_review.status` as unconstrained `VARCHAR`, mentor FK behavior). Epic 3 should reconcile those when the review pipeline lands.

### NFR and UX

- **NFR1** (PRD): server-rendered pages &lt; 1s on internal network — smoke **`GET /intern/tasks`** after a cold start; should be near-instant with SSR.
- **UX-DR11 / UX-DR5**: card grid + `TaskStatusCard` — satisfied by `task-list.html` + `fragments/task-status-card.html`.
- **UX-DR14**: extend `layout/base.html`; keep new content under `<main id="main-content">` and do not skip heading levels in this template.

### Latest `TaskReview` per task

Reviews load **newest first** (`findAllByInternIdOrderByDateCreatedDesc`). The `toMap` merge function **`(existing, replacement) -> existing`** keeps the **first** row seen per `task_id` while iterating — because the stream is ordered newest-first, that row is the **latest** review per task. When Epic 3 stores multiple attempts per task, revisit if product needs “history” instead of “latest only” on this page.

### Constraints (do not regress)

- No new Liquibase changelog for `task_review`.
- **`open-in-view: false`** — `JOIN FETCH` course on task list queries used by templates.
- **`@PreAuthorize` on each secured method** — controller `taskList` and service `findForInternByUsername`.
- Thymeleaf: **`twr.task()`** / **`twr.review()`** (record accessors), not `getTask()`.
- Do **not** add `ReviewStatusBadge`, `InternStatusCard`, or `review-polling.js` here — Epic 3.

### `findForInternByUsername` (reference)

Constructor gains `TaskReviewRepository`; add imports for `TaskReview`, `TaskReviewRepository`, and `Map`. Core logic:

```java
@Transactional(readOnly = true)
@PreAuthorize("hasRole('INTERN') or hasRole('ADMIN')")
public List<TaskWithReview> findForInternByUsername(String username) {
    UserAccount intern = userAccountRepository.findByUsername(username)
        .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
    List<Task> tasks = taskRepository.findAllWithCourseOrderByTaskNameAsc();
    List<TaskReview> reviews = taskReviewRepository
        .findAllByInternIdOrderByDateCreatedDesc(intern.getId());
    Map<Long, TaskReview> latestByTask = reviews.stream()
        .collect(java.util.stream.Collectors.toMap(
            r -> r.getTask().getId(),
            r -> r,
            (existing, replacement) -> existing
        ));
    return tasks.stream()
        .map(t -> new TaskWithReview(t, latestByTask.get(t.getId())))
        .toList();
}
```

**Tests**: `TaskServiceTest` needs `@Mock TaskReviewRepository`; `SecurityIntegrationTest` needs `@MockBean TaskService` and `when(taskService.findForInternByUsername(any())).thenReturn(emptyList())` so `@WebMvcTest` can start.

**Controller**: inject `Authentication`, use `auth.getName()`; null-guard `tasks` for Mockito defaults in web tests.

### Architecture compliance (checklist)

1. No new Liquibase for `task_review`.
2. `@Transactional` only on service methods — not on `@Controller`.
3. `@PreAuthorize` on `taskList` and `findForInternByUsername`.
4. Packages: `com.examinai.review` vs `com.examinai.task` as above.
5. Links in templates: `th:href="@{...}"` for navigational URLs.
6. Fragment: `templates/fragments/task-status-card.html`.
7. Record accessors in `task-list.html` for `TaskWithReview`.
8. JOIN FETCH for tasks that expose `task.course` in the view.

### Previous story intelligence (2.2)

- Seed users `intern` / `intern123`; three tasks under “Spring Boot Fundamentals”.
- `assertOwnership` in task CRUD used mentor username on LAZY paths — same transactional discipline applies to any future service method touching entities in-session.

### Project structure (delivered)

**Created**

- `src/main/java/com/examinai/review/ReviewStatus.java`
- `src/main/java/com/examinai/review/TaskReview.java`
- `src/main/java/com/examinai/review/TaskReviewRepository.java`
- `src/main/java/com/examinai/task/TaskWithReview.java`
- `src/main/resources/templates/fragments/task-status-card.html`

**Modified**

- `src/main/java/com/examinai/task/TaskRepository.java`
- `src/main/java/com/examinai/task/TaskService.java`
- `src/main/java/com/examinai/task/InternTaskController.java`
- `src/main/resources/templates/intern/task-list.html`
- `src/test/java/com/examinai/user/SecurityIntegrationTest.java`
- `src/test/java/com/examinai/task/TaskServiceTest.java`

**Out of scope**

- Liquibase files; `TaskController` (mentor/admin CRUD); `Course*`; `SecurityConfig` except as already configured for `/intern/**`.

### Verification checklist

1. **`mvn clean test`** — full suite green (includes `SecurityIntegrationTest`, `CourseServiceTest`, `TaskServiceTest`).
2. **NFR1 smoke**: `GET /intern/tasks` as authenticated intern — responds well under 1s on LAN (rough check is enough).
3. **Liquibase**: with PostgreSQL, app starts; no new changelog required for this story.
4. **Manual**: `intern`/`intern123` → `/intern/tasks` — card grid, “Not Started” badges, 0% progress, course names visible; **`admin`/`admin123`** — same view; empty DB — muted empty copy; **768px** — `col-6` behavior per UX-DR13; fragment resolves without template errors.

### Review Findings

- [x] [Review][Patch] Thymeleaf nested `${...}` inside outer `${...}` in `th:style` — all status-conditional borders (AC3/4/5/6) broken at render time; fix by moving entire ternary chain inside one `${...}` and removing inner `${...}` wrappers [task-status-card.html:6-14]
- [x] [Review][Patch] `TaskReviewRepository` JPQL query lacks `JOIN FETCH tr.task` — with `open-in-view: false`, accessing `r.getTask().getId()` inside `findForInternByUsername` triggers N lazy SELECTs per review [TaskReviewRepository.java:10]
- [x] [Review][Patch] `toMap` merge function `(existing, replacement) -> existing` is correct only because `findAllByInternIdOrderByDateCreatedDesc` returns newest-first — add a comment documenting this invariant [TaskService.java:117]
- [x] [Review][Patch] Dead null-check `if (tasks == null) tasks = Collections.emptyList()` — `findForInternByUsername` returns `.toList()` which is never null; remove the guard [InternTaskController.java:27]
- [x] [Review][Defer] `assertOwnership()` does not null-check `Authentication` from `SecurityContextHolder` [TaskService.java:126] — deferred, pre-existing
- [x] [Review][Defer] `TaskReview.dateCreated` has `@Column(nullable=false)` but no `@PrePersist` or `insertable=false` — spec-intentional; Epic 3 must always call `setDateCreated()` before save [TaskReview.java:44] — deferred, pre-existing
- [x] [Review][Defer] `TaskServiceTest`: `intern.getId()` is `null`, `any()` matcher hides incorrect argument passed to repository — acceptable per spec-defined test scope [TaskServiceTest.java:138] — deferred, pre-existing

## Change Log

- **2026-04-21** — `validate-create-story` pass: Status and sprint set to **review**; Dev Notes tightened; NFR/UX/deferrals/latest-review semantics documented; Dev Agent Record completed.

## Dev Agent Record

### Agent Model Used

Cursor Agent (Composer)

### Debug Log References

None — documentation-only update (validate-create-story).

### Completion Notes List

- Story file and sprint tracking aligned to **review** after quality validation.
- Dev Notes reduced to token-efficient, repo-accurate guidance; long duplicate source listings removed in favor of paths and one service-method reference.
- Documented epic naming (`findForIntern` vs `findForInternByUsername`), NFR1 smoke, UX-DR14, latest-review `toMap` behavior, and link to `deferred-work.md`.

### File List

- `_bmad-output/implementation-artifacts/2-3-intern-task-list-progress-view.md`
- `_bmad-output/implementation-artifacts/sprint-status.yaml`
