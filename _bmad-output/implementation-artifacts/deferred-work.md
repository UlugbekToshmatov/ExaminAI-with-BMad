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
