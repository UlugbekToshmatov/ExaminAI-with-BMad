package com.examinai.notification;

import com.examinai.review.AiReviewCompleteEvent;
import com.examinai.review.MentorDecisionEvent;
import com.examinai.user.UserAccount;
import com.examinai.user.UserAccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Optional;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final JavaMailSender mailSender;
    private final UserAccountRepository userAccountRepository;

    @Value("${spring.mail.username:}")
    private String mailFrom;

    public NotificationService(JavaMailSender mailSender, UserAccountRepository userAccountRepository) {
        this.mailSender = mailSender;
        this.userAccountRepository = userAccountRepository;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void onAiReviewComplete(AiReviewCompleteEvent event) {
        try {
            if (event.mentorId() == null) {
                log.info("Skipping mentor email: no mentor for reviewId={}", event.reviewId());
                return;
            }
            Optional<UserAccount> mentorOpt = userAccountRepository.findById(event.mentorId());
            if (mentorOpt.isEmpty()) {
                log.info("Skipping mentor email: user not found mentorId={} reviewId={}", event.mentorId(), event.reviewId());
                return;
            }
            String to = mentorOpt.get().getEmail();
            if (isBlankEmail(to)) {
                log.info("Skipping mentor email: no address mentorId={} reviewId={}", event.mentorId(), event.reviewId());
                return;
            }
            String body = buildMentorAiCompleteBody(event);
            SimpleMailMessage msg = new SimpleMailMessage();
            setFromIfConfigured(msg);
            msg.setTo(to);
            msg.setSubject("Examin-AI: AI review ready — " + nullSafe(event.taskName()));
            msg.setText(body);
            mailSender.send(msg);
        } catch (Exception e) {
            log.error(
                "Failed to send mentor AI-complete email reviewId={} mentorId={}: {}",
                event.reviewId(),
                event.mentorId(),
                e.toString(),
                e
            );
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void onMentorDecision(MentorDecisionEvent event) {
        try {
            if (event.internId() == null) {
                log.info("Skipping intern email: no intern for reviewId={}", event.reviewId());
                return;
            }
            Optional<UserAccount> internOpt = userAccountRepository.findById(event.internId());
            if (internOpt.isEmpty()) {
                log.info("Skipping intern email: user not found internId={} reviewId={}", event.internId(), event.reviewId());
                return;
            }
            String to = internOpt.get().getEmail();
            if (isBlankEmail(to)) {
                log.info("Skipping intern email: no address internId={} reviewId={}", event.internId(), event.reviewId());
                return;
            }
            String body = buildInternDecisionBody(event);
            SimpleMailMessage msg = new SimpleMailMessage();
            setFromIfConfigured(msg);
            msg.setTo(to);
            msg.setSubject("Examin-AI: Mentor decision — " + nullSafe(event.taskName()));
            msg.setText(body);
            mailSender.send(msg);
        } catch (Exception e) {
            log.error(
                "Failed to send intern decision email reviewId={} internId={}: {}",
                event.reviewId(),
                event.internId(),
                e.toString(),
                e
            );
        }
    }

    String buildMentorAiCompleteBody(AiReviewCompleteEvent event) {
        String internLine = "Intern (username): " + nullSafe(event.internName());
        String course = "Course: " + nullSafe(event.courseName());
        String task = "Task: " + nullSafe(event.taskName());
        String link = "Open the review: /mentor/reviews/" + event.reviewId();
        return String.join(
            System.lineSeparator(),
            "The AI review for a submission is complete.",
            "",
            internLine,
            course,
            task,
            "",
            link
        );
    }

    String buildInternDecisionBody(MentorDecisionEvent event) {
        String remarks = event.remarks();
        String remarksLine =
            (remarks == null || remarks.isBlank()) ? "No remarks provided." : remarks;
        return String.join(
            System.lineSeparator(),
            "Your submission has a final mentor decision.",
            "",
            "Course: " + nullSafe(event.courseName()),
            "Task: " + nullSafe(event.taskName()),
            "Status: " + event.finalStatus().name(),
            "Mentor remarks: " + remarksLine
        );
    }

    private void setFromIfConfigured(SimpleMailMessage msg) {
        if (mailFrom != null && !mailFrom.isBlank()) {
            msg.setFrom(mailFrom);
        }
    }

    private static boolean isBlankEmail(String s) {
        return s == null || s.isBlank();
    }

    private static String nullSafe(String s) {
        return s != null ? s : "";
    }
}
