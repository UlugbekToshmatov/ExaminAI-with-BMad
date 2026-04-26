package com.examinai.admin;

import com.examinai.stack.StackFormDto;
import com.examinai.stack.StackService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/stacks")
public class AdminStackController {

    private final StackService stackService;

    public AdminStackController(StackService stackService) {
        this.stackService = stackService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public String list(Model model) {
        model.addAttribute("stacks", stackService.findAll());
        return "admin/stack-list";
    }

    @GetMapping("/new")
    @PreAuthorize("hasRole('ADMIN')")
    public String newForm(Model model) {
        model.addAttribute("stackDto", new StackFormDto());
        return "admin/stack-form";
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public String create(
        @Valid @ModelAttribute("stackDto") StackFormDto dto,
        BindingResult bindingResult,
        Model model) {
        if (bindingResult.hasErrors()) {
            return "admin/stack-form";
        }
        try {
            stackService.create(dto);
        } catch (IllegalArgumentException e) {
            bindingResult.reject("stackName", e.getMessage());
            return "admin/stack-form";
        }
        return "redirect:/admin/stacks";
    }

    @GetMapping("/{id}/edit")
    @PreAuthorize("hasRole('ADMIN')")
    public String editForm(@PathVariable long id, Model model) {
        var stack = stackService.findByIdForAdmin(id);
        StackFormDto dto = new StackFormDto();
        dto.setName(stack.getName());
        model.addAttribute("stackDto", dto);
        model.addAttribute("stackId", id);
        return "admin/stack-form";
    }

    @PostMapping("/{id}/edit")
    @PreAuthorize("hasRole('ADMIN')")
    public String update(
        @PathVariable long id,
        @Valid @ModelAttribute("stackDto") StackFormDto dto,
        BindingResult bindingResult,
        Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("stackId", id);
            return "admin/stack-form";
        }
        try {
            stackService.update(id, dto);
        } catch (IllegalArgumentException e) {
            bindingResult.reject("stackName", e.getMessage());
            model.addAttribute("stackId", id);
            return "admin/stack-form";
        }
        return "redirect:/admin/stacks";
    }

    @PostMapping("/{id}/delete")
    @PreAuthorize("hasRole('ADMIN')")
    public String delete(@PathVariable long id, RedirectAttributes redirectAttributes) {
        try {
            stackService.delete(id);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/stacks";
    }
}
