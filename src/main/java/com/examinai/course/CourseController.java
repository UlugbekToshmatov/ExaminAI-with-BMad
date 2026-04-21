package com.examinai.course;

import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class CourseController {

    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    @GetMapping({"/mentor/courses", "/admin/courses"})
    @PreAuthorize("hasRole('MENTOR') or hasRole('ADMIN')")
    public String list(Model model) {
        model.addAttribute("courses", courseService.findAll());
        return "admin/course-list";
    }

    @GetMapping({"/mentor/courses/new", "/admin/courses/new"})
    @PreAuthorize("hasRole('MENTOR') or hasRole('ADMIN')")
    public String newForm(Model model) {
        model.addAttribute("courseDto", new CourseCreateDto());
        return "admin/course-form";
    }

    @PostMapping({"/mentor/courses", "/admin/courses"})
    @PreAuthorize("hasRole('MENTOR') or hasRole('ADMIN')")
    public String create(@Valid @ModelAttribute("courseDto") CourseCreateDto dto,
                         BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return "admin/course-form";
        }
        courseService.create(dto);
        return "redirect:/mentor/courses";
    }

    @GetMapping({"/mentor/courses/{id}/edit", "/admin/courses/{id}/edit"})
    @PreAuthorize("hasRole('MENTOR') or hasRole('ADMIN')")
    public String editForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            Course course = courseService.findById(id);
            CourseCreateDto dto = new CourseCreateDto();
            dto.setCourseName(course.getCourseName());
            dto.setTechnology(course.getTechnology());
            model.addAttribute("courseDto", dto);
            model.addAttribute("courseId", id);
            return "admin/course-form";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/mentor/courses";
        }
    }

    @PostMapping({"/mentor/courses/{id}/edit", "/admin/courses/{id}/edit"})
    @PreAuthorize("hasRole('MENTOR') or hasRole('ADMIN')")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("courseDto") CourseCreateDto dto,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("courseId", id);
            return "admin/course-form";
        }
        try {
            courseService.update(id, dto);
            return "redirect:/mentor/courses";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/mentor/courses";
        }
    }

    @PostMapping({"/mentor/courses/{id}/delete", "/admin/courses/{id}/delete"})
    @PreAuthorize("hasRole('MENTOR') or hasRole('ADMIN')")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            courseService.delete(id);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/mentor/courses";
    }
}
