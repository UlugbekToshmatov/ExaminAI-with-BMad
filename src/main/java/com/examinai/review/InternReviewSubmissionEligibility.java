package com.examinai.review;

import org.springframework.stereotype.Component;

@Component
public class InternReviewSubmissionEligibility {

    private final TaskReviewRepository taskReviewRepository;

    public InternReviewSubmissionEligibility(TaskReviewRepository taskReviewRepository) {
        this.taskReviewRepository = taskReviewRepository;
    }

    public InternTaskSubmissionInfo evaluateForInternTask(long taskId, long internId) {
        if (taskReviewRepository.existsByTask_IdAndIntern_IdAndStatus(
            taskId, internId, ReviewStatus.APPROVED)) {
            return new InternTaskSubmissionInfo(
                false,
                "This task is already approved. You cannot submit another review.");
        }
        if (taskReviewRepository.existsByTask_IdAndIntern_IdAndStatus(
            taskId, internId, ReviewStatus.PENDING)) {
            return new InternTaskSubmissionInfo(
                false,
                "A review is already in progress for this task. Please wait for the result before submitting again.");
        }
        if (taskReviewRepository.existsByTask_IdAndIntern_IdAndStatus(
            taskId, internId, ReviewStatus.LLM_EVALUATED)) {
            return new InternTaskSubmissionInfo(
                false,
                "A review is awaiting mentor review for this task. You cannot start another submission yet.");
        }
        return InternTaskSubmissionInfo.allowed();
    }

    public void requireCanStartSubmission(long taskId, long internId) {
        InternTaskSubmissionInfo info = evaluateForInternTask(taskId, internId);
        if (!info.canSubmit()) {
            throw new ReviewSubmissionBlockedException(taskId, info.blockReason());
        }
    }
}
