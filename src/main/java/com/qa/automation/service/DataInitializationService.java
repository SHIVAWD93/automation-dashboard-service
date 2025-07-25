package com.qa.automation.service;

import com.qa.automation.model.*;
import com.qa.automation.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Service
@Transactional
public class DataInitializationService implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializationService.class);

    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private DomainRepository domainRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private TesterRepository testerRepository;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        logger.info("Starting data initialization...");
        
        initializePermissions();
        initializeDomains();
        initializeProjects();
        initializeTesters();
        
        logger.info("Data initialization completed successfully");
    }

    private void initializePermissions() {
        logger.info("Initializing permissions...");
        
        List<String> defaultPermissions = Arrays.asList("read", "write", "admin", "delete");
        
        for (String permissionName : defaultPermissions) {
            if (permissionRepository.findUserPermissionByPermission(permissionName) == null) {
                UserPermission permission = new UserPermission();
                permission.setPermission(permissionName);
                permissionRepository.save(permission);
                logger.info("Created permission: {}", permissionName);
            }
        }
    }

    private void initializeDomains() {
        logger.info("Initializing domains...");
        
        List<String> defaultDomains = Arrays.asList(
            "Authentication", 
            "User Management", 
            "API Testing", 
            "UI Testing", 
            "Integration Testing",
            "Performance Testing",
            "Security Testing"
        );
        
        for (String domainName : defaultDomains) {
            if (domainRepository.findByName(domainName).isEmpty()) {
                Domain domain = new Domain();
                domain.setName(domainName);
                domain.setDescription("Default " + domainName + " domain");
                domainRepository.save(domain);
                logger.info("Created domain: {}", domainName);
            }
        }
    }

    private void initializeProjects() {
        logger.info("Initializing projects...");
        
        // Get default domain
        Domain defaultDomain = domainRepository.findByName("API Testing")
                .orElseGet(() -> {
                    Domain domain = new Domain();
                    domain.setName("API Testing");
                    domain.setDescription("Default API Testing domain");
                    return domainRepository.save(domain);
                });

        List<String> defaultProjects = Arrays.asList(
            "Web Application",
            "Mobile App", 
            "API Services",
            "Admin Dashboard"
        );
        
        for (String projectName : defaultProjects) {
            if (projectRepository.findByName(projectName).isEmpty()) {
                Project project = new Project();
                project.setName(projectName);
                project.setDescription("Default " + projectName + " project");
                project.setDomain(defaultDomain);
                projectRepository.save(project);
                logger.info("Created project: {}", projectName);
            }
        }
    }

    private void initializeTesters() {
        logger.info("Initializing testers...");
        
        List<TesterData> defaultTesters = Arrays.asList(
            new TesterData("John Doe", "john.doe@company.com", "Senior QA Engineer"),
            new TesterData("Jane Smith", "jane.smith@company.com", "QA Engineer"),
            new TesterData("Mike Johnson", "mike.johnson@company.com", "Test Automation Engineer"),
            new TesterData("Sarah Wilson", "sarah.wilson@company.com", "QA Lead")
        );
        
        for (TesterData testerData : defaultTesters) {
            if (testerRepository.findByEmail(testerData.email).isEmpty()) {
                Tester tester = new Tester();
                tester.setName(testerData.name);
                tester.setEmail(testerData.email);
                tester.setRole(testerData.role);
                testerRepository.save(tester);
                logger.info("Created tester: {}", testerData.name);
            }
        }
    }

    // Helper class for tester data
    private static class TesterData {
        final String name;
        final String email;
        final String role;

        TesterData(String name, String email, String role) {
            this.name = name;
            this.email = email;
            this.role = role;
        }
    }
}