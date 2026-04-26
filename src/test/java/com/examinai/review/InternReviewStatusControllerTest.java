package com.examinai.review;

import com.examinai.config.SecurityConfig;
import com.examinai.course.Course;
import com.examinai.task.InternTaskAccessService;
import com.examinai.task.Task;
import com.examinai.user.UserAccount;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import java.util.Optional;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = InternReviewStatusController.class)
@Import(SecurityConfig.class)
class InternReviewStatusControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    TaskReviewRepository taskReviewRepository;

    @MockBean
    InternTaskAccessService internTaskAccessService;

    @BeforeEach
    void setUp() {
        doNothing().when(internTaskAccessService).assertInternReadAccess(any(), any());
        UserAccount intern = new UserAccount();
        intern.setUsername("intern");
        TaskReview tr = new TaskReview();
        tr.setIntern(intern);
        tr.setStatus(ReviewStatus.PENDING);
        Task t = new Task();
        t.setTaskName("T");
        Course c = new Course();
        c.setCourseName("C");
        t.setCourse(c);
        tr.setTask(t);
        when(taskReviewRepository.findByIdForInternStatusPage(eq(1L))).thenReturn(Optional.of(tr));
    }

    @Test
    @WithMockUser(username = "intern", roles = "INTERN")
    void reviewStatusPageIncludesPollingScript() throws Exception {
        mockMvc.perform(get("/intern/reviews/1"))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("/js/review-polling.js")));
    }

    @Test
    @WithMockUser(username = "other", roles = "INTERN")
    void otherInternsReview_returns404() throws Exception {
        UserAccount owner = new UserAccount();
        owner.setUsername("intern");
        TaskReview tr = new TaskReview();
        tr.setIntern(owner);
        tr.setStatus(ReviewStatus.PENDING);
        Task t = new Task();
        t.setTaskName("T");
        Course c = new Course();
        c.setCourseName("C");
        t.setCourse(c);
        tr.setTask(t);
        when(taskReviewRepository.findByIdForInternStatusPage(eq(5L))).thenReturn(Optional.of(tr));

        mockMvc.perform(get("/intern/reviews/5"))
            .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void admin_canViewAnotherInternsReview() throws Exception {
        mockMvc.perform(get("/intern/reviews/1"))
            .andExpect(status().isOk());
    }

    @Test
    void unauthenticated_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/intern/reviews/1"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @WithMockUser(username = "intern", roles = "INTERN")
    void unknownReview_returns404() throws Exception {
        when(taskReviewRepository.findByIdForInternStatusPage(eq(999L))).thenReturn(Optional.empty());

        mockMvc.perform(get("/intern/reviews/999"))
            .andExpect(status().isNotFound());
    }
}
