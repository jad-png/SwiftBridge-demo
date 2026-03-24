package com.swiftbridge.orchestrator.service.Impl;

import com.swiftbridge.orchestrator.dto.auth.UserResponseDTO;
import com.swiftbridge.orchestrator.dto.auth.UserUpdateDTO;
import com.swiftbridge.orchestrator.entity.AppUser;
import com.swiftbridge.orchestrator.exception.UnauthorizedException;
import com.swiftbridge.orchestrator.repository.AppUserRepository;
import com.swiftbridge.orchestrator.security.SecurityUtils;
import com.swiftbridge.orchestrator.security.UserPrincipal;
import com.swiftbridge.orchestrator.service.UserManagementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class UserManagementServiceImpl implements UserManagementService {

    private final AppUserRepository appUserRepository;
    private final SecurityUtils securityUtils;

    @Override
    public List<UserResponseDTO> getAllUsers() {
        if (!securityUtils.isAdmin()) {
            log.warn("Unauthorized attempt to list users by non-admin user");
            throw new UnauthorizedException("Only admins can list users");
        }

        log.info("Admin fetching all users");
        return appUserRepository.findAll().stream()
                .map(this::convertToResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public UserResponseDTO getCurrentUserProfile() {
        UserPrincipal currentUser = securityUtils.getCurrentUser();

        AppUser user = appUserRepository.findById(currentUser.getId())
                .orElseThrow(() -> {
                    log.error("Current user {} not found in database", currentUser.getId());
                    return new UnauthorizedException("User not found");
                });

        log.info("User {} fetching own profile", user.getUsername());
        return convertToResponseDTO(user);
    }

    @Override
    public UserResponseDTO updateUser(Long userId, UserUpdateDTO updateDTO) {
        UserPrincipal currentUser = securityUtils.getCurrentUser();

        boolean isAdmin = securityUtils.isAdmin();
        boolean isUpdatingSelf = currentUser.getId().equals(userId);

        if (!isAdmin && !isUpdatingSelf) {
            log.warn("User {} attempted to update another user {}", currentUser.getId(), userId);
            throw new UnauthorizedException("You can only update your own profile");
        }

        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("User {} not found", userId);
                    return new UnauthorizedException("User not found");
                });

        if (!isAdmin && updateDTO.getRole() != null) {
            log.warn("User {} attempted to change their role", currentUser.getId());
            throw new UnauthorizedException("Only admins can change user roles");
        }

        if (updateDTO.getEmail() != null && !updateDTO.getEmail().isEmpty()) {
            user.setEmail(updateDTO.getEmail());
            log.info("Email updated for user {}", user.getUsername());
        }

        if (updateDTO.getRole() != null && isAdmin) {
            user.setRole(updateDTO.getRole());
            log.info("Role updated for user {} to {}", user.getUsername(), updateDTO.getRole());
        }

        AppUser updatedUser = appUserRepository.save(user);
        return convertToResponseDTO(updatedUser);
    }

    @Override
    public void deleteUser(Long userId) {
        if (!securityUtils.isAdmin()) {
            log.warn("Unauthorized attempt to delete user by non-admin user");
            throw new UnauthorizedException("Only admins can delete users");
        }

        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("User {} not found for deletion", userId);
                    return new UnauthorizedException("User not found");
                });

        UserPrincipal currentUser = securityUtils.getCurrentUser();
        if (currentUser.getId().equals(userId)) {
            log.warn("Admin {} attempted to delete their own account", currentUser.getId());
            throw new UnauthorizedException("Cannot delete your own account");
        }

        appUserRepository.deleteById(userId);
        log.info("User {} deleted by admin", user.getUsername());
    }

    private UserResponseDTO convertToResponseDTO(AppUser user) {
        return UserResponseDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
