package com.qa.automation.controller;

import com.qa.automation.dto.LoginRequest;
import com.qa.automation.dto.LoginResponse;
import com.qa.automation.dto.RegisterRequest;
import com.qa.automation.model.Tester;
import com.qa.automation.repository.TesterRepository;
import com.qa.automation.config.JwtUtils;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private TesterRepository testerRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtils jwtUtils;

    @PostMapping("/signin")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        return loginUser(loginRequest);
    }

    @PostMapping("/login") 
    public ResponseEntity<?> loginUser(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            logger.info("Login request for user: {}", loginRequest.getUsername());

            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

            SecurityContextHolder.getContext().setAuthentication(authentication);
            String jwt = jwtUtils.generateJwtToken(authentication);

            Tester tester = (Tester) authentication.getPrincipal();
            
            LoginResponse response = new LoginResponse(jwt, 
                    tester.getId(),
                    tester.getUsername(),
                    tester.getName(),
                    tester.getEmail(), 
                    tester.getRole());

            logger.info("Login successful for user: {}", loginRequest.getUsername());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Login failed for user {}: {}", loginRequest.getUsername(), e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("message", "Invalid username or password");
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequest registerRequest) {
        try {
            logger.info("Registration request for user: {}", registerRequest.getUsername());

            if (testerRepository.existsByUsername(registerRequest.getUsername())) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "Username is already taken!");
                return ResponseEntity.badRequest().body(error);
            }

            if (testerRepository.existsByEmail(registerRequest.getEmail())) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "Email is already in use!");
                return ResponseEntity.badRequest().body(error);
            }

            // Create new user
            Tester tester = new Tester();
            tester.setName(registerRequest.getName());
            tester.setUsername(registerRequest.getUsername());
            tester.setEmail(registerRequest.getEmail());
            tester.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
            tester.setRole(registerRequest.getRole());
            tester.setGender(registerRequest.getGender());
            tester.setExperience(registerRequest.getExperience());
            tester.setEnabled(true);

            testerRepository.save(tester);

            Map<String, String> response = new HashMap<>();
            response.put("message", "User registered successfully!");
            response.put("username", tester.getUsername());
            
            logger.info("Registration successful for user: {}", registerRequest.getUsername());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Registration failed for user {}: {}", registerRequest.getUsername(), e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("message", "Registration failed: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/test")
    public ResponseEntity<?> testEndpoint() {
        try {
            Map<String, String> response = new HashMap<>();
            response.put("message", "Auth endpoints are working!");
            response.put("timestamp", String.valueOf(System.currentTimeMillis()));
            response.put("status", "success");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Test endpoint error: {}", e.getMessage(), e);
            Map<String, String> error = new HashMap<>();
            error.put("message", "Test failed: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }
}