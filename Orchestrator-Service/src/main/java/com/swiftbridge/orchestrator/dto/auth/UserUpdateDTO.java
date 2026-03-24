package com.swiftbridge.orchestrator.dto.auth;

import com.swiftbridge.orchestrator.entity.AppRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserUpdateDTO {

    @Email(message = "Email should be valid")
    private String email;

    @NotNull(message = "Role cannot be null")
    private AppRole role;
}
