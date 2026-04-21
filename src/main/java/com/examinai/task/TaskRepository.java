package com.examinai.task;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findAllByOrderByTaskNameAsc();

    @Query("SELECT t FROM Task t JOIN FETCH t.course ORDER BY t.taskName ASC")
    List<Task> findAllWithCourseOrderByTaskNameAsc();
}
