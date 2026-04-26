package com.examinai.course;

import com.examinai.stack.Stack;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "course")
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "course_name", nullable = false)
    private String courseName;

    @Column(name = "technology")
    private String technology;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stack_id", nullable = false)
    private Stack stack;

    @Column(name = "date_created", nullable = false)
    private LocalDateTime dateCreated;

    @PrePersist
    private void prePersist() {
        this.dateCreated = LocalDateTime.now();
    }

    public Course() {}

    public Long getId() { return id; }
    public String getCourseName() { return courseName; }
    public void setCourseName(String courseName) { this.courseName = courseName; }
    public String getTechnology() { return technology; }
    public void setTechnology(String technology) { this.technology = technology; }
    public Stack getStack() { return stack; }
    public void setStack(Stack stack) { this.stack = stack; }
    public LocalDateTime getDateCreated() { return dateCreated; }
}
