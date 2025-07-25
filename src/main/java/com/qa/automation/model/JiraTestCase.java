package com.qa.automation.model;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDateTime;

@Entity
@Table(name = "jira_test_cases")
public class JiraTestCase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "qtest_title", nullable = false)
    private String qtestTitle;

    @Column(name = "qtest_id")
    private String qtestId;

    @Column(name = "can_be_automated", nullable = false)
    private Boolean canBeAutomated = false;

    @Column(name = "cannot_be_automated", nullable = false)
    private Boolean cannotBeAutomated = false;

    @Column(name = "automation_status")
    private String automationStatus; // "READY_TO_AUTOMATE", "NOT_AUTOMATABLE", "PENDING"

    @Column(name = "assigned_tester_id")
    private Long assignedTesterId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "jira_issue_id", nullable = false)
    @JsonIgnoreProperties("linkedTestCases")
    private JiraIssue jiraIssue;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "project_id")
    @JsonIgnoreProperties({"testCases", "domain"})
    private Project project;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "tester_id")
    @JsonIgnoreProperties("testCases")
    private Tester assignedTester;

    @Column(name = "domain_mapped")
    private String domainMapped;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        
        // Set automation status based on checkbox values
        updateAutomationStatus();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        
        // Update automation status based on checkbox values
        updateAutomationStatus();
    }

    // Constructors
    public JiraTestCase() {}

    public JiraTestCase(String qtestTitle, JiraIssue jiraIssue) {
        this.qtestTitle = qtestTitle;
        this.jiraIssue = jiraIssue;
    }

    // Private method to update automation status
    private void updateAutomationStatus() {
        if (canBeAutomated && !cannotBeAutomated) {
            this.automationStatus = "READY_TO_AUTOMATE";
        } else if (!canBeAutomated && cannotBeAutomated) {
            this.automationStatus = "NOT_AUTOMATABLE";
        } else {
            this.automationStatus = "PENDING";
        }
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getQtestTitle() {
        return qtestTitle;
    }

    public void setQtestTitle(String qtestTitle) {
        this.qtestTitle = qtestTitle;
    }

    public String getQtestId() {
        return qtestId;
    }

    public void setQtestId(String qtestId) {
        this.qtestId = qtestId;
    }

    public Boolean getCanBeAutomated() {
        return canBeAutomated;
    }

    public void setCanBeAutomated(Boolean canBeAutomated) {
        this.canBeAutomated = canBeAutomated;
        updateAutomationStatus();
    }

    public Boolean getCannotBeAutomated() {
        return cannotBeAutomated;
    }

    public void setCannotBeAutomated(Boolean cannotBeAutomated) {
        this.cannotBeAutomated = cannotBeAutomated;
        updateAutomationStatus();
    }

    public String getAutomationStatus() {
        return automationStatus;
    }

    public void setAutomationStatus(String automationStatus) {
        this.automationStatus = automationStatus;
    }

    public Long getAssignedTesterId() {
        return assignedTesterId;
    }

    public void setAssignedTesterId(Long assignedTesterId) {
        this.assignedTesterId = assignedTesterId;
    }

    public JiraIssue getJiraIssue() {
        return jiraIssue;
    }

    public void setJiraIssue(JiraIssue jiraIssue) {
        this.jiraIssue = jiraIssue;
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public Tester getAssignedTester() {
        return assignedTester;
    }

    public void setAssignedTester(Tester assignedTester) {
        this.assignedTester = assignedTester;
        if (assignedTester != null) {
            this.assignedTesterId = assignedTester.getId();
        }
    }

    public String getDomainMapped() {
        return domainMapped;
    }

    public void setDomainMapped(String domainMapped) {
        this.domainMapped = domainMapped;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
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

    // Helper methods
    public boolean isReadyToAutomate() {
        return "READY_TO_AUTOMATE".equals(automationStatus);
    }

    public boolean isNotAutomatable() {
        return "NOT_AUTOMATABLE".equals(automationStatus);
    }

    public boolean isPending() {
        return "PENDING".equals(automationStatus);
    }

    @Override
    public String toString() {
        return "JiraTestCase{" +
                "id=" + id +
                ", qtestTitle='" + qtestTitle + '\'' +
                ", canBeAutomated=" + canBeAutomated +
                ", cannotBeAutomated=" + cannotBeAutomated +
                ", automationStatus='" + automationStatus + '\'' +
                ", jiraIssue=" + (jiraIssue != null ? jiraIssue.getJiraKey() : null) +
                '}';
    }
}