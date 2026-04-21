package com.examinai.review;

import com.examinai.task.Task;
import com.examinai.user.UserAccount;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "task_review")
public class TaskReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "intern_id", nullable = false)
    private UserAccount intern;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mentor_id")
    private UserAccount mentor;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ReviewStatus status;

    @Column(name = "llm_result")
    private String llmResult;

    @Column(name = "mentor_result")
    private String mentorResult;

    @Column(name = "mentor_remarks", columnDefinition = "TEXT")
    private String mentorRemarks;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Column(name = "date_created", nullable = false)
    private LocalDateTime dateCreated;

    public TaskReview() {}

    public Long getId() { return id; }
    public Task getTask() { return task; }
    public void setTask(Task task) { this.task = task; }
    public UserAccount getIntern() { return intern; }
    public void setIntern(UserAccount intern) { this.intern = intern; }
    public UserAccount getMentor() { return mentor; }
    public void setMentor(UserAccount mentor) { this.mentor = mentor; }
    public ReviewStatus getStatus() { return status; }
    public void setStatus(ReviewStatus status) { this.status = status; }
    public String getLlmResult() { return llmResult; }
    public void setLlmResult(String llmResult) { this.llmResult = llmResult; }
    public String getMentorResult() { return mentorResult; }
    public void setMentorResult(String mentorResult) { this.mentorResult = mentorResult; }
    public String getMentorRemarks() { return mentorRemarks; }
    public void setMentorRemarks(String mentorRemarks) { this.mentorRemarks = mentorRemarks; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public LocalDateTime getDateCreated() { return dateCreated; }
    public void setDateCreated(LocalDateTime dateCreated) { this.dateCreated = dateCreated; }
}
