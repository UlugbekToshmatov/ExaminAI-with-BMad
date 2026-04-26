package com.examinai.stack;

import com.examinai.course.CourseRepository;
import com.examinai.user.UserAccountRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class StackService {

    private final StackRepository stackRepository;
    private final CourseRepository courseRepository;
    private final UserAccountRepository userAccountRepository;

    public StackService(
        StackRepository stackRepository,
        CourseRepository courseRepository,
        UserAccountRepository userAccountRepository) {
        this.stackRepository = stackRepository;
        this.courseRepository = courseRepository;
        this.userAccountRepository = userAccountRepository;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('MENTOR', 'ADMIN')")
    public List<Stack> findAll() {
        return stackRepository.findAllByOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    public Stack findByIdForAdmin(long id) {
        return stackRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Stack not found: " + id));
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public Stack create(StackFormDto dto) {
        String name = normalizeName(dto.getName());
        if (name.isEmpty()) {
            throw new IllegalArgumentException("Name is required");
        }
        if (stackRepository.existsByName(name)) {
            throw new IllegalArgumentException("A stack with this name already exists");
        }
        Stack stack = new Stack();
        stack.setName(name);
        return stackRepository.save(stack);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public Stack update(long id, StackFormDto dto) {
        String name = normalizeName(dto.getName());
        if (name.isEmpty()) {
            throw new IllegalArgumentException("Name is required");
        }
        if (stackRepository.existsByNameAndIdNot(name, id)) {
            throw new IllegalArgumentException("A stack with this name already exists");
        }
        Stack stack = findByIdForAdmin(id);
        stack.setName(name);
        return stackRepository.save(stack);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(long id) {
        long courses = courseRepository.countByStack_Id(id);
        long users = userAccountRepository.countUsersWithStack(id);
        if (courses > 0 || users > 0) {
            throw new IllegalStateException(
                "Cannot delete this stack: it is used by " + courses + " course(s) and "
                    + users + " user account(s). Reassign or remove those first.");
        }
        Stack stack = findByIdForAdmin(id);
        stackRepository.delete(stack);
    }

    private static String normalizeName(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim();
    }
}
