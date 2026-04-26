package com.examinai.course;

import com.examinai.stack.StackRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class CourseService {

    private final CourseRepository courseRepository;
    private final StackRepository stackRepository;

    public CourseService(CourseRepository courseRepository, StackRepository stackRepository) {
        this.courseRepository = courseRepository;
        this.stackRepository = stackRepository;
    }

    @PreAuthorize("hasRole('MENTOR') or hasRole('ADMIN')")
    public List<Course> findAll() {
        return courseRepository.findAllByOrderByCourseNameAsc();
    }

    @PreAuthorize("hasRole('MENTOR') or hasRole('ADMIN')")
    public Course findById(Long id) {
        return courseRepository.findByIdWithStack(id)
            .orElseThrow(() -> new IllegalArgumentException("Course not found: " + id));
    }

    @Transactional
    @PreAuthorize("hasRole('MENTOR') or hasRole('ADMIN')")
    public Course create(CourseCreateDto dto) {
        var stack = stackRepository.findById(dto.getStackId())
            .orElseThrow(() -> new IllegalArgumentException("Stack not found: " + dto.getStackId()));
        Course course = new Course();
        course.setCourseName(dto.getCourseName());
        course.setTechnology(dto.getTechnology());
        course.setStack(stack);
        return courseRepository.save(course);
    }

    @Transactional
    @PreAuthorize("hasRole('MENTOR') or hasRole('ADMIN')")
    public Course update(Long id, CourseCreateDto dto) {
        var stack = stackRepository.findById(dto.getStackId())
            .orElseThrow(() -> new IllegalArgumentException("Stack not found: " + dto.getStackId()));
        Course course = findById(id);
        course.setCourseName(dto.getCourseName());
        course.setTechnology(dto.getTechnology());
        course.setStack(stack);
        return courseRepository.save(course);
    }

    @Transactional
    @PreAuthorize("hasRole('MENTOR') or hasRole('ADMIN')")
    public void delete(Long id) {
        Course course = findById(id);
        courseRepository.delete(course);
    }
}
