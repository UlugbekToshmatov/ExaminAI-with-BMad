package com.examinai.task;

import com.examinai.course.Course;
import com.examinai.user.UserAccount;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "task")
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_name", nullable = false)
    private String taskName;

    @Column(name = "task_description", columnDefinition = "TEXT")
    private String taskDescription;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mentor_id", nullable = false)
    private UserAccount mentor;

    @Column(name = "date_created", nullable = false)
    private LocalDateTime dateCreated;

    @PrePersist
    private void prePersist() {
        this.dateCreated = LocalDateTime.now();
    }

    public Task() {}

    public Long getId() { return id; }
    public String getTaskName() { return taskName; }
    public void setTaskName(String taskName) { this.taskName = taskName; }
    public String getTaskDescription() { return taskDescription; }
    public void setTaskDescription(String taskDescription) { this.taskDescription = taskDescription; }
    public Course getCourse() { return course; }
    public void setCourse(Course course) { this.course = course; }
    public UserAccount getMentor() { return mentor; }
    public void setMentor(UserAccount mentor) { this.mentor = mentor; }
    public LocalDateTime getDateCreated() { return dateCreated; }
}
