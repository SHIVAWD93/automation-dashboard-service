package com.qa.automation.repository;

import com.qa.automation.model.Domain;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DomainRepository extends JpaRepository<Domain, Long> {

    List<Domain> findByStatus(String status);

    Optional<Domain> findByName(String name);

    boolean existsByName(String name);

    @Query("SELECT d FROM Domain d WHERE d.status = 'Active' ORDER BY d.name")
    List<Domain> findActiveDomains();

    long countByStatus(String status);
}