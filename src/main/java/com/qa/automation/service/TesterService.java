package com.qa.automation.service;

import com.qa.automation.model.Tester;
import com.qa.automation.repository.TesterRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TesterService {

    @Autowired
    private TesterRepository testerRepository;

    public List<Tester> getAllTesters() {
        return testerRepository.findAll();
    }

    public Tester createTester(Tester tester) {
        // Set default experience if not provided
        if (tester.getExperience() == null) {
            tester.setExperience(0);
        }
        return testerRepository.save(tester);
    }

    public Tester getTesterById(Long id) {
        return testerRepository.findById(id).orElse(null);
    }

    public Tester updateTester(Long id, Tester tester) {
        if (testerRepository.existsById(id)) {
            tester.setId(id);
            // Set default experience if not provided
            if (tester.getExperience() == null) {
                tester.setExperience(0);
            }
            return testerRepository.save(tester);
        }
        return null;
    }

    public boolean deleteTester(Long id) {
        if (testerRepository.existsById(id)) {
            testerRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public List<Tester> getTestersByRole(String role) {
        return testerRepository.findByRole(role);
    }

    public List<Tester> getTestersByGender(String gender) {
        return testerRepository.findByGender(gender);
    }

    public List<Tester> searchTestersByName(String name) {
        return testerRepository.findByNameContaining(name);
    }

    public List<Tester> getTestersByExperience(Integer minExperience) {
        return testerRepository.findByExperienceGreaterThanEqual(minExperience);
    }

    public long getTestersCountByRole(String role) {
        return testerRepository.countByRole(role);
    }

    public long getTestersCountByGender(String gender) {
        return testerRepository.countByGender(gender);
    }
}