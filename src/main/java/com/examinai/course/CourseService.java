package com.examinai.course;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class CourseService {

    private final CourseRepository courseRepository;

    public CourseService(CourseRepository courseRepository) {
        this.courseRepository = courseRepository;
    }

    @PreAuthorize("hasRole('MENTOR') or hasRole('ADMIN')")
    public List<Course> findAll() {
        return courseRepository.findAllByOrderByCourseNameAsc();
    }

    @PreAuthorize("hasRole('MENTOR') or hasRole('ADMIN')")
    public Course findById(Long id) {
        return courseRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Course not found: " + id));
    }

    @Transactional
    @PreAuthorize("hasRole('MENTOR') or hasRole('ADMIN')")
    public Course create(CourseCreateDto dto) {
        Course course = new Course();
        course.setCourseName(dto.getCourseName());
        course.setTechnology(dto.getTechnology());
        return courseRepository.save(course);
    }

    @Transactional
    @PreAuthorize("hasRole('MENTOR') or hasRole('ADMIN')")
    public Course update(Long id, CourseCreateDto dto) {
        Course course = findById(id);
        course.setCourseName(dto.getCourseName());
        course.setTechnology(dto.getTechnology());
        return courseRepository.save(course);
    }

    @Transactional
    @PreAuthorize("hasRole('MENTOR') or hasRole('ADMIN')")
    public void delete(Long id) {
        Course course = findById(id);
        courseRepository.delete(course);
    }
}
