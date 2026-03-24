package com.swiftbridge.orchestrator.controller;

import com.swiftbridge.orchestrator.dto.stats.UserStatsResponse;
import com.swiftbridge.orchestrator.service.UserManagementService;
import com.swiftbridge.orchestrator.service.UserStatsService;
import com.swiftbridge.orchestrator.dto.auth.UserResponseDTO;
import com.swiftbridge.orchestrator.dto.auth.UserUpdateDTO;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/api/v1/users", "/api/users"})
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserManagementService userManagementService;
    private final UserStatsService userStatsService;

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<UserResponseDTO>> getAllUsers() {
        log.info("GET /api/v1/users - Fetching all users");
        List<UserResponseDTO> users = userManagementService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserResponseDTO> getCurrentUserProfile() {
        log.info("GET /api/v1/users/me - Fetching current user profile");
        UserResponseDTO userProfile = userManagementService.getCurrentUserProfile();
        return ResponseEntity.ok(userProfile);
    }

    @GetMapping("/stats")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserStatsResponse> getCurrentUserStats() {
        log.info("GET /api/v1/users/stats - Fetching current user stats");
        return ResponseEntity.ok(userStatsService.getCurrentUserStats());
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserResponseDTO> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UserUpdateDTO updateDTO) {

        log.info("PUT /api/v1/users/{} - Updating user", id);
        UserResponseDTO updatedUser = userManagementService.updateUser(id, updateDTO);
        return ResponseEntity.ok(updatedUser);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Map<String, Object>> deleteUser(@PathVariable Long id) {
        log.info("DELETE /api/v1/users/{} - Deleting user", id);
        userManagementService.deleteUser(id);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "User deleted successfully",
                "userId", id
        ));
    }
}
