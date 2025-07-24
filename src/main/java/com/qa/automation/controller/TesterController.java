package com.qa.automation.controller;

import com.qa.automation.model.Tester;
import com.qa.automation.service.TesterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/testers")
@CrossOrigin(origins = "*")
public class TesterController {

    @Autowired
    private TesterService testerService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('MAINTAINER')")
    public ResponseEntity<List<Tester>> getAllTesters() {
        List<Tester> testers = testerService.getAllTesters();
        return ResponseEntity.ok(testers);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createTester(@RequestBody Tester tester) {
        try {
            Tester savedTester = testerService.createTester(tester);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedTester);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MAINTAINER')")
    public ResponseEntity<Tester> getTesterById(@PathVariable Long id) {
        Tester tester = testerService.getTesterById(id);
        if (tester != null) {
            return ResponseEntity.ok(tester);
        }
        return ResponseEntity.notFound().build();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateTester(@PathVariable Long id, @RequestBody Tester tester) {
        try {
            Tester updatedTester = testerService.updateTester(id, tester);
            if (updatedTester != null) {
                return ResponseEntity.ok(updatedTester);
            }
            return ResponseEntity.notFound().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteTester(@PathVariable Long id) {
        boolean deleted = testerService.deleteTester(id);
        if (deleted) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/role/{role}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MAINTAINER')")
    public ResponseEntity<List<Tester>> getTestersByRole(@PathVariable String role) {
        List<Tester> testers = testerService.getTestersByRole(role);
        return ResponseEntity.ok(testers);
    }

    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MAINTAINER')")
    public ResponseEntity<List<Tester>> searchTesters(@RequestParam String name) {
        List<Tester> testers = testerService.searchTestersByName(name);
        return ResponseEntity.ok(testers);
    }
}