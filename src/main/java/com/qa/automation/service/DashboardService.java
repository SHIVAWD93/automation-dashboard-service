package com.qa.automation.service;

import com.qa.automation.repository.DomainRepository;
import com.qa.automation.repository.ProjectRepository;
import com.qa.automation.repository.TestCaseRepository;
import com.qa.automation.repository.TesterRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DashboardService {

    @Autowired
    private DomainRepository domainRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private TestCaseRepository testCaseRepository;

    @Autowired
    private TesterRepository testerRepository;

    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();

        // Basic counts
        stats.put("totalDomains", domainRepository.count());
        stats.put("totalProjects", projectRepository.count());
        stats.put("totalTestCases", testCaseRepository.count());
        stats.put("totalTesters", testerRepository.count());

        // Active counts
        stats.put("activeDomains", domainRepository.countByStatus("Active"));
        stats.put("activeProjects", projectRepository.countByStatus("Active"));

        // Test case status counts
        stats.put("automatedTestCases", testCaseRepository.countByStatus("Automated"));
        stats.put("inProgressTestCases", testCaseRepository.countByStatus("In Progress"));
        stats.put("readyTestCases", testCaseRepository.countByStatus("Ready to Automate"));
        stats.put("completedTestCases", testCaseRepository.countByStatus("Completed"));

        return stats;
    }

    public Map<String, Object> getDomainStats(Long domainId) {
        Map<String, Object> stats = new HashMap<>();

        // Projects in domain
        stats.put("totalProjects", projectRepository.countByDomainId(domainId));
        stats.put("activeProjects", projectRepository.countByDomainIdAndStatus(domainId, "Active"));

        // Test cases in domain
        List<Object[]> testCaseStats = testCaseRepository.getTestCaseStatsByDomain(domainId);
        processTestCaseStats(stats, testCaseStats);

        return stats;
    }

    public Map<String, Object> getProjectStats(Long projectId) {
        Map<String, Object> stats = new HashMap<>();

        // Test cases in project
        stats.put("totalTestCases", testCaseRepository.countByProjectId(projectId));
        stats.put("automatedTestCases", testCaseRepository.countByProjectIdAndStatus(projectId, "Automated"));
        stats.put("inProgressTestCases", testCaseRepository.countByProjectIdAndStatus(projectId, "In Progress"));
        stats.put("readyTestCases", testCaseRepository.countByProjectIdAndStatus(projectId, "Ready to Automate"));
        stats.put("completedTestCases", testCaseRepository.countByProjectIdAndStatus(projectId, "Completed"));

        return stats;
    }

    private void processTestCaseStats(Map<String, Object> stats, List<Object[]> testCaseStats) {
        long totalTestCases = 0;
        long automatedTestCases = 0;
        long inProgressTestCases = 0;
        long readyTestCases = 0;
        long completedTestCases = 0;

        for (Object[] stat : testCaseStats) {
            String status = (String) stat[0];
            Long count = (Long) stat[1];

            totalTestCases += count;

            switch (status) {
                case "Automated":
                    automatedTestCases = count;
                    break;
                case "In Progress":
                    inProgressTestCases = count;
                    break;
                case "Ready to Automate":
                    readyTestCases = count;
                    break;
                case "Completed":
                    completedTestCases = count;
                    break;
            }
        }

        stats.put("totalTestCases", totalTestCases);
        stats.put("automatedTestCases", automatedTestCases);
        stats.put("inProgressTestCases", inProgressTestCases);
        stats.put("readyTestCases", readyTestCases);
        stats.put("completedTestCases", completedTestCases);
    }
}