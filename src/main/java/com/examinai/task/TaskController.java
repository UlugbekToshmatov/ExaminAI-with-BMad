package com.examinai.task;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping({"/mentor/tasks", "/admin/tasks"})
    @PreAuthorize("hasRole('MENTOR') or hasRole('ADMIN')")
    public String list(Model model, HttpServletRequest request) {
        model.addAttribute("tasks", taskService.findAll());
        model.addAttribute("baseTaskUrl", baseTaskUrl(request));
        return "admin/task-list";
    }

    @GetMapping({"/mentor/tasks/new", "/admin/tasks/new"})
    @PreAuthorize("hasRole('MENTOR') or hasRole('ADMIN')")
    public String newForm(Model model, HttpServletRequest request) {
        model.addAttribute("taskDto", new TaskCreateDto());
        model.addAttribute("courses", taskService.findAllCourses());
        model.addAttribute("mentors", taskService.findAllMentors());
        model.addAttribute("baseTaskUrl", baseTaskUrl(request));
        return "admin/task-form";
    }

    @PostMapping({"/mentor/tasks", "/admin/tasks"})
    @PreAuthorize("hasRole('MENTOR') or hasRole('ADMIN')")
    public String create(@Valid @ModelAttribute("taskDto") TaskCreateDto dto,
                         BindingResult bindingResult, Model model,
                         HttpServletRequest request, RedirectAttributes ra) {
        String base = baseTaskUrl(request);
        if (bindingResult.hasErrors()) {
            model.addAttribute("courses", taskService.findAllCourses());
            model.addAttribute("mentors", taskService.findAllMentors());
            model.addAttribute("baseTaskUrl", base);
            return "admin/task-form";
        }
        try {
            taskService.create(dto);
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:" + base;
    }

    @GetMapping({"/mentor/tasks/{id}/edit", "/admin/tasks/{id}/edit"})
    @PreAuthorize("hasRole('MENTOR') or hasRole('ADMIN')")
    public String editForm(@PathVariable Long id, Model model, RedirectAttributes ra,
                           HttpServletRequest request) {
        String base = baseTaskUrl(request);
        try {
            Task task = taskService.findById(id);
            TaskCreateDto dto = new TaskCreateDto();
            dto.setTaskName(task.getTaskName());
            dto.setTaskDescription(task.getTaskDescription());
            dto.setCourseId(task.getCourse().getId());
            dto.setMentorId(task.getMentor().getId());
            model.addAttribute("taskDto", dto);
            model.addAttribute("taskId", id);
            model.addAttribute("courses", taskService.findAllCourses());
            model.addAttribute("mentors", taskService.findAllMentors());
            model.addAttribute("baseTaskUrl", base);
            return "admin/task-form";
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:" + base;
        }
    }

    @PostMapping({"/mentor/tasks/{id}/edit", "/admin/tasks/{id}/edit"})
    @PreAuthorize("hasRole('MENTOR') or hasRole('ADMIN')")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("taskDto") TaskCreateDto dto,
                         BindingResult bindingResult, Model model, RedirectAttributes ra,
                         HttpServletRequest request) {
        String base = baseTaskUrl(request);
        if (bindingResult.hasErrors()) {
            model.addAttribute("taskId", id);
            model.addAttribute("courses", taskService.findAllCourses());
            model.addAttribute("mentors", taskService.findAllMentors());
            model.addAttribute("baseTaskUrl", base);
            return "admin/task-form";
        }
        try {
            taskService.update(id, dto);
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:" + base;
    }

    @PostMapping({"/mentor/tasks/{id}/delete", "/admin/tasks/{id}/delete"})
    @PreAuthorize("hasRole('MENTOR') or hasRole('ADMIN')")
    public String delete(@PathVariable Long id, RedirectAttributes ra, HttpServletRequest request) {
        try {
            taskService.delete(id);
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:" + baseTaskUrl(request);
    }

    private String baseTaskUrl(HttpServletRequest request) {
        String ctx = request.getContextPath();
        String uri = request.getRequestURI();
        return uri.startsWith(ctx + "/admin") ? ctx + "/admin/tasks" : ctx + "/mentor/tasks";
    }
}
