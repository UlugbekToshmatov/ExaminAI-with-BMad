package com.examinai.user;

import com.examinai.admin.AdminController;
import com.examinai.admin.AdminDashboardService;
import com.examinai.admin.AdminDashboardView;
import com.examinai.config.SecurityConfig;
import com.examinai.review.InternReviewStatusController;
import com.examinai.review.InternTaskSubmissionInfo;
import com.examinai.review.MentorReviewController;
import com.examinai.review.MentorReviewQueueView;
import com.examinai.review.MentorReviewService;
import com.examinai.review.ReviewPersistenceService;
import com.examinai.review.ReviewStatus;
import com.examinai.review.ReviewSubmissionController;
import com.examinai.review.ReviewPipelineService;
import com.examinai.review.TaskReview;
import com.examinai.review.TaskReviewRepository;
import com.examinai.stack.StackService;
import com.examinai.task.InternTaskAccessService;
import com.examinai.task.InternTaskController;
import com.examinai.task.InternTaskPage;
import com.examinai.task.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import java.util.List;
import java.util.Optional;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {
    InternTaskController.class,
    MentorReviewController.class,
    AdminController.class,
    ReviewSubmissionController.class,
    InternReviewStatusController.class
})
@Import(SecurityConfig.class)
class SecurityIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    CustomUserDetailsService customUserDetailsService;

    @MockBean
    UserAccountService userAccountService;

    @MockBean
    TaskService taskService;

    @MockBean
    ReviewPipelineService reviewPipelineService;

    @MockBean
    TaskReviewRepository taskReviewRepository;

    @MockBean
    MentorReviewService mentorReviewService;

    @MockBean
    AdminDashboardService adminDashboardService;

    @MockBean
    StackService stackService;

    @MockBean
    InternTaskAccessService internTaskAccessService;

    @MockBean
    ReviewPersistenceService reviewPersistenceService;

    @BeforeEach
    void setUp() {
        when(taskService.findForInternByUsername(any())).thenReturn(java.util.Collections.emptyList());
        com.examinai.task.Task stubTask = new com.examinai.task.Task();
        stubTask.setTaskName("stub");
        com.examinai.course.Course c = new com.examinai.course.Course();
        c.setCourseName("Course");
        stubTask.setCourse(c);
        when(taskService.loadInternTaskPage(anyString(), anyLong()))
            .thenReturn(new InternTaskPage(stubTask, List.of(), InternTaskSubmissionInfo.allowed()));
        com.examinai.user.UserAccount intern = new com.examinai.user.UserAccount();
        intern.setUsername("user");
        TaskReview tr = new TaskReview();
        tr.setStatus(ReviewStatus.PENDING);
        tr.setIntern(intern);
        tr.setTask(stubTask);
        when(taskReviewRepository.findByIdForInternStatusPage(eq(1L))).thenReturn(Optional.of(tr));
        when(mentorReviewService.loadQueue(any(), any(), any(), any()))
            .thenReturn(new MentorReviewQueueView(
                List.of(), ReviewStatus.LLM_EVALUATED, 0L, 0L, List.of(), List.of()));
        when(adminDashboardService.loadDashboard(any(), any(), any()))
            .thenReturn(new AdminDashboardView(
                List.of(), null, 0L, 0L, List.of(), List.of()));
    }

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
