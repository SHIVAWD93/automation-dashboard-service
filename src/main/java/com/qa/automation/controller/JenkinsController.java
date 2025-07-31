package com.qa.automation.controller;

import com.qa.automation.model.JenkinsResult;
import com.qa.automation.model.JenkinsTestCase;
import com.qa.automation.repository.JenkinsResultRepository;
import com.qa.automation.service.JenkinsService;
import com.qa.automation.service.JenkinsTestNGService;
import com.qa.automation.service.TestNGXMLParserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/jenkins")
@CrossOrigin(origins = "*")
public class JenkinsController {

    @Autowired
    private JenkinsService jenkinsService;

    @Autowired
    private JenkinsTestNGService jenkinsTestNGService;

    @Autowired
    private TestNGXMLParserService testNGXMLParserService;

    @Autowired
    private JenkinsResultRepository jenkinsResultRepository;

    @GetMapping("/test-connection")
    public ResponseEntity<Map<String, Object>> testJenkinsConnection() {
        try {
            boolean connected = jenkinsService.testJenkinsConnection();
            Map<String, Object> response = new HashMap<>();
            response.put("connected", connected);
            response.put("message", connected ? "Successfully connected to Jenkins" : "Failed to connect to Jenkins");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("connected", false);
            response.put("message", "Error testing connection: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/results")
    public ResponseEntity<List<JenkinsResult>> getAllLatestResults() {
        try {
            List<JenkinsResult> results = jenkinsService.getAllLatestResults();
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/results/{jobName}")
    public ResponseEntity<JenkinsResult> getLatestResultByJobName(@PathVariable String jobName) {
        try {
            JenkinsResult result = jenkinsService.getLatestResultByJobName(jobName);
            if (result != null) {
                return ResponseEntity.ok(result);
            }
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/results/{resultId}/testcases")
    public ResponseEntity<List<JenkinsTestCase>> getTestCasesByResultId(@PathVariable Long resultId) {
        try {
            List<JenkinsTestCase> testCases = jenkinsService.getTestCasesByResultId(resultId);
            return ResponseEntity.ok(testCases);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getJenkinsStatistics() {
        try {
            Map<String, Object> stats = jenkinsService.getJenkinsStatistics();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/sync")
    public ResponseEntity<Map<String, String>> syncAllJobs() {
        try {
            jenkinsService.syncAllJobsFromJenkins();
            Map<String, String> response = new HashMap<>();
            response.put("message", "Jenkins jobs synced successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "Failed to sync Jenkins jobs: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/sync/{jobName}")
    public ResponseEntity<Map<String, String>> syncJobResult(@PathVariable String jobName) {
        try {
            jenkinsService.syncJobResultFromJenkins(jobName);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Job " + jobName + " synced successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "Failed to sync job " + jobName + ": " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // TestNG specific endpoints
    @GetMapping("/testng/report")
    public ResponseEntity<Map<String, Object>> generateTestNGReport() {
        try {
            Map<String, Object> report = jenkinsTestNGService.generateTestNGReport();
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("error", "Failed to generate TestNG report: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/testng/{jobName}/{buildNumber}/testcases")
    public ResponseEntity<Map<String, Object>> getDetailedTestCases(
            @PathVariable String jobName,
            @PathVariable String buildNumber) {
        try {
            Map<String, Object> testCases = jenkinsTestNGService.getDetailedTestCases(jobName, buildNumber);
            return ResponseEntity.ok(testCases);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("error", "Failed to get detailed test cases: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/testng/sync-and-report")
    public ResponseEntity<Map<String, Object>> syncAndGenerateReport() {
        try {
            // First sync all jobs
            jenkinsService.syncAllJobsFromJenkins();

            // Then generate the TestNG report
            Map<String, Object> report = jenkinsTestNGService.generateTestNGReport();

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Sync completed and report generated successfully");
            response.put("report", report);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("error", "Failed to sync and generate report: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // Manual test case extraction endpoint
    @PostMapping("/extract-testcases/{jobName}/{buildNumber}")
    public ResponseEntity<Map<String, Object>> extractTestCasesForBuild(
            @PathVariable String jobName,
            @PathVariable String buildNumber) {
        try {
            // Force sync the specific job to extract test cases
            jenkinsService.syncJobResultFromJenkins(jobName);

            // Get the detailed test cases
            Map<String, Object> testCases = jenkinsTestNGService.getDetailedTestCases(jobName, buildNumber);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Test cases extracted successfully for " + jobName + " build " + buildNumber);
            response.put("result", testCases);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("error", "Failed to extract test cases: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // Debug endpoints
    @GetMapping("/debug/testng/{jobName}/{buildNumber}")
    public ResponseEntity<Map<String, Object>> debugTestNGApi(
            @PathVariable String jobName,
            @PathVariable String buildNumber) {
        try {
            Map<String, Object> debugInfo = new HashMap<>();

            // Test TestNG API endpoint
            String testngUrl = "https://jenkinsautoqa.winwholesale.com/jenkins/job/" + jobName + "/" + buildNumber + "/testngreports/api/json";
            debugInfo.put("testngUrl", testngUrl);

            // Test standard test report API endpoint
            String standardUrl = "https://jenkinsautoqa.winwholesale.com/jenkins/job/" + jobName + "/" + buildNumber + "/testReport/api/json";
            debugInfo.put("standardUrl", standardUrl);

            // Try to fetch detailed test cases
            Map<String, Object> testCases = jenkinsTestNGService.getDetailedTestCases(jobName, buildNumber);
            debugInfo.put("testCasesResult", testCases);

            return ResponseEntity.ok(debugInfo);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("error", "Debug failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // Simple test endpoint to verify the fix
    @GetMapping("/test-fix/{jobName}/{buildNumber}")
    public ResponseEntity<Map<String, Object>> testFix(
            @PathVariable String jobName,
            @PathVariable String buildNumber) {
        try {
            Map<String, Object> result = new HashMap<>();

            // Test the enhanced getDetailedTestCases method
            Map<String, Object> testCases = jenkinsTestNGService.getDetailedTestCases(jobName, buildNumber);

            result.put("success", true);
            result.put("testCaseCount", testCases.get("totalCount"));
            result.put("passedCount", testCases.get("passedCount"));
            result.put("failedCount", testCases.get("failedCount"));
            result.put("skippedCount", testCases.get("skippedCount"));

            if (testCases.containsKey("testCases")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> cases = (List<Map<String, Object>>) testCases.get("testCases");
                if (!cases.isEmpty()) {
                    result.put("sampleTestCase", cases.get(0));
                }
            }

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", "Test failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // Test XML parsing specifically
    @GetMapping("/test-xml-parsing/{jobName}/{buildNumber}")
    public ResponseEntity<Map<String, Object>> testXMLParsing(
            @PathVariable String jobName,
            @PathVariable String buildNumber) {
        try {
            Map<String, Object> result = new HashMap<>();

            // Get the Jenkins result from database
            Optional<JenkinsResult> jenkinsResult = jenkinsResultRepository
                    .findByJobNameAndBuildNumber(jobName, buildNumber);

            if (jenkinsResult.isPresent()) {
                // Test XML parsing directly
                List<JenkinsTestCase> testCases = testNGXMLParserService.extractTestCasesFromXMLFiles(jenkinsResult.get());

                result.put("success", true);
                result.put("xmlTestCaseCount", testCases.size());
                result.put("jobName", jobName);
                result.put("buildNumber", buildNumber);

                if (!testCases.isEmpty()) {
                    // Show sample test cases
                    List<Map<String, Object>> samples = new ArrayList<>();
                    for (int i = 0; i < Math.min(5, testCases.size()); i++) {
                        JenkinsTestCase tc = testCases.get(i);
                        Map<String, Object> sample = new HashMap<>();
                        sample.put("className", tc.getClassName());
                        sample.put("testName", tc.getTestName());
                        sample.put("status", tc.getStatus());
                        sample.put("duration", tc.getDuration());
                        samples.add(sample);
                    }
                    result.put("sampleTestCases", samples);

                    // Count by status
                    long passed = testCases.stream().filter(tc -> "PASSED".equals(tc.getStatus())).count();
                    long failed = testCases.stream().filter(tc -> "FAILED".equals(tc.getStatus())).count();
                    long skipped = testCases.stream().filter(tc -> "SKIPPED".equals(tc.getStatus())).count();

                    result.put("passedCount", passed);
                    result.put("failedCount", failed);
                    result.put("skippedCount", skipped);
                } else {
                    result.put("message", "No test cases found in XML files");
                }
            } else {
                result.put("success", false);
                result.put("error", "Jenkins result not found in database");
            }

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", "XML parsing test failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

//    // Get all test cases for a specific build with filtering options
//    @GetMapping("/testng/{jobName}/{buildNumber}/testcases/detailed")
//    public ResponseEntity<Map<String, Object>> getDetailedTestCasesWithFilters(
//            @PathVariable String jobName,
//            @PathVariable String buildNumber,
//            @RequestParam(required = false) String status,
//            @RequestParam(required = false) String className,
//            @RequestParam(defaultValue = "0") int page,
//            @RequestParam(defaultValue = "50") int size) {
//        try {
//            Map<String, Object> testCases = jenkinsTestNGService.getDetailedTestCasesWithFilters(
//                    jobName, buildNumber, status, className, page, size);
//            return ResponseEntity.ok(testCases);
//        } catch (Exception e) {
//            Map<String, Object> response = new HashMap<>();
//            response.put("error", "Failed to get filtered test cases: " + e.getMessage());
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
//        }
//    }
//
//    // Get test execution trends for a job
//    @GetMapping("/testng/{jobName}/trends")
//    public ResponseEntity<Map<String, Object>> getTestExecutionTrends(
//            @PathVariable String jobName,
//            @RequestParam(defaultValue = "10") int builds) {
//        try {
//            Map<String, Object> trends = jenkinsTestNGService.getTestExecutionTrends(jobName, builds);
//            return ResponseEntity.ok(trends);
//        } catch (Exception e) {
//            Map<String, Object> response = new HashMap<>();
//            response.put("error", "Failed to get test execution trends: " + e.getMessage());
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
//        }
//    }
//
//    // Get failed test cases with error details
//    @GetMapping("/testng/{jobName}/{buildNumber}/failed-tests")
//    public ResponseEntity<Map<String, Object>> getFailedTestsWithDetails(
//            @PathVariable String jobName,
//            @PathVariable String buildNumber) {
//        try {
//            Map<String, Object> failedTests = jenkinsTestNGService.getFailedTestsWithDetails(jobName, buildNumber);
//            return ResponseEntity.ok(failedTests);
//        } catch (Exception e) {
//            Map<String, Object> response = new HashMap<>();
//            response.put("error", "Failed to get failed test details: " + e.getMessage());
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
//        }
//    }

//    // Refresh test data for a specific build
//    @PostMapping("/testng/{jobName}/{buildNumber}/refresh")
//    public ResponseEntity<Map<String, Object>> refreshTestDataForBuild(
//            @PathVariable String jobName,
//            @PathVariable String buildNumber) {
//        try {
//            // Force refresh of test data
//            jenkinsTestNGService.refreshTestDataForBuild(jobName, buildNumber);
//
//            // Get updated test cases
//            Map<String, Object> testCases = jenkinsTestNGService.getDetailedTestCases(jobName, buildNumber);
//
//            Map<String, Object> response = new HashMap<>();
//            response.put("message", "Test data refreshed successfully");
//            response.put("result", testCases);
//            return ResponseEntity.ok(response);
//        } catch (Exception e) {
//            Map<String, Object> response = new HashMap<>();
//            response.put("error", "Failed to refresh test data: " + e.getMessage());
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
//        }
//    }

    /**
     * Update notes for a Jenkins result
     */
    @PutMapping("/results/{id}/notes")
    public ResponseEntity<Map<String, Object>> updateJenkinsResultNotes(
            @PathVariable Long id,
            @RequestBody NotesUpdateRequest request) {
        try {
            String notes = request.getNotes();
            
            Optional<JenkinsResult> optionalResult = jenkinsResultRepository.findById(id);
            if (optionalResult.isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Jenkins result not found with id: " + id);
                return ResponseEntity.notFound().build();
            }
            
            JenkinsResult result = optionalResult.get();
            result.setNotes(notes);
            jenkinsResultRepository.save(result);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Notes updated successfully");
            response.put("id", id);
            response.put("notes", notes);
            response.put("timestamp", new Date());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to update notes: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Get notes for a Jenkins result
     */
    @GetMapping("/results/{id}/notes")
    public ResponseEntity<Map<String, Object>> getJenkinsResultNotes(@PathVariable Long id) {
        try {
            Optional<JenkinsResult> optionalResult = jenkinsResultRepository.findById(id);
            if (optionalResult.isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Jenkins result not found with id: " + id);
                return ResponseEntity.notFound().build();
            }
            
            JenkinsResult result = optionalResult.get();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("id", id);
            response.put("notes", result.getNotes());
            response.put("jobName", result.getJobName());
            response.put("buildNumber", result.getBuildNumber());
            response.put("timestamp", new Date());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to get notes: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // Health check endpoint for the controller
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", new Date());
        health.put("service", "Jenkins Controller");
        return ResponseEntity.ok(health);
    }

    // Request DTOs
    public static class NotesUpdateRequest {
        private String notes;

        public NotesUpdateRequest() {}

        public String getNotes() {
            return notes;
        }

        public void setNotes(String notes) {
            this.notes = notes;
        }
    }
}