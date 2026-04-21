package com.examinai.task;

import com.examinai.course.Course;
import com.examinai.course.CourseRepository;
import com.examinai.user.UserAccount;
import com.examinai.user.UserAccountRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock TaskRepository taskRepository;
    @Mock CourseRepository courseRepository;
    @Mock UserAccountRepository userAccountRepository;
    @InjectMocks TaskService taskService;

    @Test
    void create_savesTaskWithCorrectFields() {
        Course course = new Course();
        course.setCourseName("Spring Boot");
        UserAccount mentor = new UserAccount();
        mentor.setUsername("mentor");

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
        Course newCourse = new Course();
        newCourse.setCourseName("New Course");
        UserAccount newMentor = new UserAccount();
        newMentor.setUsername("new_mentor");

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
}
