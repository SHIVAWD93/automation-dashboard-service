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

    @Autowired
    private QTestService qTestService;

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

            String url = UriComponentsBuilder.fromPath("/rest/api/3/search")
                    .queryParam("jql", jql)
                    .queryParam("maxResults", 1000)
                    .queryParam("expand", "changelog,renderedFields,names,schema,operations,editmeta,versionedRepresentations")
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
                         boardId != null ? boardId : jiraConfig.getJiraBoardId(), startAt, maxResults);


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
        return searchKeywordGlobally(keyword, jiraProjectKey, null);
    }

    /**
     * NEW: Global keyword search across all issues in a project with optional sprint filter
     */
    public Map<String, Object> searchKeywordGlobally(String keyword, String jiraProjectKey, String sprintId) {
        if (!jiraConfig.isConfigured() || keyword == null || keyword.trim().isEmpty()) {
            return createEmptySearchResult(keyword);
        }

        try {
            // Use provided project key or fall back to default
            String projectKey = (jiraProjectKey != null && !jiraProjectKey.trim().isEmpty())
                    ? jiraProjectKey
                    : jiraConfig.getJiraProjectKey();

            // Build JQL query with optional sprint filter
            String jql;
            if (sprintId != null && !sprintId.trim().isEmpty()) {
                // Search in specific sprint
                jql = String.format("project = %s AND sprint = %s AND (summary ~ \"%s\" OR description ~ \"%s\" OR comment ~ \"%s\")",
                        projectKey, sprintId, keyword, keyword, keyword);
            } else {
                // Search in entire project
                jql = String.format("project = %s AND (summary ~ \"%s\" OR description ~ \"%s\" OR comment ~ \"%s\")",
                        projectKey, keyword, keyword, keyword);
            }

            String url = UriComponentsBuilder.fromPath("/rest/api/2/search")
                    .queryParam("jql", jql)
                    .queryParam("maxResults", 1000)
                    .queryParam("fields", "key,summary,issuetype,status,priority")
                    .build()
                    .toUriString();

            logger.info("Performing global keyword search for '{}' in project: {} sprint: {}", 
                    keyword, projectKey, sprintId != null ? sprintId : "ALL");

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
     * Parse global search response with detailed occurrence counting
     */
    private Map<String, Object> parseGlobalSearchResponse(String response, String keyword) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> matchingIssues = new ArrayList<>();
        int totalCount = 0;
        int totalOccurrences = 0;

        try {
            JsonNode rootNode = objectMapper.readTree(response);
            JsonNode issuesNode = rootNode.path("issues");
            totalCount = rootNode.path("total").asInt();

            for (JsonNode issueNode : issuesNode) {
                Map<String, Object> issue = new HashMap<>();
                String issueKey = issueNode.path("key").asText();
                issue.put("key", issueKey);

                JsonNode fields = issueNode.path("fields");
                String summary = fields.path("summary").asText();
                String description = getTextValue(fields.path("description"));
                
                issue.put("summary", summary);
                issue.put("issueType", fields.path("issuetype").path("name").asText());
                issue.put("status", fields.path("status").path("name").asText());

                JsonNode priorityNode = fields.path("priority");
                if (!priorityNode.isMissingNode() && !priorityNode.isNull()) {
                    issue.put("priority", priorityNode.path("name").asText());
                }

                // Count keyword occurrences in this issue
                int issueOccurrences = countKeywordOccurrences(summary, keyword) + 
                                      countKeywordOccurrences(description, keyword);
                
                // Add comment occurrences
                int commentOccurrences = searchKeywordInComments(issueKey, keyword);
                issueOccurrences += commentOccurrences;
                
                issue.put("occurrences", issueOccurrences);
                totalOccurrences += issueOccurrences;

                matchingIssues.add(issue);
            }

            result.put("keyword", keyword);
            result.put("totalCount", totalCount);
            result.put("totalOccurrences", totalOccurrences);
            result.put("matchingIssues", matchingIssues);
            result.put("searchDate", new Date());

            logger.info("Global search for '{}' found {} matching issues with {} total occurrences", 
                    keyword, totalCount, totalOccurrences);

        } catch (Exception e) {
            logger.error("Error parsing global search response: {}", e.getMessage(), e);
            return createEmptySearchResult(keyword);
        }

        return result;
    }

    /**
     * Count keyword occurrences in text
     */
    private int countKeywordOccurrences(String text, String keyword) {
        if (text == null || keyword == null || text.isEmpty() || keyword.isEmpty()) {
            return 0;
        }
        
        String lowerText = text.toLowerCase();
        String lowerKeyword = keyword.toLowerCase();
        int count = 0;
        int index = 0;
        
        while ((index = lowerText.indexOf(lowerKeyword, index)) != -1) {
            count++;
            index += lowerKeyword.length();
        }
        
        return count;
    }

    /**
     * Create empty search result
     */
    private Map<String, Object> createEmptySearchResult(String keyword) {
        Map<String, Object> result = new HashMap<>();
        result.put("keyword", keyword);
        result.put("totalCount", 0);
        result.put("totalOccurrences", 0);
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
            
            // Extract summary safely
            String summary = fields.path("summary").asText();
            if (summary == null || summary.isEmpty() || "null".equals(summary)) {
                logger.debug("Missing summary for issue: {}", key);
                summary = "";
            }
            issueDto.setSummary(summary);
            
            // Extract description safely
            String description = getTextValue(fields.path("description"));
            if (description == null || description.isEmpty() || "null".equals(description)) {
                logger.debug("Missing description for issue: {}", key);
                description = "";
            }
            issueDto.setDescription(description);
            
            issueDto.setSprintId(sprintId);
            
            // Extract issue type safely
            String issueType = fields.path("issuetype").path("name").asText();
            if (issueType == null || issueType.isEmpty() || "null".equals(issueType)) {
                logger.debug("Missing issue type for issue: {}", key);
                issueType = "";
            }
            issueDto.setIssueType(issueType);
            
            // Extract status safely
            String status = fields.path("status").path("name").asText();
            if (status == null || status.isEmpty() || "null".equals(status)) {
                logger.debug("Missing status for issue: {}", key);
                status = "";
            }
            issueDto.setStatus(status);

            // Get priority safely
            JsonNode priorityNode = fields.path("priority");
            if (!priorityNode.isMissingNode() && !priorityNode.isNull()) {
                String priority = priorityNode.path("name").asText();
                if (priority != null && !priority.isEmpty() && !"null".equals(priority)) {
                    issueDto.setPriority(priority);
                }
            }

            // Get assignee information safely
            JsonNode assigneeNode = fields.path("assignee");
            if (!assigneeNode.isMissingNode() && !assigneeNode.isNull()) {
                String assigneeName = assigneeNode.path("name").asText();
                String assigneeDisplayName = assigneeNode.path("displayName").asText();
                
                // Handle case where 'name' field might not exist (newer Jira versions use 'accountId')
                if ((assigneeName == null || assigneeName.isEmpty() || "null".equals(assigneeName))) {
                    assigneeName = assigneeNode.path("accountId").asText();
                }
                
                if (assigneeName != null && !assigneeName.isEmpty() && !"null".equals(assigneeName)) {
                    issueDto.setAssignee(assigneeName);
                }
                
                if (assigneeDisplayName != null && !assigneeDisplayName.isEmpty() && !"null".equals(assigneeDisplayName)) {
                    issueDto.setAssigneeDisplayName(assigneeDisplayName);
                }
            }

            // Get sprint name from sprint field - try multiple possible sprint fields
            String sprintName = extractSprintNameFromFields(fields, sprintId);
            if (sprintName != null && !sprintName.isEmpty()) {
                issueDto.setSprintName(sprintName);
            }

            // Enhanced: Fetch linked test cases from QTest instead of extracting from text patterns
            // Per requirement: Only use qTest links from Jira remote/changelog (TC- only)
            List<JiraTestCaseDto> linkedTestCases = extractQTestLinkedFromChangelog(issueNode);
            linkedTestCases = normalizeAndFilterTcOnly(linkedTestCases);
            
            issueDto.setLinkedTestCases(linkedTestCases);
            
            logger.debug("Parsed issue {}: summary='{}', status='{}', assignee='{}'", 
                key, summary, status, issueDto.getAssignee());

            return issueDto;

        } catch (Exception e) {
            logger.error("Error parsing issue node: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Extract sprint name from various possible sprint fields
     */
    private String extractSprintNameFromFields(JsonNode fields, String sprintId) {
        // Try different possible sprint field locations
        String[] sprintFields = {
            "customfield_10020", // Common sprint field
            "customfield_11051", // Sprint field from the provided example
            "sprint",
            "sprints"
        };
        
        for (String fieldName : sprintFields) {
            JsonNode sprintNode = fields.path(fieldName);
            if (!sprintNode.isMissingNode() && !sprintNode.isNull()) {
                if (sprintNode.isArray() && sprintNode.size() > 0) {
                    // Handle array of sprints
                    for (JsonNode sprint : sprintNode) {
                        String sprintName = extractSprintName(sprint.asText());
                        if (sprintName != null && !sprintName.isEmpty()) {
                            return sprintName;
                        }
                    }
                } else if (sprintNode.isTextual()) {
                    // Handle single sprint string
                    String sprintName = extractSprintName(sprintNode.asText());
                    if (sprintName != null && !sprintName.isEmpty()) {
                        return sprintName;
                    }
                } else if (sprintNode.isObject()) {
                    // Handle sprint object
                    String sprintName = sprintNode.path("name").asText();
                    if (sprintName != null && !sprintName.isEmpty() && !"null".equals(sprintName)) {
                        return sprintName;
                    }
                }
            }
        }
        
        // Fallback: try to find sprint name in changelog if available
        return null;
    }

    /**
     * Normalize qTest IDs from titles and filter to TC- only
     */
    private List<JiraTestCaseDto> normalizeAndFilterTcOnly(List<JiraTestCaseDto> input) {
        if (input == null || input.isEmpty()) return Collections.emptyList();
        List<JiraTestCaseDto> result = new ArrayList<>();
        for (JiraTestCaseDto dto : input) {
            if (dto.getQtestId() == null || dto.getQtestId().isEmpty()) {
                String parsed = parseQTestKey(dto.getQtestTitle());
                if (parsed != null) dto.setQtestId(parsed);
            }
            if (dto.getQtestId() != null && dto.getQtestId().matches("(?i)TC-\\d+")) {
                result.add(dto);
            }
        }
        return result;
    }

    /**
     * Enhanced: Fetch linked test cases from QTest for a JIRA issue
     */
    private List<JiraTestCaseDto> fetchLinkedTestCasesFromQTest(String jiraIssueKey) {
        List<JiraTestCaseDto> testCases = new ArrayList<>();
        
        try {
            // Check if QTest integration is available
            if (!jiraConfig.isQTestConfigured()) {
                logger.debug("QTest not configured, skipping QTest integration for issue: {}", jiraIssueKey);
                return testCases;
            }
            
            // Search for test cases linked to this JIRA issue
            List<Map<String, Object>> qtestLinkedCases = qTestService.searchTestCasesLinkedToJira(jiraIssueKey);
            
            for (Map<String, Object> qtestCase : qtestLinkedCases) {
                String testCaseId = (String) qtestCase.get("id");
                String testCaseName = (String) qtestCase.get("name");
                
                if (testCaseId != null && testCaseName != null) {
                    JiraTestCaseDto testCaseDto = new JiraTestCaseDto(testCaseName);
                    testCaseDto.setQtestId(testCaseId);
                    
                    // Set additional QTest properties if available
                    if (qtestCase.get("assignee") != null) {
                        testCaseDto.setQtestAssignee((String) qtestCase.get("assignee"));
                    }
                    if (qtestCase.get("assigneeDisplayName") != null) {
                        testCaseDto.setQtestAssigneeDisplayName((String) qtestCase.get("assigneeDisplayName"));
                    }
                    
                    testCases.add(testCaseDto);
                }
            }
            
            logger.info("Fetched {} linked test cases from QTest for JIRA issue: {}", 
                    testCases.size(), jiraIssueKey);
            
        } catch (Exception e) {
            logger.warn("Failed to fetch linked test cases from QTest for issue {}: {}", 
                    jiraIssueKey, e.getMessage());
        }
        
        return testCases;
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

    /**
     * Extract qTest links from issue changelog (RemoteWorkItemLink entries)
     */
    private List<JiraTestCaseDto> extractQTestLinkedFromChangelog(JsonNode issueNode) {
        List<JiraTestCaseDto> testCases = new ArrayList<>();
        try {
            JsonNode changelogNode = issueNode.path("changelog").path("histories");
            if (changelogNode.isMissingNode() || !changelogNode.isArray()) {
                return testCases;
            }

            // Get Jira issue summary for appending to TC titles
            String jiraSummary = issueNode.path("fields").path("summary").asText("");
            if (jiraSummary == null) jiraSummary = "";
            // Truncate summary if too long to keep title manageable
            if (jiraSummary.length() > 100) {
                jiraSummary = jiraSummary.substring(0, 97) + "...";
            }

            for (JsonNode history : changelogNode) {
                JsonNode items = history.path("items");
                if (items.isMissingNode() || !items.isArray()) {
                    continue;
                }
                for (JsonNode item : items) {
                    String field = item.path("field").asText("");
                    if (!"RemoteWorkItemLink".equals(field) && !"Link".equals(field)) {
                        continue;
                    }

                    String toStringVal = item.path("toString").asText("");
                    // Example: This work item links to "TC-473 (qTest)"
                    String extractedTitle = extractQTestTitleFromToString(toStringVal);
                    if (extractedTitle != null && !extractedTitle.isEmpty()) {
                        JiraTestCaseDto dto = new JiraTestCaseDto(extractedTitle);
                        // Attempt to parse qTest ID like TC-473
                        String parsedId = parseQTestKey(extractedTitle);
                        if (parsedId != null) {
                            dto.setQtestId(parsedId);
                            // Append Jira summary to make title more descriptive
                            String enhancedTitle = parsedId + " - " + jiraSummary;
                            dto.setQtestTitle(enhancedTitle);
                        }
                        testCases.add(dto);
                    }
                }
            }
        } catch (Exception e) {
            logger.debug("Failed to extract qTest links from changelog: {}", e.getMessage());
        }
        return testCases;
    }

    /**
     * Extract title from a toString like: This work item links to "TC-473 (qTest)"
     */
    private String extractQTestTitleFromToString(String toStringVal) {
        if (toStringVal == null || toStringVal.isEmpty()) {
            return null;
        }
        // Capture content inside the last quoted segment
        int firstQuote = toStringVal.indexOf('"');
        int lastQuote = toStringVal.lastIndexOf('"');
        if (firstQuote >= 0 && lastQuote > firstQuote) {
            return toStringVal.substring(firstQuote + 1, lastQuote).trim();
        }
        // Fallback: if contains (qTest), return the suffix
        if (toStringVal.toLowerCase().contains("qtest")) {
            return toStringVal.trim();
        }
        return null;
    }

    /**
     * Parse qTest key like TC-473 from text
     */
    private String parseQTestKey(String text) {
        if (text == null) return null;
        Matcher m = Pattern.compile("(TC-\\d+)", Pattern.CASE_INSENSITIVE).matcher(text);
        if (m.find()) {
            return m.group(1).toUpperCase(Locale.ROOT);
        }
        return null;
    }

    /**
     * Merge two lists of JiraTestCaseDto, de-duplicating by qtestId or qtestTitle
     */
    private List<JiraTestCaseDto> mergeLinkedTestCases(List<JiraTestCaseDto> base, List<JiraTestCaseDto> additions) {
        if (base == null || base.isEmpty()) {
            return additions == null ? new ArrayList<>() : new ArrayList<>(additions);
        }
        if (additions == null || additions.isEmpty()) {
            return base;
        }
        Map<String, JiraTestCaseDto> byKey = new LinkedHashMap<>();
        for (JiraTestCaseDto dto : base) {
            String key = (dto.getQtestId() != null && !dto.getQtestId().isEmpty())
                    ? ("ID:" + dto.getQtestId())
                    : ("TITLE:" + (dto.getQtestTitle() != null ? dto.getQtestTitle() : UUID.randomUUID().toString()));
            byKey.put(key, dto);
        }
        for (JiraTestCaseDto dto : additions) {
            String key = (dto.getQtestId() != null && !dto.getQtestId().isEmpty())
                    ? ("ID:" + dto.getQtestId())
                    : ("TITLE:" + (dto.getQtestTitle() != null ? dto.getQtestTitle() : UUID.randomUUID().toString()));
            // If exists, prefer existing; otherwise add
            byKey.putIfAbsent(key, dto);
        }
        return new ArrayList<>(byKey.values());
    }
}