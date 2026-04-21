package com.examinai.task;

import com.examinai.review.ReviewStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import com.examinai.review.ReviewSubmissionDto;
import java.util.List;

@Controller
@RequestMapping("/intern")
public class InternTaskController {

    private final TaskService taskService;

    public InternTaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping("/tasks/{taskId}")
    @PreAuthorize("hasRole('INTERN') or hasRole('ADMIN')")
    public String taskDetail(@PathVariable Long taskId, Model model, Authentication auth) {
        model.addAttribute("task", taskService.findForInternTaskDetail(auth.getName(), taskId));
        model.addAttribute("submission", new ReviewSubmissionDto());
        return "intern/task-detail";
    }

    @GetMapping("/tasks")
    @PreAuthorize("hasRole('INTERN') or hasRole('ADMIN')")
    public String taskList(Model model, Authentication auth) {
        List<TaskWithReview> tasks = taskService.findForInternByUsername(auth.getName());
        int total = tasks.size();
        long approvedCount = tasks.stream()
            .filter(twr -> twr.review() != null
                && twr.review().getStatus() == ReviewStatus.APPROVED)
            .count();
        int progressPercent = total > 0 ? (int) (approvedCount * 100L / total) : 0;
        model.addAttribute("tasks", tasks);
        model.addAttribute("progressPercent", progressPercent);
        model.addAttribute("total", total);
        return "intern/task-list";
    }
}
