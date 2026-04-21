package com.examinai.review;

public record ReviewStatusResponse(
    long reviewId,
    String status,
    String displayLabel,
    String errorMessage
) {}
