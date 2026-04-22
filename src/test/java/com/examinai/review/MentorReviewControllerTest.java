package com.examinai.review;

import com.examinai.config.SecurityConfig;
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
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(controllers = MentorReviewController.class)
@Import(SecurityConfig.class)
class MentorReviewControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    MentorReviewService mentorReviewService;

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
    void reviewDetail_returnsPlaceholderView() throws Exception {
        TaskReview tr = new TaskReview();
        tr.setId(42L);
        when(mentorReviewService.getReviewForDetailOrThrow(eq(42L), any())).thenReturn(tr);

        mockMvc.perform(get("/mentor/reviews/42"))
            .andExpect(status().isOk())
            .andExpect(view().name("mentor/review-detail-placeholder"))
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
}
