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

## Deferred from: code review of 2-3-intern-task-list-progress-view (2026-04-21)

- `assertOwnership()` does not null-check `Authentication` returned by `SecurityContextHolder.getContext().getAuthentication()` — NPE if called outside a security context (TaskService.java:126)
- `TaskReview.dateCreated` has `@Column(nullable=false)` but no `@PrePersist` or `@Column(insertable=false)` — relies on Epic 3 always calling `setDateCreated()` before save; if omitted, DB NOT NULL constraint fires at runtime (TaskReview.java:44)
- `TaskServiceTest.findForInternByUsername_withNoReviews`: `intern.getId()` is `null`, repository mock uses `any()` — correct argument passing is untested; strengthen with a real ID + `eq()` matcher when Epic 3 adds write-path tests (TaskServiceTest.java:138)

## Deferred from: code review of 3-1-task-submission-form-async-ai-review-pipeline (2026-04-22)

- GitHub token potentially logged via Spring DEBUG HTTP logging — `RestClient` logs `Authorization` headers at DEBUG level when Spring web debug logging is enabled; add a header-masking interceptor or document as a deployment runbook constraint (GitHubClientConfig.java:19)
- LLM JSON embedded in prose not handled by `LlmOutputSanitizer` — sanitizer strips `<think>` blocks and fenced JSON but cannot extract JSON buried in plain prose text; edge case as model/prompt evolves (LlmOutputSanitizer.java:13)
- Thread pool rejection silently loses pipeline submission — when `AsyncConfig` executor is saturated, `TaskRejectedException` from `@Async` dispatch propagates through Spring's `afterCommit()` callback and leaves `TaskReview` stuck in PENDING permanently; add a rejection policy or a startup-time `TaskReviewRepository.resetStalePending()` recovery hook (AsyncConfig.java:20)

## Deferred from: code review of 3-2-review-status-polling-live-status-updates (2026-04-22)

- Inconsistent 404/403 between HTML and JSON endpoints — by spec design; enumeration risk accepted per epic AC (controllers)
- Stale fetch race — out-of-order tick responses can overwrite newer state (review-polling.js)
- `label` element null check missing between polling ticks (review-polling.js:14)
- `tr.getIntern()` null guard missing in `ReviewStatusController` — NPE leaks as 500 (ReviewStatusController.java:29)
- `tr.getStatus()` null guard missing in `ReviewStatusController` (ReviewStatusController.java:32)
- `tr.getIntern()` null guard missing in `InternReviewStatusController` (InternReviewStatusController.java:30)
- `tr.getId()` null unboxed into primitive `long` in `ReviewStatusResponse` (ReviewStatusController.java:34)
- INNER JOIN on `tr.intern` in `findByIdWithInternForStatusJson` — orphaned intern causes silent 404 (TaskReviewRepository.java:37)
- `data.displayLabel` null/undefined guard missing in polling JS (review-polling.js:14)
- Primitive `long` in `ReviewStatusResponse` — JS precision loss for IDs > 2^53 (ReviewStatusResponse.java)
- Duplicate ownership-check logic across both controllers — extract to shared policy component (controllers)
- Repository query naming inconsistency — three different suffixing conventions (TaskReviewRepository.java)
- No polling back-off or jitter — infinite polling on stuck reviews (review-polling.js)
- APPROVED/REJECTED/ERROR badge cases include dead hidden-spinner elements — terminal states, spinner never shown (review-status-badge.html)

## Deferred from: code review of 2-1-course-management (2026-04-21)

- Spring AOP self-invocation: `@PreAuthorize` on `findById()` skipped when called from `update()`/`delete()` — no current security gap (same expression on all methods), but annotation is misleading for in-process callers (CourseService.java:22)
- JavaScript `confirm()` is the only delete guard — no server-side second-factor confirmation; standard pattern but easily bypassed (course-list.html:38)
- `LocalDateTime.now()` for `dateCreated` — JVM local time, not UTC; timestamps inconsistent in multi-timezone/containerized deployments (Course.java:25)
- Redundant `courseRepository.save()` in `@Transactional update()` — JPA dirty checking auto-flushes changes on commit; explicit save harmless but unnecessary (CourseService.java:43)
- `findAllByOrderByCourseNameAsc()` returns unbounded `List` — no pagination; potential heap pressure at scale (CourseRepository.java:7)
- `Course` entity has no `equals`/`hashCode` override — identity-based comparison; subtle bugs possible in set/cache operations (Course.java)
- `create_savesCourseWithCorrectFields` test does not assert `dateCreated` — `@PrePersist` not invoked in Mockito context; lifecycle behavior untested (CourseServiceTest.java:27)
- No `CourseControllerTest` — form binding, redirect, and validation re-render untested at controller level
