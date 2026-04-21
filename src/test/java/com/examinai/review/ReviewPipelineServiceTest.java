package com.examinai.review;

import com.examinai.course.Course;
import com.examinai.task.Task;
import com.examinai.task.TaskRepository;
import com.examinai.user.UserAccount;
import com.examinai.user.UserAccountRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.fasterxml.jackson.core.JsonParseException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewPipelineServiceTest {

    @Mock TaskReviewRepository taskReviewRepository;
    @Mock TaskRepository taskRepository;
    @Mock UserAccountRepository userAccountRepository;
    @Mock ApplicationEventPublisher eventPublisher;
    @Mock GitHubClient gitHubClient;
    @Mock LLMReviewService llmReviewService;
    @Mock ReviewPersistenceService reviewPersistenceService;

    @InjectMocks
    ReviewPipelineService reviewPipelineService;

    @Test
    void submit_flushesPendingThenPublishesPipelineEvent() {
        UserAccount intern = new UserAccount();
        intern.setUsername("intern");
        UserAccount mentor = new UserAccount();
        mentor.setUsername("mentor");
        Course course = new Course();
        course.setCourseName("C");
        Task task = new Task();
        task.setTaskName("T");
        task.setTaskDescription("Do thing");
        task.setCourse(course);
        task.setMentor(mentor);

        when(userAccountRepository.findByUsername("intern")).thenReturn(Optional.of(intern));
        when(taskRepository.findByIdWithCourseAndMentor(7L)).thenReturn(Optional.of(task));
        when(taskReviewRepository.saveAndFlush(any(TaskReview.class))).thenAnswer(inv -> {
            TaskReview tr = inv.getArgument(0);
            tr.setId(100L);
            return tr;
        });

        Long id = reviewPipelineService.submitPendingReview(7L, "intern", "o", "r", 3);

        assertThat(id).isEqualTo(100L);
        var inOrder = inOrder(taskReviewRepository, eventPublisher);
        inOrder.verify(taskReviewRepository).saveAndFlush(any(TaskReview.class));
        inOrder.verify(eventPublisher).publishEvent(any(ReviewPipelineStartedEvent.class));
    }

    @Test
    void runPipeline_fetchesGithubAfterLoadingReview_thenPersistsLlm() {
        UserAccount mentor = new UserAccount();
        Task task = new Task();
        task.setTaskDescription("desc");
        TaskReview tr = new TaskReview();
        tr.setTask(task);
        tr.setGithubRepoOwner("a");
        tr.setGithubRepoName("b");
        tr.setGithubPrNumber(9);

        when(taskReviewRepository.findByIdWithTask(5L)).thenReturn(Optional.of(tr));
        when(gitHubClient.getPrDiff("a", "b", 9)).thenReturn("diff");
        when(llmReviewService.review("desc", "diff")).thenReturn(new ReviewFeedback("PASS", java.util.List.of()));

        reviewPipelineService.runPipeline(5L);

        var order = inOrder(taskReviewRepository, gitHubClient, llmReviewService, reviewPersistenceService);
        order.verify(taskReviewRepository).findByIdWithTask(5L);
        order.verify(gitHubClient).getPrDiff("a", "b", 9);
        order.verify(llmReviewService).review("desc", "diff");
        order.verify(reviewPersistenceService).saveLlmSuccess(eq(5L), any(ReviewFeedback.class));
    }

    @Test
    void runPipeline_failure_marksError() {
        when(taskReviewRepository.findByIdWithTask(1L)).thenThrow(new RuntimeException("boom"));

        reviewPipelineService.runPipeline(1L);

        verify(reviewPersistenceService).markPipelineError(eq(1L), org.mockito.ArgumentMatchers.contains("boom"));
    }

    @Test
    void runPipeline_github404_setsExactUserMessage() {
        TaskReview tr = reviewStub();
        when(taskReviewRepository.findByIdWithTask(8L)).thenReturn(Optional.of(tr));
        when(gitHubClient.getPrDiff("a", "b", 9)).thenThrow(
            HttpClientErrorException.create(HttpStatus.NOT_FOUND, "Not Found",
                null, null, StandardCharsets.UTF_8));

        reviewPipelineService.runPipeline(8L);

        verify(reviewPersistenceService).markPipelineError(8L, ReviewPipelineService.MSG_GITHUB_404);
        verify(reviewPersistenceService, never()).saveLlmSuccess(any(), any());
    }

    @Test
    void runPipeline_github403_setsExactUserMessage() {
        TaskReview tr = reviewStub();
        when(taskReviewRepository.findByIdWithTask(8L)).thenReturn(Optional.of(tr));
        when(gitHubClient.getPrDiff("a", "b", 9)).thenThrow(
            HttpClientErrorException.create(HttpStatus.FORBIDDEN, "Forbidden",
                null, null, StandardCharsets.UTF_8));

        reviewPipelineService.runPipeline(8L);

        verify(reviewPersistenceService).markPipelineError(8L, ReviewPipelineService.MSG_GITHUB_403);
    }

    @Test
    void runPipeline_github429_setsExactUserMessage() {
        TaskReview tr = reviewStub();
        when(taskReviewRepository.findByIdWithTask(8L)).thenReturn(Optional.of(tr));
        when(gitHubClient.getPrDiff("a", "b", 9)).thenThrow(
            HttpClientErrorException.create(HttpStatus.TOO_MANY_REQUESTS, "Too Many",
                null, null, StandardCharsets.UTF_8));

        reviewPipelineService.runPipeline(8L);

        verify(reviewPersistenceService).markPipelineError(8L, ReviewPipelineService.MSG_GITHUB_429);
    }

    @Test
    void runPipeline_llmReadTimeout_setsExactUserMessage() {
        TaskReview tr = reviewStub();
        when(taskReviewRepository.findByIdWithTask(8L)).thenReturn(Optional.of(tr));
        when(gitHubClient.getPrDiff("a", "b", 9)).thenReturn("diff");
        when(llmReviewService.review("desc", "diff")).thenThrow(new ResourceAccessException("Read timed out"));

        reviewPipelineService.runPipeline(8L);

        verify(reviewPersistenceService).markPipelineError(8L, ReviewPipelineService.MSG_LLM_TIMEOUT);
    }

    @Test
    void runPipeline_llmSocketTimeout_setsExactUserMessage() {
        TaskReview tr = reviewStub();
        when(taskReviewRepository.findByIdWithTask(8L)).thenReturn(Optional.of(tr));
        when(gitHubClient.getPrDiff("a", "b", 9)).thenReturn("diff");
        doAnswer(inv -> {
            throw new RuntimeException(new SocketTimeoutException("timeout"));
        }).when(llmReviewService).review(anyString(), anyString());

        reviewPipelineService.runPipeline(8L);

        verify(reviewPersistenceService).markPipelineError(8L, ReviewPipelineService.MSG_LLM_TIMEOUT);
    }

    @Test
    void runPipeline_llmParseFailure_setsExactUserMessage() {
        TaskReview tr = reviewStub();
        when(taskReviewRepository.findByIdWithTask(8L)).thenReturn(Optional.of(tr));
        when(gitHubClient.getPrDiff("a", "b", 9)).thenReturn("diff");
        when(llmReviewService.review("desc", "diff"))
            .thenThrow(new RuntimeException(new JsonParseException(null, "bad json")));

        reviewPipelineService.runPipeline(8L);

        verify(reviewPersistenceService).markPipelineError(8L, ReviewPipelineService.MSG_LLM_PARSE);
    }

    @Test
    void runPipeline_llmNullFeedbackIllegalState_setsExactUserMessage() {
        TaskReview tr = reviewStub();
        when(taskReviewRepository.findByIdWithTask(8L)).thenReturn(Optional.of(tr));
        when(gitHubClient.getPrDiff("a", "b", 9)).thenReturn("diff");
        when(llmReviewService.review("desc", "diff"))
            .thenThrow(new IllegalStateException("LLM output could not be parsed into ReviewFeedback: {}"));

        reviewPipelineService.runPipeline(8L);

        verify(reviewPersistenceService).markPipelineError(8L, ReviewPipelineService.MSG_LLM_PARSE);
    }

    private static TaskReview reviewStub() {
        Task task = new Task();
        task.setTaskDescription("desc");
        TaskReview tr = new TaskReview();
        tr.setTask(task);
        tr.setGithubRepoOwner("a");
        tr.setGithubRepoName("b");
        tr.setGithubPrNumber(9);
        return tr;
    }
}
