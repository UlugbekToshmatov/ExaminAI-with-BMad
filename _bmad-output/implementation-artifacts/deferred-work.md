# Deferred Work

## Deferred from: code review of 1-1-project-scaffold-base-configuration (2026-04-20)

- `GITHUB_TOKEN` env var in `.env.example` not wired into `application.yml` — intended for Story 3.x AI review pipeline (.env.example:4)
- Empty `MAIL_PORT` env var causes Integer type-conversion failure at startup; no default value set (application.yml:19)
- `.mentor-action-panel { top: 72px }` hardcoded — sticky positioning breaks when navbar wraps on mobile below 992px breakpoint (custom.css:40-43)
- `_csrf` null on error dispatch path — `${_csrf.token}` in base.html head will throw when CSRF is unavailable (e.g., error pages). No error pages until Story 3.3 (base.html:10-11)
- No `SecurityFilterChain` — default Spring Boot Security auto-config applies; security posture fully undefined until Story 1.2
- `WebMvcConfig` custom `/webjars/**` handler registers no `setCachePeriod()` — Bootstrap CSS/JS re-fetched on every page load with no Cache-Control headers (WebMvcConfig.java:11-13)
- Liquibase `db.changelog-master.xml` references `dbchangelog-4.31.xsd` schema URL — may not be on classpath if Boot-managed Liquibase version is < 4.31; non-critical in online environments (db.changelog-master.xml:6)

## Deferred from: code review of 1-2-user-authentication-role-based-access (2026-04-21)

- `DisabledException` thrown directly from `CustomUserDetailsService.loadUserByUsername()` — spec-required but anti-pattern; wrapped as `InternalAuthenticationServiceException` internally, auth failure handler still fires correctly. Consider returning `UserDetails` with `isEnabled()=false` in a future refactor (CustomUserDetailsService.java:24)
- GET `/logout` silently ignored by Spring Security 6 (POST-only by default) — must ensure any logout link in templates uses a POST form, not a plain `<a href="/logout">` link (SecurityConfig.java:39)
- `roleBasedSuccessHandler` falls back silently to `/intern/tasks` when no matching role is found — consider logging or throwing if this path is reached to catch data corruption early (SecurityConfig.java:60)
- `email` column in `user_account` has no uniqueness constraint — if password reset or email-lookup features are added, duplicate emails will cause unpredictable behavior; add UNIQUE index in a future migration (001-init-schema.sql:6)
- `base.html` accesses `#authentication.authorities[0].authority` without an empty-check — if a user has zero granted authorities, this throws `ArrayIndexOutOfBoundsException` and breaks the navbar (pre-existing, not in this diff)
- `UserAccount.dateCreated` has no Java-side default value — only set via `@PrePersist`; a manually constructed `UserAccount` not persisted via JPA will have `null` dateCreated (UserAccount.java:32)
- `task_review.status`, `llm_result`, `mentor_result` are unconstrained `VARCHAR(20)` — add `CHECK` constraints or use a PostgreSQL enum when Story 3+ implements the AI review pipeline (001-init-schema.sql)
- `UserAccountRepository.findAllByRole()` returns an unbounded list — add `Page<UserAccount>` + `Pageable` overload when Story 1.3 admin UI implements user listing (UserAccountRepository.java:8)

## Deferred from: code review of 1-3-admin-user-account-management-seed-data (2026-04-21)

- Race condition in username uniqueness check — `existsByUsername` + `save` has no DB unique constraint fallback at service layer; concurrent requests can both pass the check and trigger a `ConstraintViolationException` (UserAccountService.java:24-33)
- BCrypt hashes for seed credentials committed to git history — offline dictionary attack possible for known dev passwords; rotation requires a new changeset (004-seed-data.sql)
- No reactivate path — account deactivation is irreversible via the UI; restoring an account requires direct DB access; consider a toggle in a future admin story
- Seed data uses `now()` for all timestamps — non-deterministic values across environments; use a fixed literal (e.g., `'2026-01-01 00:00:00'`) for deterministic seed data
- Absolute `href` paths in admin templates break under non-root context path deployment; replace with `th:href="@{...}"` (user-list.html:10, user-form.html:37)
- No password complexity enforcement — single-character passwords are accepted by the service; add minimum length/complexity validation in a future story

## Deferred from: code review of 2-2-task-management (2026-04-21)

- Orphaned tasks after mentor deletion — `mentor_id` FK in `task` table lacks `ON DELETE CASCADE` or `ON DELETE RESTRICT`; deleting a `UserAccount` with role MENTOR leaves orphaned task rows with dangling FK (001-init-schema.sql:26)
- `IllegalArgumentException` messages expose internal DB primary keys in UI flash messages — `"Course not found: 42"` style messages visible to ADMIN/MENTOR users; low risk given authenticated audience, but consider a sanitized user message layer (TaskService.java)
- `@PreAuthorize` on read methods (`findAll`, `findById`, `findAllCourses`, `findAllMentors`) is partially bypassed for internal calls — `findById` annotation skipped when invoked from `update`/`delete` via `this.findById()`; outer method protection covers the gap but annotation creates false impression of independent security (TaskService.java)

## Deferred from: code review of 2-1-course-management (2026-04-21)

- Spring AOP self-invocation: `@PreAuthorize` on `findById()` skipped when called from `update()`/`delete()` — no current security gap (same expression on all methods), but annotation is misleading for in-process callers (CourseService.java:22)
- JavaScript `confirm()` is the only delete guard — no server-side second-factor confirmation; standard pattern but easily bypassed (course-list.html:38)
- `LocalDateTime.now()` for `dateCreated` — JVM local time, not UTC; timestamps inconsistent in multi-timezone/containerized deployments (Course.java:25)
- Redundant `courseRepository.save()` in `@Transactional update()` — JPA dirty checking auto-flushes changes on commit; explicit save harmless but unnecessary (CourseService.java:43)
- `findAllByOrderByCourseNameAsc()` returns unbounded `List` — no pagination; potential heap pressure at scale (CourseRepository.java:7)
- `Course` entity has no `equals`/`hashCode` override — identity-based comparison; subtle bugs possible in set/cache operations (Course.java)
- `create_savesCourseWithCorrectFields` test does not assert `dateCreated` — `@PrePersist` not invoked in Mockito context; lifecycle behavior untested (CourseServiceTest.java:27)
- No `CourseControllerTest` — form binding, redirect, and validation re-render untested at controller level
