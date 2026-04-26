package com.examinai.config;

import com.examinai.review.ReviewSubmissionBlockedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@ControllerAdvice
public class ReviewSubmissionExceptionHandler {

    @ExceptionHandler(ReviewSubmissionBlockedException.class)
    public String reviewSubmissionBlocked(ReviewSubmissionBlockedException ex, RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("submissionError", ex.getMessage());
        return "redirect:/intern/tasks/" + ex.getTaskId();
    }
}
