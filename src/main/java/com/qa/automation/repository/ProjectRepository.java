package com.qa.automation.repository;

import com.qa.automation.model.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

    Optional<Project> findByName(String name);

    long countByStatus(String status);

    List<Project> findByDomainId(Long domainId);

    List<Project> findByDomainIdAndStatus(Long domainId, String status);

    List<Project> findByStatus(String status);

    @Query("SELECT p FROM Project p WHERE p.domain.id = :domainId ORDER BY p.name")
    List<Project> findByDomainIdOrderByName(@Param("domainId") Long domainId);

    @Query("SELECT COUNT(p) FROM Project p WHERE p.domain.id = :domainId")
    long countByDomainId(@Param("domainId") Long domainId);

    @Query("SELECT COUNT(p) FROM Project p WHERE p.domain.id = :domainId AND p.status = :status")
    long countByDomainIdAndStatus(@Param("domainId") Long domainId, @Param("status") String status);
}