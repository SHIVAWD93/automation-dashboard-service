package com.qa.automation.controller;

import com.qa.automation.model.TestCase;
import com.qa.automation.service.TestCaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/testcases")
@CrossOrigin(origins = "*")
public class TestCaseController {

    @Autowired
    private TestCaseService testCaseService;

    @GetMapping
    public ResponseEntity<List<TestCase>> getAllTestCases() {
        List<TestCase> testCases = testCaseService.getAllTestCases();
        return ResponseEntity.ok(testCases);
    }

    @PostMapping
    public ResponseEntity<?> createTestCase(@RequestBody TestCase testCase) {
        try {
            TestCase savedTestCase = testCaseService.createTestCase(testCase);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedTestCase);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<TestCase> getTestCaseById(@PathVariable Long id) {
        TestCase testCase = testCaseService.getTestCaseById(id);
        if (testCase != null) {
            return ResponseEntity.ok(testCase);
        }
        return ResponseEntity.notFound().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateTestCase(@PathVariable Long id, @RequestBody TestCase testCase) {
        try {
            TestCase updatedTestCase = testCaseService.updateTestCase(id, testCase);
            if (updatedTestCase != null) {
                return ResponseEntity.ok(updatedTestCase);
            }
            return ResponseEntity.notFound().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTestCase(@PathVariable Long id) {
        boolean deleted = testCaseService.deleteTestCase(id);
        if (deleted) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/project/{projectId}")
    public ResponseEntity<List<TestCase>> getTestCasesByProject(@PathVariable Long projectId) {
        List<TestCase> testCases = testCaseService.getTestCasesByProject(projectId);
        return ResponseEntity.ok(testCases);
    }

    @GetMapping("/domain/{domainId}")
    public ResponseEntity<List<TestCase>> getTestCasesByDomain(@PathVariable Long domainId) {
        List<TestCase> testCases = testCaseService.getTestCasesByDomain(domainId);
        return ResponseEntity.ok(testCases);
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<TestCase>> getTestCasesByStatus(@PathVariable String status) {
        List<TestCase> testCases = testCaseService.getTestCasesByStatus(status);
        return ResponseEntity.ok(testCases);
    }

    @GetMapping("/tester/{testerId}")
    public ResponseEntity<List<TestCase>> getTestCasesByTester(@PathVariable Long testerId) {
        List<TestCase> testCases = testCaseService.getTestCasesByTester(testerId);
        return ResponseEntity.ok(testCases);
    }

    @GetMapping("/search")
    public ResponseEntity<List<TestCase>> searchTestCases(@RequestParam String keyword) {
        List<TestCase> testCases = testCaseService.searchTestCases(keyword);
        return ResponseEntity.ok(testCases);
    }

    @GetMapping("/search/domain/{domainId}")
    public ResponseEntity<List<TestCase>> searchTestCasesInDomain(
            @PathVariable Long domainId, @RequestParam String keyword) {
        List<TestCase> testCases = testCaseService.searchTestCasesInDomain(domainId, keyword);
        return ResponseEntity.ok(testCases);
    }
}