package com.examinai.review;

import jakarta.persistence.*;

@Entity
@Table(name = "task_review_issue")
public class TaskReviewIssue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_review_id", nullable = false)
    private TaskReview taskReview;

    @Column(name = "line")
    private Integer line;

    @Column(name = "code", columnDefinition = "TEXT")
    private String code;

    @Column(name = "issue", nullable = false, columnDefinition = "TEXT")
    private String issue;

    @Column(name = "improvement", columnDefinition = "TEXT")
    private String improvement;

    public TaskReviewIssue() {}

    public Long getId() { return id; }
    public TaskReview getTaskReview() { return taskReview; }
    public void setTaskReview(TaskReview taskReview) { this.taskReview = taskReview; }
    public Integer getLine() { return line; }
    public void setLine(Integer line) { this.line = line; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getIssue() { return issue; }
    public void setIssue(String issue) { this.issue = issue; }
    public String getImprovement() { return improvement; }
    public void setImprovement(String improvement) { this.improvement = improvement; }
}
