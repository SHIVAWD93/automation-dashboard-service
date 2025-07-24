package com.qa.automation.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qa.automation.config.JiraConfig;
import com.qa.automation.dto.JiraIssueDto;
import com.qa.automation.dto.JiraTestCaseDto;
import com.qa.automation.model.JiraIssue;
import com.qa.automation.model.JiraTestCase;
import com.qa.automation.repository.JiraIssueRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class JiraIntegrationService {

    private static final Logger logger = LoggerFactory.getLogger(JiraIntegrationService.class);

    @Autowired
    private JiraConfig jiraConfig;

    @Autowired
    private WebClient jiraWebClient;

    @Autowired
    private JiraIssueRepository jiraIssueRepository;

    @Autowired
    private ObjectMapper objectMapper;

    // Pattern to extract QTest test case links from Jira issues
    private static final Pattern QTEST_PATTERN = Pattern.compile(
            "(?i)(?:qtest|test\\s*case)\\s*:?\\s*([\\w\\s\\-_.,()\\[\\]]+)", 
            Pattern.CASE_INSENSITIVE
    );

    /**
     * Fetch all issues from a specific sprint
     */
    public List<JiraIssueDto> fetchIssuesFromSprint(String sprintId) {
        if (!jiraConfig.isConfigured()) {
            logger.warn("Jira configuration is not complete");
            return new ArrayList<>();
        }

        try {
            String jql = String.format("sprint = %s AND project = %s", sprintId, jiraConfig.getJiraProjectKey());
            String url = String.format("/rest/api/2/search?jql=%s&maxResults=1000&expand=changelog", 
                    jql.replace(" ", "%20"));

            logger.info("Fetching Jira issues from sprint: {} using JQL: {}", sprintId, jql);

            String response = jiraWebClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(30))
                    .block();

            return parseJiraResponse(response, sprintId);

        } catch (WebClientResponseException e) {
            logger.error("Error fetching Jira issues from sprint {}: {} - {}", 
                    sprintId, e.getStatusCode(), e.getResponseBodyAsString());
            return new ArrayList<>();
        } catch (Exception e) {
            logger.error("Unexpected error fetching Jira issues from sprint {}: {}", sprintId, e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * Fetch all sprints for the configured board
     */
    public List<Map<String, Object>> fetchSprints() {
        if (!jiraConfig.isConfigured()) {
            logger.warn("Jira configuration is not complete");
            return new ArrayList<>();
        }

        try {
            String url = String.format("/rest/agile/1.0/board/%s/sprint", jiraConfig.getJiraBoardId());
            
            logger.info("Fetching sprints for board: {}", jiraConfig.getJiraBoardId());

            String response = jiraWebClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(30))
                    .block();

            return parseSprintsResponse(response);

        } catch (WebClientResponseException e) {
            logger.error("Error fetching sprints: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            return new ArrayList<>();
        } catch (Exception e) {
            logger.error("Unexpected error fetching sprints: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * Search for a keyword in issue comments and return count
     */
    public int searchKeywordInComments(String issueKey, String keyword) {
        if (!jiraConfig.isConfigured() || issueKey == null || keyword == null) {
            return 0;
        }

        try {
            String url = String.format("/rest/api/2/issue/%s/comment", issueKey);
            
            logger.debug("Searching for keyword '{}' in comments of issue: {}", keyword, issueKey);

            String response = jiraWebClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(15))
                    .block();

            return countKeywordInComments(response, keyword);

        } catch (WebClientResponseException e) {
            logger.warn("Error fetching comments for issue {}: {} - {}", 
                    issueKey, e.getStatusCode(), e.getResponseBodyAsString());
            return 0;
        } catch (Exception e) {
            logger.warn("Unexpected error fetching comments for issue {}: {}", issueKey, e.getMessage());
            return 0;
        }
    }

    /**
     * Parse Jira API response and convert to DTOs
     */
    private List<JiraIssueDto> parseJiraResponse(String response, String sprintId) {
        List<JiraIssueDto> issues = new ArrayList<>();

        try {
            JsonNode rootNode = objectMapper.readTree(response);
            JsonNode issuesNode = rootNode.path("issues");

            for (JsonNode issueNode : issuesNode) {
                JiraIssueDto issueDto = parseIssueNode(issueNode, sprintId);
                if (issueDto != null) {
                    issues.add(issueDto);
                }
            }

            logger.info("Parsed {} issues from Jira response", issues.size());

        } catch (Exception e) {
            logger.error("Error parsing Jira response: {}", e.getMessage(), e);
        }

        return issues;
    }

    /**
     * Parse individual issue node from Jira response
     */
    private JiraIssueDto parseIssueNode(JsonNode issueNode, String sprintId) {
        try {
            String key = issueNode.path("key").asText();
            JsonNode fields = issueNode.path("fields");

            JiraIssueDto issueDto = new JiraIssueDto();
            issueDto.setJiraKey(key);
            issueDto.setSummary(fields.path("summary").asText());
            issueDto.setDescription(getTextValue(fields.path("description")));
            issueDto.setSprintId(sprintId);
            issueDto.setIssueType(fields.path("issuetype").path("name").asText());
            issueDto.setStatus(fields.path("status").path("name").asText());
            
            // Get priority safely
            JsonNode priorityNode = fields.path("priority");
            if (!priorityNode.isMissingNode() && !priorityNode.isNull()) {
                issueDto.setPriority(priorityNode.path("name").asText());
            }

            // Get assignee information
            JsonNode assigneeNode = fields.path("assignee");
            if (!assigneeNode.isMissingNode() && !assigneeNode.isNull()) {
                issueDto.setAssignee(assigneeNode.path("name").asText());
                issueDto.setAssigneeDisplayName(assigneeNode.path("displayName").asText());
            }

            // Get sprint name from sprint field
            JsonNode sprintNode = fields.path("customfield_10020"); // This is typically the sprint field
            if (!sprintNode.isMissingNode() && sprintNode.isArray() && sprintNode.size() > 0) {
                String sprintName = extractSprintName(sprintNode.get(0).asText());
                issueDto.setSprintName(sprintName);
            }

            // Extract linked test cases from description and comments
            List<JiraTestCaseDto> linkedTestCases = extractLinkedTestCases(issueDto.getDescription());
            issueDto.setLinkedTestCases(linkedTestCases);

            return issueDto;

        } catch (Exception e) {
            logger.error("Error parsing issue node: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Extract sprint name from sprint string
     */
    private String extractSprintName(String sprintString) {
        try {
            // Sprint string format: "com.atlassian.greenhopper.service.sprint.Sprint@[id=123,name=Sprint 1,...]"
            Pattern namePattern = Pattern.compile("name=([^,\\]]+)");
            Matcher matcher = namePattern.matcher(sprintString);
            if (matcher.find()) {
                return matcher.group(1);
            }
        } catch (Exception e) {
            logger.debug("Could not extract sprint name from: {}", sprintString);
        }
        return "Sprint " + jiraConfig.getJiraBoardId(); // Fallback
    }

    /**
     * Extract linked test cases from text using patterns
     */
    private List<JiraTestCaseDto> extractLinkedTestCases(String text) {
        List<JiraTestCaseDto> testCases = new ArrayList<>();
        
        if (text == null || text.trim().isEmpty()) {
            return testCases;
        }

        try {
            // Look for QTest test case patterns
            Matcher matcher = QTEST_PATTERN.matcher(text);
            Set<String> foundTestCases = new HashSet<>(); // Avoid duplicates

            while (matcher.find()) {
                String testCaseTitle = matcher.group(1).trim();
                if (!testCaseTitle.isEmpty() && !foundTestCases.contains(testCaseTitle)) {
                    foundTestCases.add(testCaseTitle);
                    JiraTestCaseDto testCaseDto = new JiraTestCaseDto(testCaseTitle);
                    testCases.add(testCaseDto);
                }
            }

            // Also look for bulleted or numbered lists that might be test cases
            String[] lines = text.split("\n");
            for (String line : lines) {
                line = line.trim();
                if (line.matches("^[*\\-•]\\s+.+") || line.matches("^\\d+\\.\\s+.+")) {
                    String testCaseTitle = line.replaceFirst("^[*\\-•\\d\\.\\s]+", "").trim();
                    if (testCaseTitle.length() > 10 && testCaseTitle.length() < 200 && 
                        !foundTestCases.contains(testCaseTitle)) {
                        foundTestCases.add(testCaseTitle);
                        JiraTestCaseDto testCaseDto = new JiraTestCaseDto(testCaseTitle);
                        testCases.add(testCaseDto);
                    }
                }
            }

        } catch (Exception e) {
            logger.error("Error extracting test cases from text: {}", e.getMessage(), e);
        }

        logger.debug("Extracted {} test cases from issue text", testCases.size());
        return testCases;
    }

    /**
     * Parse sprints response from Jira Agile API
     */
    private List<Map<String, Object>> parseSprintsResponse(String response) {
        List<Map<String, Object>> sprints = new ArrayList<>();

        try {
            JsonNode rootNode = objectMapper.readTree(response);
            JsonNode valuesNode = rootNode.path("values");

            for (JsonNode sprintNode : valuesNode) {
                Map<String, Object> sprint = new HashMap<>();
                sprint.put("id", sprintNode.path("id").asText());
                sprint.put("name", sprintNode.path("name").asText());
                sprint.put("state", sprintNode.path("state").asText());
                sprint.put("startDate", sprintNode.path("startDate").asText());
                sprint.put("endDate", sprintNode.path("endDate").asText());
                
                sprints.add(sprint);
            }

            logger.info("Parsed {} sprints from Jira response", sprints.size());

        } catch (Exception e) {
            logger.error("Error parsing sprints response: {}", e.getMessage(), e);
        }

        return sprints;
    }

    /**
     * Count keyword occurrences in comments
     */
    private int countKeywordInComments(String response, String keyword) {
        int count = 0;

        try {
            JsonNode rootNode = objectMapper.readTree(response);
            JsonNode commentsNode = rootNode.path("comments");

            String lowerKeyword = keyword.toLowerCase();

            for (JsonNode commentNode : commentsNode) {
                String commentBody = getTextValue(commentNode.path("body"));
                if (commentBody != null) {
                    String lowerComment = commentBody.toLowerCase();
                    int index = 0;
                    while ((index = lowerComment.indexOf(lowerKeyword, index)) != -1) {
                        count++;
                        index += lowerKeyword.length();
                    }
                }
            }

            logger.debug("Found {} occurrences of keyword '{}' in comments", count, keyword);

        } catch (Exception e) {
            logger.error("Error counting keyword in comments: {}", e.getMessage(), e);
        }

        return count;
    }

    /**
     * Safely extract text value from JSON node (handles both string and object formats)
     */
    private String getTextValue(JsonNode node) {
        if (node.isMissingNode() || node.isNull()) {
            return "";
        }
        
        if (node.isTextual()) {
            return node.asText();
        }
        
        // Handle Atlassian Document Format (ADF)
        if (node.isObject()) {
            try {
                return extractTextFromADF(node);
            } catch (Exception e) {
                logger.debug("Could not extract text from ADF format: {}", e.getMessage());
                return node.toString();
            }
        }
        
        return node.asText();
    }

    /**
     * Extract plain text from Atlassian Document Format (ADF)
     */
    private String extractTextFromADF(JsonNode adfNode) {
        StringBuilder text = new StringBuilder();
        extractTextRecursive(adfNode, text);
        return text.toString().trim();
    }

    private void extractTextRecursive(JsonNode node, StringBuilder text) {
        if (node.has("text")) {
            text.append(node.path("text").asText()).append(" ");
        }
        
        if (node.has("content") && node.path("content").isArray()) {
            for (JsonNode child : node.path("content")) {
                extractTextRecursive(child, text);
            }
        }
    }

    /**
     * Test connection to Jira
     */
    public boolean testConnection() {
        if (!jiraConfig.isConfigured()) {
            return false;
        }

        try {
            String response = jiraWebClient.get()
                    .uri("/rest/api/2/myself")
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(10))
                    .block();

            logger.info("Jira connection test successful");
            return true;

        } catch (Exception e) {
            logger.error("Jira connection test failed: {}", e.getMessage());
            return false;
        }
    }
}