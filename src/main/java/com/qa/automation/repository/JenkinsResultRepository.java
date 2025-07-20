package com.qa.automation.repository;

import com.qa.automation.model.JenkinsResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JenkinsResultRepository extends JpaRepository<JenkinsResult, Long> {

    // Find latest result for a specific job
    @Query("SELECT jr FROM JenkinsResult jr WHERE jr.jobName = :jobName ORDER BY jr.buildTimestamp DESC")
    List<JenkinsResult> findLatestByJobName(@Param("jobName") String jobName);

    // Find latest results for all jobs
    @Query("SELECT jr FROM JenkinsResult jr WHERE jr.id IN " +
            "(SELECT MAX(jr2.id) FROM JenkinsResult jr2 GROUP BY jr2.jobName) " +
            "ORDER BY jr.jobName")
    List<JenkinsResult> findLatestResultsForAllJobs();

    // Check if job exists
    boolean existsByJobName(String jobName);

    // Find by job name and build number
    Optional<JenkinsResult> findByJobNameAndBuildNumber(String jobName, String buildNumber);

    // Get all unique job names
    @Query("SELECT DISTINCT jr.jobName FROM JenkinsResult jr ORDER BY jr.jobName")
    List<String> findAllJobNames();

    // Get statistics for dashboard
    @Query("SELECT COUNT(jr) FROM JenkinsResult jr WHERE jr.buildStatus = :status")
    Long countByBuildStatus(@Param("status") String status);

    // Get total test counts
    @Query("SELECT COALESCE(SUM(jr.totalTests), 0) FROM JenkinsResult jr WHERE jr.id IN " +
            "(SELECT MAX(jr2.id) FROM JenkinsResult jr2 GROUP BY jr2.jobName)")
    Long getTotalTestsFromLatestBuilds();

    @Query("SELECT COALESCE(SUM(jr.passedTests), 0) FROM JenkinsResult jr WHERE jr.id IN " +
            "(SELECT MAX(jr2.id) FROM JenkinsResult jr2 GROUP BY jr2.jobName)")
    Long getTotalPassedTestsFromLatestBuilds();

    @Query("SELECT COALESCE(SUM(jr.failedTests), 0) FROM JenkinsResult jr WHERE jr.id IN " +
            "(SELECT MAX(jr2.id) FROM JenkinsResult jr2 GROUP BY jr2.jobName)")
    Long getTotalFailedTestsFromLatestBuilds();
}