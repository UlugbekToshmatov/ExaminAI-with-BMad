package com.examinai.review;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class AiReviewNotificationListener {

    private static final Logger log = LoggerFactory.getLogger(AiReviewNotificationListener.class);

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void onAiReviewComplete(AiReviewCompleteEvent event) {
        try {
            log.info(
                "AI review complete reviewId={} mentorId={} intern={} course={} task={}",
                event.reviewId(),
                event.mentorId(),
                event.internName(),
                event.courseName(),
                event.taskName()
            );
        } catch (RuntimeException e) {
            log.warn("AI review notification handler swallowed error: {}", e.toString());
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void onMentorDecision(MentorDecisionEvent event) {
        try {
            log.info(
                "Mentor decision reviewId={} internId={} course={} task={} finalStatus={} remarksPresent={}",
                event.reviewId(),
                event.internId(),
                event.courseName(),
                event.taskName(),
                event.finalStatus(),
                event.remarks() != null && !event.remarks().isBlank()
            );
        } catch (RuntimeException e) {
            log.warn("Mentor decision notification handler swallowed error: {}", e.toString());
        }
    }
}
