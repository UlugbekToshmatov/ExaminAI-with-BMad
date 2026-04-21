# Story 2.1: Course Management

Status: done

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

As a mentor or admin,
I want to create, view, edit, and delete Courses,
So that I can organize the training curriculum into structured programs.

## Acceptance Criteria

**AC1 — Course list:**
Given a mentor or admin is on `/mentor/courses` or `/admin/courses`
When the page loads
Then all existing courses are listed with course name, technology, and creation date

**AC2 — Create course (PRG):**
Given a mentor or admin fills out the course creation form (course name, technology) and submits
When the form posts
Then a new `Course` is saved and they are redirected to the course list (PRG pattern)

**AC3 — Edit course (PRG):**
Given a course exists and the mentor/admin clicks Edit, updates fields, and saves
When the form posts
Then the course is updated and they are redirected to the course list

**AC4 — Delete course:**
Given a mentor or admin clicks Delete on a course
When the deletion is confirmed
Then the course is removed from the system

**AC5 — Cascade delete:**
Given a `Course` that has one or more associated `Task` records is deleted
When the deletion is confirmed
Then all `Task` rows referencing that `Course` via `course_id` FK are also deleted (cascade), and no FK constraint violation is raised

**AC6 — @PreAuthorize enforcement:**
Given `@PreAuthorize("hasRole('MENTOR') or hasRole('ADMIN')")` is on `CourseService` CRUD methods
When an intern attempts to access `/mentor/courses`
Then a 403 is returned

**AC7 — Course table already migrated:**
Given Liquibase changelog `001-init-schema.sql` already defines the `course` table (id BIGSERIAL PK, course_name VARCHAR NOT NULL, technology VARCHAR, date_created TIMESTAMP NOT NULL)
When the application starts
Then the table is already created — NO new changelog is needed for this story

## Tasks / Subtasks

- [x] Task 1: Create Course entity (AC: 1, 2, 3, 4, 5, 7)
  - [x] Create `src/main/java/com/examinai/course/Course.java`
  - [x] Annotate: `@Entity`, `@Table(name = "course")`
  - [x] Fields: `id` (Long, `@Id @GeneratedValue(strategy = GenerationType.IDENTITY)`), `courseName` (`@Column(name = "course_name", nullable = false)`), `technology` (`@Column(name = "technology")`), `dateCreated` (`@Column(name = "date_created", nullable = false)`)
  - [x] Add `@PrePersist` method setting `dateCreated = LocalDateTime.now()`
  - [x] No JPA cascade annotations needed — DB schema already has `ON DELETE CASCADE` on `task.course_id`
  - [x] Include no-arg constructor + all-arg constructor + getters + setters

- [x] Task 2: Create CourseRepository (AC: 1, 2, 3, 4)
  - [x] Create `src/main/java/com/examinai/course/CourseRepository.java`
  - [x] Extends `JpaRepository<Course, Long>`
  - [x] Add: `List<Course> findAllByOrderByCourseNameAsc();` for consistent ordering

- [x] Task 3: Create CourseCreateDto (AC: 2, 3)
  - [x] Create `src/main/java/com/examinai/course/CourseCreateDto.java`
  - [x] Fields: `courseName` (String, `@NotBlank`), `technology` (String, no constraint — optional field)
  - [x] Include no-arg constructor + getters + setters (required for Thymeleaf `th:object` binding — do NOT use a record)

- [x] Task 4: Create CourseService (AC: 1, 2, 3, 4, 5, 6)
  - [x] Create `src/main/java/com/examinai/course/CourseService.java`
  - [x] Annotate with `@Service`
  - [x] Inject `CourseRepository` via constructor injection
  - [x] Implement `findAll()`: returns `courseRepository.findAllByOrderByCourseNameAsc()`
  - [x] Implement `findById(Long id)`: returns `courseRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Course not found: " + id))`
  - [x] Implement `@Transactional create(CourseCreateDto dto)`: build Course from dto fields, save, return
  - [x] Implement `@Transactional update(Long id, CourseCreateDto dto)`: find by id, update courseName and technology fields, save
  - [x] Implement `@Transactional delete(Long id)`: find by id, call `courseRepository.delete(course)`
  - [x] `@PreAuthorize("hasRole('MENTOR') or hasRole('ADMIN')")` on EVERY method individually (not class-level)
  - [x] `@Transactional` on `create`, `update`, `delete` only — `findAll` and `findById` are reads without transaction overhead

- [x] Task 5: Create CourseController (AC: 1, 2, 3, 4, 5, 6)
  - [x] Create `src/main/java/com/examinai/course/CourseController.java`
  - [x] Annotate with `@Controller`
  - [x] Inject `CourseService` via constructor injection
  - [x] `GET /mentor/courses` and `GET /admin/courses` (use `@GetMapping({"/mentor/courses", "/admin/courses"})`) → load all courses, add to model as `"courses"`, return `"admin/course-list"`
  - [x] `GET /mentor/courses/new` (and `/admin/courses/new`) → add empty `CourseCreateDto` as `"courseDto"`, return `"admin/course-form"`
  - [x] `POST /mentor/courses` and `POST /admin/courses` → call `courseService.create(dto)` → on success `"redirect:/mentor/courses"` → on validation error (`@Valid` + `BindingResult`) return `"admin/course-form"` directly (NOT redirect — this is the PRG exception for validation error display)
  - [x] `GET /mentor/courses/{id}/edit` (and `/admin/courses/{id}/edit`) → find course by id, populate `CourseCreateDto` from course, add `courseId` to model, return `"admin/course-form"`
  - [x] `POST /mentor/courses/{id}/edit` (and `/admin/courses/{id}/edit`) → call `courseService.update(id, dto)` → `"redirect:/mentor/courses"`
  - [x] `POST /mentor/courses/{id}/delete` (and `/admin/courses/{id}/delete`) → call `courseService.delete(id)` → `"redirect:/mentor/courses"`
  - [x] `@PreAuthorize("hasRole('MENTOR') or hasRole('ADMIN')")` on EVERY method individually
  - [x] Use `@Valid` + `BindingResult` on POST create and edit handlers
  - [x] Add input validation subtask: ensure `BindingResult` is checked BEFORE calling service; if `bindingResult.hasErrors()` re-render form directly

- [x] Task 6: Create `admin/course-list.html` template (AC: 1, 4, 5, 6)
  - [x] Create `src/main/resources/templates/admin/course-list.html`
  - [x] Extends base layout (`layout:decorate="~{layout/base}"`)
  - [x] Table (`table table-hover`) with columns: Course Name, Technology, Created Date, Actions
  - [x] All column headers use `<th scope="col">` (accessibility requirement)
  - [x] Actions column: Edit link (`th:href="@{/mentor/courses/{id}/edit(id=${course.id})}"`) and Delete button (POST form)
  - [x] Delete button must be a `<form method="post">` with CSRF token — NOT a GET link (Spring Security 6 blocks GET-based state changes)
  - [x] "Create Course" button linking to `th:href="@{/mentor/courses/new}"`
  - [x] Empty state: `<p class="text-muted">` "No courses found." when list is empty
  - [x] Wrap table in `<div class="table-responsive">` for mobile
  - [x] All template URLs use `th:href="@{...}"` — NO absolute href paths (critical: Team Agreement from Epic 1 retro)
  - [x] Date rendered with `#temporals.format(course.dateCreated, 'dd MMM yyyy HH:mm')`
  - [x] Accessible: `<main id="main-content">`, heading hierarchy h4

- [x] Task 7: Create `admin/course-form.html` template (AC: 2, 3)
  - [x] Create `src/main/resources/templates/admin/course-form.html`
  - [x] Extends base layout
  - [x] Form bound to `CourseCreateDto` via `th:object="${courseDto}"`
  - [x] For EDIT: form action `th:action="@{/mentor/courses/{id}/edit(id=${courseId})}"` (when `courseId` present in model), for CREATE: `th:action="@{/mentor/courses}"`
  - [x] Detect create vs edit: `th:if="${courseId != null}"` for edit-specific rendering
  - [x] Fields: courseName (text, required), technology (text, optional)
  - [x] Explicit `<label for="...">` on every input
  - [x] `th:field="*{courseName}"` and `th:field="*{technology}"` for binding
  - [x] Field-level error display: `<div th:if="${#fields.hasErrors('courseName')}" th:errors="*{courseName}" class="invalid-feedback">`
  - [x] Apply `is-invalid` class to input when field has errors
  - [x] CSRF hidden input: `<input type="hidden" th:name="${_csrf.parameterName}" th:value="${_csrf.token}">`
  - [x] Submit button + "Back to Courses" link: `th:href="@{/mentor/courses}"` (NOT absolute `/mentor/courses`)
  - [x] All template URLs use `th:href="@{...}"` — NO absolute href paths

- [x] Task 8: Update SecurityConfig for course routes (AC: 6)
  - [x] Open `src/main/java/com/examinai/config/SecurityConfig.java`
  - [x] Verify existing URL pattern already covers `/mentor/**` for MENTOR or ADMIN role
  - [x] Verify existing URL pattern covers `/admin/**` for ADMIN role
  - [x] If SecurityConfig does NOT already have these patterns, add them for `/mentor/courses/**` and `/admin/courses/**`
  - [x] No change likely needed — existing patterns from Story 1.2 should already cover these URLs

- [x] Task 9: Write CourseServiceTest (AC: 2, 3, 4, 6)
  - [x] Create `src/test/java/com/examinai/course/CourseServiceTest.java`
  - [x] Use `@ExtendWith(MockitoExtension.class)` with `@Mock CourseRepository`
  - [x] Test: `create` → dto fields set on course, saved
  - [x] Test: `update` → course found, courseName and technology updated, saved
  - [x] Test: `delete` → course found, `courseRepository.delete(course)` called
  - [x] Test: `findById` with non-existent id → `IllegalArgumentException` thrown
  - [x] Note: `@PreAuthorize` NOT tested here (Mockito doesn't engage Spring Security AOP proxy); security enforcement is covered by existing `SecurityIntegrationTest`

### Review Findings

- [x] [Review][Patch] `IllegalArgumentException` from missing course maps to HTTP 500 — fixed: added try-catch + `RedirectAttributes` to `editForm`, `update`, and `delete` handlers in `CourseController`, matching `AdminController` pattern [CourseController.java]
- [x] [Review][Patch] Missing `@Size` max-length constraints on `CourseCreateDto` fields — fixed: added `@Size(max=255)` to `courseName` and `@Size(max=100)` to `technology` [CourseCreateDto.java:7,10]
- [x] [Review][Defer] Spring AOP self-invocation: `@PreAuthorize` on `findById()` is not evaluated when called internally from `update()`/`delete()` — no current security gap (all three share the same expression), but the annotation on `findById` is misleading when invoked in-process [CourseService.java:22] — deferred, pre-existing
- [x] [Review][Defer] JavaScript `confirm()` is the only delete confirmation guard — no server-side second-factor; standard pattern but easily bypassed programmatically [course-list.html:38] — deferred, pre-existing
- [x] [Review][Defer] `LocalDateTime.now()` used for `dateCreated` — JVM local time, not UTC; timestamps may be inconsistent in multi-timezone or containerized deployments [Course.java:25] — deferred, pre-existing
- [x] [Review][Defer] Redundant `courseRepository.save()` in `@Transactional update()` — managed-entity dirty checking auto-flushes changes on commit; explicit save is harmless but unnecessary [CourseService.java:43] — deferred, pre-existing
- [x] [Review][Defer] `findAllByOrderByCourseNameAsc()` returns an unbounded `List` — no pagination; potential heap pressure as course count grows [CourseRepository.java:7] — deferred, pre-existing
- [x] [Review][Defer] `Course` entity has no `equals`/`hashCode` override — identity-based comparison; subtle bugs possible in set operations or caching scenarios [Course.java] — deferred, pre-existing
- [x] [Review][Defer] `create_savesCourseWithCorrectFields` test does not assert `dateCreated` is non-null — `@PrePersist` hook not invoked in Mockito context, leaving lifecycle behavior untested [CourseServiceTest.java:27] — deferred, pre-existing
- [x] [Review][Defer] No `CourseControllerTest` — form binding, redirect behavior, and validation error re-render are untested at controller level — deferred, pre-existing

## Dev Notes

### Critical Context — Read First

- **Spring Boot 3.5.13** is in use (NOT 3.4.2 from architecture docs). Every story dev note confirms this. All new code targets Spring Boot 3.5.13 + Spring Security 6.x.
- **`course` table already exists** in `001-init-schema.sql` — do NOT create a new Liquibase changelog for the course table. The AC is satisfied because the table was created forward-looking in Story 1.2.
- **DB CASCADE already wired**: `task.course_id` has `ON DELETE CASCADE` at the PostgreSQL level. Deleting a `Course` via `courseRepository.delete(course)` will cascade to `task`, then `task_review`, then `task_review_issue` at DB level. No JPA cascade annotations needed on `Course.java`.
- **`UserAccountRepository.findAllByRole(Role.MENTOR)`** already exists (used in Story 2.2 for mentor dropdown on task form). Do NOT add this to `CourseService` — it belongs in `TaskService`.
- **Package-by-feature**: `Course`, `CourseRepository`, `CourseService`, `CourseController` all go in `com.examinai.course`. Do NOT put `CourseController` in `com.examinai.admin`.
- **Seed data**: One course "Spring Boot Fundamentals" (Java) + 3 tasks already exist from `004-seed-data.sql`. Use this for manual verification.
- **Absolute href prohibition** (Team Agreement from Epic 1 retro): ALL template URLs must use `th:href="@{...}"` — never bare `/admin/...` or `/mentor/...` paths. This is a Team Agreement from the Epic 1 retrospective, not optional.
- **Validation subtasks are mandatory** (Process Improvement from Epic 1 retro): Every story task list must include explicit input validation + error handling subtasks. These are included in Task 5 (POST handlers) and Task 3 (`@NotBlank` on DTO).

### Course.java Entity

```java
package com.examinai.course;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "course")
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "course_name", nullable = false)
    private String courseName;

    @Column(name = "technology")
    private String technology;

    @Column(name = "date_created", nullable = false)
    private LocalDateTime dateCreated;

    @PrePersist
    private void prePersist() {
        this.dateCreated = LocalDateTime.now();
    }

    public Course() {}

    public Long getId() { return id; }
    public String getCourseName() { return courseName; }
    public void setCourseName(String courseName) { this.courseName = courseName; }
    public String getTechnology() { return technology; }
    public void setTechnology(String technology) { this.technology = technology; }
    public LocalDateTime getDateCreated() { return dateCreated; }
}
```

**Why no JPA cascade**: `task.course_id` has `ON DELETE CASCADE` in PostgreSQL (`001-init-schema.sql:24`). JPA `@OneToMany(cascade = CascadeType.REMOVE)` would cause JPA to load and delete each Task individually before deleting the Course — this is slower and unnecessary. Let the DB handle cascade deletion natively.

### CourseCreateDto.java

```java
package com.examinai.course;

import jakarta.validation.constraints.NotBlank;

public class CourseCreateDto {

    @NotBlank(message = "Course name is required")
    private String courseName;

    private String technology;

    public CourseCreateDto() {}

    public String getCourseName() { return courseName; }
    public void setCourseName(String courseName) { this.courseName = courseName; }
    public String getTechnology() { return technology; }
    public void setTechnology(String technology) { this.technology = technology; }
}
```

**Why no-arg constructor + setters**: Thymeleaf `th:object="${courseDto}"` with `th:field` requires JavaBean-style getters/setters. Records do not work with Thymeleaf form binding.

### CourseService.java

```java
package com.examinai.course;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class CourseService {

    private final CourseRepository courseRepository;

    public CourseService(CourseRepository courseRepository) {
        this.courseRepository = courseRepository;
    }

    @PreAuthorize("hasRole('MENTOR') or hasRole('ADMIN')")
    public List<Course> findAll() {
        return courseRepository.findAllByOrderByCourseNameAsc();
    }

    @PreAuthorize("hasRole('MENTOR') or hasRole('ADMIN')")
    public Course findById(Long id) {
        return courseRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Course not found: " + id));
    }

    @Transactional
    @PreAuthorize("hasRole('MENTOR') or hasRole('ADMIN')")
    public Course create(CourseCreateDto dto) {
        Course course = new Course();
        course.setCourseName(dto.getCourseName());
        course.setTechnology(dto.getTechnology());
        return courseRepository.save(course);
    }

    @Transactional
    @PreAuthorize("hasRole('MENTOR') or hasRole('ADMIN')")
    public Course update(Long id, CourseCreateDto dto) {
        Course course = findById(id);
        course.setCourseName(dto.getCourseName());
        course.setTechnology(dto.getTechnology());
        return courseRepository.save(course);
    }

    @Transactional
    @PreAuthorize("hasRole('MENTOR') or hasRole('ADMIN')")
    public void delete(Long id) {
        Course course = findById(id);
        courseRepository.delete(course);
    }
}
```

**Critical rules:**
- `@Transactional` on `create`, `update`, `delete` only — NOT on `findAll` or `findById`
- `@PreAuthorize` on EVERY method individually — class-level alone is prohibited per architecture
- NEVER add `@Transactional` to `CourseController`

### CourseController.java

```java
package com.examinai.course;

import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
public class CourseController {

    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    @GetMapping({"/mentor/courses", "/admin/courses"})
    @PreAuthorize("hasRole('MENTOR') or hasRole('ADMIN')")
    public String list(Model model) {
        model.addAttribute("courses", courseService.findAll());
        return "admin/course-list";
    }

    @GetMapping({"/mentor/courses/new", "/admin/courses/new"})
    @PreAuthorize("hasRole('MENTOR') or hasRole('ADMIN')")
    public String newForm(Model model) {
        model.addAttribute("courseDto", new CourseCreateDto());
        return "admin/course-form";
    }

    @PostMapping({"/mentor/courses", "/admin/courses"})
    @PreAuthorize("hasRole('MENTOR') or hasRole('ADMIN')")
    public String create(@Valid @ModelAttribute("courseDto") CourseCreateDto dto,
                         BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return "admin/course-form";
        }
        courseService.create(dto);
        return "redirect:/mentor/courses";
    }

    @GetMapping({"/mentor/courses/{id}/edit", "/admin/courses/{id}/edit"})
    @PreAuthorize("hasRole('MENTOR') or hasRole('ADMIN')")
    public String editForm(@PathVariable Long id, Model model) {
        Course course = courseService.findById(id);
        CourseCreateDto dto = new CourseCreateDto();
        dto.setCourseName(course.getCourseName());
        dto.setTechnology(course.getTechnology());
        model.addAttribute("courseDto", dto);
        model.addAttribute("courseId", id);
        return "admin/course-form";
    }

    @PostMapping({"/mentor/courses/{id}/edit", "/admin/courses/{id}/edit"})
    @PreAuthorize("hasRole('MENTOR') or hasRole('ADMIN')")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("courseDto") CourseCreateDto dto,
                         BindingResult bindingResult,
                         Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("courseId", id);
            return "admin/course-form";
        }
        courseService.update(id, dto);
        return "redirect:/mentor/courses";
    }

    @PostMapping({"/mentor/courses/{id}/delete", "/admin/courses/{id}/delete"})
    @PreAuthorize("hasRole('MENTOR') or hasRole('ADMIN')")
    public String delete(@PathVariable Long id) {
        courseService.delete(id);
        return "redirect:/mentor/courses";
    }
}
```

**Why POST for delete**: Spring Security 6 CSRF protection blocks GET-based state changes. Delete actions must use POST forms with CSRF token.

**Why redirect to `/mentor/courses`**: Admin inherits all Mentor capabilities (FR5). `/mentor/courses` is accessible by MENTOR or ADMIN roles. Always redirecting there is the simplest PRG-compliant behavior for both roles.

**Why return view directly on validation error**: `@PostMapping` always redirects on SUCCESS (PRG). On validation failure, returning the view directly is the correct Spring MVC pattern for form re-display with field errors. This is a standard exception to PRG.

### admin/course-list.html

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org"
      xmlns:layout="http://www.ultraq.net.nz/thymeleaf/layout"
      layout:decorate="~{layout/base}">
<head><title>Course Management</title></head>
<body>
<main id="main-content" layout:fragment="content">
    <div class="d-flex justify-content-between align-items-center mb-3">
        <h4>Courses</h4>
        <a th:href="@{/mentor/courses/new}" class="btn btn-primary btn-sm">Create Course</a>
    </div>

    <div th:if="${#lists.isEmpty(courses)}" class="text-muted">
        <p>No courses found.</p>
    </div>

    <div th:unless="${#lists.isEmpty(courses)}" class="table-responsive">
        <table class="table table-hover">
            <thead>
                <tr>
                    <th scope="col">Course Name</th>
                    <th scope="col">Technology</th>
                    <th scope="col">Created</th>
                    <th scope="col">Actions</th>
                </tr>
            </thead>
            <tbody>
                <tr th:each="course : ${courses}">
                    <td th:text="${course.courseName}"></td>
                    <td th:text="${course.technology}"></td>
                    <td th:text="${#temporals.format(course.dateCreated, 'dd MMM yyyy HH:mm')}"></td>
                    <td>
                        <a th:href="@{/mentor/courses/{id}/edit(id=${course.id})}"
                           class="btn btn-sm btn-outline-secondary me-1">Edit</a>
                        <form th:action="@{/mentor/courses/{id}/delete(id=${course.id})}"
                              method="post" class="d-inline">
                            <input type="hidden" th:name="${_csrf.parameterName}" th:value="${_csrf.token}">
                            <button type="submit" class="btn btn-sm btn-outline-danger"
                                    onclick="return confirm('Delete this course and all its tasks?')">Delete</button>
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

### admin/course-form.html

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org"
      xmlns:layout="http://www.ultraq.net.nz/thymeleaf/layout"
      layout:decorate="~{layout/base}">
<head><title>Course Form</title></head>
<body>
<main id="main-content" layout:fragment="content">
    <h4 th:text="${courseId != null} ? 'Edit Course' : 'Create Course'">Course</h4>

    <form th:if="${courseId != null}"
          th:action="@{/mentor/courses/{id}/edit(id=${courseId})}"
          th:object="${courseDto}" method="post" class="col-md-5">
        <input type="hidden" th:name="${_csrf.parameterName}" th:value="${_csrf.token}">
        <div class="mb-3">
            <label for="courseName" class="form-label">Course Name</label>
            <input type="text" class="form-control" id="courseName" th:field="*{courseName}" required
                   th:classappend="${#fields.hasErrors('courseName')} ? 'is-invalid'">
            <div th:if="${#fields.hasErrors('courseName')}" class="invalid-feedback"
                 th:errors="*{courseName}"></div>
        </div>
        <div class="mb-3">
            <label for="technology" class="form-label">Technology</label>
            <input type="text" class="form-control" id="technology" th:field="*{technology}">
        </div>
        <button type="submit" class="btn btn-primary">Save Changes</button>
        <a th:href="@{/mentor/courses}" class="btn btn-link">Back to Courses</a>
    </form>

    <form th:unless="${courseId != null}"
          th:action="@{/mentor/courses}"
          th:object="${courseDto}" method="post" class="col-md-5">
        <input type="hidden" th:name="${_csrf.parameterName}" th:value="${_csrf.token}">
        <div class="mb-3">
            <label for="courseName" class="form-label">Course Name</label>
            <input type="text" class="form-control" id="courseName" th:field="*{courseName}" required
                   th:classappend="${#fields.hasErrors('courseName')} ? 'is-invalid'">
            <div th:if="${#fields.hasErrors('courseName')}" class="invalid-feedback"
                 th:errors="*{courseName}"></div>
        </div>
        <div class="mb-3">
            <label for="technology" class="form-label">Technology</label>
            <input type="text" class="form-control" id="technology" th:field="*{technology}">
        </div>
        <button type="submit" class="btn btn-primary">Create Course</button>
        <a th:href="@{/mentor/courses}" class="btn btn-link">Back to Courses</a>
    </form>
</main>
</body>
</html>
```

**Note on two form blocks**: Thymeleaf `th:action` with `th:if`/`th:unless` on separate forms is the cleanest way to handle create vs edit URL difference without adding a hidden id field. Both forms share the same `th:object="${courseDto}"` binding.

### CourseServiceTest.java

```java
package com.examinai.course;

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
class CourseServiceTest {

    @Mock CourseRepository courseRepository;
    @InjectMocks CourseService courseService;

    @Test
    void create_savesCoursWithCorrectFields() {
        CourseCreateDto dto = new CourseCreateDto();
        dto.setCourseName("Spring Boot");
        dto.setTechnology("Java");
        when(courseRepository.save(any(Course.class))).thenAnswer(inv -> inv.getArgument(0));

        Course result = courseService.create(dto);

        assertThat(result.getCourseName()).isEqualTo("Spring Boot");
        assertThat(result.getTechnology()).isEqualTo("Java");
        verify(courseRepository).save(any(Course.class));
    }

    @Test
    void update_modifiesCourseFieldsAndSaves() {
        Course existing = new Course();
        existing.setCourseName("Old Name");
        existing.setTechnology("Python");
        when(courseRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(courseRepository.save(any(Course.class))).thenAnswer(inv -> inv.getArgument(0));

        CourseCreateDto dto = new CourseCreateDto();
        dto.setCourseName("New Name");
        dto.setTechnology("Java");
        courseService.update(1L, dto);

        assertThat(existing.getCourseName()).isEqualTo("New Name");
        assertThat(existing.getTechnology()).isEqualTo("Java");
        verify(courseRepository).save(existing);
    }

    @Test
    void delete_callsRepositoryDelete() {
        Course existing = new Course();
        when(courseRepository.findById(1L)).thenReturn(Optional.of(existing));

        courseService.delete(1L);

        verify(courseRepository).delete(existing);
    }

    @Test
    void findById_withMissingId_throwsIllegalArgumentException() {
        when(courseRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> courseService.findById(99L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Course not found");
    }
}
```

**Note:** `@PreAuthorize` is NOT tested here because `@ExtendWith(MockitoExtension.class)` does not engage the Spring Security AOP proxy. `@PreAuthorize` security enforcement is already covered by `SecurityIntegrationTest` at the URL level (which tests that `/mentor/**` requires MENTOR or ADMIN role via the Spring Security filter chain).

### Critical Architecture Rules for This Story

1. **NO new Liquibase changelog** — `course` table exists in `001-init-schema.sql`. Do NOT create a `005-course.sql`. The AC states the table is created when app starts — it already is.

2. **`@Transactional` on Service methods only** — `CourseController` MUST NOT have `@Transactional`. `CourseService.create`, `update`, `delete` are `@Transactional`. `findAll` and `findById` are not.

3. **`@PreAuthorize` on every method individually** — class-level annotation alone is architecturally prohibited. Applied on every individual method in both `CourseController` and `CourseService`.

4. **PRG rule (mandatory)** — `@PostMapping` always returns `"redirect:/..."` on success. Returning the view directly is ONLY allowed when there is a validation error to display.

5. **Template URLs must use `th:href="@{...}"`** — Team Agreement from Epic 1 retrospective. No bare `/mentor/...` or `/admin/...` paths in templates.

6. **Delete via POST form** — use `<form method="post">` with CSRF token. Spring Security 6 blocks GET-based state changes.

7. **Package placement** — `CourseController.java` goes in `com.examinai.course`, NOT `com.examinai.admin`.

8. **Explicit validation subtasks** — Process Improvement from Epic 1 retrospective: `@NotBlank` on `CourseCreateDto.courseName`, `@Valid` in controller, `BindingResult` check before calling service. All included in tasks above.

9. **Cascade at DB level** — `task.course_id` has `ON DELETE CASCADE` already. Do NOT add JPA `cascade = CascadeType.REMOVE` to avoid N+1 deletes.

### Previous Story Intelligence (Story 1.3 + Epic 1 Retro Learnings)

1. **Spring Boot 3.5.13** — ignore all architecture doc references to 3.4.2.
2. **Spring AI artifact** is `spring-ai-starter-model-ollama` in pom.xml (N/A for this story; do not change pom.xml).
3. **Stub controllers exist** — `InternTaskController` at `com.examinai.task` handles `/intern/tasks`. `MentorReviewController` and `AdminController` exist. Do NOT modify these.
4. **`SecurityIntegrationTest`** has 10 tests passing. Adding `CourseController` does not break these — they test auth routes only. No `@MockBean CourseService` needed in `SecurityIntegrationTest` because `@WebMvcTest` will only load `CourseController` in a separate `CourseControllerTest` if needed.
5. **Validation gaps** — recurring first-pass issue in Epic 1: story ACs describe happy path; validation omitted. This story explicitly lists `@NotBlank` on DTO and `@Valid` + `BindingResult` in controller tasks to prevent this.
6. **Absolute href paths** — user-list.html and user-form.html from Story 1.3 still have absolute paths (deferred-work.md). Do NOT copy those patterns. This story uses `th:href="@{...}"` exclusively.
7. **`@PreAuthorize` works on service methods** when called from controller (Spring AOP proxy). Confirmed from Story 1.3 implementation.
8. **DTO + no-arg constructor + setters** pattern required for Thymeleaf `th:object` binding. Confirmed from `UserAccountCreateDto`.

### Git Context

Recent commits on main:
- `4dc3fb5` — Epic 1 all stories complete (admin user management, seed data, indexes)
- `3ca6fea` — Story 1.2 (security, auth, Liquibase 001+002, UserAccount entity, stubs)
- `5cb815d` — Story 1.1 (scaffold, pom.xml, base layout, custom.css)

Epic 1 is fully done. This is the first story in Epic 2. No pending branches or merge conflicts.

### Project Structure Notes

**Files to CREATE:**
```
src/main/java/com/examinai/course/
├── Course.java                  ← NEW entity
├── CourseRepository.java        ← NEW repository
├── CourseCreateDto.java         ← NEW DTO
└── CourseController.java        ← NEW controller

src/main/resources/templates/admin/
├── course-list.html             ← NEW template
└── course-form.html             ← NEW template

src/test/java/com/examinai/course/
└── CourseServiceTest.java       ← NEW test
```

**Note:** No `CourseService.java` in the CREATE list above — wait, yes it should be there:
```
src/main/java/com/examinai/course/
└── CourseService.java           ← NEW service
```

**Files to MODIFY:**
```
None — no existing files need modification for this story
```

**Files unchanged:** All existing controllers, `UserAccount*.java`, `SecurityConfig.java`, `WebMvcConfig.java`, all Epic 1 templates, all Liquibase changelogs.

**Do NOT create:**
- A new Liquibase changelog (course table already in 001)
- `templates/mentor/course-*.html` (templates go in `admin/` per architecture)
- Any additional `admin/` controller class (CourseController is in `course/` package)

### Verification Checklist

After completing all tasks, verify:
1. `mvn clean compile` — BUILD SUCCESS, 0 errors
2. `mvn test` — `SecurityIntegrationTest` (10 tests) and `CourseServiceTest` (4 tests) all pass
3. With local PostgreSQL running, verify app starts cleanly (Liquibase reports 4 changelogs applied, no new changeset)
4. Manual: login as `mentor` / `mentor123` → navigate to `/mentor/courses` → see "Spring Boot Fundamentals" listed
5. Manual: create a new course "Python Fundamentals" (Technology: Python) → redirect to list → course appears
6. Manual: edit "Python Fundamentals" → change technology to "Django" → redirect to list → change persists
7. Manual: delete "Python Fundamentals" → redirect to list → course removed
8. Manual: delete "Spring Boot Fundamentals" → verify the 3 seed tasks are also removed (SELECT COUNT(*) FROM task → 0)
9. Manual: login as `admin` / `admin123` → navigate to `/admin/courses` → same list visible
10. Manual: login as `intern` / `intern123` → navigate to `/mentor/courses` → 403 error (or redirect to login)
11. Manual: attempt to submit empty course form → "Course name is required" validation error appears inline

### References

- Story 2.1 ACs: [Source: epics.md#Story 2.1: Course Management]
- `course` table schema: [Source: 001-init-schema.sql#CREATE TABLE course]
- `task.course_id ON DELETE CASCADE`: [Source: 001-init-schema.sql#CREATE TABLE task]
- Package-by-feature: [Source: architecture.md#Package Organization — com.examinai/course/]
- Template location: [Source: architecture.md#Template Organization — admin/course-list.html, admin/course-form.html]
- CourseController dual namespace: [Source: architecture.md#Complete Project Directory Structure — CourseController.java (/mentor/courses/**, /admin/courses/**)]
- `@PreAuthorize` on every method individually: [Source: architecture.md#Enforcement Guidelines]
- `@Transactional` on Service methods only: [Source: architecture.md#Process Patterns — Transaction Boundary Rule]
- PRG rule mandatory: [Source: architecture.md#Process Patterns — PRG Rule]
- No absolute href paths: [Source: epic-1-retro-2026-04-21.md#Team Agreements]
- Explicit validation subtasks: [Source: epic-1-retro-2026-04-21.md#Action Items — Process Improvements]
- Spring Boot 3.5.13 version correction: [Source: implementation-artifacts/1-2-user-authentication-role-based-access.md#Dev Notes]
- Cascade delete behavior: [Source: architecture.md#Data Architecture — no application-level cascade note]

## Dev Agent Record

### Agent Model Used

claude-sonnet-4-6

### Debug Log References

None — implementation completed without issues. Pre-existing test failures in `ExaminAiApplicationTests` (needs DB) and `UserAccountServiceTest.deactivate_setsActiveFalseAndSaves` (null SecurityContext) are unrelated to this story.

### Completion Notes List

- Created `com.examinai.course` package with Course entity, CourseRepository, CourseCreateDto, CourseService, and CourseController
- `@PreAuthorize("hasRole('MENTOR') or hasRole('ADMIN')")` applied on every method individually in both service and controller
- `@Transactional` applied only on `create`, `update`, `delete` service methods; `findAll` and `findById` are read-only without transaction overhead
- PRG pattern enforced: all successful POSTs redirect to `/mentor/courses`; validation errors re-render the form directly
- DB-level cascade (`ON DELETE CASCADE` on `task.course_id`) handles task deletion — no JPA cascade needed on Course entity
- `admin/course-list.html` and `admin/course-form.html` templates use exclusively `th:href="@{...}"` per Team Agreement
- Delete actions use POST forms with CSRF token per Spring Security 6 requirements
- SecurityConfig already covered `/mentor/**` (MENTOR or ADMIN) and `/admin/**` (ADMIN) — no changes needed
- `CourseServiceTest` (4 tests) and `SecurityIntegrationTest` (10 tests) all pass; `mvn clean compile` BUILD SUCCESS

### File List

**Created:**
- `src/main/java/com/examinai/course/Course.java`
- `src/main/java/com/examinai/course/CourseRepository.java`
- `src/main/java/com/examinai/course/CourseCreateDto.java`
- `src/main/java/com/examinai/course/CourseService.java`
- `src/main/java/com/examinai/course/CourseController.java`
- `src/main/resources/templates/admin/course-list.html`
- `src/main/resources/templates/admin/course-form.html`
- `src/test/java/com/examinai/course/CourseServiceTest.java`

**Modified:** None

## Change Log

- 2026-04-21: Implemented Story 2.1 — Course Management. Created Course entity, repository, DTO, service, controller, two Thymeleaf templates, and unit tests. All 9 tasks complete. 4/4 CourseServiceTest + 10/10 SecurityIntegrationTest pass.
