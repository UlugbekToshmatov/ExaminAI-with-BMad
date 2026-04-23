package com.examinai.review;

/** User-visible copy for review flows (keep polling + API + persistence aligned). */
final class ReviewMessages {

    private ReviewMessages() {}

    /**
     * When status is ERROR but no detail was stored (should be rare after {@link ReviewPersistenceService#markPipelineError}).
     */
    static final String ERROR_DETAIL_FALLBACK =
        "Review failed with no saved detail. Try submitting again or contact support.";
}
