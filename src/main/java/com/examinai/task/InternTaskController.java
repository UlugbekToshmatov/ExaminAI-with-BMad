package com.examinai.task;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/intern")
public class InternTaskController {

    @GetMapping("/tasks")
    @PreAuthorize("hasRole('INTERN') or hasRole('ADMIN')")
    public String taskList() {
        return "intern/task-list";
    }
}
