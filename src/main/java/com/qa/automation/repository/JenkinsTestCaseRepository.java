package com.qa.automation.repository;

import com.qa.automation.model.JenkinsTestCase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JenkinsTestCaseRepository extends JpaRepository<JenkinsTestCase, Long> {

    // Find test cases by Jenkins result ID
    List<JenkinsTestCase> findByJenkinsResultId(Long jenkinsResultId);

    // Find test cases by status
    List<JenkinsTestCase> findByStatus(String status);

    // Find test cases by Jenkins result ID and status
    List<JenkinsTestCase> findByJenkinsResultIdAndStatus(Long jenkinsResultId, String status);

    // Count test cases by status for a specific Jenkins result
    Long countByJenkinsResultIdAndStatus(Long jenkinsResultId, String status);

    // Find failed test cases with error messages
    @Query("SELECT jtc FROM JenkinsTestCase jtc WHERE jtc.status = 'FAILED' AND jtc.jenkinsResult.id = :resultId")
    List<JenkinsTestCase> findFailedTestCasesByResultId(@Param("resultId") Long resultId);

    // Search test cases by name
    @Query("SELECT jtc FROM JenkinsTestCase jtc WHERE jtc.testName LIKE %:keyword% OR jtc.className LIKE %:keyword%")
    List<JenkinsTestCase> searchByKeyword(@Param("keyword") String keyword);

    // Get test case statistics by job
    @Query("SELECT jtc.status, COUNT(jtc) FROM JenkinsTestCase jtc WHERE jtc.jenkinsResult.jobName = :jobName " +
            "AND jtc.jenkinsResult.id = (SELECT MAX(jr.id) FROM JenkinsResult jr WHERE jr.jobName = :jobName) " +
            "GROUP BY jtc.status")
    List<Object[]> getTestCaseStatsByJobName(@Param("jobName") String jobName);
}