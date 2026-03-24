package com.swiftbridge.orchestrator.controller;

import com.swiftbridge.orchestrator.dto.auth.UserResponseDTO;
import com.swiftbridge.orchestrator.dto.stats.AdminStatsResponse;
import com.swiftbridge.orchestrator.service.AdminStatsService;
import com.swiftbridge.orchestrator.service.UserManagementService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminStatsService adminStatsService;
    private final UserManagementService userManagementService;

    @GetMapping
    public ResponseEntity<AdminStatsResponse> adminInfo() {
        return ResponseEntity.ok(adminStatsService.getAdminStats());
    }

    @GetMapping("/stats")
    public ResponseEntity<AdminStatsResponse> adminStats() {
        return ResponseEntity.ok(adminStatsService.getAdminStats());
    }

    @GetMapping("/users")
    public ResponseEntity<List<UserResponseDTO>> getAllUsersForAdmin() {
        return ResponseEntity.ok(userManagementService.getAllUsers());
    }
}
