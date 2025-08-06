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

    @Bean
    CommandLineRunner initDatabase(PermissionRepository repository, DomainRepository domainRepository) {
        return args -> {
            repository.save(new UserPermission(1L, "read", LocalDateTime.now(), LocalDateTime.now()));
            repository.save(new UserPermission(2L, "write", LocalDateTime.now(), LocalDateTime.now()));
            domainRepository.saveAll(Arrays.asList(
                    new Domain("Shipping Solutions", "Comprehensive logistics and shipping management systems for tracking, routing, and delivery optimization", "Active"),
                    new Domain("Sales", "Customer relationship management and sales pipeline tracking applications", "Active"),
                    new Domain("Accounting and Finance", "Financial management systems including general ledger, budgeting, and financial reporting", "Active"),
                    new Domain("Accounts Receivable", "Customer billing, invoice management, and payment tracking systems", "Active"),
                    new Domain("Inventory", "Stock management and warehouse operations including tracking, replenishment, and optimization", "Active"),
                    new Domain("Marketing IT", "Digital marketing platforms, campaign management, and marketing automation tools", "Active"),
                    new Domain("Pricing", "Dynamic pricing engines, cost analysis, and revenue optimization systems", "Active"),
                    new Domain("Purchasing", "Procurement management systems for vendor relations, purchase orders, and supply chain coordination", "Active")
            ));
            System.out.println("Domains preloaded");
        };
    }
}
