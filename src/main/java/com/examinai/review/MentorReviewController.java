package com.examinai.review;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/mentor")
public class MentorReviewController {

    private final MentorReviewService mentorReviewService;
    private final ReviewPersistenceService reviewPersistenceService;

    public MentorReviewController(
        MentorReviewService mentorReviewService,
        ReviewPersistenceService reviewPersistenceService) {
        this.mentorReviewService = mentorReviewService;
        this.reviewPersistenceService = reviewPersistenceService;
    }

    /**
     * Query params: {@code status} (defaults to {@link ReviewStatus#LLM_EVALUATED}),
     * {@code internId}, {@code taskId} (optional; empty for "all").
     * Non-blank but invalid enum or numeric values yield HTTP 400.
     */
    @GetMapping("/reviews")
    @PreAuthorize("hasRole('MENTOR') or hasRole('ADMIN')")
    public String reviewQueue(
        @RequestParam(name = "status", required = false) String status,
        @RequestParam(name = "internId", required = false) String internId,
        @RequestParam(name = "taskId", required = false) String taskId,
        Authentication auth,
        Model model) {
        model.addAttribute("queue", mentorReviewService.loadQueue(auth, status, internId, taskId));
        return "mentor/review-queue";
    }

    @GetMapping("/reviews/{reviewId}")
    @PreAuthorize("hasRole('MENTOR') or hasRole('ADMIN')")
    public String reviewDetail(@PathVariable("reviewId") long reviewId, Authentication auth, Model model) {
        model.addAttribute("taskReview", mentorReviewService.getReviewForDetailOrThrow(reviewId, auth));
        return "mentor/review-detail";
    }

    @PostMapping("/reviews/{reviewId}/approve")
    @PreAuthorize("hasRole('MENTOR') or hasRole('ADMIN')")
    public String approveReview(
        @PathVariable("reviewId") long reviewId,
        @RequestParam(name = "mentorRemarks", required = false) String mentorRemarks,
        Authentication auth) {
        return submitDecision(reviewId, true, mentorRemarks, auth);
    }

    @PostMapping("/reviews/{reviewId}/reject")
    @PreAuthorize("hasRole('MENTOR') or hasRole('ADMIN')")
    public String rejectReview(
        @PathVariable("reviewId") long reviewId,
        @RequestParam(name = "mentorRemarks", required = false) String mentorRemarks,
        Authentication auth) {
        return submitDecision(reviewId, false, mentorRemarks, auth);
    }

    private String submitDecision(
        long reviewId,
        boolean approve,
        String mentorRemarks,
        Authentication auth) {
        mentorReviewService.ensureMentorCanActOnReview(reviewId, auth);
        reviewPersistenceService.saveMentorDecision(reviewId, approve, mentorRemarks);
        return "redirect:/mentor/reviews";
    }
}
