package com.examinai.admin;

import com.examinai.user.Role;
import com.examinai.user.UserAccountCreateDto;
import com.examinai.user.UserAccountService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final UserAccountService userAccountService;
    private final AdminDashboardService adminDashboardService;

    public AdminController(
        UserAccountService userAccountService,
        AdminDashboardService adminDashboardService) {
        this.userAccountService = userAccountService;
        this.adminDashboardService = adminDashboardService;
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('ADMIN')")
    public String dashboard(
        @RequestParam(value = "internId", required = false) String internId,
        @RequestParam(value = "taskId", required = false) String taskId,
        @RequestParam(value = "status", required = false) String status,
        Model model) {
        model.addAttribute("adminDashboard", adminDashboardService.loadDashboard(internId, taskId, status));
        return "admin/dashboard";
    }

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public String userList(Model model) {
        model.addAttribute("users", userAccountService.findAll());
        return "admin/user-list";
    }

    @GetMapping("/users/new")
    @PreAuthorize("hasRole('ADMIN')")
    public String userForm(Model model) {
        model.addAttribute("createDto", new UserAccountCreateDto());
        model.addAttribute("roles", Role.values());
        return "admin/user-form";
    }

    @PostMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public String createUser(@Valid @ModelAttribute("createDto") UserAccountCreateDto dto,
                              BindingResult bindingResult,
                              Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("roles", Role.values());
            return "admin/user-form";
        }
        try {
            userAccountService.createUser(dto);
            return "redirect:/admin/users";
        } catch (IllegalArgumentException e) {
            bindingResult.rejectValue("username", "duplicate", e.getMessage());
            model.addAttribute("roles", Role.values());
            return "admin/user-form";
        }
    }

    @PostMapping("/users/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public String deactivateUser(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            userAccountService.deactivate(id);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/users";
    }
}
