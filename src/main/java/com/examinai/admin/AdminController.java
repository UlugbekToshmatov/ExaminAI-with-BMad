package com.examinai.admin;

import com.examinai.user.InternStackAssignmentDto;
import com.examinai.user.Role;
import com.examinai.user.UserAccountCreateDto;
import com.examinai.user.UserAccountService;
import com.examinai.stack.StackService;
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
    private final StackService stackService;

    public AdminController(
        UserAccountService userAccountService,
        AdminDashboardService adminDashboardService,
        StackService stackService) {
        this.userAccountService = userAccountService;
        this.adminDashboardService = adminDashboardService;
        this.stackService = stackService;
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
        model.addAttribute("stacks", stackService.findAll());
        return "admin/user-form";
    }

    @PostMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public String createUser(@Valid @ModelAttribute("createDto") UserAccountCreateDto dto,
                              BindingResult bindingResult,
                              Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("roles", Role.values());
            model.addAttribute("stacks", stackService.findAll());
            return "admin/user-form";
        }
        try {
            userAccountService.createUser(dto);
            return "redirect:/admin/users";
        } catch (IllegalArgumentException e) {
            if (e.getMessage() != null && e.getMessage().contains("Username already")) {
                bindingResult.rejectValue("username", "duplicate", e.getMessage());
            } else {
                bindingResult.reject("createUser", e.getMessage());
            }
            model.addAttribute("roles", Role.values());
            model.addAttribute("stacks", stackService.findAll());
            return "admin/user-form";
        }
    }

    @GetMapping("/users/{id}/stacks")
    @PreAuthorize("hasRole('ADMIN')")
    public String internStacksForm(@PathVariable Long id, Model model) {
        var user = userAccountService.findByIdForStackEditor(id);
        if (user.getRole() != Role.INTERN) {
            return "redirect:/admin/users";
        }
        var dto = new InternStackAssignmentDto();
        dto.setStackIds(user.getStacks().stream().map(s -> s.getId()).sorted().toList());
        model.addAttribute("user", user);
        model.addAttribute("stackDto", dto);
        model.addAttribute("stacks", stackService.findAll());
        return "admin/intern-stacks-form";
    }

    @PostMapping("/users/{id}/stacks")
    @PreAuthorize("hasRole('ADMIN')")
    public String updateInternStacks(
        @PathVariable Long id,
        @ModelAttribute("stackDto") InternStackAssignmentDto stackDto,
        RedirectAttributes redirectAttributes) {
        try {
            userAccountService.updateInternStacks(id, stackDto.getStackIds());
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/users/" + id + "/stacks";
        }
        return "redirect:/admin/users";
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
