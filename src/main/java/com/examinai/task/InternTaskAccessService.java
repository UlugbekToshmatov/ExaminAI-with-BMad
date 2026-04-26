package com.examinai.task;

import com.examinai.user.Role;
import com.examinai.user.UserAccount;
import com.examinai.user.UserAccountRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class InternTaskAccessService {

    private final UserAccountRepository userAccountRepository;

    public InternTaskAccessService(UserAccountRepository userAccountRepository) {
        this.userAccountRepository = userAccountRepository;
    }

    @Transactional(readOnly = true)
    public void assertInternReadAccess(Authentication auth, Task task) {
        if (isAdmin(auth)) {
            return;
        }
        UserAccount intern = userAccountRepository.findByUsernameWithStacks(auth.getName())
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + auth.getName()));
        if (intern.getRole() != Role.INTERN) {
            return;
        }
        long courseStackId = task.getCourse().getStack().getId();
        Set<Long> allowed = intern.getStacks().stream()
            .map(s -> s.getId())
            .collect(Collectors.toSet());
        if (!allowed.contains(courseStackId)) {
            throw new AccessDeniedException("This task is not in your assigned stacks");
        }
    }

    @Transactional(readOnly = true)
    public void assertInternReadAccessForCurrentUser(Task task) {
        assertInternReadAccess(SecurityContextHolder.getContext().getAuthentication(), task);
    }

    public static boolean isAdmin(Authentication auth) {
        if (auth == null) {
            return false;
        }
        for (GrantedAuthority a : auth.getAuthorities()) {
            if ("ROLE_ADMIN".equals(a.getAuthority())) {
                return true;
            }
        }
        return false;
    }

    public static boolean currentUserIsAdmin() {
        return isAdmin(SecurityContextHolder.getContext().getAuthentication());
    }
}
