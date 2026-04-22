package com.examinai.review;

import com.examinai.user.UserAccount;
import com.examinai.user.UserAccountRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.Authentication;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MentorReviewServiceTest {

    @Mock
    TaskReviewRepository taskReviewRepository;

    @Mock
    UserAccountRepository userAccountRepository;

    @InjectMocks
    MentorReviewService mentorReviewService;

    @Test
    void loadQueue_mentor_usesUserIdAndDefaultStatus() {
        UserAccount mentor = mock(UserAccount.class);
        when(mentor.getId()).thenReturn(9L);
        when(userAccountRepository.findByUsername("m1")).thenReturn(Optional.of(mentor));
        when(taskReviewRepository.findMentorQueue(9L, ReviewStatus.LLM_EVALUATED, 0L, 0L))
            .thenReturn(List.of());
        when(taskReviewRepository.findMentorQueueInternOptions(9L, ReviewStatus.LLM_EVALUATED, 0L))
            .thenReturn(List.of());
        when(taskReviewRepository.findMentorQueueTaskOptions(9L, ReviewStatus.LLM_EVALUATED, 0L))
            .thenReturn(List.of());

        Authentication auth = new UsernamePasswordAuthenticationToken(
            "m1", "n/a", List.of(new SimpleGrantedAuthority("ROLE_MENTOR")));

        mentorReviewService.loadQueue(auth, null, null, null);

        verify(taskReviewRepository).findMentorQueue(9L, ReviewStatus.LLM_EVALUATED, 0L, 0L);
    }

    @Test
    void loadQueue_admin_usesAllMentorsSentinel() {
        when(taskReviewRepository.findMentorQueue(-1L, ReviewStatus.LLM_EVALUATED, 0L, 0L))
            .thenReturn(List.of());
        when(taskReviewRepository.findMentorQueueInternOptions(-1L, ReviewStatus.LLM_EVALUATED, 0L))
            .thenReturn(List.of());
        when(taskReviewRepository.findMentorQueueTaskOptions(-1L, ReviewStatus.LLM_EVALUATED, 0L))
            .thenReturn(List.of());

        Authentication auth = new UsernamePasswordAuthenticationToken(
            "admin", "n/a", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

        mentorReviewService.loadQueue(auth, "", "", "");

        verify(taskReviewRepository).findMentorQueue(-1L, ReviewStatus.LLM_EVALUATED, 0L, 0L);
    }

    @Test
    void loadQueue_appliesOptionalFilters() {
        UserAccount mentor = mock(UserAccount.class);
        when(mentor.getId()).thenReturn(2L);
        when(userAccountRepository.findByUsername("m")).thenReturn(Optional.of(mentor));
        when(taskReviewRepository.findMentorQueue(2L, ReviewStatus.PENDING, 10L, 20L))
            .thenReturn(List.of());
        when(taskReviewRepository.findMentorQueueInternOptions(2L, ReviewStatus.PENDING, 20L))
            .thenReturn(List.of());
        when(taskReviewRepository.findMentorQueueTaskOptions(2L, ReviewStatus.PENDING, 10L))
            .thenReturn(List.of());

        Authentication auth = new UsernamePasswordAuthenticationToken(
            "m", "n/a", List.of(new SimpleGrantedAuthority("ROLE_MENTOR")));

        mentorReviewService.loadQueue(auth, "PENDING", "10", "20");

        verify(taskReviewRepository).findMentorQueue(2L, ReviewStatus.PENDING, 10L, 20L);
        verify(taskReviewRepository).findMentorQueueInternOptions(2L, ReviewStatus.PENDING, 20L);
        verify(taskReviewRepository).findMentorQueueTaskOptions(2L, ReviewStatus.PENDING, 10L);
    }

    @Test
    void loadQueue_invalidStatus_throwsBadRequest() {
        Authentication auth = new UsernamePasswordAuthenticationToken(
            "m1", "n/a", List.of(new SimpleGrantedAuthority("ROLE_MENTOR")));

        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                mentorReviewService.loadQueue(auth, "NOT_A_STATUS", null, null))
            .isInstanceOf(org.springframework.web.server.ResponseStatusException.class)
            .extracting(ex -> ((org.springframework.web.server.ResponseStatusException) ex).getStatusCode().value())
            .isEqualTo(400);
    }

    @Test
    void loadQueue_invalidInternId_throwsBadRequest() {
        Authentication auth = new UsernamePasswordAuthenticationToken(
            "m1", "n/a", List.of(new SimpleGrantedAuthority("ROLE_MENTOR")));

        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                mentorReviewService.loadQueue(auth, null, "x", null))
            .isInstanceOf(org.springframework.web.server.ResponseStatusException.class)
            .extracting(ex -> ((org.springframework.web.server.ResponseStatusException) ex).getStatusCode().value())
            .isEqualTo(400);
    }

    @Test
    void loadQueue_invalidTaskId_throwsBadRequest() {
        Authentication auth = new UsernamePasswordAuthenticationToken(
            "m1", "n/a", List.of(new SimpleGrantedAuthority("ROLE_MENTOR")));

        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                mentorReviewService.loadQueue(auth, null, null, "bogus"))
            .isInstanceOf(org.springframework.web.server.ResponseStatusException.class)
            .extracting(ex -> ((org.springframework.web.server.ResponseStatusException) ex).getStatusCode().value())
            .isEqualTo(400);
    }

    @Test
    void getReviewForDetailOrThrow_mentorMismatch_forbidden() {
        UserAccount taskMentor = mock(UserAccount.class);
        when(taskMentor.getId()).thenReturn(1L);
        com.examinai.task.Task task = new com.examinai.task.Task();
        task.setMentor(taskMentor);
        TaskReview tr = new TaskReview();
        tr.setTask(task);
        when(taskReviewRepository.findByIdWithTaskAndTaskMentor(5L)).thenReturn(Optional.of(tr));

        UserAccount other = mock(UserAccount.class);
        when(other.getId()).thenReturn(99L);
        when(userAccountRepository.findByUsername("other")).thenReturn(Optional.of(other));

        Authentication auth = new UsernamePasswordAuthenticationToken(
            "other", "n/a", List.of(new SimpleGrantedAuthority("ROLE_MENTOR")));

        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                mentorReviewService.getReviewForDetailOrThrow(5L, auth))
            .isInstanceOf(org.springframework.web.server.ResponseStatusException.class)
            .extracting(ex -> ((org.springframework.web.server.ResponseStatusException) ex).getStatusCode().value())
            .isEqualTo(403);
    }

    @Test
    void getReviewForDetailOrThrow_admin_skipsMentorCheck() {
        com.examinai.task.Task task = new com.examinai.task.Task();
        task.setMentor(mock(UserAccount.class));
        TaskReview tr = new TaskReview();
        tr.setId(7L);
        tr.setTask(task);
        when(taskReviewRepository.findByIdWithTaskAndTaskMentor(7L)).thenReturn(Optional.of(tr));

        Authentication auth = new UsernamePasswordAuthenticationToken(
            "admin", "n/a", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

        TaskReview result = mentorReviewService.getReviewForDetailOrThrow(7L, auth);
        assertThat(result.getId()).isEqualTo(7L);
    }
}
