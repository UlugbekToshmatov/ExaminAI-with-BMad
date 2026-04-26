package com.examinai.review;

/**
 * Thrown when an intern (or admin using intern routes) cannot start a new review submission.
 * Handled by global MVC advice with redirect back to the task.
 */
public class ReviewSubmissionBlockedException extends RuntimeException {

    private final long taskId;

    public ReviewSubmissionBlockedException(long taskId, String message) {
        super(message);
        this.taskId = taskId;
    }

    public long getTaskId() {
        return taskId;
    }
}
