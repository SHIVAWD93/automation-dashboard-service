package com.qa.automation.controller;

import com.qa.automation.model.Tester;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/user")
public class UserController {

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body("User not authenticated");
        }

        Tester tester = (Tester) authentication.getPrincipal();
        
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", tester.getId());
        userInfo.put("username", tester.getUsername());
        userInfo.put("name", tester.getName());
        userInfo.put("email", tester.getEmail());
        userInfo.put("role", tester.getRole());
        userInfo.put("gender", tester.getGender());
        userInfo.put("experience", tester.getExperience());
        
        return ResponseEntity.ok(userInfo);
    }
}