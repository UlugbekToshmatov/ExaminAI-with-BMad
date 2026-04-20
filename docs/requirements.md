ExaminAI BMAD
——— Attempt with idea and empty context ——
I want an AI powered application that allows mentors to review interns' codes using AI-powered tools that help to achieve readable and clean code.



—— Attempt with application description and stack —— 
# Application Overview

The application allows checking developers’ code using the GitHub API combined with a local LLM such as llama based AI tool.

---

## Main Logic

### Intern
- Can see the task list of the course with its technology (e.g., Java, Python, etc.).
- Can submit a task (by providing a commit branch, PR name, or PR number through a form).
- Can see the review result with comments.
- Can see their own progress.

### Mentor
- Can Add/Edit/Delete task 
- Can see the task list from all interns.
- Can filter tasks by intern name, group name, or task.
- Can perform a review (manually reject or accept a task).

### Admin
- Full access and functionality as a mentor and as an intern
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
8. The LLM returns a JSON response. **Note:** deepseek-r1 models prepend `<think>...</think>` reasoning tokens before the JSON — these must be stripped before parsing.
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

---

### Course
- `id`
- `courseName`
- `technology` (e.g., Java, Python)
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
- `status` (enum: `PENDING`, `LLM_EVALUATED`, `APPROVED`, `REJECTED`)
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
- Local AI agent: Ollama with model `deepseek-r1:8b` (Docker image)
- Prompt templates, system prompt messages, and related configuration must be defined in `.st` template files under `src/main/resources/prompts/` and referenced from `application.yml`
- Generate `.env` file for all configuration variables (DB credentials, GitHub token, mail config, Ollama URL)
- Application, database, and LLM must be deployed as separate Docker containers via **Docker Compose**
- Docker Compose must include **health checks** for PostgreSQL (so the app waits for DB readiness before starting) and a named volume for Ollama model persistence
- The `spring.ai.ollama.chat.options.model` property must be set to `deepseek-r1:8b`
- GitHub API calls must use a `GITHUB_TOKEN` env variable for authentication (`Authorization: Bearer {GITHUB_TOKEN}`)
- LLM response post-processing: strip `<think>...</think>` blocks from deepseek-r1 responses before JSON parsing
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
