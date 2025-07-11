package com.qa.automation.controller;

import com.qa.automation.model.Tester;
import com.qa.automation.service.TesterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/testers")
public class TesterController {
    
    @Autowired
    private TesterService testerService;
    
    @GetMapping
    public ResponseEntity<List<Tester>> getAllTesters() {
        List<Tester> testers = testerService.getAllTesters();
        return ResponseEntity.ok(testers);
    }
    
    @PostMapping
    public ResponseEntity<Tester> createTester(
            @RequestParam("name") String name,
            @RequestParam("role") String role,
            @RequestParam("gender") String gender,
            @RequestParam(value = "profileImage", required = false) MultipartFile profileImage) {
        
        try {
            Tester tester = new Tester();
            tester.setName(name);
            tester.setRole(role);
            tester.setGender(gender);
            
            Tester savedTester = testerService.createTester(tester, profileImage);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedTester);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Tester> getTesterById(@PathVariable Long id) {
        Tester tester = testerService.getTesterById(id);
        if (tester != null) {
            return ResponseEntity.ok(tester);
        }
        return ResponseEntity.notFound().build();
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTester(@PathVariable Long id) {
        boolean deleted = testerService.deleteTester(id);
        if (deleted) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
