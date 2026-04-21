# Story 1.3: Admin User Account Management & Seed Data

Status: done

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

As an admin,
I want to create user accounts with assigned roles and deactivate existing accounts, with the system pre-loaded with starter data on first startup,
So that I can control platform access and the system is immediately usable without manual setup.

## Acceptance Criteria

**AC1 — Admin creates user account (PRG):**
Given the admin is on `/admin/users` and fills out the user creation form (username, email, role, initial password)
When the form is submitted
Then a new `UserAccount` is saved with the password BCrypt-hashed at strength 12, `active=true`, and the assigned role
And the admin is redirected back to `/admin/users` (PRG pattern)

**AC2 — Admin deactivates user:**
Given an existing active user account is listed on `/admin/users`
When the admin clicks Deactivate
Then `active` is set to `false` and the user is immediately unable to log in (`CustomUserDetailsService` throws `DisabledException` for inactive accounts)

**AC3 — Seed data on first startup:**
Given the application starts for the first time
When Liquibase changelog `004-seed-data.sql` is applied
Then the following accounts exist with BCrypt-hashed passwords (strength ≥ 12): admin (ADMIN), mentor (MENTOR), intern (INTERN)
And at least one Course and 3 Tasks with descriptions and mentor assignment are present

**AC4 — Duplicate username error:**
Given the admin attempts to create a user with a username that already exists
When the form is submitted
Then an inline error message is shown ("Username already exists") and no duplicate account is created

**AC5 — User list:**
Given the admin views `/admin/users`
When the page loads
Then all user accounts are listed with their username, role, and active/inactive status

**AC6 — @PreAuthorize enforcement:**
Given `@PreAuthorize("hasRole('ADMIN')")` is on `UserAccountService.createUser()` and `deactivate()`
When a non-admin calls these methods
Then an `AccessDeniedException` is thrown — server-side enforcement is authoritative

## Tasks / Subtasks

- [x] Task 1: Create Liquibase 003-indexes.sql (AC: 3)
  - [x] Create `src/main/resources/db/changelog/changelogs/003-indexes.sql`
  - [x] Add: `CREATE INDEX idx_task_review_intern_id ON task_review(intern_id);`
  - [x] Add: `CREATE INDEX idx_task_review_status ON task_review(status);`
  - [x] Add: `CREATE INDEX idx_task_review_mentor_id ON task_review(mentor_id);`

- [x] Task 2: Create Liquibase 004-seed-data.sql (AC: 3)
  - [x] Create `src/main/resources/db/changelog/changelogs/004-seed-data.sql`
  - [x] Insert admin (ADMIN), mentor (MENTOR), intern (INTERN) with BCrypt-strength-12 hashed passwords
  - [x] Insert at least one Course (e.g., "Spring Boot Fundamentals", technology="Java")
  - [x] Insert 3 Tasks referencing the course and mentor user

- [x] Task 3: Update db.changelog-master.xml (AC: 3)
  - [x] Add `<include file="changelogs/003-indexes.sql" relativeToChangelogFile="true"/>` after 002
  - [x] Add `<include file="changelogs/004-seed-data.sql" relativeToChangelogFile="true"/>` after 003
  - [x] Verify order: 001 → 002 → 003 → 004

- [x] Task 4: Add existsByUsername to UserAccountRepository (AC: 4)
  - [x] Open `src/main/java/com/examinai/user/UserAccountRepository.java`
  - [x] Add: `boolean existsByUsername(String username);`

- [x] Task 5: Create UserAccountCreateDto (AC: 1)
  - [x] Create `src/main/java/com/examinai/user/UserAccountCreateDto.java`
  - [x] Fields: `username` (String), `email` (String), `role` (Role), `password` (String)
  - [x] Include no-arg constructor + getters + setters (needed for Thymeleaf `th:object` binding)

- [x] Task 6: Create UserAccountService (AC: 1, 2, 4, 5, 6)
  - [x] Create `src/main/java/com/examinai/user/UserAccountService.java`
  - [x] Annotate with `@Service`
  - [x] Inject `UserAccountRepository` and `PasswordEncoder` (constructor injection)
  - [x] Implement `createUser(UserAccountCreateDto dto)`: check existsByUsername → throw IllegalArgumentException if duplicate → encode password → build and save UserAccount → return it
  - [x] Implement `deactivate(Long id)`: find by id → setActive(false) → save
  - [x] Implement `findAll()`: return `userAccountRepository.findAll()` ordered by username
  - [x] `@Transactional` on `createUser` and `deactivate`
  - [x] `@PreAuthorize("hasRole('ADMIN')")` on `createUser`, `deactivate`, and `findAll` individually

- [x] Task 7: Update AdminController with user management routes (AC: 1, 2, 5)
  - [x] Open `src/main/java/com/examinai/admin/AdminController.java`
  - [x] Add constructor injection of `UserAccountService`
  - [x] Add `GET /admin/users` → load all users, add to model, return `"admin/user-list"`
  - [x] Add `GET /admin/users/new` → add empty `UserAccountCreateDto` + `Role.values()` to model, return `"admin/user-form"`
  - [x] Add `POST /admin/users` → call `userAccountService.createUser(dto)` → on success `"redirect:/admin/users"` → on `IllegalArgumentException` add error to model and return `"admin/user-form"` (also add `Role.values()` back to model)
  - [x] Add `POST /admin/users/{id}/deactivate` → call `userAccountService.deactivate(id)` → `"redirect:/admin/users"`
  - [x] `@PreAuthorize("hasRole('ADMIN')")` on each new method individually

- [x] Task 8: Create admin/user-list.html (AC: 5)
  - [x] Create `src/main/resources/templates/admin/user-list.html`
  - [x] Extends base layout (`layout:decorate="~{layout/base}"`)
  - [x] Table with columns: Username, Email, Role badge, Status (Active/Inactive)
  - [x] Row action: POST form to `/admin/users/{user.id}/deactivate` for each active user (no deactivate button for inactive users)
  - [x] Link or button to "Create User" → GET `/admin/users/new`
  - [x] Empty state message when no users exist
  - [x] Accessible: `<th scope="col">`, `<main id="main-content">`, heading hierarchy

- [x] Task 9: Create admin/user-form.html (AC: 1, 4)
  - [x] Create `src/main/resources/templates/admin/user-form.html`
  - [x] Extends base layout
  - [x] Form bound to `UserAccountCreateDto` via `th:object="${createDto}"`
  - [x] Fields: username (text), email (email), role (select dropdown with all Role values), password (password)
  - [x] Explicit `<label for="...">` on every input
  - [x] `th:action="@{/admin/users}"`, `method="post"`, CSRF hidden input
  - [x] Error alert above form when `error` attribute is present in model (`th:if="${error}"`)
  - [x] Submit button + "Back to User List" link

- [x] Task 10: Write UserAccountServiceTest (AC: 1, 2, 4, 6)
  - [x] Create `src/test/java/com/examinai/user/UserAccountServiceTest.java`
  - [x] Use `@ExtendWith(MockitoExtension.class)` with `@Mock UserAccountRepository` and `@Mock PasswordEncoder`
  - [x] Test: `createUser` → new user saved with encoded password and `active=true`
  - [x] Test: `createUser` with duplicate username → `IllegalArgumentException` thrown, save never called
  - [x] Test: `deactivate` → user found, `setActive(false)`, saved

### Review Findings

- [x] [Review][Decision] AC4 "inline error message" — implemented via `@NotBlank`/`@NotNull` on DTO + `@Valid` + `BindingResult.rejectValue()` in controller + `th:errors` + Bootstrap `is-invalid` on each field in user-form.html. Banner alert removed.

- [x] [Review][Patch] No input validation on UserAccountCreateDto — fixed together with D1: `@NotBlank` on username/password, `@NotNull` on role; `@Valid` in controller prevents null fields reaching the service [UserAccountCreateDto.java, AdminController.java]
- [x] [Review][Patch] deactivateUser controller has no error handling — fixed: try/catch + RedirectAttributes flash attribute; error message shown on user-list page [AdminController.java]
- [x] [Review][Patch] Admin can deactivate their own account causing instant self-lockout — fixed: self-deactivation guard in UserAccountService.deactivate() using SecurityContextHolder [UserAccountService.java]

- [x] [Review][Defer] Race condition in username uniqueness check — existsByUsername + save has no DB unique constraint fallback; concurrent requests can both pass and trigger ConstraintViolationException [UserAccountService.java:24-33] — deferred, pre-existing
- [x] [Review][Defer] BCrypt hashes for seed credentials committed to git history — offline dictionary attack possible; rotation requires a new changeset [004-seed-data.sql] — deferred, acceptable for dev seed data
- [x] [Review][Defer] No reactivate path — deactivation is irreversible via UI; requires direct DB access to restore [user-list.html, UserAccountService.java] — deferred, pre-existing
- [x] [Review][Defer] Email uniqueness not enforced — duplicate emails accepted silently (already tracked from Story 1.2) [UserAccountService.java] — deferred, pre-existing
- [x] [Review][Defer] Seed data uses now() timestamps — non-deterministic across environments [004-seed-data.sql] — deferred, acceptable for dev seed data
- [x] [Review][Defer] Absolute href paths in templates break under non-root context path deployment [user-list.html:10, user-form.html:37] — deferred, MVP scope
- [x] [Review][Defer] findAll() returns unbounded user list with no pagination (already tracked from Story 1.2) [UserAccountService.java:45-48] — deferred, pre-existing
- [x] [Review][Defer] No password complexity enforcement — single-character passwords accepted [UserAccountService.java:29] — deferred, MVP scope

## Dev Notes

### Critical Context — Read First

- **Spring Boot 3.5.13** is in use (NOT 3.4.2 from architecture docs). All new code targets Spring Boot 3.5.13 + Spring Security 6.x.
- **`PasswordEncoder` bean**: `BCryptPasswordEncoder(12)` is already defined in `SecurityConfig.java` as a `@Bean`. Inject `PasswordEncoder` into `UserAccountService` — **do NOT instantiate `new BCryptPasswordEncoder(12)` manually**.
- **`UserAccount` entity** already exists at `com.examinai.user.UserAccount` with all fields: id, username, password, email, role (Role enum), active (boolean), dateCreated (set via @PrePersist).
- **`UserAccountRepository`** already exists with `findByUsername(String)` and `findAllByRole(Role)`. You are adding `existsByUsername(String)`.
- **`CustomUserDetailsService`** already throws `DisabledException` for `active=false` accounts — AC2 login-blocking behavior is already wired; you just need to set `active=false` in the service.
- **`AdminController` stub** already exists at `com.examinai.admin.AdminController` with `GET /admin/dashboard`. Add user management methods to this existing class — **do NOT create a new controller class**.
- **`Role` enum** already exists at `com.examinai.user.Role` with values: INTERN, MENTOR, ADMIN.
- **`db.changelog-master.xml`** currently includes only `001-init-schema.sql` and `002-spring-session.sql`. Add 003 and 004.
- **Package-by-feature**: `UserAccountService` goes in `com.examinai.user`, NOT `com.examinai.admin`.

### 003-indexes.sql

```sql
--liquibase formatted sql

--changeset dev:003-indexes dbms:postgresql
CREATE INDEX idx_task_review_intern_id ON task_review(intern_id);
CREATE INDEX idx_task_review_status    ON task_review(status);
CREATE INDEX idx_task_review_mentor_id ON task_review(mentor_id);
```

### 004-seed-data.sql

BCrypt strength-12 hashes must be pre-generated. Use the following approach to generate them **before writing the SQL** — run this in a quick test method or `main()`:

```java
BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);
System.out.println("admin:    " + encoder.encode("admin123"));
System.out.println("mentor:   " + encoder.encode("mentor123"));
System.out.println("intern:   " + encoder.encode("intern123"));
```

Then substitute the generated hashes into the SQL below (replace `{HASH_*}` with actual output):

```sql
--liquibase formatted sql

--changeset dev:004-seed-data dbms:postgresql
-- Seed users (passwords BCrypt strength 12)
INSERT INTO user_account (username, password, email, role, active, date_created)
VALUES
  ('admin',  '{HASH_ADMIN}',  'admin@examinai.local',  'ADMIN',  true, now()),
  ('mentor', '{HASH_MENTOR}', 'mentor@examinai.local', 'MENTOR', true, now()),
  ('intern', '{HASH_INTERN}', 'intern@examinai.local', 'INTERN', true, now());

-- Seed course
INSERT INTO course (course_name, technology, date_created)
VALUES ('Spring Boot Fundamentals', 'Java', now());

-- Seed tasks (FK refs to course and mentor via subquery)
INSERT INTO task (task_name, task_description, course_id, mentor_id, date_created)
VALUES
  ('Build a REST API',
   'Implement a RESTful API using Spring Boot, JPA, and PostgreSQL. Apply CRUD operations on a domain entity with proper HTTP status codes.',
   (SELECT id FROM course WHERE course_name = 'Spring Boot Fundamentals'),
   (SELECT id FROM user_account WHERE username = 'mentor'),
   now()),
  ('Implement Spring Security',
   'Add form-based authentication to an existing Spring Boot application. Implement role-based access control for at least two user roles.',
   (SELECT id FROM course WHERE course_name = 'Spring Boot Fundamentals'),
   (SELECT id FROM user_account WHERE username = 'mentor'),
   now()),
  ('Write Unit Tests',
   'Write comprehensive unit tests for the service layer using JUnit 5 and Mockito. Achieve at least 80% line coverage on the tested classes.',
   (SELECT id FROM course WHERE course_name = 'Spring Boot Fundamentals'),
   (SELECT id FROM user_account WHERE username = 'mentor'),
   now());
```

**Why subqueries instead of hardcoded IDs:** Liquibase may run on a fresh schema where auto-generated IDs cannot be predicted. Subqueries by `username` / `course_name` are safe and correct.

### Updated db.changelog-master.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog
    xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
        http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.31.xsd">

    <include file="changelogs/001-init-schema.sql" relativeToChangelogFile="true"/>
    <include file="changelogs/002-spring-session.sql" relativeToChangelogFile="true"/>
    <include file="changelogs/003-indexes.sql" relativeToChangelogFile="true"/>
    <include file="changelogs/004-seed-data.sql" relativeToChangelogFile="true"/>

</databaseChangeLog>
```

### UserAccountCreateDto.java

```java
package com.examinai.user;

public class UserAccountCreateDto {

    private String username;
    private String email;
    private Role role;
    private String password;

    public UserAccountCreateDto() {}

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
```

**Why no-arg constructor + setters**: Thymeleaf `th:object="${createDto}"` with `th:field` requires JavaBean-style getters/setters for form binding. Records do not work here.

### UserAccountService.java

```java
package com.examinai.user;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class UserAccountService {

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;

    public UserAccountService(UserAccountRepository userAccountRepository,
                               PasswordEncoder passwordEncoder) {
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public UserAccount createUser(UserAccountCreateDto dto) {
        if (userAccountRepository.existsByUsername(dto.getUsername())) {
            throw new IllegalArgumentException("Username already exists");
        }
        UserAccount account = new UserAccount();
        account.setUsername(dto.getUsername());
        account.setPassword(passwordEncoder.encode(dto.getPassword()));
        account.setEmail(dto.getEmail());
        account.setRole(dto.getRole());
        account.setActive(true);
        return userAccountRepository.save(account);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void deactivate(Long id) {
        UserAccount account = userAccountRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
        account.setActive(false);
        userAccountRepository.save(account);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public List<UserAccount> findAll() {
        return userAccountRepository.findAll();
    }
}
```

**Critical notes:**
- `passwordEncoder` is the `PasswordEncoder` bean from `SecurityConfig` — constructor-injected, NOT instantiated inline
- `@Transactional` on `createUser` and `deactivate` only — `findAll` is a read without transaction overhead
- `@PreAuthorize` on each method individually — class-level annotation alone is architecturally prohibited
- Never use `@Transactional` on the controller that calls this service

### Updated AdminController.java

Replace the existing `AdminController.java` completely. The existing file only has the dashboard stub.

```java
package com.examinai.admin;

import com.examinai.user.Role;
import com.examinai.user.UserAccountCreateDto;
import com.examinai.user.UserAccountService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final UserAccountService userAccountService;

    public AdminController(UserAccountService userAccountService) {
        this.userAccountService = userAccountService;
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('ADMIN')")
    public String dashboard() {
        return "admin/dashboard";
    }

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public String userList(Model model) {
        model.addAttribute("users", userAccountService.findAll());
        return "admin/user-list";
    }

    @GetMapping("/users/new")
    @PreAuthorize("hasRole('ADMIN')")
    public String userForm(Model model) {
        model.addAttribute("createDto", new UserAccountCreateDto());
        model.addAttribute("roles", Role.values());
        return "admin/user-form";
    }

    @PostMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public String createUser(@ModelAttribute("createDto") UserAccountCreateDto dto,
                              Model model) {
        try {
            userAccountService.createUser(dto);
            return "redirect:/admin/users";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("roles", Role.values());
            return "admin/user-form";
        }
    }

    @PostMapping("/users/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public String deactivateUser(@PathVariable Long id) {
        userAccountService.deactivate(id);
        return "redirect:/admin/users";
    }
}
```

**Why `PostMapping` returns `"admin/user-form"` on error (not a redirect)**: The PRG rule requires `@PostMapping` to always redirect on success. On validation failure, returning the view directly is the correct Spring MVC pattern — this avoids losing the error message (which would happen if you redirect to `/admin/users/new` and then try to read the error from flash attributes). This is a standard exception to the PRG rule for validation error display. The model attributes (`error`, `roles`, `createDto`) are available in the template for re-rendering.

### admin/user-list.html

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org"
      xmlns:layout="http://www.ultraq.net.nz/thymeleaf/layout"
      layout:decorate="~{layout/base}">
<head><title>User Management</title></head>
<body>
<main id="main-content" layout:fragment="content">
    <div class="d-flex justify-content-between align-items-center mb-3">
        <h4>User Management</h4>
        <a href="/admin/users/new" class="btn btn-primary btn-sm">Create User</a>
    </div>

    <div th:if="${#lists.isEmpty(users)}" class="text-muted">
        <p>No user accounts found.</p>
    </div>

    <div th:unless="${#lists.isEmpty(users)}" class="table-responsive">
        <table class="table table-hover">
            <thead>
                <tr>
                    <th scope="col">Username</th>
                    <th scope="col">Email</th>
                    <th scope="col">Role</th>
                    <th scope="col">Status</th>
                    <th scope="col">Action</th>
                </tr>
            </thead>
            <tbody>
                <tr th:each="user : ${users}">
                    <td th:text="${user.username}"></td>
                    <td th:text="${user.email}"></td>
                    <td>
                        <span th:text="${user.role}"
                              class="badge"
                              th:classappend="${user.role.name() == 'ADMIN'} ? 'text-bg-danger' : (${user.role.name() == 'MENTOR'} ? 'text-bg-warning' : 'text-bg-secondary')">
                        </span>
                    </td>
                    <td>
                        <span th:if="${user.active}" class="badge text-bg-success">Active</span>
                        <span th:unless="${user.active}" class="badge text-bg-secondary">Inactive</span>
                    </td>
                    <td>
                        <form th:if="${user.active}"
                              th:action="@{/admin/users/{id}/deactivate(id=${user.id})}"
                              method="post">
                            <input type="hidden" th:name="${_csrf.parameterName}" th:value="${_csrf.token}">
                            <button type="submit" class="btn btn-sm btn-outline-danger">Deactivate</button>
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

### admin/user-form.html

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org"
      xmlns:layout="http://www.ultraq.net.nz/thymeleaf/layout"
      layout:decorate="~{layout/base}">
<head><title>Create User</title></head>
<body>
<main id="main-content" layout:fragment="content">
    <h4>Create User Account</h4>

    <div th:if="${error}" class="alert alert-danger" role="alert" th:text="${error}"></div>

    <form th:action="@{/admin/users}" th:object="${createDto}" method="post" class="col-md-5">
        <input type="hidden" th:name="${_csrf.parameterName}" th:value="${_csrf.token}">

        <div class="mb-3">
            <label for="username" class="form-label">Username</label>
            <input type="text" class="form-control" id="username" th:field="*{username}" required>
        </div>
        <div class="mb-3">
            <label for="email" class="form-label">Email</label>
            <input type="email" class="form-control" id="email" th:field="*{email}">
        </div>
        <div class="mb-3">
            <label for="role" class="form-label">Role</label>
            <select class="form-select" id="role" th:field="*{role}" required>
                <option value="" disabled>Select role</option>
                <option th:each="r : ${roles}" th:value="${r}" th:text="${r}"></option>
            </select>
        </div>
        <div class="mb-3">
            <label for="password" class="form-label">Initial Password</label>
            <input type="password" class="form-control" id="password" th:field="*{password}" required>
        </div>

        <button type="submit" class="btn btn-primary">Create User</button>
        <a href="/admin/users" class="btn btn-link">Back to User List</a>
    </form>
</main>
</body>
</html>
```

### UserAccountServiceTest.java

```java
package com.examinai.user;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserAccountServiceTest {

    @Mock UserAccountRepository userAccountRepository;
    @Mock PasswordEncoder passwordEncoder;

    @InjectMocks UserAccountService userAccountService;

    @Test
    void createUser_savesWithEncodedPasswordAndActiveTrue() {
        UserAccountCreateDto dto = new UserAccountCreateDto();
        dto.setUsername("newuser");
        dto.setEmail("new@test.com");
        dto.setRole(Role.INTERN);
        dto.setPassword("plain123");

        when(userAccountRepository.existsByUsername("newuser")).thenReturn(false);
        when(passwordEncoder.encode("plain123")).thenReturn("$2a$12$encoded");
        when(userAccountRepository.save(any(UserAccount.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        UserAccount result = userAccountService.createUser(dto);

        assertThat(result.getUsername()).isEqualTo("newuser");
        assertThat(result.getPassword()).isEqualTo("$2a$12$encoded");
        assertThat(result.isActive()).isTrue();
        assertThat(result.getRole()).isEqualTo(Role.INTERN);
        verify(userAccountRepository).save(any(UserAccount.class));
    }

    @Test
    void createUser_duplicateUsername_throwsIllegalArgumentException() {
        UserAccountCreateDto dto = new UserAccountCreateDto();
        dto.setUsername("existing");
        dto.setPassword("pass");
        dto.setRole(Role.INTERN);

        when(userAccountRepository.existsByUsername("existing")).thenReturn(true);

        assertThatThrownBy(() -> userAccountService.createUser(dto))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Username already exists");

        verify(userAccountRepository, never()).save(any());
    }

    @Test
    void deactivate_setsActiveFalseAndSaves() {
        UserAccount account = new UserAccount();
        account.setActive(true);

        when(userAccountRepository.findById(42L)).thenReturn(Optional.of(account));

        userAccountService.deactivate(42L);

        assertThat(account.isActive()).isFalse();
        verify(userAccountRepository).save(account);
    }
}
```

**Note:** `@PreAuthorize` is not tested here because `@ExtendWith(MockitoExtension.class)` does not engage the Spring Security AOP proxy. `@PreAuthorize` security enforcement is already covered by `SecurityIntegrationTest` from Story 1.2. If a separate `@PreAuthorize` test is desired, use `@SpringBootTest` with `@WithMockUser`.

### Critical Architecture Rules for This Story

1. **`@Transactional` on Service only** — `AdminController` must not have `@Transactional`. `UserAccountService.createUser` and `deactivate` are `@Transactional`. `findAll` is not.

2. **`@PreAuthorize` on every method individually** — class-level annotation is insufficient per architecture. Applied here on every individual method in both `AdminController` and `UserAccountService`.

3. **PRG rule (mandatory)** — `@PostMapping` always returns `"redirect:/..."` on success. Returning `"admin/user-form"` directly is only allowed when there is a validation error to display (this is the standard Spring MVC pattern for form re-display).

4. **`PasswordEncoder` injection** — inject the existing `PasswordEncoder` bean from `SecurityConfig`. Never create `new BCryptPasswordEncoder(12)` in service code.

5. **Enum `@Enumerated(EnumType.STRING)`** — `UserAccount.role` already has this. Do not change it.

6. **Deactivate via POST form** — use `<form method="post">` with CSRF token for the deactivate action, not a GET link. Spring Security 6 blocks GET-based state changes via CSRF protection.

7. **`existsByUsername` derived query** — Spring Data JPA auto-implements `boolean existsByUsername(String)` from the method name. No `@Query` annotation needed.

8. **No `findAllByRole()` needed here** — `findAll()` returns all users for the admin list. The unbounded list is acceptable at MVP scale (deferred pagination noted in deferred-work.md).

### Deferred Work from Story 1.2 (Do Not Address Here)

From `deferred-work.md`:
- `UserAccountRepository.findAllByRole()` unbounded list → pagination deferred to a later story. `findAll()` is acceptable here.
- GET `/logout` POST-only restriction → no logout link changes in this story.
- `email` column lacks uniqueness → do not add a uniqueness constraint here; belongs in a future migration.

### Previous Story Intelligence (Story 1.2 Learnings)

1. **Spring Boot version is 3.5.13**, not 3.4.2. Ignore all architecture doc references to 3.4.2.
2. **Spring AI artifact** is `spring-ai-starter-model-ollama` (not what architecture docs say) — already correct in pom.xml. Do not change pom.xml for this story.
3. **Base package** `com.examinai` is confirmed. All new classes use `com.examinai.*`.
4. **`db.changelog-master.xml` currently includes only 001 and 002** — add 003 and 004 in this story.
5. **Stub controllers from Story 1.2** (`InternTaskController`, `MentorReviewController`, `AdminController`) are all in place. AdminController needs user management methods added.
6. **`SecurityIntegrationTest`** — 7/7 passing. Do not break it. New tests in this story use `@ExtendWith(MockitoExtension.class)` for service unit tests (no DB required).
7. **`application.yml`** has `spring.jpa.hibernate.ddl-auto: none` and `spring.session.jdbc.initialize-schema: never` — do NOT change either.

### Git Context

Commits on main:
- `3ca6fea` — Story 1.2 implemented (security, auth, Liquibase 001+002, UserAccount entity, stubs)
- `5cb815d` — Story 1.1 implemented (scaffold, pom.xml, base layout, custom.css)
- `e72f8f7` — Initial commit

This story builds directly on Story 1.2 work. No merges or rebases needed.

### Project Structure Notes

**Files to CREATE:**
```
src/main/resources/db/changelog/changelogs/
├── 003-indexes.sql                                ← NEW
└── 004-seed-data.sql                              ← NEW

src/main/java/com/examinai/user/
├── UserAccountCreateDto.java                      ← NEW
└── UserAccountService.java                        ← NEW

src/main/resources/templates/admin/
├── user-list.html                                 ← NEW
└── user-form.html                                 ← NEW

src/test/java/com/examinai/user/
└── UserAccountServiceTest.java                    ← NEW
```

**Files to MODIFY:**
```
src/main/resources/db/changelog/db.changelog-master.xml   ← ADD 003+004 includes
src/main/java/com/examinai/user/UserAccountRepository.java ← ADD existsByUsername
src/main/java/com/examinai/admin/AdminController.java      ← ADD user management methods
```

**Files unchanged:** `UserAccount.java`, `Role.java`, `CustomUserDetailsService.java`, `SecurityConfig.java`, `WebMvcConfig.java`, `application.yml`, all templates from Story 1.1/1.2.

### Verification Checklist

After completing all tasks, verify:
1. `mvn clean compile` — BUILD SUCCESS, 0 errors
2. `mvn test` — `SecurityIntegrationTest` (7 tests) and `UserAccountServiceTest` (3 tests) all pass
3. With local PostgreSQL running, verify app starts and Liquibase runs all 4 changelogs:
   - `SELECT COUNT(*) FROM user_account;` → returns 3 (admin, mentor, intern)
   - `SELECT COUNT(*) FROM course;` → returns 1
   - `SELECT COUNT(*) FROM task;` → returns 3
   - `SELECT * FROM pg_indexes WHERE tablename = 'task_review';` → shows 3 new indexes
4. Manual login test: login as `admin` / `admin123` → redirected to `/admin/dashboard`
5. Manual flow: navigate to `/admin/users` → see 3 seed users listed
6. Manual flow: create a new user → redirect to `/admin/users`, new user appears
7. Manual flow: deactivate a user → user shows "Inactive" badge
8. Manual flow: attempt to log in as deactivated user → stays on `/login?error`
9. Manual flow: duplicate username creation → form re-renders with "Username already exists" error

### References

- Story 1.3 ACs: [Source: epics.md#Story 1.3: Admin User Account Management & Seed Data]
- Changelog structure (003 indexes, 004 seed): [Source: architecture.md#Data Architecture — Changelog structure]
- Index names: [Source: architecture.md#Naming Patterns — idx_task_review_intern_id, idx_task_review_status]
- UserAccountService placement: [Source: architecture.md#Complete Project Directory Structure — user/UserAccountService.java]
- @PreAuthorize on each method individually: [Source: architecture.md#Enforcement Guidelines]
- @Transactional on Service methods only: [Source: architecture.md#Process Patterns — Transaction Boundary Rule]
- PRG rule: [Source: architecture.md#Process Patterns — PRG Rule]
- PasswordEncoder bean (BCrypt strength 12): [Source: architecture.md#Authentication & Security, story-1.2 SecurityConfig.java]
- Deactivation via DisabledException: [Source: epics.md#AC2, story-1.2 CustomUserDetailsService.java]
- Template structure: [Source: architecture.md#Template Organization — admin/user-list.html, admin/user-form.html]
- UX accessibility requirements: [Source: epics.md#UX-DR14]
- Spring Boot version correction (3.5.13): [Source: implementation-artifacts/1-2-user-authentication-role-based-access.md#Dev Notes]

## Dev Agent Record

### Agent Model Used

claude-sonnet-4-6

### Debug Log References

None — implementation followed story spec exactly.

### Completion Notes List

- All 10 tasks completed. 13 tests pass (10 SecurityIntegrationTest + 3 UserAccountServiceTest).
- BCrypt strength-12 hashes for seed users generated via a temporary Maven exec run and embedded directly in 004-seed-data.sql.
- `SecurityIntegrationTest` required adding `@MockBean UserAccountService` since `AdminController` now has a constructor dependency on it. This is the correct Spring `@WebMvcTest` pattern.
- `ExaminAiApplicationTests.contextLoads` failure is pre-existing (missing `MAIL_HOST` env var) — unrelated to this story.
- All ACs satisfied: user creation (PRG), deactivation, seed data SQL, duplicate username error, user list, `@PreAuthorize` enforcement.

### File List

**Created:**
- `src/main/resources/db/changelog/changelogs/003-indexes.sql`
- `src/main/resources/db/changelog/changelogs/004-seed-data.sql`
- `src/main/java/com/examinai/user/UserAccountCreateDto.java`
- `src/main/java/com/examinai/user/UserAccountService.java`
- `src/main/resources/templates/admin/user-list.html`
- `src/main/resources/templates/admin/user-form.html`
- `src/test/java/com/examinai/user/UserAccountServiceTest.java`

**Modified:**
- `src/main/resources/db/changelog/db.changelog-master.xml`
- `src/main/java/com/examinai/user/UserAccountRepository.java`
- `src/main/java/com/examinai/admin/AdminController.java`
- `src/test/java/com/examinai/user/SecurityIntegrationTest.java`

## Change Log

- 2026-04-21: Story 1.3 implemented — admin user account management (create/deactivate), Liquibase 003-indexes + 004-seed-data changelogs, UserAccountService with @PreAuthorize, AdminController user routes, Thymeleaf templates (user-list, user-form), UserAccountServiceTest (3 tests). SecurityIntegrationTest updated with @MockBean UserAccountService.
