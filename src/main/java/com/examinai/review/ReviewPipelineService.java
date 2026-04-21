package com.examinai.review;

import com.examinai.task.Task;
import com.examinai.task.TaskRepository;
import com.examinai.user.UserAccount;
import com.examinai.user.UserAccountRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;
import java.net.SocketTimeoutException;
import java.time.LocalDateTime;
import java.util.concurrent.TimeoutException;

@Service
public class ReviewPipelineService {

    private static final Logger log = LoggerFactory.getLogger(ReviewPipelineService.class);

    static final String MSG_GITHUB_404 = "GitHub PR not found. Check your PR number and resubmit.";
    static final String MSG_GITHUB_403 = "GitHub token has insufficient permissions.";
    static final String MSG_GITHUB_429 = "GitHub API rate limited. Wait a few minutes and resubmit.";
    static final String MSG_LLM_TIMEOUT = "AI review timed out. Try resubmitting.";
    static final String MSG_LLM_PARSE = "AI review failed. Try resubmitting.";

    private final TaskReviewRepository taskReviewRepository;
    private final TaskRepository taskRepository;
    private final UserAccountRepository userAccountRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final GitHubClient gitHubClient;
    private final LLMReviewService llmReviewService;
    private final ReviewPersistenceService reviewPersistenceService;

    public ReviewPipelineService(TaskReviewRepository taskReviewRepository,
                                 TaskRepository taskRepository,
                                 UserAccountRepository userAccountRepository,
                                 ApplicationEventPublisher eventPublisher,
                                 GitHubClient gitHubClient,
                                 LLMReviewService llmReviewService,
                                 ReviewPersistenceService reviewPersistenceService) {
        this.taskReviewRepository = taskReviewRepository;
        this.taskRepository = taskRepository;
        this.userAccountRepository = userAccountRepository;
        this.eventPublisher = eventPublisher;
        this.gitHubClient = gitHubClient;
        this.llmReviewService = llmReviewService;
        this.reviewPersistenceService = reviewPersistenceService;
    }

    @Transactional
    public Long submitPendingReview(Long taskId, String username, String repoOwner, String repoName, int prNumber) {
        UserAccount intern = userAccountRepository.findByUsername(username)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
        Task task = taskRepository.findByIdWithCourseAndMentor(taskId)
            .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));
        TaskReview tr = new TaskReview();
        tr.setTask(task);
        tr.setIntern(intern);
        tr.setMentor(task.getMentor());
        tr.setStatus(ReviewStatus.PENDING);
        tr.setDateCreated(LocalDateTime.now());
        tr.setGithubRepoOwner(repoOwner);
        tr.setGithubRepoName(repoName);
        tr.setGithubPrNumber(prNumber);
        if (taskReviewRepository.existsByTask_IdAndIntern_IdAndStatus(taskId, intern.getId(), ReviewStatus.PENDING)) {
            throw new IllegalStateException("A review is already pending for this task.");
        }
        tr = taskReviewRepository.saveAndFlush(tr);
        eventPublisher.publishEvent(new ReviewPipelineStartedEvent(tr.getId()));
        return tr.getId();
    }

    public void runPipeline(Long reviewId) {
        try {
            executePipeline(reviewId);
        } catch (Exception e) {
            log.error("Async review pipeline failed reviewId={}", reviewId, e);
            String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            reviewPersistenceService.markPipelineError(reviewId, "Review pipeline failed: " + msg);
        }
    }

    private void executePipeline(Long reviewId) {
        TaskReview tr = taskReviewRepository.findByIdWithTask(reviewId)
            .orElseThrow(() -> new IllegalStateException("TaskReview not found: " + reviewId));
        String diff;
        try {
            diff = gitHubClient.getPrDiff(
                tr.getGithubRepoOwner(),
                tr.getGithubRepoName(),
                tr.getGithubPrNumber()
            );
        } catch (RestClientResponseException e) {
            int code = e.getStatusCode().value();
            if (code == 404) {
                reviewPersistenceService.markPipelineError(reviewId, MSG_GITHUB_404);
                return;
            }
            if (code == 403) {
                reviewPersistenceService.markPipelineError(reviewId, MSG_GITHUB_403);
                return;
            }
            if (code == 429) {
                reviewPersistenceService.markPipelineError(reviewId, MSG_GITHUB_429);
                return;
            }
            throw e;
        }
        String taskDescription = tr.getTask().getTaskDescription() != null
            ? tr.getTask().getTaskDescription() : "";
        ReviewFeedback feedback;
        try {
            feedback = llmReviewService.review(taskDescription, diff);
        } catch (Exception e) {
            if (isLlmTimeout(e)) {
                reviewPersistenceService.markPipelineError(reviewId, MSG_LLM_TIMEOUT);
                return;
            }
            if (isLlmParseFailure(e)) {
                reviewPersistenceService.markPipelineError(reviewId, MSG_LLM_PARSE);
                return;
            }
            throw e;
        }
        reviewPersistenceService.saveLlmSuccess(reviewId, feedback);
    }

    private static boolean isLlmTimeout(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof SocketTimeoutException || t instanceof TimeoutException) {
                return true;
            }
        }
        if (e instanceof ResourceAccessException rae) {
            String m = rae.getMessage();
            return m != null && m.toLowerCase().contains("timed out");
        }
        return false;
    }

    private static boolean isLlmParseFailure(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof JsonProcessingException) {
                return true;
            }
            if (t instanceof IllegalStateException ise) {
                String m = ise.getMessage();
                if (m != null && m.contains("could not be parsed")) {
                    return true;
                }
            }
        }
        return false;
    }
}
