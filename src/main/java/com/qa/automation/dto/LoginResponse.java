package com.qa.automation.dto;

public class LoginResponse {
    
    private String token;
    private String type = "Bearer";
    private Long id;
    private String username;
    private String name;
    private String email;
    private String role;
    
    // Constructors
    public LoginResponse() {}
    
    public LoginResponse(String token, Long id, String username, String name, String email, String role) {
        this.token = token;
        this.id = id;
        this.username = username;
        this.name = name;
        this.email = email;
        this.role = role;
    }
    
    // Getters and Setters
    public String getToken() {
        return token;
    }
    
    public void setToken(String token) {
        this.token = token;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getRole() {
        return role;
    }
    
    public void setRole(String role) {
        this.role = role;
    }
}