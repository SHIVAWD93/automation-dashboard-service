package com.qa.automation.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qa.automation.config.JiraConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.*;

@Service
public class QTestService {

    private static final Logger logger = LoggerFactory.getLogger(QTestService.class);

    @Autowired
    private JiraConfig jiraConfig;

    @Autowired
    private WebClient qtestWebClient;

    @Autowired
    private ObjectMapper objectMapper;

    private String accessToken;
    private long tokenExpiryTime;
    private boolean authenticationFailed = false;
    private long lastAuthFailTime = 0;

    /**
     * Login to QTest and obtain access token (supports both token and password auth)
     */
    public boolean loginToQTest() {
        if (!jiraConfig.isQTestConfigured()) {
            logger.warn("QTest configuration is incomplete");
            return false;
        }

        // If token is provided, use it directly
        if (jiraConfig.getQtestToken() != null && !jiraConfig.getQtestToken().isEmpty()) {
            logger.info("Using provided QTest token for authentication");
            this.accessToken = jiraConfig.getQtestToken();
            // Set token expiry (assume token is long-lived, check every hour)
            this.tokenExpiryTime = System.currentTimeMillis() + (60 * 60 * 1000);
            logger.info("Successfully configured QTest with provided token");
            return true;
        }

        // Fallback to username/password authentication
        try {
            Map<String, String> loginRequest = new HashMap<>();
            loginRequest.put("username", jiraConfig.getQtestUsername());
            loginRequest.put("password", jiraConfig.getQtestPassword());

            logger.info("Attempting to login to QTest for user: {}", jiraConfig.getQtestUsername());
            logger.debug("QTest URL: {}", jiraConfig.getQtestUrl());

            String response = qtestWebClient.post()
                    .uri("/api/login")
                    .bodyValue(loginRequest)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(30))
                    .block();

            JsonNode responseNode = objectMapper.readTree(response);
            this.accessToken = responseNode.path("access_token").asText();
            
            if (accessToken == null || accessToken.isEmpty()) {
                logger.error("No access token received from QTest login response");
                return false;
            }
            
            // Set token expiry (typically 1 hour, but we'll refresh every 50 minutes)
            this.tokenExpiryTime = System.currentTimeMillis() + (50 * 60 * 1000);

            logger.info("Successfully logged in to QTest using username/password");
            return true;

        } catch (WebClientResponseException e) {
            logger.error("Login to QTest failed: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            
            // Provide specific guidance for common issues
            if (e.getStatusCode().value() == 401) {
                logger.error("QTest Authentication Failed - Please check:");
                logger.error("1. Username: {}", jiraConfig.getQtestUsername());
                logger.error("2. Password/Token is correct");
                logger.error("3. QTest URL is correct: {}", jiraConfig.getQtestUrl());
                logger.error("4. Account is not locked or requires 2FA");
                logger.error("5. Consider using qtest.token instead of username/password");
            }
            return false;
        } catch (Exception e) {
            logger.error("Unexpected error during QTest login: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Check if current token is valid and refresh if needed
     */
    private boolean ensureValidToken() {
        // Don't retry authentication if it failed recently (within 5 minutes)
        if (authenticationFailed && (System.currentTimeMillis() - lastAuthFailTime) < 300000) {
            logger.debug("Skipping QTest authentication - recent failure detected");
            return false;
        }
        
        if (accessToken == null || System.currentTimeMillis() >= tokenExpiryTime) {
            boolean success = loginToQTest();
            if (!success) {
                authenticationFailed = true;
                lastAuthFailTime = System.currentTimeMillis();
            } else {
                authenticationFailed = false;
            }
            return success;
        }
        return true;
    }

    /**
     * Fetch test case details from QTest by test case ID
     */
    public Map<String, Object> fetchTestCaseDetails(String testCaseId) {
        if (!ensureValidToken()) {
            logger.error("Cannot fetch test case details - authentication failed");
            return new HashMap<>();
        }

        try {
            String url = String.format("/api/v3/projects/%s/test-cases/%s",
                    jiraConfig.getQtestProjectId(), testCaseId);

            logger.debug("Fetching QTest test case details for ID: {}", testCaseId);

            String response = WebClient.builder()
                    .baseUrl(jiraConfig.getQtestUrl())
                    .defaultHeader("Authorization", "Bearer " + accessToken)
                    .defaultHeader("Content-Type", "application/json")
                    .build()
                    .get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(30))
                    .block();

            return parseTestCaseResponse(response);

        } catch (WebClientResponseException e) {
            logger.error("Error fetching QTest test case {}: {} - {}",
                    testCaseId, e.getStatusCode(), e.getResponseBodyAsString());
            return new HashMap<>();
        } catch (Exception e) {
            logger.error("Unexpected error fetching QTest test case {}: {}", testCaseId, e.getMessage(), e);
            return new HashMap<>();
        }
    }

    /**
     * Search for test cases in QTest project by name/title
     */
    public List<Map<String, Object>> searchTestCasesByTitle(String title) {
        if (!ensureValidToken()) {
            logger.error("Cannot search test cases - authentication failed");
            return new ArrayList<>();
        }

        try {
            String url = String.format("/api/v3/projects/%s/test-cases?size=100", 
                    jiraConfig.getQtestProjectId());

            logger.debug("Searching QTest test cases by title: {}", title);

            String response = WebClient.builder()
                    .baseUrl(jiraConfig.getQtestUrl())
                    .defaultHeader("Authorization", "Bearer " + accessToken)
                    .defaultHeader("Content-Type", "application/json")
                    .build()
                    .get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(30))
                    .block();

            return filterTestCasesByTitle(response, title);

        } catch (WebClientResponseException e) {
            logger.error("Error searching QTest test cases: {} - {}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            return new ArrayList<>();
        } catch (Exception e) {
            logger.error("Unexpected error searching QTest test cases: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * Search for test cases linked to a specific JIRA issue
     */
    public List<Map<String, Object>> searchTestCasesLinkedToJira(String jiraIssueKey) {
        if (!ensureValidToken()) {
            logger.error("Cannot search linked test cases - authentication failed");
            return new ArrayList<>();
        }

        try {
            // Search for test cases that have the JIRA issue key in their links or requirements
            String url = String.format("/api/v3/projects/%s/test-cases?size=500", 
                    jiraConfig.getQtestProjectId());

            logger.debug("Searching QTest test cases linked to JIRA issue: {}", jiraIssueKey);

            String response = WebClient.builder()
                    .baseUrl(jiraConfig.getQtestUrl())
                    .defaultHeader("Authorization", "Bearer " + accessToken)
                    .defaultHeader("Content-Type", "application/json")
                    .build()
                    .get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(30))
                    .block();

            return filterTestCasesByJiraLink(response, jiraIssueKey);

        } catch (WebClientResponseException e) {
            logger.error("Error searching QTest test cases linked to JIRA {}: {} - {}",
                    jiraIssueKey, e.getStatusCode(), e.getResponseBodyAsString());
            return new ArrayList<>();
        } catch (Exception e) {
            logger.error("Unexpected error searching QTest test cases linked to JIRA {}: {}", 
                    jiraIssueKey, e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * Parse QTest test case response
     */
    private Map<String, Object> parseTestCaseResponse(String response) {
        Map<String, Object> testCase = new HashMap<>();

        try {
            JsonNode testCaseNode = objectMapper.readTree(response);

            testCase.put("id", testCaseNode.path("id").asText());
            testCase.put("name", testCaseNode.path("name").asText());
            testCase.put("description", testCaseNode.path("description").asText());

            // Extract assignee
            JsonNode assigneeNode = testCaseNode.path("assignee");
            if (!assigneeNode.isMissingNode() && !assigneeNode.isNull()) {
                testCase.put("assignee", assigneeNode.path("username").asText());
                testCase.put("assigneeDisplayName", assigneeNode.path("displayName").asText());
            }

            // Extract priority
            JsonNode priorityNode = testCaseNode.path("priority");
            if (!priorityNode.isMissingNode() && !priorityNode.isNull()) {
                testCase.put("priority", priorityNode.path("name").asText());
            }

            // Extract automation status
            JsonNode propertiesNode = testCaseNode.path("properties");
            if (propertiesNode.isArray()) {
                for (JsonNode property : propertiesNode) {
                    String fieldName = property.path("field").path("label").asText();
                    if ("Automation Status".equalsIgnoreCase(fieldName)) {
                        testCase.put("automationStatus", property.path("field_value").asText());
                        break;
                    }
                }
            }

            logger.debug("Parsed QTest test case: {}", testCase.get("name"));

        } catch (Exception e) {
            logger.error("Error parsing QTest test case response: {}", e.getMessage(), e);
        }

        return testCase;
    }

    /**
     * Filter test cases by title match
     */
    private List<Map<String, Object>> filterTestCasesByTitle(String response, String titleFilter) {
        List<Map<String, Object>> matchingTestCases = new ArrayList<>();

        try {
            JsonNode rootNode = objectMapper.readTree(response);
            JsonNode itemsNode = rootNode.path("items");

            String lowerTitleFilter = titleFilter.toLowerCase().trim();

            for (JsonNode testCaseNode : itemsNode) {
                String testCaseName = testCaseNode.path("name").asText();
                if (testCaseName.toLowerCase().contains(lowerTitleFilter)) {
                    Map<String, Object> testCase = new HashMap<>();
                    testCase.put("id", testCaseNode.path("id").asText());
                    testCase.put("name", testCaseName);
                    
                    // Add more details if needed
                    JsonNode assigneeNode = testCaseNode.path("assignee");
                    if (!assigneeNode.isMissingNode()) {
                        testCase.put("assignee", assigneeNode.path("username").asText());
                    }

                    matchingTestCases.add(testCase);
                }
            }

            logger.info("Found {} matching test cases for title filter: {}", 
                    matchingTestCases.size(), titleFilter);

        } catch (Exception e) {
            logger.error("Error filtering test cases by title: {}", e.getMessage(), e);
        }

        return matchingTestCases;
    }

    /**
     * Filter test cases by JIRA issue link
     */
    private List<Map<String, Object>> filterTestCasesByJiraLink(String response, String jiraIssueKey) {
        List<Map<String, Object>> linkedTestCases = new ArrayList<>();

        try {
            JsonNode rootNode = objectMapper.readTree(response);
            JsonNode itemsNode = rootNode.path("items");

            for (JsonNode testCaseNode : itemsNode) {
                String testCaseName = testCaseNode.path("name").asText();
                String testCaseId = testCaseNode.path("id").asText();
                
                // Check if test case has links to the JIRA issue
                boolean isLinked = false;
                
                // Check in properties for JIRA links
                JsonNode propertiesNode = testCaseNode.path("properties");
                if (propertiesNode.isArray()) {
                    for (JsonNode property : propertiesNode) {
                        String fieldName = property.path("field").path("label").asText();
                        String fieldValue = property.path("field_value").asText();
                        
                        // Check various fields that might contain JIRA references
                        if (fieldName.toLowerCase().contains("jira") || 
                            fieldName.toLowerCase().contains("defect") ||
                            fieldName.toLowerCase().contains("requirement")) {
                            if (fieldValue.contains(jiraIssueKey)) {
                                isLinked = true;
                                break;
                            }
                        }
                    }
                }
                
                // Also check in description for JIRA issue references
                String description = testCaseNode.path("description").asText();
                if (!isLinked && description.contains(jiraIssueKey)) {
                    isLinked = true;
                }
                
                if (isLinked) {
                    Map<String, Object> testCase = new HashMap<>();
                    testCase.put("id", testCaseId);
                    testCase.put("name", testCaseName);
                    testCase.put("description", description);
                    
                    // Add assignee if available
                    JsonNode assigneeNode = testCaseNode.path("assignee");
                    if (!assigneeNode.isMissingNode() && !assigneeNode.isNull()) {
                        testCase.put("assignee", assigneeNode.path("username").asText());
                        testCase.put("assigneeDisplayName", assigneeNode.path("displayName").asText());
                    }
                    
                    linkedTestCases.add(testCase);
                }
            }

            logger.info("Found {} test cases linked to JIRA issue: {}", 
                    linkedTestCases.size(), jiraIssueKey);

        } catch (Exception e) {
            logger.error("Error filtering test cases by JIRA link: {}", e.getMessage(), e);
        }

        return linkedTestCases;
    }

    /**
     * Test QTest connection and authentication
     */
    public boolean testConnection() {
        if (!jiraConfig.isQTestConfigured()) {
            logger.debug("QTest configuration incomplete - cannot test connection");
            return false;
        }
        
        // Reset authentication failure state for connection test
        authenticationFailed = false;
        return loginToQTest();
    }

    /**
     * Force retry authentication (clears failure state)
     */
    public void retryAuthentication() {
        authenticationFailed = false;
        lastAuthFailTime = 0;
        accessToken = null;
    }

    /**
     * Get current access token (for debugging)
     */
    public String getAccessToken() {
        return accessToken;
    }

    /**
     * Check if QTest is authenticated
     */
    public boolean isAuthenticated() {
        return accessToken != null && System.currentTimeMillis() < tokenExpiryTime;
    }
}