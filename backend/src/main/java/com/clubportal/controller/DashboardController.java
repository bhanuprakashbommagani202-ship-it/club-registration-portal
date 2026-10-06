package com.clubportal.controller;

import com.clubportal.dto.Dashboard;
import com.clubportal.model.User;
import com.clubportal.service.DashboardService;
import java.util.Map;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/api/dashboard")
    public Dashboard dashboard(@AuthenticationPrincipal User user) {
        return dashboardService.forUser(user);
    }

    @GetMapping("/api/health")
    public Map<String, String> health() {
        return Map.of("status", "UP");
    }
}
