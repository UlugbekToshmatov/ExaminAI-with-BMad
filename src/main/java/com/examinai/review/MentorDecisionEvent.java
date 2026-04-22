package com.examinai.review;

/**
 * Published after a mentor (or admin) finalizes a review. Listeners (e.g. email in Epic 5.1) should use
 * {@code @TransactionalEventListener(phase = AFTER_COMMIT)} and must not rethrow.
 */
public record MentorDecisionEvent(
    Long reviewId,
    Long internId,
    String courseName,
    String taskName,
    ReviewStatus finalStatus,
    String remarks
) {}
