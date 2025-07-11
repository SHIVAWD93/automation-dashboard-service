package com.qa.automation.service;

import com.qa.automation.dto.DashboardStats;
import com.qa.automation.repository.ProjectRepository;
import com.qa.automation.repository.TesterRepository;
import com.qa.automation.repository.TestCaseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class DashboardService {
    
    @Autowired
    private TesterRepository testerRepository;
    
    @Autowired
    private ProjectRepository projectRepository;
    
    @Autowired
    private TestCaseRepository testCaseRepository;
    
    public DashboardStats getDashboardStats() {
        DashboardStats stats = new DashboardStats();
        
        try {
            // Count totals
            stats.setTotalTesters(testerRepository.count()); // testerRepository.count()
            stats.setTotalProjects(projectRepository.count()); // projectRepository.count()
            stats.setTotalTestCases(testCaseRepository.count()); // testCaseRepository.count()
            
            // Count by status - hardcoded from database
            stats.setAutomatedCount(testCaseRepository.countByStatus("Automated")+testCaseRepository.countByStatus("Completed")); // Automated
            stats.setManualCount(testCaseRepository.countByStatus("Ready to Automate")); // Manual Testing
            stats.setInProgressCount(testCaseRepository.countByStatus("In Progress")); // In Progress
            stats.setCompletedCount(testCaseRepository.countByStatus("Completed")); // Ready to Automate
            
        } catch (Exception e) {
            // Fallback values if database query fails
            stats.setTotalTesters(0);
            stats.setTotalProjects(0);
            stats.setTotalTestCases(0);
            stats.setAutomatedCount(0);
            stats.setManualCount(0);
            stats.setInProgressCount(0);
            stats.setCompletedCount(0);
        }
        
        return stats;
    }
    
    public Map<String, Long> getProjectStats(Long projectId) {
        Map<String, Long> stats = new HashMap<>();
        
        stats.put("readyToAutomateCount", testCaseRepository.countByProjectIdAndStatus(projectId, "Ready to Automate"));
        stats.put("automatedCount", testCaseRepository.countByProjectIdAndStatus(projectId, "Automated"));
        stats.put("inProgressCount", testCaseRepository.countByProjectIdAndStatus(projectId, "In Progress"));
        stats.put("completedCount", testCaseRepository.countByProjectIdAndStatus(projectId, "Completed"));
        
        return stats;
    }
}
