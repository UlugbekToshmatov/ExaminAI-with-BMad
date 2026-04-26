ExaminAI BMAD

**Planning alignment:** Product-level FR summary lives in `_bmad-output/planning-artifacts/prd.md` (including FR34–FR39, 2026-04-26). Architecture notes: `_bmad-output/planning-artifacts/architecture.md` (supplement for stacks and submission gating). This file is the **authoritative** requirements + schema narrative for implementers.

——— Attempt with idea and empty context ——
I want an AI powered application that allows mentors to review interns' codes using AI-powered tools that help to achieve readable and clean code.



—— Attempt with application description and stack —— 
# Application Overview

The application allows checking developers’ code using the GitHub API combined with a local LLM such as llama based AI tool.

---

## Main Logic

### Intern
- Is assigned one or more **technology stacks** (e.g. Java, React). Can **only** see and open tasks for courses whose **stack** matches one of those assignments (not the full platform catalog).
- Can see the task list (filtered by stack), course name, and related technology metadata.
- Can submit a task (repo owner, repo name, PR number) when allowed by submission rules (see **Intern review submission rules** below).
- Can see the review result with comments.
- Can see their own progress.

### Mentor
- Can Add/Edit/Delete task 
- Can see the task list from all interns.
- Can filter tasks by intern name, group name, or task.
- Can perform a review (manually reject or accept a task).

### Admin
- Full access and functionality as a mentor and as an intern (except intern-only views still respect the same submission and stack rules when using intern URLs).
- Manages the **stack catalog** (create / edit / delete stacks) and assigns stacks to users and courses as described below.

---

## Review Process

The review is processed **asynchronously** because LLM inference takes 10–60 seconds. The intern receives an immediate response and polls for the result.

### Submission (synchronous — returns immediately)
1. Intern selects a task and submits it (provides repo owner, repo name, and PR number).
2. The application creates a `TaskReview` row with `status = PENDING` and returns `202 Accepted` with a `reviewId`.
3. Intern's UI polls `GET /reviews/{reviewId}/status` every 3 seconds until complete.

### AI Review (asynchronous — background thread)
4. The application calls GitHub and requests PR file diffs via the GitHub API (authenticated with `GITHUB_TOKEN` from env).
5. The task description is retrieved from the database.
6. A system prompt is assembled from the configured prompt template.
7. PR data + task description are sent to the LLM.
8. The LLM returns a JSON response. **Note:** Some models may prepend reasoning tags (e.g. `<think>...</think>`), markdown fences, or prose around JSON — `LlmOutputSanitizer` normalizes output before parsing.
9. The response is parsed and saved to the database (`TaskReview` status → `LLM_EVALUATED`, + `TaskReviewIssue` rows).
10. A notification is sent to the mentor.

### Mentor Review (synchronous)
11. Mentor reviews the LLM result and makes the final decision (approve or reject).
12. The `TaskReview` status is updated to `APPROVED` or `REJECTED` and `mentorResult` is set.
13. The pull request review result is sent to GitHub.
14. A notification is sent to the intern.
15. If `mentorResult = APPROVED`, the intern's `TaskReview` record is marked as the passing attempt (`status = APPROVED`).

---

## Notes

- Each attempt must be saved in the database as a separate row (new `TaskReview` per submission).
- The final decision suggested by the LLM is reviewed by the mentor after receiving a notification containing the LLM evaluation.
- The mentor retains full authority to manually approve or reject the submission.
- Notifications are delivered via **email** (SMTP via `spring-boot-starter-mail`). The notification channel (email) must be configured via env variables (`MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`).
- Mentor notification must contain:
  - Intern name
  - Course name
  - Task name
- Intern notification must contain:
  - Course name
  - Task name
  - Mentor’s result with status and remarks

---

## Database Structure

### UserAccount
- `id`
- `username` (unique)
- `password` (bcrypt-hashed — never plaintext)
- `email`
- `role` (enum: `INTERN`, `MENTOR`, `ADMIN`)
- `active` (default: `true`)
- `dateCreated` (default: current timestamp)
- **Stacks (interns only):** many-to-many with `Stack` via join table `user_account_stack`. At least one stack is required when creating an **INTERN** account. Admins can update an intern’s stacks later.

---

### Stack
- `id`
- `name` (unique, required) — e.g. `Java`, `React`, `TypeScript`
- Managed by **Admin** (full CRUD). A stack cannot be deleted while any **course** or **user** still references it.

---

### Course
- `id`
- `courseName`
- `technology` (optional free text, e.g. Java, Python — distinct from the formal **stack**)
- **`stack_id`** (FK → `Stack`, required) — all tasks in the course share this stack
- `dateCreated` (default: current timestamp)

---

### Task
- `id`
- `taskName`
- `taskDescription`
- `courseId` (FK → Course)
- `mentorId` (FK → UserAccount)
- `dateCreated` (default: current timestamp)

> **Note:** `dateDone` has been removed from `Task`. Task completion is tracked per-intern via `TaskReview.status = APPROVED`. A task is considered done for a given intern when their `TaskReview` has `status = APPROVED`.

---

### TaskReview
*(Many-to-one relationship with Task; one row per submission attempt)*

- `id`
- `taskId` (FK → Task)
- `internId` (FK → UserAccount)
- `mentorId` (FK → UserAccount — assigned reviewer)
- `status` (enum: `PENDING`, `LLM_EVALUATED`, `APPROVED`, `REJECTED`, `ERROR`)
- `llmResult` (e.g., `APPROVED` — AI suggestion)
- `mentorResult` (e.g., `APPROVED` — final human decision)
- `mentorRemarks`
- `dateCreated` (default: current timestamp)

---

### TaskReviewIssue
*(Many-to-one relationship with TaskReview)*

- `id`
- `taskReviewId` (FK → TaskReview)
- `line`
- `code`
- `issue`
- `improvement`

---

## Application Requirements
URGENT: use only compatible versions for all dependencies 
- Spring Boot **3.4.2+** (required for Spring AI 1.0.x compatibility — 3.2.x is incompatible)
- Spring MVC for the front end (Thymeleaf + JavaScript)
- Basic security with a login page using Spring Security
- PostgreSQL 16 database (Docker image)
- Liquibase **4.31.1+** for the DB versioning
- Spring AI **1.0.0** (via BOM import) for communication with LLM
- Local AI agent: Ollama with model `qwen2.5-coder:3b` (Docker image)
- Prompt templates, system prompt messages, and related configuration must be defined in `.st` template files under `src/main/resources/prompts/` and referenced from `application.yml`
- Generate `.env` file for all configuration variables (DB credentials, GitHub token, mail config, Ollama URL)
- Application, database, and LLM must be deployed as separate Docker containers via **Docker Compose**
- Docker Compose must include **health checks** for PostgreSQL (so the app waits for DB readiness before starting) and a named volume for Ollama model persistence
- The `spring.ai.ollama.chat.options.model` property must be set to `qwen2.5-coder:3b`
- GitHub API calls must use a `GITHUB_TOKEN` env variable for authentication (`Authorization: Bearer {GITHUB_TOKEN}`)
- LLM response post-processing: strip common reasoning tags / markdown fences and extract JSON as needed (`LlmOutputSanitizer` + `BeanOutputConverter`) before persisting issues
- Async processing: LLM review calls must run in a dedicated `@Async` thread pool; submission endpoint returns `202 Accepted` with `reviewId` for polling

---

## AI Prompt Configuration

### System Prompt Example

> You are the mentor of the **{course technology}** course. As a senior-level developer, you need to provide pull request review feedback.
> If deficiencies, errors, or deviations from best practices are identified, you must generate a concise and precise description of the issues.
> Feedback must be constructive, professional, and suitable for a learning environment.
> Based on this feedback, you must also provide a final result: **APPROVED** or **REJECTED**.

---

### Review Request Prompt

> Please, provide a pull request review.
> Task: **{task description}**
> Pull request: **{pull request text}**
>
> Send the response as a structured JSON:

```json
{
  "feedback": {
    "taskId": "",
    "issues": [
      {
        "line": "/* code line in the pull request */",
        "code": "/* piece of code with an issue */",
        "issue": "/* issue description */",
        "improvement": "/* improvement suggestion */"
      }
    ],
    "result": "APPROVED or REJECTED"
  }
}
```

> Do not add any additional information to the response.

### On start up
Please, add to the DB
 - admin entry
 - mentor entry
 - intern entry
 - course entry
 - several tasks assigned to the course and intern, and mentor as an owner

---

## Platform updates — stacks, submissions, admin, and UI (2026-04-26)

This section aligns **requirements** with behavior implemented in the codebase. The PRD (`_bmad-output/planning-artifacts/prd.md`) includes a short **alignment** subsection that points here for detail.

### Stacks and visibility
- Each **course** belongs to exactly one **stack**; all tasks in that course inherit it.
- Each **intern** may have **multiple** stacks (e.g. Java + React for full-stack paths).
- Intern **task list** and **task detail** only include tasks whose course stack is in the intern’s assigned stacks. **Admins** browsing intern URLs see the full catalog (unchanged for admin preview).
- Access to task detail, submission, and intern review status is enforced server-side (`InternTaskAccessService` and related checks) so URLs cannot bypass stack rules.

### Admin stack catalog (CRUD)
- Admins can **list / create / edit / delete** stacks under `/admin/stacks`.
- Delete is blocked with a clear message if any course or user still uses the stack.

### Intern review submission rules
- A **new** submission is **not** allowed when, for that intern and task, a review already exists with status:
  - **`APPROVED`** — task is complete for that intern; no further submissions.
  - **`PENDING`** — pipeline not finished; intern must wait.
  - **`LLM_EVALUATED`** — awaiting mentor decision; no parallel submission.
- **`REJECTED`** and **`ERROR`** allow a **new** attempt (resubmit).
- If a duplicate blocked submission is attempted (e.g. second submit while **PENDING**), the app **redirects** back to the task page with a **flash message** instead of a generic error page (`ReviewSubmissionBlockedException` + MVC advice).
- The task detail page shows the submit form only when submission is allowed; otherwise a short **reason** is shown.

### Layout and navigation
- The main navbar highlights the **current section** (active link) using request path + context path. Model attributes `navContextPath` and `navRequestUri` are set by `NavViewAdvice` so Thymeleaf works in all environments (including tests).

### Implementation pointers (for developers / AI context)
- Liquibase: stack tables and `course.stack_id` introduced in changelog `006-stacks-and-course-stack.sql` (see `db/changelog`).
- Aggregated intern task page load: `TaskService.loadInternTaskPage` (task, history, submission eligibility) to avoid redundant fetches.
