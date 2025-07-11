package com.qa.automation.service;

import com.qa.automation.model.TestCase;
import com.qa.automation.repository.TestCaseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TestCaseService {
    
    @Autowired
    private TestCaseRepository testCaseRepository;
    
    public List<TestCase> getAllTestCases() {
        return testCaseRepository.findAll();
    }
    
    public TestCase createTestCase(TestCase testCase) {
        return testCaseRepository.save(testCase);
    }
    
    public TestCase getTestCaseById(Long id) {
        return testCaseRepository.findById(id).orElse(null);
    }
    
    public TestCase updateTestCase(Long id, TestCase testCase) {
        if (testCaseRepository.existsById(id)) {
            testCase.setId(id);
            return testCaseRepository.save(testCase);
        }
        return null;
    }
    
    public boolean deleteTestCase(Long id) {
        if (testCaseRepository.existsById(id)) {
            testCaseRepository.deleteById(id);
            return true;
        }
        return false;
    }
    
    public List<TestCase> getTestCasesByProject(Long projectId) {
        return testCaseRepository.findByProjectId(projectId);
    }
}
