package com.examinai.review;

import java.util.List;

/**
 * Structured LLM output; JSON shape must match the review prompt and {@code BeanOutputConverter}.
 */
public record ReviewFeedback(String verdict, List<ReviewIssuePayload> issues) {

    public record ReviewIssuePayload(Integer line, String code, String issue, String improvement) {}
}
