package com.examinai.review;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@Service
public class ReviewPersistenceService {

    private static final Logger log = LoggerFactory.getLogger(ReviewPersistenceService.class);

    /** Matches mentor action panel textarea {@code maxlength} in templates. */
    static final int MENTOR_REMARKS_MAX_LEN = 2000;

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

    /**
     * Applies the mentor (or admin) final decision from {@link ReviewStatus#PENDING},
     * {@link ReviewStatus#LLM_EVALUATED}, or {@link ReviewStatus#ERROR} to
     * {@code APPROVED}/{@code REJECTED}. Controllers must not set status directly.
     * <p>
     * Stale or duplicate submit after a decision: HTTP 409 Conflict
     * with no state change. Unknown review id: 404.
     * </p>
     *
     * @param reviewId      persisted review
     * @param approve       {@code true} for approve, {@code false} for reject (mentor may override AI suggestion)
     * @param mentorRemarks optional remarks; blank strings are stored as {@code null}; trimmed length must not exceed {@link #MENTOR_REMARKS_MAX_LEN}
     */
    @Transactional
    public void saveMentorDecision(Long reviewId, boolean approve, String mentorRemarks) {
        TaskReview tr = taskReviewRepository.findByIdForMentorDetail(reviewId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!tr.getStatus().allowsMentorDecision()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Review is not awaiting a mentor decision");
        }
        ReviewStatus terminal = approve ? ReviewStatus.APPROVED : ReviewStatus.REJECTED;
        tr.setStatus(terminal);
        tr.setMentorResult(approve ? "APPROVED" : "REJECTED");
        tr.setMentorRemarks(normalizeRemarks(mentorRemarks));
        taskReviewRepository.save(tr);
        var task = tr.getTask();
        var course = task.getCourse();
        var intern = tr.getIntern();
        eventPublisher.publishEvent(new MentorDecisionEvent(
            reviewId,
            intern.getId(),
            course.getCourseName(),
            task.getTaskName(),
            terminal,
            tr.getMentorRemarks()
        ));
    }

    private static String normalizeRemarks(String mentorRemarks) {
        if (mentorRemarks == null) {
            return null;
        }
        String t = mentorRemarks.trim();
        if (t.isEmpty()) {
            return null;
        }
        if (t.length() > MENTOR_REMARKS_MAX_LEN) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Mentor remarks must be at most " + MENTOR_REMARKS_MAX_LEN + " characters");
        }
        return t;
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
