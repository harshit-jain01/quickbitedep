package com.quickbite.auth.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.quickbite.auth.dto.AuthResponse;
import com.quickbite.auth.dto.UpdatePasswordRequest;
import com.quickbite.auth.dto.UpdateProfileRequest;
import com.quickbite.auth.dto.UserProfileResponse;
import com.quickbite.auth.dto.ValidateTokenResponse;
import com.quickbite.auth.exception.GlobalExceptionHandler;
import com.quickbite.auth.model.AuthProvider;
import com.quickbite.auth.model.UserRole;
import com.quickbite.auth.security.JwtAuthenticationFilter;
import com.quickbite.auth.service.AuthService;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @BeforeEach
    void setUp() {
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    void register_shouldReturn201_whenRequestIsValid() throws Exception {
        when(authService.register(any())).thenReturn(authResponse("token-1", "john@mail.com"));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName":"John",
                                  "email":"john@mail.com",
                                  "password":"Password@123",
                                  "phone":"9999999999",
                                  "role":"CUSTOMER"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("token-1"))
                .andExpect(jsonPath("$.user.email").value("john@mail.com"));

        verify(authService).register(any());
    }

    @Test
    void register_shouldReturn400_whenValidationFails() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName":"",
                                  "email":"invalid-email",
                                  "password":"short"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void login_shouldReturn200_whenCredentialsAreValid() throws Exception {
        when(authService.login(any())).thenReturn(authResponse("login-token", "alice@mail.com"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email":"alice@mail.com",
                                  "password":"Password@123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("login-token"));

        verify(authService).login(any());
    }

    @Test
    @WithMockUser(username = "principal@mail.com", roles = "CUSTOMER")
    void getProfile_shouldReturn200_whenAuthenticated() throws Exception {
        when(authService.getProfile("principal@mail.com")).thenReturn(profile("principal@mail.com"));

        mockMvc.perform(get("/api/v1/auth/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("principal@mail.com"));
        
        verify(authService).getProfile("principal@mail.com");
    }

    @Test
    @WithMockUser(username = "principal@mail.com", roles = "CUSTOMER")
    void updateProfile_shouldReturn200_whenValidRequest() throws Exception {
        when(authService.updateProfile(org.mockito.ArgumentMatchers.eq("principal@mail.com"), any()))
                .thenReturn(profile("principal@mail.com"));

        mockMvc.perform(put("/api/v1/auth/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateProfileRequest("Updated User", "9876543210", "https://img"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("principal@mail.com"));
        
        verify(authService).updateProfile(org.mockito.ArgumentMatchers.eq("principal@mail.com"), any());
    }

    @Test
    @WithMockUser(username = "principal@mail.com", roles = "CUSTOMER")
    void updateProfile_shouldReturn400_whenValidationFails() throws Exception {
        mockMvc.perform(put("/api/v1/auth/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateProfileRequest("", "9876543210", null))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @WithMockUser(username = "principal@mail.com", roles = "CUSTOMER")
    void updatePassword_shouldReturn200_whenValidRequest() throws Exception {
        mockMvc.perform(put("/api/v1/auth/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdatePasswordRequest("Current@123", "NewPass@123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password updated successfully"));

        verify(authService).updatePassword(org.mockito.ArgumentMatchers.eq("principal@mail.com"), any());
    }

    @Test
    void validate_shouldReturn200_whenRequestIsValid() throws Exception {
        when(authService.validateToken(any(), any())).thenReturn(new ValidateTokenResponse(true, "john@mail.com", UserRole.CUSTOMER));

        mockMvc.perform(post("/api/v1/auth/validate")
                        .header("Authorization", "Bearer token-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new java.util.LinkedHashMap<>())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.email").value("john@mail.com"));

        verify(authService).validateToken(any(), any());
    }

    @Test
    void oauth2Success_shouldReturnTokenFromAuthorizationHeader() throws Exception {
        mockMvc.perform(get("/api/v1/auth/oauth2/success")
                        .header("Authorization", "Bearer oauth-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("oauth-token"));
    }

    @Test
    void oauth2Success_shouldPreferQueryToken_whenBothPresent() throws Exception {
        mockMvc.perform(get("/api/v1/auth/oauth2/success")
                        .param("token", "query-token")
                        .header("Authorization", "Bearer header-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("query-token"));
    }

    @Test
    void oauth2Success_shouldReturnEmptyToken_whenNoTokenProvided() throws Exception {
        mockMvc.perform(get("/api/v1/auth/oauth2/success"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value(""));
    }

    private AuthResponse authResponse(String token, String email) {
        return new AuthResponse(
                token,
                "Bearer",
                3600,
                new UserProfileResponse(
                        UUID.randomUUID(),
                        "User",
                        email,
                        "9999999999",
                        UserRole.CUSTOMER,
                        AuthProvider.LOCAL,
                        true,
                        Instant.now(),
                        null
                )
        );
    }

    private UserProfileResponse profile(String email) {
        return new UserProfileResponse(
                UUID.randomUUID(),
                "User",
                email,
                "9999999999",
                UserRole.CUSTOMER,
                AuthProvider.LOCAL,
                true,
                Instant.now(),
                null
        );
    }
}
