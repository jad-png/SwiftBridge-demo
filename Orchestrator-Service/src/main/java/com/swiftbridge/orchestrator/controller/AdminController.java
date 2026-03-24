package com.swiftbridge.orchestrator.controller;

import com.swiftbridge.orchestrator.dto.AdminStatsResponse;
import com.swiftbridge.orchestrator.service.AdminStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminStatsService adminStatsService;

    @GetMapping
    public ResponseEntity<AdminStatsResponse> adminInfo() {
        return ResponseEntity.ok(adminStatsService.getAdminStats());
    }

    @GetMapping("/stats")
    public ResponseEntity<AdminStatsResponse> adminStats() {
        return ResponseEntity.ok(adminStatsService.getAdminStats());
    }
}
