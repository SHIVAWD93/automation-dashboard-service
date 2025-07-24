package com.qa.automation.config;

import com.qa.automation.model.Tester;
import com.qa.automation.repository.TesterRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private TesterRepository testerRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // Create default admin user if not exists
        if (!testerRepository.existsByUsername("admin")) {
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
        }

        // Create default maintainer user if not exists
        if (!testerRepository.existsByUsername("maintainer")) {
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
        }
    }
}