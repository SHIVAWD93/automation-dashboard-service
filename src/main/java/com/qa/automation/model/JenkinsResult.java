package com.qa.automation.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "jenkins_results")
public class JenkinsResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String jobName;

    @Column(nullable = false)
    private String buildNumber;

    @Column(nullable = false)
    private String buildStatus; // SUCCESS, FAILURE, UNSTABLE, ABORTED

    @Column(name = "total_tests")
    private Integer totalTests;

    @Column(name = "passed_tests")
    private Integer passedTests;

    @Column(name = "failed_tests")
    private Integer failedTests;

    @Column(name = "skipped_tests")
    private Integer skippedTests;

    @Column(name = "build_url")
    private String buildUrl;

    @Column(name = "build_timestamp")
    private LocalDateTime buildTimestamp;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @OneToMany(mappedBy = "jenkinsResult", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<JenkinsTestCase> testCases;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Constructors
    public JenkinsResult() {}

    public JenkinsResult(String jobName, String buildNumber, String buildStatus) {
        this.jobName = jobName;
        this.buildNumber = buildNumber;
        this.buildStatus = buildStatus;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public String getBuildNumber() {
        return buildNumber;
    }

    public void setBuildNumber(String buildNumber) {
        this.buildNumber = buildNumber;
    }

    public String getBuildStatus() {
        return buildStatus;
    }

    public void setBuildStatus(String buildStatus) {
        this.buildStatus = buildStatus;
    }

    public Integer getTotalTests() {
        return totalTests;
    }

    public void setTotalTests(Integer totalTests) {
        this.totalTests = totalTests;
    }

    public Integer getPassedTests() {
        return passedTests;
    }

    public void setPassedTests(Integer passedTests) {
        this.passedTests = passedTests;
    }

    public Integer getFailedTests() {
        return failedTests;
    }

    public void setFailedTests(Integer failedTests) {
        this.failedTests = failedTests;
    }

    public Integer getSkippedTests() {
        return skippedTests;
    }

    public void setSkippedTests(Integer skippedTests) {
        this.skippedTests = skippedTests;
    }

    public String getBuildUrl() {
        return buildUrl;
    }

    public void setBuildUrl(String buildUrl) {
        this.buildUrl = buildUrl;
    }

    public LocalDateTime getBuildTimestamp() {
        return buildTimestamp;
    }

    public void setBuildTimestamp(LocalDateTime buildTimestamp) {
        this.buildTimestamp = buildTimestamp;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<JenkinsTestCase> getTestCases() {
        return testCases;
    }

    public void setTestCases(List<JenkinsTestCase> testCases) {
        this.testCases = testCases;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}