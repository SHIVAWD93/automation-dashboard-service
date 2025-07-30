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
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;
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
     * ENHANCED: Fetch all issues from a specific sprint with optional project configuration
     */
    public List<JiraIssueDto> fetchIssuesFromSprint(String sprintId, String jiraProjectKey, String jiraBoardId) {
        if (!jiraConfig.isConfigured()) {
            logger.warn("Jira configuration is not complete");
            return new ArrayList<>();
        }

        try {
            // Use provided project key or fall back to default
            String projectKey = (jiraProjectKey != null && !jiraProjectKey.trim().isEmpty())
                    ? jiraProjectKey
                    : jiraConfig.getJiraProjectKey();

            String jql = String.format("sprint = %s AND project = %s", sprintId, projectKey);

            String url = UriComponentsBuilder.fromPath("/rest/api/2/search")
                    .queryParam("jql", jql)
                    .queryParam("maxResults", 1000)
                    .queryParam("expand", "changelog")
                    .build()
                    .toUriString();

            logger.info("Fetching Jira issues from sprint: {} using JQL: {} (Project: {})",
                    sprintId, jql, projectKey);
            logger.debug("Request URL: {}", url);

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
     * ENHANCED: Fetch all sprints for the configured board with optional board ID
     */
    public List<Map<String, Object>> fetchSprints(String projectKey, String boardId) {
        List<Map<String, Object>> allSprints = new ArrayList<>();
        int startAt = 0;
        int maxResults = 50; // Use Jira's default limit
        boolean hasMore = true;

        try {
            logger.info("Starting paginated fetch for board: {} (Project: {})", boardId, projectKey);

            while (hasMore) {
                String url = String.format("/rest/agile/1.0/board/%s/sprint?startAt=%d&maxResults=%d",
                         boardId, startAt, maxResults);


                logger.debug("Fetching sprints batch: startAt={}, maxResults={}", startAt, maxResults);

                // Make your existing WebClient call but with pagination parameters
                ResponseEntity<Map> response = jiraWebClient.get()
                        .uri(url)
                        .retrieve()
                        .toEntity(Map.class)
                        .block();

                if (response != null && response.getBody() != null) {
                    Map<String, Object> responseBody = response.getBody();

                    // Extract pagination info from response
                    Integer total = (Integer) responseBody.get("total");
                    Integer returnedMaxResults = (Integer) responseBody.get("maxResults");
                    Integer returnedStartAt = (Integer) responseBody.get("startAt");
                    Boolean isLast = (Boolean) responseBody.get("isLast");

                    List<Map<String, Object>> values = (List<Map<String, Object>>) responseBody.get("values");

                    if (values != null && !values.isEmpty()) {
                        allSprints.addAll(values);
                        logger.info("Batch {}: Fetched {} sprints (Total so far: {}/{})",
                                (startAt / maxResults) + 1, values.size(), allSprints.size(), total);
                    }

                    // Determine if there are more results
                    if (isLast != null) {
                        hasMore = !isLast;
                    } else {
                        // Fallback: check if we've reached the total
                        hasMore = total != null && allSprints.size() < total;
                    }

                    // Update startAt for next batch
                    if (hasMore) {
                        startAt += (returnedMaxResults != null ? returnedMaxResults : maxResults);
                    }

                } else {
                    logger.warn("Received null response from Jira API");
                    hasMore = false;
                }
            }

            logger.info("Successfully fetched all {} sprints for board: {} (Project: {})",
                    allSprints.size(), boardId, projectKey);

            return allSprints;

        } catch (Exception e) {
            logger.error("Error fetching sprints with pagination: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Original method for backward compatibility
     */
    public List<JiraIssueDto> fetchIssuesFromSprint(String sprintId) {
        return fetchIssuesFromSprint(sprintId, null, null);
    }

    /**
     * Original method for backward compatibility
     */
    public List<Map<String, Object>> fetchSprints() {
        return fetchSprints(null, null);
    }

    /**
     * NEW: Global keyword search across all issues in a project
     */
    public Map<String, Object> searchKeywordGlobally(String keyword, String jiraProjectKey) {
        if (!jiraConfig.isConfigured() || keyword == null || keyword.trim().isEmpty()) {
            return createEmptySearchResult(keyword);
        }

        try {
            // Use provided project key or fall back to default
            String projectKey = (jiraProjectKey != null && !jiraProjectKey.trim().isEmpty())
                    ? jiraProjectKey
                    : jiraConfig.getJiraProjectKey();

            // Search in issue summaries, descriptions, and comments
            String jql = String.format("project = %s AND (summary ~ \"%s\" OR description ~ \"%s\" OR comment ~ \"%s\")",
                    projectKey, keyword, keyword, keyword);

            String url = UriComponentsBuilder.fromPath("/rest/api/2/search")
                    .queryParam("jql", jql)
                    .queryParam("maxResults", 1000)
                    .queryParam("fields", "key,summary,issuetype,status,priority")
                    .build()
                    .toUriString();

            logger.info("Performing global keyword search for '{}' in project: {}", keyword, projectKey);

            String response = jiraWebClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(30))
                    .block();

            return parseGlobalSearchResponse(response, keyword);

        } catch (WebClientResponseException e) {
            logger.error("Error performing global keyword search: {} - {}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            return createEmptySearchResult(keyword);
        } catch (Exception e) {
            logger.error("Unexpected error performing global keyword search: {}", e.getMessage(), e);
            return createEmptySearchResult(keyword);
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
     * Parse global search response
     */
    private Map<String, Object> parseGlobalSearchResponse(String response, String keyword) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> matchingIssues = new ArrayList<>();
        int totalCount = 0;

        try {
            JsonNode rootNode = objectMapper.readTree(response);
            JsonNode issuesNode = rootNode.path("issues");
            totalCount = rootNode.path("total").asInt();

            for (JsonNode issueNode : issuesNode) {
                Map<String, Object> issue = new HashMap<>();
                issue.put("key", issueNode.path("key").asText());

                JsonNode fields = issueNode.path("fields");
                issue.put("summary", fields.path("summary").asText());
                issue.put("issueType", fields.path("issuetype").path("name").asText());
                issue.put("status", fields.path("status").path("name").asText());

                JsonNode priorityNode = fields.path("priority");
                if (!priorityNode.isMissingNode() && !priorityNode.isNull()) {
                    issue.put("priority", priorityNode.path("name").asText());
                }

                matchingIssues.add(issue);
            }

            result.put("keyword", keyword);
            result.put("totalCount", totalCount);
            result.put("matchingIssues", matchingIssues);
            result.put("searchDate", new Date());

            logger.info("Global search for '{}' found {} matching issues", keyword, totalCount);

        } catch (Exception e) {
            logger.error("Error parsing global search response: {}", e.getMessage(), e);
            return createEmptySearchResult(keyword);
        }

        return result;
    }

    /**
     * Create empty search result
     */
    private Map<String, Object> createEmptySearchResult(String keyword) {
        Map<String, Object> result = new HashMap<>();
        result.put("keyword", keyword);
        result.put("totalCount", 0);
        result.put("matchingIssues", new ArrayList<>());
        result.put("searchDate", new Date());
        return result;
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