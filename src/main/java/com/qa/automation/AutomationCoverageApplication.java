package com.qa.automation;

import com.qa.automation.model.Tester;
import com.qa.automation.model.User;
import com.qa.automation.model.UserPermission;
import com.qa.automation.repository.PermissionRepository;
import com.qa.automation.repository.TesterRepository;
import com.qa.automation.repository.UserRepository;
import java.security.Permission;
import java.time.LocalDateTime;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class AutomationCoverageApplication {
    public static void main(String[] args) {
        SpringApplication.run(AutomationCoverageApplication.class, args);
    }
    @Bean
    CommandLineRunner initDatabase(PermissionRepository repository, UserRepository userRepository) {
        return args -> {
            repository.save(new UserPermission( 1L,"read", LocalDateTime.now(),LocalDateTime.now()));
            repository.save(new UserPermission( 2L,"write", LocalDateTime.now(),LocalDateTime.now()));
            userRepository.save(new User("admin","admin","admin",repository.findById(2L).get()));
            System.out.println("Testers preloaded");
        };
    }
}
