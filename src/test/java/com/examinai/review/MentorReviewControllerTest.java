package com.examinai.review;

import com.examinai.config.SecurityConfig;
import com.examinai.course.Course;
import com.examinai.task.Task;
import com.examinai.user.UserAccount;
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
import org.springframework.security.core.Authentication;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(controllers = MentorReviewController.class)
@Import(SecurityConfig.class)
class MentorReviewControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    MentorReviewService mentorReviewService;

    @MockBean
    ReviewPersistenceService reviewPersistenceService;

    @Test
    @WithMockUser(roles = "INTERN")
    void queue_forbiddenForIntern() throws Exception {
        mockMvc.perform(get("/mentor/reviews"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "mentor1", roles = "MENTOR")
    void queue_loadsViewAndPassesQueueModel() throws Exception {
        MentorReviewQueueView view = new MentorReviewQueueView(
            List.of(), ReviewStatus.LLM_EVALUATED, 0L, 0L, List.of(), List.of());
        when(mentorReviewService.loadQueue(any(), isNull(), isNull(), isNull())).thenReturn(view);

        mockMvc.perform(get("/mentor/reviews"))
            .andExpect(status().isOk())
            .andExpect(view().name("mentor/review-queue"))
            .andExpect(model().attribute("queue", view));

        verify(mentorReviewService).loadQueue(any(), isNull(), isNull(), isNull());
    }

    @Test
    @WithMockUser(username = "mentor1", roles = "MENTOR")
    void queue_forwardsQueryParamsToService() throws Exception {
        MentorReviewQueueView q = new MentorReviewQueueView(
            List.of(), ReviewStatus.PENDING, 2L, 3L, List.of(), List.of());
        when(mentorReviewService.loadQueue(any(), eq("PENDING"), eq("2"), eq("3"))).thenReturn(q);

        mockMvc.perform(get("/mentor/reviews")
                .param("status", "PENDING")
                .param("internId", "2")
                .param("taskId", "3"))
            .andExpect(status().isOk())
            .andExpect(model().attribute("queue", q));
    }

    @Test
    @WithMockUser(username = "mentor1", roles = "MENTOR")
    void queue_emptyStateShowsExactCopy() throws Exception {
        when(mentorReviewService.loadQueue(any(), any(), any(), any()))
            .thenReturn(new MentorReviewQueueView(
                List.of(), ReviewStatus.LLM_EVALUATED, 0L, 0L, List.of(), List.of()));

        mockMvc.perform(get("/mentor/reviews"))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString(
                "No reviews awaiting your decision. Check back after interns submit.")));
    }

    @Test
    @WithMockUser(username = "mentor1", roles = "MENTOR")
    void reviewDetail_returnsDetailView() throws Exception {
        UserAccount intern = new UserAccount();
        intern.setUsername("intern1");
        Course course = new Course();
        course.setCourseName("Course A");
        Task task = new Task();
        task.setTaskName("Task X");
        task.setCourse(course);
        TaskReview tr = new TaskReview();
        tr.setId(42L);
        tr.setStatus(ReviewStatus.LLM_EVALUATED);
        tr.setLlmResult("APPROVE");
        tr.setIntern(intern);
        tr.setTask(task);
        TaskReviewIssue i1 = new TaskReviewIssue();
        i1.setLine(5);
        i1.setCode("x = 1");
        i1.setIssue("test issue");
        i1.setImprovement("improve");
        tr.getIssues().add(i1);
        when(mentorReviewService.getReviewForDetailOrThrow(eq(42L), any())).thenReturn(tr);

        mockMvc.perform(get("/mentor/reviews/42"))
            .andExpect(status().isOk())
            .andExpect(view().name("mentor/review-detail"))
            .andExpect(model().attribute("taskReview", tr));
    }

    @Test
    @WithMockUser(username = "mentor1", roles = "MENTOR")
    void reviewDetail_notFound_returns404() throws Exception {
        when(mentorReviewService.getReviewForDetailOrThrow(eq(99L), any()))
            .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));

        mockMvc.perform(get("/mentor/reviews/99"))
            .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "mentor1", roles = "MENTOR")
    void reviewDetail_forbidden_returns403() throws Exception {
        when(mentorReviewService.getReviewForDetailOrThrow(eq(5L), any()))
            .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN));

        mockMvc.perform(get("/mentor/reviews/5"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "INTERN")
    void approve_forbiddenForIntern() throws Exception {
        mockMvc.perform(post("/mentor/reviews/1/approve")
                .with(csrf())
                .param("mentorRemarks", ""))
            .andExpect(status().isForbidden());
        verify(mentorReviewService, never()).ensureMentorCanActOnReview(anyLong(), any(Authentication.class));
    }

    @Test
    @WithMockUser(username = "m1", roles = "MENTOR")
    void approve_mentorWrongScope_returns403() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN))
            .when(mentorReviewService).ensureMentorCanActOnReview(eq(8L), any());

        mockMvc.perform(post("/mentor/reviews/8/approve")
                .with(csrf())
                .param("mentorRemarks", "x"))
            .andExpect(status().isForbidden());
        verify(reviewPersistenceService, never()).saveMentorDecision(any(), anyBoolean(), any());
    }

    @Test
    @WithMockUser(username = "mentor1", roles = "MENTOR")
    void approve_mentorHappyPath_redirectsToQueue() throws Exception {
        doNothing().when(mentorReviewService).ensureMentorCanActOnReview(eq(3L), any());
        doNothing().when(reviewPersistenceService).saveMentorDecision(eq(3L), eq(true), eq("r"));

        mockMvc.perform(post("/mentor/reviews/3/approve")
                .with(csrf())
                .param("mentorRemarks", "r"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/mentor/reviews"));

        verify(reviewPersistenceService).saveMentorDecision(3L, true, "r");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void approve_admin_mayOverrideMentorScope() throws Exception {
        doNothing().when(mentorReviewService).ensureMentorCanActOnReview(eq(50L), any());
        doNothing().when(reviewPersistenceService).saveMentorDecision(eq(50L), eq(true), isNull());

        mockMvc.perform(post("/mentor/reviews/50/approve")
                .with(csrf()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/mentor/reviews"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void reject_happyPath_redirects() throws Exception {
        doNothing().when(mentorReviewService).ensureMentorCanActOnReview(eq(4L), any());
        doNothing().when(reviewPersistenceService).saveMentorDecision(eq(4L), eq(false), isNull());

        mockMvc.perform(post("/mentor/reviews/4/reject")
                .with(csrf()))
            .andExpect(redirectedUrl("/mentor/reviews"));
    }
}
