package com.qa.automation.service;

import com.qa.automation.config.JiraConfig;
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
    JiraConfig jiraConfig;

    @Autowired
    private JiraIssueRepository jiraIssueRepository;

    @Autowired
    private JiraTestCaseRepository jiraTestCaseRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private TesterRepository testerRepository;

    @Autowired
    private DomainRepository domainRepository;

    @Autowired
    private QTestService qTestService;

    @Autowired
    private DataInitializationService dataInitializationService;

    /**
     * ENHANCED: Fetch and sync issues from a specific sprint with domain and project mapping
     */
    public List<JiraIssueDto> fetchAndSyncSprintIssues(String sprintId, String jiraProjectKey, String jiraBoardId, Long domainId, Long projectId) {
        logger.info("Fetching and syncing issues from sprint: {} with domain {} and project {} mapping",
                sprintId, domainId, projectId);

        // Fetch issues from Jira with optional project configuration
        List<JiraIssueDto> jiraIssues = jiraIntegrationService.fetchIssuesFromSprint(
                sprintId, jiraProjectKey, jiraBoardId);

        // Find domain and project for mapping
        Domain selectedDomain = null;
        Project selectedProject = null;
        
        if (domainId != null) {
            selectedDomain = domainRepository.findById(domainId).orElse(null);
        }
        
        if (projectId != null) {
            selectedProject = projectRepository.findById(projectId).orElse(null);
        }

        // Sync with database and apply mappings
        List<JiraIssueDto> syncedIssues = new ArrayList<>();
        for (JiraIssueDto issueDto : jiraIssues) {
            try {
                JiraIssueDto syncedIssue = syncIssueWithDomainAndProjectMapping(issueDto, selectedDomain, selectedProject);
                syncedIssues.add(syncedIssue);
            } catch (Exception e) {
                logger.error("Error syncing issue {}: {}", issueDto.getJiraKey(), e.getMessage(), e);
            }
        }

        logger.info("Synced {} issues for sprint {} with domain/project mapping", syncedIssues.size(), sprintId);
        return syncedIssues;
    }

    /**
     * ENHANCED: Fetch and sync issues from a specific sprint with optional project configuration
     */
    public List<JiraIssueDto> fetchAndSyncSprintIssues(String sprintId, String jiraProjectKey, String jiraBoardId) {
        logger.info("Fetching and syncing issues from sprint: {} (Project: {}, Board: {})",
                sprintId, jiraProjectKey, jiraBoardId);

        // Fetch issues from Jira with optional project configuration
        List<JiraIssueDto> jiraIssues = jiraIntegrationService.fetchIssuesFromSprint(
                sprintId, jiraProjectKey, jiraBoardId);

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
     * Original method for backward compatibility
     */
    public List<JiraIssueDto> fetchAndSyncSprintIssues(String sprintId) {
        return fetchAndSyncSprintIssues(sprintId, null, null);
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
     * ENHANCED: Get all available sprints with optional project configuration
     */
    public List<Map<String, Object>> getAvailableSprints(String jiraProjectKey, String jiraBoardId) {
        return jiraIntegrationService.fetchSprints(jiraProjectKey, jiraBoardId);
    }

    /**
     * Original method for backward compatibility
     */
    public List<Map<String, Object>> getAvailableSprints() {
        return getAvailableSprints(null, null);
    }

    /**
     * Get all projects for mapping
     */
    public List<Project> getAllProjects() {
        return projectRepository.findAll();
    }

    /**
     * NEW: Get all domains for filtering
     */
    public List<Domain> getAllDomains() {
        return domainRepository.findAll();
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

        // Set project with validation
        if (projectId != null) {
            Optional<Project> optionalProject = projectRepository.findById(projectId);
            if (optionalProject.isPresent()) {
                testCase.setProject(optionalProject.get());
                if (optionalProject.get().getDomain() != null) {
                    testCase.setDomainMapped(optionalProject.get().getDomain().getName());
                }
                logger.debug("Mapped test case {} to project: {}", testCaseId, optionalProject.get().getName());
            } else {
                logger.warn("Project with ID {} does not exist, skipping project mapping for test case: {}", projectId, testCaseId);
                throw new RuntimeException("Project not found with id: " + projectId);
            }
        }

        // Set tester with validation
        if (testerId != null) {
            Optional<Tester> optionalTester = testerRepository.findById(testerId);
            if (optionalTester.isPresent()) {
                testCase.setAssignedTester(optionalTester.get());
                logger.debug("Assigned test case {} to tester: {}", testCaseId, optionalTester.get().getName());
            } else {
                logger.warn("Tester with ID {} does not exist, skipping tester assignment for test case: {}", testerId, testCaseId);
                throw new RuntimeException("Tester not found with id: " + testerId);
            }
        }

        JiraTestCase savedTestCase = jiraTestCaseRepository.save(testCase);
        return convertTestCaseToDto(savedTestCase);
    }

    // Private helper methods

    /**
     * Sync Jira issue with database and apply domain/project mapping
     */
    private JiraIssueDto syncIssueWithDomainAndProjectMapping(JiraIssueDto issueDto, Domain selectedDomain, Project selectedProject) {
        JiraIssueDto syncedIssue = syncIssueWithDatabase(issueDto);
        
        // Apply domain and project mapping to linked test cases
        if ((selectedDomain != null || selectedProject != null) && syncedIssue.getLinkedTestCases() != null) {
            for (JiraTestCaseDto testCaseDto : syncedIssue.getLinkedTestCases()) {
                if (testCaseDto.getId() != null) {
                    Optional<JiraTestCase> optionalTestCase = jiraTestCaseRepository.findById(testCaseDto.getId());
                    if (optionalTestCase.isPresent()) {
                        JiraTestCase testCase = optionalTestCase.get();
                        
                                                 // Apply project mapping with validation
                         if (selectedProject != null) {
                             // Validate that the project exists in database
                             Optional<Project> validProject = projectRepository.findById(selectedProject.getId());
                             if (validProject.isPresent()) {
                                 testCase.setProject(validProject.get());
                                 // Set domain from project's domain
                                 if (validProject.get().getDomain() != null) {
                                     testCase.setDomainMapped(validProject.get().getDomain().getName());
                                 }
                                                           } else {
                                  logger.warn("Selected project with ID {} does not exist, using default project for test case: {}", 
                                          selectedProject.getId(), testCase.getQtestTitle());
                                  // Use default project as fallback
                                  Project defaultProject = dataInitializationService.getDefaultProject();
                                  testCase.setProject(defaultProject);
                                  if (defaultProject.getDomain() != null) {
                                      testCase.setDomainMapped(defaultProject.getDomain().getName());
                                  }
                              }
                         } else if (selectedDomain != null) {
                             // If only domain is selected, set domain mapping
                             testCase.setDomainMapped(selectedDomain.getName());
                         }
                         
                         try {
                             jiraTestCaseRepository.save(testCase);
                         } catch (Exception e) {
                             logger.error("Failed to save test case {}: {}", testCase.getQtestTitle(), e.getMessage());
                             // Continue processing other test cases
                         }
                        
                        // Update DTO
                        if (selectedProject != null) {
                            testCaseDto.setProjectId(selectedProject.getId());
                            testCaseDto.setProjectName(selectedProject.getName());
                        }
                        if (selectedDomain != null) {
                            testCaseDto.setDomainMapped(selectedDomain.getName());
                        }
                        
                        logger.debug("Applied domain/project mapping to test case: {}", testCase.getQtestTitle());
                    }
                }
            }
        }
        
        return syncedIssue;
    }

    /**
     * Fix existing test cases with invalid foreign key references
     */
    public void fixOrphanedTestCases() {
        logger.info("Checking and fixing orphaned test cases...");
        
        try {
            List<JiraTestCase> allTestCases = jiraTestCaseRepository.findAll();
            Project defaultProject = dataInitializationService.getDefaultProject();
            Tester defaultTester = dataInitializationService.getDefaultTester();
            
            int fixedCount = 0;
            
            for (JiraTestCase testCase : allTestCases) {
                boolean needsSave = false;
                
                // Check if project reference is valid
                if (testCase.getProject() != null) {
                    Optional<Project> projectCheck = projectRepository.findById(testCase.getProject().getId());
                    if (projectCheck.isEmpty()) {
                        logger.warn("Test case {} has invalid project reference, fixing with default project", 
                                testCase.getQtestTitle());
                        testCase.setProject(defaultProject);
                        testCase.setDomainMapped(defaultProject.getDomain().getName());
                        needsSave = true;
                    }
                }
                
                // Check if tester reference is valid
                if (testCase.getAssignedTester() != null) {
                    Optional<Tester> testerCheck = testerRepository.findById(testCase.getAssignedTester().getId());
                    if (testerCheck.isEmpty()) {
                        logger.warn("Test case {} has invalid tester reference, fixing with default tester", 
                                testCase.getQtestTitle());
                        testCase.setAssignedTester(defaultTester);
                        testCase.setAssignedTesterId(defaultTester.getId());
                        needsSave = true;
                    }
                }
                
                if (needsSave) {
                    try {
                        jiraTestCaseRepository.save(testCase);
                        fixedCount++;
                    } catch (Exception e) {
                        logger.error("Failed to fix test case {}: {}", testCase.getQtestTitle(), e.getMessage());
                    }
                }
            }
            
            logger.info("Fixed {} orphaned test cases", fixedCount);
            
        } catch (Exception e) {
            logger.error("Error while fixing orphaned test cases: {}", e.getMessage(), e);
        }
    }

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
     * Sync linked test cases with enhanced QTest data retrieval
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

                // Enhanced: Fetch additional QTest data
                enrichTestCaseWithQTestData(testCase, testCaseDto.getQtestTitle());

                issue.addLinkedTestCase(testCase);
            }
        }
    }

    /**
     * Enrich test case with data from QTest
     */
    private void enrichTestCaseWithQTestData(JiraTestCase testCase, String qtestTitle) {
        // Check if QTest service is available and configured
        if (!jiraConfig.isQTestConfigured()) {
            logger.debug("QTest not configured, skipping enrichment for test case: {}", qtestTitle);
            return;
        }

        try {
            // Check if QTest is authenticated before attempting to search
            if (!qTestService.isAuthenticated() && !qTestService.testConnection()) {
                logger.debug("QTest authentication not available, skipping enrichment for test case: {}", qtestTitle);
                return;
            }

            // Search for the test case in QTest by title
            List<Map<String, Object>> searchResults = qTestService.searchTestCasesByTitle(qtestTitle);
            
            if (!searchResults.isEmpty()) {
                // Get the first matching test case
                Map<String, Object> qtestData = searchResults.get(0);
                String testCaseId = (String) qtestData.get("id");
                
                if (testCaseId != null) {
                    // Fetch detailed test case information
                    Map<String, Object> detailedTestCase = qTestService.fetchTestCaseDetails(testCaseId);
                    
                    if (!detailedTestCase.isEmpty()) {
                        // Set QTest ID
                        testCase.setQtestId(testCaseId);
                        
                        // Set assignee from QTest
                        String assignee = (String) detailedTestCase.get("assignee");
                        if (assignee != null && !assignee.isEmpty()) {
                            testCase.setQtestAssignee(assignee);
                        }
                        
                        // Set priority from QTest
                        String priority = (String) detailedTestCase.get("priority");
                        if (priority != null && !priority.isEmpty()) {
                            testCase.setQtestPriority(priority);
                        }
                        
                        // Set automation status from QTest
                        String automationStatus = (String) detailedTestCase.get("automationStatus");
                        if (automationStatus != null && !automationStatus.isEmpty()) {
                            testCase.setQtestAutomationStatus(automationStatus);
                        }
                        
                        logger.debug("Enriched test case '{}' with QTest data: assignee={}, priority={}, automationStatus={}",
                                qtestTitle, assignee, priority, automationStatus);
                    }
                }
            } else {
                logger.debug("No matching QTest test case found for title: {}", qtestTitle);
            }
            
        } catch (Exception e) {
            logger.debug("Failed to enrich test case '{}' with QTest data: {}", qtestTitle, e.getMessage());
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
            
            // Use QTest priority if available, otherwise default to Medium
            String priority = (jiraTestCase.getQtestPriority() != null && !jiraTestCase.getQtestPriority().isEmpty()) 
                ? jiraTestCase.getQtestPriority() : "Medium";
            testCase.setPriority(priority);
            
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
        // Enhanced: Prioritize QA tester names over Jira assignees
        String effectiveAssignee = getEffectiveAssignee(issue);
        String effectiveAssigneeDisplayName = getEffectiveAssigneeDisplayName(issue);
        
        dto.setAssignee(effectiveAssignee);
        dto.setAssigneeDisplayName(effectiveAssigneeDisplayName);
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
        dto.setQtestAssignee(testCase.getQtestAssignee());
        dto.setQtestPriority(testCase.getQtestPriority());
        dto.setQtestAutomationStatus(testCase.getQtestAutomationStatus());
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

    /**
     * Get effective assignee prioritizing QA tester names
     */
    private String getEffectiveAssignee(JiraIssue issue) {
        // Check if any linked test case has a QTest assignee
        if (issue.getLinkedTestCases() != null && !issue.getLinkedTestCases().isEmpty()) {
            for (JiraTestCase testCase : issue.getLinkedTestCases()) {
                if (testCase.getQtestAssignee() != null && !testCase.getQtestAssignee().trim().isEmpty()) {
                    return testCase.getQtestAssignee();
                }
                // Fallback to assigned tester if available
                if (testCase.getAssignedTester() != null) {
                    return testCase.getAssignedTester().getName();
                }
            }
        }
        
        // Fallback to original Jira assignee
        return issue.getAssignee();
    }

    /**
     * Get effective assignee display name prioritizing QA tester names
     */
    private String getEffectiveAssigneeDisplayName(JiraIssue issue) {
        // Check if any linked test case has a QTest assignee
        if (issue.getLinkedTestCases() != null && !issue.getLinkedTestCases().isEmpty()) {
            for (JiraTestCase testCase : issue.getLinkedTestCases()) {
                if (testCase.getQtestAssignee() != null && !testCase.getQtestAssignee().trim().isEmpty()) {
                    return testCase.getQtestAssignee(); // QTest assignee as display name
                }
                // Fallback to assigned tester if available
                if (testCase.getAssignedTester() != null) {
                    return testCase.getAssignedTester().getName();
                }
            }
        }
        
        // Fallback to original Jira assignee display name
        return issue.getAssigneeDisplayName();
    }
}