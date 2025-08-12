package com.qa.automation.dto;

import java.time.LocalDateTime;

public class JiraTestCaseDto {
    private Long id;
    private String qtestTitle;
    private String qtestId;
    private String qtestAssignee;
    private String qtestPriority;
    private String qtestAutomationStatus;
    private Boolean canBeAutomated;
    private Boolean cannotBeAutomated;
    private String automationStatus;
    private Long assignedTesterId;
    private String assignedTesterName;
    private Long projectId;
    private String projectName;
    private String domainMapped;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Constructors
    public JiraTestCaseDto() {}

    public JiraTestCaseDto(String qtestTitle) {
        this.qtestTitle = qtestTitle;
        this.canBeAutomated = false;
        this.cannotBeAutomated = false;
        this.automationStatus = "PENDING";
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

    public String getQtestAssignee() {
        return qtestAssignee;
    }

    public void setQtestAssignee(String qtestAssignee) {
        this.qtestAssignee = qtestAssignee;
    }

    public String getQtestPriority() {
        return qtestPriority;
    }

    public void setQtestPriority(String qtestPriority) {
        this.qtestPriority = qtestPriority;
    }

    public String getQtestAutomationStatus() {
        return qtestAutomationStatus;
    }

    public void setQtestAutomationStatus(String qtestAutomationStatus) {
        this.qtestAutomationStatus = qtestAutomationStatus;
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

    public String getAssignedTesterName() {
        return assignedTesterName;
    }

    public void setAssignedTesterName(String assignedTesterName) {
        this.assignedTesterName = assignedTesterName;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
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
    private void updateAutomationStatus() {
        if (canBeAutomated != null && cannotBeAutomated != null) {
            if (canBeAutomated && !cannotBeAutomated) {
                this.automationStatus = "Ready to Automate";
            } else if (!canBeAutomated && cannotBeAutomated) {
                this.automationStatus = "NOT_AUTOMATABLE";
            } else {
                this.automationStatus = "PENDING";
            }
        }
    }

    public boolean isReadyToAutomate() {
        return "Ready to Automate".equals(automationStatus);
    }

    public boolean isNotAutomatable() {
        return "NOT_AUTOMATABLE".equals(automationStatus);
    }

    public boolean isPending() {
        return "PENDING".equals(automationStatus);
    }
}