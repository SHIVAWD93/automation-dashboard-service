package com.qa.automation.service;

import com.qa.automation.model.JenkinsResult;
import com.qa.automation.model.JenkinsTestCase;
import com.qa.automation.repository.JenkinsResultRepository;
import com.qa.automation.repository.JenkinsTestCaseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.Instant;
import java.util.*;

@Service
public class JenkinsTestNGService {

    @Autowired
    private JenkinsResultRepository jenkinsResultRepository;

    @Autowired
    private JenkinsTestCaseRepository jenkinsTestCaseRepository;

    @Autowired
    private TestNGXMLParserService testNGXMLParserService;

    @Value("${jenkins.url:}")
    private String jenkinsUrl;

    @Value("${jenkins.username:}")
    private String jenkinsUsername;

    @Value("${jenkins.token:}")
    private String jenkinsToken;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Generates a comprehensive report similar to your JenkinsTestNGResultReport class
     */
    public Map<String, Object> generateTestNGReport() {
        Map<String, Object> report = new HashMap<>();
        List<Map<String, Object>> jobReports = new ArrayList<>();

        try {
            JsonNode jobs = fetchJobs();
            if (jobs == null) {
                report.put("error", "Failed to fetch jobs from Jenkins");
                return report;
            }

            for (JsonNode job : jobs) {
                String jobName = job.get("name").asText();
                Map<String, Object> jobReport = processJobReport(jobName);
                if (jobReport != null) {
                    jobReports.add(jobReport);
                }
            }

            report.put("totalJobs", jobReports.size());
            report.put("jobReports", jobReports);
            report.put("generatedAt", LocalDateTime.now().toString());

            int totalTests = 0;
            int totalPassed = 0;
            int totalFailed = 0;
            int totalSkipped = 0;

            for (Map<String, Object> jr : jobReports) {
                totalTests += getIntValue(jr.get("totalTestCases"));
                totalPassed += getIntValue(jr.get("passCount"));
                totalFailed += getIntValue(jr.get("failCount"));
                totalSkipped += getIntValue(jr.get("skipCount"));
            }

            report.put("summary", Map.of(
                    "totalTests", totalTests,
                    "totalPassed", totalPassed,
                    "totalFailed", totalFailed,
                    "totalSkipped", totalSkipped
            ));

        } catch (Exception e) {
            report.put("error", "Failed to generate TestNG report: " + e.getMessage());
            e.printStackTrace();
        }

        return report;
    }

    private int getIntValue(Object value) {
        if (value instanceof Integer) {
            return (Integer) value;
        } else if (value instanceof String && !"N/A".equals(value)) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }

    private Map<String, Object> processJobReport(String jobName) {
        try {
            JsonNode latestBuild = fetchLatestBuildDetails(jobName);
            if (latestBuild == null) {
                return Map.of(
                        "jobName", jobName,
                        "buildNumber", "No Build Found",
                        "buildStatus", "UNKNOWN",
                        "totalTestCases", "N/A",
                        "passCount", "N/A",
                        "failCount", "N/A",
                        "skipCount", "N/A",
                        "executionDate", "N/A");
            }

            int latestBuildNumber = latestBuild.get("number").asInt();
            String executionDate = formatTimestamp(latestBuild.get("timestamp").asLong());
            String buildStatus = latestBuild.has("result") && !latestBuild.get("result").isNull() ?
                    latestBuild.get("result").asText() : "IN_PROGRESS";

            JsonNode testResults = fetchTestNGResults(jobName, latestBuildNumber);
            Map<String, Object> jobReport = new HashMap<>();
            jobReport.put("jobName", jobName);
            jobReport.put("buildNumber", latestBuildNumber);
            jobReport.put("buildStatus", buildStatus);
            jobReport.put("executionDate", executionDate);

            if (testResults != null) {
                int failCount = testResults.has("failCount") ? testResults.get("failCount").asInt() : 0;
                int skipCount = testResults.has("skipCount") ? testResults.get("skipCount").asInt() : 0;
                int totalCount = testResults.has("total") ? testResults.get("total").asInt() : 0;
                int passCount = totalCount - failCount - skipCount;

                jobReport.put("totalTestCases", totalCount);
                jobReport.put("passCount", Math.max(0, passCount));
                jobReport.put("failCount", failCount);
                jobReport.put("skipCount", skipCount);

                updateDatabaseRecord(jobName, latestBuildNumber, buildStatus,
                        totalCount, passCount, failCount, skipCount, latestBuild);
            } else {
                jobReport.put("totalTestCases", "N/A");
                jobReport.put("passCount", "N/A");
                jobReport.put("failCount", "N/A");
                jobReport.put("skipCount", "N/A");
            }

            return jobReport;
        } catch (Exception e) {
            System.err.println("Error processing job report for " + jobName + ": " + e.getMessage());
            return null;
        }
    }

    private void updateDatabaseRecord(String jobName, int buildNumber, String buildStatus,
                                      int totalTests, int passCount, int failCount, int skipCount,
                                      JsonNode buildInfo) {
        try {
            Optional<JenkinsResult> existingResult = jenkinsResultRepository
                    .findByJobNameAndBuildNumber(jobName, String.valueOf(buildNumber));

            JenkinsResult jenkinsResult = existingResult.orElse(new JenkinsResult());
            jenkinsResult.setJobName(jobName);
            jenkinsResult.setBuildNumber(String.valueOf(buildNumber));
            jenkinsResult.setBuildStatus(buildStatus);
            jenkinsResult.setTotalTests(totalTests);
            jenkinsResult.setPassedTests(passCount);
            jenkinsResult.setFailedTests(failCount);
            jenkinsResult.setSkippedTests(skipCount);

            if (buildInfo.has("url")) {
                jenkinsResult.setBuildUrl(buildInfo.get("url").asText());
            }

            if (buildInfo.has("timestamp")) {
                long timestamp = buildInfo.get("timestamp").asLong();
                jenkinsResult.setBuildTimestamp(
                        LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp),
                                java.time.ZoneId.systemDefault()));
            }

            JenkinsResult savedResult = jenkinsResultRepository.save(jenkinsResult);
            System.out.println("Updated database record for " + jobName + " build " + buildNumber +
                    " - ID: " + savedResult.getId());

        } catch (Exception e) {
            System.err.println("Error updating database record for " + jobName + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Get detailed test cases for a specific job and build - Enhanced version that uses Jenkins Test Report API
     */
    public Map<String, Object> getDetailedTestCases(String jobName, String buildNumber) {
        Map<String, Object> result = new HashMap<>();

        try {
            // First try to get from database
            Optional<JenkinsResult> jenkinsResult = jenkinsResultRepository
                    .findByJobNameAndBuildNumber(jobName, buildNumber);

            if (jenkinsResult.isPresent()) {
                List<JenkinsTestCase> testCases = jenkinsTestCaseRepository
                        .findByJenkinsResultId(jenkinsResult.get().getId());

                if (!testCases.isEmpty()) {
                    // Return database results
                    result.put("jobName", jobName);
                    result.put("buildNumber", buildNumber);
                    result.put("testCases", convertToDetailedFormat(testCases));
                    result.put("totalCount", testCases.size());
                    result.put("passedCount", testCases.stream().filter(tc -> "PASSED".equals(tc.getStatus())).count());
                    result.put("failedCount", testCases.stream().filter(tc -> "FAILED".equals(tc.getStatus())).count());
                    result.put("skippedCount", testCases.stream().filter(tc -> "SKIPPED".equals(tc.getStatus())).count());
                    return result;
                }
            }

            // If no database results, fetch fresh from Jenkins using multiple approaches
            List<Map<String, Object>> testCases = new ArrayList<>();

            // PRIORITY 1: Try TestNG XML files (most reliable)
            if (jenkinsResult.isPresent()) {
                List<JenkinsTestCase> xmlTestCases = testNGXMLParserService.extractTestCasesFromXMLFiles(jenkinsResult.get());
                if (!xmlTestCases.isEmpty()) {
                    testCases.addAll(convertToDetailedFormat(xmlTestCases));
                    System.out.println("Successfully extracted " + testCases.size() + " test cases from XML files");
                }
            }

            // PRIORITY 2: Try Jenkins Test Report API if no XML results
            if (testCases.isEmpty()) {
                JsonNode testReport = fetchJenkinsTestReport(jobName, buildNumber);
                if (testReport != null) {
                    testCases.addAll(parseJenkinsTestReportForAPI(testReport));
                    System.out.println("Successfully extracted " + testCases.size() + " test cases from Jenkins Test Report API");
                }
            }

            // PRIORITY 3: Try console log parsing if still no results
            if (testCases.isEmpty()) {
                testCases.addAll(parseTestCasesFromConsoleLogForAPI(jobName, buildNumber));
                System.out.println("Extracted " + testCases.size() + " test cases from console log");
            }

            result.put("jobName", jobName);
            result.put("buildNumber", buildNumber);
            result.put("testCases", testCases);
            result.put("totalCount", testCases.size());

            // Calculate counts by status
            long passedCount = testCases.stream().filter(tc -> "PASSED".equals(tc.get("status"))).count();
            long failedCount = testCases.stream().filter(tc -> "FAILED".equals(tc.get("status"))).count();
            long skippedCount = testCases.stream().filter(tc -> "SKIPPED".equals(tc.get("status"))).count();

            result.put("passedCount", passedCount);
            result.put("failedCount", failedCount);
            result.put("skippedCount", skippedCount);

        } catch (Exception e) {
            result.put("error", "Error fetching detailed test cases: " + e.getMessage());
            e.printStackTrace();
        }

        return result;
    }

    private JsonNode fetchJenkinsTestReport(String jobName, String buildNumber) {
        String url = jenkinsUrl + "/job/" + jobName + "/" + buildNumber + "/testReport/api/json";

        try {
            HttpHeaders headers = createAuthHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                return objectMapper.readTree(response.getBody());
            }
        } catch (Exception e) {
            System.err.println("Failed to fetch Jenkins test report: " + e.getMessage());
        }
        return null;
    }

    private List<Map<String, Object>> parseJenkinsTestReportForAPI(JsonNode testReport) {
        List<Map<String, Object>> testCases = new ArrayList<>();

        try {
            if (testReport.has("suites")) {
                JsonNode suites = testReport.get("suites");
                for (JsonNode suite : suites) {
                    if (suite.has("cases")) {
                        JsonNode cases = suite.get("cases");
                        for (JsonNode testCase : cases) {
                            Map<String, Object> tcMap = new HashMap<>();

                            tcMap.put("className", testCase.has("className") ? testCase.get("className").asText() : "Unknown");
                            tcMap.put("testName", testCase.has("name") ? testCase.get("name").asText() : "Unknown");

                            String status = "UNKNOWN";
                            if (testCase.has("status")) {
                                status = normalizeTestNGStatus(testCase.get("status").asText());
                            }
                            tcMap.put("status", status);

                            if (testCase.has("duration")) {
                                tcMap.put("duration", testCase.get("duration").asDouble());
                            }

                            if (testCase.has("errorDetails") && !testCase.get("errorDetails").isNull()) {
                                tcMap.put("errorMessage", testCase.get("errorDetails").asText());
                            }

                            if (testCase.has("errorStackTrace") && !testCase.get("errorStackTrace").isNull()) {
                                tcMap.put("stackTrace", testCase.get("errorStackTrace").asText());
                            }

                            testCases.add(tcMap);
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error parsing Jenkins test report: " + e.getMessage());
        }

        return testCases;
    }

    private List<Map<String, Object>> parseTestCasesFromConsoleLogForAPI(String jobName, String buildNumber) {
        List<Map<String, Object>> testCases = new ArrayList<>();

        try {
            String consoleUrl = jenkinsUrl + "/job/" + jobName + "/" + buildNumber + "/consoleText";

            HttpHeaders headers = createAuthHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    consoleUrl, HttpMethod.GET, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                String consoleLog = response.getBody();
                String[] lines = consoleLog.split("\n");

                for (String line : lines) {
                    // Look for TestNG result patterns
                    if (line.contains("testcases.") && (line.contains("PASSED") || line.contains("FAILED") || line.contains("SKIPPED"))) {
                        Map<String, Object> testCase = parseTestCaseFromLogLineForAPI(line);
                        if (testCase != null) {
                            testCases.add(testCase);
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error parsing console log: " + e.getMessage());
        }

        return testCases;
    }

    private Map<String, Object> parseTestCaseFromLogLineForAPI(String line) {
        try {
            Map<String, Object> testCase = new HashMap<>();

            // Determine status
            String status = "UNKNOWN";
            if (line.contains("PASSED")) {
                status = "PASSED";
            } else if (line.contains("FAILED")) {
                status = "FAILED";
            } else if (line.contains("SKIPPED")) {
                status = "SKIPPED";
            }
            testCase.put("status", status);

            // Extract class and method name from patterns like "testcases.AccountReceivableIT.methodName"
            if (line.contains("testcases.")) {
                int start = line.indexOf("testcases.");
                String testInfo = line.substring(start);

                String[] parts = testInfo.split("\\s+");
                if (parts.length > 0) {
                    String fullTestName = parts[0];
                    int lastDot = fullTestName.lastIndexOf(".");
                    if (lastDot > 0) {
                        testCase.put("className", fullTestName.substring(0, lastDot));
                        testCase.put("testName", fullTestName.substring(lastDot + 1));
                    } else {
                        testCase.put("className", "testcases.AccountReceivableIT");
                        testCase.put("testName", fullTestName);
                    }
                }
            }

            return testCase;
        } catch (Exception e) {
            System.err.println("Error parsing test case from log line: " + e.getMessage());
            return null;
        }
    }

    private List<Map<String, Object>> convertToDetailedFormat(List<JenkinsTestCase> testCases) {
        List<Map<String, Object>> result = new ArrayList<>();

        for (JenkinsTestCase testCase : testCases) {
            Map<String, Object> tcMap = new HashMap<>();
            tcMap.put("className", testCase.getClassName());
            tcMap.put("testName", testCase.getTestName());
            tcMap.put("status", testCase.getStatus());
            tcMap.put("duration", testCase.getDuration());
            if (testCase.getErrorMessage() != null) {
                tcMap.put("errorMessage", testCase.getErrorMessage());
            }
            if (testCase.getStackTrace() != null) {
                tcMap.put("stackTrace", testCase.getStackTrace());
            }
            result.add(tcMap);
        }

        return result;
    }

    private String normalizeTestNGStatus(String status) {
        if (status == null) return "UNKNOWN";

        switch (status.toUpperCase()) {
            case "PASS":
            case "PASSED":
                return "PASSED";
            case "FAIL":
            case "FAILED":
                return "FAILED";
            case "SKIP":
            case "SKIPPED":
                return "SKIPPED";
            default:
                return status.toUpperCase();
        }
    }

    // Helper methods for Jenkins API calls
    private JsonNode fetchJobs() throws Exception {
        String url = jenkinsUrl + "/api/json";
        JsonNode response = sendGetRequest(url);
        return response != null ? response.get("jobs") : null;
    }

    private JsonNode fetchLatestBuildDetails(String jobName) throws Exception {
        String url = jenkinsUrl + "/job/" + jobName + "/api/json";
        JsonNode response = sendGetRequest(url);

        if (response != null && response.has("lastCompletedBuild") &&
                !response.get("lastCompletedBuild").isNull()) {
            int buildNumber = response.get("lastCompletedBuild").get("number").asInt();
            return sendGetRequest(jenkinsUrl + "/job/" + jobName + "/" + buildNumber + "/api/json");
        }
        return null;
    }

    private JsonNode fetchTestNGResults(String jobName, int buildNumber) throws Exception {
        String url = jenkinsUrl + "/job/" + jobName + "/" + buildNumber + "/testngreports/api/json";
        return sendGetRequest(url);
    }

    private JsonNode sendGetRequest(String urlString) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Authorization", "Basic " + encodeCredentials());

        int responseCode = connection.getResponseCode();
        if (responseCode == 200) {
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String inputLine;
            StringBuilder response = new StringBuilder();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            return objectMapper.readTree(response.toString());
        } else {
            System.err.println("Failed to fetch data from: " + urlString + ". Response code: " + responseCode);
            return null;
        }
    }

    private HttpHeaders createAuthHeaders() {
        HttpHeaders headers = new HttpHeaders();
        if (jenkinsUsername != null && !jenkinsUsername.isEmpty() &&
                jenkinsToken != null && !jenkinsToken.isEmpty()) {
            String auth = jenkinsUsername + ":" + jenkinsToken;
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
            headers.set("Authorization", "Basic " + encodedAuth);
        }
        return headers;
    }

    private String encodeCredentials() {
        String credentials = jenkinsUsername + ":" + jenkinsToken;
        return Base64.getEncoder().encodeToString(credentials.getBytes());
    }

    private String formatTimestamp(long timestamp) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp),
                java.time.ZoneId.systemDefault()).toString();
    }
}