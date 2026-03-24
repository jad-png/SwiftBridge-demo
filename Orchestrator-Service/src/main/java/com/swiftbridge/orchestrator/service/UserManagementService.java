package com.swiftbridge.orchestrator.service;

import com.swiftbridge.orchestrator.dto.auth.UserResponseDTO;
import com.swiftbridge.orchestrator.dto.auth.UserUpdateDTO;

import java.util.List;

public interface UserManagementService {

    List<UserResponseDTO> getAllUsers();

    UserResponseDTO getCurrentUserProfile();

    UserResponseDTO updateUser(Long userId, UserUpdateDTO updateDTO);

    void deleteUser(Long userId);
}
