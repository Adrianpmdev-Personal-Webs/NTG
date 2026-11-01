package com.adrianpmdev.core.features.dashboard.api.controller;

import com.adrianpmdev.core.features.dashboard.api.dto.response.DashboardResponse;
import com.adrianpmdev.core.features.dashboard.application.service.DashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for the admin dashboard (ADMIN only, see {@code SecurityConfig}).
 *
 * @author adrianpmdev
 */
@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * Builds the controller with its dependencies.
     *
     * @param dashboardService dashboard service
     */
    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    /**
     * The dashboard as it is right now.
     *
     * @return dashboard data
     */
    @GetMapping
    public DashboardResponse get() {
        return dashboardService.get();
    }
}
