package com.examinai.review;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/reviews")
public class ReviewStatusController {

    private final TaskReviewRepository taskReviewRepository;

    public ReviewStatusController(TaskReviewRepository taskReviewRepository) {
        this.taskReviewRepository = taskReviewRepository;
    }

    @GetMapping("/{reviewId}/status")
    @PreAuthorize("hasRole('INTERN') or hasRole('ADMIN')")
    public ReviewStatusResponse getStatus(@PathVariable Long reviewId, Authentication auth) {
        TaskReview tr = taskReviewRepository.findByIdWithInternForStatusJson(reviewId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        boolean admin = auth.getAuthorities().stream()
            .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        if (!admin && !tr.getIntern().getUsername().equals(auth.getName())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        ReviewStatus s = tr.getStatus();
        String err = s == ReviewStatus.ERROR ? sanitizeErrorMessageForClient(tr.getErrorMessage()) : null;
        return new ReviewStatusResponse(
            tr.getId(),
            s.name(),
            s.getDisplayLabel(),
            err
        );
    }

    /** Limits pipeline/stack details leaked to the intern browser (AC still allows a short user-facing line). */
    private static String sanitizeErrorMessageForClient(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String firstLine = raw.split("\\R", 2)[0].trim();
        int max = 500;
        if (firstLine.length() <= max) {
            return firstLine;
        }
        return firstLine.substring(0, max) + "…";
    }
}
