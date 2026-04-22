# Story 4.2: Mentor Review Detail — AI Feedback & Decision

Status: done

<!-- Ultimate context engine analysis completed - comprehensive developer guide created -->

## Story

As a mentor,
I want to open a review, scan all AI-flagged issues with code context, and make my final approve or reject decision with optional remarks,
so that I can complete a full review cycle in under 2 minutes without leaving the page.

## Acceptance Criteria

1. **Detail header & breadcrumb**  
   **Given** a mentor opens `/mentor/reviews/{reviewId}`  
   **When** the page loads  
   **Then** a full-width review header shows: **intern name**, **task name**, **course name**, and AI suggested verdict labeled **`AI Suggestion: APPROVE`** or **`AI Suggestion: REJECT`** — **never** copy that reads `Decision:` for the AI line  
   **And** a breadcrumb reads **`Review Queue > {InternName} — {TaskName}`** with **“Review Queue”** linking to `/mentor/reviews`

2. **AI feedback cards**  
   **Given** the review detail page renders  
   **When** the AI feedback section loads  
   **Then** each `TaskReviewIssue` is rendered as an **`AIFeedbackCard`** implemented as a Thymeleaf fragment **`th:fragment="aiIssueCard(issue)"`**, containing:  
   - a **line** badge  
   - a `<pre><code>` code snippet (**dark** background, monospace) with **`role="region"`** and **`aria-label="Code at line N"`** (use the actual line number)  
   - **issue** description  
   - **improvement** suggestion  

3. **Mentor action panel (sticky)**  
   **Given** the review detail page renders  
   **When** the **`MentorActionPanel`** loads (`**th:fragment="mentorActionPanel(review)"**`)  
   **Then** it is **sticky** (`position: sticky; top: 72px`) in the **`col-md-4`** column and contains:  
   - AI Suggestion badge (consistent with header wording)  
   - **“Your decision is final”** text  
   - **remarks** `<textarea>` (**rows=4**, optional)  
   - **Approve** button (`btn-success`, full width)  
   - **Reject** button (`btn-danger`, full width)  
   - **“← Back to Queue”** link (`text-muted small`)  
   **And** implementation reuses **`.mentor-action-panel`** in [`src/main/resources/static/css/custom.css`](../../src/main/resources/static/css/custom.css) where appropriate.

4. **Approve (PRG)**  
   **Given** the mentor clicks **Approve** (with or without remarks)  
   **When** `POST /mentor/reviews/{reviewId}/approve` is processed  
   **Then** **`ReviewPersistenceService.saveMentorDecision()`** sets `TaskReview.status` → **`APPROVED`**, stores **`mentorRemarks`**, sets **`mentor_result`** appropriately, publishes **`MentorDecisionEvent`**, and the user is **redirected** to `/mentor/reviews` (**PRG** — no view returned on success).

5. **Reject (PRG)**  
   **Given** the mentor clicks **Reject** (with or without remarks)  
   **When** `POST /mentor/reviews/{reviewId}/reject` is processed  
   **Then** **`saveMentorDecision()`** sets `TaskReview.status` → **`REJECTED`**, stores remarks, publishes **`MentorDecisionEvent`**, and redirects to `/mentor/reviews`.

6. **Override AI verdict**  
   **Given** the AI suggested verdict is **REJECT** and the mentor clicks **Approve**  
   **When** the decision is saved  
   **Then** `TaskReview.status` is **`APPROVED`** and **`mentor_result`** is **`"APPROVED"`** — **no** server-side validation blocks this override.

7. **State transitions only in persistence service**  
   **Given** transition logic is inspected  
   **When** the codebase is reviewed  
   **Then** **`LLM_EVALUATED` → `APPROVED` / `REJECTED`** occurs **only** inside **`ReviewPersistenceService.saveMentorDecision()`** — **not** in the controller (controllers call the service only).

8. **Admin POST**  
   **Given** an admin accesses approve/reject  
   **When** the request is processed  
   **Then** they can complete the decision **regardless** of task mentor assignment — **`@PreAuthorize("hasRole('MENTOR') or hasRole('ADMIN')")`** on POST handlers (same role gate as queue/detail GET).

9. **Mentor scope on POST**  
   **Given** a **mentor** (non-admin) tries to approve/reject a review whose **`task.mentor`** is not the current user  
   **When** the request is processed  
   **Then** **403** is returned — **reuse / mirror** the scope rules already used in **`MentorReviewService.getReviewForDetailOrThrow`** (task mentor vs current `UserAccount`).

10. **Responsive layout**  
    **Given** the review detail page is viewed at **768px** viewport  
    **When** the layout collapses  
    **Then** the **`col-md-4`** action panel appears **below** the **`col-md-8`** issues list as a full-width block, Approve/Reject remain **full width**  
    **Note:** Epic text also mentions `table-responsive` on the **queue** table — that is already required in Story **4.1**; do **not** regress [`review-queue.html`](../../src/main/resources/templates/mentor/review-queue.html).

## Tasks / Subtasks

- [x] **Repository: mentor detail fetch** (AC: 1–2)  
  - [x] Replace or extend the detail loader so **one** read path `JOIN FETCH`es **`task` → `course`**, **`intern`**, and **`issues`** (avoid N+1 when rendering many issues).  
  - [x] Today [`findByIdWithTaskAndTaskMentor`](../../src/main/java/com/examinai/review/TaskReviewRepository.java) only fetches task + `task.mentor` — **insufficient** for header + issues.

- [x] **`ReviewPersistenceService.saveMentorDecision`** (AC: 4–7)  
  - [x] Add `@Transactional` method that: loads review by id (with pessimistic or fresh read as appropriate); **only** allows transition when current status is **`LLM_EVALUATED`** (reject double-submit / stale POST with **409** or **400** — pick one and document in Javadoc).  
  - [x] Sets **`status`**, **`mentorResult`** (string **`APPROVED`** / **`REJECTED`** per AC 6), **`mentorRemarks`** (nullable).  
  - [x] Publishes **`MentorDecisionEvent`** **after** persist, matching the contract in [architecture.md](../planning-artifacts/architecture.md#communication-patterns) (create the `record` in `com.examinai.review` if missing).  
  - [x] **Do not** add business logic that prevents mentor overriding `llmResult`.

- [x] **`MentorReviewController` POSTs + real detail GET** (AC: 4–5, 8–9)  
  - [x] `GET /mentor/reviews/{reviewId}` → return **`mentor/review-detail`** (new or rename from placeholder), with model containing fully loaded `TaskReview` (or a dedicated DTO if you keep controllers thin — prefer consistency with existing patterns).  
  - [x] `POST .../approve` and `POST .../reject` → call service → **`redirect:/mentor/reviews`**; **`@PreAuthorize`** on each method.  
  - [x] Enforce mentor scope on POST (admin bypass) — extract shared helper on service layer to avoid duplicating ID resolution logic.

- [x] **Thymeleaf: `review-detail.html` + fragments** (AC: 1–3, 10)  
  - [x] Layout: `row` → `col-md-8` (issues list) + `col-md-4` (action panel); ensure column order works on small screens (panel below content).  
  - [x] Add `templates/fragments/ai-feedback-card.html` (or name aligned with team) exporting **`aiIssueCard`**.  
  - [x] Add [`templates/fragments/mentor-action-panel.html`](../../src/main/resources/templates/fragments/mentor-action-panel.html) exporting **`mentorActionPanel`**.  
  - [x] Forms: **CSRF** + `th:action` for POST; two separate forms (approve/reject) **or** one form with hidden `decision` — either is fine if PRG and semantics stay clear.  
  - [x] Remove or replace [`review-detail-placeholder.html`](../../src/main/resources/templates/mentor/review-detail-placeholder.html) so production uses the real template.

- [x] **Tests** (AC: 4–9)  
  - [x] **`@WebMvcTest(MentorReviewController.class)`**: mentor **403** on another mentor’s review POST; admin **200** + redirect; intern **403**; happy path **302** to queue.  
  - [x] **Service / integration test** for `saveMentorDecision`: from `LLM_EVALUATED` to APPROVED/REJECTED; override case; illegal transition from non-`LLM_EVALUATED` state.  
  - [x] Optional: assert **`MentorDecisionEvent`** published (mock `ApplicationEventPublisher` or `@SpyBean`).

### Review Findings

- [x] [Review][Patch] Align `aiIssueCard` Thymeleaf fragment with AC — use `th:fragment="aiIssueCard(issue)"` and pass `issue` from `review-detail.html` instead of relying on a parent `th:object` wrapper [`src/main/resources/templates/fragments/ai-feedback-card.html`, `src/main/resources/templates/mentor/review-detail.html`] — fixed 2026-04-22
- [x] [Review][Patch] Enforce server-side max length for `mentorRemarks` (match the UI `maxlength="2000"`) so direct POSTs cannot bypass the browser limit [`ReviewPersistenceService.java` + `ReviewPersistenceServiceTest`] — fixed 2026-04-22
- [x] [Review][Defer] `#strings.toUpperCase` on `llmResult` uses the default locale — in rare locales (e.g. Turkish) edge-case strings could mis-map to APPROVE/REJECT badges [`review-detail.html`, `mentor-action-panel.html`] — deferred, pre-existing Thymeleaf pattern

## Dev Notes

### Brownfield snapshot (do not reinvent)

- **Placeholder detail** today: [`MentorReviewController.reviewDetail`](../../src/main/java/com/examinai/review/MentorReviewController.java) returns [`review-detail-placeholder.html`](../../src/main/resources/templates/mentor/review-detail-placeholder.html) — **replace** with full UI.  
- **Scope pattern:** [`MentorReviewService.getReviewForDetailOrThrow`](../../src/main/java/com/examinai/review/MentorReviewService.java) — **mentor** must match **`task.getMentor()`**; **admin** bypass. Extend this for POST decision endpoints.  
- **Events:** [`AiReviewCompleteEvent`](../../src/main/java/com/examinai/review/AiReviewCompleteEvent.java) + [`AiReviewNotificationListener`](../../src/main/java/com/examinai/review/AiReviewNotificationListener.java) show the **AFTER_COMMIT** listener pattern — **`MentorDecisionEvent`** should follow the same for Epic **5.1** listeners (Story 4.2 **publishes** the event; full email handling may arrive in 5.1).  
- **CSS tokens** for AI vs mentor: [`.ai-content`](../../src/main/resources/static/css/custom.css), [`.ai-code-block`](../../src/main/resources/static/css/custom.css), [`.mentor-action-panel`](../../src/main/resources/static/css/custom.css).  
- **No `MentorDecisionEvent` in codebase yet** — add the `record` per architecture; **no** duplicate event types.

### Architecture compliance

- **MPA + Thymeleaf + PRG** for mentor decisions — POST must **redirect**, never return the detail view directly on success. [Source: `architecture.md` — API & Communication Patterns, PRG Rule]  
- **`@PreAuthorize` on every controller method** — not only class level. [Source: `architecture.md` — `@PreAuthorize` Placement]  
- **`@Transactional` on services**, not controllers. [Source: `architecture.md` — Process Patterns]  
- **Package-by-feature:** keep changes under `com.examinai.review` and `templates/mentor/`, `templates/fragments/`. [Source: `architecture.md` — Code Organization]  
- **Epic vs architecture doc:** Epic AC mandates **`ReviewPersistenceService.saveMentorDecision()`** for **`LLM_EVALUATED` → terminal** transitions. An older sentence in `architecture.md` mentions `ReviewPipelineService only` for a subset of transitions — **for mentor decisions, follow the epic and centralize in `ReviewPersistenceService`**.

### Technical requirements (guardrails)

| Topic | Requirement |
|--------|-------------|
| AI label | **“AI Suggestion: …”** only — never imply the AI row is the final **Decision** |
| `mentor_result` | Store **`APPROVED`** / **`REJECTED`** string consistent with AC 6 |
| Decision eligibility | Only from **`LLM_EVALUATED`** unless product later expands; reject duplicate POST after decision |
| CSRF | Spring Security default — forms must include token |
| Accessibility | `role="region"`, `aria-label` on code blocks; semantic `<main id="main-content">`; labels for textarea |

### Library / stack

- **Spring Boot 3.5.13**, **Java 21** ([`pom.xml`](../../pom.xml)).  
- **Thymeleaf** + Layout Dialect — extend [`layout/base.html`](../../src/main/resources/templates/layout/base.html).  
- **Spring Security 6** method security — same role constants as Story 4.1.

### File / module touch map

| Area | Files likely touched |
|------|----------------------|
| Persistence | `ReviewPersistenceService.java` — `saveMentorDecision`; new `MentorDecisionEvent.java` |
| Service | `MentorReviewService.java` — detail fetch + decision guard helpers; or small `MentorDecisionService` if split improves clarity |
| Repository | `TaskReviewRepository.java` — mentor detail query with issues/course/intern |
| Controller | `MentorReviewController.java` — GET detail, POST approve/reject |
| Templates | `mentor/review-detail.html`, `fragments/ai-feedback-card.html`, `fragments/mentor-action-panel.html`; remove/replace placeholder |
| Tests | `MentorReviewControllerTest.java`, new service test class |

### Testing requirements

- Reuse patterns from [`MentorReviewControllerTest.java`](../../src/test/java/com/examinai/review/MentorReviewControllerTest.java) (`@WebMvcTest`, `@MockBean`, `@WithMockUser`).  
- Cover **403** matrix for POST same as GET detail scope.

### Previous story intelligence (4.1)

- Queue links already target **`/mentor/reviews/{id}`** — detail must be production-ready.  
- Query params on queue: `status`, `internId`, `taskId` — after decision redirect, landing on **default queue** (`LLM_EVALUATED`) is acceptable unless you preserve query string (optional enhancement, not in AC).  
- Review findings from 4.1: invalid filter params → **400**; cross-filtered dropdowns — do not break when touching shared services.

### Git intelligence (recent commits)

- **`340d3d9`** — Story 4.1: `MentorReviewController`, `MentorReviewService`, queue template, `TaskReviewRepository.findMentorQueue`, placeholder detail.

### Latest tech / versions

- Pin to **Spring Boot 3.5.13** / **Java 21** at implementation time; no new AI dependencies for this story.

### Project context reference

- No `project-context.md` in repo — use this file + [`architecture.md`](../planning-artifacts/architecture.md) + [`epics.md`](../planning-artifacts/epics.md).

### UX references

- **UX-DR10 / DR11 / DR12:** Mentor detail — two-column layout, sticky decision panel, breadcrumb back to queue, AI vs mentor visual hierarchy [Source: `ux-design-specification.md` — Component Library / Journey 3].  
- **Principle:** “AI assists, humans decides” — wording and styling must reinforce draft vs final decision.

## Dev Agent Record

### Agent Model Used

{{agent_model_name_version}}

### Debug Log References

### Completion Notes List

- Mentor detail: `TaskReviewRepository.findByIdForMentorDetail` JOIN FETCH task, course, task.mentor, intern, issues; `MentorReviewService` refactors load + `assertMentorScopeForReview`; `ensureMentorCanActOnReview` used before `saveMentorDecision` on POST.
- `saveMentorDecision`: only `LLM_EVALUATED` → terminal; else **409** (documented in Javadoc); `mentorResult` `APPROVED`/`REJECTED`; `MentorDecisionEvent` after save per architecture.
- Thymeleaf: AI cards use `fragments/ai-feedback-card` with parent `th:object` wrapper so `*{}` works with external includes; `mentorActionPanel(review=${taskReview})` for sticky panel; placeholder template removed.
- `AiReviewNotificationListener` extended with `onMentorDecision` (log, AFTER_COMMIT) for Epic 5.1.
- `SecurityIntegrationTest`: `@MockBean` `ReviewPersistenceService` for slice context.

### File List

- `src/main/java/com/examinai/review/MentorDecisionEvent.java` (new)
- `src/main/java/com/examinai/review/TaskReviewRepository.java`
- `src/main/java/com/examinai/review/MentorReviewService.java`
- `src/main/java/com/examinai/review/ReviewPersistenceService.java`
- `src/main/java/com/examinai/review/MentorReviewController.java`
- `src/main/java/com/examinai/review/AiReviewNotificationListener.java`
- `src/main/resources/templates/mentor/review-detail.html` (new)
- `src/main/resources/templates/fragments/ai-feedback-card.html` (new)
- `src/main/resources/templates/fragments/mentor-action-panel.html` (new)
- `src/main/resources/templates/mentor/review-detail-placeholder.html` (deleted)
- `src/test/java/com/examinai/review/MentorReviewControllerTest.java`
- `src/test/java/com/examinai/review/MentorReviewServiceTest.java`
- `src/test/java/com/examinai/review/ReviewPersistenceServiceTest.java` (new)
- `src/test/java/com/examinai/user/SecurityIntegrationTest.java`

### Change Log

- 2026-04-22: Story 4.2 — mentor review detail UI, `saveMentorDecision` + `MentorDecisionEvent`, POST approve/reject with mentor scope, tests and listener.

---

**Story completion status:** done

### Clarifications / open questions (non-blocking)

- Exact HTTP status for “already decided” review (**409** vs **400**) — document in service Javadoc once chosen.  
- Whether **`MentorDecisionEvent`** listeners are no-op stubs in 4.2 or fully wired — **publishing** is in scope; **email** is Epic **5.1**.
