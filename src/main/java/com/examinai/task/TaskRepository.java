package com.examinai.task;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findAllByOrderByTaskNameAsc();

    @Query("SELECT t FROM Task t JOIN FETCH t.course ORDER BY t.taskName ASC")
    List<Task> findAllWithCourseOrderByTaskNameAsc();

    @Query("SELECT t FROM Task t JOIN FETCH t.course JOIN FETCH t.mentor WHERE t.id = :id")
    Optional<Task> findByIdWithCourseAndMentor(@Param("id") Long id);
}
