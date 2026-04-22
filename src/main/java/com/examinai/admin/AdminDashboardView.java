package com.examinai.admin;

import com.examinai.review.MentorQueueLabelValue;
import com.examinai.review.ReviewStatus;
import com.examinai.review.TaskReview;

import java.util.List;

/**
 * Model for {@code admin/dashboard}: table rows, filter form values, and filter dropdown options.
 */
public final class AdminDashboardView {

    private final List<TaskReview> reviews;
    /**
     * {@code null} means all statuses in the filter (GET param empty / "All").
     */
    private final ReviewStatus selectedStatus;
    private final long selectedInternId;
    private final long selectedTaskId;
    private final List<MentorQueueLabelValue> internOptions;
    private final List<MentorQueueLabelValue> taskOptions;

    public AdminDashboardView(
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
