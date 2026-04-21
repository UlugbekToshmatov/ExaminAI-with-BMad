package com.examinai.task;

import com.examinai.review.TaskReview;

public record TaskWithReview(Task task, TaskReview review) {}
