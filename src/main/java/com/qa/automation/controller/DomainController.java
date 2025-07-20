package com.qa.automation.controller;

import com.qa.automation.model.Domain;
import com.qa.automation.service.DomainService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/domains")
@CrossOrigin(origins = "*")
public class DomainController {

    @Autowired
    private DomainService domainService;

    @GetMapping
    public ResponseEntity<List<Domain>> getAllDomains() {
        List<Domain> domains = domainService.getAllDomains();
        return ResponseEntity.ok(domains);
    }

    @GetMapping("/active")
    public ResponseEntity<List<Domain>> getActiveDomains() {
        List<Domain> domains = domainService.getActiveDomains();
        return ResponseEntity.ok(domains);
    }

    @PostMapping
    public ResponseEntity<?> createDomain(@RequestBody Domain domain) {
        try {
            Domain savedDomain = domainService.createDomain(domain);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedDomain);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Domain> getDomainById(@PathVariable Long id) {
        Domain domain = domainService.getDomainById(id);
        if (domain != null) {
            return ResponseEntity.ok(domain);
        }
        return ResponseEntity.notFound().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateDomain(@PathVariable Long id, @RequestBody Domain domain) {
        try {
            Domain updatedDomain = domainService.updateDomain(id, domain);
            if (updatedDomain != null) {
                return ResponseEntity.ok(updatedDomain);
            }
            return ResponseEntity.notFound().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDomain(@PathVariable Long id) {
        boolean deleted = domainService.deleteDomain(id);
        if (deleted) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<Domain>> getDomainsByStatus(@PathVariable String status) {
        List<Domain> domains = domainService.getDomainsByStatus(status);
        return ResponseEntity.ok(domains);
    }
}