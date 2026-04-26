package com.examinai.config;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.lang.NonNull;
import org.springframework.core.annotation.Order;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Exposes the current request path for layout navigation highlighting (reliable in all MVC contexts, including
 * Thymeleaf's limited {@code #httpServletRequest} in some tests).
 */
@ControllerAdvice
@Order(0)
public class NavViewAdvice {

    @ModelAttribute
    public void addNavPathAttributes(
        @NonNull HttpServletRequest request,
        @NonNull Model model) {
        String cp = request.getContextPath();
        model.addAttribute("navContextPath", cp == null ? "" : cp);
        String uri = request.getRequestURI();
        model.addAttribute("navRequestUri", uri == null ? "" : uri);
    }
}
