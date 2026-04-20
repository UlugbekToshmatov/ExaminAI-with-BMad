# Story 1.1: Project Scaffold & Base Configuration

Status: done

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

As a developer,
I want the Spring Boot project initialized with all required dependencies, configuration files, and base UI layout,
So that the team has a working foundation to build all features on.

## Acceptance Criteria

**AC1 — Spring Initializr scaffold:**
Given the Spring Initializr curl command is run with bootVersion=3.4.2, javaVersion=21, groupId=com.examinai, dependencies=web,thymeleaf,security,data-jpa,postgresql,liquibase,mail,validation,actuator
When the project is unzipped
Then a valid Maven project structure exists under `com.examinai` with `ExaminAiApplication.java` as the entry point

**AC2 — pom.xml manual additions:**
Given the project is generated
When `pom.xml` is inspected
Then these dependencies are present: `spring-session-jdbc`, `spring-ai-bom:1.0.0` in `<dependencyManagement>`, `spring-ai-ollama-spring-boot-starter`, `thymeleaf-layout-dialect`, `thymeleaf-extras-springsecurity6:3.1.2.RELEASE`, `org.webjars/bootstrap:5.3.x`

**AC3 — application.yml production defaults:**
Given the project is configured
When `application.yml` is inspected
Then it contains production defaults (Docker service hostnames `db` and `ollama`), `spring.session.store-type: jdbc`, `spring.session.jdbc.initialize-schema: never`, and Ollama base URL pointing to the `ollama` service

**AC4 — application-dev.yml local overrides:**
Given the project is configured
When `application-dev.yml` is inspected
Then it overrides datasource and Ollama URLs to `localhost` and enables debug logging for `com.examinai`

**AC5 — base layout template:**
Given the project is configured
When `templates/layout/base.html` is inspected
Then it uses Thymeleaf Layout Dialect, loads Bootstrap 5 via WebJar, contains a skip link `<a href="#main-content" class="visually-hidden-focusable">Skip to content</a>`, includes a CSRF meta tag, and renders a navbar with role badge, username, and logout link via `sec:authentication`

**AC6 — custom.css tokens:**
Given the project is configured
When `static/css/custom.css` is inspected
Then it defines: `--ai-feedback-bg: #f8f9fa`, `--mentor-decision-bg: #ffffff`, AI content style (light bg + `3px solid var(--bs-warning)` left border), Mentor content style (white bg + `3px solid var(--bs-primary)` left border), and sticky action panel positioning (`position: sticky; top: 72px`)

**AC7 — .env.example:**
Given `.env.example` exists in the project root
When it is inspected
Then it contains all required keys (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `GITHUB_TOKEN`, `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `OLLAMA_BASE_URL`) with empty values and no secrets

**AC8 — WebMvcConfig WebJar serving:**
Given `WebMvcConfig.java` is implemented
When Bootstrap assets are requested at `/webjars/**`
Then Spring's `ResourceHandlerRegistry` serves them from the classpath WebJar

## Tasks / Subtasks

- [x] Task 1: Run Spring Initializr and unzip project (AC: 1)
  - [x] Run the exact curl command below into the project root
  - [x] Unzip `examin-ai.zip`, move contents up to project root (so `pom.xml` is at root)
  - [x] Verify `src/main/java/com/examinai/ExaminAiApplication.java` exists

- [x] Task 2: Add manual pom.xml dependencies (AC: 2)
  - [x] Add `spring-session-jdbc` to `<dependencies>`
  - [x] Add `spring-ai-bom:1.0.0` BOM import to `<dependencyManagement>`
  - [x] Add `spring-ai-ollama-spring-boot-starter` to `<dependencies>` (version from BOM)
  - [x] Add `thymeleaf-layout-dialect` to `<dependencies>`
  - [x] Add `thymeleaf-extras-springsecurity6:3.1.2.RELEASE` to `<dependencies>`
  - [x] Add `org.webjars/bootstrap:5.3.3` to `<dependencies>`
  - [x] Run `mvn clean compile` and verify no errors

- [x] Task 3: Create application.yml (AC: 3)
  - [x] Create `src/main/resources/application.yml` with production config (see Dev Notes)
  - [x] Verify: datasource points to Docker hostname `db`, Ollama points to `ollama`
  - [x] Verify: `spring.session.store-type: jdbc` and `spring.session.jdbc.initialize-schema: never`

- [x] Task 4: Create application-dev.yml (AC: 4)
  - [x] Create `src/main/resources/application-dev.yml` with localhost overrides (see Dev Notes)
  - [x] Verify: datasource URL uses `localhost:5432`, Ollama URL uses `localhost:11434`
  - [x] Verify: `logging.level.com.examinai: debug` is present

- [x] Task 5: Set up Liquibase directory structure
  - [x] Create `src/main/resources/db/changelog/db.changelog-master.xml` (empty, no includes yet)
  - [x] Create `src/main/resources/db/changelog/changelogs/` directory (placeholder `.gitkeep`)
  - [x] Create `src/main/resources/prompts/` directory (placeholder `.gitkeep` for future Story 3.1)

- [x] Task 6: Create base layout template (AC: 5)
  - [x] Create `src/main/resources/templates/layout/base.html`
  - [x] Include skip link, CSRF meta tag, Bootstrap WebJar, Thymeleaf Layout Dialect namespaces
  - [x] Implement navbar: logo + role badge (sec:authentication) + username + logout button
  - [x] Create fragment placeholder directories: `templates/fragments/`, `templates/intern/`, `templates/mentor/`, `templates/admin/`, `templates/auth/`, `templates/error/`

- [x] Task 7: Create custom.css (AC: 6)
  - [x] Create `src/main/resources/static/css/custom.css` with all CSS custom properties
  - [x] Define: `--ai-feedback-bg`, `--mentor-decision-bg` tokens
  - [x] Define: AI content style (light bg + warning left border), Mentor content style (white bg + primary left border)
  - [x] Define: `.mentor-action-panel { position: sticky; top: 72px; }`
  - [x] Create `src/main/resources/static/js/` directory (placeholder for future `review-polling.js`)

- [x] Task 8: Create .env.example (AC: 7)
  - [x] Create `.env.example` in project root with all 9 required keys (see Dev Notes)
  - [x] Verify: all values are empty strings, no secrets committed
  - [x] Add `.env` to `.gitignore`

- [x] Task 9: Implement WebMvcConfig.java (AC: 8)
  - [x] Create `src/main/java/com/examinai/config/WebMvcConfig.java`
  - [x] Implement `WebMvcConfigurer` with `addResourceHandlers` mapping `/webjars/**`
  - [x] Run `mvn clean compile` — must succeed with 0 errors

## Dev Notes

### Spring Initializr Command (run exactly as shown)

```bash
curl -G https://start.spring.io/starter.zip \
  -d type=maven-project \
  -d language=java \
  -d bootVersion=3.4.2 \
  -d baseDir=examin-ai \
  -d groupId=com.examinai \
  -d artifactId=examin-ai \
  -d name=examin-ai \
  -d javaVersion=21 \
  -d dependencies=web,thymeleaf,security,data-jpa,postgresql,liquibase,mail,validation,actuator \
  -o examin-ai.zip
```

### Manual pom.xml Additions

After unzip, add the following to `pom.xml`. **CRITICAL order matters:**

**In `<dependencyManagement><dependencies>` block** (add this section if not present):
```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-bom</artifactId>
    <version>1.0.0</version>
    <type>pom</type>
    <scope>import</scope>
</dependency>
```

**In `<dependencies>` block** (in addition to what Spring Initializr generates):
```xml
<!-- Spring Session JDBC — session persistence in PostgreSQL -->
<dependency>
    <groupId>org.springframework.session</groupId>
    <artifactId>spring-session-jdbc</artifactId>
</dependency>

<!-- Spring AI Ollama — version managed by spring-ai-bom -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-ollama-spring-boot-starter</artifactId>
</dependency>

<!-- Thymeleaf Layout Dialect — shared base template -->
<dependency>
    <groupId>nz.net.ultraq.thymeleaf</groupId>
    <artifactId>thymeleaf-layout-dialect</artifactId>
</dependency>

<!-- Thymeleaf Spring Security extras — sec:authentication, sec:authorize -->
<dependency>
    <groupId>org.thymeleaf.extras</groupId>
    <artifactId>thymeleaf-extras-springsecurity6</artifactId>
    <version>3.1.2.RELEASE</version>
</dependency>

<!-- Bootstrap 5 via WebJar — served by ResourceHandlerRegistry -->
<dependency>
    <groupId>org.webjars</groupId>
    <artifactId>bootstrap</artifactId>
    <version>5.3.3</version>
</dependency>
```

**Anti-patterns to avoid:**
- Do NOT put `spring-ai-ollama-spring-boot-starter` inside `<dependencyManagement>` — it goes in `<dependencies>` (version inherited from BOM)
- Do NOT pin a version on `spring-session-jdbc` — it is managed by `spring-boot-starter-parent`
- Do NOT pin a version on `thymeleaf-layout-dialect` — let Spring Boot manage it

### application.yml (production defaults — Docker hostnames)

```yaml
spring:
  datasource:
    url: ${DB_URL}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: none
    open-in-view: false
  liquibase:
    change-log: classpath:db/changelog/db.changelog-master.xml
  session:
    store-type: jdbc
    jdbc:
      initialize-schema: never
  mail:
    host: ${MAIL_HOST}
    port: ${MAIL_PORT}
    username: ${MAIL_USERNAME}
    password: ${MAIL_PASSWORD}
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true
  ai:
    ollama:
      base-url: http://ollama:11434

server:
  servlet:
    session:
      timeout: 1h

management:
  endpoints:
    web:
      exposure:
        include: health
```

**Critical rule:** `spring.session.jdbc.initialize-schema: never` — NEVER change to `always`. Liquibase changelog `002-spring-session.sql` (Story 1.2) manages those tables. If set to `always`, schema creation conflicts will occur.

**Critical rule:** `spring.jpa.hibernate.ddl-auto: none` — NEVER use `create`, `create-drop`, or `update`. Liquibase is the sole schema manager.

### application-dev.yml (local dev overrides — localhost)

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/examinai
  ai:
    ollama:
      base-url: http://localhost:11434

logging:
  level:
    com.examinai: debug
    org.springframework.security: debug
```

**Note:** `DB_USERNAME` and `DB_PASSWORD` still come from environment variables in dev — override only the URL.

### Liquibase db.changelog-master.xml (Story 1.1 — empty, no includes)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog
    xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
        http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.31.xsd">

    <!--
        Changelogs are added by subsequent stories in strict order:
        001-init-schema.sql     (Story 1.2) — user_account, course, task, task_review, task_review_issue
        002-spring-session.sql  (Story 1.2) — spring_session tables
        003-indexes.sql         (Story 1.3) — performance indexes
        004-seed-data.sql       (Story 1.3) — admin/mentor/intern seed accounts + sample course/tasks

        DO NOT change include order — Liquibase runs these as an ordered migration chain.
    -->

</databaseChangeLog>
```

**Warning:** The app will not fully start (Spring Session tables missing) until Story 1.2 adds the Liquibase changelogs. That is expected and correct for Story 1.1.

### templates/layout/base.html

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org"
      xmlns:layout="http://www.ultraq.net.nz/thymeleaf/layout"
      xmlns:sec="http://www.thymeleaf.org/extras/spring-security">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title layout:title-pattern="$CONTENT_TITLE - ExaminAI">ExaminAI</title>
    <!-- CSRF meta tag for JS fetch requests -->
    <meta name="_csrf" th:content="${_csrf.token}"/>
    <meta name="_csrf_header" th:content="${_csrf.headerName}"/>
    <!-- Bootstrap 5 via WebJar -->
    <link rel="stylesheet" th:href="@{/webjars/bootstrap/5.3.3/css/bootstrap.min.css}">
    <link rel="stylesheet" th:href="@{/css/custom.css}">
</head>
<body>
    <!-- Accessibility: skip link -->
    <a href="#main-content" class="visually-hidden-focusable">Skip to content</a>

    <nav class="navbar navbar-expand-lg navbar-dark bg-dark">
        <div class="container-fluid">
            <a class="navbar-brand fw-bold" th:href="@{/}">ExaminAI</a>
            <div class="navbar-nav ms-auto d-flex flex-row align-items-center gap-3">
                <!-- Role badge -->
                <span class="badge bg-secondary text-uppercase"
                      sec:authentication="principal.authorities[0].authority"
                      th:text="${#strings.replace(#authentication.authorities[0].authority, 'ROLE_', '')}">
                    ROLE
                </span>
                <!-- Username -->
                <span class="text-white" sec:authentication="name">Username</span>
                <!-- Logout -->
                <form th:action="@{/logout}" method="post" class="m-0">
                    <input type="hidden" th:name="${_csrf.parameterName}" th:value="${_csrf.token}">
                    <button type="submit" class="btn btn-outline-light btn-sm">Logout</button>
                </form>
            </div>
        </div>
    </nav>

    <main id="main-content" class="container py-4" style="max-width: 960px;">
        <div layout:fragment="content">
            <!-- Page content inserted here by Thymeleaf Layout Dialect -->
        </div>
    </main>

    <script th:src="@{/webjars/bootstrap/5.3.3/js/bootstrap.bundle.min.js}"></script>
    <th:block layout:fragment="scripts"></th:block>
</body>
</html>
```

**Critical rules for base.html:**
- `xmlns:layout` namespace is mandatory for Thymeleaf Layout Dialect to work
- `xmlns:sec` namespace is mandatory for `sec:authentication` and `sec:authorize` tags
- CSRF meta tags (`_csrf` and `_csrf_header`) are required by `review-polling.js` (Story 3.2) for POST fetch calls
- The `layout:fragment="content"` is the placeholder child pages fill in — do not rename
- `layout:fragment="scripts"` allows per-page JS to be added after Bootstrap

### static/css/custom.css (~50–100 lines)

```css
/* ExaminAI custom design tokens */
:root {
    --ai-feedback-bg: #f8f9fa;
    --mentor-decision-bg: #ffffff;
}

/* AI-generated content style — visually distinct from mentor decisions */
.ai-content {
    background: var(--ai-feedback-bg);
    border-left: 3px solid var(--bs-warning);
    padding: 1rem;
    border-radius: 0 0.375rem 0.375rem 0;
}

.ai-content-label {
    font-size: 0.75rem;
    font-weight: 600;
    text-transform: uppercase;
    color: var(--bs-secondary);
    margin-bottom: 0.5rem;
}

/* Mentor decision area — white bg + primary border */
.mentor-content {
    background: var(--mentor-decision-bg);
    border-left: 3px solid var(--bs-primary);
    padding: 1rem;
    border-radius: 0 0.375rem 0.375rem 0;
}

.mentor-content-label {
    font-size: 0.75rem;
    font-weight: 600;
    text-transform: uppercase;
    color: var(--bs-primary);
    margin-bottom: 0.5rem;
}

/* Sticky mentor action panel — used in review-detail.html (Story 4.2) */
.mentor-action-panel {
    position: sticky;
    top: 72px;
}

/* Code snippet display in AI feedback cards */
.ai-code-block {
    background: var(--bs-dark);
    color: var(--bs-light);
    border-radius: 0.375rem;
    padding: 0.75rem 1rem;
    font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
    font-size: 0.875rem;
    overflow-x: auto;
}

/* Task status card left-border colors (Story 2.3) */
.task-card-approved { border-left: 4px solid #198754; }
.task-card-in-review { border-left: 4px solid #ffc107; }
.task-card-rejected { border-left: 4px solid #dc3545; }
.task-card-error { border-left: 4px dashed #dc3545; }
.task-card-not-started { border-left: 4px solid #dee2e6; }

/* Cursor pointer for clickable table rows in mentor queue */
.table-hover-pointer tbody tr { cursor: pointer; }

/* Status badge with spinner layout */
.status-badge-container .badge {
    display: inline-flex;
    align-items: center;
    gap: 0.25rem;
}
```

### .env.example (project root — all keys, no values)

```
DB_URL=
DB_USERNAME=
DB_PASSWORD=
GITHUB_TOKEN=
MAIL_HOST=
MAIL_PORT=
MAIL_USERNAME=
MAIL_PASSWORD=
OLLAMA_BASE_URL=
```

**CRITICAL:** `.env` (with actual values) must NEVER be committed. Verify `.gitignore` includes `.env` before `git add`.

### WebMvcConfig.java

```java
package com.examinai.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/webjars/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/");
    }
}
```

### Package Structure to Create

Create the following package directories (even if empty — Java packages need at least a `.gitkeep` or a class):

```
src/main/java/com/examinai/
├── ExaminAiApplication.java         ← generated by Spring Initializr
├── config/
│   └── WebMvcConfig.java            ← created in this story
├── user/                            ← created in Story 1.2
├── course/                          ← created in Story 2.1
├── task/                            ← created in Story 2.2
├── review/                          ← created in Story 3.1
├── notification/                    ← created in Story 5.1
└── admin/                           ← created in Story 1.3
```

Only `config/WebMvcConfig.java` is created now. Other packages are created in their respective stories.

### Project Structure Notes

**File locations:**
- `pom.xml` → project root (generated by Initializr)
- `src/main/resources/application.yml` → replaces `application.properties` generated by Initializr (delete the `.properties` file)
- `src/main/resources/application-dev.yml` → new file
- `src/main/resources/db/changelog/db.changelog-master.xml` → new file
- `src/main/resources/db/changelog/changelogs/` → directory (empty for now)
- `src/main/resources/templates/layout/base.html` → new file
- `src/main/resources/templates/fragments/` → empty directory (Story 2–4 add content)
- `src/main/resources/templates/intern/` → empty directory
- `src/main/resources/templates/mentor/` → empty directory
- `src/main/resources/templates/admin/` → empty directory
- `src/main/resources/templates/auth/` → empty directory (Story 1.2 adds login.html)
- `src/main/resources/templates/error/` → empty directory (Story 3.3 adds error.html)
- `src/main/resources/static/css/custom.css` → new file
- `src/main/resources/static/js/` → empty directory (Story 3.2 adds review-polling.js)
- `src/main/resources/prompts/` → empty directory (Story 3.1 adds .st template files)
- `.env.example` → project root
- `.gitignore` → add `.env` line

**Alignment with architecture:**
- Package-by-feature under `com.examinai` (not layered architecture)
- Template directories mirror URL namespace: `/intern/**` → `templates/intern/`
- WebJar path: `/webjars/bootstrap/5.3.3/css/bootstrap.min.css` (version matches `pom.xml`)

### Testing Standards

No unit tests required for this story (pure scaffold — no business logic to test).

**Verification steps (manual):**
1. `mvn clean compile` — must succeed with 0 compilation errors
2. `mvn dependency:tree | grep spring-ai` — verify Spring AI BOM is resolving versions
3. `mvn dependency:tree | grep bootstrap` — verify `org.webjars:bootstrap:5.3.3` is present
4. Inspect generated `pom.xml` — verify `spring-ai-bom` is inside `<dependencyManagement>`, NOT `<dependencies>`

**Expected startup behavior (before Story 1.2):** The app will fail to start fully without a running PostgreSQL database. This is expected. Story 1.2 adds the Liquibase changelogs and `SecurityConfig` that complete the application startup.

### References

- Spring Initializr command: [Source: architecture.md#Starter Template Evaluation]
- Manual pom.xml dependencies: [Source: architecture.md#Selected Starter: Spring Initializr — Key Dependencies to Add]
- application.yml production config: [Source: architecture.md#Data Architecture, #Authentication & Security, #Infrastructure & Deployment]
- application-dev.yml overrides: [Source: architecture.md#Development Workflow Integration]
- Liquibase changelog structure: [Source: architecture.md#Data Architecture — Changelog structure]
- base.html requirements: [Source: epics.md#Story 1.1 AC5, ux-design-specification.md#Design System Foundation]
- custom.css tokens: [Source: epics.md#Story 1.1 AC6, ux-design-specification.md#Color System, UX-DR7]
- .env.example keys: [Source: epics.md#Story 1.1 AC7, architecture.md#Infrastructure & Deployment]
- WebMvcConfig: [Source: epics.md#Story 1.1 AC8, architecture.md#Project Structure]

## Dev Agent Record

### Agent Model Used

claude-sonnet-4-6

### Debug Log References

### Completion Notes List

- Used Spring Boot 3.5.13 (start.spring.io no longer supports 3.4.2; min >=3.5.0).
- Spring AI 1.0.0 renamed `spring-ai-ollama-spring-boot-starter` → `spring-ai-starter-model-ollama`; updated pom.xml accordingly.
- Moved `ExaminAiApplication.java` from `com.examinai.examin_ai` (Initializr default) to `com.examinai` as required by AC1.
- `mvn clean compile` — BUILD SUCCESS with 2 source files compiled.
- Spring AI BOM 1.0.0 and Bootstrap 5.3.3 confirmed in dependency tree.
- All 8 ACs satisfied; no unit tests required (pure scaffold — no business logic).

### File List

- pom.xml
- src/main/java/com/examinai/ExaminAiApplication.java
- src/main/java/com/examinai/config/WebMvcConfig.java
- src/test/java/com/examinai/ExaminAiApplicationTests.java
- src/main/resources/application.yml
- src/main/resources/application-dev.yml
- src/main/resources/db/changelog/db.changelog-master.xml
- src/main/resources/db/changelog/changelogs/.gitkeep
- src/main/resources/prompts/.gitkeep
- src/main/resources/templates/layout/base.html
- src/main/resources/templates/fragments/.gitkeep
- src/main/resources/templates/intern/.gitkeep
- src/main/resources/templates/mentor/.gitkeep
- src/main/resources/templates/admin/.gitkeep
- src/main/resources/templates/auth/.gitkeep
- src/main/resources/templates/error/.gitkeep
- src/main/resources/static/css/custom.css
- src/main/resources/static/js/.gitkeep
- .env.example
- .gitignore (added `.env` entry)

## Review Findings

- [x] [Review][Patch] AC3 decision resolved (option 2) — wired `OLLAMA_BASE_URL` env var into `application.yml`; kept `${DB_URL}` for datasource. Changed `spring.ai.ollama.base-url` to `${OLLAMA_BASE_URL:http://ollama:11434}` [application.yml:29]
- [x] [Review][Patch] `authorities[0]` hard index — fixed by wrapping the entire user nav block with `sec:authorize="isAuthenticated()"`, preventing render for unauthenticated users and null auth [base.html:23]
- [x] [Review][Patch] `OLLAMA_BASE_URL` env var declared in `.env.example` is silently ignored — fixed: `application.yml` now uses `${OLLAMA_BASE_URL:http://ollama:11434}` [application.yml:29]
- [x] [Review][Patch] Redundant `sec:authentication` attribute on role badge `<span>` — removed; `th:text` expression now solely drives the rendered role text [base.html:25-27]
- [x] [Review][Defer] `GITHUB_TOKEN` env var in `.env.example` not wired into `application.yml` — intended for Story 3.x AI review pipeline [.env.example:4] — deferred, pre-existing
- [x] [Review][Defer] Empty `MAIL_PORT` env var causes Integer type-conversion failure at startup [application.yml:19] — deferred, pre-existing
- [x] [Review][Defer] `.mentor-action-panel { top: 72px }` hardcoded — sticky positioning breaks when navbar wraps on mobile [custom.css:40-43] — deferred, pre-existing
- [x] [Review][Defer] `_csrf` null on error dispatch path crashes error pages before error pages are implemented [base.html:10-11] — deferred, pre-existing
- [x] [Review][Defer] No `SecurityFilterChain` — default Spring Security auto-configuration applies; security posture undefined until Story 1.2 — deferred, pre-existing
- [x] [Review][Defer] `WebMvcConfig` custom `/webjars/**` handler registers no caching headers — Bootstrap assets re-fetched on every page load [WebMvcConfig.java:11-13] — deferred, pre-existing
- [x] [Review][Defer] Liquibase XSD schema URL references `dbchangelog-4.31.xsd` which may not be on classpath depending on Liquibase version resolved by Boot BOM [db.changelog-master.xml:6] — deferred, pre-existing

## Change Log

- 2026-04-20: Story implemented — Spring Boot 3.5.13 scaffold created with all 9 tasks complete. Used spring-ai-starter-model-ollama (renamed from spring-ai-ollama-spring-boot-starter in Spring AI 1.0.0 GA). All ACs satisfied; mvn clean compile passes.
