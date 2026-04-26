package com.examinai.course;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface CourseRepository extends JpaRepository<Course, Long> {

    @Query("SELECT DISTINCT c FROM Course c JOIN FETCH c.stack ORDER BY c.courseName ASC")
    List<Course> findAllByOrderByCourseNameAsc();

    @Query("SELECT c FROM Course c JOIN FETCH c.stack WHERE c.id = :id")
    Optional<Course> findByIdWithStack(@Param("id") Long id);

    long countByStack_Id(Long stackId);
}
