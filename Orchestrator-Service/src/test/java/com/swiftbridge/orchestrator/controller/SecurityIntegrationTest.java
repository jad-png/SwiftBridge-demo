package com.swiftbridge.orchestrator.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swiftbridge.orchestrator.dto.auth.LoginRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Security Integration Tests")
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("POST /api/auth/register is public - no JWT required")
    void testRegisterEndpointIsPublic() throws Exception {

        LoginRequest registerRequest = new LoginRequest();
        registerRequest.setUsername("publicuser@test.com");
        registerRequest.setPassword("Password123!");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/auth/login is public - no JWT required")
    void testLoginEndpointIsPublic() throws Exception {

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("admin");
        loginRequest.setPassword("admin123");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/convert returns 401 Unauthorized without valid JWT")
    void testConversionEndpointRequiresJwt() throws Exception {

        mockMvc.perform(get("/api/convert"))
            .andExpect(status().is(401));
    }

    @Test
    @DisplayName("GET /api/history/* returns 401 Unauthorized without valid JWT")
    void testHistoryEndpointRequiresJwt() throws Exception {

        mockMvc.perform(get("/api/history/123"))
            .andExpect(status().is(401));
    }

    @Test
    @DisplayName("GET /api/v1/users returns 401 Unauthorized without JWT (admin only)")
    void testUserManagementEndpointRequiresJwt() throws Exception {

        mockMvc.perform(get("/api/v1/users"))
            .andExpect(status().is(401));
    }

    @Test
    @DisplayName("GET /actuator/health is public - no JWT required")
    void testActuatorHealthIsPublic() throws Exception {

        mockMvc.perform(get("/actuator/health")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Invalid JWT token returns 401 Unauthorized")
    void testInvalidJwtTokenRejected() throws Exception {

        String invalidJwt = "Bearer invalid.token.format";

        mockMvc.perform(get("/api/v1/users")
                .header("Authorization", invalidJwt))
            .andExpect(status().is(401));
    }

    @Test
    @DisplayName("Malformed Authorization header returns 401")
    void testMalformedAuthorizationHeader() throws Exception {

        mockMvc.perform(get("/api/v1/users")
                .header("Authorization", "NotABearerToken"))
            .andExpect(status().is(401));
    }

    @Test
    @DisplayName("Missing Authorization header returns 401 for protected endpoint")
    void testMissingAuthorizationHeaderReturns401() throws Exception {

        mockMvc.perform(get("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().is(401));
    }

    @Test
    @DisplayName("POST /api/admin/* requires JWT and admin role")
    void testAdminEndpointsRequireAdminRole() throws Exception {

        mockMvc.perform(post("/api/admin/users")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().is(401));
    }

    @Test
    @DisplayName("CORS does not block public endpoints")
    void testCorsAllowsPublicEndpoints() throws Exception {

        LoginRequest request = new LoginRequest();
        request.setUsername("corstest@test.com");
        request.setPassword("Password123!");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Origin", "http://localhost:5173")
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Session creation is stateless - independent 401 responses")
    void testSessionIsStateless() throws Exception {

        mockMvc.perform(get("/api/convert"))
            .andExpect(status().is(401));

        mockMvc.perform(get("/api/convert"))
            .andExpect(status().is(401));

    }

    @Test
    @DisplayName("All protected endpoints consistently require authentication")
    void testMultipleProtectedEndpointsRequireAuth() throws Exception {

        mockMvc.perform(get("/api/history/test"))
            .andExpect(status().is(401));

        mockMvc.perform(get("/api/v1/users/me"))
            .andExpect(status().is(401));

        mockMvc.perform(post("/api/admin/test")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().is(401));
    }
}
