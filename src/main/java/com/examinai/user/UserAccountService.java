package com.examinai.user;

import com.examinai.stack.Stack;
import com.examinai.stack.StackRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class UserAccountService {

    private final UserAccountRepository userAccountRepository;
    private final StackRepository stackRepository;
    private final PasswordEncoder passwordEncoder;

    public UserAccountService(UserAccountRepository userAccountRepository,
                               StackRepository stackRepository,
                               PasswordEncoder passwordEncoder) {
        this.userAccountRepository = userAccountRepository;
        this.stackRepository = stackRepository;
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
        if (dto.getRole() == Role.INTERN) {
            if (dto.getStackIds() == null || dto.getStackIds().isEmpty()) {
                throw new IllegalArgumentException("Interns must be assigned at least one stack");
            }
            Set<Stack> selected = new HashSet<>(stackRepository.findAllById(dto.getStackIds()));
            if (selected.size() != dto.getStackIds().size()) {
                throw new IllegalArgumentException("One or more stacks are invalid");
            }
            account.setStacks(selected);
        } else {
            account.setStacks(new HashSet<>());
        }
        return userAccountRepository.save(account);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void updateInternStacks(long userId, List<Long> stackIds) {
        UserAccount account = userAccountRepository.findByIdWithStacks(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        if (account.getRole() != Role.INTERN) {
            throw new IllegalArgumentException("Only intern accounts can have stacks assigned");
        }
        if (stackIds == null || stackIds.isEmpty()) {
            throw new IllegalArgumentException("Interns must be assigned at least one stack");
        }
        Set<Stack> selected = new HashSet<>(stackRepository.findAllById(stackIds));
        if (selected.size() != stackIds.size()) {
            throw new IllegalArgumentException("One or more stacks are invalid");
        }
        account.setStacks(selected);
        userAccountRepository.save(account);
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
        return userAccountRepository.findAllWithStacks();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public UserAccount findByIdForStackEditor(long id) {
        return userAccountRepository.findByIdWithStacks(id)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
    }
}
