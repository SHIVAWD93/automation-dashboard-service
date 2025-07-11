package com.qa.automation.repository;

import com.qa.automation.model.TestCase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TestCaseRepository extends JpaRepository<TestCase, Long> {
    List<TestCase> findByProjectId(Long projectId);
    long countByStatus(String status);
    long countByProjectIdAndStatus(Long projectId, String status);
}
