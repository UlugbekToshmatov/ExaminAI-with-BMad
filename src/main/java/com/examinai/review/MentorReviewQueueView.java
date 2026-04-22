package com.examinai.review;

import java.util.List;

/**
 * Model for {@code mentor/review-queue}: table rows, filter form values, and filter dropdown options.
 */
public final class MentorReviewQueueView {

    private final List<TaskReview> reviews;
    private final ReviewStatus selectedStatus;
    private final long selectedInternId;
    private final long selectedTaskId;
    private final List<MentorQueueLabelValue> internOptions;
    private final List<MentorQueueLabelValue> taskOptions;

    public MentorReviewQueueView(
        List<TaskReview> reviews,
        ReviewStatus selectedStatus,
        long selectedInternId,
        long selectedTaskId,
        List<MentorQueueLabelValue> internOptions,
        List<MentorQueueLabelValue> taskOptions) {
        this.reviews = reviews;
        this.selectedStatus = selectedStatus;
        this.selectedInternId = selectedInternId;
        this.selectedTaskId = selectedTaskId;
        this.internOptions = internOptions;
        this.taskOptions = taskOptions;
    }

    public List<TaskReview> getReviews() {
        return reviews;
    }

    public ReviewStatus getSelectedStatus() {
        return selectedStatus;
    }

    public long getSelectedInternId() {
        return selectedInternId;
    }

    public long getSelectedTaskId() {
        return selectedTaskId;
    }

    public List<MentorQueueLabelValue> getInternOptions() {
        return internOptions;
    }

    public List<MentorQueueLabelValue> getTaskOptions() {
        return taskOptions;
    }
}
