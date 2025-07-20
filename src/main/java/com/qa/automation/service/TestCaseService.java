package com.qa.automation.service;

import com.qa.automation.model.TestCase;
import com.qa.automation.model.Project;
import com.qa.automation.model.Tester;
import com.qa.automation.repository.TestCaseRepository;
import com.qa.automation.repository.ProjectRepository;
import com.qa.automation.repository.TesterRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TestCaseService {

    @Autowired
    private TestCaseRepository testCaseRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private TesterRepository testerRepository;

    public List<TestCase> getAllTestCases() {
        return testCaseRepository.findAll();
    }

    public TestCase createTestCase(TestCase testCase) {
        // Handle project assignment
        if (testCase.getProjectId() != null) {
            Project project = projectRepository.findById(testCase.getProjectId()).orElse(null);
            if (project == null) {
                throw new RuntimeException("Project not found with id: " + testCase.getProjectId());
            }
            testCase.setProject(project);
        } else if (testCase.getProject() != null && testCase.getProject().getId() != null) {
            Project project = projectRepository.findById(testCase.getProject().getId()).orElse(null);
            if (project == null) {
                throw new RuntimeException("Project not found with id: " + testCase.getProject().getId());
            }
            testCase.setProject(project);
        } else {
            throw new RuntimeException("Project is required for creating a test case");
        }

        // Handle tester assignment
        if (testCase.getTesterId() != null) {
            Tester tester = testerRepository.findById(testCase.getTesterId()).orElse(null);
            if (tester == null) {
                throw new RuntimeException("Tester not found with id: " + testCase.getTesterId());
            }
            testCase.setTester(tester);
        } else if (testCase.getTester() != null && testCase.getTester().getId() != null) {
            Tester tester = testerRepository.findById(testCase.getTester().getId()).orElse(null);
            if (tester == null) {
                throw new RuntimeException("Tester not found with id: " + testCase.getTester().getId());
            }
            testCase.setTester(tester);
        } else {
            throw new RuntimeException("Tester is required for creating a test case");
        }

        return testCaseRepository.save(testCase);
    }

    public TestCase getTestCaseById(Long id) {
        return testCaseRepository.findById(id).orElse(null);
    }

    public TestCase updateTestCase(Long id, TestCase testCase) {
        if (testCaseRepository.existsById(id)) {
            testCase.setId(id);

            // Handle project assignment for update
            if (testCase.getProjectId() != null) {
                Project project = projectRepository.findById(testCase.getProjectId()).orElse(null);
                if (project == null) {
                    throw new RuntimeException("Project not found with id: " + testCase.getProjectId());
                }
                testCase.setProject(project);
            } else if (testCase.getProject() != null && testCase.getProject().getId() != null) {
                Project project = projectRepository.findById(testCase.getProject().getId()).orElse(null);
                if (project == null) {
                    throw new RuntimeException("Project not found with id: " + testCase.getProject().getId());
                }
                testCase.setProject(project);
            }

            // Handle tester assignment for update
            if (testCase.getTesterId() != null) {
                Tester tester = testerRepository.findById(testCase.getTesterId()).orElse(null);
                if (tester == null) {
                    throw new RuntimeException("Tester not found with id: " + testCase.getTesterId());
                }
                testCase.setTester(tester);
            } else if (testCase.getTester() != null && testCase.getTester().getId() != null) {
                Tester tester = testerRepository.findById(testCase.getTester().getId()).orElse(null);
                if (tester == null) {
                    throw new RuntimeException("Tester not found with id: " + testCase.getTester().getId());
                }
                testCase.setTester(tester);
            }

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

    public List<TestCase> getTestCasesByDomain(Long domainId) {
        return testCaseRepository.findByDomainId(domainId);
    }

    public List<TestCase> getTestCasesByStatus(String status) {
        return testCaseRepository.findByStatus(status);
    }

    public List<TestCase> getTestCasesByPriority(String priority) {
        return testCaseRepository.findByPriority(priority);
    }

    public List<TestCase> getTestCasesByTester(Long testerId) {
        return testCaseRepository.findByTesterId(testerId);
    }

    public List<TestCase> getTestCasesByProjectAndStatus(Long projectId, String status) {
        return testCaseRepository.findByProjectIdAndStatus(projectId, status);
    }

    public List<TestCase> getTestCasesByDomainAndStatus(Long domainId, String status) {
        return testCaseRepository.findByDomainIdAndStatus(domainId, status);
    }

    public List<TestCase> searchTestCases(String keyword) {
        return testCaseRepository.searchByKeyword(keyword);
    }

    public List<TestCase> searchTestCasesInDomain(Long domainId, String keyword) {
        return testCaseRepository.searchByKeywordInDomain(domainId, keyword);
    }

    // Count methods
    public long getTestCasesCountByProject(Long projectId) {
        return testCaseRepository.countByProjectId(projectId);
    }

    public long getTestCasesCountByStatus(String status) {
        return testCaseRepository.countByStatus(status);
    }

    public long getTestCasesCountByProjectAndStatus(Long projectId, String status) {
        return testCaseRepository.countByProjectIdAndStatus(projectId, status);
    }

    public long getTestCasesCountByDomain(Long domainId) {
        return testCaseRepository.countByDomainId(domainId);
    }

    public long getTestCasesCountByDomainAndStatus(Long domainId, String status) {
        return testCaseRepository.countByDomainIdAndStatus(domainId, status);
    }

    // Statistics methods
    public List<Object[]> getTestCaseStatsByProject(Long projectId) {
        return testCaseRepository.getTestCaseStatsByProject(projectId);
    }

    public List<Object[]> getTestCaseStatsByDomain(Long domainId) {
        return testCaseRepository.getTestCaseStatsByDomain(domainId);
    }

    public List<Object[]> getTestCaseStatsByPriority() {
        return testCaseRepository.getTestCaseStatsByPriority();
    }

    // Bulk operations
    public List<TestCase> createTestCasesBulk(List<TestCase> testCases) {
        // Validate all test cases before saving
        for (TestCase testCase : testCases) {
            if (testCase.getProjectId() != null) {
                Project project = projectRepository.findById(testCase.getProjectId()).orElse(null);
                if (project == null) {
                    throw new RuntimeException("Project not found with id: " + testCase.getProjectId());
                }
                testCase.setProject(project);
            }

            if (testCase.getTesterId() != null) {
                Tester tester = testerRepository.findById(testCase.getTesterId()).orElse(null);
                if (tester == null) {
                    throw new RuntimeException("Tester not found with id: " + testCase.getTesterId());
                }
                testCase.setTester(tester);
            }
        }

        return testCaseRepository.saveAll(testCases);
    }

    public boolean deleteTestCasesByProject(Long projectId) {
        List<TestCase> testCases = testCaseRepository.findByProjectId(projectId);
        if (!testCases.isEmpty()) {
            testCaseRepository.deleteAll(testCases);
            return true;
        }
        return false;
    }

    public boolean deleteTestCasesByDomain(Long domainId) {
        List<TestCase> testCases = testCaseRepository.findByDomainId(domainId);
        if (!testCases.isEmpty()) {
            testCaseRepository.deleteAll(testCases);
            return true;
        }
        return false;
    }
}