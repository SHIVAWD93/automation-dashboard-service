package com.qa.automation.model;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

@Entity
@Table(name = "jira_issues")
public class JiraIssue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "jira_key", nullable = false, unique = true)
    private String jiraKey;

    @Column(nullable = false)
    private String summary;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "assignee")
    private String assignee;

    @Column(name = "assignee_display_name")
    private String assigneeDisplayName;

    @Column(name = "sprint_id")
    private String sprintId;

    @Column(name = "sprint_name")
    private String sprintName;

    @Column(name = "issue_type")
    private String issueType;

    @Column(name = "status")
    private String status;

    @Column(name = "priority")
    private String priority;

    @Column(name = "keyword_count")
    private Integer keywordCount = 0;

    @Column(name = "search_keyword")
    private String searchKeyword;

    @OneToMany(mappedBy = "jiraIssue", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnoreProperties("jiraIssue")
    private List<JiraTestCase> linkedTestCases = new ArrayList<>();

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
    public JiraIssue() {}

    public JiraIssue(String jiraKey, String summary, String description, String assignee) {
        this.jiraKey = jiraKey;
        this.summary = summary;
        this.description = description;
        this.assignee = assignee;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getJiraKey() {
        return jiraKey;
    }

    public void setJiraKey(String jiraKey) {
        this.jiraKey = jiraKey;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAssignee() {
        return assignee;
    }

    public void setAssignee(String assignee) {
        this.assignee = assignee;
    }

    public String getAssigneeDisplayName() {
        return assigneeDisplayName;
    }

    public void setAssigneeDisplayName(String assigneeDisplayName) {
        this.assigneeDisplayName = assigneeDisplayName;
    }

    public String getSprintId() {
        return sprintId;
    }

    public void setSprintId(String sprintId) {
        this.sprintId = sprintId;
    }

    public String getSprintName() {
        return sprintName;
    }

    public void setSprintName(String sprintName) {
        this.sprintName = sprintName;
    }

    public String getIssueType() {
        return issueType;
    }

    public void setIssueType(String issueType) {
        this.issueType = issueType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public Integer getKeywordCount() {
        return keywordCount;
    }

    public void setKeywordCount(Integer keywordCount) {
        this.keywordCount = keywordCount;
    }

    public String getSearchKeyword() {
        return searchKeyword;
    }

    public void setSearchKeyword(String searchKeyword) {
        this.searchKeyword = searchKeyword;
    }

    public List<JiraTestCase> getLinkedTestCases() {
        return linkedTestCases;
    }

    public void setLinkedTestCases(List<JiraTestCase> linkedTestCases) {
        this.linkedTestCases = linkedTestCases;
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
    public void addLinkedTestCase(JiraTestCase testCase) {
        linkedTestCases.add(testCase);
        testCase.setJiraIssue(this);
    }

    public void removeLinkedTestCase(JiraTestCase testCase) {
        linkedTestCases.remove(testCase);
        testCase.setJiraIssue(null);
    }

    @Override
    public String toString() {
        return "JiraIssue{" +
                "id=" + id +
                ", jiraKey='" + jiraKey + '\'' +
                ", summary='" + summary + '\'' +
                ", assignee='" + assignee + '\'' +
                ", sprintName='" + sprintName + '\'' +
                ", keywordCount=" + keywordCount +
                '}';
    }
}