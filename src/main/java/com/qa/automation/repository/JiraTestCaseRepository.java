package com.qa.automation.repository;

import com.qa.automation.model.JiraTestCase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JiraTestCaseRepository extends JpaRepository<JiraTestCase, Long> {

    // Find by QTest title
    List<JiraTestCase> findByQtestTitle(String qtestTitle);

    // Find by QTest ID
    Optional<JiraTestCase> findByQtestId(String qtestId);

    // Find by Jira issue ID
    @Query("SELECT jtc FROM JiraTestCase jtc WHERE jtc.jiraIssue.id = :jiraIssueId")
    List<JiraTestCase> findByJiraIssueId(@Param("jiraIssueId") Long jiraIssueId);

    // Find by Jira issue key
    @Query("SELECT jtc FROM JiraTestCase jtc WHERE jtc.jiraIssue.jiraKey = :jiraKey")
    List<JiraTestCase> findByJiraIssueKey(@Param("jiraKey") String jiraKey);

    // Find by automation status
    List<JiraTestCase> findByAutomationStatus(String automationStatus);

    // Find ready to automate test cases
    @Query("SELECT jtc FROM JiraTestCase jtc WHERE jtc.automationStatus = 'READY_TO_AUTOMATE'")
    List<JiraTestCase> findReadyToAutomate();

    // Find not automatable test cases
    @Query("SELECT jtc FROM JiraTestCase jtc WHERE jtc.automationStatus = 'NOT_AUTOMATABLE'")
    List<JiraTestCase> findNotAutomatable();

    // Find pending test cases
    @Query("SELECT jtc FROM JiraTestCase jtc WHERE jtc.automationStatus = 'PENDING'")
    List<JiraTestCase> findPending();

    // Find by project ID
    @Query("SELECT jtc FROM JiraTestCase jtc WHERE jtc.project.id = :projectId")
    List<JiraTestCase> findByProjectId(@Param("projectId") Long projectId);

    // Find by assigned tester ID
    @Query("SELECT jtc FROM JiraTestCase jtc WHERE jtc.assignedTester.id = :testerId")
    List<JiraTestCase> findByAssignedTesterId(@Param("testerId") Long testerId);

    // Find test cases with project mapping
    @Query("SELECT jtc FROM JiraTestCase jtc WHERE jtc.project IS NOT NULL")
    List<JiraTestCase> findWithProjectMapping();

    // Find test cases without project mapping
    @Query("SELECT jtc FROM JiraTestCase jtc WHERE jtc.project IS NULL")
    List<JiraTestCase> findWithoutProjectMapping();

    // Count by automation status
    @Query("SELECT COUNT(jtc) FROM JiraTestCase jtc WHERE jtc.automationStatus = :status")
    Long countByAutomationStatus(@Param("status") String status);

    // Count ready to automate by project
    @Query("SELECT COUNT(jtc) FROM JiraTestCase jtc WHERE jtc.automationStatus = 'READY_TO_AUTOMATE' AND jtc.project.id = :projectId")
    Long countReadyToAutomateByProject(@Param("projectId") Long projectId);

    // Find by sprint ID through Jira issue
    @Query("SELECT jtc FROM JiraTestCase jtc WHERE jtc.jiraIssue.sprintId = :sprintId")
    List<JiraTestCase> findBySprintId(@Param("sprintId") String sprintId);

    // Find by domain mapped
    List<JiraTestCase> findByDomainMapped(String domainMapped);

    // Check if test case exists by QTest title and Jira issue
    @Query("SELECT COUNT(jtc) > 0 FROM JiraTestCase jtc WHERE jtc.qtestTitle = :qtestTitle AND jtc.jiraIssue.id = :jiraIssueId")
    boolean existsByQtestTitleAndJiraIssueId(@Param("qtestTitle") String qtestTitle, @Param("jiraIssueId") Long jiraIssueId);

    // Get automation statistics
    @Query("SELECT " +
           "SUM(CASE WHEN jtc.automationStatus = 'READY_TO_AUTOMATE' THEN 1 ELSE 0 END) as readyCount, " +
           "SUM(CASE WHEN jtc.automationStatus = 'NOT_AUTOMATABLE' THEN 1 ELSE 0 END) as notAutomatableCount, " +
           "SUM(CASE WHEN jtc.automationStatus = 'PENDING' THEN 1 ELSE 0 END) as pendingCount, " +
           "COUNT(jtc) as totalCount " +
           "FROM JiraTestCase jtc")
    Object[] getAutomationStatistics();

    // Get automation statistics by project
    @Query("SELECT " +
           "p.name as projectName, " +
           "SUM(CASE WHEN jtc.automationStatus = 'READY_TO_AUTOMATE' THEN 1 ELSE 0 END) as readyCount, " +
           "SUM(CASE WHEN jtc.automationStatus = 'NOT_AUTOMATABLE' THEN 1 ELSE 0 END) as notAutomatableCount, " +
           "SUM(CASE WHEN jtc.automationStatus = 'PENDING' THEN 1 ELSE 0 END) as pendingCount, " +
           "COUNT(jtc) as totalCount " +
           "FROM JiraTestCase jtc " +
           "LEFT JOIN jtc.project p " +
           "GROUP BY p.name")
    List<Object[]> getAutomationStatisticsByProject();
}