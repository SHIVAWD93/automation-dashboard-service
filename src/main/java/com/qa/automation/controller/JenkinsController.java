package com.qa.automation.controller;

import com.qa.automation.model.JenkinsResult;
import com.qa.automation.model.JenkinsTestCase;
import com.qa.automation.model.Tester;
import com.qa.automation.model.Project;
import com.qa.automation.repository.JenkinsResultRepository;
import com.qa.automation.repository.TesterRepository;
import com.qa.automation.repository.ProjectRepository;
import com.qa.automation.service.JenkinsService;
import com.qa.automation.service.JenkinsTestNGService;
import com.qa.automation.service.TestNGXMLParserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

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

    @Autowired
    private TesterRepository testerRepository;

    @Autowired
    private ProjectRepository projectRepository;

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
            // Calculate and set pass percentage for each result
            for (JenkinsResult result : results) {
                calculateAndSetPassPercentage(result);
            }
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    // NEW: Enhanced results endpoint with filtering
    @GetMapping("/results/filtered")
    public ResponseEntity<List<JenkinsResult>> getFilteredResults(
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) Long automationTesterId,
            @RequestParam(required = false) String jobFrequency,
            @RequestParam(required = false) String buildStatus) {
        try {
            List<JenkinsResult> results = jenkinsService.getAllLatestResults();

            // Apply filters
            List<JenkinsResult> filteredResults = results.stream()
                    .filter(result -> {
                        // Project filter
                        if (projectId != null && (result.getProject() == null || !result.getProject().getId().equals(projectId))) {
                            return false;
                        }
                        // Automation tester filter
                        if (automationTesterId != null && (result.getAutomationTester() == null || !result.getAutomationTester().getId().equals(automationTesterId))) {
                            return false;
                        }
                        // Job frequency filter
                        if (jobFrequency != null && !jobFrequency.isEmpty() && !jobFrequency.equalsIgnoreCase(result.getJobFrequency())) {
                            return false;
                        }
                        // Build status filter
                        if (buildStatus != null && !buildStatus.isEmpty() && !buildStatus.equalsIgnoreCase(result.getBuildStatus())) {
                            return false;
                        }
                        return true;
                    })
                    .collect(Collectors.toList());

            // Calculate and set pass percentage for each result
            for (JenkinsResult result : filteredResults) {
                calculateAndSetPassPercentage(result);
                // Ensure job frequency is set
                if (result.getJobFrequency() == null || result.getJobFrequency().isEmpty()) {
                    result.inferJobFrequency();
                }
            }

            return ResponseEntity.ok(filteredResults);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // NEW: Get unique job frequencies for filter dropdown
    @GetMapping("/frequencies")
    public ResponseEntity<List<String>> getJobFrequencies() {
        try {
            List<String> frequencies = jenkinsResultRepository.findAll().stream()
                    .map(result -> {
                        if (result.getJobFrequency() == null || result.getJobFrequency().isEmpty()) {
                            result.inferJobFrequency();
                            return result.getJobFrequency();
                        }
                        return result.getJobFrequency();
                    })
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList());

            return ResponseEntity.ok(frequencies);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // NEW: Get projects that have Jenkins results for filter dropdown
    @GetMapping("/projects")
    public ResponseEntity<List<Project>> getProjectsWithJenkinsResults() {
        try {
            List<Project> projects = jenkinsResultRepository.findAll().stream()
                    .filter(result -> result.getProject() != null)
                    .map(JenkinsResult::getProject)
                    .distinct()
                    .sorted((p1, p2) -> p1.getName().compareToIgnoreCase(p2.getName()))
                    .collect(Collectors.toList());

            return ResponseEntity.ok(projects);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // NEW: Get automation testers that have Jenkins results for filter dropdown
    @GetMapping("/automation-testers")
    public ResponseEntity<List<Tester>> getAutomationTestersWithJenkinsResults() {
        try {
            List<Tester> testers = jenkinsResultRepository.findAll().stream()
                    .filter(result -> result.getAutomationTester() != null)
                    .map(JenkinsResult::getAutomationTester)
                    .distinct()
                    .sorted((t1, t2) -> t1.getName().compareToIgnoreCase(t2.getName()))
                    .collect(Collectors.toList());

            return ResponseEntity.ok(testers);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/results/{jobName}")
    public ResponseEntity<JenkinsResult> getLatestResultByJobName(@PathVariable String jobName) {
        try {
            JenkinsResult result = jenkinsService.getLatestResultByJobName(jobName);
            if (result != null) {
                calculateAndSetPassPercentage(result);
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

    /**
     * FIXED: Enhanced notes update endpoint
     */
    @PutMapping("/results/{id}/notes")
    public ResponseEntity<Map<String, Object>> updateJenkinsResultNotes(
            @PathVariable Long id,
            @RequestBody Map<String, Object> requestBody) {
        try {
            System.out.println("Received request body: " + requestBody);

            Optional<JenkinsResult> optionalResult = jenkinsResultRepository.findById(id);
            if (optionalResult.isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Jenkins result not found with id: " + id);
                return ResponseEntity.notFound().build();
            }

            JenkinsResult result = optionalResult.get();

            // Handle different possible request formats
            String notes = null;
            if (requestBody.containsKey("bugsIdentified")) {
                notes = (String) requestBody.get("bugsIdentified");
            } else if (requestBody.containsKey("failureReasons")) {
                notes = (String) requestBody.get("failureReasons");
            } else if (requestBody.containsKey("notes")) {
                notes = (String) requestBody.get("notes");
            }

            // Set the notes (allow empty string, but convert null to empty)
            result.setBugsIdentified(notes != null ? notes : "");
            result.setFailureReasons(notes != null ? notes : "");

            jenkinsResultRepository.save(result);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Notes updated successfully");
            response.put("id", id);
            response.put("notes", notes);
            response.put("timestamp", new Date());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("Error updating notes: " + e.getMessage());
            e.printStackTrace();
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to update notes: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * FIXED: Endpoint for assigning testers to Jenkins results
     */
    @PostMapping("/results/{id}/testers")
    public ResponseEntity<Map<String, Object>> assignTestersToJenkinsResult(
            @PathVariable Long id,
            @RequestBody TesterAssignmentRequest request) {
        try {
            System.out.println("Assigning testers to Jenkins result: " + id);
            System.out.println("Request: " + request);

            Optional<JenkinsResult> optionalResult = jenkinsResultRepository.findById(id);
            if (optionalResult.isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Jenkins result not found with id: " + id);
                return ResponseEntity.notFound().build();
            }

            JenkinsResult result = optionalResult.get();

            // Assign automation tester
            if (request.getAutomationTesterId() != null) {
                Optional<Tester> automationTester = testerRepository.findById(request.getAutomationTesterId());
                if (automationTester.isPresent()) {
                    result.setAutomationTester(automationTester.get());
                    System.out.println("Assigned automation tester: " + automationTester.get().getName());
                }
            }

            // Assign manual tester
            if (request.getManualTesterId() != null) {
                Optional<Tester> manualTester = testerRepository.findById(request.getManualTesterId());
                if (manualTester.isPresent()) {
                    result.setManualTester(manualTester.get());
                    System.out.println("Assigned manual tester: " + manualTester.get().getName());
                }
            }

            // Calculate and set pass percentage
            calculateAndSetPassPercentage(result);

            jenkinsResultRepository.save(result);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Testers assigned successfully");
            response.put("id", id);
            response.put("automationTester", result.getAutomationTester() != null ?
                    result.getAutomationTester().getName() : null);
            response.put("manualTester", result.getManualTester() != null ?
                    result.getManualTester().getName() : null);
            response.put("passPercentage", result.getPassPercentage());
            response.put("timestamp", new Date());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("Error assigning testers: " + e.getMessage());
            e.printStackTrace();
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to assign testers: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * FIXED: Main endpoint to save all data (notes, testers, and project) in one request
     */
    @PostMapping("/results/{id}/save-all")
    public ResponseEntity<Map<String, Object>> saveAllData(
            @PathVariable Long id,
            @RequestBody CombinedSaveRequest request) {
        try {
            System.out.println("Saving combined data for Jenkins result: " + id);
            System.out.println("Request: " + request);

            Optional<JenkinsResult> optionalResult = jenkinsResultRepository.findById(id);
            if (optionalResult.isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Jenkins result not found with id: " + id);
                return ResponseEntity.notFound().build();
            }

            JenkinsResult result = optionalResult.get();

            // Update notes if provided
            if (request.getNotes() != null) {
                String notes = request.getNotes().trim();
                result.setBugsIdentified(notes);
                result.setFailureReasons(notes);
            }

            // Update testers if provided
            if (request.getAutomationTesterId() != null) {
                Optional<Tester> automationTester = testerRepository.findById(request.getAutomationTesterId());
                if (automationTester.isPresent()) {
                    result.setAutomationTester(automationTester.get());
                } else {
                    result.setAutomationTester(null);
                }
            }

            if (request.getManualTesterId() != null) {
                Optional<Tester> manualTester = testerRepository.findById(request.getManualTesterId());
                if (manualTester.isPresent()) {
                    result.setManualTester(manualTester.get());
                } else {
                    result.setManualTester(null);
                }
            }

            // NEW: Update project if provided
            if (request.getProjectId() != null) {
                Optional<Project> project = projectRepository.findById(request.getProjectId());
                if (project.isPresent()) {
                    result.setProject(project.get());
                } else {
                    result.setProject(null);
                }
            }

            // Calculate and set pass percentage
            calculateAndSetPassPercentage(result);

            jenkinsResultRepository.save(result);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Data saved successfully");
            response.put("id", id);
            response.put("notes", result.getBugsIdentified());
            response.put("automationTester", result.getAutomationTester() != null ?
                    result.getAutomationTester().getName() : null);
            response.put("manualTester", result.getManualTester() != null ?
                    result.getManualTester().getName() : null);
            response.put("project", result.getProject() != null ?
                    result.getProject().getName() : null);
            response.put("passPercentage", result.getPassPercentage());
            response.put("timestamp", new Date());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("Error saving combined data: " + e.getMessage());
            e.printStackTrace();
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to save data: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Helper method to calculate and set pass percentage
     */
    private void calculateAndSetPassPercentage(JenkinsResult result) {
        if (result.getTotalTests() != null && result.getTotalTests() > 0) {
            int passedTests = result.getPassedTests() != null ? result.getPassedTests() : 0;
            double percentage = ((double) passedTests / result.getTotalTests()) * 100;
            result.setPassPercentage((int) Math.round(percentage));
        } else {
            result.setPassPercentage(0);
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
            response.put("notes", result.getBugsIdentified());
            response.put("bugsIdentified", result.getBugsIdentified());
            response.put("failureReasons", result.getFailureReasons());
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
    public static class TesterAssignmentRequest {
        private Long automationTesterId;
        private Long manualTesterId;

        public TesterAssignmentRequest() {}

        public Long getAutomationTesterId() {
            return automationTesterId;
        }

        public void setAutomationTesterId(Long automationTesterId) {
            this.automationTesterId = automationTesterId;
        }

        public Long getManualTesterId() {
            return manualTesterId;
        }

        public void setManualTesterId(Long manualTesterId) {
            this.manualTesterId = manualTesterId;
        }

        @Override
        public String toString() {
            return "TesterAssignmentRequest{" +
                    "automationTesterId=" + automationTesterId +
                    ", manualTesterId=" + manualTesterId +
                    '}';
        }
    }

    public static class CombinedSaveRequest {
        private String notes;
        private Long automationTesterId;
        private Long manualTesterId;
        private Long projectId; // NEW: Added project support

        public CombinedSaveRequest() {}

        public String getNotes() {
            return notes;
        }

        public void setNotes(String notes) {
            this.notes = notes;
        }

        public Long getAutomationTesterId() {
            return automationTesterId;
        }

        public void setAutomationTesterId(Long automationTesterId) {
            this.automationTesterId = automationTesterId;
        }

        public Long getManualTesterId() {
            return manualTesterId;
        }

        public void setManualTesterId(Long manualTesterId) {
            this.manualTesterId = manualTesterId;
        }

        public Long getProjectId() {
            return projectId;
        }

        public void setProjectId(Long projectId) {
            this.projectId = projectId;
        }

        @Override
        public String toString() {
            return "CombinedSaveRequest{" +
                    "notes='" + notes + '\'' +
                    ", automationTesterId=" + automationTesterId +
                    ", manualTesterId=" + manualTesterId +
                    ", projectId=" + projectId +
                    '}';
        }
    }

    public static class NotesUpdateRequest {
        private String notes;
        private String bugsIdentified;
        private String failureReasons;

        public NotesUpdateRequest() {}

        public String getNotes() {
            return notes;
        }

        public void setNotes(String notes) {
            this.notes = notes;
        }

        public String getBugsIdentified() {
            return bugsIdentified;
        }

        public void setBugsIdentified(String bugsIdentified) {
            this.bugsIdentified = bugsIdentified;
        }

        public String getFailureReasons() {
            return failureReasons;
        }

        public void setFailureReasons(String failureReasons) {
            this.failureReasons = failureReasons;
        }
    }
}