package com.examinai.review;

import com.examinai.config.SecurityConfig;
import com.examinai.course.Course;
import com.examinai.task.Task;
import com.examinai.task.TaskService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.ArgumentMatchers.eq;
import java.util.List;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ReviewSubmissionController.class)
@Import(SecurityConfig.class)
class ReviewSubmissionControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    TaskService taskService;

    @MockBean
    ReviewPipelineService reviewPipelineService;

    @Test
    @WithMockUser(roles = "INTERN")
    void submit_persistsViaServiceAndRedirects() throws Exception {
        when(reviewPipelineService.submitPendingReview(eq(1L), eq("user"), eq("acme"), eq("demo"), eq(42)))
            .thenReturn(99L);

        mockMvc.perform(post("/intern/tasks/1/submit")
                .with(csrf())
                .param("repoOwner", "acme")
                .param("repoName", "demo")
                .param("prNumber", "42"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/intern/reviews/99"));

        verify(reviewPipelineService).submitPendingReview(1L, "user", "acme", "demo", 42);
    }

    @Test
    @WithMockUser(roles = "INTERN")
    void submit_validationError_returnsForm() throws Exception {
        Task task = new Task();
        task.setTaskName("T");
        Course course = new Course();
        course.setCourseName("C");
        task.setCourse(course);
        when(taskService.findForInternTaskDetail("user", 1L)).thenReturn(task);
        when(taskService.findSubmissionHistoryForInternTask("user", 1L)).thenReturn(List.of());

        mockMvc.perform(post("/intern/tasks/1/submit")
                .with(csrf())
                .param("repoOwner", "")
                .param("repoName", "demo")
                .param("prNumber", "42"))
            .andExpect(status().isOk())
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.view().name("intern/task-detail"));

        verify(taskService).findSubmissionHistoryForInternTask("user", 1L);
    }
}
