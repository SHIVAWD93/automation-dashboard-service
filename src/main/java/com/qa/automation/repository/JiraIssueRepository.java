package com.qa.automation.repository;

import com.qa.automation.model.JiraIssue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JiraIssueRepository extends JpaRepository<JiraIssue, Long> {

    // Find by Jira key (unique identifier)
    Optional<JiraIssue> findByJiraKey(String jiraKey);

    // Find all issues for a specific sprint
    List<JiraIssue> findBySprintId(String sprintId);

    // Find by sprint name
    List<JiraIssue> findBySprintName(String sprintName);

    // Find by assignee
    List<JiraIssue> findByAssignee(String assignee);

    // Find by issue type
    List<JiraIssue> findByIssueType(String issueType);

    // Find by status
    List<JiraIssue> findByStatus(String status);

    // Find issues with keyword count greater than zero
    @Query("SELECT ji FROM JiraIssue ji WHERE ji.keywordCount > 0")
    List<JiraIssue> findIssuesWithKeywords();

    // Find by sprint and keyword search
    @Query("SELECT ji FROM JiraIssue ji WHERE ji.sprintId = :sprintId AND ji.searchKeyword = :keyword")
    List<JiraIssue> findBySprintIdAndKeyword(@Param("sprintId") String sprintId, @Param("keyword") String keyword);

    // Find issues with linked test cases
    @Query("SELECT DISTINCT ji FROM JiraIssue ji LEFT JOIN FETCH ji.linkedTestCases")
    List<JiraIssue> findAllWithLinkedTestCases();

    // Find issues by sprint with linked test cases
    @Query("SELECT DISTINCT ji FROM JiraIssue ji LEFT JOIN FETCH ji.linkedTestCases WHERE ji.sprintId = :sprintId")
    List<JiraIssue> findBySprintIdWithLinkedTestCases(@Param("sprintId") String sprintId);

    // Find issues by assignee with linked test cases
    @Query("SELECT DISTINCT ji FROM JiraIssue ji LEFT JOIN FETCH ji.linkedTestCases WHERE ji.assignee = :assignee")
    List<JiraIssue> findByAssigneeWithLinkedTestCases(@Param("assignee") String assignee);

    // Check if issue exists by Jira key
    boolean existsByJiraKey(String jiraKey);

    // Count by sprint
    @Query("SELECT COUNT(ji) FROM JiraIssue ji WHERE ji.sprintId = :sprintId")
    Long countBySprintId(@Param("sprintId") String sprintId);

    // Get all sprint names
    @Query("SELECT DISTINCT ji.sprintName FROM JiraIssue ji WHERE ji.sprintName IS NOT NULL ORDER BY ji.sprintName")
    List<String> findDistinctSprintNames();

    // Get all assignees
    @Query("SELECT DISTINCT ji.assignee FROM JiraIssue ji WHERE ji.assignee IS NOT NULL ORDER BY ji.assignee")
    List<String> findDistinctAssignees();
}