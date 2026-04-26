package com.examinai.stack;

import com.examinai.course.CourseRepository;
import com.examinai.user.UserAccountRepository;
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
class StackServiceTest {

    @Mock StackRepository stackRepository;
    @Mock CourseRepository courseRepository;
    @Mock UserAccountRepository userAccountRepository;
    @InjectMocks StackService stackService;

    @Test
    void create_savesAndReturnsStack() {
        when(stackRepository.existsByName("Rust")).thenReturn(false);
        when(stackRepository.save(any(Stack.class))).thenAnswer(inv -> inv.getArgument(0));
        var dto = new StackFormDto();
        dto.setName("  Rust  ");

        Stack result = stackService.create(dto);

        assertThat(result.getName()).isEqualTo("Rust");
        verify(stackRepository).save(any(Stack.class));
    }

    @Test
    void create_duplicateName_throws() {
        when(stackRepository.existsByName("Java")).thenReturn(true);
        var dto = new StackFormDto();
        dto.setName("Java");
        assertThatThrownBy(() -> stackService.create(dto))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("already exists");
        verify(stackRepository, never()).save(any());
    }

    @Test
    void update_renamesStack() {
        Stack existing = new Stack();
        ReflectionTestUtils.setField(existing, "id", 1L);
        existing.setName("Old");
        when(stackRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(stackRepository.existsByNameAndIdNot("New", 1L)).thenReturn(false);
        when(stackRepository.save(any(Stack.class))).thenAnswer(inv -> inv.getArgument(0));
        var dto = new StackFormDto();
        dto.setName("New");

        Stack out = stackService.update(1L, dto);

        assertThat(out.getName()).isEqualTo("New");
    }

    @Test
    void delete_removesWhenUnused() {
        when(courseRepository.countByStack_Id(2L)).thenReturn(0L);
        when(userAccountRepository.countUsersWithStack(2L)).thenReturn(0L);
        Stack s = new Stack();
        ReflectionTestUtils.setField(s, "id", 2L);
        when(stackRepository.findById(2L)).thenReturn(Optional.of(s));

        stackService.delete(2L);

        verify(stackRepository).delete(s);
    }

    @Test
    void delete_whenCourseUsesStack_throws() {
        when(courseRepository.countByStack_Id(2L)).thenReturn(1L);
        when(userAccountRepository.countUsersWithStack(2L)).thenReturn(0L);

        assertThatThrownBy(() -> stackService.delete(2L))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("course(s)");

        verify(stackRepository, never()).delete(any());
    }
}
