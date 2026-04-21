package com.examinai.review;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/mentor")
public class MentorReviewController {

    @GetMapping("/reviews")
    @PreAuthorize("hasRole('MENTOR') or hasRole('ADMIN')")
    public String reviewQueue() {
        return "mentor/review-queue";
    }
}
