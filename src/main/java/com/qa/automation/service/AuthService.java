package com.qa.automation.service;

import com.qa.automation.dto.LoginRequest;
import com.qa.automation.dto.LoginResponse;
import com.qa.automation.dto.RegisterRequest;
import com.qa.automation.model.Tester;
import com.qa.automation.repository.TesterRepository;
import com.qa.automation.config.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    TesterRepository testerRepository;

    @Autowired
    PasswordEncoder encoder;

    @Autowired
    JwtUtils jwtUtils;

    public ResponseEntity<?> authenticateUser(LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateJwtToken(authentication);

        Tester tester = (Tester) authentication.getPrincipal();
        return ResponseEntity.ok(new LoginResponse(jwt,
                tester.getId(),
                tester.getUsername(),
                tester.getName(),
                tester.getEmail(),
                tester.getRole()));
    }

    public ResponseEntity<?> registerUser(RegisterRequest registerRequest) {
        if (testerRepository.existsByUsername(registerRequest.getUsername())) {
            return ResponseEntity
                    .badRequest()
                    .body("Error: Username is already taken!");
        }

        if (testerRepository.existsByEmail(registerRequest.getEmail())) {
            return ResponseEntity
                    .badRequest()
                    .body("Error: Email is already in use!");
        }

        // Create new user's account
        Tester tester = new Tester(registerRequest.getName(),
                registerRequest.getRole(),
                registerRequest.getGender(),
                registerRequest.getExperience(),
                registerRequest.getUsername(),
                encoder.encode(registerRequest.getPassword()),
                registerRequest.getEmail());

        testerRepository.save(tester);

        return ResponseEntity.ok("User registered successfully!");
    }
}