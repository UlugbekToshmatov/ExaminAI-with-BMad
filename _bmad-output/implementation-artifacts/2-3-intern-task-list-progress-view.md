# Story 2.3: Intern Task List & Progress View

Status: ready-for-dev

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

### Critical Context — Read First

- **Spring Boot 3.5.13** (not 3.4.2 from architecture docs) — every prior story confirms this
- **`task_review` table ALREADY EXISTS** in `001-init-schema.sql:30-41` — DO NOT create any new Liquibase changelog
- **Indexes ALREADY EXIST**: `idx_task_review_intern_id` and `idx_task_review_status` in `003-indexes.sql:4-5`, plus `idx_task_review_mentor_id` at `003-indexes.sql:6` — no new index migration needed
- **`open-in-view: false`** is set in `application.yml:11` — accessing LAZY-loaded associations outside a `@Transactional` scope throws `LazyInitializationException`. Use `JOIN FETCH` in `TaskRepository.findAllWithCourseOrderByTaskNameAsc()` for eager Course loading
- **`InternTaskController.java` already exists** as a stub at `com.examinai.task.InternTaskController` — MODIFY it, do not recreate. It has `@RequestMapping("/intern")` at class level
- **`SecurityIntegrationTest` will FAIL** without `@MockBean TaskService` added — `@WebMvcTest` cannot create `TaskService` (non-web bean); context startup fails without the mock
- **`fragments/` directory does NOT exist yet** — you must create it; `task-status-card.html` is the first fragment
- **`th:replace`** with fragment including review-status-badge is in Phase 2 (Epic 3) — do NOT implement `ReviewStatusBadge`, `InternStatusCard`, or `review-polling.js` in this story
- **Package placement**: `ReviewStatus` and `TaskReview` and `TaskReviewRepository` → `com.examinai.review`; `TaskWithReview` → `com.examinai.task`; `InternTaskController` stays in `com.examinai.task`

### `ReviewStatus.java`

```java
package com.examinai.review;

public enum ReviewStatus {
    PENDING,
    LLM_EVALUATED,
    APPROVED,
    REJECTED,
    ERROR
}
```

### `TaskReview.java`

```java
package com.examinai.review;

import com.examinai.task.Task;
import com.examinai.user.UserAccount;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "task_review")
public class TaskReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "intern_id", nullable = false)
    private UserAccount intern;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mentor_id")
    private UserAccount mentor;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ReviewStatus status;

    @Column(name = "llm_result")
    private String llmResult;

    @Column(name = "mentor_result")
    private String mentorResult;

    @Column(name = "mentor_remarks", columnDefinition = "TEXT")
    private String mentorRemarks;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Column(name = "date_created", nullable = false)
    private LocalDateTime dateCreated;

    public TaskReview() {}

    public Long getId() { return id; }
    public Task getTask() { return task; }
    public void setTask(Task task) { this.task = task; }
    public UserAccount getIntern() { return intern; }
    public void setIntern(UserAccount intern) { this.intern = intern; }
    public UserAccount getMentor() { return mentor; }
    public void setMentor(UserAccount mentor) { this.mentor = mentor; }
    public ReviewStatus getStatus() { return status; }
    public void setStatus(ReviewStatus status) { this.status = status; }
    public String getLlmResult() { return llmResult; }
    public void setLlmResult(String llmResult) { this.llmResult = llmResult; }
    public String getMentorResult() { return mentorResult; }
    public void setMentorResult(String mentorResult) { this.mentorResult = mentorResult; }
    public String getMentorRemarks() { return mentorRemarks; }
    public void setMentorRemarks(String mentorRemarks) { this.mentorRemarks = mentorRemarks; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public LocalDateTime getDateCreated() { return dateCreated; }
    public void setDateCreated(LocalDateTime dateCreated) { this.dateCreated = dateCreated; }
}
```

**Why `@ManyToOne` for task/intern/mentor**: Needed for JPQL query `tr.intern.id = :internId` in the repository and for `r.getTask().getId()` in the service. Bare Long fields would require native queries. Pattern matches `Task.java` (`@ManyToOne` for course and mentor).

**Why no `@PrePersist` for `dateCreated`**: Epic 3's `ReviewPipelineService` will explicitly set `dateCreated` at submission time. The DB column defaults to `now()` for safety, but the entity sets it explicitly in the pipeline.

### `TaskReviewRepository.java`

```java
package com.examinai.review;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface TaskReviewRepository extends JpaRepository<TaskReview, Long> {

    @Query("SELECT tr FROM TaskReview tr WHERE tr.intern.id = :internId ORDER BY tr.dateCreated DESC")
    List<TaskReview> findAllByInternIdOrderByDateCreatedDesc(@Param("internId") Long internId);
}
```

### `TaskWithReview.java`

```java
package com.examinai.task;

import com.examinai.review.TaskReview;

public record TaskWithReview(Task task, TaskReview review) {}
```

### Updated `TaskRepository.java`

```java
package com.examinai.task;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findAllByOrderByTaskNameAsc();

    @Query("SELECT t FROM Task t JOIN FETCH t.course ORDER BY t.taskName ASC")
    List<Task> findAllWithCourseOrderByTaskNameAsc();
}
```

### Updated `InternTaskController.java`

```java
package com.examinai.task;

import com.examinai.review.ReviewStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import java.util.Collections;
import java.util.List;

@Controller
@RequestMapping("/intern")
public class InternTaskController {

    private final TaskService taskService;

    public InternTaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping("/tasks")
    @PreAuthorize("hasRole('INTERN') or hasRole('ADMIN')")
    public String taskList(Model model, Authentication auth) {
        List<TaskWithReview> tasks = taskService.findForInternByUsername(auth.getName());
        if (tasks == null) tasks = Collections.emptyList();
        int total = tasks.size();
        long approvedCount = tasks.stream()
            .filter(twr -> twr.review() != null
                && twr.review().getStatus() == ReviewStatus.APPROVED)
            .count();
        int progressPercent = total > 0 ? (int) (approvedCount * 100L / total) : 0;
        model.addAttribute("tasks", tasks);
        model.addAttribute("progressPercent", progressPercent);
        model.addAttribute("total", total);
        return "intern/task-list";
    }
}
```

**Why `auth.getName()` and not `SecurityContextHolder`**: Spring MVC auto-injects `Authentication` as a method parameter — cleaner than static holder access. The `auth.getName()` returns the authenticated username from `CustomUserDetailsService`.

**Why null-check on `tasks`**: `@MockBean TaskService` in `SecurityIntegrationTest` returns null by default from Mockito. The null-check prevents NPE in the test when model attributes are computed.

### `TaskService.java` additions

Add `TaskReviewRepository` injection and the new method. The existing constructor:

```java
public TaskService(TaskRepository taskRepository,
                   CourseRepository courseRepository,
                   UserAccountRepository userAccountRepository) {
```

Becomes:

```java
private final TaskReviewRepository taskReviewRepository;

public TaskService(TaskRepository taskRepository,
                   CourseRepository courseRepository,
                   UserAccountRepository userAccountRepository,
                   TaskReviewRepository taskReviewRepository) {
    this.taskRepository = taskRepository;
    this.courseRepository = courseRepository;
    this.userAccountRepository = userAccountRepository;
    this.taskReviewRepository = taskReviewRepository;
}
```

Add the new method (with full imports):

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

Add imports at top of file:
```java
import com.examinai.review.TaskReview;
import com.examinai.review.TaskReviewRepository;
import java.util.Map;
```

**CRITICAL — `TaskServiceTest` impact**: Adding `TaskReviewRepository` to the constructor means `@InjectMocks` in `TaskServiceTest` will try to inject it. Add `@Mock TaskReviewRepository taskReviewRepository;` to `TaskServiceTest` or all existing tests will fail at context creation (Mockito `@InjectMocks` matches by type — the extra mock just gets injected automatically).

### `SecurityIntegrationTest.java` changes

```java
// Add alongside existing @MockBean fields:
@MockBean
TaskService taskService;

// Add import:
import com.examinai.task.TaskService;

// Add @BeforeEach:
@BeforeEach
void setUp() {
    when(taskService.findForInternByUsername(any())).thenReturn(java.util.Collections.emptyList());
}

// Add imports:
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.BeforeEach;
```

**Why this is required**: `@WebMvcTest` loads the Spring MVC layer only. Non-web beans (like `TaskService`) are not auto-created. Since `InternTaskController` requires `TaskService` in its constructor, the application context will fail to start without a `@MockBean` for it. This is a mandatory change to keep the existing security tests green.

### Architecture Compliance Rules

1. **NO new Liquibase changelog** — `task_review` table and all its indexes already exist in `001-init-schema.sql` and `003-indexes.sql`
2. **`@Transactional` on service methods only** — `InternTaskController` MUST NOT have `@Transactional`
3. **`@PreAuthorize` on every method individually** — `taskList()` in controller AND `findForInternByUsername()` in service
4. **Package-by-feature** — `ReviewStatus`, `TaskReview`, `TaskReviewRepository` → `com.examinai.review`; `TaskWithReview` → `com.examinai.task`
5. **Template URLs via `th:href="@{...}"`** — Team Agreement from Epic 1 retro
6. **Fragment path** → `templates/fragments/task-status-card.html` (architecture spec: `templates/fragments/` directory)
7. **Record accessor syntax** → `twr.task()` and `twr.review()`, NOT `twr.getTask()` — Java records don't generate `get` prefixed methods
8. **`open-in-view: false`** → JOIN FETCH in `findAllWithCourseOrderByTaskNameAsc()` is mandatory

### Previous Story Intelligence (Story 2.2 Learnings)

1. **Spring Boot 3.5.13** (not 3.4.2) — confirmed in every prior story dev note
2. **`open-in-view: false`** — confirmed in `application.yml:11`; LAZY access in templates throws `LazyInitializationException`. This story's `findAllWithCourseOrderByTaskNameAsc()` with `JOIN FETCH t.course` prevents the issue.
3. **No new changelog** — same situation as Story 2.2 (`task` table) and Story 2.1 (`course` table): the `task_review` table is pre-existing in `001-init-schema.sql`
4. **`@PreAuthorize` on every individual method** — do not use class-level only; confirmed working from all prior stories
5. **`th:href="@{...}"` always** — Team Agreement from Epic 1 retro; any absolute path causes regressions
6. **Seed data available**: `intern`/`intern123` login exists; 3 tasks in "Spring Boot Fundamentals" course pre-loaded for manual testing
7. **Review finding from 2.2**: `assertOwnership()` reads `task.getMentor().getUsername()` — if LAZY and outside transaction, it crashes. Same risk applies here; `findForInternByUsername` uses `@Transactional(readOnly=true)` to keep session open.

### Git Context

Recent commits:
- `db6e229` — Story 2.2 complete (Task entity, service, controller, templates, tests; ownership check patch applied)
- `2bba320` — Story 2.1 complete (Course entity, service, controller, templates, tests)
- `4dc3fb5` — Epic 1 all stories complete

Story 2.3 is the third and final story in Epic 2. After this, run `epic-2-retrospective`.

### Project Structure

**Files to CREATE:**
```
src/main/java/com/examinai/review/
├── ReviewStatus.java              ← NEW enum
├── TaskReview.java                ← NEW entity (table already exists)
└── TaskReviewRepository.java      ← NEW repository

src/main/java/com/examinai/task/
└── TaskWithReview.java            ← NEW record DTO

src/main/resources/templates/fragments/
└── task-status-card.html          ← NEW fragment (create fragments/ directory)
```

**Files to MODIFY:**
```
src/main/java/com/examinai/task/
├── TaskRepository.java            ← Add findAllWithCourseOrderByTaskNameAsc()
├── TaskService.java               ← Add TaskReviewRepository injection + findForInternByUsername()
└── InternTaskController.java      ← Flesh out stub (add TaskService injection + model logic)

src/main/resources/templates/intern/
└── task-list.html                 ← Replace stub with card-grid + progress bar

src/test/java/com/examinai/user/
└── SecurityIntegrationTest.java   ← Add @MockBean TaskService + @BeforeEach stub

src/test/java/com/examinai/task/
└── TaskServiceTest.java           ← Add @Mock TaskReviewRepository + findForInternByUsername test
```

**Do NOT touch:**
- Any Liquibase changelog files — `task_review` and all indexes already exist
- `TaskController.java` — handles mentor/admin task CRUD only; intern tasks are `InternTaskController`'s responsibility
- `CourseService.java`, `CourseController.java` — not involved
- `SecurityConfig.java` — existing `/intern/**` pattern already covers this story

### Verification Checklist

After completing all tasks:
1. `mvn clean compile` — BUILD SUCCESS, 0 errors
2. `mvn test` — `SecurityIntegrationTest` (10 tests), `CourseServiceTest` (4 tests), `TaskServiceTest` (5 tests — 4 existing + 1 new) all pass
3. With local PostgreSQL running: app starts cleanly, Liquibase reports 4 changelogs applied (no new changeset)
4. Manual: login as `intern`/`intern123` → navigate to `/intern/tasks` → see 3 seed task cards in a card-grid layout
5. Manual: each card shows grey left border ("Not Started") since no reviews exist yet
6. Manual: progress bar shows "0%" since no tasks are APPROVED
7. Manual: card shows task name and course name ("Spring Boot Fundamentals") — LAZY loading does NOT throw
8. Manual: navigate to `/intern/tasks` — task cards adapt to `col-6` at tablet width (768px)
9. Manual: login as `admin`/`admin123` → navigate to `/intern/tasks` → same view (admin inherits intern)
10. Manual: confirm `fragments/task-status-card.html` is included successfully (no Thymeleaf template resolution error)

## Dev Agent Record

### Agent Model Used

{{agent_model_name_version}}

### Debug Log References

### Completion Notes List

### File List
