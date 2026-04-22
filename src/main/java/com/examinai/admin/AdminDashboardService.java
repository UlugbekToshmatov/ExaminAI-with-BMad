package com.examinai.admin;

import com.examinai.review.MentorQueueLabelValue;
import com.examinai.review.MentorReviewService;
import com.examinai.review.ReviewStatus;
import com.examinai.review.TaskReview;
import com.examinai.review.TaskReviewRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Loads the admin cross-intern dashboard with GET filter semantics: invalid query values yield HTTP 400
 * (same approach as {@link com.examinai.review.MentorReviewService#parseRequiredStatus} for status).
 */
@Service
public class AdminDashboardService {

    private final TaskReviewRepository taskReviewRepository;

    public AdminDashboardService(TaskReviewRepository taskReviewRepository) {
        this.taskReviewRepository = taskReviewRepository;
    }

    @Transactional(readOnly = true)
    public AdminDashboardView loadDashboard(String internIdParam, String taskIdParam, String statusParam) {
        long internFilter = parseOptionalId("internId", internIdParam);
        long taskFilter = parseOptionalId("taskId", taskIdParam);
        ReviewStatus status = parseOptionalStatus(statusParam);
        List<TaskReview> reviews = taskReviewRepository.findAdminDashboardRows(internFilter, taskFilter, status);
        List<MentorQueueLabelValue> internOptions =
            taskReviewRepository.findAdminDashboardInternOptions(taskFilter, status);
        List<MentorQueueLabelValue> taskOptions =
            taskReviewRepository.findAdminDashboardTaskOptions(internFilter, status);
        return new AdminDashboardView(reviews, status, internFilter, taskFilter, internOptions, taskOptions);
    }

    /**
     * Empty or missing status param = all {@link ReviewStatus} values. Non-empty and not a valid enum name → 400.
     */
    private ReviewStatus parseOptionalStatus(String statusParam) {
        if (statusParam == null || statusParam.isBlank()) {
            return null;
        }
        try {
            return ReviewStatus.valueOf(statusParam.trim());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status filter");
        }
    }

    private long parseOptionalId(String paramName, String value) {
        if (value == null || value.isBlank()) {
            return MentorReviewService.FILTER_ID_ANY;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid " + paramName);
        }
    }
}
