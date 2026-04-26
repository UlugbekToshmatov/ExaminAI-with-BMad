package com.examinai.course;

import com.examinai.stack.Stack;
import com.examinai.stack.StackRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

    @Mock CourseRepository courseRepository;
    @Mock StackRepository stackRepository;
    @InjectMocks CourseService courseService;

    @Test
    void create_savesCourseWithCorrectFields() {
        Stack javaStack = new Stack();
        ReflectionTestUtils.setField(javaStack, "id", 1L);
        javaStack.setName("Java");
        CourseCreateDto dto = new CourseCreateDto();
        dto.setCourseName("Spring Boot");
        dto.setTechnology("Java");
        dto.setStackId(1L);
        when(stackRepository.findById(1L)).thenReturn(Optional.of(javaStack));
        when(courseRepository.save(any(Course.class))).thenAnswer(inv -> inv.getArgument(0));

        Course result = courseService.create(dto);

        assertThat(result.getCourseName()).isEqualTo("Spring Boot");
        assertThat(result.getTechnology()).isEqualTo("Java");
        assertThat(result.getStack()).isSameAs(javaStack);
        verify(courseRepository).save(any(Course.class));
    }

    @Test
    void update_modifiesCourseFieldsAndSaves() {
        Stack reactStack = new Stack();
        ReflectionTestUtils.setField(reactStack, "id", 2L);
        reactStack.setName("React");
        Course existing = new Course();
        existing.setCourseName("Old Name");
        existing.setTechnology("Python");
        when(courseRepository.findByIdWithStack(1L)).thenReturn(Optional.of(existing));
        when(stackRepository.findById(2L)).thenReturn(Optional.of(reactStack));
        when(courseRepository.save(any(Course.class))).thenAnswer(inv -> inv.getArgument(0));

        CourseCreateDto dto = new CourseCreateDto();
        dto.setCourseName("New Name");
        dto.setTechnology("Java");
        dto.setStackId(2L);
        courseService.update(1L, dto);

        assertThat(existing.getCourseName()).isEqualTo("New Name");
        assertThat(existing.getTechnology()).isEqualTo("Java");
        assertThat(existing.getStack()).isSameAs(reactStack);
        verify(courseRepository).save(existing);
    }

    @Test
    void delete_callsRepositoryDelete() {
        Course existing = new Course();
        when(courseRepository.findByIdWithStack(1L)).thenReturn(Optional.of(existing));

        courseService.delete(1L);

        verify(courseRepository).delete(existing);
    }

    @Test
    void findById_withMissingId_throwsIllegalArgumentException() {
        when(courseRepository.findByIdWithStack(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> courseService.findById(99L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Course not found");
    }
}
