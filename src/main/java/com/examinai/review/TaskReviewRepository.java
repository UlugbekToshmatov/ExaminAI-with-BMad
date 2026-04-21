package com.examinai.review;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface TaskReviewRepository extends JpaRepository<TaskReview, Long> {

    boolean existsByTask_IdAndIntern_IdAndStatus(Long taskId, Long internId, ReviewStatus status);

    @Query("SELECT tr FROM TaskReview tr JOIN FETCH tr.task WHERE tr.intern.id = :internId ORDER BY tr.dateCreated DESC")
    List<TaskReview> findAllByInternIdOrderByDateCreatedDesc(@Param("internId") Long internId);

    @Query("SELECT tr FROM TaskReview tr JOIN FETCH tr.task t WHERE tr.id = :id")
    Optional<TaskReview> findByIdWithTask(@Param("id") Long id);

    @Query("""
        SELECT DISTINCT tr FROM TaskReview tr
        JOIN FETCH tr.intern
        LEFT JOIN FETCH tr.mentor
        WHERE tr.id = :id
        """)
    Optional<TaskReview> findByIdWithInternAndMentor(@Param("id") Long id);

    @Query("""
        SELECT DISTINCT tr FROM TaskReview tr
        JOIN FETCH tr.task t JOIN FETCH t.course
        JOIN FETCH tr.intern
        LEFT JOIN FETCH tr.mentor
        LEFT JOIN FETCH tr.issues
        WHERE tr.id = :id
        """)
    Optional<TaskReview> findByIdForLlmPersistence(@Param("id") Long id);
}
