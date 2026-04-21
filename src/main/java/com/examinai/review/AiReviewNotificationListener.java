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
}
