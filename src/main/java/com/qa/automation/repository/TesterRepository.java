package com.qa.automation.repository;

import com.qa.automation.model.Tester;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TesterRepository extends JpaRepository<Tester, Long> {

    List<Tester> findByRole(String role);

    List<Tester> findByGender(String gender);

    List<Tester> findByExperienceGreaterThanEqual(Integer experience);

    @Query("SELECT t FROM Tester t WHERE t.name LIKE %:name%")
    List<Tester> findByNameContaining(String name);

    long countByRole(String role);

    long countByGender(String gender);
}