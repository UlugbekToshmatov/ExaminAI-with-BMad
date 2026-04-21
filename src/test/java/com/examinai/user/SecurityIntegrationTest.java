package com.examinai.user;

import com.examinai.admin.AdminController;
import com.examinai.config.SecurityConfig;
import com.examinai.review.MentorReviewController;
import com.examinai.task.InternTaskController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {InternTaskController.class, MentorReviewController.class, AdminController.class})
@Import(SecurityConfig.class)
class SecurityIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    CustomUserDetailsService customUserDetailsService;

    @Test
    void unauthenticatedRedirectsToLogin() throws Exception {
        mockMvc.perform(get("/intern/tasks"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @WithMockUser(roles = "INTERN")
    void internAccessesOwnRoutes() throws Exception {
        mockMvc.perform(get("/intern/tasks"))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "INTERN")
    void internCannotAccessMentorRoutes() throws Exception {
        mockMvc.perform(get("/mentor/reviews"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "MENTOR")
    void mentorCannotAccessInternRoutes() throws Exception {
        mockMvc.perform(get("/intern/tasks"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "MENTOR")
    void mentorAccessesOwnRoutes() throws Exception {
        mockMvc.perform(get("/mentor/reviews"))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminAccessesDashboard() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanAccessInternRoutes() throws Exception {
        mockMvc.perform(get("/intern/tasks"))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanAccessMentorRoutes() throws Exception {
        mockMvc.perform(get("/mentor/reviews"))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "INTERN")
    void internCannotAccessAdminRoutes() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "MENTOR")
    void mentorCannotAccessAdminRoutes() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
            .andExpect(status().isForbidden());
    }
}
