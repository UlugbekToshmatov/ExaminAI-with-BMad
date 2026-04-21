package com.examinai.user;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class UserAccountService {

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;

    public UserAccountService(UserAccountRepository userAccountRepository,
                               PasswordEncoder passwordEncoder) {
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public UserAccount createUser(UserAccountCreateDto dto) {
        if (userAccountRepository.existsByUsername(dto.getUsername())) {
            throw new IllegalArgumentException("Username already exists");
        }
        UserAccount account = new UserAccount();
        account.setUsername(dto.getUsername());
        account.setPassword(passwordEncoder.encode(dto.getPassword()));
        account.setEmail(dto.getEmail());
        account.setRole(dto.getRole());
        account.setActive(true);
        return userAccountRepository.save(account);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void deactivate(Long id) {
        UserAccount account = userAccountRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        if (account.getUsername().equals(currentUsername)) {
            throw new IllegalArgumentException("Cannot deactivate your own account");
        }
        account.setActive(false);
        userAccountRepository.save(account);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public List<UserAccount> findAll() {
        return userAccountRepository.findAll();
    }
}
