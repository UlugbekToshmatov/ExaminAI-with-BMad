package com.examinai.review;

import com.examinai.course.Course;
import com.examinai.task.Task;
import com.examinai.user.UserAccount;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReviewPersistenceServiceTest {

    @Mock
    TaskReviewRepository taskReviewRepository;
    @Mock
    ApplicationEventPublisher eventPublisher;

    ReviewPersistenceService reviewPersistenceService;

    @BeforeEach
    void setUp() {
        reviewPersistenceService = new ReviewPersistenceService(taskReviewRepository, eventPublisher);
    }

    @Test
    void saveMentorDecision_approve_fromLlmEvaluated_persistsAndPublishes() {
        TaskReview tr = buildReview(ReviewStatus.LLM_EVALUATED, "REJECT");
        when(taskReviewRepository.findByIdForMentorDetail(1L)).thenReturn(Optional.of(tr));
        when(taskReviewRepository.save(any(TaskReview.class))).thenAnswer(inv -> inv.getArgument(0));

        reviewPersistenceService.saveMentorDecision(1L, true, " nice ");

        assertThat(tr.getStatus()).isEqualTo(ReviewStatus.APPROVED);
        assertThat(tr.getMentorResult()).isEqualTo("APPROVED");
        assertThat(tr.getMentorRemarks()).isEqualTo("nice");
        var captor = ArgumentCaptor.forClass(MentorDecisionEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().finalStatus()).isEqualTo(ReviewStatus.APPROVED);
        assertThat(captor.getValue().remarks()).isEqualTo("nice");
    }

    @Test
    void saveMentorDecision_reject_setsRejectedAndPublishes() {
        TaskReview tr = buildReview(ReviewStatus.LLM_EVALUATED, "APPROVE");
        when(taskReviewRepository.findByIdForMentorDetail(2L)).thenReturn(Optional.of(tr));
        when(taskReviewRepository.save(any(TaskReview.class))).thenAnswer(inv -> inv.getArgument(0));

        reviewPersistenceService.saveMentorDecision(2L, false, null);

        assertThat(tr.getStatus()).isEqualTo(ReviewStatus.REJECTED);
        assertThat(tr.getMentorResult()).isEqualTo("REJECTED");
        verify(eventPublisher).publishEvent(any(MentorDecisionEvent.class));
    }

    @Test
    void saveMentorDecision_notLlmEvaluated_throws409() {
        TaskReview tr = buildReview(ReviewStatus.APPROVED, "APPROVE");
        when(taskReviewRepository.findByIdForMentorDetail(3L)).thenReturn(Optional.of(tr));

        assertThatThrownBy(() -> reviewPersistenceService.saveMentorDecision(3L, true, null))
            .isInstanceOf(ResponseStatusException.class)
            .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.CONFLICT));

        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void saveMentorDecision_missingReview_throws404() {
        when(taskReviewRepository.findByIdForMentorDetail(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewPersistenceService.saveMentorDecision(9L, true, null))
            .isInstanceOf(ResponseStatusException.class)
            .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void saveMentorDecision_remarksExceedMax_throws400() {
        TaskReview tr = buildReview(ReviewStatus.LLM_EVALUATED, "APPROVE");
        when(taskReviewRepository.findByIdForMentorDetail(5L)).thenReturn(Optional.of(tr));
        String tooLong = "x".repeat(ReviewPersistenceService.MENTOR_REMARKS_MAX_LEN + 1);

        assertThatThrownBy(() -> reviewPersistenceService.saveMentorDecision(5L, true, tooLong))
            .isInstanceOf(ResponseStatusException.class)
            .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));

        verify(taskReviewRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    private static TaskReview buildReview(ReviewStatus status, String llmResult) {
        UserAccount intern = mock(UserAccount.class);
        when(intern.getId()).thenReturn(10L);
        UserAccount m = mock(UserAccount.class);
        when(m.getId()).thenReturn(3L);
        Course course = new Course();
        course.setCourseName("C1");
        Task task = new Task();
        task.setTaskName("T1");
        task.setCourse(course);
        task.setMentor(m);
        TaskReview tr = new TaskReview();
        tr.setId(1L);
        tr.setStatus(status);
        tr.setLlmResult(llmResult);
        tr.setIntern(intern);
        tr.setTask(task);
        return tr;
    }
}
