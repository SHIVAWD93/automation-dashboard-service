package com.qa.automation.dto;

import com.qa.automation.model.JiraTestCase;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

public class JiraIssueDto {
    private Long id;
    private String jiraKey;
    private String summary;
    private String description;
    private String assignee;
    private String assigneeDisplayName;
    private String sprintId;
    private String sprintName;
    private String issueType;
    private String status;
    private String priority;
    private Integer keywordCount;
    private String searchKeyword;
    private List<JiraTestCaseDto> linkedTestCases = new ArrayList<>();
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Constructors
    public JiraIssueDto() {}

    public JiraIssueDto(String jiraKey, String summary, String description, String assignee) {
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

    public List<JiraTestCaseDto> getLinkedTestCases() {
        return linkedTestCases;
    }

    public void setLinkedTestCases(List<JiraTestCaseDto> linkedTestCases) {
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
    public void addLinkedTestCase(JiraTestCaseDto testCase) {
        this.linkedTestCases.add(testCase);
    }

    public int getAutomationReadyCount() {
        return (int) linkedTestCases.stream()
                .filter(tc -> "Ready to Automate".equals(tc.getAutomationStatus()))
                .count();
    }

    public int getNotAutomatableCount() {
        return (int) linkedTestCases.stream()
                .filter(tc -> "NOT_AUTOMATABLE".equals(tc.getAutomationStatus()))
                .count();
    }

    public int getPendingCount() {
        return (int) linkedTestCases.stream()
                .filter(tc -> "PENDING".equals(tc.getAutomationStatus()))
                .count();
    }
}