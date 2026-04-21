package com.examinai.review;

import com.examinai.task.Task;
import com.examinai.task.TaskRepository;
import com.examinai.user.UserAccount;
import com.examinai.user.UserAccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service
public class ReviewPipelineService {

    private static final Logger log = LoggerFactory.getLogger(ReviewPipelineService.class);

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
        String diff = gitHubClient.getPrDiff(
            tr.getGithubRepoOwner(),
            tr.getGithubRepoName(),
            tr.getGithubPrNumber()
        );
        String taskDescription = tr.getTask().getTaskDescription() != null
            ? tr.getTask().getTaskDescription() : "";
        ReviewFeedback feedback = llmReviewService.review(taskDescription, diff);
        reviewPersistenceService.saveLlmSuccess(reviewId, feedback);
    }
}
