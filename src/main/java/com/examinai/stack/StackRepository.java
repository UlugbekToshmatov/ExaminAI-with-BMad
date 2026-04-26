package com.examinai.stack;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface StackRepository extends JpaRepository<Stack, Long> {
    List<Stack> findAllByOrderByNameAsc();
}
