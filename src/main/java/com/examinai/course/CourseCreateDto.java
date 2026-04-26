package com.examinai.course;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class CourseCreateDto {

    @NotBlank(message = "Course name is required")
    @Size(max = 255, message = "Course name must not exceed 255 characters")
    private String courseName;

    @Size(max = 100, message = "Technology must not exceed 100 characters")
    private String technology;

    @NotNull(message = "Stack is required")
    private Long stackId;

    public CourseCreateDto() {}

    public String getCourseName() { return courseName; }
    public void setCourseName(String courseName) { this.courseName = courseName; }
    public String getTechnology() { return technology; }
    public void setTechnology(String technology) { this.technology = technology; }
    public Long getStackId() { return stackId; }
    public void setStackId(Long stackId) { this.stackId = stackId; }
}
