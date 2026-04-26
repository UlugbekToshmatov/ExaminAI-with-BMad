package com.examinai.task;

import com.examinai.course.Course;
import com.examinai.course.CourseRepository;
import com.examinai.review.TaskReview;
import com.examinai.review.TaskReviewRepository;
import com.examinai.stack.Stack;
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
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final CourseRepository courseRepository;
    private final UserAccountRepository userAccountRepository;
    private final TaskReviewRepository taskReviewRepository;
    private final InternTaskAccessService internTaskAccessService;

    public TaskService(TaskRepository taskRepository,
                       CourseRepository courseRepository,
                       UserAccountRepository userAccountRepository,
                       TaskReviewRepository taskReviewRepository,
                       InternTaskAccessService internTaskAccessService) {
        this.taskRepository = taskRepository;
        this.courseRepository = courseRepository;
        this.userAccountRepository = userAccountRepository;
        this.taskReviewRepository = taskReviewRepository;
        this.internTaskAccessService = internTaskAccessService;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('MENTOR') or hasRole('ADMIN')")
    public List<Task> findAll() {
        return taskRepository.findAllWithCourseAndMentorOrderByTaskNameAsc();
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

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('INTERN') or hasRole('ADMIN')")
    public Task findForInternTaskDetail(String username, Long taskId) {
        userAccountRepository.findByUsername(username)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
        Task task = taskRepository.findByIdWithCourseAndMentor(taskId)
            .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));
        internTaskAccessService.assertInternReadAccessForCurrentUser(task);
        return task;
    }

    /**
     * All submission attempts for this intern on the task, newest first (for history list / attempt numbering).
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('INTERN') or hasRole('ADMIN')")
    public List<TaskReview> findSubmissionHistoryForInternTask(String username, Long taskId) {
        UserAccount intern = userAccountRepository.findByUsername(username)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
        Task task = taskRepository.findByIdWithCourseAndMentor(taskId)
            .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));
        internTaskAccessService.assertInternReadAccessForCurrentUser(task);
        return taskReviewRepository.findAllByTask_IdAndIntern_IdOrderByDateCreatedDesc(taskId, intern.getId());
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('INTERN') or hasRole('ADMIN')")
    public List<TaskWithReview> findForInternByUsername(String username) {
        UserAccount intern = userAccountRepository.findByUsernameWithStacks(username)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
        List<Task> tasks;
        if (InternTaskAccessService.currentUserIsAdmin()) {
            tasks = taskRepository.findAllWithCourseAndStackOrderByTaskNameAsc();
        } else {
            List<Long> stackIds = intern.getStacks().stream()
                .map(Stack::getId)
                .collect(Collectors.toList());
            tasks = stackIds.isEmpty()
                ? List.of()
                : taskRepository.findAllForInternByCourseStackIdIn(stackIds);
        }
        List<TaskReview> reviews = taskReviewRepository
            .findAllByInternIdOrderByDateCreatedDesc(intern.getId());
        // query returns newest-first (DESC); keep existing (first seen = latest) per task
        Map<Long, TaskReview> latestByTask = reviews.stream()
            .collect(java.util.stream.Collectors.toMap(
                r -> r.getTask().getId(),
                r -> r,
                (existing, replacement) -> existing
            ));
        return tasks.stream()
            .map(t -> new TaskWithReview(t, latestByTask.get(t.getId())))
            .toList();
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
