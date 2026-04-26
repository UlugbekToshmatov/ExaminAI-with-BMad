package com.examinai.task;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findAllByOrderByTaskNameAsc();

    @Query("SELECT DISTINCT t FROM Task t "
        + "JOIN FETCH t.course c JOIN FETCH c.stack "
        + "JOIN FETCH t.mentor ORDER BY t.taskName ASC")
    List<Task> findAllWithCourseAndMentorOrderByTaskNameAsc();

    @Query("SELECT t FROM Task t "
        + "JOIN FETCH t.course c JOIN FETCH c.stack "
        + "JOIN FETCH t.mentor "
        + "WHERE c.stack.id IN :stackIds "
        + "ORDER BY t.taskName ASC")
    List<Task> findAllForInternByCourseStackIdIn(@Param("stackIds") java.util.List<Long> stackIds);

    @Query("SELECT t FROM Task t "
        + "JOIN FETCH t.course c JOIN FETCH c.stack "
        + "ORDER BY t.taskName ASC")
    List<Task> findAllWithCourseAndStackOrderByTaskNameAsc();

    @Query("SELECT t FROM Task t "
        + "JOIN FETCH t.course c JOIN FETCH c.stack "
        + "JOIN FETCH t.mentor WHERE t.id = :id")
    Optional<Task> findByIdWithCourseAndMentor(@Param("id") Long id);
}
