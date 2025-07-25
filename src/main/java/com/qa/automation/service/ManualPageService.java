package com.qa.automation.service;

import com.qa.automation.dto.JiraIssueDto;
import com.qa.automation.dto.JiraTestCaseDto;
import com.qa.automation.model.*;
import com.qa.automation.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class ManualPageService {

    private static final Logger logger = LoggerFactory.getLogger(ManualPageService.class);

    @Autowired
    private JiraIntegrationService jiraIntegrationService;

    @Autowired
    private TestCaseService testCaseService;

    @Autowired
    private JiraIssueRepository jiraIssueRepository;

    @Autowired
    private JiraTestCaseRepository jiraTestCaseRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private TesterRepository testerRepository;

    /**
     * Fetch and sync issues from a specific sprint
     */
    public List<JiraIssueDto> fetchAndSyncSprintIssues(String sprintId) {
        logger.info("Fetching and syncing issues from sprint: {}", sprintId);

        // Fetch issues from Jira
        List<JiraIssueDto> jiraIssues = jiraIntegrationService.fetchIssuesFromSprint(sprintId);

        // Sync with database
        List<JiraIssueDto> syncedIssues = new ArrayList<>();
        for (JiraIssueDto issueDto : jiraIssues) {
            try {
                JiraIssueDto syncedIssue = syncIssueWithDatabase(issueDto);
                syncedIssues.add(syncedIssue);
            } catch (Exception e) {
                logger.error("Error syncing issue {}: {}", issueDto.getJiraKey(), e.getMessage(), e);
            }
        }

        logger.info("Synced {} issues for sprint {}", syncedIssues.size(), sprintId);
        return syncedIssues;
    }

    /**
     * Get all saved issues for a sprint
     */
    public List<JiraIssueDto> getSprintIssues(String sprintId) {
        List<JiraIssue> issues = jiraIssueRepository.findBySprintIdWithLinkedTestCases(sprintId);
        return issues.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * Update test case automation flags
     */
    public JiraTestCaseDto updateTestCaseAutomationFlags(Long testCaseId, boolean canBeAutomated, boolean cannotBeAutomated) {
        logger.info("Updating automation flags for test case {}: canAutomate={}, cannotAutomate={}", 
                testCaseId, canBeAutomated, cannotBeAutomated);

        Optional<JiraTestCase> optionalTestCase = jiraTestCaseRepository.findById(testCaseId);
        if (optionalTestCase.isEmpty()) {
            throw new RuntimeException("Test case not found with id: " + testCaseId);
        }

        JiraTestCase testCase = optionalTestCase.get();
        testCase.setCanBeAutomated(canBeAutomated);
        testCase.setCannotBeAutomated(cannotBeAutomated);

        // If marked as "Can be Automated", trigger the automation readiness flow
        if (canBeAutomated && !cannotBeAutomated) {
            processAutomationReadiness(testCase);
        }

        JiraTestCase savedTestCase = jiraTestCaseRepository.save(testCase);
        return convertTestCaseToDto(savedTestCase);
    }

    /**
     * Search for keyword in issue comments and update count
     */
    public JiraIssueDto searchKeywordInIssue(String jiraKey, String keyword) {
        logger.info("Searching for keyword '{}' in issue: {}", keyword, jiraKey);

        Optional<JiraIssue> optionalIssue = jiraIssueRepository.findByJiraKey(jiraKey);
        if (optionalIssue.isEmpty()) {
            throw new RuntimeException("Issue not found with key: " + jiraKey);
        }

        JiraIssue issue = optionalIssue.get();
        
        // Search for keyword in comments via Jira API
        int keywordCount = jiraIntegrationService.searchKeywordInComments(jiraKey, keyword);
        
        // Update the issue
        issue.setKeywordCount(keywordCount);
        issue.setSearchKeyword(keyword);
        
        JiraIssue savedIssue = jiraIssueRepository.save(issue);
        return convertToDto(savedIssue);
    }

    /**
     * Get automation statistics for a sprint
     */
    public Map<String, Object> getSprintAutomationStatistics(String sprintId) {
        List<JiraTestCase> testCases = jiraTestCaseRepository.findBySprintId(sprintId);
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalTestCases", testCases.size());
        stats.put("readyToAutomate", testCases.stream().filter(tc -> tc.isReadyToAutomate()).count());
        stats.put("notAutomatable", testCases.stream().filter(tc -> tc.isNotAutomatable()).count());
        stats.put("pending", testCases.stream().filter(tc -> tc.isPending()).count());
        
        // Group by project
        Map<String, Map<String, Long>> projectStats = testCases.stream()
                .filter(tc -> tc.getProject() != null)
                .collect(Collectors.groupingBy(
                        tc -> tc.getProject().getName(),
                        Collectors.groupingBy(
                                JiraTestCase::getAutomationStatus,
                                Collectors.counting()
                        )
                ));
        
        stats.put("projectBreakdown", projectStats);
        
        return stats;
    }

    /**
     * Get all available sprints
     */
    public List<Map<String, Object>> getAvailableSprints() {
        return jiraIntegrationService.fetchSprints();
    }

    /**
     * Get all projects for mapping
     */
    public List<Project> getAllProjects() {
        return projectRepository.findAll();
    }

    /**
     * Get all testers for assignment
     */
    public List<Tester> getAllTesters() {
        return testerRepository.findAll();
    }

    /**
     * Map test case to project and domain
     */
    public JiraTestCaseDto mapTestCaseToProject(Long testCaseId, Long projectId, Long testerId) {
        logger.info("Mapping test case {} to project {} and tester {}", testCaseId, projectId, testerId);

        Optional<JiraTestCase> optionalTestCase = jiraTestCaseRepository.findById(testCaseId);
        if (optionalTestCase.isEmpty()) {
            throw new RuntimeException("Test case not found with id: " + testCaseId);
        }

        JiraTestCase testCase = optionalTestCase.get();

        // Set project
        if (projectId != null) {
            Optional<Project> optionalProject = projectRepository.findById(projectId);
            if (optionalProject.isPresent()) {
                testCase.setProject(optionalProject.get());
                if (optionalProject.get().getDomain() != null) {
                    testCase.setDomainMapped(optionalProject.get().getDomain().getName());
                }
            }
        }

        // Set tester
        if (testerId != null) {
            Optional<Tester> optionalTester = testerRepository.findById(testerId);
            if (optionalTester.isPresent()) {
                testCase.setAssignedTester(optionalTester.get());
            }
        }

        JiraTestCase savedTestCase = jiraTestCaseRepository.save(testCase);
        return convertTestCaseToDto(savedTestCase);
    }

    // Private helper methods

    /**
     * Sync Jira issue with database
     */
    private JiraIssueDto syncIssueWithDatabase(JiraIssueDto issueDto) {
        Optional<JiraIssue> existingIssue = jiraIssueRepository.findByJiraKey(issueDto.getJiraKey());
        
        JiraIssue issue;
        if (existingIssue.isPresent()) {
            // Update existing issue
            issue = existingIssue.get();
            updateIssueFromDto(issue, issueDto);
        } else {
            // Create new issue
            issue = createIssueFromDto(issueDto);
        }

        JiraIssue savedIssue = jiraIssueRepository.save(issue);
        
        // Sync linked test cases
        syncLinkedTestCases(savedIssue, issueDto.getLinkedTestCases());
        
        return convertToDto(savedIssue);
    }

    /**
     * Update existing issue from DTO
     */
    private void updateIssueFromDto(JiraIssue issue, JiraIssueDto issueDto) {
        issue.setSummary(issueDto.getSummary());
        issue.setDescription(issueDto.getDescription());
        issue.setAssignee(issueDto.getAssignee());
        issue.setAssigneeDisplayName(issueDto.getAssigneeDisplayName());
        issue.setSprintId(issueDto.getSprintId());
        issue.setSprintName(issueDto.getSprintName());
        issue.setIssueType(issueDto.getIssueType());
        issue.setStatus(issueDto.getStatus());
        issue.setPriority(issueDto.getPriority());
    }

    /**
     * Create new issue from DTO
     */
    private JiraIssue createIssueFromDto(JiraIssueDto issueDto) {
        JiraIssue issue = new JiraIssue();
        issue.setJiraKey(issueDto.getJiraKey());
        issue.setSummary(issueDto.getSummary());
        issue.setDescription(issueDto.getDescription());
        issue.setAssignee(issueDto.getAssignee());
        issue.setAssigneeDisplayName(issueDto.getAssigneeDisplayName());
        issue.setSprintId(issueDto.getSprintId());
        issue.setSprintName(issueDto.getSprintName());
        issue.setIssueType(issueDto.getIssueType());
        issue.setStatus(issueDto.getStatus());
        issue.setPriority(issueDto.getPriority());
        return issue;
    }

    /**
     * Sync linked test cases
     */
    private void syncLinkedTestCases(JiraIssue issue, List<JiraTestCaseDto> testCaseDtos) {
        // Get existing test cases for this issue
        Set<String> existingTestCases = issue.getLinkedTestCases().stream()
                .map(JiraTestCase::getQtestTitle)
                .collect(Collectors.toSet());

        // Add new test cases
        for (JiraTestCaseDto testCaseDto : testCaseDtos) {
            if (!existingTestCases.contains(testCaseDto.getQtestTitle())) {
                JiraTestCase testCase = new JiraTestCase();
                testCase.setQtestTitle(testCaseDto.getQtestTitle());
                testCase.setQtestId(testCaseDto.getQtestId());
                testCase.setJiraIssue(issue);
                issue.addLinkedTestCase(testCase);
            }
        }
    }

    /**
     * Process automation readiness when test case is marked as "Can be Automated"
     */
    private void processAutomationReadiness(JiraTestCase jiraTestCase) {
        logger.info("Processing automation readiness for test case: {}", jiraTestCase.getQtestTitle());

        try {
            // Check if we have project and tester assignment
            if (jiraTestCase.getProject() != null && jiraTestCase.getAssignedTester() != null) {
                
                // Create or update corresponding TestCase entity
                TestCase automationTestCase = createOrUpdateAutomationTestCase(jiraTestCase);
                
                logger.info("Test case '{}' is ready for automation and assigned to tester: {}", 
                        jiraTestCase.getQtestTitle(), 
                        jiraTestCase.getAssignedTester().getName());
            } else {
                logger.warn("Test case '{}' marked as automatable but missing project or tester assignment", 
                        jiraTestCase.getQtestTitle());
            }

        } catch (Exception e) {
            logger.error("Error processing automation readiness for test case '{}': {}", 
                    jiraTestCase.getQtestTitle(), e.getMessage(), e);
        }
    }

    /**
     * Create or update TestCase entity for automation
     */
    private TestCase createOrUpdateAutomationTestCase(JiraTestCase jiraTestCase) {
        // Check if automation test case already exists
        List<TestCase> existingTestCases = testCaseService.getAllTestCases()
                .stream()
                .filter(tc -> tc.getTitle().equals(jiraTestCase.getQtestTitle()))
                .collect(Collectors.toList());

        TestCase testCase;
        if (!existingTestCases.isEmpty()) {
            // Update existing test case
            testCase = existingTestCases.get(0);
            testCase.setStatus("READY_TO_AUTOMATE");
        } else {
            // Create new test case
            testCase = new TestCase();
            testCase.setTitle(jiraTestCase.getQtestTitle());
            testCase.setDescription("Test case imported from Jira issue: " + jiraTestCase.getJiraIssue().getJiraKey());
            testCase.setTestSteps("To be defined during automation implementation");
            testCase.setExpectedResult("To be defined during automation implementation");
            testCase.setPriority("Medium");
            testCase.setStatus("READY_TO_AUTOMATE");
            testCase.setProject(jiraTestCase.getProject());
            testCase.setTester(jiraTestCase.getAssignedTester());
        }

        return testCaseService.createTestCase(testCase);
    }

    /**
     * Convert JiraIssue entity to DTO
     */
    private JiraIssueDto convertToDto(JiraIssue issue) {
        JiraIssueDto dto = new JiraIssueDto();
        dto.setId(issue.getId());
        dto.setJiraKey(issue.getJiraKey());
        dto.setSummary(issue.getSummary());
        dto.setDescription(issue.getDescription());
        dto.setAssignee(issue.getAssignee());
        dto.setAssigneeDisplayName(issue.getAssigneeDisplayName());
        dto.setSprintId(issue.getSprintId());
        dto.setSprintName(issue.getSprintName());
        dto.setIssueType(issue.getIssueType());
        dto.setStatus(issue.getStatus());
        dto.setPriority(issue.getPriority());
        dto.setKeywordCount(issue.getKeywordCount());
        dto.setSearchKeyword(issue.getSearchKeyword());
        dto.setCreatedAt(issue.getCreatedAt());
        dto.setUpdatedAt(issue.getUpdatedAt());

        // Convert linked test cases
        List<JiraTestCaseDto> testCaseDtos = issue.getLinkedTestCases().stream()
                .map(this::convertTestCaseToDto)
                .collect(Collectors.toList());
        dto.setLinkedTestCases(testCaseDtos);

        return dto;
    }

    /**
     * Convert JiraTestCase entity to DTO
     */
    private JiraTestCaseDto convertTestCaseToDto(JiraTestCase testCase) {
        JiraTestCaseDto dto = new JiraTestCaseDto();
        dto.setId(testCase.getId());
        dto.setQtestTitle(testCase.getQtestTitle());
        dto.setQtestId(testCase.getQtestId());
        dto.setCanBeAutomated(testCase.getCanBeAutomated());
        dto.setCannotBeAutomated(testCase.getCannotBeAutomated());
        dto.setAutomationStatus(testCase.getAutomationStatus());
        dto.setAssignedTesterId(testCase.getAssignedTesterId());
        dto.setDomainMapped(testCase.getDomainMapped());
        dto.setNotes(testCase.getNotes());
        dto.setCreatedAt(testCase.getCreatedAt());
        dto.setUpdatedAt(testCase.getUpdatedAt());

        // Set project information
        if (testCase.getProject() != null) {
            dto.setProjectId(testCase.getProject().getId());
            dto.setProjectName(testCase.getProject().getName());
        }

        // Set tester information
        if (testCase.getAssignedTester() != null) {
            dto.setAssignedTesterName(testCase.getAssignedTester().getName());
        }

        return dto;
    }
}