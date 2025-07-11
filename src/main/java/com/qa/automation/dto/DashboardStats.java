package com.qa.automation.dto;

public class DashboardStats {
    private long totalTesters;
    private long totalProjects;
    private long totalTestCases;
    private long automatedCount;
    private long manualCount;
    private long inProgressCount;
    private long completedCount;
    
    // Getters and Setters
    public long getTotalTesters() {
        return totalTesters;
    }
    
    public void setTotalTesters(long totalTesters) {
        this.totalTesters = totalTesters;
    }
    
    public long getTotalProjects() {
        return totalProjects;
    }
    
    public void setTotalProjects(long totalProjects) {
        this.totalProjects = totalProjects;
    }
    
    public long getTotalTestCases() {
        return totalTestCases;
    }
    
    public void setTotalTestCases(long totalTestCases) {
        this.totalTestCases = totalTestCases;
    }
    
    public long getAutomatedCount() {
        return automatedCount;
    }
    
    public void setAutomatedCount(long automatedCount) {
        this.automatedCount = automatedCount;
    }
    
    public long getManualCount() {
        return manualCount;
    }
    
    public void setManualCount(long manualCount) {
        this.manualCount = manualCount;
    }
    
    public long getInProgressCount() {
        return inProgressCount;
    }
    
    public void setInProgressCount(long inProgressCount) {
        this.inProgressCount = inProgressCount;
    }
    
    public long getCompletedCount() {
        return completedCount;
    }
    
    public void setCompletedCount(long completedCount) {
        this.completedCount = completedCount;
    }
}
