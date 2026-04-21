package com.examinai.task;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class TaskCreateDto {

    @NotBlank(message = "Task name is required")
    @Size(max = 255)
    private String taskName;

    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    private String taskDescription;

    @NotNull(message = "Course is required")
    private Long courseId;

    @NotNull(message = "Mentor is required")
    private Long mentorId;

    public TaskCreateDto() {}

    public String getTaskName() { return taskName; }
    public void setTaskName(String taskName) { this.taskName = taskName; }
    public String getTaskDescription() { return taskDescription; }
    public void setTaskDescription(String taskDescription) { this.taskDescription = taskDescription; }
    public Long getCourseId() { return courseId; }
    public void setCourseId(Long courseId) { this.courseId = courseId; }
    public Long getMentorId() { return mentorId; }
    public void setMentorId(Long mentorId) { this.mentorId = mentorId; }
}
