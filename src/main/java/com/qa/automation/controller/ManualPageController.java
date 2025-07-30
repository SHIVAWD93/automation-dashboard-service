package com.qa.automation.controller;

import com.qa.automation.dto.JiraIssueDto;
import com.qa.automation.dto.JiraTestCaseDto;
import com.qa.automation.model.Project;
import com.qa.automation.model.Tester;
import com.qa.automation.model.Domain;
import com.qa.automation.service.ManualPageService;
import com.qa.automation.service.JiraIntegrationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/manual-page")
@CrossOrigin(origins = {"http://localhost:4200", "http://localhost:5000"})
public class ManualPageController {

    private static final Logger logger = LoggerFactory.getLogger(ManualPageController.class);

    @Autowired
    private ManualPageService manualPageService;

    @Autowired
    private JiraIntegrationService jiraIntegrationService;

    /**
     * ENHANCED: Get all available sprints with optional project configuration
     */
    @GetMapping("/sprints")
    public ResponseEntity<List<Map<String, Object>>> getAvailableSprints(
            @RequestParam(required = false) String jiraProjectKey,
            @RequestParam(required = false) String jiraBoardId) {
        try {
            logger.info("Fetching available sprints (Project: {}, Board: {})", jiraProjectKey, jiraBoardId);
            List<Map<String, Object>> sprints = manualPageService.getAvailableSprints(jiraProjectKey, jiraBoardId);
            return ResponseEntity.ok(sprints);
        } catch (Exception e) {
            logger.error("Error fetching sprints: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * ENHANCED: Fetch and sync issues from a specific sprint with optional project configuration
     */
    @PostMapping("/sprints/{sprintId}/sync")
    public ResponseEntity<List<JiraIssueDto>> syncSprintIssues(
            @PathVariable String sprintId,
            @RequestParam(required = false) String jiraProjectKey,
            @RequestParam(required = false) String jiraBoardId) {
        try {
            logger.info("Syncing issues for sprint: {} (Project: {}, Board: {})",
                    sprintId, jiraProjectKey, jiraBoardId);
            List<JiraIssueDto> issues = manualPageService.fetchAndSyncSprintIssues(
                    sprintId, jiraProjectKey, jiraBoardId);
            return ResponseEntity.ok(issues);
        } catch (Exception e) {
            logger.error("Error syncing sprint issues: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get saved issues for a specific sprint
     */
    @GetMapping("/sprints/{sprintId}/issues")
    public ResponseEntity<List<JiraIssueDto>> getSprintIssues(@PathVariable String sprintId) {
        try {
            logger.info("Getting issues for sprint: {}", sprintId);
            List<JiraIssueDto> issues = manualPageService.getSprintIssues(sprintId);
            return ResponseEntity.ok(issues);
        } catch (Exception e) {
            logger.error("Error getting sprint issues: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Update test case automation flags
     */
    @PutMapping("/test-cases/{testCaseId}/automation-flags")
    public ResponseEntity<JiraTestCaseDto> updateAutomationFlags(
            @PathVariable Long testCaseId,
            @RequestBody AutomationFlagsRequest request) {
        try {
            logger.info("Updating automation flags for test case: {}", testCaseId);
            JiraTestCaseDto updatedTestCase = manualPageService.updateTestCaseAutomationFlags(
                    testCaseId,
                    request.getCanBeAutomated(),
                    request.getCannotBeAutomated()
            );
            return ResponseEntity.ok(updatedTestCase);
        } catch (Exception e) {
            logger.error("Error updating automation flags: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Map test case to project and assign tester
     */
    @PutMapping("/test-cases/{testCaseId}/mapping")
    public ResponseEntity<JiraTestCaseDto> mapTestCase(
            @PathVariable Long testCaseId,
            @RequestBody TestCaseMappingRequest request) {
        try {
            logger.info("Mapping test case {} to project {} and tester {}",
                    testCaseId, request.getProjectId(), request.getTesterId());
            JiraTestCaseDto updatedTestCase = manualPageService.mapTestCaseToProject(
                    testCaseId,
                    request.getProjectId(),
                    request.getTesterId()
            );
            return ResponseEntity.ok(updatedTestCase);
        } catch (Exception e) {
            logger.error("Error mapping test case: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Search for keyword in issue comments
     */
    @PostMapping("/issues/{jiraKey}/keyword-search")
    public ResponseEntity<JiraIssueDto> searchKeywordInComments(
            @PathVariable String jiraKey,
            @RequestBody KeywordSearchRequest request) {
        try {
            logger.info("Searching for keyword '{}' in issue: {}", request.getKeyword(), jiraKey);
            JiraIssueDto updatedIssue = manualPageService.searchKeywordInIssue(jiraKey, request.getKeyword());
            return ResponseEntity.ok(updatedIssue);
        } catch (Exception e) {
            logger.error("Error searching keyword in comments: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * NEW: Global keyword search across all issues
     */
    @PostMapping("/global-keyword-search")
    public ResponseEntity<Map<String, Object>> globalKeywordSearch(
            @RequestBody GlobalKeywordSearchRequest request) {
        try {
            logger.info("Performing global keyword search for '{}' in project: {}",
                    request.getKeyword(), request.getJiraProjectKey());
            Map<String, Object> searchResults = jiraIntegrationService.searchKeywordGlobally(
                    request.getKeyword(), request.getJiraProjectKey());
            return ResponseEntity.ok(searchResults);
        } catch (Exception e) {
            logger.error("Error performing global keyword search: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get automation statistics for a sprint
     */
    @GetMapping("/sprints/{sprintId}/statistics")
    public ResponseEntity<Map<String, Object>> getSprintStatistics(@PathVariable String sprintId) {
        try {
            logger.info("Getting automation statistics for sprint: {}", sprintId);
            Map<String, Object> statistics = manualPageService.getSprintAutomationStatistics(sprintId);
            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            logger.error("Error getting sprint statistics: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get all projects for mapping
     */
    @GetMapping("/projects")
    public ResponseEntity<List<Project>> getAllProjects() {
        try {
            logger.info("Getting all projects for mapping");
            List<Project> projects = manualPageService.getAllProjects();
            return ResponseEntity.ok(projects);
        } catch (Exception e) {
            logger.error("Error getting projects: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * NEW: Get all domains for filtering
     */
    @GetMapping("/domains")
    public ResponseEntity<List<Domain>> getAllDomains() {
        try {
            logger.info("Getting all domains for filtering");
            List<Domain> domains = manualPageService.getAllDomains();
            return ResponseEntity.ok(domains);
        } catch (Exception e) {
            logger.error("Error getting domains: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get all testers for assignment
     */
    @GetMapping("/testers")
    public ResponseEntity<List<Tester>> getAllTesters() {
        try {
            logger.info("Getting all testers for assignment");
            List<Tester> testers = manualPageService.getAllTesters();
            return ResponseEntity.ok(testers);
        } catch (Exception e) {
            logger.error("Error getting testers: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Test Jira connection
     */
    @GetMapping("/test-connection")
    public ResponseEntity<Map<String, Object>> testConnection() {
        try {
            logger.info("Testing Jira connection");
            boolean isConnected = jiraIntegrationService.testConnection();
            Map<String, Object> response = Map.of(
                    "connected", isConnected,
                    "message", isConnected ? "Successfully connected to Jira" : "Failed to connect to Jira"
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error testing connection: {}", e.getMessage(), e);
            Map<String, Object> response = Map.of(
                    "connected", false,
                    "message", "Error testing connection: " + e.getMessage()
            );
            return ResponseEntity.ok(response);
        }
    }

    // Request DTOs
    public static class AutomationFlagsRequest {
        private boolean canBeAutomated;
        private boolean cannotBeAutomated;

        public boolean getCanBeAutomated() {
            return canBeAutomated;
        }

        public void setCanBeAutomated(boolean canBeAutomated) {
            this.canBeAutomated = canBeAutomated;
        }

        public boolean getCannotBeAutomated() {
            return cannotBeAutomated;
        }

        public void setCannotBeAutomated(boolean cannotBeAutomated) {
            this.cannotBeAutomated = cannotBeAutomated;
        }
    }

    public static class TestCaseMappingRequest {
        private Long projectId;
        private Long testerId;

        public Long getProjectId() {
            return projectId;
        }

        public void setProjectId(Long projectId) {
            this.projectId = projectId;
        }

        public Long getTesterId() {
            return testerId;
        }

        public void setTesterId(Long testerId) {
            this.testerId = testerId;
        }
    }

    public static class KeywordSearchRequest {
        private String keyword;

        public String getKeyword() {
            return keyword;
        }

        public void setKeyword(String keyword) {
            this.keyword = keyword;
        }
    }

    // NEW: Global keyword search request
    public static class GlobalKeywordSearchRequest {
        private String keyword;
        private String jiraProjectKey;

        public String getKeyword() {
            return keyword;
        }

        public void setKeyword(String keyword) {
            this.keyword = keyword;
        }

        public String getJiraProjectKey() {
            return jiraProjectKey;
        }

        public void setJiraProjectKey(String jiraProjectKey) {
            this.jiraProjectKey = jiraProjectKey;
        }
    }
}