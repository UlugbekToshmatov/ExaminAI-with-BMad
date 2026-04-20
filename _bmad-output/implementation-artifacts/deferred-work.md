# Deferred Work

## Deferred from: code review of 1-1-project-scaffold-base-configuration (2026-04-20)

- `GITHUB_TOKEN` env var in `.env.example` not wired into `application.yml` — intended for Story 3.x AI review pipeline (.env.example:4)
- Empty `MAIL_PORT` env var causes Integer type-conversion failure at startup; no default value set (application.yml:19)
- `.mentor-action-panel { top: 72px }` hardcoded — sticky positioning breaks when navbar wraps on mobile below 992px breakpoint (custom.css:40-43)
- `_csrf` null on error dispatch path — `${_csrf.token}` in base.html head will throw when CSRF is unavailable (e.g., error pages). No error pages until Story 3.3 (base.html:10-11)
- No `SecurityFilterChain` — default Spring Boot Security auto-config applies; security posture fully undefined until Story 1.2
- `WebMvcConfig` custom `/webjars/**` handler registers no `setCachePeriod()` — Bootstrap CSS/JS re-fetched on every page load with no Cache-Control headers (WebMvcConfig.java:11-13)
- Liquibase `db.changelog-master.xml` references `dbchangelog-4.31.xsd` schema URL — may not be on classpath if Boot-managed Liquibase version is < 4.31; non-critical in online environments (db.changelog-master.xml:6)
