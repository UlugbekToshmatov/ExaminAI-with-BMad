package com.examinai.review;

public enum ReviewStatus {
    PENDING,
    LLM_EVALUATED,
    APPROVED,
    REJECTED,
    ERROR;

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
