package com.examinai.task;

import com.examinai.review.ReviewStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import java.util.Collections;
import java.util.List;

@Controller
@RequestMapping("/intern")
public class InternTaskController {

    private final TaskService taskService;

    public InternTaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping("/tasks")
    @PreAuthorize("hasRole('INTERN') or hasRole('ADMIN')")
    public String taskList(Model model, Authentication auth) {
        List<TaskWithReview> tasks = taskService.findForInternByUsername(auth.getName());
        if (tasks == null) tasks = Collections.emptyList();
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
