package com.qa.automation.controller;

import com.qa.automation.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = "*")
public class DashboardController {

    @Autowired
    private DashboardService dashboardService;

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getDashboardStats() {
        Map<String, Object> stats = dashboardService.getDashboardStats();
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/stats/domain/{domainId}")
    public ResponseEntity<Map<String, Object>> getDomainStats(@PathVariable Long domainId) {
        Map<String, Object> stats = dashboardService.getDomainStats(domainId);
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/stats/project/{projectId}")
    public ResponseEntity<Map<String, Object>> getProjectStats(@PathVariable Long projectId) {
        Map<String, Object> stats = dashboardService.getProjectStats(projectId);
        return ResponseEntity.ok(stats);
    }
}