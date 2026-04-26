package com.examinai.course;

import com.examinai.stack.StackService;
import jakarta.servlet.http.HttpServletRequest;
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
    private final StackService stackService;

    public CourseController(CourseService courseService, StackService stackService) {
        this.courseService = courseService;
        this.stackService = stackService;
    }

    @GetMapping({"/mentor/courses", "/admin/courses"})
    @PreAuthorize("hasRole('MENTOR') or hasRole('ADMIN')")
    public String list(Model model, HttpServletRequest request) {
        model.addAttribute("courses", courseService.findAll());
        model.addAttribute("baseCourseUrl", baseCourseUrl(request));
        return "admin/course-list";
    }

    @GetMapping({"/mentor/courses/new", "/admin/courses/new"})
    @PreAuthorize("hasRole('MENTOR') or hasRole('ADMIN')")
    public String newForm(Model model, HttpServletRequest request) {
        model.addAttribute("courseDto", new CourseCreateDto());
        model.addAttribute("stacks", stackService.findAll());
        model.addAttribute("baseCourseUrl", baseCourseUrl(request));
        return "admin/course-form";
    }

    @PostMapping({"/mentor/courses", "/admin/courses"})
    @PreAuthorize("hasRole('MENTOR') or hasRole('ADMIN')")
    public String create(@Valid @ModelAttribute("courseDto") CourseCreateDto dto,
                         BindingResult bindingResult, Model model, HttpServletRequest request) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("stacks", stackService.findAll());
            model.addAttribute("baseCourseUrl", baseCourseUrl(request));
            return "admin/course-form";
        }
        courseService.create(dto);
        return "redirect:" + baseCourseUrl(request);
    }

    @GetMapping({"/mentor/courses/{id}/edit", "/admin/courses/{id}/edit"})
    @PreAuthorize("hasRole('MENTOR') or hasRole('ADMIN')")
    public String editForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes,
                           HttpServletRequest request) {
        try {
            Course course = courseService.findById(id);
            CourseCreateDto dto = new CourseCreateDto();
            dto.setCourseName(course.getCourseName());
            dto.setTechnology(course.getTechnology());
            dto.setStackId(course.getStack().getId());
            model.addAttribute("courseDto", dto);
            model.addAttribute("courseId", id);
            model.addAttribute("stacks", stackService.findAll());
            model.addAttribute("baseCourseUrl", baseCourseUrl(request));
            return "admin/course-form";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:" + baseCourseUrl(request);
        }
    }

    @PostMapping({"/mentor/courses/{id}/edit", "/admin/courses/{id}/edit"})
    @PreAuthorize("hasRole('MENTOR') or hasRole('ADMIN')")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("courseDto") CourseCreateDto dto,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes,
                         HttpServletRequest request) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("courseId", id);
            model.addAttribute("stacks", stackService.findAll());
            model.addAttribute("baseCourseUrl", baseCourseUrl(request));
            return "admin/course-form";
        }
        try {
            courseService.update(id, dto);
            return "redirect:" + baseCourseUrl(request);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:" + baseCourseUrl(request);
        }
    }

    @PostMapping({"/mentor/courses/{id}/delete", "/admin/courses/{id}/delete"})
    @PreAuthorize("hasRole('MENTOR') or hasRole('ADMIN')")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes, HttpServletRequest request) {
        try {
            courseService.delete(id);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:" + baseCourseUrl(request);
    }

    private String baseCourseUrl(HttpServletRequest request) {
        String ctx = request.getContextPath();
        String uri = request.getRequestURI();
        return uri.startsWith(ctx + "/admin") ? ctx + "/admin/courses" : ctx + "/mentor/courses";
    }
}
