package com.qa.automation.model;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDateTime;

@Entity
@Table(name = "jenkins_test_cases")
public class JenkinsTestCase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String testName;

    @Column(name = "class_name")
    private String className;

    @Column(nullable = false)
    private String status; // PASSED, FAILED, SKIPPED

    @Column(name = "duration")
    private Double duration; // Test execution duration in seconds

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "stack_trace", columnDefinition = "TEXT")
    private String stackTrace;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "jenkins_result_id", nullable = false)
    @JsonIgnoreProperties("testCases")
    private JenkinsResult jenkinsResult;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // Constructors
    public JenkinsTestCase() {}

    public JenkinsTestCase(String testName, String className, String status) {
        this.testName = testName;
        this.className = className;
        this.status = status;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTestName() {
        return testName;
    }

    public void setTestName(String testName) {
        this.testName = testName;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Double getDuration() {
        return duration;
    }

    public void setDuration(Double duration) {
        this.duration = duration;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getStackTrace() {
        return stackTrace;
    }

    public void setStackTrace(String stackTrace) {
        this.stackTrace = stackTrace;
    }

    public JenkinsResult getJenkinsResult() {
        return jenkinsResult;
    }

    public void setJenkinsResult(JenkinsResult jenkinsResult) {
        this.jenkinsResult = jenkinsResult;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}