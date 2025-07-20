package com.qa.automation.service;

import com.qa.automation.model.JenkinsResult;
import com.qa.automation.model.JenkinsTestCase;
import com.qa.automation.repository.JenkinsResultRepository;
import com.qa.automation.repository.JenkinsTestCaseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class DatabaseDebugService {

    @Autowired
    private JenkinsResultRepository jenkinsResultRepository;

    @Autowired
    private JenkinsTestCaseRepository jenkinsTestCaseRepository;

    public Map<String, Object> getDatabaseStatus() {
        Map<String, Object> status = new HashMap<>();

        try {
            // Count total results
            long totalResults = jenkinsResultRepository.count();
            status.put("totalJenkinsResults", totalResults);

            // Count total test cases
            long totalTestCases = jenkinsTestCaseRepository.count();
            status.put("totalTestCases", totalTestCases);

            // Get sample results
            List<JenkinsResult> sampleResults = jenkinsResultRepository.findAll().stream()
                    .limit(5).toList();
            status.put("sampleResults", sampleResults);

            // Get results with test cases
            List<JenkinsResult> resultsWithTestCases = jenkinsResultRepository.findAll().stream()
                    .filter(result -> {
                        List<JenkinsTestCase> testCases = jenkinsTestCaseRepository.findByJenkinsResultId(result.getId());
                        return !testCases.isEmpty();
                    })
                    .limit(5).toList();
            status.put("resultsWithTestCases", resultsWithTestCases);

            // Get specific job info
            Optional<JenkinsResult> wiseRegression = jenkinsResultRepository
                    .findByJobNameAndBuildNumber("AccountReceivableIT_AR_WISE_Regression", "180");
            if (wiseRegression.isPresent()) {
                List<JenkinsTestCase> testCases = jenkinsTestCaseRepository
                        .findByJenkinsResultId(wiseRegression.get().getId());
                status.put("wiseRegressionTestCases", testCases.size());
                status.put("wiseRegressionDetails", wiseRegression.get());
            }

        } catch (Exception e) {
            status.put("error", "Database query failed: " + e.getMessage());
        }

        return status;
    }

    public Map<String, Object> getJobTestCaseDetails(String jobName, String buildNumber) {
        Map<String, Object> details = new HashMap<>();

        try {
            Optional<JenkinsResult> result = jenkinsResultRepository
                    .findByJobNameAndBuildNumber(jobName, buildNumber);

            if (result.isPresent()) {
                JenkinsResult jenkinsResult = result.get();
                details.put("jenkinsResult", jenkinsResult);

                List<JenkinsTestCase> testCases = jenkinsTestCaseRepository
                        .findByJenkinsResultId(jenkinsResult.getId());
                details.put("testCases", testCases);
                details.put("testCaseCount", testCases.size());

                // Group by status
                Map<String, Long> statusCounts = new HashMap<>();
                testCases.forEach(tc -> {
                    statusCounts.merge(tc.getStatus(), 1L, Long::sum);
                });
                details.put("statusCounts", statusCounts);

            } else {
                details.put("error", "No result found for " + jobName + " build " + buildNumber);
            }

        } catch (Exception e) {
            details.put("error", "Query failed: " + e.getMessage());
        }

        return details;
    }
}