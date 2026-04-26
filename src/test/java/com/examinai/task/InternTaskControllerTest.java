package com.examinai.task;

import com.examinai.config.SecurityConfig;
import com.examinai.course.Course;
import com.examinai.review.InternTaskSubmissionInfo;
import com.examinai.review.ReviewStatus;
import com.examinai.review.ReviewSubmissionDto;
import com.examinai.review.TaskReview;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import java.time.LocalDateTime;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = InternTaskController.class)
@Import(SecurityConfig.class)
class InternTaskControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    TaskService taskService;

    @Test
    @WithMockUser(username = "intern", roles = "INTERN")
    void taskDetail_exposesSubmissionHistoryForListGroup() throws Exception {
        Task task = new Task();
        task.setTaskName("T");
        Course course = new Course();
        course.setCourseName("C");
        task.setCourse(course);

        TaskReview r1 = new TaskReview();
        r1.setStatus(ReviewStatus.APPROVED);
        r1.setDateCreated(LocalDateTime.of(2026, 3, 2, 15, 0));
        TaskReview r2 = new TaskReview();
        r2.setStatus(ReviewStatus.ERROR);
        r2.setDateCreated(LocalDateTime.of(2026, 3, 1, 12, 0));

        when(taskService.loadInternTaskPage("intern", 7L))
            .thenReturn(new InternTaskPage(task, List.of(r1, r2), InternTaskSubmissionInfo.allowed()));

        mockMvc.perform(get("/intern/tasks/7"))
            .andExpect(status().isOk())
            .andExpect(model().attribute("submissionHistory", List.of(r1, r2)))
            .andExpect(model().attribute("canSubmit", true))
            .andExpect(model().attribute("submitBlockReason", (Object) null))
            .andExpect(model().attributeExists("submission"))
            .andExpect(result -> assertThat(result.getModelAndView().getModel().get("submission"))
                .isInstanceOf(ReviewSubmissionDto.class));
    }
}
