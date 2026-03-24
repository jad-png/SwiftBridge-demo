package com.swiftbridge.orchestrator.controller;

import com.swiftbridge.orchestrator.dto.auth.LoginRequest;
import com.swiftbridge.orchestrator.dto.auth.LoginResponse;
import com.swiftbridge.orchestrator.dto.auth.RegisterRequest;
import com.swiftbridge.orchestrator.entity.AppRole;
import com.swiftbridge.orchestrator.entity.AppUser;
import com.swiftbridge.orchestrator.repository.AppUserRepository;
import com.swiftbridge.orchestrator.security.JwtUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );

            String token = jwtUtil.generateJwt(authentication);

            return ResponseEntity.ok(LoginResponse.builder()
                    .token(token)
                    .tokenType("Bearer")
                    .expiresIn(86400L)
                    .build());
        } catch (Exception e) {
            log.error("Authentication failed for user: {}", request.getUsername());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Invalid credentials");
            errorResponse.put("message", "Username or password is incorrect");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(LoginResponse.builder()
                    .token(null)
                    .tokenType("Bearer")
                    .build());
        }
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@Valid @RequestBody RegisterRequest request) {
        Map<String, Object> response = new HashMap<>();

        if (appUserRepository.findByUsername(request.getUsername()).isPresent()) {
            response.put("error", "Username already exists");
            response.put("message", "The username is already registered in the system");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        }

        try {

            String hashedPassword = passwordEncoder.encode(request.getPassword());

            AppUser newUser = AppUser.builder()
                    .username(request.getUsername())
                    .email(request.getEmail())
                    .passwordHash(hashedPassword)
                    .role(AppRole.ROLE_USER)
                    .build();

            AppUser savedUser = appUserRepository.save(newUser);
            log.info("User registered successfully: {}", savedUser.getUsername());

            response.put("success", true);
            response.put("message", "User registered successfully");
            response.put("userId", savedUser.getId());
            response.put("username", savedUser.getUsername());
            response.put("email", savedUser.getEmail());

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            log.error("Error during user registration: {}", e.getMessage(), e);
            response.put("error", "Registration failed");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
