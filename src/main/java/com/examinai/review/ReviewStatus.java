package com.examinai.review;

public enum ReviewStatus {
    PENDING,
    LLM_EVALUATED,
    APPROVED,
    REJECTED,
    ERROR;

    /**
     * Submissions the mentor (or admin) may still resolve when AI output is missing or failed.
     */
    public boolean allowsMentorDecision() {
        return switch (this) {
            case PENDING, LLM_EVALUATED, ERROR -> true;
            case APPROVED, REJECTED -> false;
        };
    }

    public String getDisplayLabel() {
        return switch (this) {
            case PENDING -> "Submitted";
            case LLM_EVALUATED -> "Awaiting Mentor Review";
            case APPROVED -> "Approved";
            case REJECTED -> "Rejected";
            case ERROR -> "Review Failed";
        };
    }
}
