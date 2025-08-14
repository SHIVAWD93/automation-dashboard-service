package com.qa.automation;

import com.qa.automation.model.Domain;
import com.qa.automation.model.UserPermission;
import com.qa.automation.repository.DomainRepository;
import com.qa.automation.repository.PermissionRepository;
import java.time.LocalDateTime;
import java.util.Arrays;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class AutomationCoverageApplication {
    public static void main(String[] args) {
        SpringApplication.run(AutomationCoverageApplication.class, args);
    }
}
