package com.examinai.stack;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class StackService {

    private final StackRepository stackRepository;

    public StackService(StackRepository stackRepository) {
        this.stackRepository = stackRepository;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('MENTOR', 'ADMIN')")
    public List<Stack> findAll() {
        return stackRepository.findAllByOrderByNameAsc();
    }
}
