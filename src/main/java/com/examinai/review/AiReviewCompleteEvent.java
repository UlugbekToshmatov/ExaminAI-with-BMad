package com.examinai.review;

public record AiReviewCompleteEvent(
    Long reviewId,
    Long mentorId,
    String internName,
    String courseName,
    String taskName
) {}
