# Story 1.2: User Authentication & Role-Based Access

Status: done

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

As an authenticated user (intern, mentor, or admin),
I want to log in with my username and password and be directed to my role's dashboard,
So that I can immediately access features relevant to my role without unnecessary navigation.

## Acceptance Criteria

**AC1 — Intern login redirects to /intern/tasks:**
Given a user account exists with role INTERN
When they submit valid credentials on `/login`
Then they are redirected to `/intern/tasks`
And their session is persisted via Spring Session JDBC in the `spring_session` table

**AC2 — Mentor login redirects to /mentor/reviews:**
Given a user account exists with role MENTOR
When they submit valid credentials on `/login`
Then they are redirected to `/mentor/reviews`

**AC3 — Admin login redirects to /admin/dashboard:**
Given a user account exists with role ADMIN
When they submit valid credentials on `/login`
Then they are redirected to `/admin/dashboard`

**AC4 — Unauthenticated access redirects to /login:**
Given an unauthenticated user attempts to access any protected route (e.g. `/intern/tasks`)
When the request is received
Then they are redirected to `/login` with no error shown

**AC5 — Cross-role 403 enforcement:**
Given an intern is authenticated
When they attempt to access `/mentor/reviews` directly
Then a 403 response is returned — enforced by `@PreAuthorize("hasRole('MENTOR') or hasRole('ADMIN')")` on the controller method, not by Thymeleaf alone

**AC6 — Invalid credentials stay on /login:**
Given invalid credentials are submitted on the login form
When Spring Security processes the authentication
Then the user remains on `/login` with an authentication failure message and no session is created

**AC7 — Liquibase 001-init-schema.sql:**
Given Liquibase changelog `001-init-schema.sql` defines the `user_account` table (id BIGSERIAL PK, username VARCHAR UNIQUE NOT NULL, password VARCHAR NOT NULL, email VARCHAR, role VARCHAR NOT NULL, active BOOLEAN NOT NULL, date_created TIMESTAMP NOT NULL)
When the application starts
Then the table is created before the first HTTP request is served

**AC8 — Liquibase 002-spring-session.sql:**
Given Liquibase changelog `002-spring-session.sql` defines `spring_session` and `spring_session_attributes` tables
When the application starts
Then both tables exist and Spring Session can persist sessions immediately

**AC9 — Session timeout 1 hour:**
Given a user has been idle for 1 hour (`server.servlet.session.timeout=1h`)
When they attempt to make a request
Then their session is expired and they are redirected to `/login`

## Tasks / Subtasks

- [x] Task 1: Create Liquibase 001-init-schema.sql (AC: 7)
  - [x] Create `src/main/resources/db/changelog/changelogs/001-init-schema.sql`
  - [x] Define: `user_account` table (id BIGSERIAL PK, username UNIQUE NOT NULL, password NOT NULL, email, role NOT NULL, active NOT NULL, date_created TIMESTAMP NOT NULL)
  - [x] Define: `course` table (id BIGSERIAL PK, course_name NOT NULL, technology, date_created TIMESTAMP NOT NULL)
  - [x] Define: `task` table (id BIGSERIAL PK, task_name NOT NULL, task_description TEXT, course_id FK→course.id, mentor_id FK→user_account.id, date_created TIMESTAMP NOT NULL)
  - [x] Define: `task_review` table (id BIGSERIAL PK, task_id FK, intern_id FK, mentor_id FK, status NOT NULL, llm_result, mentor_result, mentor_remarks TEXT, error_message VARCHAR(500), date_created TIMESTAMP NOT NULL)
  - [x] Define: `task_review_issue` table (id BIGSERIAL PK, task_review_id FK, line INTEGER, code TEXT, issue TEXT NOT NULL, improvement TEXT)

- [x] Task 2: Create Liquibase 002-spring-session.sql (AC: 8)
  - [x] Create `src/main/resources/db/changelog/changelogs/002-spring-session.sql`
  - [x] Include Spring Session JDBC schema for PostgreSQL: SPRING_SESSION + SPRING_SESSION_ATTRIBUTES tables with indexes

- [x] Task 3: Update db.changelog-master.xml to include both changelogs (AC: 7, 8)
  - [x] Add `<include file="changelogs/001-init-schema.sql" relativeToChangelogFile="true"/>`
  - [x] Add `<include file="changelogs/002-spring-session.sql" relativeToChangelogFile="true"/>`
  - [x] Verify order: 001 → 002 (strictly)
  - [x] DO NOT add 003 or 004 — those belong to Story 1.3

- [x] Task 4: Create Role enum (AC: 7)
  - [x] Create `src/main/java/com/examinai/user/Role.java`
  - [x] Values: INTERN, MENTOR, ADMIN

- [x] Task 5: Create UserAccount entity (AC: 7)
  - [x] Create `src/main/java/com/examinai/user/UserAccount.java`
  - [x] Fields: id (BIGSERIAL), username, password, email, role (enum), active (boolean), dateCreated (LocalDateTime)
  - [x] Use `@Column(name="snake_case")` for all fields
  - [x] Use `@Enumerated(EnumType.STRING)` on role field
  - [x] Add `@PrePersist` to set dateCreated

- [x] Task 6: Create UserAccountRepository (AC: 7)
  - [x] Create `src/main/java/com/examinai/user/UserAccountRepository.java`
  - [x] Extends `JpaRepository<UserAccount, Long>`
  - [x] Add: `Optional<UserAccount> findByUsername(String username)`
  - [x] Add: `List<UserAccount> findAllByRole(Role role)` (needed by Story 1.3 admin UI)

- [x] Task 7: Create CustomUserDetailsService (AC: 1, 6)
  - [x] Create `src/main/java/com/examinai/user/CustomUserDetailsService.java`
  - [x] Implements `UserDetailsService`
  - [x] `loadUserByUsername`: find by username → throw `UsernameNotFoundException` if not found
  - [x] Throw `DisabledException` if `account.isActive()` is false (Story 1.3 deactivation)
  - [x] Build `UserDetails` with `roles(account.getRole().name())`

- [x] Task 8: Create SecurityConfig (AC: 1, 2, 3, 4, 5, 6, 9)
  - [x] Create `src/main/java/com/examinai/config/SecurityConfig.java`
  - [x] Annotate with `@Configuration`, `@EnableWebSecurity`, `@EnableMethodSecurity`
  - [x] `SecurityFilterChain` bean: permit `/webjars/**`, `/css/**`, `/js/**`, `/actuator/health`, `/login`
  - [x] `.requestMatchers("/intern/**").hasAnyRole("INTERN","ADMIN")`
  - [x] `.requestMatchers("/mentor/**").hasAnyRole("MENTOR","ADMIN")`
  - [x] `.requestMatchers("/admin/**").hasRole("ADMIN")`
  - [x] `.anyRequest().authenticated()`
  - [x] `formLogin()` with custom login page `/login`, role-based success handler, failure URL `/login?error`
  - [x] `logout()` with `logoutSuccessUrl("/login?logout")`
  - [x] `BCryptPasswordEncoder` bean at strength 12

- [x] Task 9: Create login.html (AC: 6)
  - [x] Create `src/main/resources/templates/auth/login.html`
  - [x] Uses base layout (Thymeleaf Layout Dialect) OR standalone — see Dev Notes
  - [x] Show `?error` parameter → error alert "Invalid credentials. Please try again."
  - [x] Show `?logout` parameter → success message "You have been logged out."
  - [x] Username + password fields with explicit `<label for="...">` on each
  - [x] `<form method="post" th:action="@{/login}">` with CSRF hidden input
  - [x] Submit button

- [x] Task 10: Create stub dashboard controllers and templates (AC: 1, 2, 3, 5)
  - [x] Create `src/main/java/com/examinai/task/InternTaskController.java` (stub for `/intern/tasks`)
  - [x] Create `src/main/java/com/examinai/review/MentorReviewController.java` (stub for `/mentor/reviews`)
  - [x] Create `src/main/java/com/examinai/admin/AdminController.java` (stub for `/admin/dashboard`)
  - [x] Each annotated with `@PreAuthorize` per role (INTERN, MENTOR, ADMIN respectively)
  - [x] Create placeholder Thymeleaf templates for each landing page
  - [x] Stubs return "coming soon" text — will be replaced in stories 1.3, 2.x, etc.

- [x] Task 11: Fix deferred MAIL_PORT startup issue (Deferred from Story 1.1)
  - [x] Open `src/main/resources/application.yml`
  - [x] Change `port: ${MAIL_PORT}` → `port: ${MAIL_PORT:587}` to prevent Integer type-conversion failure when MAIL_PORT is unset
  - [x] Verify `mvn clean compile` still passes

- [x] Task 12: Write SecurityIntegrationTest (AC: 4, 5)
  - [x] Create `src/test/java/com/examinai/user/SecurityIntegrationTest.java`
  - [x] `@WebMvcTest` + `@Import(SecurityConfig.class)` + `@MockBean UserAccountRepository`
  - [x] Test: unauthenticated GET `/intern/tasks` → 302 redirect to login
  - [x] Test: `@WithMockUser(roles="INTERN")` GET `/mentor/reviews` → 403
  - [x] Test: `@WithMockUser(roles="MENTOR")` GET `/intern/tasks` → 403
  - [x] Test: `@WithMockUser(roles="MENTOR")` GET `/mentor/reviews` → 200
  - [x] Test: `@WithMockUser(roles="ADMIN")` GET `/admin/dashboard` → 200

## Dev Notes

### Spring Boot Version — CRITICAL
Story 1.1 used Spring Boot **3.5.13** (start.spring.io no longer offers 3.4.2). All code must be written for Spring Boot 3.5.13 + Spring Security 6.x. The `pom.xml` parent is `spring-boot-starter-parent:3.5.13`.

The Spring AI artifact was renamed: **use `spring-ai-starter-model-ollama`** (NOT `spring-ai-ollama-spring-boot-starter`). This is already in `pom.xml` from Story 1.1.

### Package Structure
All new classes use **package-by-feature** under `com.examinai`. Story 1.1 created only:
- `com.examinai.ExaminAiApplication`
- `com.examinai.config.WebMvcConfig`

This story adds: `com.examinai.user.*`, `com.examinai.config.SecurityConfig`, and stub controllers in `com.examinai.task`, `com.examinai.review`, `com.examinai.admin`.

### 001-init-schema.sql (complete Liquibase formatted SQL)

Per architecture, ALL 5 domain tables go into 001-init-schema.sql (not split by story):

```sql
--liquibase formatted sql

--changeset dev:001-init-schema dbms:postgresql
CREATE TABLE user_account (
    id          BIGSERIAL PRIMARY KEY,
    username    VARCHAR(100) NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,
    email       VARCHAR(255),
    role        VARCHAR(20)  NOT NULL,
    active      BOOLEAN      NOT NULL DEFAULT true,
    date_created TIMESTAMP   NOT NULL DEFAULT now()
);

CREATE TABLE course (
    id           BIGSERIAL PRIMARY KEY,
    course_name  VARCHAR(255) NOT NULL,
    technology   VARCHAR(100),
    date_created TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE TABLE task (
    id               BIGSERIAL PRIMARY KEY,
    task_name        VARCHAR(255) NOT NULL,
    task_description TEXT,
    course_id        BIGINT NOT NULL REFERENCES course(id) ON DELETE CASCADE,
    mentor_id        BIGINT NOT NULL REFERENCES user_account(id),
    date_created     TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE task_review (
    id              BIGSERIAL PRIMARY KEY,
    task_id         BIGINT NOT NULL REFERENCES task(id) ON DELETE CASCADE,
    intern_id       BIGINT NOT NULL REFERENCES user_account(id),
    mentor_id       BIGINT REFERENCES user_account(id),
    status          VARCHAR(20)  NOT NULL,
    llm_result      VARCHAR(20),
    mentor_result   VARCHAR(20),
    mentor_remarks  TEXT,
    error_message   VARCHAR(500),
    date_created    TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE task_review_issue (
    id             BIGSERIAL PRIMARY KEY,
    task_review_id BIGINT NOT NULL REFERENCES task_review(id) ON DELETE CASCADE,
    line           INTEGER,
    code           TEXT,
    issue          TEXT NOT NULL,
    improvement    TEXT
);
```

### 002-spring-session.sql (Spring Session JDBC schema for PostgreSQL)

```sql
--liquibase formatted sql

--changeset dev:002-spring-session dbms:postgresql
CREATE TABLE SPRING_SESSION (
    PRIMARY_ID            CHAR(36)     NOT NULL,
    SESSION_ID            CHAR(36)     NOT NULL,
    CREATION_TIME         BIGINT       NOT NULL,
    LAST_ACCESS_TIME      BIGINT       NOT NULL,
    MAX_INACTIVE_INTERVAL INT          NOT NULL,
    EXPIRY_TIME           BIGINT       NOT NULL,
    PRINCIPAL_NAME        VARCHAR(100),
    CONSTRAINT SPRING_SESSION_PK PRIMARY KEY (PRIMARY_ID)
);

CREATE UNIQUE INDEX SPRING_SESSION_IX1 ON SPRING_SESSION (SESSION_ID);
CREATE INDEX SPRING_SESSION_IX2 ON SPRING_SESSION (EXPIRY_TIME);
CREATE INDEX SPRING_SESSION_IX3 ON SPRING_SESSION (PRINCIPAL_NAME);

CREATE TABLE SPRING_SESSION_ATTRIBUTES (
    SESSION_PRIMARY_ID CHAR(36)      NOT NULL,
    ATTRIBUTE_NAME     VARCHAR(200)  NOT NULL,
    ATTRIBUTE_BYTES    BYTEA         NOT NULL,
    CONSTRAINT SPRING_SESSION_ATTRIBUTES_PK PRIMARY KEY (SESSION_PRIMARY_ID, ATTRIBUTE_NAME),
    CONSTRAINT SPRING_SESSION_ATTRIBUTES_FK FOREIGN KEY (SESSION_PRIMARY_ID)
        REFERENCES SPRING_SESSION(PRIMARY_ID) ON DELETE CASCADE
);
```

**Why `initialize-schema: never`:** Spring Session can auto-create these tables. We set it to `never` because Liquibase owns schema management. If set to `always`, Spring Session will attempt DDL before Liquibase runs and may conflict.

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

    <!--
        003-indexes.sql     (Story 1.3) — performance indexes
        004-seed-data.sql   (Story 1.3) — admin/mentor/intern seed accounts + sample course/tasks
        DO NOT add those includes here — they belong in Story 1.3
    -->

</databaseChangeLog>
```

### Role.java

```java
package com.examinai.user;

public enum Role {
    INTERN, MENTOR, ADMIN
}
```

### UserAccount.java

```java
package com.examinai.user;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_account")
public class UserAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", unique = true, nullable = false)
    private String username;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "email")
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private Role role;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "date_created", nullable = false)
    private LocalDateTime dateCreated;

    @PrePersist
    protected void onCreate() {
        dateCreated = LocalDateTime.now();
    }

    // Getters and setters for all fields
    public Long getId() { return id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public LocalDateTime getDateCreated() { return dateCreated; }
}
```

**Critical:** `@Enumerated(EnumType.STRING)` — never `ORDINAL`. Field name `dateCreated` maps to column `date_created`.

### UserAccountRepository.java

```java
package com.examinai.user;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {
    Optional<UserAccount> findByUsername(String username);
    List<UserAccount> findAllByRole(Role role);
}
```

### CustomUserDetailsService.java

```java
package com.examinai.user;

import org.springframework.security.core.userdetails.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserAccountRepository userAccountRepository;

    public CustomUserDetailsService(UserAccountRepository userAccountRepository) {
        this.userAccountRepository = userAccountRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserAccount account = userAccountRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        if (!account.isActive()) {
            throw new DisabledException("Account is disabled");
        }

        return new User(
            account.getUsername(),
            account.getPassword(),
            List.of(new SimpleGrantedAuthority("ROLE_" + account.getRole().name()))
        );
    }
}
```

**Why `"ROLE_" + account.getRole().name()`:** Spring Security's `hasRole()` strips the `ROLE_` prefix internally, so the authority must include it. Using `User.builder().roles(role)` would work too, but explicit `SimpleGrantedAuthority` is clearer.

### SecurityConfig.java

```java
package com.examinai.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import java.io.IOException;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/webjars/**", "/css/**", "/js/**").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/login", "/logout").permitAll()
                .requestMatchers("/intern/**").hasAnyRole("INTERN", "ADMIN")
                .requestMatchers("/mentor/**").hasAnyRole("MENTOR", "ADMIN")
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .successHandler(roleBasedSuccessHandler())
                .failureUrl("/login?error")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public AuthenticationSuccessHandler roleBasedSuccessHandler() {
        return (HttpServletRequest request, HttpServletResponse response, Authentication authentication) -> {
            String role = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .orElse("ROLE_INTERN");
            String targetUrl = switch (role) {
                case "ROLE_ADMIN"   -> "/admin/dashboard";
                case "ROLE_MENTOR"  -> "/mentor/reviews";
                default             -> "/intern/tasks";
            };
            response.sendRedirect(request.getContextPath() + targetUrl);
        };
    }
}
```

**`@EnableMethodSecurity`** enables `@PreAuthorize` across the application. Without it, `@PreAuthorize` annotations are silently ignored. This annotation replaces `@EnableGlobalMethodSecurity(prePostEnabled=true)` from Spring Security 5.x.

### login.html

The login page is standalone (does not extend base layout) because `sec:authentication` would NPE when unauthenticated. This is the documented Spring Security pattern.

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>ExaminAI — Login</title>
    <link rel="stylesheet" th:href="@{/webjars/bootstrap/5.3.3/css/bootstrap.min.css}">
    <link rel="stylesheet" th:href="@{/css/custom.css}">
</head>
<body class="bg-light">
    <div class="container py-5">
        <div class="row justify-content-center">
            <div class="col-md-4">
                <div class="card shadow-sm">
                    <div class="card-body p-4">
                        <h1 class="h4 mb-4 text-center fw-bold">ExaminAI</h1>

                        <div th:if="${param.error}" class="alert alert-danger" role="alert">
                            Invalid credentials. Please try again.
                        </div>
                        <div th:if="${param.logout}" class="alert alert-success" role="alert">
                            You have been logged out.
                        </div>

                        <form method="post" th:action="@{/login}">
                            <input type="hidden" th:name="${_csrf.parameterName}" th:value="${_csrf.token}">
                            <div class="mb-3">
                                <label for="username" class="form-label">Username</label>
                                <input type="text" class="form-control" id="username" name="username"
                                       required autofocus>
                            </div>
                            <div class="mb-3">
                                <label for="password" class="form-label">Password</label>
                                <input type="password" class="form-control" id="password" name="password"
                                       required>
                            </div>
                            <button type="submit" class="btn btn-primary w-100">Sign In</button>
                        </form>
                    </div>
                </div>
            </div>
        </div>
    </div>
</body>
</html>
```

**Why standalone (not extending base layout):** The base layout uses `sec:authentication` to render the navbar (role badge, username, logout). When the user is not authenticated, `sec:authentication` evaluates against an anonymous authentication, which can produce null values or unexpected output. The login page avoids this by not extending base.html.

### Stub Controllers

**InternTaskController.java** (`com.examinai.task`):

```java
package com.examinai.task;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/intern")
public class InternTaskController {

    @GetMapping("/tasks")
    @PreAuthorize("hasRole('INTERN') or hasRole('ADMIN')")
    public String taskList() {
        return "intern/task-list";
    }
}
```

**MentorReviewController.java** (`com.examinai.review`):

```java
package com.examinai.review;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/mentor")
public class MentorReviewController {

    @GetMapping("/reviews")
    @PreAuthorize("hasRole('MENTOR') or hasRole('ADMIN')")
    public String reviewQueue() {
        return "mentor/review-queue";
    }
}
```

**AdminController.java** (`com.examinai.admin`):

```java
package com.examinai.admin;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('ADMIN')")
    public String dashboard() {
        return "admin/dashboard";
    }
}
```

### Stub Templates

Create these three minimal templates (extend base.html to verify navbar rendering):

**`templates/intern/task-list.html`** (stub):
```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org"
      xmlns:layout="http://www.ultraq.net.nz/thymeleaf/layout"
      layout:decorate="~{layout/base}">
<head><title>Tasks</title></head>
<body>
<main id="main-content" layout:fragment="content">
    <h4>Intern Task List</h4>
    <p class="text-muted">Task list coming in Story 2.3.</p>
</main>
</body>
</html>
```

**`templates/mentor/review-queue.html`** (stub):
```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org"
      xmlns:layout="http://www.ultraq.net.nz/thymeleaf/layout"
      layout:decorate="~{layout/base}">
<head><title>Reviews</title></head>
<body>
<main id="main-content" layout:fragment="content">
    <h4>Mentor Review Queue</h4>
    <p class="text-muted">Review queue coming in Story 4.1.</p>
</main>
</body>
</html>
```

**`templates/admin/dashboard.html`** (stub):
```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org"
      xmlns:layout="http://www.ultraq.net.nz/thymeleaf/layout"
      layout:decorate="~{layout/base}">
<head><title>Admin Dashboard</title></head>
<body>
<main id="main-content" layout:fragment="content">
    <h4>Admin Dashboard</h4>
    <p class="text-muted">Admin dashboard coming in Story 5.2.</p>
</main>
</body>
</html>
```

### application.yml Fix (Deferred from Story 1.1)

Change the MAIL_PORT binding to have a default to prevent `Integer` type-conversion failure at startup:

```yaml
# Before (causes startup failure when MAIL_PORT env var is empty):
port: ${MAIL_PORT}

# After (safe default):
port: ${MAIL_PORT:587}
```

The Ollama base URL is already fixed in Story 1.1 (`${OLLAMA_BASE_URL:http://ollama:11434}`). Apply the same pattern to MAIL_PORT.

### Critical Architecture Rules for This Story

1. **`@Transactional` placement:** Must NOT appear on `SecurityConfig` or any `@Controller`. Only on `@Service` methods (not needed in this story, but don't violate this when creating stubs).

2. **`@PreAuthorize` on every controller method individually** — class-level `@PreAuthorize` alone is insufficient per architecture mandate. Even if `SecurityConfig` URL-pattern restrictions are set, method-level `@PreAuthorize` is the authoritative enforcement.

3. **BCrypt strength 12** — not 10 (Spring Security default). Use `new BCryptPasswordEncoder(12)`.

4. **Spring Session `initialize-schema: never`** — already set in `application.yml` from Story 1.1. Never change this.

5. **`spring.jpa.hibernate.ddl-auto: none`** — already set in `application.yml`. Never change. Liquibase is sole schema manager.

6. **PRG pattern** — not relevant to this story (no form handlers except Spring Security's own `/login`), but note it for all subsequent stories.

7. **No secrets in source** — `SecurityConfig` references no hardcoded passwords. BCrypt encoding happens in Story 1.3's `UserAccountService.createUser()`.

### Testing Standards

Architecture specifies `SecurityIntegrationTest` using `@SpringBootTest`. For Story 1.2, use `@WebMvcTest` to keep tests fast and avoid PostgreSQL dependency:

```java
package com.examinai.user;

import com.examinai.admin.AdminController;
import com.examinai.config.SecurityConfig;
import com.examinai.review.MentorReviewController;
import com.examinai.task.InternTaskController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {InternTaskController.class, MentorReviewController.class, AdminController.class})
@Import(SecurityConfig.class)
class SecurityIntegrationTest {

    @Autowired MockMvc mockMvc;

    @MockBean CustomUserDetailsService customUserDetailsService;

    @Test
    void unauthenticatedRedirectsToLogin() throws Exception {
        mockMvc.perform(get("/intern/tasks"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @WithMockUser(roles = "INTERN")
    void internAccessesOwnRoutes() throws Exception {
        mockMvc.perform(get("/intern/tasks"))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "INTERN")
    void internCannotAccessMentorRoutes() throws Exception {
        mockMvc.perform(get("/mentor/reviews"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "MENTOR")
    void mentorCannotAccessInternRoutes() throws Exception {
        mockMvc.perform(get("/intern/tasks"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "MENTOR")
    void mentorAccessesOwnRoutes() throws Exception {
        mockMvc.perform(get("/mentor/reviews"))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminAccessesDashboard() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanAccessInternRoutes() throws Exception {
        mockMvc.perform(get("/intern/tasks"))
            .andExpect(status().isOk());
    }
}
```

**Note:** `@MockBean CustomUserDetailsService` prevents Spring from trying to wire a real PostgreSQL connection in tests. The `@WebMvcTest` slice does not start the full application context.

### Previous Story Learnings (Story 1.1 — Critical)

1. **Spring Boot 3.5.13** is in use (not 3.4.2). All architecture docs reference 3.4.2 but start.spring.io only offers 3.5.x+. Treat all Boot-version-sensitive claims with this in mind.

2. **Spring AI artifact rename:** `spring-ai-starter-model-ollama` (not `spring-ai-ollama-spring-boot-starter`). Already correct in `pom.xml`.

3. **Base package:** `com.examinai` (NOT `com.examinai.examin_ai` which was Initializr's default). Story 1.1 moved the entry point. New classes must use `com.examinai.*`.

4. **`application.yml` uses `${OLLAMA_BASE_URL:http://ollama:11434}`** — the same default-value pattern needed for `MAIL_PORT:587`.

5. **Only 2 Java files currently exist:** `ExaminAiApplication.java` and `WebMvcConfig.java`. All user, security, controller, and template files need to be created from scratch.

6. **`db.changelog-master.xml` currently has no `<include>` entries** — the app will start without schema (and fail authentication). This story creates the changelogs and includes them.

7. **CSRF null dereference on error pages** is a known deferred issue. The fix (error-specific CSRF handling) is deferred to Story 3.3 when error.html is created. Do not add `_csrf` references to templates that are used as Spring MVC error views.

### Verification Checklist

After completing all tasks, verify:
1. `mvn clean compile` — BUILD SUCCESS, 0 errors
2. `mvn test` — SecurityIntegrationTest passes (requires only MockMvc, no DB)
3. With local PostgreSQL running, verify app starts and Liquibase runs both changelogs:
   - `SELECT table_name FROM information_schema.tables WHERE table_schema='public';` → shows all 7 tables (5 domain + 2 session)
4. Manual login test (requires seed data from Story 1.3 — not available yet, but schema must exist)
5. `spring.session.jdbc.initialize-schema: never` must remain in `application.yml` — Liquibase owns those tables

### Project Structure Notes

Files created in this story:
```
src/main/resources/db/changelog/changelogs/
├── 001-init-schema.sql            ← NEW
└── 002-spring-session.sql         ← NEW

src/main/resources/db/changelog/
└── db.changelog-master.xml        ← UPDATED (add 2 includes)

src/main/resources/templates/
├── auth/
│   └── login.html                 ← NEW (standalone, no base layout)
├── intern/
│   └── task-list.html             ← NEW (stub, extends base)
├── mentor/
│   └── review-queue.html          ← NEW (stub, extends base)
└── admin/
    └── dashboard.html             ← NEW (stub, extends base)

src/main/java/com/examinai/
├── user/
│   ├── Role.java                  ← NEW
│   ├── UserAccount.java           ← NEW
│   ├── UserAccountRepository.java ← NEW
│   └── CustomUserDetailsService.java ← NEW
├── config/
│   └── SecurityConfig.java        ← NEW
├── task/
│   └── InternTaskController.java  ← NEW (stub)
├── review/
│   └── MentorReviewController.java ← NEW (stub)
└── admin/
    └── AdminController.java       ← NEW (stub)

src/main/resources/
└── application.yml                ← MODIFIED (MAIL_PORT default)

src/test/java/com/examinai/
└── user/
    └── SecurityIntegrationTest.java ← NEW
```

### References

- Story 1.2 ACs: [Source: epics.md#Story 1.2: User Authentication & Role-Based Access]
- SecurityConfig pattern: [Source: architecture.md#Authentication & Security]
- `@EnableMethodSecurity`: [Source: architecture.md#Enforcement Guidelines — @PreAuthorize on every controller method]
- BCrypt strength 12: [Source: architecture.md#Authentication & Security, epics.md#NFR6]
- Spring Session `initialize-schema: never`: [Source: architecture.md#Data Architecture, story-1.1 Dev Notes]
- 001-init-schema.sql table definitions: [Source: architecture.md#Data Architecture — Changelog structure, epics.md#Story 2.3 AC, Story 2.2 AC]
- Role-based URL namespaces: [Source: architecture.md#URL Naming Conventions, #Authentication & Security]
- PRG pattern (not applicable here, reminder for future stories): [Source: architecture.md#Process Patterns]
- Login page standalone pattern: [Source: epics.md#UX-DR9 — base layout, ux-design-specification.md#Experience Principles]
- MAIL_PORT fix: [Source: implementation-artifacts/deferred-work.md]

## Dev Agent Record

### Agent Model Used

claude-sonnet-4-6

### Debug Log References

None — implementation completed cleanly on first pass.

### Completion Notes List

- All 12 tasks implemented following story spec exactly.
- `SecurityIntegrationTest`: 7/7 tests pass using `@WebMvcTest` (no DB required). Tests cover: unauthenticated redirect to login, INTERN/MENTOR cross-role 403, INTERN own routes 200, MENTOR own routes 200, ADMIN dashboard 200, ADMIN accessing INTERN routes 200.
- `mvn clean compile`: BUILD SUCCESS, 0 errors.
- `ExaminAiApplicationTests.contextLoads` remains failing (pre-existing from Story 1.1): requires `MAIL_HOST` env var and PostgreSQL — environment limitation, not a code bug. Story spec confirms only `SecurityIntegrationTest` needs to pass.
- `CustomUserDetailsService` uses explicit `SimpleGrantedAuthority("ROLE_" + role)` pattern — consistent with Spring Security 6.x `hasRole()` behavior.
- `SecurityConfig` uses `@EnableMethodSecurity` (replaces deprecated `@EnableGlobalMethodSecurity`) to enable `@PreAuthorize` on controllers.
- Login page is standalone HTML (does not extend base layout) to avoid `sec:authentication` NPE on unauthenticated requests.
- BCryptPasswordEncoder strength set to 12 per architecture spec (not Spring default of 10).
- MAIL_PORT fix applied: `${MAIL_PORT}` → `${MAIL_PORT:587}` in application.yml.

### File List

- `src/main/resources/db/changelog/changelogs/001-init-schema.sql` (NEW)
- `src/main/resources/db/changelog/changelogs/002-spring-session.sql` (NEW)
- `src/main/resources/db/changelog/db.changelog-master.xml` (MODIFIED — added 2 includes)
- `src/main/java/com/examinai/user/Role.java` (NEW)
- `src/main/java/com/examinai/user/UserAccount.java` (NEW)
- `src/main/java/com/examinai/user/UserAccountRepository.java` (NEW)
- `src/main/java/com/examinai/user/CustomUserDetailsService.java` (NEW)
- `src/main/java/com/examinai/config/SecurityConfig.java` (NEW)
- `src/main/resources/templates/auth/login.html` (NEW)
- `src/main/java/com/examinai/task/InternTaskController.java` (NEW)
- `src/main/java/com/examinai/review/MentorReviewController.java` (NEW)
- `src/main/java/com/examinai/admin/AdminController.java` (NEW)
- `src/main/resources/templates/intern/task-list.html` (NEW)
- `src/main/resources/templates/mentor/review-queue.html` (NEW)
- `src/main/resources/templates/admin/dashboard.html` (NEW)
- `src/main/resources/application.yml` (MODIFIED — MAIL_PORT default :587)
- `src/test/java/com/examinai/user/SecurityIntegrationTest.java` (NEW)

### Review Findings

- [x] [Review][Decision] FK ON DELETE policy undefined — resolved: RESTRICT is intentional; account removal uses `active = false` soft-delete, not physical deletion [src/main/resources/db/changelog/changelogs/001-init-schema.sql]
- [x] [Review][Patch] Missing admin-route 403 tests for non-ADMIN roles — added `internCannotAccessAdminRoutes()` and `mentorCannotAccessAdminRoutes()` [src/test/java/com/examinai/user/SecurityIntegrationTest.java]
- [x] [Review][Patch] Missing ADMIN→`/mentor/reviews` 200 test — added `adminCanAccessMentorRoutes()` [src/test/java/com/examinai/user/SecurityIntegrationTest.java]
- [x] [Review][Defer] `DisabledException` thrown from `loadUserByUsername` [src/main/java/com/examinai/user/CustomUserDetailsService.java:24] — deferred, spec-required behavior; wrapped as InternalAuthenticationServiceException but auth failure handler still fires correctly
- [x] [Review][Defer] GET `/logout` silently ignored by Spring Security 6 (POST-only by default) [src/main/java/com/examinai/config/SecurityConfig.java:39] — deferred, no logout links in templates yet
- [x] [Review][Defer] `roleBasedSuccessHandler` falls back to `ROLE_INTERN` silently for unknown roles [src/main/java/com/examinai/config/SecurityConfig.java:60] — deferred, edge case not required by spec
- [x] [Review][Defer] `email` column lacks uniqueness constraint [src/main/resources/db/changelog/changelogs/001-init-schema.sql:6] — deferred, belongs to a future story
- [x] [Review][Defer] `base.html` accesses `authorities[0]` without bounds check — deferred, pre-existing file not in this diff
- [x] [Review][Defer] `UserAccount.dateCreated` has no Java-side default — only set via `@PrePersist` [src/main/java/com/examinai/user/UserAccount.java:32] — deferred, no immediate null risk in current story
- [x] [Review][Defer] `task_review.status`/`llm_result`/`mentor_result` are unconstrained `VARCHAR(20)` [src/main/resources/db/changelog/changelogs/001-init-schema.sql] — deferred, belongs to Story 3+ (AI review pipeline)
- [x] [Review][Defer] `findAllByRole` returns unbounded list with no pagination [src/main/java/com/examinai/user/UserAccountRepository.java:8] — deferred, belongs to Story 1.3+ (admin UI)

## Change Log

- 2026-04-21: Story 1.2 implemented — Spring Security 6.x authentication with role-based access control. Created 5-table domain schema + Spring Session JDBC schema via Liquibase, UserAccount JPA entity + repository, CustomUserDetailsService, SecurityConfig with BCrypt-12 and role-based redirect handler, standalone login.html, three stub controllers (@PreAuthorize on each method), three stub templates extending base layout. Fixed deferred MAIL_PORT:587 default. SecurityIntegrationTest: 7/7 passing.
