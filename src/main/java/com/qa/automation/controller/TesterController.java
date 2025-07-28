package com.qa.automation.controller;

import com.qa.automation.model.Tester;
import com.qa.automation.service.TesterService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/testers")
@CrossOrigin(origins = "*")
public class TesterController {

    @Autowired
    private TesterService testerService;

    @GetMapping
    public ResponseEntity<List<Tester>> getAllTesters() {
        List<Tester> testers = testerService.getAllTesters();
        return ResponseEntity.ok(testers);
    }

    @PostMapping
    public ResponseEntity<?> createTester(@RequestBody Tester tester) {
        try {
            Tester savedTester = testerService.createTester(tester);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedTester);
        }
        catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping(value = "with-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createTesterWithImage(
            @ModelAttribute Tester tester,
            @RequestParam("profileImage") MultipartFile profileImage) {
        try {
            Tester savedTester = testerService.createTester(tester, profileImage);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedTester);
        }
        catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
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

    @GetMapping("/uploads/{filename}")
    public ResponseEntity<String> getTesterImage(@PathVariable String filename) {
        try {
            Path filePath = Paths.get("uploads", filename);
            byte[] fileBytes = Files.readAllBytes(filePath);
            String base64 = Base64.getEncoder().encodeToString(fileBytes);
            return ResponseEntity.ok(base64);
        }
        catch (IOException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Image not found");
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateTester(@PathVariable Long id, @RequestBody Tester tester) {
        try {
            Tester updatedTester = testerService.updateTester(id, tester);
            if (updatedTester != null) {
                return ResponseEntity.ok(updatedTester);
            }
            return ResponseEntity.notFound().build();
        }
        catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTester(@PathVariable Long id) {
        boolean deleted = testerService.deleteTester(id);
        if (deleted) {
            Path imagePath = Paths.get("uploads", id + ".png");
            try {
                Files.deleteIfExists(imagePath);
            } catch (IOException e) {
                // Optional: log the failure, but don't fail the delete just for image
                System.err.println("Failed to delete image for tester ID " + id + ": " + e.getMessage());
            }
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/role/{role}")
    public ResponseEntity<List<Tester>> getTestersByRole(@PathVariable String role) {
        List<Tester> testers = testerService.getTestersByRole(role);
        return ResponseEntity.ok(testers);
    }

    @GetMapping("/search")
    public ResponseEntity<List<Tester>> searchTesters(@RequestParam String name) {
        List<Tester> testers = testerService.searchTestersByName(name);
        return ResponseEntity.ok(testers);
    }
}