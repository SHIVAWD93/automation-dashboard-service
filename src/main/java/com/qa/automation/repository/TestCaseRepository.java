package com.qa.automation.repository;

import com.qa.automation.model.TestCase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TestCaseRepository extends JpaRepository<TestCase, Long> {

    // Basic finder methods using relationships
    @Query("SELECT tc FROM TestCase tc WHERE tc.project.id = :projectId")
    List<TestCase> findByProjectId(@Param("projectId") Long projectId);

    List<TestCase> findByStatus(String status);

    List<TestCase> findByPriority(String priority);

    @Query("SELECT tc FROM TestCase tc WHERE tc.tester.id = :testerId")
    List<TestCase> findByTesterId(@Param("testerId") Long testerId);

    @Query("SELECT tc FROM TestCase tc WHERE tc.project.id = :projectId AND tc.status = :status")
    List<TestCase> findByProjectIdAndStatus(@Param("projectId") Long projectId, @Param("status") String status);

    @Query("SELECT tc FROM TestCase tc WHERE tc.project.domain.id = :domainId")
    List<TestCase> findByDomainId(@Param("domainId") Long domainId);

    @Query("SELECT tc FROM TestCase tc WHERE tc.project.domain.id = :domainId AND tc.status = :status")
    List<TestCase> findByDomainIdAndStatus(@Param("domainId") Long domainId, @Param("status") String status);

    // Count methods
    @Query("SELECT COUNT(tc) FROM TestCase tc WHERE tc.project.id = :projectId")
    long countByProjectId(@Param("projectId") Long projectId);

    long countByStatus(String status);

    @Query("SELECT COUNT(tc) FROM TestCase tc WHERE tc.project.id = :projectId AND tc.status = :status")
    long countByProjectIdAndStatus(@Param("projectId") Long projectId, @Param("status") String status);

    @Query("SELECT COUNT(tc) FROM TestCase tc WHERE tc.project.domain.id = :domainId")
    long countByDomainId(@Param("domainId") Long domainId);

    @Query("SELECT COUNT(tc) FROM TestCase tc WHERE tc.project.domain.id = :domainId AND tc.status = :status")
    long countByDomainIdAndStatus(@Param("domainId") Long domainId, @Param("status") String status);

    // Statistics methods
    @Query("SELECT tc.status, COUNT(tc) FROM TestCase tc WHERE tc.project.id = :projectId GROUP BY tc.status")
    List<Object[]> getTestCaseStatsByProject(@Param("projectId") Long projectId);

    @Query("SELECT tc.status, COUNT(tc) FROM TestCase tc WHERE tc.project.domain.id = :domainId GROUP BY tc.status")
    List<Object[]> getTestCaseStatsByDomain(@Param("domainId") Long domainId);

    @Query("SELECT tc.priority, COUNT(tc) FROM TestCase tc GROUP BY tc.priority")
    List<Object[]> getTestCaseStatsByPriority();

    // Search methods
    @Query("SELECT tc FROM TestCase tc WHERE tc.title LIKE %:keyword% OR tc.description LIKE %:keyword%")
    List<TestCase> searchByKeyword(@Param("keyword") String keyword);

    @Query("SELECT tc FROM TestCase tc WHERE tc.project.domain.id = :domainId AND (tc.title LIKE %:keyword% OR tc.description LIKE %:keyword%)")
    List<TestCase> searchByKeywordInDomain(@Param("domainId") Long domainId, @Param("keyword") String keyword);
}