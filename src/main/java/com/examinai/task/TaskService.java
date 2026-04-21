package com.examinai.task;

import com.examinai.course.Course;
import com.examinai.course.CourseRepository;
import com.examinai.user.Role;
import com.examinai.user.UserAccount;
import com.examinai.user.UserAccountRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final CourseRepository courseRepository;
    private final UserAccountRepository userAccountRepository;

    public TaskService(TaskRepository taskRepository,
                       CourseRepository courseRepository,
                       UserAccountRepository userAccountRepository) {
        this.taskRepository = taskRepository;
        this.courseRepository = courseRepository;
        this.userAccountRepository = userAccountRepository;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('MENTOR') or hasRole('ADMIN')")
    public List<Task> findAll() {
        return taskRepository.findAllByOrderByTaskNameAsc();
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('MENTOR') or hasRole('ADMIN')")
    public Task findById(Long id) {
        return taskRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Task not found: " + id));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('MENTOR') or hasRole('ADMIN')")
    public List<Course> findAllCourses() {
        return courseRepository.findAllByOrderByCourseNameAsc();
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('MENTOR') or hasRole('ADMIN')")
    public List<UserAccount> findAllMentors() {
        return userAccountRepository.findAllByRole(Role.MENTOR);
    }

    @Transactional
    @PreAuthorize("hasRole('MENTOR') or hasRole('ADMIN')")
    public Task create(TaskCreateDto dto) {
        Course course = courseRepository.findById(dto.getCourseId())
            .orElseThrow(() -> new IllegalArgumentException("Course not found: " + dto.getCourseId()));
        UserAccount mentor = userAccountRepository.findById(dto.getMentorId())
            .orElseThrow(() -> new IllegalArgumentException("Mentor not found: " + dto.getMentorId()));
        if (mentor.getRole() != Role.MENTOR) {
            throw new IllegalArgumentException("Selected user is not a mentor");
        }
        Task task = new Task();
        task.setTaskName(dto.getTaskName());
        task.setTaskDescription(dto.getTaskDescription());
        task.setCourse(course);
        task.setMentor(mentor);
        return taskRepository.save(task);
    }

    @Transactional
    @PreAuthorize("hasRole('MENTOR') or hasRole('ADMIN')")
    public Task update(Long id, TaskCreateDto dto) {
        Task task = findById(id);
        assertOwnership(task);
        Course course = courseRepository.findById(dto.getCourseId())
            .orElseThrow(() -> new IllegalArgumentException("Course not found: " + dto.getCourseId()));
        UserAccount mentor = userAccountRepository.findById(dto.getMentorId())
            .orElseThrow(() -> new IllegalArgumentException("Mentor not found: " + dto.getMentorId()));
        if (mentor.getRole() != Role.MENTOR) {
            throw new IllegalArgumentException("Selected user is not a mentor");
        }
        task.setTaskName(dto.getTaskName());
        task.setTaskDescription(dto.getTaskDescription());
        task.setCourse(course);
        task.setMentor(mentor);
        return taskRepository.save(task);
    }

    @Transactional
    @PreAuthorize("hasRole('MENTOR') or hasRole('ADMIN')")
    public void delete(Long id) {
        Task task = findById(id);
        assertOwnership(task);
        taskRepository.delete(task);
    }

    private void assertOwnership(Task task) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin && !task.getMentor().getUsername().equals(auth.getName())) {
            throw new AccessDeniedException("You do not own this task");
        }
    }
}
