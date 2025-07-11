package com.qa.automation.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class HomeController {

    @GetMapping("/")
    @ResponseBody
    public String home() {
        return "<h1>QA Automation Coverage Dashboard API</h1>" +
               "<p>Backend is running successfully!</p>" +
               "<ul>" +
               "<li><a href='/api/testers'>Testers API</a></li>" +
               "<li><a href='/api/projects'>Projects API</a></li>" +
               "<li><a href='/api/testcases'>Test Cases API</a></li>" +
               "<li><a href='/api/dashboard/stats'>Dashboard Stats API</a></li>" +
               "<li><a href='/actuator/health'>Health Check</a></li>" +
               "</ul>" +
               "<p>Frontend is available at: <a href='http://localhost:5000'>http://localhost:5000</a></p>";
    }
}