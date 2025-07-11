package com.qa.automation.controller;

import com.qa.automation.dto.DashboardStats;
import com.qa.automation.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    
    @Autowired
    private DashboardService dashboardService;
    
    @GetMapping("/stats")
    public ResponseEntity<DashboardStats> getDashboardStats() {
        DashboardStats stats = dashboardService.getDashboardStats();
        return ResponseEntity.ok(stats);
    }
    
    @GetMapping("/project-stats/{projectId}")
    public ResponseEntity<Map<String, Long>> getProjectStats(@PathVariable Long projectId) {
        Map<String, Long> stats = dashboardService.getProjectStats(projectId);
        return ResponseEntity.ok(stats);
    }
}
