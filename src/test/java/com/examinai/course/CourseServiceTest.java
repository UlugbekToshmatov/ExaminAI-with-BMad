package com.examinai.course;

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
class CourseServiceTest {

    @Mock CourseRepository courseRepository;
    @InjectMocks CourseService courseService;

    @Test
    void create_savesCourseWithCorrectFields() {
        CourseCreateDto dto = new CourseCreateDto();
        dto.setCourseName("Spring Boot");
        dto.setTechnology("Java");
        when(courseRepository.save(any(Course.class))).thenAnswer(inv -> inv.getArgument(0));

        Course result = courseService.create(dto);

        assertThat(result.getCourseName()).isEqualTo("Spring Boot");
        assertThat(result.getTechnology()).isEqualTo("Java");
        verify(courseRepository).save(any(Course.class));
    }

    @Test
    void update_modifiesCourseFieldsAndSaves() {
        Course existing = new Course();
        existing.setCourseName("Old Name");
        existing.setTechnology("Python");
        when(courseRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(courseRepository.save(any(Course.class))).thenAnswer(inv -> inv.getArgument(0));

        CourseCreateDto dto = new CourseCreateDto();
        dto.setCourseName("New Name");
        dto.setTechnology("Java");
        courseService.update(1L, dto);

        assertThat(existing.getCourseName()).isEqualTo("New Name");
        assertThat(existing.getTechnology()).isEqualTo("Java");
        verify(courseRepository).save(existing);
    }

    @Test
    void delete_callsRepositoryDelete() {
        Course existing = new Course();
        when(courseRepository.findById(1L)).thenReturn(Optional.of(existing));

        courseService.delete(1L);

        verify(courseRepository).delete(existing);
    }

    @Test
    void findById_withMissingId_throwsIllegalArgumentException() {
        when(courseRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> courseService.findById(99L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Course not found");
    }
}
