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

    @Query("SELECT tr FROM TaskReview tr JOIN FETCH tr.intern i WHERE tr.id = :id")
    Optional<TaskReview> findByIdWithInternForStatusJson(@Param("id") Long id);

    @Query("""
        SELECT DISTINCT tr FROM TaskReview tr
        JOIN FETCH tr.task t
        LEFT JOIN FETCH t.course
        JOIN FETCH tr.intern
        LEFT JOIN FETCH tr.issues
        WHERE tr.id = :id
        """)
    Optional<TaskReview> findByIdForInternStatusPage(@Param("id") Long id);

    List<TaskReview> findAllByTask_IdAndIntern_IdOrderByDateCreatedDesc(Long taskId, Long internId);

    /**
     * Mentor queue: scoped by {@code task.mentor} when {@code mentorFilter != -1}; admin uses {@code -1L} for all mentors.
     * Use {@code 0L} for {@code internFilter} or {@code taskFilter} to mean "any".
     */
    @Query("""
        SELECT tr FROM TaskReview tr
        JOIN FETCH tr.task t
        JOIN FETCH tr.intern
        WHERE (-1L = :mentorFilter OR t.mentor.id = :mentorFilter)
        AND tr.status = :status
        AND (0L = :internFilter OR tr.intern.id = :internFilter)
        AND (0L = :taskFilter OR t.id = :taskFilter)
        ORDER BY tr.dateCreated DESC
        """)
    List<TaskReview> findMentorQueue(
        @Param("mentorFilter") long mentorFilter,
        @Param("status") ReviewStatus status,
        @Param("internFilter") long internFilter,
        @Param("taskFilter") long taskFilter);

    /**
     * Intern labels for the queue filter: distinct interns among reviews matching mentor scope, status,
     * and optional task filter (use {@code 0L} for any task).
     */
    @Query("""
        SELECT DISTINCT new com.examinai.review.MentorQueueLabelValue(i.id, i.username)
        FROM TaskReview tr
        JOIN tr.intern i
        JOIN tr.task t
        WHERE (-1L = :mentorFilter OR t.mentor.id = :mentorFilter)
        AND tr.status = :status
        AND (0L = :taskFilter OR t.id = :taskFilter)
        ORDER BY i.username
        """)
    List<MentorQueueLabelValue> findMentorQueueInternOptions(
        @Param("mentorFilter") long mentorFilter,
        @Param("status") ReviewStatus status,
        @Param("taskFilter") long taskFilter);

    /**
     * Task labels for the queue filter: distinct tasks among reviews matching mentor scope, status,
     * and optional intern filter (use {@code 0L} for any intern).
     */
    @Query("""
        SELECT DISTINCT new com.examinai.review.MentorQueueLabelValue(t.id, t.taskName)
        FROM TaskReview tr
        JOIN tr.task t
        JOIN tr.intern i
        WHERE (-1L = :mentorFilter OR t.mentor.id = :mentorFilter)
        AND tr.status = :status
        AND (0L = :internFilter OR i.id = :internFilter)
        ORDER BY t.taskName
        """)
    List<MentorQueueLabelValue> findMentorQueueTaskOptions(
        @Param("mentorFilter") long mentorFilter,
        @Param("status") ReviewStatus status,
        @Param("internFilter") long internFilter);

    @Query("""
        SELECT tr FROM TaskReview tr
        JOIN FETCH tr.task t
        JOIN FETCH t.mentor
        WHERE tr.id = :id
        """)
    Optional<TaskReview> findByIdWithTaskAndTaskMentor(@Param("id") Long id);

    /**
     * Mentor review detail: one round-trip with task, course, task mentor, intern, and issues (no N+1 on issues).
     */
    @Query("""
        SELECT DISTINCT tr FROM TaskReview tr
        JOIN FETCH tr.task t
        JOIN FETCH t.course
        JOIN FETCH t.mentor
        JOIN FETCH tr.intern
        LEFT JOIN FETCH tr.issues
        WHERE tr.id = :id
        """)
    Optional<TaskReview> findByIdForMentorDetail(@Param("id") Long id);
}
