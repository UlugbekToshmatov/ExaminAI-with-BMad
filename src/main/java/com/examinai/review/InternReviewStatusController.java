package com.examinai.review;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;

@Controller
@RequestMapping("/intern/reviews")
public class InternReviewStatusController {

    private final TaskReviewRepository taskReviewRepository;

    public InternReviewStatusController(TaskReviewRepository taskReviewRepository) {
        this.taskReviewRepository = taskReviewRepository;
    }

    @GetMapping("/{reviewId}")
    @PreAuthorize("hasRole('INTERN') or hasRole('ADMIN')")
    public String reviewStatus(@PathVariable Long reviewId, Authentication auth, Model model) {
        TaskReview tr = taskReviewRepository.findByIdForInternStatusPage(reviewId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        boolean admin = auth.getAuthorities().stream()
            .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        if (!admin && !tr.getIntern().getUsername().equals(auth.getName())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        model.addAttribute("review", tr);
        model.addAttribute("submission", new ReviewSubmissionDto());
        return "intern/review-status";
    }
}
