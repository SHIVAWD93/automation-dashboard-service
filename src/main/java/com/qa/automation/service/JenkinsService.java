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

import java.time.LocalDateTime;
import java.time.Instant;
import java.util.*;

@Service
public class JenkinsService {

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

    public List<JenkinsResult> getAllLatestResults() {
        try {
            return jenkinsResultRepository.findLatestResultsForAllJobs();
        } catch (Exception e) {
            System.err.println("Error getting latest results: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public JenkinsResult getLatestResultByJobName(String jobName) {
        try {
            List<JenkinsResult> results = jenkinsResultRepository.findLatestByJobName(jobName);
            return results.isEmpty() ? null : results.get(0);
        } catch (Exception e) {
            System.err.println("Error getting result for job " + jobName + ": " + e.getMessage());
            return null;
        }
    }

    public List<JenkinsTestCase> getTestCasesByResultId(Long resultId) {
        try {
            return jenkinsTestCaseRepository.findByJenkinsResultId(resultId);
        } catch (Exception e) {
            System.err.println("Error getting test cases for result " + resultId + ": " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public Map<String, Object> getJenkinsStatistics() {
        Map<String, Object> stats = new HashMap<>();

        try {
            List<JenkinsResult> latestResults = jenkinsResultRepository.findLatestResultsForAllJobs();

            stats.put("totalJobs", latestResults.size());
            stats.put("successfulJobs", latestResults.stream()
                    .mapToLong(jr -> "SUCCESS".equals(jr.getBuildStatus()) ? 1 : 0).sum());
            stats.put("failedJobs", latestResults.stream()
                    .mapToLong(jr -> "FAILURE".equals(jr.getBuildStatus()) ? 1 : 0).sum());

            Long totalTests = jenkinsResultRepository.getTotalTestsFromLatestBuilds();
            Long passedTests = jenkinsResultRepository.getTotalPassedTestsFromLatestBuilds();
            Long failedTests = jenkinsResultRepository.getTotalFailedTestsFromLatestBuilds();

            stats.put("totalTests", totalTests != null ? totalTests : 0);
            stats.put("passedTests", passedTests != null ? passedTests : 0);
            stats.put("failedTests", failedTests != null ? failedTests : 0);
        } catch (Exception e) {
            System.err.println("Error getting statistics: " + e.getMessage());
            stats.put("totalJobs", 0);
            stats.put("successfulJobs", 0);
            stats.put("failedJobs", 0);
            stats.put("totalTests", 0);
            stats.put("passedTests", 0);
            stats.put("failedTests", 0);
        }

        return stats;
    }

    public void syncAllJobsFromJenkins() {
        try {
            List<String> jobNames = fetchJobNamesFromJenkins();

            for (String jobName : jobNames) {
                try {
                    syncJobResultFromJenkins(jobName);
                } catch (Exception e) {
                    System.err.println("Failed to sync job " + jobName + ": " + e.getMessage());
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to sync jobs from Jenkins: " + e.getMessage(), e);
        }
    }

    public void syncJobResultFromJenkins(String jobName) {
        try {
            JsonNode buildInfo = fetchLatestCompletedBuildInfo(jobName);
            if (buildInfo == null) {
                System.out.println("No completed builds found for job: " + jobName);
                return;
            }

            String buildNumber = buildInfo.get("number").asText();
            String buildStatus = buildInfo.get("result") != null ?
                    buildInfo.get("result").asText() : "IN_PROGRESS";

            Optional<JenkinsResult> existingResult = jenkinsResultRepository
                    .findByJobNameAndBuildNumber(jobName, buildNumber);

            if (existingResult.isPresent() &&
                    existingResult.get().getBuildStatus().equals(buildStatus)) {
                System.out.println("Build " + buildNumber + " for job " + jobName + " is already up to date");
                return;
            }

            JenkinsResult jenkinsResult = existingResult.orElse(new JenkinsResult());
            jenkinsResult.setJobName(jobName);
            jenkinsResult.setBuildNumber(buildNumber);
            jenkinsResult.setBuildStatus(buildStatus);
            jenkinsResult.setBuildUrl(buildInfo.get("url").asText());

            long timestamp = buildInfo.get("timestamp").asLong();
            jenkinsResult.setBuildTimestamp(
                    LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp),
                            java.time.ZoneId.systemDefault()));

            // Try TestNG results first for counts
            JsonNode testNGResults = fetchTestNGResults(jobName, buildNumber);
            if (testNGResults != null) {
                processTestNGResults(jenkinsResult, testNGResults);
            } else {
                JsonNode standardResults = fetchStandardTestResults(jobName, buildNumber);
                if (standardResults != null) {
                    processStandardTestResults(jenkinsResult, standardResults);
                }
            }

            JenkinsResult savedResult = jenkinsResultRepository.save(jenkinsResult);
            System.out.println("Saved Jenkins result for job: " + jobName + ", build: " + buildNumber);

            // Now fetch individual test cases using Jenkins Test Results API
            fetchAndSaveIndividualTestCases(savedResult);

        } catch (Exception e) {
            System.err.println("Failed to sync job result for " + jobName + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void fetchAndSaveIndividualTestCases(JenkinsResult jenkinsResult) {
        try {
            // Clear existing test cases
            List<JenkinsTestCase> existingTestCases = jenkinsTestCaseRepository
                    .findByJenkinsResultId(jenkinsResult.getId());
            if (!existingTestCases.isEmpty()) {
                jenkinsTestCaseRepository.deleteAll(existingTestCases);
                System.out.println("Deleted " + existingTestCases.size() + " existing test cases for job: " + jenkinsResult.getJobName());
            }

            List<JenkinsTestCase> testCases = new ArrayList<>();

            // PRIORITY 1: Try to extract from TestNG XML files (most reliable)
            testCases.addAll(testNGXMLParserService.extractTestCasesFromXMLFiles(jenkinsResult));

            if (testCases.isEmpty()) {
                System.out.println("No test cases found in XML files, trying Jenkins Test Report API...");

                // PRIORITY 2: Use Jenkins standard test results API for individual test cases
                JsonNode testReport = fetchJenkinsTestReport(jenkinsResult.getJobName(), jenkinsResult.getBuildNumber());

                if (testReport != null) {
                    System.out.println("Successfully fetched Jenkins test report for " + jenkinsResult.getJobName());
                    testCases.addAll(parseJenkinsTestReport(jenkinsResult, testReport));
                } else {
                    System.out.println("No Jenkins test report found, trying console log parsing...");

                    // PRIORITY 3: Fallback to console log parsing
                    testCases.addAll(parseTestCasesFromConsoleLog(jenkinsResult));
                }
            }

            if (!testCases.isEmpty()) {
                List<JenkinsTestCase> savedTestCases = jenkinsTestCaseRepository.saveAll(testCases);
                System.out.println("Successfully saved " + savedTestCases.size() + " test cases for job: " +
                        jenkinsResult.getJobName() + " build: " + jenkinsResult.getBuildNumber());

                // Log sample test cases
                for (int i = 0; i < Math.min(3, savedTestCases.size()); i++) {
                    JenkinsTestCase tc = savedTestCases.get(i);
                    System.out.println("Sample test case " + (i+1) + ": " + tc.getClassName() + "." + tc.getTestName() + " - " + tc.getStatus());
                }
            } else {
                System.out.println("No individual test cases could be extracted for job: " + jenkinsResult.getJobName());
                System.out.println("This may indicate that:");
                System.out.println("1. TestNG XML files are not archived as Jenkins artifacts");
                System.out.println("2. Jenkins Test Report plugin is not configured");
                System.out.println("3. Console output doesn't contain recognizable test patterns");
            }

        } catch (Exception e) {
            System.err.println("Error fetching individual test cases: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private JsonNode fetchJenkinsTestReport(String jobName, String buildNumber) {
        String url = jenkinsUrl + "/job/" + jobName + "/" + buildNumber + "/testReport/api/json";

        try {
            HttpHeaders headers = createAuthHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                JsonNode result = objectMapper.readTree(response.getBody());
                System.out.println("Jenkins test report keys: " + getJsonKeys(result));
                return result;
            } else {
                System.out.println("Jenkins test report not available (HTTP " + response.getStatusCodeValue() + ")");
                return null;
            }
        } catch (Exception e) {
            System.err.println("Failed to fetch Jenkins test report for " + jobName + " build " + buildNumber + ": " + e.getMessage());
            return null;
        }
    }

    private List<JenkinsTestCase> parseJenkinsTestReport(JenkinsResult jenkinsResult, JsonNode testReport) {
        List<JenkinsTestCase> testCases = new ArrayList<>();

        try {
            System.out.println("Parsing Jenkins test report structure...");

            // Jenkins test report structure: { "suites": [{ "cases": [...] }] }
            if (testReport.has("suites")) {
                JsonNode suites = testReport.get("suites");
                System.out.println("Found " + suites.size() + " test suites");

                for (JsonNode suite : suites) {
                    if (suite.has("cases")) {
                        JsonNode cases = suite.get("cases");
                        System.out.println("Found " + cases.size() + " test cases in suite");

                        for (JsonNode testCase : cases) {
                            JenkinsTestCase tc = createTestCaseFromJenkinsReport(jenkinsResult, testCase);
                            if (tc != null) {
                                testCases.add(tc);
                                System.out.println("Parsed test case: " + tc.getClassName() + "." + tc.getTestName() + " - " + tc.getStatus());
                            }
                        }
                    }
                }
            }

            // Alternative structure: direct cases array
            else if (testReport.has("cases")) {
                JsonNode cases = testReport.get("cases");
                System.out.println("Found " + cases.size() + " test cases directly");

                for (JsonNode testCase : cases) {
                    JenkinsTestCase tc = createTestCaseFromJenkinsReport(jenkinsResult, testCase);
                    if (tc != null) {
                        testCases.add(tc);
                    }
                }
            }

            System.out.println("Total test cases parsed from Jenkins report: " + testCases.size());

        } catch (Exception e) {
            System.err.println("Error parsing Jenkins test report: " + e.getMessage());
            e.printStackTrace();
        }

        return testCases;
    }

    private JenkinsTestCase createTestCaseFromJenkinsReport(JenkinsResult jenkinsResult, JsonNode testCase) {
        try {
            JenkinsTestCase tc = new JenkinsTestCase();
            tc.setJenkinsResult(jenkinsResult);

            // Extract class name
            String className = "Unknown";
            if (testCase.has("className")) {
                className = testCase.get("className").asText();
            }
            tc.setClassName(className);

            // Extract test name
            String testName = "Unknown";
            if (testCase.has("name")) {
                testName = testCase.get("name").asText();
            }
            tc.setTestName(testName);

            // Extract status
            String status = "UNKNOWN";
            if (testCase.has("status")) {
                String jenkinsStatus = testCase.get("status").asText();
                status = normalizeJenkinsStatus(jenkinsStatus);
            }
            tc.setStatus(status);

            // Extract duration
            if (testCase.has("duration")) {
                tc.setDuration(testCase.get("duration").asDouble());
            }

            // Extract error details
            if (testCase.has("errorDetails") && !testCase.get("errorDetails").isNull()) {
                String errorMessage = testCase.get("errorDetails").asText();
                tc.setErrorMessage(errorMessage.length() > 2000 ?
                        errorMessage.substring(0, 2000) + "..." : errorMessage);
            }

            if (testCase.has("errorStackTrace") && !testCase.get("errorStackTrace").isNull()) {
                String stackTrace = testCase.get("errorStackTrace").asText();
                tc.setStackTrace(stackTrace.length() > 5000 ?
                        stackTrace.substring(0, 5000) + "..." : stackTrace);
            }

            return tc;

        } catch (Exception e) {
            System.err.println("Error creating test case from Jenkins report: " + e.getMessage());
            return null;
        }
    }

    private List<JenkinsTestCase> parseTestCasesFromConsoleLog(JenkinsResult jenkinsResult) {
        List<JenkinsTestCase> testCases = new ArrayList<>();

        try {
            String consoleUrl = jenkinsUrl + "/job/" + jenkinsResult.getJobName() + "/" +
                    jenkinsResult.getBuildNumber() + "/consoleText";

            HttpHeaders headers = createAuthHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    consoleUrl, HttpMethod.GET, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                String consoleLog = response.getBody();
                System.out.println("Parsing console log for test results...");

                String[] lines = consoleLog.split("\n");
                for (String line : lines) {
                    // Look for TestNG result patterns
                    if (line.contains("testcases.") && (line.contains("PASSED") || line.contains("FAILED") || line.contains("SKIPPED"))) {
                        JenkinsTestCase testCase = parseTestCaseFromLogLine(jenkinsResult, line);
                        if (testCase != null) {
                            testCases.add(testCase);
                        }
                    }
                }

                System.out.println("Extracted " + testCases.size() + " test cases from console log");
            }

        } catch (Exception e) {
            System.err.println("Error parsing console log: " + e.getMessage());
        }

        return testCases;
    }

    private JenkinsTestCase parseTestCaseFromLogLine(JenkinsResult jenkinsResult, String line) {
        try {
            JenkinsTestCase testCase = new JenkinsTestCase();
            testCase.setJenkinsResult(jenkinsResult);

            // Parse patterns like: "testcases.AccountReceivableIT.methodName ... PASSED"
            String status = "UNKNOWN";
            if (line.contains("PASSED")) {
                status = "PASSED";
            } else if (line.contains("FAILED")) {
                status = "FAILED";
            } else if (line.contains("SKIPPED")) {
                status = "SKIPPED";
            }
            testCase.setStatus(status);

            // Extract class and method name
            if (line.contains("testcases.")) {
                int start = line.indexOf("testcases.");
                String testInfo = line.substring(start);

                // Find the end of the test identifier
                String[] parts = testInfo.split("\\s+");
                if (parts.length > 0) {
                    String fullTestName = parts[0];
                    int lastDot = fullTestName.lastIndexOf(".");
                    if (lastDot > 0) {
                        testCase.setClassName(fullTestName.substring(0, lastDot));
                        testCase.setTestName(fullTestName.substring(lastDot + 1));
                    } else {
                        testCase.setClassName("testcases.AccountReceivableIT");
                        testCase.setTestName(fullTestName);
                    }
                }
            }

            return testCase;

        } catch (Exception e) {
            System.err.println("Error parsing test case from log line: " + e.getMessage());
            return null;
        }
    }

    private String normalizeJenkinsStatus(String status) {
        if (status == null) return "UNKNOWN";

        switch (status.toUpperCase()) {
            case "PASSED":
                return "PASSED";
            case "FAILED":
                return "FAILED";
            case "SKIPPED":
                return "SKIPPED";
            default:
                return status.toUpperCase();
        }
    }

    private List<String> getJsonKeys(JsonNode node) {
        List<String> keys = new ArrayList<>();
        if (node != null && node.isObject()) {
            node.fieldNames().forEachRemaining(keys::add);
        }
        return keys;
    }

    private List<String> fetchJobNamesFromJenkins() {
        String url = jenkinsUrl + "/api/json?tree=jobs[name]";

        try {
            HttpHeaders headers = createAuthHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, String.class);

            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode jobs = root.get("jobs");

            List<String> jobNames = new ArrayList<>();
            if (jobs.isArray()) {
                for (JsonNode job : jobs) {
                    jobNames.add(job.get("name").asText());
                }
            }

            System.out.println("Found " + jobNames.size() + " jobs in Jenkins");
            return jobNames;
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch job names from Jenkins", e);
        }
    }

    private JsonNode fetchLatestCompletedBuildInfo(String jobName) {
        String url = jenkinsUrl + "/job/" + jobName + "/api/json";

        try {
            HttpHeaders headers = createAuthHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, String.class);

            JsonNode jobInfo = objectMapper.readTree(response.getBody());

            JsonNode lastCompletedBuild = jobInfo.get("lastCompletedBuild");
            if (lastCompletedBuild == null || lastCompletedBuild.isNull()) {
                return null;
            }

            int buildNumber = lastCompletedBuild.get("number").asInt();

            String buildUrl = jenkinsUrl + "/job/" + jobName + "/" + buildNumber + "/api/json";
            ResponseEntity<String> buildResponse = restTemplate.exchange(
                    buildUrl, HttpMethod.GET, entity, String.class);

            return objectMapper.readTree(buildResponse.getBody());
        } catch (Exception e) {
            System.err.println("Failed to fetch build info for job: " + jobName + " - " + e.getMessage());
            return null;
        }
    }

    private JsonNode fetchTestNGResults(String jobName, String buildNumber) {
        String url = jenkinsUrl + "/job/" + jobName + "/" + buildNumber + "/testngreports/api/json";

        try {
            HttpHeaders headers = createAuthHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, String.class);

            JsonNode result = objectMapper.readTree(response.getBody());
            System.out.println("Successfully fetched TestNG results for " + jobName + " build " + buildNumber);
            return result;
        } catch (Exception e) {
            System.err.println("No TestNG results available for job: " + jobName +
                    " build: " + buildNumber + " - " + e.getMessage());
            return null;
        }
    }

    private JsonNode fetchStandardTestResults(String jobName, String buildNumber) {
        String url = jenkinsUrl + "/job/" + jobName + "/" + buildNumber + "/testReport/api/json";

        try {
            HttpHeaders headers = createAuthHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, String.class);

            return objectMapper.readTree(response.getBody());
        } catch (Exception e) {
            System.err.println("No standard test results available for job: " + jobName +
                    " build: " + buildNumber);
            return null;
        }
    }

    private void processTestNGResults(JenkinsResult jenkinsResult, JsonNode testResults) {
        try {
            int failCount = testResults.has("failCount") ? testResults.get("failCount").asInt() : 0;
            int skipCount = testResults.has("skipCount") ? testResults.get("skipCount").asInt() : 0;
            int totalCount = testResults.has("total") ? testResults.get("total").asInt() : 0;

            int passCount = totalCount - failCount - skipCount;

            jenkinsResult.setTotalTests(totalCount);
            jenkinsResult.setPassedTests(Math.max(0, passCount));
            jenkinsResult.setFailedTests(failCount);
            jenkinsResult.setSkippedTests(skipCount);

            System.out.println("Processed TestNG results - Total: " + totalCount +
                    ", Passed: " + passCount + ", Failed: " + failCount + ", Skipped: " + skipCount);
        } catch (Exception e) {
            System.err.println("Error processing TestNG results: " + e.getMessage());
        }
    }

    private void processStandardTestResults(JenkinsResult jenkinsResult, JsonNode testResults) {
        try {
            if (testResults.has("passCount")) {
                jenkinsResult.setPassedTests(testResults.get("passCount").asInt());
            }
            if (testResults.has("failCount")) {
                jenkinsResult.setFailedTests(testResults.get("failCount").asInt());
            }
            if (testResults.has("skipCount")) {
                jenkinsResult.setSkippedTests(testResults.get("skipCount").asInt());
            }

            int total = (jenkinsResult.getPassedTests() != null ? jenkinsResult.getPassedTests() : 0) +
                    (jenkinsResult.getFailedTests() != null ? jenkinsResult.getFailedTests() : 0) +
                    (jenkinsResult.getSkippedTests() != null ? jenkinsResult.getSkippedTests() : 0);
            jenkinsResult.setTotalTests(total);
        } catch (Exception e) {
            System.err.println("Error processing standard test results: " + e.getMessage());
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

    public boolean testJenkinsConnection() {
        try {
            String url = jenkinsUrl + "/api/json";
            HttpHeaders headers = createAuthHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, String.class);

            boolean connected = response.getStatusCode().is2xxSuccessful();
            System.out.println("Jenkins connection test: " + (connected ? "SUCCESS" : "FAILED"));
            return connected;
        } catch (Exception e) {
            System.err.println("Jenkins connection test failed: " + e.getMessage());
            return false;
        }
    }
}