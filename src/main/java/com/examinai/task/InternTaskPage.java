package com.examinai.task;

import com.examinai.review.InternTaskSubmissionInfo;
import com.examinai.review.TaskReview;
import java.util.List;

/**
 * Envelope for intern task detail view: one round-trip of shared lookups.
 */
public record InternTaskPage(
    Task task,
    List<TaskReview> submissionHistory,
    InternTaskSubmissionInfo submissionInfo) {
}
