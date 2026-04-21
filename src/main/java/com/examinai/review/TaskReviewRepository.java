package com.examinai.review;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface TaskReviewRepository extends JpaRepository<TaskReview, Long> {

    @Query("SELECT tr FROM TaskReview tr WHERE tr.intern.id = :internId ORDER BY tr.dateCreated DESC")
    List<TaskReview> findAllByInternIdOrderByDateCreatedDesc(@Param("internId") Long internId);
}
