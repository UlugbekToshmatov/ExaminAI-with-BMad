package com.examinai.review;

/**
 * Whether the current user can submit a new review attempt for a task, and a short user-facing reason when not.
 *
 * @param canSubmit   true when a new review submission is allowed
 * @param blockReason non-null when {@code canSubmit} is false; suitable for the task detail page
 */
public record InternTaskSubmissionInfo(boolean canSubmit, String blockReason) {
    public static InternTaskSubmissionInfo allowed() {
        return new InternTaskSubmissionInfo(true, null);
    }
}
