package com.qa.automation.model;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDateTime;

@Entity
@Table(name = "test_cases")
public class TestCase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "test_steps", columnDefinition = "TEXT")
    private String testSteps;

    @Column(name = "expected_result", columnDefinition = "TEXT")
    private String expectedResult;

    @Column(nullable = false)
    private String priority;

    @Column(nullable = false)
    private String status;

    // Relationship to Project
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "project_id", nullable = false)
    @JsonIgnoreProperties({"testCases", "domain"})
    private Project project;

    // Relationship to Tester
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "tester_id", nullable = false)
    @JsonIgnoreProperties("testCases")
    private Tester tester;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

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
    public TestCase() {}

    public TestCase(String title, String description, String testSteps, String expectedResult,
                    String priority, String status, Project project, Tester tester) {
        this.title = title;
        this.description = description;
        this.testSteps = testSteps;
        this.expectedResult = expectedResult;
        this.priority = priority;
        this.status = status;
        this.project = project;
        this.tester = tester;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTestSteps() {
        return testSteps;
    }

    public void setTestSteps(String testSteps) {
        this.testSteps = testSteps;
    }

    public String getExpectedResult() {
        return expectedResult;
    }

    public void setExpectedResult(String expectedResult) {
        this.expectedResult = expectedResult;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public Tester getTester() {
        return tester;
    }

    public void setTester(Tester tester) {
        this.tester = tester;
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

    // Convenience methods for backward compatibility
    public Long getProjectId() {
        return project != null ? project.getId() : null;
    }

    public void setProjectId(Long projectId) {
        if (projectId != null) {
            if (this.project == null) {
                this.project = new Project();
            }
            this.project.setId(projectId);
        }
    }

    public Long getTesterId() {
        return tester != null ? tester.getId() : null;
    }

    public void setTesterId(Long testerId) {
        if (testerId != null) {
            if (this.tester == null) {
                this.tester = new Tester();
            }
            this.tester.setId(testerId);
        }
    }
}