package com.examinai.review;

import com.examinai.config.SecurityConfig;
import com.examinai.user.UserAccount;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import java.util.Optional;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ReviewStatusController.class)
@Import(SecurityConfig.class)
class ReviewStatusControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    TaskReviewRepository taskReviewRepository;

    @Test
    @WithMockUser(username = "alice", roles = "INTERN")
    void owner_returns200_andJson() throws Exception {
        TaskReview tr = reviewOwnedBy(7L, "alice", ReviewStatus.LLM_EVALUATED, "err-ignored");
        when(taskReviewRepository.findByIdWithInternForStatusJson(eq(7L)))
            .thenReturn(Optional.of(tr));

        mockMvc.perform(get("/reviews/7/status"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.reviewId", is(7)))
            .andExpect(jsonPath("$.status", is("LLM_EVALUATED")))
            .andExpect(jsonPath("$.displayLabel", is("Awaiting Mentor Review")))
            .andExpect(jsonPath("$.errorMessage", nullValue()));
    }

    @Test
    @WithMockUser(username = "other", roles = "INTERN")
    void otherIntern_returns403() throws Exception {
        TaskReview tr = reviewOwnedBy(7L, "alice", ReviewStatus.PENDING, null);
        when(taskReviewRepository.findByIdWithInternForStatusJson(eq(7L)))
            .thenReturn(Optional.of(tr));

        mockMvc.perform(get("/reviews/7/status"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void admin_canViewAnyReview() throws Exception {
        TaskReview tr = reviewOwnedBy(2L, "alice", ReviewStatus.ERROR, "pipeline failed");
        when(taskReviewRepository.findByIdWithInternForStatusJson(eq(2L)))
            .thenReturn(Optional.of(tr));

        mockMvc.perform(get("/reviews/2/status"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status", is("ERROR")))
            .andExpect(jsonPath("$.displayLabel", is("Review Failed")))
            .andExpect(jsonPath("$.errorMessage", is("pipeline failed")));
    }

    @Test
    @WithMockUser(username = "u", roles = "INTERN")
    void unknownId_returns404() throws Exception {
        when(taskReviewRepository.findByIdWithInternForStatusJson(eq(99L)))
            .thenReturn(Optional.empty());

        mockMvc.perform(get("/reviews/99/status"))
            .andExpect(status().isNotFound());
    }

    @Test
    void unauthenticated_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/reviews/1/status"))
            .andExpect(status().is3xxRedirection());
        verifyNoInteractions(taskReviewRepository);
    }

    @Test
    @WithMockUser(roles = "MENTOR")
    void mentor_cannotCallJsonEndpoint() throws Exception {
        mockMvc.perform(get("/reviews/1/status"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "alice", roles = "INTERN")
    void approved_errorMessageSuppressed() throws Exception {
        TaskReview tr = reviewOwnedBy(3L, "alice", ReviewStatus.APPROVED, "should-be-suppressed");
        when(taskReviewRepository.findByIdWithInternForStatusJson(eq(3L)))
            .thenReturn(Optional.of(tr));

        mockMvc.perform(get("/reviews/3/status"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status", is("APPROVED")))
            .andExpect(jsonPath("$.errorMessage", nullValue()));
    }

    @Test
    @WithMockUser(username = "alice", roles = "INTERN")
    void rejected_errorMessageSuppressed() throws Exception {
        TaskReview tr = reviewOwnedBy(4L, "alice", ReviewStatus.REJECTED, "should-be-suppressed");
        when(taskReviewRepository.findByIdWithInternForStatusJson(eq(4L)))
            .thenReturn(Optional.of(tr));

        mockMvc.perform(get("/reviews/4/status"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status", is("REJECTED")))
            .andExpect(jsonPath("$.errorMessage", nullValue()));
    }

    @Test
    @WithMockUser(username = "alice", roles = "INTERN")
    void error_multilineMessage_returnsFirstLineOnly() throws Exception {
        TaskReview tr = reviewOwnedBy(8L, "alice", ReviewStatus.ERROR, "Something went wrong.\n\tat com.example.Pipeline");
        when(taskReviewRepository.findByIdWithInternForStatusJson(eq(8L)))
            .thenReturn(Optional.of(tr));

        mockMvc.perform(get("/reviews/8/status"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status", is("ERROR")))
            .andExpect(jsonPath("$.errorMessage", is("Something went wrong.")));
    }

    private static TaskReview reviewOwnedBy(long id, String internUsername, ReviewStatus status, String err) {
        UserAccount intern = new UserAccount();
        intern.setUsername(internUsername);
        TaskReview tr = new TaskReview();
        tr.setId(id);
        tr.setIntern(intern);
        tr.setStatus(status);
        tr.setErrorMessage(err);
        return tr;
    }
}
