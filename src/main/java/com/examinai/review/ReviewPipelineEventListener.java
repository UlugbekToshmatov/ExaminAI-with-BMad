package com.examinai.review;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class ReviewPipelineEventListener {

    private final ReviewPipelineService reviewPipelineService;

    public ReviewPipelineEventListener(ReviewPipelineService reviewPipelineService) {
        this.reviewPipelineService = reviewPipelineService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void onPipelineQueued(ReviewPipelineStartedEvent event) {
        reviewPipelineService.runPipeline(event.reviewId());
    }
}
