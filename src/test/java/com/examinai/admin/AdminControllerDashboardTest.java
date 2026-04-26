package com.examinai.admin;

import com.examinai.config.NavViewAdvice;
import com.examinai.config.SecurityConfig;
import com.examinai.review.MentorQueueLabelValue;
import com.examinai.review.ReviewStatus;
import com.examinai.review.TaskReview;
import com.examinai.user.UserAccount;
import com.examinai.user.UserAccountService;
import com.examinai.course.Course;
import com.examinai.stack.StackService;
import com.examinai.task.Task;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(controllers = AdminController.class)
@Import({ SecurityConfig.class, NavViewAdvice.class })
class AdminControllerDashboardTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    UserAccountService userAccountService;

    @MockBean
    AdminDashboardService adminDashboardService;

    @MockBean
    StackService stackService;

    @Test
    @WithMockUser(roles = "ADMIN")
    void dashboard_rendersViewAndModel() throws Exception {
        UserAccount intern = new UserAccount();
        intern.setUsername("u1");
        Course course = new Course();
        course.setCourseName("C1");
        Task task = new Task();
        task.setTaskName("T1");
        task.setCourse(course);
        TaskReview r = new TaskReview();
        r.setStatus(ReviewStatus.PENDING);
        r.setMentorRemarks("ok");
        r.setIntern(intern);
        r.setTask(task);
        r.setDateCreated(java.time.LocalDateTime.of(2026, 4, 1, 10, 0));

        AdminDashboardView view = new AdminDashboardView(
            List.of(r),
            null,
            0L,
            0L,
            List.of(new MentorQueueLabelValue(1L, "u1")),
            List.of(new MentorQueueLabelValue(2L, "T1"))
        );
        when(adminDashboardService.loadDashboard(null, null, null)).thenReturn(view);

        mockMvc.perform(get("/admin/dashboard"))
            .andExpect(status().isOk())
            .andExpect(view().name("admin/dashboard"))
            .andExpect(model().attribute("adminDashboard", view))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("u1")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("C1")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void dashboard_forwardsQueryParams() throws Exception {
        AdminDashboardView v = new AdminDashboardView(
            List.of(), ReviewStatus.APPROVED, 2L, 3L, List.of(), List.of());
        when(adminDashboardService.loadDashboard(eq("2"), eq("3"), eq("APPROVED"))).thenReturn(v);

        mockMvc.perform(get("/admin/dashboard")
                .param("internId", "2")
                .param("taskId", "3")
                .param("status", "APPROVED"))
            .andExpect(status().isOk())
            .andExpect(model().attribute("adminDashboard", v));
    }

    @Test
    @WithMockUser(roles = "INTERN")
    void dashboard_forbiddenForIntern() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
            .andExpect(status().isForbidden());
        verify(adminDashboardService, never()).loadDashboard(any(), any(), any());
    }

    @Test
    @WithMockUser(roles = "MENTOR")
    void dashboard_forbiddenForMentor() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
            .andExpect(status().isForbidden());
        verify(adminDashboardService, never()).loadDashboard(any(), any(), any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void dashboard_invalidStatus_returns400() throws Exception {
        when(adminDashboardService.loadDashboard(any(), any(), eq("NOT_A_STATUS")))
            .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST));

        mockMvc.perform(get("/admin/dashboard").param("status", "NOT_A_STATUS"))
            .andExpect(status().isBadRequest());
    }
}
