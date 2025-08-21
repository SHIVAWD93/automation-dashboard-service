package com.qa.automation.controller;

import com.qa.automation.dto.UserDto;
import com.qa.automation.model.User;
import com.qa.automation.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@CrossOrigin(origins = "*")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("")
    public ResponseEntity<UserDto> getUser(@RequestParam String userName, @RequestParam String password) {
        UserDto user = userService.getUserDetails(userName, password);
        if (user != null) {
            user.setPassword(null);
            return ResponseEntity.ok(user); // 200 OK with user data
        } else {
            return ResponseEntity.notFound().build(); // 404 Not Found
        }
    }

    @PostMapping("")
    public ResponseEntity<User> saveUser(@RequestBody UserDto user) {
        User savedUser = userService.saveUser(user);
        return ResponseEntity.ok(savedUser); // 200 OK
        // Alternatively, use created(): return ResponseEntity.status(HttpStatus.CREATED).body(savedUser);
    }

    @PutMapping("")
    public ResponseEntity<User> updateUser(@RequestBody User user) {
        User updatedUser = userService.updateUser(user);
        if (updatedUser != null) {
            return ResponseEntity.ok(updatedUser); // 200 OK
        } else {
            return ResponseEntity.notFound().build(); // 404 Not Found if update failed (optional)
        }
    }
}
