package com.examinai.task;

import com.examinai.course.Course;
import com.examinai.course.CourseRepository;
import com.examinai.review.TaskReviewRepository;
import com.examinai.stack.Stack;
import com.examinai.task.TaskWithReview;
import com.examinai.user.Role;
import com.examinai.user.UserAccount;
import com.examinai.user.UserAccountRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.test.util.ReflectionTestUtils;
import com.examinai.review.ReviewStatus;
import com.examinai.review.TaskReview;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock TaskRepository taskRepository;
    @Mock CourseRepository courseRepository;
    @Mock UserAccountRepository userAccountRepository;
    @Mock TaskReviewRepository taskReviewRepository;
    @Mock InternTaskAccessService internTaskAccessService;
    @InjectMocks TaskService taskService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private void setAdminContext() {
        SecurityContextHolder.setContext(new SecurityContextImpl(
            new UsernamePasswordAuthenticationToken("admin", "password",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")))));
    }

    private void setInternContext() {
        SecurityContextHolder.setContext(new SecurityContextImpl(
            new UsernamePasswordAuthenticationToken("intern", "password",
                List.of(new SimpleGrantedAuthority("ROLE_INTERN")))));
    }

    @Test
    void create_savesTaskWithCorrectFields() {
        Course course = new Course();
        course.setCourseName("Spring Boot");
        UserAccount mentor = new UserAccount();
        mentor.setUsername("mentor");
        mentor.setRole(Role.MENTOR);

        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        when(userAccountRepository.findById(2L)).thenReturn(Optional.of(mentor));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        TaskCreateDto dto = new TaskCreateDto();
        dto.setTaskName("Build REST API");
        dto.setTaskDescription("Implement CRUD endpoints");
        dto.setCourseId(1L);
        dto.setMentorId(2L);

        Task result = taskService.create(dto);

        assertThat(result.getTaskName()).isEqualTo("Build REST API");
        assertThat(result.getTaskDescription()).isEqualTo("Implement CRUD endpoints");
        assertThat(result.getCourse()).isSameAs(course);
        assertThat(result.getMentor()).isSameAs(mentor);
        verify(taskRepository).save(any(Task.class));
    }

    @Test
    void update_modifiesTaskFieldsAndSaves() {
        setAdminContext();
        Course newCourse = new Course();
        newCourse.setCourseName("New Course");
        UserAccount newMentor = new UserAccount();
        newMentor.setUsername("new_mentor");
        newMentor.setRole(Role.MENTOR);

        Task existing = new Task();
        existing.setTaskName("Old Name");
        when(taskRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(courseRepository.findById(3L)).thenReturn(Optional.of(newCourse));
        when(userAccountRepository.findById(4L)).thenReturn(Optional.of(newMentor));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        TaskCreateDto dto = new TaskCreateDto();
        dto.setTaskName("New Name");
        dto.setTaskDescription("Updated description");
        dto.setCourseId(3L);
        dto.setMentorId(4L);
        taskService.update(1L, dto);

        assertThat(existing.getTaskName()).isEqualTo("New Name");
        assertThat(existing.getCourse()).isSameAs(newCourse);
        assertThat(existing.getMentor()).isSameAs(newMentor);
        verify(taskRepository).save(existing);
    }

    @Test
    void delete_callsRepositoryDelete() {
        setAdminContext();
        Task existing = new Task();
        when(taskRepository.findById(1L)).thenReturn(Optional.of(existing));

        taskService.delete(1L);

        verify(taskRepository).delete(existing);
    }

    @Test
    void findById_withMissingId_throwsIllegalArgumentException() {
        when(taskRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.findById(99L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Task not found");
    }

    @Test
    void findForInternTaskDetail_returnsTaskWhenUserAndTaskExist() {
        UserAccount intern = new UserAccount();
        intern.setUsername("intern");
        Task task = new Task();
        task.setTaskName("One");

        when(userAccountRepository.findByUsername("intern")).thenReturn(Optional.of(intern));
        when(taskRepository.findByIdWithCourseAndMentor(3L)).thenReturn(Optional.of(task));
        doNothing().when(internTaskAccessService).assertInternReadAccessForCurrentUser(task);

        Task result = taskService.findForInternTaskDetail("intern", 3L);

        assertThat(result).isSameAs(task);
        verify(internTaskAccessService).assertInternReadAccessForCurrentUser(task);
    }

    @Test
    void findSubmissionHistoryForInternTask_newestFirst() {
        setInternContext();
        UserAccount intern = new UserAccount();
        intern.setUsername("intern");
        ReflectionTestUtils.setField(intern, "id", 1L);
        Task task = new Task();
        task.setTaskName("T");
        TaskReview older = new TaskReview();
        older.setStatus(ReviewStatus.ERROR);
        older.setDateCreated(LocalDateTime.of(2026, 1, 1, 10, 0));
        TaskReview newer = new TaskReview();
        newer.setStatus(ReviewStatus.PENDING);
        newer.setDateCreated(LocalDateTime.of(2026, 2, 1, 10, 0));

        when(userAccountRepository.findByUsername("intern")).thenReturn(Optional.of(intern));
        when(taskRepository.findByIdWithCourseAndMentor(3L)).thenReturn(Optional.of(task));
        doNothing().when(internTaskAccessService).assertInternReadAccessForCurrentUser(task);
        when(taskReviewRepository.findAllByTask_IdAndIntern_IdOrderByDateCreatedDesc(eq(3L), eq(1L)))
            .thenReturn(List.of(newer, older));

        List<TaskReview> history = taskService.findSubmissionHistoryForInternTask("intern", 3L);

        assertThat(history).containsExactly(newer, older);
    }

    @Test
    void findForInternByUsername_withNoReviews_returnsTasksWithNullReview() {
        setInternContext();
        UserAccount intern = new UserAccount();
        intern.setUsername("intern");
        Stack stack = new Stack();
        ReflectionTestUtils.setField(stack, "id", 10L);
        stack.setName("Java");
        intern.setStacks(Set.of(stack));

        Task task1 = new Task();
        task1.setTaskName("Build REST API");
        Course course = new Course();
        course.setCourseName("Spring Boot");
        course.setStack(stack);
        task1.setCourse(course);

        when(userAccountRepository.findByUsernameWithStacks("intern")).thenReturn(Optional.of(intern));
        when(taskRepository.findAllForInternByCourseStackIdIn(List.of(10L))).thenReturn(List.of(task1));
        when(taskReviewRepository.findAllByInternIdOrderByDateCreatedDesc(any())).thenReturn(Collections.emptyList());

        List<TaskWithReview> result = taskService.findForInternByUsername("intern");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).task()).isSameAs(task1);
        assertThat(result.get(0).review()).isNull();
    }
}
