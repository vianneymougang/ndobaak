package com.ndobaak.backend.dashboard.controller;

import com.ndobaak.backend.dashboard.dto.DailyViewsDTO;
import com.ndobaak.backend.dashboard.dto.LandlordDashboardResponse;
import com.ndobaak.backend.dashboard.dto.MonthlyRevenueDTO;
import com.ndobaak.backend.dashboard.service.DashboardService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/landlord")
    @PreAuthorize("hasRole('LANDLORD')")
    public ResponseEntity<LandlordDashboardResponse> getLandlordDashboard(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        if (to == null) to = LocalDate.now();
        if (from == null) from = to.minusDays(29);
        return ResponseEntity.ok(dashboardService.getLandlordDashboard(from, to));
    }

    @GetMapping("/landlord/revenue-chart")
    @PreAuthorize("hasRole('LANDLORD')")
    public ResponseEntity<List<MonthlyRevenueDTO>> getRevenueChart(
            @RequestParam(defaultValue = "12") int months) {
        return ResponseEntity.ok(dashboardService.getRevenueChart(months));
    }

    @GetMapping("/landlord/views-chart")
    @PreAuthorize("hasRole('LANDLORD')")
    public ResponseEntity<List<DailyViewsDTO>> getViewsChart(
            @RequestParam(defaultValue = "30") int days) {
        return ResponseEntity.ok(dashboardService.getViewsChart(days));
    }
}
