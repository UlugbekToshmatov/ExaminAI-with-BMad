package com.examinai.review;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class ReviewPersistenceService {

    private static final Logger log = LoggerFactory.getLogger(ReviewPersistenceService.class);

    private final TaskReviewRepository taskReviewRepository;
    private final ApplicationEventPublisher eventPublisher;

    public ReviewPersistenceService(TaskReviewRepository taskReviewRepository,
                                    ApplicationEventPublisher eventPublisher) {
        this.taskReviewRepository = taskReviewRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public void saveLlmSuccess(Long reviewId, ReviewFeedback feedback) {
        TaskReview tr = taskReviewRepository.findByIdForLlmPersistence(reviewId)
            .orElseThrow(() -> new IllegalStateException("TaskReview not found: " + reviewId));
        tr.getIssues().clear();
        List<ReviewFeedback.ReviewIssuePayload> payloads =
            feedback.issues() != null ? feedback.issues() : List.of();
        for (ReviewFeedback.ReviewIssuePayload item : payloads) {
            TaskReviewIssue issue = new TaskReviewIssue();
            issue.setTaskReview(tr);
            issue.setLine(item.line());
            issue.setCode(item.code());
            issue.setIssue(item.issue() != null ? item.issue() : "");
            issue.setImprovement(item.improvement());
            tr.getIssues().add(issue);
        }
        tr.setStatus(ReviewStatus.LLM_EVALUATED);
        tr.setLlmResult(normalizeVerdict(feedback.verdict()));
        tr.setErrorMessage(null);
        taskReviewRepository.save(tr);
        var task = tr.getTask();
        var course = task.getCourse();
        var intern = tr.getIntern();
        var mentor = tr.getMentor();
        eventPublisher.publishEvent(new AiReviewCompleteEvent(
            reviewId,
            mentor != null ? mentor.getId() : null,
            intern.getUsername(),
            course.getCourseName(),
            task.getTaskName()
        ));
    }

    @Transactional
    public void markPipelineError(Long reviewId, String message) {
        taskReviewRepository.findById(reviewId).ifPresentOrElse(tr -> {
            tr.setStatus(ReviewStatus.ERROR);
            tr.setErrorMessage(truncate(message, 500));
            taskReviewRepository.save(tr);
        }, () -> log.warn("markPipelineError: TaskReview {} not found — status not updated", reviewId));
    }

    private static String normalizeVerdict(String verdict) {
        if (verdict == null) {
            return null;
        }
        String t = verdict.trim();
        if (t.length() > 20) {
            log.warn("normalizeVerdict: verdict truncated from {} to 20 chars: '{}'", t.length(), t);
            return t.substring(0, 20);
        }
        return t;
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() <= max ? s : s.substring(0, max);
    }
}
