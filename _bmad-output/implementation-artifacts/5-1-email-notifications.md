# Story 5.1: Email Notifications

Status: done

<!-- Ultimate context engine analysis completed - comprehensive developer guide created -->

## Story

As a mentor or intern,
I want to receive email notifications at key moments in the review lifecycle,
so that I do not have to constantly check the platform and the review loop closes automatically.

## Acceptance Criteria

(From [epics.md](../planning-artifacts/epics.md) — Story 5.1; maps to **FR29**, **FR30**, **NFR7**, **NFR15**.)

1. **AFTER_COMMIT + async for AI completion**  
   **Given** `ReviewPersistenceService` completes saving **`LLM_EVALUATED`** and publishes **`AiReviewCompleteEvent`** (see [`saveLlmSuccess`](../../src/main/java/com/examinai/review/ReviewPersistenceService.java))  
   **When** the DB transaction **commits**  
   **Then** the email handler runs via **`@TransactionalEventListener(phase = AFTER_COMMIT)`** together with **`@Async`** — it does **not** run before commit and does **not** block the pipeline thread that published the event.

2. **Mentor email content (AI review complete)**  
   **Given** the mentor notification path runs for a successful AI review  
   **When** the email is sent  
   **Then** the message goes to the **assigned mentor’s** `UserAccount.email` and includes: **intern identifier** (event field `internName` is the intern’s **username** from persistence — use that or resolve `UserAccount` by `reviewId` if you add a lookup), **course name**, **task name**, and a clear call to open the review in the app (link to `/mentor/reviews/{reviewId}` on the same origin is acceptable for MVP; plain text is fine).

3. **AFTER_COMMIT + async for mentor decision**  
   **Given** `saveMentorDecision` persists **`APPROVED`** or **`REJECTED`** and publishes **`MentorDecisionEvent`** (see [`ReviewPersistenceService.saveMentorDecision`](../../src/main/java/com/examinai/review/ReviewPersistenceService.java))  
   **When** the DB transaction **commits**  
   **Then** the intern email handler runs with **`@TransactionalEventListener(phase = AFTER_COMMIT)`** + **`@Async`**.

4. **Intern email content (mentor decision)**  
   **Given** the intern notification path runs  
   **When** the email is sent  
   **Then** it goes to the **intern’s** `UserAccount.email` and includes: **course name**, **task name**, final status **APPROVED** or **REJECTED** (as stored in the event’s `finalStatus`), and **mentor remarks** — if remarks are `null` or blank, the body must still state **"No remarks provided."**

5. **SMTP failure does not break data**  
   **Given** either notification method runs and **`JavaMailSender.send(...)`** throws  
   **When** the exception is handled  
   **Then** log at **ERROR** (or WARN if you standardize on warn for notification-only failures) with enough context, **do not rethrow** — **`TaskReview`** state and mentor decision remain as persisted (**NFR15**).

6. **Configuration**  
   **Given** `application.yml` is reviewed  
   **When** mail settings are checked  
   **Then** `spring.mail.host`, `port`, `username`, and `password` are bound from **`MAIL_HOST`**, **`MAIL_PORT`** (default `587` already present), **`MAIL_USERNAME`**, **`MAIL_PASSWORD`** — **no** literals for secrets in Java or YAML.

7. **Single async executor**  
   **Given** notification methods are **`@Async`**  
   **When** the application starts  
   **Then** they use the **same** `Executor` as the rest of the app: **`AsyncConfig`** implements **`AsyncConfigurer`** and defines the pool (**core=15, max=30, queue=150, awaitTerminationSeconds=120**) — do **not** add a second `TaskExecutor` bean for mail unless you have a strong reason and document it.

8. **Optional edge cases**  
   - **`mentorId` null** on `AiReviewCompleteEvent` (task with no mentor): log and **skip** email; do not throw.  
   - **Recipient `email` null/blank** on `UserAccount`: log and **skip**; do not break the commit.  
   - Seed users already have addresses in [`004-seed-data.sql`](../../src/main/resources/db/changelog/changelogs/004-seed-data.sql) (`mentor@examinai.local`, `intern@examinai.local`) for local testing.

## Tasks / Subtasks

- [x] **Create `com.examinai.notification.NotificationService`** (AC: 1–5, 7)  
  - [x] Inject `JavaMailSender` and `UserAccountRepository` (resolve mentor/intern email by `mentorId` / `internId`).  
  - [x] Move **`@TransactionalEventListener(phase = AFTER_COMMIT)`** + **`@Async`** handler methods here for **`AiReviewCompleteEvent`** and **`MentorDecisionEvent`**.  
  - [x] Build **`MimeMessage`** (or `SimpleMailMessage` if you keep content short) with subjects/bodies matching AC 2 and 4.  
  - [x] Wrap `send` in try/catch; on failure log and swallow.

- [x] **Remove duplicate listener** (AC: 1, 3)  
  - [x] Delete or fully empty [`AiReviewNotificationListener`](../../src/main/java/com/examinai/review/AiReviewNotificationListener.java) after migrating behavior — **only one** Spring bean may listen to each event type to avoid **double emails**.

- [x] **Keep events and publisher unchanged** (AC: 1, 3)  
  - [x] **`AiReviewCompleteEvent`**, **`MentorDecisionEvent`**, and **`ReviewPersistenceService`** stay the single source of truth for **when** events fire; this story only implements **delivery**.

- [x] **Verify `application.yml` mail block** (AC: 6)  
  - [x] Confirm env-only secrets; align with [`.env.example`](../../.env.example) if any key is missing.

- [x] **Tests** (NFR15-friendly)  
  - [x] Prefer **`@MockBean JavaMailSender`** (or `JavaMailSender` spy) in a **`@SpringBootTest`** or slice test: assert **listener runs after transaction** when you use **`@Transactional` test + `TransactionTemplate`** or a dedicated integration test that commits; **or** use **GreenMail** for a full SMTP integration test.  
  - [x] At minimum: **unit** test for message construction (to/subject/body contains required strings) and **one** test proving **exception from `send` does not propagate** from the listener.

## Dev Notes

### Developer context (why this story exists)

- Email is the last mile of the async pipeline: **FR29** (mentor) and **FR30** (intern). The domain events and persistence paths already exist; **this story only wires `JavaMailSender` and replaces logging stubs** with real SMTP sends.
- **NFR15** is non-negotiable: email failure is **out-of-band** of the review state machine.

### Technical requirements

- Use Spring Boot’s auto-configured **`JavaMailSender`** (starter **mail** is already on the classpath from Epic 1 scaffold).  
- **Mentor lookup:** `UserAccountRepository.findById(mentorId)` for `AiReviewCompleteEvent.mentorId()`.  
- **Intern lookup:** `UserAccountRepository.findById(internId)` for `MentorDecisionEvent.internId()`.  
- **Remarks in email:** `MentorDecisionEvent.remarks()` may be `null` after normalization in persistence — still show **"No remarks provided."** per AC.  
- **Identity display:** `AiReviewCompleteEvent.internName()` is populated as **`intern.getUsername()`** in code — present it clearly in the email (e.g. “Intern (username): …”).

### Architecture compliance

- Package **`com.examinai.notification`** is the canonical home for **`NotificationService.java`** per [architecture.md](../planning-artifacts/architecture.md) (see “Package Organization” and “Integration Points”).  
- **Event listen rules:** `@TransactionalEventListener(phase = AFTER_COMMIT)` + `@Async`; never rethrow from listeners.  
- **Do not** add `@Transactional` on the listener class for send logic unless you have a clear sub-transaction need — avoid coupling SMTP to DB transactions.

### Library and framework

- **Spring Boot 3.4.2** + **`spring-boot-starter-mail`** — `JavaMailSender` API is stable; no extra dependency required for basic SMTP.  
- Optional **GreenMail** (`org.greenmail:greenmail-junit5` or similar) for integration tests — decide one approach and stay consistent (see [epic-4 retro notes](./epic-4-retro-2026-04-22.md) on SMTP test strategy).

### File structure

| Action | Path |
|--------|------|
| Add | `src/main/java/com/examinai/notification/NotificationService.java` |
| Remove or gut | `src/main/java/com/examinai/review/AiReviewNotificationListener.java` |
| Touch if needed | `src/main/resources/application.yml` (verify placeholders only) |
| Tests | `src/test/java/com/examinai/notification/NotificationServiceTest.java` (and/or integration test with mocks) |

### Testing requirements

- Mock **`JavaMailSender`** to verify **`send(MimeMessage)`** (or `SimpleMailMessage`) is invoked with expected **To** and body snippets.  
- If using **GreenMail**, document required **`application-test.yml`** or profile props (`spring.mail.host=localhost`, random port).  
- Do not require real external SMTP in CI for green builds.

### Continuity from Epic 4 (previous work)

- Story **4.2** already publishes **`MentorDecisionEvent`** with `(reviewId, internId, courseName, taskName, finalStatus, remarks)` and **`AiReviewCompleteEvent`** after **`saveLlmSuccess`**. The stub [`AiReviewNotificationListener`](../../src/main/java/com/examinai/review/AiReviewNotificationListener.java) only **logs**; this story **replaces** that behavior in **`NotificationService`**.  
- [Epic 4 retro](./epic-4-retro-2026-04-22.md) flags **thread-pool saturation** (async executor full) as a risk: if the pool rejects work, **AFTER_COMMIT** listeners may not run. Out of scope to redesign the pool here, but if you add metrics/logging for rejected executions, do not block story completion; document any follow-up.

### Git intelligence (recent commits)

- Recent work: Epic 4 implementation (`82844f7`, `340d3d9`) — mentor review detail, **`MentorDecisionEvent`**, **`AiReviewNotificationListener`** stub. Email body implementation is still **outstanding**.

### Latest technical notes (mail)

- Spring Boot **`spring.mail.*`** properties map to JavaMail; **`MAIL_PORT:587`** with **`starttls` + `auth`** matches typical submission ports ([`application.yml`](../../src/main/resources/application.yml) already sets `smtp.auth` and `starttls.enable`).  
- For local dev without SMTP, use **MailHog**, **Mailpit**, or similar, or keep mocks in tests.

### Project context

- No **`project-context.md`** file exists in the repo; this story plus **`architecture.md`** and **`epics.md`** are the authoritative implementation references.

## Dev Agent Record

### Agent Model Used

Cursor agent (dev-story workflow) — 2026-04-22

### Debug Log References

_n/a_

### Implementation Plan

- Added `NotificationService` with `AFTER_COMMIT` + `@Async` listeners, `JavaMailSender` + `UserAccountRepository`, plain-text `SimpleMailMessage` bodies per AC, ERROR logging and swallowed exceptions on send failure.
- Removed `AiReviewNotificationListener` to ensure a single listener per event.
- Unit tests: body construction, send invocation, skip paths, and non-propagating `MailSendException`.

### Completion Notes List

- **5-1** implemented: mentor email after AI review (intern username, course, task, path `/mentor/reviews/{id}`); intern email after decision (status, remarks or “No remarks provided.”); `mentorId` null / missing email / blank email handled without throwing; `application.yml` mail block verified as env-driven (aligned with `.env.example`).
- Full suite `mvn test` passed (exit 0; `ExaminAiApplicationTests` skipped without Docker per `@Testcontainers(disabledWithoutDocker = true)`).

### File List

- `src/main/java/com/examinai/notification/NotificationService.java` (new)
- `src/main/java/com/examinai/review/AiReviewNotificationListener.java` (removed)
- `src/test/java/com/examinai/notification/NotificationServiceTest.java` (new)
- `_bmad-output/implementation-artifacts/sprint-status.yaml` (5-1 → review)

### Change Log

- 2026-04-22: Email notifications for `AiReviewCompleteEvent` and `MentorDecisionEvent` via `NotificationService`; removed duplicate `AiReviewNotificationListener`; added tests.
- 2026-04-22: Code review patches — `internId` null guard on `onMentorDecision`; `onAiReviewComplete_sendFailure_doesNotPropagate` + `onMentorDecision_nullInternId_skipsSend` tests.

### Review Findings

- [x] [Review][Patch] Add early return when `MentorDecisionEvent.internId()` is `null` (symmetry with `mentorId` on AI-complete; avoids `userAccountRepository.findById(null)`) — `NotificationService.java` (before intern lookup, ~L73-75) — fixed 2026-04-22
- [x] [Review][Patch] Add unit test: `onAiReviewComplete` when `mailSender.send` throws (same pattern as `onMentorDecision_sendFailure_doesNotPropagate`) so AC5 is covered for both notification paths — `NotificationServiceTest.java` — fixed 2026-04-22
- [x] [Review][Defer] `application.yml` mail env wiring (AC6) not visible in the story’s code diff; verified separately — existing file remains env-driven. — `application.yml` — deferred, pre-existing

## Story completion status

- **Status:** `done`  
- **Context engine:** Analysis completed — event contracts, file paths, and consolidation with **`AiReviewNotificationListener`** called out to prevent double-send and missed recipients.

## References

- [epics.md — Story 5.1](../planning-artifacts/epics.md)  
- [architecture.md — Notifications, events, package layout](../planning-artifacts/architecture.md)  
- [prd.md](../planning-artifacts/prd.md) — FR29, FR30, NFR7, NFR15  
- Source: [`ReviewPersistenceService.java`](../../src/main/java/com/examinai/review/ReviewPersistenceService.java)  
- Source: [`AsyncConfig.java`](../../src/main/java/com/examinai/config/AsyncConfig.java)  
- Source: [`application.yml`](../../src/main/resources/application.yml)  

## Open questions (non-blocking)

- **Deep link base URL:** If the app is not always at `http://localhost:8080`, should **`server` URL** come from a new env var (e.g. `APP_BASE_URL`) for links in emails? Optional enhancement for 5.1; plain path-only links are acceptable for MVP.  
- **Display name vs username:** `UserAccount` has no separate display name; using **username** for “intern name” in the mentor email is consistent with current event payload.
