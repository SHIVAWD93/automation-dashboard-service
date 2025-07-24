package com.qa.automation.config;

import com.qa.automation.model.Tester;
import com.qa.automation.repository.TesterRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    @Autowired
    private TesterRepository testerRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        try {
            logger.info("Starting user initialization...");
            
            // Create default admin user if not exists
            if (!testerRepository.existsByUsername("admin")) {
                logger.info("Creating admin user...");
                Tester admin = new Tester();
                admin.setName("System Administrator");
                admin.setUsername("admin");
                admin.setEmail("admin@qa.automation.com");
                admin.setPassword(passwordEncoder.encode("admin123"));
                admin.setRole("ADMIN");
                admin.setGender("Male");
                admin.setExperience(5);
                admin.setEnabled(true);
                testerRepository.save(admin);
                logger.info("Admin user created successfully");
            } else {
                logger.info("Admin user already exists");
            }

            // Create default maintainer user if not exists
            if (!testerRepository.existsByUsername("maintainer")) {
                logger.info("Creating maintainer user...");
                Tester maintainer = new Tester();
                maintainer.setName("QA Maintainer");
                maintainer.setUsername("maintainer");
                maintainer.setEmail("maintainer@qa.automation.com");
                maintainer.setPassword(passwordEncoder.encode("maintainer123"));
                maintainer.setRole("MAINTAINER");
                maintainer.setGender("Female");
                maintainer.setExperience(3);
                maintainer.setEnabled(true);
                testerRepository.save(maintainer);
                logger.info("Maintainer user created successfully");
            } else {
                logger.info("Maintainer user already exists");
            }
            
            logger.info("User initialization completed successfully");
        } catch (Exception e) {
            logger.error("Error during user initialization: {}", e.getMessage(), e);
        }
    }
}