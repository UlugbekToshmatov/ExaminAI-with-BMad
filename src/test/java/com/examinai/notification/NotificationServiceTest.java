package com.examinai.notification;

import com.examinai.review.AiReviewCompleteEvent;
import com.examinai.review.MentorDecisionEvent;
import com.examinai.review.ReviewStatus;
import com.examinai.user.UserAccount;
import com.examinai.user.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NotificationServiceTest {

    @Mock
    JavaMailSender mailSender;

    @Mock
    UserAccountRepository userAccountRepository;

    NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(mailSender, userAccountRepository);
        ReflectionTestUtils.setField(notificationService, "mailFrom", "noreply@test.local");
    }

    @Test
    void buildMentorAiCompleteBody_includesInternCourseTaskAndLink() {
        var event = new AiReviewCompleteEvent(42L, 1L, "intern1", "Course A", "Task B");
        String body = notificationService.buildMentorAiCompleteBody(event);
        org.assertj.core.api.Assertions.assertThat(body)
            .contains("Intern (username): intern1")
            .contains("Course: Course A")
            .contains("Task: Task B")
            .contains("/mentor/reviews/42");
    }

    @Test
    void buildInternDecisionBody_includesStatusAndNoRemarksPlaceholderWhenNull() {
        var event = new MentorDecisionEvent(1L, 2L, "C", "T", ReviewStatus.APPROVED, null);
        String body = notificationService.buildInternDecisionBody(event);
        org.assertj.core.api.Assertions.assertThat(body)
            .contains("Course: C")
            .contains("Task: T")
            .contains("Status: APPROVED")
            .contains("Mentor remarks: No remarks provided.");
    }

    @Test
    void buildInternDecisionBody_includesRemarksWhenPresent() {
        var event = new MentorDecisionEvent(1L, 2L, "C", "T", ReviewStatus.REJECTED, "Please retry.");
        String body = notificationService.buildInternDecisionBody(event);
        org.assertj.core.api.Assertions.assertThat(body)
            .contains("Status: REJECTED")
            .contains("Mentor remarks: Please retry.");
    }

    @Test
    void buildInternDecisionBody_blankRemarks_usesNoRemarksPlaceholder() {
        var event = new MentorDecisionEvent(1L, 2L, "C", "T", ReviewStatus.REJECTED, "   ");
        String body = notificationService.buildInternDecisionBody(event);
        org.assertj.core.api.Assertions.assertThat(body).contains("Mentor remarks: No remarks provided.");
    }

    @Test
    void onAiReviewComplete_sendsToMentorWithExpectedContent() {
        var event = new AiReviewCompleteEvent(7L, 3L, "u1", "Co", "Ta");
        UserAccount mentor = new UserAccount();
        mentor.setEmail("mentor@ex.local");
        when(userAccountRepository.findById(3L)).thenReturn(Optional.of(mentor));

        notificationService.onAiReviewComplete(event);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        SimpleMailMessage sent = captor.getValue();
        org.assertj.core.api.Assertions.assertThat(sent.getTo()).containsExactly("mentor@ex.local");
        org.assertj.core.api.Assertions.assertThat(sent.getText()).contains("Intern (username): u1", "Course: Co", "Task: Ta", "/mentor/reviews/7");
    }

    @Test
    void onAiReviewComplete_nullMentorId_skipsSend() {
        var event = new AiReviewCompleteEvent(1L, null, "u", "c", "t");
        notificationService.onAiReviewComplete(event);
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void onAiReviewComplete_blankMentorEmail_skipsSend() {
        var event = new AiReviewCompleteEvent(1L, 5L, "u", "c", "t");
        UserAccount mentor = new UserAccount();
        mentor.setEmail("  ");
        when(userAccountRepository.findById(5L)).thenReturn(Optional.of(mentor));
        notificationService.onAiReviewComplete(event);
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void onMentorDecision_nullInternId_skipsSend() {
        var event = new MentorDecisionEvent(1L, null, "c", "t", ReviewStatus.APPROVED, null);
        notificationService.onMentorDecision(event);
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void onMentorDecision_sendFailure_doesNotPropagate() {
        var event = new MentorDecisionEvent(9L, 4L, "C", "T", ReviewStatus.APPROVED, null);
        UserAccount intern = new UserAccount();
        intern.setEmail("i@test.local");
        when(userAccountRepository.findById(4L)).thenReturn(Optional.of(intern));
        doThrow(new MailSendException("smtp down")).when(mailSender).send(any(SimpleMailMessage.class));

        assertThatCode(() -> notificationService.onMentorDecision(event)).doesNotThrowAnyException();
    }

    @Test
    void onAiReviewComplete_sendFailure_doesNotPropagate() {
        var event = new AiReviewCompleteEvent(8L, 3L, "u1", "Co", "Ta");
        UserAccount mentor = new UserAccount();
        mentor.setEmail("mentor@ex.local");
        when(userAccountRepository.findById(3L)).thenReturn(Optional.of(mentor));
        doThrow(new MailSendException("smtp down")).when(mailSender).send(any(SimpleMailMessage.class));

        assertThatCode(() -> notificationService.onAiReviewComplete(event)).doesNotThrowAnyException();
    }
}
