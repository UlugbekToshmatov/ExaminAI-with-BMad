# Story 2.2: Task Management

Status: done

## Story

As a mentor or admin,
I want to create, view, edit, and delete Tasks within a Course and assign them to an owning mentor,
So that interns have structured assignments with clear descriptions and ownership.

## Acceptance Criteria

**AC1 — Task list:**
Given a mentor or admin is on `/mentor/tasks` or `/admin/tasks`
When the page loads
Then all tasks are listed with task name, associated course, owning mentor, and creation date

**AC2 — Create task (PRG):**
Given a mentor or admin fills out the task creation form (task name, description, course selection, mentor assignment) and submits
When the form posts
Then a new `Task` is saved with FK references to `Course` (course_id) and `UserAccount` (mentor_id), and they are redirected to the task list (PRG pattern)

**AC3 — Edit task (PRG):**
Given a task exists and the mentor/admin clicks Edit, updates fields, and saves
When the form posts
Then the task is updated and they are redirected to the task list

**AC4 — Delete task:**
Given a mentor or admin clicks Delete on a task
When the deletion is confirmed
Then the task is removed from the system

**AC5 — Cascade delete:**
Given a `Task` that has one or more associated `TaskReview` records is deleted
When the deletion is confirmed
Then all `TaskReview` rows referencing that `Task` via `task_id` FK are also deleted (cascade), and no FK constraint violation is raised

**AC6 — Dropdowns populated:**
Given the task create/edit form renders
When a mentor or admin opens it
Then a dropdown lists all available Courses and a second dropdown lists all accounts with role MENTOR

**AC7 — Task table already migrated:**
Given Liquibase changelog `001-init-schema.sql` already defines the `task` table (id BIGSERIAL PK, task_name VARCHAR NOT NULL, task_description TEXT, course_id BIGINT NOT NULL FK → course.id, mentor_id BIGINT NOT NULL FK → user_account.id, date_created TIMESTAMP NOT NULL)
When the application starts
Then the table is already created — NO new changelog is needed for this story

**AC8 — @PreAuthorize enforcement:**
Given `@PreAuthorize("hasRole('MENTOR') or hasRole('ADMIN')")` is on `TaskService` CRUD methods
When an intern attempts to call task management operations
Then a 403 is returned

## Tasks / Subtasks

- [x] Task 1: Create Task entity (AC: 1, 2, 3, 4, 5, 7)
  - [x] Create `src/main/java/com/examinai/task/Task.java`
  - [x] Annotate: `@Entity`, `@Table(name = "task")`
  - [x] Fields: `id` (Long, `@Id @GeneratedValue(strategy = GenerationType.IDENTITY)`), `taskName` (`@Column(name = "task_name", nullable = false)`), `taskDescription` (`@Column(name = "task_description", columnDefinition = "TEXT")`), `dateCreated` (`@Column(name = "date_created", nullable = false)`)
  - [x] `@ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "course_id", nullable = false)` for `Course course`
  - [x] `@ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "mentor_id", nullable = false)` for `UserAccount mentor`
  - [x] Add `@PrePersist` method setting `dateCreated = LocalDateTime.now()`
  - [x] No JPA cascade annotations — DB has `ON DELETE CASCADE` on `task_review.task_id` (and `task_review_issue.task_review_id`)
  - [x] Include no-arg constructor + all getters + setters

- [x] Task 2: Create TaskRepository (AC: 1, 2, 3, 4)
  - [x] Create `src/main/java/com/examinai/task/TaskRepository.java`
  - [x] Extends `JpaRepository<Task, Long>`
  - [x] Add: `List<Task> findAllByOrderByTaskNameAsc();` for consistent ordering

- [x] Task 3: Create TaskCreateDto (AC: 2, 3, 6)
  - [x] Create `src/main/java/com/examinai/task/TaskCreateDto.java`
  - [x] Fields: `taskName` (String, `@NotBlank(message = "Task name is required")`), `taskDescription` (String, no constraint — optional), `courseId` (Long, `@NotNull(message = "Course is required")`), `mentorId` (Long, `@NotNull(message = "Mentor is required")`)
  - [x] No-arg constructor + getters + setters — NOT a record (Thymeleaf `th:object` binding requirement)

- [x] Task 4: Create TaskService (AC: 1, 2, 3, 4, 5, 6, 8)
  - [x] Create `src/main/java/com/examinai/task/TaskService.java`
  - [x] Annotate with `@Service`
  - [x] Inject `TaskRepository`, `CourseRepository`, `UserAccountRepository` via constructor
  - [x] Implement `findAll()`: returns `taskRepository.findAllByOrderByTaskNameAsc()`
  - [x] Implement `findById(Long id)`: `taskRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Task not found: " + id))`
  - [x] Implement `findAllCourses()`: returns `courseRepository.findAllByOrderByCourseNameAsc()`
  - [x] Implement `findAllMentors()`: returns `userAccountRepository.findAllByRole(Role.MENTOR)`
  - [x] Implement `@Transactional create(TaskCreateDto dto)`: look up Course by courseId, look up UserAccount by mentorId, set task fields, save, return
  - [x] Implement `@Transactional update(Long id, TaskCreateDto dto)`: findById, update taskName + taskDescription + course + mentor, save, return
  - [x] Implement `@Transactional delete(Long id)`: findById, call `taskRepository.delete(task)`
  - [x] `@PreAuthorize("hasRole('MENTOR') or hasRole('ADMIN')")` on EVERY method individually (not class-level)
  - [x] `@Transactional` on `create`, `update`, `delete` only — NOT on `findAll`, `findById`, `findAllCourses`, `findAllMentors`

- [x] Task 5: Create TaskController (AC: 1, 2, 3, 4, 5, 6, 8)
  - [x] Create `src/main/java/com/examinai/task/TaskController.java`
  - [x] Annotate with `@Controller`
  - [x] Inject `TaskService` via constructor
  - [x] `GET /mentor/tasks` and `GET /admin/tasks` → load all tasks, add to model as `"tasks"`, return `"admin/task-list"`
  - [x] `GET /mentor/tasks/new` (and `/admin/tasks/new`) → add empty `TaskCreateDto` as `"taskDto"`, add `taskService.findAllCourses()` as `"courses"`, add `taskService.findAllMentors()` as `"mentors"`, return `"admin/task-form"`
  - [x] `POST /mentor/tasks` and `POST /admin/tasks` → `@Valid` + `BindingResult` → if `bindingResult.hasErrors()` re-add `"courses"` and `"mentors"` to model, return `"admin/task-form"` directly → else `taskService.create(dto)` → `"redirect:/mentor/tasks"`
  - [x] `GET /mentor/tasks/{id}/edit` (and `/admin/tasks/{id}/edit`) → wrap in try/catch `IllegalArgumentException` → on error add flash message via `RedirectAttributes` and `"redirect:/mentor/tasks"` → on success: populate dto, add `"taskId"` + `"courses"` + `"mentors"` to model, return `"admin/task-form"`
  - [x] `POST /mentor/tasks/{id}/edit` (and `/admin/tasks/{id}/edit`) → `@Valid` + `BindingResult` → re-add model attrs on error → wrap service call in try/catch `IllegalArgumentException` → `"redirect:/mentor/tasks"`
  - [x] `POST /mentor/tasks/{id}/delete` (and `/admin/tasks/{id}/delete`) → wrap in try/catch `IllegalArgumentException` → `"redirect:/mentor/tasks"` (with or without flash error)
  - [x] `@PreAuthorize("hasRole('MENTOR') or hasRole('ADMIN')")` on EVERY method individually
  - [x] Do NOT add `@RequestMapping` at class level (use full paths per method, matching CourseController pattern)
  - [x] **CRITICAL**: `InternTaskController.java` already exists in `com.examinai.task` — `TaskController.java` is a SEPARATE new class; do NOT modify `InternTaskController.java`

- [x] Task 6: Create `admin/task-list.html` template (AC: 1, 4, 5, 8)
  - [x] Create `src/main/resources/templates/admin/task-list.html`
  - [x] Extends base layout: `layout:decorate="~{layout/base}"`
  - [x] `<main id="main-content" layout:fragment="content">`
  - [x] Heading: `<h4>Tasks</h4>` + "Create Task" button `th:href="@{/mentor/tasks/new}"`
  - [x] Table (`table table-hover`) with columns: Task Name, Course, Mentor, Created, Actions
  - [x] All column headers: `<th scope="col">`
  - [x] Row data: `${task.taskName}`, `${task.course.courseName}`, `${task.mentor.username}`, `#temporals.format(task.dateCreated, 'dd MMM yyyy HH:mm')`
  - [x] Edit link: `th:href="@{/mentor/tasks/{id}/edit(id=${task.id})}"`
  - [x] Delete: `<form th:action="@{/mentor/tasks/{id}/delete(id=${task.id})}" method="post">` with CSRF token + confirm dialog
  - [x] Empty state: `<p class="text-muted">No tasks found.</p>` when list is empty
  - [x] Wrap table in `<div class="table-responsive">`
  - [x] ALL URLs use `th:href="@{...}"` — NO absolute href paths

- [x] Task 7: Create `admin/task-form.html` template (AC: 2, 3, 6)
  - [x] Create `src/main/resources/templates/admin/task-form.html`
  - [x] Extends base layout
  - [x] Heading: "Create Task" or "Edit Task" using `th:text="${taskId != null} ? 'Edit Task' : 'Create Task'"`
  - [x] Separate form blocks for create vs edit (using `th:if="${taskId != null}"` / `th:unless`) — matching course-form.html pattern
  - [x] Create form action: `th:action="@{/mentor/tasks}"`; Edit form action: `th:action="@{/mentor/tasks/{id}/edit(id=${taskId})}"`
  - [x] Both forms: `th:object="${taskDto}" method="post" class="col-md-6"`
  - [x] CSRF: `<input type="hidden" th:name="${_csrf.parameterName}" th:value="${_csrf.token}">`
  - [x] taskName: text input, `th:field="*{taskName}"`, `<label for="taskName">`, `is-invalid` on error, `invalid-feedback` div
  - [x] taskDescription: textarea, `th:field="*{taskDescription}"`, `<label for="taskDescription">`, rows="4"
  - [x] courseId: `<select>` with `th:field="*{courseId}"`, `<label for="courseId">`, options via `th:each="course : ${courses}"` with `th:value="${course.id}"` and `th:text="${course.courseName}"`, `is-invalid` on error
  - [x] mentorId: `<select>` with `th:field="*{mentorId}"`, `<label for="mentorId">`, options via `th:each="mentor : ${mentors}"` with `th:value="${mentor.id}"` and `th:text="${mentor.username}"`, `is-invalid` on error
  - [x] Submit button + "Back to Tasks" link: `th:href="@{/mentor/tasks}"`
  - [x] ALL URLs use `th:href="@{...}"`

- [x] Task 8: Verify SecurityConfig coverage (AC: 8)
  - [x] Open `src/main/java/com/examinai/config/SecurityConfig.java`
  - [x] Confirm `/mentor/**` is configured for MENTOR or ADMIN role — no changes expected
  - [x] Confirm `/admin/**` is configured for ADMIN role — no changes expected
  - [x] No modification needed

- [x] Task 9: Write TaskServiceTest (AC: 2, 3, 4, 8)
  - [x] Create `src/test/java/com/examinai/task/TaskServiceTest.java`
  - [x] Use `@ExtendWith(MockitoExtension.class)` with `@Mock TaskRepository`, `@Mock CourseRepository`, `@Mock UserAccountRepository`
  - [x] Test: `create` → courseRepository.findById called with courseId, userAccountRepository.findById called with mentorId, task saved with correct fields
  - [x] Test: `update` → task found, taskName + course + mentor updated, saved
  - [x] Test: `delete` → task found, `taskRepository.delete(task)` called
  - [x] Test: `findById` with non-existent id → `IllegalArgumentException` thrown
  - [x] Note: `@PreAuthorize` NOT tested here (Mockito context has no Spring Security AOP proxy); security covered by existing `SecurityIntegrationTest`

## Dev Notes

### Critical Context — Read First

- **Spring Boot 3.5.13** is in use (NOT 3.4.2 from architecture docs). Every story dev note confirms this.
- **`task` table already exists** in `001-init-schema.sql:21-28` — do NOT create any new Liquibase changelog for this story.
- **DB cascade already wired**: `task_review.task_id` has `ON DELETE CASCADE` at DB level, and `task_review_issue.task_review_id` also cascades. Deleting a Task via `taskRepository.delete(task)` cascades through task_review to task_review_issue automatically. No JPA cascade annotations needed on `Task.java`.
- **`UserAccountRepository.findAllByRole(Role.MENTOR)` already exists** (`UserAccountRepository.java:9`) — inject `UserAccountRepository` directly into `TaskService`. Do NOT create a duplicate method anywhere.
- **`CourseRepository.findAllByOrderByCourseNameAsc()` already exists** — inject `CourseRepository` directly in `TaskService`. Do NOT call `CourseService` from `TaskService` to avoid service-to-service coupling issues.
- **`InternTaskController.java` already exists** at `com.examinai.task.InternTaskController` — it handles `/intern/tasks` and returns `"intern/task-list"`. It is a stub for Story 2.3. Do NOT modify it. Create `TaskController.java` as a SEPARATE new class in the same `com.examinai.task` package — no conflict since they are different class names with different URL mappings.
- **Package-by-feature**: `Task`, `TaskRepository`, `TaskCreateDto`, `TaskService`, `TaskController` all go in `com.examinai.task`. Do NOT put `TaskController` in `com.examinai.admin`.
- **Absolute href prohibition** (Team Agreement from Epic 1 retro): ALL template URLs MUST use `th:href="@{...}"` — never bare `/admin/...` or `/mentor/...` paths. This caused regressions in Epic 1 and is a hard Team Agreement.
- **Validation subtasks are mandatory** (Process Improvement from Epic 1 retro): `@NotBlank` on `taskName`, `@NotNull` on `courseId` and `mentorId`, `@Valid` + `BindingResult` in all POST controller handlers.
- **Re-add model attributes on validation error**: Unlike simple forms, the task form needs `"courses"` and `"mentors"` in the model even on re-render after validation errors. The controller MUST re-add them before returning `"admin/task-form"` on error — otherwise the dropdowns will be empty and Thymeleaf will throw an exception.
- **Seed data**: 3 tasks already exist (Build a REST API, Implement Spring Security, Write Unit Tests) assigned to 'mentor' user in 'Spring Boot Fundamentals' course. Use for manual verification.

### Task.java Entity

```java
package com.examinai.task;

import com.examinai.course.Course;
import com.examinai.user.UserAccount;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "task")
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_name", nullable = false)
    private String taskName;

    @Column(name = "task_description", columnDefinition = "TEXT")
    private String taskDescription;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mentor_id", nullable = false)
    private UserAccount mentor;

    @Column(name = "date_created", nullable = false)
    private LocalDateTime dateCreated;

    @PrePersist
    private void prePersist() {
        this.dateCreated = LocalDateTime.now();
    }

    public Task() {}

    public Long getId() { return id; }
    public String getTaskName() { return taskName; }
    public void setTaskName(String taskName) { this.taskName = taskName; }
    public String getTaskDescription() { return taskDescription; }
    public void setTaskDescription(String taskDescription) { this.taskDescription = taskDescription; }
    public Course getCourse() { return course; }
    public void setCourse(Course course) { this.course = course; }
    public UserAccount getMentor() { return mentor; }
    public void setMentor(UserAccount mentor) { this.mentor = mentor; }
    public LocalDateTime getDateCreated() { return dateCreated; }
}
```

**Why @ManyToOne instead of bare Long IDs**: The task list template needs `task.course.courseName` and `task.mentor.username` for display. Using `@ManyToOne(fetch = FetchType.LAZY)` with `@JoinColumn` keeps the DB schema identical (same `course_id` and `mentor_id` columns) while allowing Thymeleaf direct property access. Architecture lists `courseId`/`mentorId` conceptually — JPA associations are the correct implementation.

**Why no JPA cascade**: `task_review.task_id` has `ON DELETE CASCADE` in PostgreSQL (`001-init-schema.sql:25`). JPA `CascadeType.REMOVE` would trigger N+1 deletes (loads every TaskReview then deletes individually). DB cascade is faster and sufficient.

### TaskCreateDto.java

```java
package com.examinai.task;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class TaskCreateDto {

    @NotBlank(message = "Task name is required")
    @Size(max = 255)
    private String taskName;

    private String taskDescription;

    @NotNull(message = "Course is required")
    private Long courseId;

    @NotNull(message = "Mentor is required")
    private Long mentorId;

    public TaskCreateDto() {}

    public String getTaskName() { return taskName; }
    public void setTaskName(String taskName) { this.taskName = taskName; }
    public String getTaskDescription() { return taskDescription; }
    public void setTaskDescription(String taskDescription) { this.taskDescription = taskDescription; }
    public Long getCourseId() { return courseId; }
    public void setCourseId(Long courseId) { this.courseId = courseId; }
    public Long getMentorId() { return mentorId; }
    public void setMentorId(Long mentorId) { this.mentorId = mentorId; }
}
```

**Why @NotNull on courseId/mentorId**: An unselected dropdown in HTML submits an empty string or no value, which would cause a NullPointerException when looking up by ID. `@NotNull` catches this before the service is called.

### TaskService.java

```java
package com.examinai.task;

import com.examinai.course.Course;
import com.examinai.course.CourseRepository;
import com.examinai.user.Role;
import com.examinai.user.UserAccount;
import com.examinai.user.UserAccountRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final CourseRepository courseRepository;
    private final UserAccountRepository userAccountRepository;

    public TaskService(TaskRepository taskRepository,
                       CourseRepository courseRepository,
                       UserAccountRepository userAccountRepository) {
        this.taskRepository = taskRepository;
        this.courseRepository = courseRepository;
        this.userAccountRepository = userAccountRepository;
    }

    @PreAuthorize("hasRole('MENTOR') or hasRole('ADMIN')")
    public List<Task> findAll() {
        return taskRepository.findAllByOrderByTaskNameAsc();
    }

    @PreAuthorize("hasRole('MENTOR') or hasRole('ADMIN')")
    public Task findById(Long id) {
        return taskRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Task not found: " + id));
    }

    @PreAuthorize("hasRole('MENTOR') or hasRole('ADMIN')")
    public List<Course> findAllCourses() {
        return courseRepository.findAllByOrderByCourseNameAsc();
    }

    @PreAuthorize("hasRole('MENTOR') or hasRole('ADMIN')")
    public List<UserAccount> findAllMentors() {
        return userAccountRepository.findAllByRole(Role.MENTOR);
    }

    @Transactional
    @PreAuthorize("hasRole('MENTOR') or hasRole('ADMIN')")
    public Task create(TaskCreateDto dto) {
        Course course = courseRepository.findById(dto.getCourseId())
            .orElseThrow(() -> new IllegalArgumentException("Course not found: " + dto.getCourseId()));
        UserAccount mentor = userAccountRepository.findById(dto.getMentorId())
            .orElseThrow(() -> new IllegalArgumentException("Mentor not found: " + dto.getMentorId()));
        Task task = new Task();
        task.setTaskName(dto.getTaskName());
        task.setTaskDescription(dto.getTaskDescription());
        task.setCourse(course);
        task.setMentor(mentor);
        return taskRepository.save(task);
    }

    @Transactional
    @PreAuthorize("hasRole('MENTOR') or hasRole('ADMIN')")
    public Task update(Long id, TaskCreateDto dto) {
        Task task = findById(id);
        Course course = courseRepository.findById(dto.getCourseId())
            .orElseThrow(() -> new IllegalArgumentException("Course not found: " + dto.getCourseId()));
        UserAccount mentor = userAccountRepository.findById(dto.getMentorId())
            .orElseThrow(() -> new IllegalArgumentException("Mentor not found: " + dto.getMentorId()));
        task.setTaskName(dto.getTaskName());
        task.setTaskDescription(dto.getTaskDescription());
        task.setCourse(course);
        task.setMentor(mentor);
        return taskRepository.save(task);
    }

    @Transactional
    @PreAuthorize("hasRole('MENTOR') or hasRole('ADMIN')")
    public void delete(Long id) {
        Task task = findById(id);
        taskRepository.delete(task);
    }
}
```

**Critical rules:**
- `@Transactional` on `create`, `update`, `delete` only — NOT on reads
- `@PreAuthorize` on EVERY method individually — class-level alone prohibited
- NEVER add `@Transactional` to `TaskController`
- Inject `CourseRepository` and `UserAccountRepository` directly — NOT `CourseService` (avoids service-to-service coupling)

### TaskController.java

```java
package com.examinai.task;

import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping({"/mentor/tasks", "/admin/tasks"})
    @PreAuthorize("hasRole('MENTOR') or hasRole('ADMIN')")
    public String list(Model model) {
        model.addAttribute("tasks", taskService.findAll());
        return "admin/task-list";
    }

    @GetMapping({"/mentor/tasks/new", "/admin/tasks/new"})
    @PreAuthorize("hasRole('MENTOR') or hasRole('ADMIN')")
    public String newForm(Model model) {
        model.addAttribute("taskDto", new TaskCreateDto());
        model.addAttribute("courses", taskService.findAllCourses());
        model.addAttribute("mentors", taskService.findAllMentors());
        return "admin/task-form";
    }

    @PostMapping({"/mentor/tasks", "/admin/tasks"})
    @PreAuthorize("hasRole('MENTOR') or hasRole('ADMIN')")
    public String create(@Valid @ModelAttribute("taskDto") TaskCreateDto dto,
                         BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("courses", taskService.findAllCourses());
            model.addAttribute("mentors", taskService.findAllMentors());
            return "admin/task-form";
        }
        taskService.create(dto);
        return "redirect:/mentor/tasks";
    }

    @GetMapping({"/mentor/tasks/{id}/edit", "/admin/tasks/{id}/edit"})
    @PreAuthorize("hasRole('MENTOR') or hasRole('ADMIN')")
    public String editForm(@PathVariable Long id, Model model, RedirectAttributes ra) {
        try {
            Task task = taskService.findById(id);
            TaskCreateDto dto = new TaskCreateDto();
            dto.setTaskName(task.getTaskName());
            dto.setTaskDescription(task.getTaskDescription());
            dto.setCourseId(task.getCourse().getId());
            dto.setMentorId(task.getMentor().getId());
            model.addAttribute("taskDto", dto);
            model.addAttribute("taskId", id);
            model.addAttribute("courses", taskService.findAllCourses());
            model.addAttribute("mentors", taskService.findAllMentors());
            return "admin/task-form";
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/mentor/tasks";
        }
    }

    @PostMapping({"/mentor/tasks/{id}/edit", "/admin/tasks/{id}/edit"})
    @PreAuthorize("hasRole('MENTOR') or hasRole('ADMIN')")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("taskDto") TaskCreateDto dto,
                         BindingResult bindingResult, Model model, RedirectAttributes ra) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("taskId", id);
            model.addAttribute("courses", taskService.findAllCourses());
            model.addAttribute("mentors", taskService.findAllMentors());
            return "admin/task-form";
        }
        try {
            taskService.update(id, dto);
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/mentor/tasks";
    }

    @PostMapping({"/mentor/tasks/{id}/delete", "/admin/tasks/{id}/delete"})
    @PreAuthorize("hasRole('MENTOR') or hasRole('ADMIN')")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        try {
            taskService.delete(id);
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/mentor/tasks";
    }
}
```

**Why redirect to `/mentor/tasks`**: Admin inherits all Mentor capabilities (FR5). `/mentor/tasks` is accessible by MENTOR or ADMIN. Same redirect strategy as `CourseController`.

**Why no class-level `@RequestMapping`**: Consistent with `CourseController` pattern — full paths on each method. Avoids URL prefix ambiguity with `/intern/tasks` which is handled by `InternTaskController`.

**CRITICAL: `InternTaskController` coexists**: `InternTaskController` is in the same package and handles `GET /intern/tasks`. `TaskController` handles `/mentor/tasks/**` and `/admin/tasks/**`. No Spring MVC conflict — they are separate beans with non-overlapping URL patterns.

### admin/task-list.html

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org"
      xmlns:layout="http://www.ultraq.net.nz/thymeleaf/layout"
      layout:decorate="~{layout/base}">
<head><title>Task Management</title></head>
<body>
<main id="main-content" layout:fragment="content">
    <div class="d-flex justify-content-between align-items-center mb-3">
        <h4>Tasks</h4>
        <a th:href="@{/mentor/tasks/new}" class="btn btn-primary btn-sm">Create Task</a>
    </div>

    <div th:if="${#lists.isEmpty(tasks)}" class="text-muted">
        <p>No tasks found.</p>
    </div>

    <div th:unless="${#lists.isEmpty(tasks)}" class="table-responsive">
        <table class="table table-hover">
            <thead>
                <tr>
                    <th scope="col">Task Name</th>
                    <th scope="col">Course</th>
                    <th scope="col">Mentor</th>
                    <th scope="col">Created</th>
                    <th scope="col">Actions</th>
                </tr>
            </thead>
            <tbody>
                <tr th:each="task : ${tasks}">
                    <td th:text="${task.taskName}"></td>
                    <td th:text="${task.course.courseName}"></td>
                    <td th:text="${task.mentor.username}"></td>
                    <td th:text="${#temporals.format(task.dateCreated, 'dd MMM yyyy HH:mm')}"></td>
                    <td>
                        <a th:href="@{/mentor/tasks/{id}/edit(id=${task.id})}"
                           class="btn btn-sm btn-outline-secondary me-1">Edit</a>
                        <form th:action="@{/mentor/tasks/{id}/delete(id=${task.id})}"
                              method="post" class="d-inline">
                            <input type="hidden" th:name="${_csrf.parameterName}" th:value="${_csrf.token}">
                            <button type="submit" class="btn btn-sm btn-outline-danger"
                                    onclick="return confirm('Delete this task and all its reviews?')">Delete</button>
                        </form>
                    </td>
                </tr>
            </tbody>
        </table>
    </div>
</main>
</body>
</html>
```

**LAZY loading in Thymeleaf**: `task.course.courseName` and `task.mentor.username` trigger LAZY loading during template rendering. This is fine within the open Hibernate session (default Spring MVC behavior with `spring.jpa.open-in-view=true`). If `open-in-view` is disabled, use `@EntityGraph` or JOIN FETCH in the repository. Check `application.yml` — default Spring Boot has open-in-view enabled. Do NOT change this setting.

### admin/task-form.html

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org"
      xmlns:layout="http://www.ultraq.net.nz/thymeleaf/layout"
      layout:decorate="~{layout/base}">
<head><title>Task Form</title></head>
<body>
<main id="main-content" layout:fragment="content">
    <h4 th:text="${taskId != null} ? 'Edit Task' : 'Create Task'">Task</h4>

    <!-- EDIT form -->
    <form th:if="${taskId != null}"
          th:action="@{/mentor/tasks/{id}/edit(id=${taskId})}"
          th:object="${taskDto}" method="post" class="col-md-6">
        <input type="hidden" th:name="${_csrf.parameterName}" th:value="${_csrf.token}">
        <div class="mb-3">
            <label for="taskName" class="form-label">Task Name</label>
            <input type="text" class="form-control" id="taskName" th:field="*{taskName}" required
                   th:classappend="${#fields.hasErrors('taskName')} ? 'is-invalid'">
            <div th:if="${#fields.hasErrors('taskName')}" class="invalid-feedback"
                 th:errors="*{taskName}"></div>
        </div>
        <div class="mb-3">
            <label for="taskDescription" class="form-label">Description</label>
            <textarea class="form-control" id="taskDescription" th:field="*{taskDescription}" rows="4"></textarea>
        </div>
        <div class="mb-3">
            <label for="courseId" class="form-label">Course</label>
            <select class="form-select" id="courseId" th:field="*{courseId}"
                    th:classappend="${#fields.hasErrors('courseId')} ? 'is-invalid'">
                <option value="">-- Select Course --</option>
                <option th:each="course : ${courses}"
                        th:value="${course.id}"
                        th:text="${course.courseName}"></option>
            </select>
            <div th:if="${#fields.hasErrors('courseId')}" class="invalid-feedback"
                 th:errors="*{courseId}"></div>
        </div>
        <div class="mb-3">
            <label for="mentorId" class="form-label">Mentor</label>
            <select class="form-select" id="mentorId" th:field="*{mentorId}"
                    th:classappend="${#fields.hasErrors('mentorId')} ? 'is-invalid'">
                <option value="">-- Select Mentor --</option>
                <option th:each="mentor : ${mentors}"
                        th:value="${mentor.id}"
                        th:text="${mentor.username}"></option>
            </select>
            <div th:if="${#fields.hasErrors('mentorId')}" class="invalid-feedback"
                 th:errors="*{mentorId}"></div>
        </div>
        <button type="submit" class="btn btn-primary">Save Changes</button>
        <a th:href="@{/mentor/tasks}" class="btn btn-link">Back to Tasks</a>
    </form>

    <!-- CREATE form -->
    <form th:unless="${taskId != null}"
          th:action="@{/mentor/tasks}"
          th:object="${taskDto}" method="post" class="col-md-6">
        <input type="hidden" th:name="${_csrf.parameterName}" th:value="${_csrf.token}">
        <div class="mb-3">
            <label for="taskName" class="form-label">Task Name</label>
            <input type="text" class="form-control" id="taskName" th:field="*{taskName}" required
                   th:classappend="${#fields.hasErrors('taskName')} ? 'is-invalid'">
            <div th:if="${#fields.hasErrors('taskName')}" class="invalid-feedback"
                 th:errors="*{taskName}"></div>
        </div>
        <div class="mb-3">
            <label for="taskDescription" class="form-label">Description</label>
            <textarea class="form-control" id="taskDescription" th:field="*{taskDescription}" rows="4"></textarea>
        </div>
        <div class="mb-3">
            <label for="courseId" class="form-label">Course</label>
            <select class="form-select" id="courseId" th:field="*{courseId}"
                    th:classappend="${#fields.hasErrors('courseId')} ? 'is-invalid'">
                <option value="">-- Select Course --</option>
                <option th:each="course : ${courses}"
                        th:value="${course.id}"
                        th:text="${course.courseName}"></option>
            </select>
            <div th:if="${#fields.hasErrors('courseId')}" class="invalid-feedback"
                 th:errors="*{courseId}"></div>
        </div>
        <div class="mb-3">
            <label for="mentorId" class="form-label">Mentor</label>
            <select class="form-select" id="mentorId" th:field="*{mentorId}"
                    th:classappend="${#fields.hasErrors('mentorId')} ? 'is-invalid'">
                <option value="">-- Select Mentor --</option>
                <option th:each="mentor : ${mentors}"
                        th:value="${mentor.id}"
                        th:text="${mentor.username}"></option>
            </select>
            <div th:if="${#fields.hasErrors('mentorId')}" class="invalid-feedback"
                 th:errors="*{mentorId}"></div>
        </div>
        <button type="submit" class="btn btn-primary">Create Task</button>
        <a th:href="@{/mentor/tasks}" class="btn btn-link">Back to Tasks</a>
    </form>
</main>
</body>
</html>
```

**Note on two form blocks**: Same pattern as `course-form.html` — `th:if`/`th:unless` on separate forms handles the create vs edit URL difference cleanly. Thymeleaf `th:field` pre-selects the correct dropdown option when editing via `option[value=${courseId}]` matching.

### TaskServiceTest.java

```java
package com.examinai.task;

import com.examinai.course.Course;
import com.examinai.course.CourseRepository;
import com.examinai.user.UserAccount;
import com.examinai.user.UserAccountRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock TaskRepository taskRepository;
    @Mock CourseRepository courseRepository;
    @Mock UserAccountRepository userAccountRepository;
    @InjectMocks TaskService taskService;

    @Test
    void create_savesTaskWithCorrectFields() {
        Course course = new Course();
        course.setCourseName("Spring Boot");
        UserAccount mentor = new UserAccount();
        mentor.setUsername("mentor");

        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        when(userAccountRepository.findById(2L)).thenReturn(Optional.of(mentor));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        TaskCreateDto dto = new TaskCreateDto();
        dto.setTaskName("Build REST API");
        dto.setTaskDescription("Implement CRUD endpoints");
        dto.setCourseId(1L);
        dto.setMentorId(2L);

        Task result = taskService.create(dto);

        assertThat(result.getTaskName()).isEqualTo("Build REST API");
        assertThat(result.getTaskDescription()).isEqualTo("Implement CRUD endpoints");
        assertThat(result.getCourse()).isSameAs(course);
        assertThat(result.getMentor()).isSameAs(mentor);
        verify(taskRepository).save(any(Task.class));
    }

    @Test
    void update_modifiesTaskFieldsAndSaves() {
        Course newCourse = new Course();
        newCourse.setCourseName("New Course");
        UserAccount newMentor = new UserAccount();
        newMentor.setUsername("new_mentor");

        Task existing = new Task();
        existing.setTaskName("Old Name");
        when(taskRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(courseRepository.findById(3L)).thenReturn(Optional.of(newCourse));
        when(userAccountRepository.findById(4L)).thenReturn(Optional.of(newMentor));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        TaskCreateDto dto = new TaskCreateDto();
        dto.setTaskName("New Name");
        dto.setTaskDescription("Updated description");
        dto.setCourseId(3L);
        dto.setMentorId(4L);
        taskService.update(1L, dto);

        assertThat(existing.getTaskName()).isEqualTo("New Name");
        assertThat(existing.getCourse()).isSameAs(newCourse);
        assertThat(existing.getMentor()).isSameAs(newMentor);
        verify(taskRepository).save(existing);
    }

    @Test
    void delete_callsRepositoryDelete() {
        Task existing = new Task();
        when(taskRepository.findById(1L)).thenReturn(Optional.of(existing));

        taskService.delete(1L);

        verify(taskRepository).delete(existing);
    }

    @Test
    void findById_withMissingId_throwsIllegalArgumentException() {
        when(taskRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.findById(99L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Task not found");
    }
}
```

**Note**: `UserAccount` requires a public `setUsername()` setter for the test above. Verify `UserAccount.java` has a setter for `username` (it does — confirmed from Story 1.2).

### Critical Architecture Rules for This Story

1. **NO new Liquibase changelog** — `task` table exists in `001-init-schema.sql:21-28`. The cascade to `task_review` also pre-exists. Do NOT create a `005-task.sql` or similar.

2. **`@Transactional` on Service methods only** — `TaskController` MUST NOT have `@Transactional`. Only `create`, `update`, `delete` in `TaskService` are transactional.

3. **`@PreAuthorize` on every method individually** — prohibited to use class-level annotation alone. Every method in both `TaskController` and `TaskService` must have it.

4. **PRG rule (mandatory)** — all successful `@PostMapping` return `"redirect:/mentor/tasks"`. Direct view return ONLY allowed for validation errors.

5. **Re-add courses and mentors on validation error** — the task form's dropdowns need `"courses"` and `"mentors"` in model even on error re-render. Forgetting this causes `NullPointerException` in Thymeleaf (`${courses}` is null).

6. **Template URLs via `th:href="@{...}"` only** — Team Agreement from Epic 1 retro. No absolute paths.

7. **DELETE via POST form** — Spring Security 6 CSRF blocks GET-based state changes. Delete uses `<form method="post">` with CSRF token.

8. **Package placement** — `TaskController.java` goes in `com.examinai.task`, NOT `com.examinai.admin`.

9. **Cascade at DB level** — `task_review.task_id` has `ON DELETE CASCADE`, which cascades further to `task_review_issue.task_review_id`. No JPA cascade needed.

10. **LAZY loading requires open-in-view** — default Spring Boot has `spring.jpa.open-in-view=true` which keeps the Hibernate session open during template rendering, allowing `task.course.courseName` to load lazily. Do NOT disable this setting.

### Previous Story Intelligence (Story 2.1 Learnings)

1. **Spring Boot version** — 3.5.13 (not 3.4.2). Architecture doc is outdated.
2. **No new changelog needed** — task and task_review tables already in 001-init-schema.sql. Same as course table situation in Story 2.1.
3. **Pattern for create vs edit form** — use two `<form>` blocks with `th:if`/`th:unless` (not a hidden field hack). Course-form.html established this pattern. Follow exactly.
4. **PRG redirect to mentor namespace** — admins also use `/mentor/tasks` redirect (admin inherits mentor), same as courses always redirected to `/mentor/courses`.
5. **`@PreAuthorize` on every individual method** — confirmed working with Spring AOP proxy when controller calls service. Do not skip.
6. **DTO must be a class with no-arg constructor and setters** — records do not work with Thymeleaf `th:object` binding. Confirmed from CourseCreateDto.
7. **`CourseRepository` can be injected directly** into `TaskService` — no need to go through `CourseService`. CourseService's methods are protected by `@PreAuthorize` which would cause issues if called from TaskService's service context.
8. **Review findings from 2.1 to avoid repeating**: Add `@Size` max constraints on DTO string fields (add `@Size(max=255)` to `taskName` and `@Size(max=100)` to `taskDescription`). Add try-catch + `RedirectAttributes` in controller edit/delete handlers for `IllegalArgumentException`.

### Git Context

Recent commits:
- `2bba320` — Story 2.1 complete (Course entity, service, controller, templates, tests)
- `4dc3fb5` — Epic 1 all stories complete
- `3ca6fea` — Story 1.2 (security, Liquibase, UserAccount entity)

Story 2.2 is the second story in Epic 2 (epic-2 is already `in-progress`).

### Project Structure

**Files to CREATE:**
```
src/main/java/com/examinai/task/
├── Task.java                  ← NEW entity
├── TaskRepository.java        ← NEW repository
├── TaskCreateDto.java         ← NEW DTO
├── TaskService.java           ← NEW service
└── TaskController.java        ← NEW controller (mentor/admin CRUD only)

src/main/resources/templates/admin/
├── task-list.html             ← NEW template
└── task-form.html             ← NEW template

src/test/java/com/examinai/task/
└── TaskServiceTest.java       ← NEW test
```

**Files to MODIFY:**
```
None — no existing files need modification for this story
```

**Do NOT touch:**
- `src/main/java/com/examinai/task/InternTaskController.java` — Story 2.3 concern
- Any Liquibase changelog files — task table already exists
- `SecurityConfig.java` — existing patterns cover `/mentor/**` and `/admin/**`
- `CourseService.java` or `CourseRepository.java` — they already have what TaskService needs

### Verification Checklist

After completing all tasks:
1. `mvn clean compile` — BUILD SUCCESS, 0 errors
2. `mvn test` — `SecurityIntegrationTest` (10 tests), `CourseServiceTest` (4 tests), `TaskServiceTest` (4 tests) all pass
3. With local PostgreSQL running: app starts cleanly, Liquibase reports 4 changelogs applied (no new changeset)
4. Manual: login as `mentor`/`mentor123` → navigate to `/mentor/tasks` → see 3 seed tasks listed with course and mentor
5. Manual: create a new task "Test Task" (description: "A test", Course: Spring Boot Fundamentals, Mentor: mentor) → redirect to list → task appears
6. Manual: edit "Test Task" → change name to "Updated Task" → redirect → change persists
7. Manual: delete "Updated Task" → redirect → task removed
8. Manual: delete a seed task that has task_review rows (if any) → verify no FK constraint violation
9. Manual: login as `admin`/`admin123` → navigate to `/admin/tasks` → same task list visible
10. Manual: login as `intern`/`intern123` → attempt `/mentor/tasks` → 403
11. Manual: submit task form with empty Task Name → "Task name is required" shows inline + dropdowns still populated
12. Manual: submit task form with no Course selected → "Course is required" shows inline

## Dev Agent Record

### Agent Model Used

claude-sonnet-4-6

### Completion Notes List

- Implemented all 9 tasks for Story 2.2 Task Management
- Created `Task.java` JPA entity with `@ManyToOne(LAZY)` associations to `Course` and `UserAccount`; `@PrePersist` sets `dateCreated`; no JPA cascade (DB-level `ON DELETE CASCADE` handles task_review cleanup)
- Created `TaskRepository` extending `JpaRepository` with `findAllByOrderByTaskNameAsc()` for consistent list ordering
- Created `TaskCreateDto` as a plain class (not record) with `@NotBlank`, `@NotNull`, `@Size` constraints for Thymeleaf `th:object` binding
- Created `TaskService` with `@PreAuthorize` on every individual method; `@Transactional` only on `create`, `update`, `delete`; injects `CourseRepository` and `UserAccountRepository` directly (no service-to-service coupling)
- Created `TaskController` with full URL paths per method (no class-level `@RequestMapping`); PRG pattern on all POST success paths; re-adds `"courses"` and `"mentors"` to model on validation errors; try/catch with `RedirectAttributes` on edit/delete
- Created `admin/task-list.html` and `admin/task-form.html` templates with `layout:decorate`, `th:href="@{...}"` URLs (Team Agreement), CSRF tokens, Bootstrap table and form styling
- Verified `SecurityConfig.java` covers `/mentor/**` (MENTOR or ADMIN) and `/admin/**` (ADMIN) — no changes needed
- All 4 `TaskServiceTest` tests pass; `CourseServiceTest` (4) and `SecurityIntegrationTest` (10) confirm no regressions
- `mvn clean compile` succeeds with zero errors
- `InternTaskController.java` untouched — handles `/intern/tasks` for Story 2.3

### File List

- src/main/java/com/examinai/task/Task.java
- src/main/java/com/examinai/task/TaskRepository.java
- src/main/java/com/examinai/task/TaskCreateDto.java
- src/main/java/com/examinai/task/TaskService.java
- src/main/java/com/examinai/task/TaskController.java
- src/main/resources/templates/admin/task-list.html
- src/main/resources/templates/admin/task-form.html
- src/test/java/com/examinai/task/TaskServiceTest.java

## Change Log

- 2026-04-21: Story 2.2 created — Task Management. Ready for dev agent.
- 2026-04-21: Story 2.2 implemented — Task entity, repository, DTO, service, controller, templates, and tests created. All 9 tasks complete. Status → review.
- 2026-04-21: Code review complete. 7 patches applied (incl. 1 from decision), 3 deferred, 3 dismissed. Status → done.

## Review Findings

### Patch

- [x] [Review][Patch] LazyInitializationException crashes task list — `findAll()` and `findById()` lack `@Transactional(readOnly=true)`; `application.yml` has `open-in-view: false`; LAZY associations on `course` and `mentor` fail at template render time [TaskService.java:29]
- [x] [Review][Patch] Unhandled IllegalArgumentException in `create()` controller — `create()` has no try/catch unlike `update()` and `delete()`; if course or mentor is deleted after form load, uncaught exception propagates as 500 [TaskController.java:36-46]
- [x] [Review][Patch] No role validation when assigning mentor to task — `create()` and `update()` assign any `UserAccount` as mentor without verifying `Role.MENTOR`; an INTERN or ADMIN account could be assigned as task mentor [TaskService.java:54-55]
- [x] [Review][Patch] Hardcoded `/mentor/tasks` URLs break admin workflow — `task-list.html` Edit/Delete links and "Create Task" button all use `/mentor/tasks` paths; admin arriving via `/admin/tasks` follows cross-path links on every action [task-list.html:10,35,37]
- [x] [Review][Patch] Flash error messages never rendered — `errorMessage` flash attribute set in controller but base layout has no rendering block for it; errors on update/delete silently dropped [TaskController.java:65,95]
- [x] [Review][Patch] `taskDescription` missing `@Size` constraint — no max-length validation on DTO; DB column is `TEXT` (unbounded); `taskName` has `@Size(max=255)` but `taskDescription` does not [TaskCreateDto.java:13]
- [x] [Review][Patch] MENTOR can edit/delete tasks owned by other mentors — Decision: restrict MENTORs to own tasks; ADMINs bypass; add ownership check in `update()` and `delete()` against authenticated principal [TaskService.java:66,80]

### Deferred

- [x] [Review][Defer] Orphaned tasks after mentor deletion — `mentor_id` FK lacks `ON DELETE` constraint in schema; pre-existing schema design outside this story's scope [001-init-schema.sql:26] — deferred, pre-existing
- [x] [Review][Defer] `IllegalArgumentException` messages expose internal DB IDs in UI — `orElseThrow` messages like `"Course not found: 42"` appear in flash messages; low risk (authenticated ADMIN/MENTOR only) [TaskService.java] — deferred, pre-existing
- [x] [Review][Defer] `@PreAuthorize` on read methods partially bypassed via internal calls — `findById` annotation not evaluated when called from `update`/`delete`; outer method `@PreAuthorize` provides full protection [TaskService.java] — deferred, pre-existing
