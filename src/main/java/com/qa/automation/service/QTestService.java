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

    /**
     * Login to QTest and obtain access token
     */
    public boolean loginToQTest() {
        if (!jiraConfig.isQTestConfigured()) {
            logger.warn("QTest configuration is incomplete");
            return false;
        }

        try {
            Map<String, String> loginRequest = new HashMap<>();
            loginRequest.put("username", jiraConfig.getQtestUsername());
            loginRequest.put("password", jiraConfig.getQtestPassword());

            logger.info("Attempting to login to QTest for user: {}", jiraConfig.getQtestUsername());

            String response = qtestWebClient.post()
                    .uri("/api/login")
                    .bodyValue(loginRequest)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(30))
                    .block();

            JsonNode responseNode = objectMapper.readTree(response);
            this.accessToken = responseNode.path("access_token").asText();
            
            // Set token expiry (typically 1 hour, but we'll refresh every 50 minutes)
            this.tokenExpiryTime = System.currentTimeMillis() + (50 * 60 * 1000);

            logger.info("Successfully logged in to QTest");
            return true;

        } catch (WebClientResponseException e) {
            logger.error("Login to QTest failed: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
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
        if (accessToken == null || System.currentTimeMillis() >= tokenExpiryTime) {
            return loginToQTest();
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
     * Test QTest connection and authentication
     */
    public boolean testConnection() {
        return loginToQTest();
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