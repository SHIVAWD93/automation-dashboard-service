package com.qa.automation.model;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "jenkins_results")
public class JenkinsResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_name", nullable = false)
    private String jobName;

    @Column(name = "build_number", nullable = false)
    private String buildNumber;

    @Column(name = "build_status")
    private String buildStatus; // SUCCESS, FAILURE, UNSTABLE, ABORTED

    @Column(name = "build_url")
    private String buildUrl;

    @Column(name = "build_timestamp")
    private LocalDateTime buildTimestamp;

    @Column(name = "total_tests")
    private Integer totalTests;

    @Column(name = "passed_tests")
    private Integer passedTests;

    @Column(name = "failed_tests")
    private Integer failedTests;

    @Column(name = "skipped_tests")
    private Integer skippedTests;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ENHANCEMENT: Added pass percentage field
    @Column(name = "pass_percentage")
    private Integer passPercentage;

    // ENHANCEMENT: Added notes fields for bugs and failure reasons
    @Column(name = "bugs_identified", columnDefinition = "TEXT")
    private String bugsIdentified;

    @Column(name = "failure_reasons", columnDefinition = "TEXT")
    private String failureReasons;

    // NEW: Added job frequency field
    @Column(name = "job_frequency")
    private String jobFrequency;

    // ENHANCEMENT: Added tester relationships
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "automation_tester_id")
    private Tester automationTester;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "manual_tester_id")
    private Tester manualTester;

    // EXISTING: Project relationship
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "project_id")
    private Project project;

    @OneToMany(mappedBy = "jenkinsResult", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnoreProperties("jenkinsResult")
    private List<JenkinsTestCase> testCases;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (jobFrequency == null) {
            jobFrequency = "Unknown";
        }
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
        this.jobFrequency = "Unknown";
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

    // ENHANCED GETTERS AND SETTERS

    public Integer getPassPercentage() {
        return passPercentage;
    }

    public void setPassPercentage(Integer passPercentage) {
        this.passPercentage = passPercentage;
    }

    public String getBugsIdentified() {
        return bugsIdentified;
    }

    public void setBugsIdentified(String bugsIdentified) {
        this.bugsIdentified = bugsIdentified;
    }

    public String getFailureReasons() {
        return failureReasons;
    }

    public void setFailureReasons(String failureReasons) {
        this.failureReasons = failureReasons;
    }

    // NEW: Job frequency getter and setter
    public String getJobFrequency() {
        return jobFrequency;
    }

    public void setJobFrequency(String jobFrequency) {
        this.jobFrequency = jobFrequency;
    }

    public Tester getAutomationTester() {
        return automationTester;
    }

    public void setAutomationTester(Tester automationTester) {
        this.automationTester = automationTester;
    }

    public Tester getManualTester() {
        return manualTester;
    }

    public void setManualTester(Tester manualTester) {
        this.manualTester = manualTester;
    }

    // EXISTING: Project getter and setter
    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public List<JenkinsTestCase> getTestCases() {
        return testCases;
    }

    public void setTestCases(List<JenkinsTestCase> testCases) {
        this.testCases = testCases;
    }

    // Utility methods
    public double getPassPercentageAsDouble() {
        if (totalTests == null || totalTests == 0) {
            return 0.0;
        }
        int passed = passedTests != null ? passedTests : 0;
        return ((double) passed / totalTests) * 100.0;
    }

    public boolean hasFailures() {
        return failedTests != null && failedTests > 0;
    }

    public boolean hasNotes() {
        return (bugsIdentified != null && !bugsIdentified.trim().isEmpty()) ||
                (failureReasons != null && !failureReasons.trim().isEmpty());
    }

    public boolean hasTesters() {
        return automationTester != null || manualTester != null;
    }

    public boolean hasProject() {
        return project != null;
    }

    public String getProjectName() {
        return project != null ? project.getName() : null;
    }

    // NEW: Helper method to get frequency display
    public String getFrequencyDisplay() {
        return jobFrequency != null ? jobFrequency : "Unknown";
    }

    // NEW: Helper method to determine frequency based on job name patterns
    public void inferJobFrequency() {
        if (jobName == null) {
            this.jobFrequency = "Unknown";
            return;
        }

        String lowerJobName = jobName.toLowerCase();

        if (lowerJobName.contains("hourly")) {
            this.jobFrequency = "Hourly";
        } else if (lowerJobName.contains("daily") || lowerJobName.contains("nightly")) {
            this.jobFrequency = "Daily";
        } else if (lowerJobName.contains("weekly")) {
            this.jobFrequency = "Weekly";
        } else if (lowerJobName.contains("monthly")) {
            this.jobFrequency = "Monthly";
        } else if (lowerJobName.contains("manual") || lowerJobName.contains("ondemand") || lowerJobName.contains("trigger")) {
            this.jobFrequency = "On Demand";
        } else if (lowerJobName.contains("continuous") || lowerJobName.contains("ci") || lowerJobName.contains("commit")) {
            this.jobFrequency = "Continuous";
        } else {
            this.jobFrequency = "Unknown";
        }
    }

    @Override
    public String toString() {
        return "JenkinsResult{" +
                "id=" + id +
                ", jobName='" + jobName + '\'' +
                ", buildNumber='" + buildNumber + '\'' +
                ", buildStatus='" + buildStatus + '\'' +
                ", totalTests=" + totalTests +
                ", passedTests=" + passedTests +
                ", failedTests=" + failedTests +
                ", passPercentage=" + passPercentage +
                ", project=" + (project != null ? project.getName() : "null") +
                ", jobFrequency='" + jobFrequency + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof JenkinsResult)) return false;
        JenkinsResult that = (JenkinsResult) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(jobName, that.jobName) &&
                Objects.equals(buildNumber, that.buildNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, jobName, buildNumber);
    }
}