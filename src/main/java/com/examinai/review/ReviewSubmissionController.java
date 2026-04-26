package com.examinai.review;

import com.examinai.task.TaskService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/intern")
public class ReviewSubmissionController {

    private final ReviewPipelineService reviewPipelineService;
    private final TaskService taskService;

    public ReviewSubmissionController(ReviewPipelineService reviewPipelineService, TaskService taskService) {
        this.reviewPipelineService = reviewPipelineService;
        this.taskService = taskService;
    }

    @PostMapping("/tasks/{taskId}/submit")
    @PreAuthorize("hasRole('INTERN') or hasRole('ADMIN')")
    public String submit(@PathVariable Long taskId,
                         @Valid @ModelAttribute("submission") ReviewSubmissionDto submission,
                         BindingResult bindingResult,
                         Model model,
                         Authentication auth) {
        if (bindingResult.hasErrors()) {
            var page = taskService.loadInternTaskPage(auth.getName(), taskId);
            model.addAttribute("task", page.task());
            model.addAttribute("submissionHistory", page.submissionHistory());
            model.addAttribute("canSubmit", page.submissionInfo().canSubmit());
            model.addAttribute("submitBlockReason", page.submissionInfo().blockReason());
            return "intern/task-detail";
        }
        Long reviewId = reviewPipelineService.submitPendingReview(
            taskId,
            auth.getName(),
            submission.getRepoOwner().trim(),
            submission.getRepoName().trim(),
            submission.getPrNumber()
        );
        return "redirect:/intern/reviews/" + reviewId;
    }
}
