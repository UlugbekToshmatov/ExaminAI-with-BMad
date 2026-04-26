package com.examinai.user;

import com.examinai.stack.Stack;
import com.examinai.stack.StackRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserAccountServiceTest {

    @Mock UserAccountRepository userAccountRepository;
    @Mock StackRepository stackRepository;
    @Mock PasswordEncoder passwordEncoder;

    @InjectMocks UserAccountService userAccountService;

    @BeforeEach
    void adminSecurityContext() {
        SecurityContextHolder.setContext(new SecurityContextImpl(
            new UsernamePasswordAuthenticationToken("admin", "pw",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")))));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createUser_savesWithEncodedPasswordAndActiveTrue() {
        UserAccountCreateDto dto = new UserAccountCreateDto();
        dto.setUsername("newuser");
        dto.setEmail("new@test.com");
        dto.setRole(Role.INTERN);
        dto.setPassword("plain123");
        Stack javaStack = new Stack();
        ReflectionTestUtils.setField(javaStack, "id", 1L);
        javaStack.setName("Java");
        dto.getStackIds().add(1L);

        when(userAccountRepository.existsByUsername("newuser")).thenReturn(false);
        when(stackRepository.findAllById(any())).thenReturn(List.of(javaStack));
        when(passwordEncoder.encode("plain123")).thenReturn("$2a$12$encoded");
        when(userAccountRepository.save(any(UserAccount.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        UserAccount result = userAccountService.createUser(dto);

        assertThat(result.getUsername()).isEqualTo("newuser");
        assertThat(result.getPassword()).isEqualTo("$2a$12$encoded");
        assertThat(result.isActive()).isTrue();
        assertThat(result.getRole()).isEqualTo(Role.INTERN);
        verify(userAccountRepository).save(any(UserAccount.class));
    }

    @Test
    void createUser_duplicateUsername_throwsIllegalArgumentException() {
        UserAccountCreateDto dto = new UserAccountCreateDto();
        dto.setUsername("existing");
        dto.setPassword("pass");
        dto.setRole(Role.INTERN);

        when(userAccountRepository.existsByUsername("existing")).thenReturn(true);

        assertThatThrownBy(() -> userAccountService.createUser(dto))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Username already exists");

        verify(userAccountRepository, never()).save(any());
    }

    @Test
    void deactivate_setsActiveFalseAndSaves() {
        UserAccount account = new UserAccount();
        account.setUsername("intern1");
        account.setActive(true);

        when(userAccountRepository.findById(42L)).thenReturn(Optional.of(account));

        userAccountService.deactivate(42L);

        assertThat(account.isActive()).isFalse();
        verify(userAccountRepository).save(account);
    }
}
